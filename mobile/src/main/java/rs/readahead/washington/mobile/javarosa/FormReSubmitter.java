package rs.readahead.washington.mobile.javarosa;

import android.content.Context;
import android.support.annotation.NonNull;

import com.crashlytics.android.Crashlytics;

import io.reactivex.Observable;
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
import rs.readahead.washington.mobile.data.repository.OpenRosaRepository;
import rs.readahead.washington.mobile.data.database.CacheWordDataSource;
import rs.readahead.washington.mobile.data.database.DataSource;
import rs.readahead.washington.mobile.domain.entity.collect.CollectFormInstance;
import rs.readahead.washington.mobile.domain.entity.collect.CollectFormInstanceStatus;
import rs.readahead.washington.mobile.domain.entity.collect.CollectServer;
import rs.readahead.washington.mobile.domain.entity.collect.NegotiatedCollectServer;
import rs.readahead.washington.mobile.domain.entity.collect.OpenRosaResponse;
import rs.readahead.washington.mobile.domain.exception.NoConnectivityException;
import rs.readahead.washington.mobile.domain.repository.IOpenRosaRepository;


public class FormReSubmitter implements IFormReSubmitterContract.IFormReSubmitter {
    private IFormReSubmitterContract.IView view;
    private CacheWordDataSource cacheWordDataSource;
    private CompositeDisposable disposables = new CompositeDisposable();
    private IOpenRosaRepository openRosaRepository;
    private Context context;


    public FormReSubmitter(IFormReSubmitterContract.IView view) {
        this.view = view;
        this.context = view.getContext().getApplicationContext();
        this.openRosaRepository = new OpenRosaRepository();
        this.cacheWordDataSource = new CacheWordDataSource(context);
    }

    @Override
    public void reSubmitFormInstance(final CollectFormInstance instance) {
        disposables.add(cacheWordDataSource.getDataSource()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe(new Consumer<Disposable>() {
                    @Override
                    public void accept(@NonNull Disposable disposable) throws Exception {
                        view.showReFormSubmitLoading();
                    }
                })
                .flatMapSingle(new Function<DataSource, SingleSource<CollectServer>>() {
                    @Override
                    public SingleSource<CollectServer> apply(@NonNull final DataSource dataSource) throws Exception {
                        return dataSource.getInstance(instance.getId()).flatMap(new Function<CollectFormInstance, SingleSource<CollectServer>>() {
                            @Override
                            public SingleSource<CollectServer> apply(@NonNull CollectFormInstance fullInstance) throws Exception {
                                instance.setFormDef(fullInstance.getFormDef()); // todo: think about this..
                                return dataSource.getCollectServer(instance.getServerId());
                            }
                        });
                    }
                })
                .flatMapSingle(new Function<CollectServer, SingleSource<NegotiatedCollectServer>>() {
                    @Override
                    public SingleSource<NegotiatedCollectServer> apply(@NonNull CollectServer server) throws Exception {
                        if (! MyApplication.isConnectedToInternet(context)) {
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
                        instance.setStatus(CollectFormInstanceStatus.SUBMITTED);
                        return rxSaveSuccessInstance(instance, response);
                    }
                })
                .onErrorResumeNext(new Function<Throwable, ObservableSource<? extends OpenRosaResponse>>() {
                    @Override
                    public ObservableSource<? extends OpenRosaResponse> apply(@NonNull Throwable throwable) throws Exception {
                        instance.setStatus(throwable instanceof NoConnectivityException ?
                                CollectFormInstanceStatus.SUBMISSION_PENDING :
                                CollectFormInstanceStatus.SUBMISSION_ERROR);

                        return rxSaveErrorInstance(instance, throwable);
                    }
                })
                .doFinally(new Action() {
                    @Override
                    public void run() throws Exception {
                        view.hideReFormSubmitLoading();
                    }
                })
                .subscribe(new Consumer<OpenRosaResponse>() {
                    @Override
                    public void accept(@NonNull OpenRosaResponse openRosaResponse) throws Exception {
                        view.formReSubmitSuccess(instance, openRosaResponse);
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(@NonNull Throwable throwable) throws Exception {
                        if (throwable instanceof NoConnectivityException) {
                            view.formReSubmitNoConnectivity();
                        } else {
                            Crashlytics.logException(throwable);
                            view.formReSubmitError(throwable);
                        }
                    }
                })
        );
    }

    @Override
    public void destroy() {
        cacheWordDataSource.dispose();
        disposables.dispose();
        view = null;
    }

    @SuppressWarnings("MethodOnlyUsedFromInnerClass")
    private <T> ObservableSource<T> rxSaveSuccessInstance(final CollectFormInstance instance, final T value) {
        return cacheWordDataSource.getDataSource().flatMap(new Function<DataSource, ObservableSource<T>>() {
            @Override
            public ObservableSource<T> apply(@NonNull final DataSource dataSource) throws Exception {
                return dataSource.saveInstance(instance).toObservable().flatMap(new Function<CollectFormInstance, ObservableSource<T>>() {
                    @Override
                    public ObservableSource<T> apply(@NonNull CollectFormInstance instance) throws Exception {
                        return Observable.just(value);
                    }
                });
            }
        });
    }

    @SuppressWarnings("MethodOnlyUsedFromInnerClass")
    private <T> ObservableSource<T> rxSaveErrorInstance(final CollectFormInstance instance, final Throwable throwable) {
        return cacheWordDataSource.getDataSource().flatMap(new Function<DataSource, ObservableSource<T>>() {
            @Override
            public ObservableSource<T> apply(@NonNull final DataSource dataSource) throws Exception {
                return dataSource.saveInstance(instance).toObservable().flatMap(new Function<CollectFormInstance, ObservableSource<T>>() {
                    @Override
                    public ObservableSource<T> apply(@NonNull CollectFormInstance instance) throws Exception {
                        return Observable.error(throwable);
                    }
                });
            }
        });
    }
}
