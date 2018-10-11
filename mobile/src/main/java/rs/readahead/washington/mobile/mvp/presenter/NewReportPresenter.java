package rs.readahead.washington.mobile.mvp.presenter;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.crashlytics.android.Crashlytics;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.reactivex.ObservableSource;
import io.reactivex.SingleSource;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Action;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;
import rs.readahead.washington.mobile.MyApplication;
import rs.readahead.washington.mobile.R;
import rs.readahead.washington.mobile.data.database.CacheWordDataSource;
import rs.readahead.washington.mobile.data.database.DataSource;
import rs.readahead.washington.mobile.data.repository.ReportRepository;
import rs.readahead.washington.mobile.data.sharedpref.Preferences;
import rs.readahead.washington.mobile.domain.entity.ContactSetting;
import rs.readahead.washington.mobile.domain.entity.MediaFile;
import rs.readahead.washington.mobile.domain.entity.MediaRecipient;
import rs.readahead.washington.mobile.domain.entity.Report;
import rs.readahead.washington.mobile.domain.exception.NotFountException;
import rs.readahead.washington.mobile.domain.repository.IReportRepository;
import rs.readahead.washington.mobile.mvp.contract.INewReportPresenterContract;
import rs.readahead.washington.mobile.presentation.entity.EvidenceData;
import rs.readahead.washington.mobile.presentation.entity.ReportRecipientData;
import rs.readahead.washington.mobile.presentation.entity.ReportViewType;
import rs.readahead.washington.mobile.queue.EvidenceQueue;
import rs.readahead.washington.mobile.util.jobs.EvidenceUploadJob;


public class NewReportPresenter implements INewReportPresenterContract.IPresenter {
    private INewReportPresenterContract.IView view;
    private CacheWordDataSource cacheWordDataSource;
    private IReportRepository reportRepository;
    private CompositeDisposable disposable;
    private ReportViewType type;

    private Report report; // current report in view
    @SuppressLint("UseSparseArrays")
    private Map<Long, MediaRecipient> combinedRecipients = new HashMap<>();


    public NewReportPresenter(INewReportPresenterContract.IView view) {
        this.view = view;
        cacheWordDataSource = new CacheWordDataSource(view.getContext().getApplicationContext());
        reportRepository = new ReportRepository();
        disposable = new CompositeDisposable();
    }

    @Override
    public void startNewReport(@Nullable EvidenceData evidenceData) {
        report = new Report();
        report.startTouchTracking();

        if (evidenceData != null) {
            setEvidenceData(evidenceData);
        }
    }

    @Override
    public void loadReport(final long id) {
        disposable.add(cacheWordDataSource.getDataSource()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .flatMapSingle(new Function<DataSource, SingleSource<Report>>() {
                    @Override
                    public SingleSource<Report> apply(DataSource dataSource) throws Exception {
                        return dataSource.loadReport(id);
                    }
                })
                // todo: loading indicators..
                .subscribe(new Consumer<Report>() {
                    @Override
                    public void accept(final Report report) throws Exception {
                        if (Report.NONE.equals(report)) {
                            view.onReportLoadError(new NotFountException());
                            return;
                        }

                        if (report.getSaved() == Report.Saved.ARCHIVE) { // if loading from archive, make if fresh to resend..
                            report.setId(Report.UNSAVED_REPORT_ID);
                            report.setUid(null);
                        }
                        report.startTouchTracking();

                        setCurrentReport(report);
                        updateView();
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) throws Exception {
                        view.onReportLoadError(throwable);
                    }
                })
        );
    }

    private void updateView() {
        view.setTitleText(report.getTitle());
        view.setDescription(report.getContent());
        view.setDate(report.getDate());
        getRecipientsCount();
        getEvidenceCount();
        view.onContactInfoAvailable(report.isContactInformation());
        view.setPublicInfo(report.isReportPublic(), report.isContactInformation());
        checkIfPreviewModeIsActive();
    }

    @Override
    public void saveDraft() {
        disposable.add(cacheWordDataSource.getDataSource()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .flatMapSingle(new Function<DataSource, SingleSource<Report>>() {
                    @Override
                    public SingleSource<Report> apply(DataSource dataSource) throws Exception {
                        report.setSaved(Report.Saved.DRAFT);
                        return dataSource.saveReport(report, report.getSaved());
                    }
                })
                // todo: loading indicators..
                .subscribe(new Consumer<Report>() {
                    @Override
                    public void accept(final Report report) throws Exception {
                        setCurrentReport(report);
                        report.startTouchTracking();
                        view.onDraftSaved();
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) throws Exception {
                        view.onDraftSaveError(throwable);
                    }
                })
        );
    }

    @Override
    public void sendReport() {
        if (!validateReport()) {
            view.onSendValidationFailed();
            return;
        }

        maybeRemoveMetadata();

        disposable.add(cacheWordDataSource.getDataSource()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe(new Consumer<Disposable>() {
                    @Override
                    public void accept(Disposable disposable) throws Exception {
                        view.onSendReportStart();
                    }
                })
                .flatMap(new Function<DataSource, ObservableSource<ContactSetting>>() {
                    @Override
                    public ObservableSource<ContactSetting> apply(DataSource dataSource) throws Exception {
                        return dataSource.getContactSetting().toObservable();
                    }
                })
                .flatMap(new Function<ContactSetting, ObservableSource<String>>() {
                    @Override
                    public ObservableSource<String> apply(ContactSetting setting) throws Exception {
                        if (!ContactSetting.NONE.equals(setting) && report.isContactInformation()) {
                            report.setContactInformationData(setting.getContactString(view.getContext()));
                            report.setReportPublic(false); // just in case, here also..
                        } else {
                            report.setContactInformationData(null);
                        }

                        return reportRepository.createReport(report, combinedRecipients);
                    }
                })
                .flatMap(new Function<String, ObservableSource<Report>>() {
                    @Override
                    public ObservableSource<Report> apply(final String uid) throws Exception {
                        return cacheWordDataSource.getDataSource().flatMap(new Function<DataSource, ObservableSource<Report>>() {
                            @Override
                            public ObservableSource<Report> apply(DataSource dataSource) throws Exception {
                                report.setUid(uid);
                                report.setSaved(Report.Saved.ARCHIVE);
                                return dataSource.saveReport(report, report.getSaved()).toObservable();
                            }
                        });
                    }
                })
                .doFinally(new Action() {
                    @Override
                    public void run() throws Exception {
                        view.onSendReportDone();
                    }
                })
                .subscribe(new Consumer<Report>() {
                    @Override
                    public void accept(Report report) throws Exception {
                        updateEvidenceQueue(report.getEvidences()); // start uploading jobs
                        view.onSentReport(getSentReportMsg());
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) throws Exception {
                        view.onSendReportError(throwable); // todo: IErrorBundle
                    }
                })
        );
    }

    @Override
    public void destroy() {
        view = null;
        disposable.dispose();
        cacheWordDataSource.dispose();
    }

    private void setCurrentReport(Report report) {
        this.report = report;
    }

    @Override
    public void checkAnonymousState() {
        if (view == null) return;
        view.showNoMetadataInfo(Preferences.isAnonymousMode());
    }

    @Override
    public void setReportTitle(String title) {
        if (isReportReady()) {
            report.setTitle(title);
        }
    }

    @Override
    public void setReportDate(Date date) {
        if (isReportReady()) {
            report.setDate(date);
            setDate();
        }
    }

    @Override
    public void setReportDescription(String description) {
        if (isReportReady()) {
            report.setContent(description);
        }
    }

    @Override
    public void setContactInfo(boolean useContactInfo) {
        report.setContactInformation(useContactInfo);

        if (report.isContactInformation()) {
            report.setReportPublic(false);
        }

        if (view != null) {
            view.setPublicInfo(report.isReportPublic(), report.isContactInformation());
        }
    }

    @Override
    public void setPublicInfo(boolean isReportPublic) {
        report.setReportPublic(isReportPublic);
    }

    @Override
    public boolean isReportTitleEmpty() {
        return TextUtils.isEmpty(report.getTitle());
    }

    @Override
    public boolean isReportDescriptionEmpty() {
        return TextUtils.isEmpty(report.getContent());
    }

    @Override
    public ReportRecipientData getRecipientData() {
        return new ReportRecipientData(report.getRecipients(), report.getRecipientLists());
    }

    @Override
    public void setRecipientData(ReportRecipientData recipientData) {
        report.setRecipients(recipientData.getMediaRecipients());
        report.setRecipientLists(recipientData.getMediaRecipientLists());
        getRecipientsCount();
    }

    @Override
    public void setEvidenceData(EvidenceData evidencedata) {
        report.setEvidences(evidencedata.getEvidences());
        getEvidenceCount();
    }

    private void getEvidenceCount() {
        int videoCount = 0, imageCount = 0, audioCount = 0;
        if (report.getEvidences().isEmpty()) {
            setEvidenceText(null);
            return;
        }

        for (MediaFile mediaFile : report.getEvidences()) {
            if (mediaFile.getType() == MediaFile.Type.IMAGE) {
                imageCount++;
            } else if (mediaFile.getType() == MediaFile.Type.AUDIO) {
                audioCount++;
            } else if (mediaFile.getType() == MediaFile.Type.VIDEO) {
                videoCount++;
            }
        }

        StringBuilder sb = new StringBuilder();

        if (view == null) return;
        if (imageCount > 0) {
            sb.append(String.format(view.getContext().getResources()
                    .getQuantityString(R.plurals.photo_number, imageCount), String.valueOf(imageCount)));
            sb.append(", ");
        }

        if (videoCount > 0) {
            sb.append(String.format(view.getContext().getResources()
                    .getQuantityString(R.plurals.video_number, videoCount), String.valueOf(videoCount)));
            sb.append(", ");
        }

        if (audioCount > 0) {
            sb.append(String.format(view.getContext().getResources()
                    .getQuantityString(R.plurals.audio_number, audioCount), String.valueOf(audioCount)));
            sb.append(", ");
        }

        String evidenceText = sb.substring(0, sb.length() - 2);
        setEvidenceText(evidenceText);
    }

    private void setEvidenceText(String text) {
        if (view == null) return;
        view.onEvidenceCounted(text);
    }

    @Override
    public void getRecipientsCount() {
        combinedRecipients.clear();
        for (MediaRecipient recipient : report.getRecipients()) {
            combinedRecipients.put(recipient.getId(), recipient);
        }
        view.onGetRecipientsCount(combinedRecipients.size());

        /*disposable.add(cacheWordDataSource.getDataSource()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .flatMapSingle(new Function<DataSource, SingleSource<Map<Long, MediaRecipient>>>() {
                    @Override
                    public SingleSource<Map<Long, MediaRecipient>> apply(DataSource dataSource) throws Exception {
                        return dataSource.getCombinedMediaRecipients(report.getRecipients(), report.getRecipientLists());
                    }
                })
                // todo: loading indicators..
                .subscribe(new Consumer<Map<Long, MediaRecipient>>() {
                    @Override
                    public void accept(final Map<Long, MediaRecipient> map) throws Exception {
                        combinedRecipients = map;
                        view.onGetRecipientsCount(combinedRecipients.size());
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) throws Exception {
                        view.onGetRecipientsCountError(throwable);
                    }
                })
        );*/
    }

    @Override
    public EvidenceData getEvidenceData() {
        return new EvidenceData(report.getEvidences());
    }

    @Override
    public void checkContactInfo() {
        disposable.add(cacheWordDataSource.getDataSource()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .flatMapSingle(new Function<DataSource, SingleSource<ContactSetting>>() {
                    @Override
                    public SingleSource<ContactSetting> apply(DataSource dataSource) throws Exception {
                        return dataSource.getContactSetting();
                    }
                })
                // todo: loading indicators..
                .subscribe(new Consumer<ContactSetting>() {
                    @Override
                    public void accept(final ContactSetting setting) throws Exception {
                        view.onContactInfoAvailable(!ContactSetting.NONE.equals(setting));
                        view.setContactInfo(!ContactSetting.NONE.equals(setting) && report.isContactInformation());
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) throws Exception {
                        Crashlytics.logException(throwable);
                        view.onContactInfoAvailable(false);
                    }
                })
        );
    }

    @Override
    public void setReportType(ReportViewType type) {
        this.type = type;
        setActivityTitle();
        checkIfPreviewModeIsActive();
    }

    private void setActivityTitle() {
        if (view == null || type == ReportViewType.NEW) return;
        view.setActivityTitle();
    }

    @Override
    public ReportViewType getReportType() {
        return type;
    }

    @Override
    public boolean isInPreviewMode() {
        return type == ReportViewType.PREVIEW;
    }

    @Override
    public boolean isReportChanged() {
        return report.isTouched();
    }

    private void setDate() {
        if (view == null || report.getDate() == null) return;
        view.setDate(report.getDate());
    }

    private void maybeRemoveMetadata() {
        if (Preferences.isAnonymousMode()) {
            for (MediaFile mediaFile : report.getEvidences()) {
                if (mediaFile.getMetadata() != null) {
                    mediaFile.getMetadata().clear();
                }
            }
        }
    }

    private boolean validateReport() {

        boolean result = true;


        if (TextUtils.isEmpty(report.getTitle())) {
            setTitleError();
            result = false;
        }

        if (report.getDate() == null) {
            setDateError();
            result = false;
        }

        if (report.getEvidences().size() == 0) {
            setEvidenceError();
            result = false;
        }

        if (combinedRecipients.size() == 0) {
            setRecipientsError();
            result = false;
        }

        return result;
    }

    private void updateEvidenceQueue(List<MediaFile> evidences) {
        if (evidences.isEmpty()) {
            return;
        }

        EvidenceQueue queue = MyApplication.evidenceQueue();
        if (queue == null) {
            return;
        }

        for (MediaFile mediaFile : evidences) {
            queue.add(mediaFile);
        }

        EvidenceUploadJob.scheduleJob();
    }

    private String getSentReportMsg() {
        return view.getContext().getString(R.string.sending_report_success) +
                (report.getEvidences().isEmpty() ? "" :
                        " " + view.getContext().getString(R.string.sending_report_evidences_msg));
    }

    private void setTitleError() {
        if (view == null) return;
        view.setTitleIndicatorColor(Color.RED);
    }

    private void setDateError() {
        if (view == null) return;
        view.setDateIndicatorColor(Color.RED);

    }

    private void setEvidenceError() {
        if (view == null) return;
        view.setEvidenceIndicatorColor(Color.RED);
    }

    private void setRecipientsError() {
        if (view == null) return;
        view.setRecipientsIndicatorColor(Color.RED);
    }

    private void checkIfPreviewModeIsActive() {
        if (view == null) return;
        view.setPreviewMode(type == ReportViewType.PREVIEW);
    }

    private boolean isReportReady() {
        return report != null;
    }
}
