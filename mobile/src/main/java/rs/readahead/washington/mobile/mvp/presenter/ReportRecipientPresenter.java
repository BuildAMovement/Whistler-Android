package rs.readahead.washington.mobile.mvp.presenter;

import java.util.List;

import io.reactivex.SingleSource;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;
import rs.readahead.washington.mobile.data.database.CacheWordDataSource;
import rs.readahead.washington.mobile.data.database.DataSource;
import rs.readahead.washington.mobile.domain.entity.MediaRecipient;
import rs.readahead.washington.mobile.domain.entity.MediaRecipientList;
import rs.readahead.washington.mobile.presentation.entity.ReportRecipientData;
import rs.readahead.washington.mobile.mvp.contract.IReportRecipientsPresenterContract;
import rs.readahead.washington.mobile.presentation.entity.ReportViewType;


public class ReportRecipientPresenter implements IReportRecipientsPresenterContract.IPresenter {
    private IReportRecipientsPresenterContract.IView view;
    private CacheWordDataSource cacheWordDataSource;
    private CompositeDisposable disposable;
    private ReportRecipientData reportRecipientData = new ReportRecipientData();
    private ReportViewType type;


    public ReportRecipientPresenter(IReportRecipientsPresenterContract.IView view) {
        this.view = view;
        cacheWordDataSource = new CacheWordDataSource(view.getContext().getApplicationContext());
        disposable = new CompositeDisposable();
    }

    @Override
    public void listAllRecipients() {
        disposable.add(cacheWordDataSource.getDataSource()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .flatMapSingle(new Function<DataSource, SingleSource<List<MediaRecipient>>>() {
                    @Override
                    public SingleSource<List<MediaRecipient>> apply(DataSource dataSource) throws Exception {
                        return dataSource.listMediaRecipients();
                    }
                })
                // todo: add progress callbacks
                .subscribe(new Consumer<List<MediaRecipient>>() {
                    @Override
                    public void accept(List<MediaRecipient> mediaRecipients) throws Exception {
                        view.onAllRecipient(mediaRecipients);
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) throws Exception {
                        view.onAllRecipientError(throwable);
                    }
                })
        );
    }

    @Override
    public void listNonEmptyRecipientLists() {
        disposable.add(cacheWordDataSource.getDataSource()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .flatMapSingle(new Function<DataSource, SingleSource<List<MediaRecipientList>>>() {
                    @Override
                    public SingleSource<List<MediaRecipientList>> apply(DataSource dataSource) throws Exception {
                        return dataSource.listNonEmptyMediaRecipientLists();
                    }
                })
                // todo: add progress callbacks
                .subscribe(new Consumer<List<MediaRecipientList>>() {
                    @Override
                    public void accept(List<MediaRecipientList> mediaRecipientLists) throws Exception {
                        view.onListNonEmptyRecipientList(mediaRecipientLists);
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) throws Exception {
                        view.onListNonEmptyRecipientListError(throwable);
                    }
                })
        );
    }

    @Override
    public void addMediaRecipient(final MediaRecipient mediaRecipient) {
        disposable.add(cacheWordDataSource.getDataSource()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .flatMapSingle(new Function<DataSource, SingleSource<MediaRecipient>>() {
                    @Override
                    public SingleSource<MediaRecipient> apply(DataSource dataSource) throws Exception {
                        return dataSource.addMediaRecipient(mediaRecipient);
                    }
                })
                // todo: add progress callbacks
                .subscribe(new Consumer<MediaRecipient>() {
                    @Override
                    public void accept(MediaRecipient mr) throws Exception {
                        view.onAddMediaRecipient(mr);
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) throws Exception {
                        view.onAddMediaRecipientError(throwable);
                    }
                })
        );
    }

    @Override
    public void addMediaRecipientList(final MediaRecipientList mediaRecipientList) {
        disposable.add(cacheWordDataSource.getDataSource()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .flatMapSingle(new Function<DataSource, SingleSource<MediaRecipientList>>() {
                    @Override
                    public SingleSource<MediaRecipientList> apply(DataSource dataSource) throws Exception {
                        return dataSource.addMediaRecipientList(mediaRecipientList);
                    }
                })
                // todo: add progress callbacks
                .subscribe(new Consumer<MediaRecipientList>() {
                    @Override
                    public void accept(MediaRecipientList mrl) throws Exception {
                        view.onAddMediaRecipientList(mrl);
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) throws Exception {
                        view.onAddMediaRecipientListError(throwable);
                    }
                })
        );
    }

    @Override
    public void setRecipientData(ReportRecipientData recipientData) {
        reportRecipientData = recipientData;
        if (!isInPreviewMode()) return;
        setPreviewData();
    }

    private void setPreviewData() {
        if (view == null) return;
        view.onPreviewMode();
        view.onListNonEmptyRecipientList(reportRecipientData.getMediaRecipientLists());
        view.onAllRecipient(reportRecipientData.getMediaRecipients());
    }

    @Override
    public boolean ifRecipientIsSelected(MediaRecipient mediaRecipient) {
        return reportRecipientData.getMediaRecipients().contains(mediaRecipient);
    }

    @Override
    public boolean ifRecipientListIsSelected(MediaRecipientList mediaRecipientList) {
        return reportRecipientData.getMediaRecipientLists().contains(mediaRecipientList);
    }

    @Override
    public ReportRecipientData getRecipientsData() {
        return reportRecipientData;
    }

    @Override
    public void addRecipientToReport(MediaRecipient mediaRecipient) {
        reportRecipientData.getMediaRecipients().add(mediaRecipient);
    }

    @Override
    public void removeRecipientFromReport(MediaRecipient mediaRecipient) {
        reportRecipientData.getMediaRecipients().remove(mediaRecipient);
    }

    @Override
    public void addRecipientListToReport(MediaRecipientList mediaRecipientList) {
        reportRecipientData.getMediaRecipientLists().add(mediaRecipientList);
    }

    @Override
    public void removeRecipientListFromReport(MediaRecipientList mediaRecipientList) {
        reportRecipientData.getMediaRecipientLists().remove(mediaRecipientList);
    }

//    @Override
//    public boolean checkSelectMenuItemStatus() {
//        return !reportRecipientData.getMediaRecipientLists().isEmpty() || !reportRecipientData.getMediaRecipients().isEmpty();
//    }

    @Override
    public void destroy() {
        view = null;
        disposable.dispose();
        cacheWordDataSource.dispose();
    }

    @Override
    public void setReportType(ReportViewType type) {
        this.type = type;
    }

    @Override
    public ReportViewType getReportType() {
        return type;
    }

    @Override
    public boolean isInPreviewMode() {
        return type == ReportViewType.PREVIEW;
    }
}
