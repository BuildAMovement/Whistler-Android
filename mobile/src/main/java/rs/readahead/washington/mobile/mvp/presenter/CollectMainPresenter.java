package rs.readahead.washington.mobile.mvp.presenter;

import com.crashlytics.android.Crashlytics;

import org.javarosa.core.model.FormDef;

import info.guardianproject.cacheword.CacheWordHandler;
import info.guardianproject.cacheword.ICacheWordSubscriber;
import io.reactivex.CompletableSource;
import io.reactivex.ObservableSource;
import io.reactivex.SingleSource;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.annotations.NonNull;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.functions.Action;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.AsyncSubject;
import rs.readahead.washington.mobile.data.database.CacheWordDataSource;
import rs.readahead.washington.mobile.data.database.DataSource;
import rs.readahead.washington.mobile.data.repository.OpenRosaRepository;
import rs.readahead.washington.mobile.domain.entity.collect.CollectForm;
import rs.readahead.washington.mobile.domain.entity.collect.CollectFormInstance;
import rs.readahead.washington.mobile.domain.entity.collect.CollectFormInstanceStatus;
import rs.readahead.washington.mobile.domain.entity.collect.CollectServer;
import rs.readahead.washington.mobile.domain.repository.IOpenRosaRepository;
import rs.readahead.washington.mobile.mvp.contract.ICollectMainPresenterContract;


public class CollectMainPresenter implements
        ICollectMainPresenterContract.IPresenter,
        ICacheWordSubscriber {
    private IOpenRosaRepository odkRepository;
    private ICollectMainPresenterContract.IView view;
    private CacheWordHandler cacheWordHandler;
    private CacheWordDataSource cacheWordDataSource;
    private CompositeDisposable disposables = new CompositeDisposable();
    private AsyncSubject<DataSource> asyncDataSource = AsyncSubject.create();


    public CollectMainPresenter(ICollectMainPresenterContract.IView view) {
        this.odkRepository = new OpenRosaRepository();
        this.view = view;
        this.cacheWordDataSource = new CacheWordDataSource(view.getContext());
        this.cacheWordHandler = new CacheWordHandler(view.getContext().getApplicationContext(), this);

        cacheWordHandler.connectToService();
    }

    @Override
    public void getBlankFormDef(final CollectForm form) {
        disposables.add(asyncDataSource
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .flatMap(new Function<DataSource, ObservableSource<FormDef>>() {
                    @Override
                    public ObservableSource<FormDef> apply(@NonNull DataSource dataSource) throws Exception {
                        return dataSource.getBlankFormDef(form).toObservable();
                    }
                }).subscribe(new Consumer<FormDef>() {
                    @Override
                    public void accept(@NonNull FormDef formDef) throws Exception {
                        view.onGetBlankFormDefSuccess(form, formDef);
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(@NonNull Throwable throwable) throws Exception {
                        Crashlytics.logException(throwable);
                        view.onFormDefError(throwable);
                    }
                })
        );
    }

    @Override
    public void downloadBlankFormDef(final CollectForm form) {
        disposables.add(asyncDataSource
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .flatMap(new Function<DataSource, ObservableSource<CollectServer>>() {
                    @Override
                    public ObservableSource<CollectServer> apply(@NonNull DataSource dataSource) throws Exception {
                        return dataSource.getCollectServer(form.getServerId()).toObservable();
                    }
                }).flatMap(new Function<CollectServer, ObservableSource<FormDef>>() {
                    @Override
                    public ObservableSource<FormDef> apply(@NonNull CollectServer server) throws Exception {
                        return odkRepository.getFormDef(server, form).toObservable();
                    }
                }).flatMap(new Function<FormDef, ObservableSource<FormDef>>() {
                    @Override
                    public ObservableSource<FormDef> apply(@NonNull final FormDef formDef) throws Exception {
                        return asyncDataSource.flatMap(new Function<DataSource, ObservableSource<FormDef>>() {
                            @Override
                            public ObservableSource<FormDef> apply(@NonNull DataSource dataSource) throws Exception {
                                return dataSource.updateBlankFormDef(form, formDef).toObservable();
                            }
                        });
                    }
                }).subscribe(new Consumer<FormDef>() {
                    @Override
                    public void accept(@NonNull FormDef formDef) throws Exception {
                        view.onDownloadBlankFormDefSuccess(form, formDef);
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(@NonNull Throwable throwable) throws Exception {
                        Crashlytics.logException(throwable);
                        view.onFormDefError(throwable);
                    }
                })
        );
    }

    @Override
    public void getInstanceFormDef(final long instanceId) {
        disposables.add(asyncDataSource
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .flatMapSingle(new Function<DataSource, SingleSource<CollectFormInstance>>() {
                    @Override
                    public SingleSource<CollectFormInstance> apply(@NonNull DataSource dataSource) throws Exception {
                        return dataSource.getInstance(instanceId);
                    }
                }).subscribe(new Consumer<CollectFormInstance>() {
                    @Override
                    public void accept(@NonNull CollectFormInstance instance) throws Exception {
                        view.onInstanceFormDefSuccess(maybeCloneInstance(instance));
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(@NonNull Throwable throwable) throws Exception {
                        Crashlytics.logException(throwable);
                        view.onFormDefError(throwable);
                    }
                })
        );
    }

    @Override
    public void toggleFavorite(final CollectForm collectForm) {
        disposables.add(asyncDataSource
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .flatMapSingle(new Function<DataSource, SingleSource<CollectForm>>() {
                    @Override
                    public SingleSource<CollectForm> apply(@NonNull DataSource dataSource) throws Exception {
                        return dataSource.toggleFavorite(collectForm);
                    }
                }).subscribe(new Consumer<CollectForm>() {
                    @Override
                    public void accept(@NonNull CollectForm form) throws Exception {
                        view.onToggleFavoriteSuccess(form);
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(@NonNull Throwable throwable) throws Exception {
                        Crashlytics.logException(throwable);
                        view.onToggleFavoriteError(throwable);
                    }
                })
        );
    }

    @Override
    public void deleteFormInstance(final long id) {
        disposables.add(asyncDataSource
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .flatMapCompletable(new Function<DataSource, CompletableSource>() {
                    @Override
                    public CompletableSource apply(@NonNull DataSource dataSource) throws Exception {
                        return dataSource.deleteInstance(id);
                    }
                })
                .subscribe(new Action() {
                    @Override
                    public void run() throws Exception {
                        view.onFormInstanceDeleteSuccess();
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) throws Exception {
                        Crashlytics.logException(throwable);
                        view.onFormInstanceDeleteError(throwable);
                    }
                })
        );
    }

    @Override
    public void countCollectServers() {
        disposables.add(cacheWordDataSource.getDataSource()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .flatMapSingle(new Function<DataSource, SingleSource<Long>>() {
                    @Override
                    public SingleSource<Long> apply(DataSource dataSource) throws Exception {
                        return dataSource.countCollectServers();
                    }
                })
                .subscribe(new Consumer<Long>() {
                    @Override
                    public void accept(Long num) throws Exception {
                        view.onCountCollectServersEnded(num);
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) throws Exception {
                        Crashlytics.logException(throwable);
                        view.onCountCollectServersFailed(throwable);
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
        cacheWordDataSource.dispose();
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
    private CollectFormInstance maybeCloneInstance(CollectFormInstance instance) {
        if (instance.getStatus() == CollectFormInstanceStatus.SUBMITTED) {
            instance.setClonedId(instance.getId()); // we are clone of submitted form
            instance.setId(0);
            instance.setStatus(CollectFormInstanceStatus.UNKNOWN);
            instance.setUpdated(0);
            instance.setInstanceName(instance.getFormName());
        }

        return instance;
    }
}
