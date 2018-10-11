package rs.readahead.washington.mobile.javarosa;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.crashlytics.android.Crashlytics;

import org.javarosa.core.model.FormDef;
import org.javarosa.core.model.FormIndex;
import org.javarosa.core.model.data.IAnswerData;
import org.javarosa.core.model.data.StringData;
import org.javarosa.form.api.FormEntryController;
import org.javarosa.form.api.FormEntryModel;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

import info.guardianproject.cacheword.CacheWordHandler;
import info.guardianproject.cacheword.ICacheWordSubscriber;
import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.Single;
import io.reactivex.SingleSource;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Action;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.AsyncSubject;
import rs.readahead.washington.mobile.MyApplication;
import rs.readahead.washington.mobile.R;
import rs.readahead.washington.mobile.data.repository.MediaFileRepository;
import rs.readahead.washington.mobile.data.repository.OpenRosaRepository;
import rs.readahead.washington.mobile.data.database.DataSource;
import rs.readahead.washington.mobile.data.sharedpref.Preferences;
import rs.readahead.washington.mobile.domain.entity.MediaFile;
import rs.readahead.washington.mobile.domain.entity.collect.CollectFormInstance;
import rs.readahead.washington.mobile.domain.entity.collect.CollectFormInstanceStatus;
import rs.readahead.washington.mobile.domain.entity.collect.CollectServer;
import rs.readahead.washington.mobile.domain.entity.collect.NegotiatedCollectServer;
import rs.readahead.washington.mobile.domain.entity.collect.OpenRosaResponse;
import rs.readahead.washington.mobile.domain.exception.NoConnectivityException;
import rs.readahead.washington.mobile.domain.repository.IMediaFileRepository;
import rs.readahead.washington.mobile.domain.repository.IOpenRosaRepository;
import rs.readahead.washington.mobile.odk.FormController;
import rs.readahead.washington.mobile.queue.RawMediaFileQueue;
import rs.readahead.washington.mobile.util.jobs.PendingFormSendJob;
import timber.log.Timber;


public class FormSubmitter implements IFormSubmitterContract.IFormSubmitter,
        ICacheWordSubscriber {
    private IFormSubmitterContract.IView view;
    private AsyncSubject<DataSource> asyncDataSource = AsyncSubject.create();
    private CacheWordHandler cacheWordHandler;
    private CompositeDisposable disposables = new CompositeDisposable();
    private IOpenRosaRepository openRosaRepository;
    private IMediaFileRepository mediaFileRepository;
    private Context context;


    public FormSubmitter(IFormSubmitterContract.IView view) {
        this.view = view;
        this.context = view.getContext().getApplicationContext();
        this.openRosaRepository = new OpenRosaRepository();
        this.mediaFileRepository = new MediaFileRepository();
        this.cacheWordHandler = new CacheWordHandler(this.context, this);
        cacheWordHandler.connectToService();
    }

    @Override
    public void submitActiveFormInstance(String name) {
        CollectFormInstance instance = FormController.getActive().getCollectFormInstance();

        if (! TextUtils.isEmpty(name)) {
            instance.setInstanceName(name);
        }

        submitFormInstance(instance);
    }

    @Override
    public void submitFormInstance(final CollectFormInstance instance) {
        disposables.add(asyncDataSource
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe(new Consumer<Disposable>() {
                    @Override
                    public void accept(@NonNull Disposable disposable) throws Exception {
                        view.showFormSubmitLoading();
                    }
                })
                .flatMapSingle(new Function<DataSource, SingleSource<CollectServer>>() {
                    @Override
                    public SingleSource<CollectServer> apply(@NonNull final DataSource dataSource) throws Exception {
                        // put attachment urls in form field
                        if (hasAttachments(instance)) {
                            addMediaFileUrls(instance.getFormDef(), instance.getMediaFiles());
                        }

                        // finalize form (FormDef & CollectFormInstance)
                        instance.getFormDef().postProcessInstance();
                        instance.setStatus(CollectFormInstanceStatus.FINALIZED);

                        return dataSource.saveInstance(instance).flatMap(new Function<CollectFormInstance, SingleSource<CollectServer>>() {
                            @Override
                            public SingleSource<CollectServer> apply(@NonNull CollectFormInstance instance) throws Exception {
                                return dataSource.getCollectServer(instance.getServerId());
                            }
                        });
                    }
                })
                .flatMapSingle(new Function<CollectServer, SingleSource<CollectServer>>() {
                    @Override
                    public SingleSource<CollectServer> apply(@NonNull CollectServer server) throws Exception {
                        if (hasAttachments(instance)) {
                            List<MediaFile> mediaFiles = instance.getMediaFiles();

                            if (Preferences.isAnonymousMode()) {
                                for (MediaFile mediaFile: mediaFiles) {
                                    mediaFile.setMetadata(null);
                                }
                            }

                            return mediaFileRepository.registerFormAttachments(mediaFiles).toSingleDefault(server);
                        } else {
                            return Single.just(server);
                        }
                    }
                })
                .flatMapSingle(new Function<CollectServer, SingleSource<NegotiatedCollectServer>>() {
                    @Override
                    public SingleSource<NegotiatedCollectServer> apply(@NonNull CollectServer server) throws Exception {
                        if (! MyApplication.isConnectedToInternet(view.getContext())) {
                            throw new NoConnectivityException();
                        }

                        return openRosaRepository.submitFormNegotiate(server);
                    }
                })
                .flatMapSingle(new Function<NegotiatedCollectServer, SingleSource<OpenRosaResponse>>() {
                    @Override
                    public SingleSource<OpenRosaResponse> apply(@NonNull NegotiatedCollectServer server) throws Exception {
                        return openRosaRepository.submitForm(server, instance);
                    }
                })
                .flatMap(new Function<OpenRosaResponse, ObservableSource<OpenRosaResponse>>() {
                    @Override
                    public ObservableSource<OpenRosaResponse> apply(@NonNull final OpenRosaResponse response) throws Exception {
                        // mark form submitted
                        instance.setStatus(CollectFormInstanceStatus.SUBMITTED);
                        return rxSaveFormInstance(instance, response, null);
                    }
                })
                .onErrorResumeNext(new Function<Throwable, ObservableSource<? extends OpenRosaResponse>>() {
                    @Override
                    public ObservableSource<? extends OpenRosaResponse> apply(@NonNull Throwable throwable) throws Exception {
                        instance.setStatus(throwable instanceof NoConnectivityException ?
                                CollectFormInstanceStatus.SUBMISSION_PENDING :
                                CollectFormInstanceStatus.SUBMISSION_ERROR);

                        return rxSaveFormInstance(instance, null, throwable);
                    }
                })
                .doFinally(new Action() {
                    @Override
                    public void run() throws Exception {
                        view.hideFormSubmitLoading();
                    }
                })
                .subscribe(new Consumer<OpenRosaResponse>() {
                    @Override
                    public void accept(@NonNull OpenRosaResponse openRosaResponse) throws Exception {
                        // start attachment upload process
                        if (hasAttachments(instance)) {
                            updateMediaFilesQueue(instance.getMediaFiles());
                        }

                        view.formSubmitSuccess(instance, openRosaResponse);
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(@NonNull Throwable throwable) throws Exception {
                        if (throwable instanceof NoConnectivityException) {
                            PendingFormSendJob.scheduleJob();
                            view.formSubmitNoConnectivity();
                        } else {
                            Crashlytics.logException(throwable);
                            view.formSubmitError(throwable);
                        }
                    }
                })
        );
    }

    @Override
    public void destroy() {
        if (cacheWordHandler != null) {
            cacheWordHandler.disconnectFromService();
        }
        disposables.dispose();
        view = null;
    }

    @Override
    public void onCacheWordUninitialized() {
    }

    @Override
    public void onCacheWordLocked() {
    }

    @Override
    public void onCacheWordOpened() {
        if (view != null) {
            DataSource dataSource = DataSource.getInstance(view.getContext(), cacheWordHandler.getEncryptionKey());
            asyncDataSource.onNext(dataSource);
            asyncDataSource.onComplete();
        }

        cacheWordHandler.disconnectFromService();
        cacheWordHandler = null;
    }

    @SuppressWarnings("MethodOnlyUsedFromInnerClass")
    private void addMediaFileUrls(FormDef formDef, Collection<MediaFile> attachments) {
        FormIndex index = FormUtils.findWhistlerAttachmentFieldIndex(context, formDef);

        FormEntryModel formEntryModel = new FormEntryModel(formDef);
        FormEntryController formEntryController = new FormEntryController(formEntryModel);

        List<String> urls = new ArrayList<>(attachments.size());
        String urlFormat = context.getString(R.string.ra_config_form_attachment_url);
        for (MediaFile attachment: attachments) {
            urls.add(String.format(Locale.US, urlFormat, attachment.getUid()));
        }

        IAnswerData data = new StringData(TextUtils.join("\n", urls));

        try {
            formEntryController.answerQuestion(index, data, true);
        } catch (Exception e) {
            Crashlytics.logException(e);
            Timber.e(e, getClass().getName());
        }
    }

    private <T> ObservableSource<T> rxSaveFormInstance(final CollectFormInstance instance, final T value, @Nullable final Throwable throwable) {
        return asyncDataSource.flatMap(new Function<DataSource, ObservableSource<T>>() {
            @Override
            public ObservableSource<T> apply(@NonNull final DataSource dataSource) throws Exception {
                return dataSource.saveInstance(instance).toObservable().flatMap(new Function<CollectFormInstance, ObservableSource<T>>() {
                    @Override
                    public ObservableSource<T> apply(@NonNull CollectFormInstance instance) throws Exception {
                        if (throwable == null) {
                            return Observable.just(value);
                        }

                        return Observable.error(throwable);
                    }
                });
            }
        });
    }

    private boolean hasAttachments(CollectFormInstance instance) {
        return instance.getMediaFiles().size() > 0;
    }

    @SuppressWarnings("MethodOnlyUsedFromInnerClass")
    private void updateMediaFilesQueue(@NonNull Collection<MediaFile> attachments) {
        if (attachments.size() == 0) {
            return;
        }

        RawMediaFileQueue queue = MyApplication.mediaFileQueue();

        if (queue == null) {
            return;
        }

        for (MediaFile mediaFile: attachments) {
            queue.addAndStartUpload(mediaFile);
        }
    }
}
