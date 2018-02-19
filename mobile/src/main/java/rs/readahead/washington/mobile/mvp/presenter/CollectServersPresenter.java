package rs.readahead.washington.mobile.mvp.presenter;

import com.crashlytics.android.Crashlytics;

import java.util.List;

import io.reactivex.CompletableSource;
import io.reactivex.SingleSource;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.annotations.NonNull;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Action;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;
import rs.readahead.washington.mobile.data.database.CacheWordDataSource;
import rs.readahead.washington.mobile.data.database.DataSource;
import rs.readahead.washington.mobile.data.openrosa.OpenRosaService;
import rs.readahead.washington.mobile.domain.entity.collect.CollectServer;
import rs.readahead.washington.mobile.mvp.contract.ICollectServersPresenterContract;


public class CollectServersPresenter implements ICollectServersPresenterContract.IPresenter {
    private CacheWordDataSource cacheWordDataSource;
    private ICollectServersPresenterContract.IView view;
    private CompositeDisposable disposables = new CompositeDisposable();


    //@Inject
    public CollectServersPresenter(ICollectServersPresenterContract.IView view) {
        cacheWordDataSource = new CacheWordDataSource(view.getContext().getApplicationContext());
        this.view = view;
    }

    public void getServers() {
        disposables.add(cacheWordDataSource.getDataSource()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe(new Consumer<Disposable>() {
                    @Override
                    public void accept(@NonNull Disposable disposable) throws Exception {
                        view.showLoading();
                    }
                })
                .flatMapSingle(new Function<DataSource, SingleSource<List<CollectServer>>>() {
                    @Override
                    public SingleSource<List<CollectServer>> apply(DataSource dataSource) throws Exception {
                        return dataSource.listCollectServers();
                    }
                })
                .doFinally(new Action() {
                    @Override
                    public void run() throws Exception {
                        view.hideLoading();
                    }
                })
                .subscribe(new Consumer<List<CollectServer>>() {
                               @Override
                               public void accept(@NonNull List<CollectServer> list) throws Exception {
                                   view.onServersLoaded(list);
                               }
                           },
                        new Consumer<Throwable>() {
                            @Override
                            public void accept(@NonNull Throwable throwable) throws Exception {
                                Crashlytics.logException(throwable);
                                view.onLoadServersError(throwable);
                            }
                        })
        );
    }

    public void create(final CollectServer server) {
        disposables.add(cacheWordDataSource.getDataSource()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe(new Consumer<Disposable>() {
                    @Override
                    public void accept(@NonNull Disposable disposable) throws Exception {
                        view.showLoading();
                    }
                })
                .flatMapSingle(new Function<DataSource, SingleSource<CollectServer>>() {
                    @Override
                    public SingleSource<CollectServer> apply(DataSource dataSource) throws Exception {
                        return dataSource.createCollectServer(server);
                    }
                })
                .doFinally(new Action() {
                    @Override
                    public void run() throws Exception {
                        view.hideLoading();
                    }
                })
                .subscribe(new Consumer<CollectServer>() {
                               @Override
                               public void accept(@NonNull CollectServer server) throws Exception {
                                   view.onCreatedServer(server);
                               }
                           },
                        new Consumer<Throwable>() {
                            @Override
                            public void accept(@NonNull Throwable throwable) throws Exception {
                                Crashlytics.logException(throwable);
                                view.onCreateServerError(throwable);
                            }
                        })
        );
    }

    public void update(final CollectServer server) {
        disposables.add(cacheWordDataSource.getDataSource()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe(new Consumer<Disposable>() {
                    @Override
                    public void accept(@NonNull Disposable disposable) throws Exception {
                        view.showLoading();
                    }
                })
                .flatMapSingle(new Function<DataSource, SingleSource<CollectServer>>() {
                    @Override
                    public SingleSource<CollectServer> apply(DataSource dataSource) throws Exception {
                        return dataSource.updateCollectServer(server);
                    }
                })
                .doFinally(new Action() {
                    @Override
                    public void run() throws Exception {
                        view.hideLoading();
                    }
                })
                .subscribe(new Consumer<CollectServer>() {
                               @Override
                               public void accept(@NonNull CollectServer server) throws Exception {
                                   OpenRosaService.clearCache();
                                   view.onUpdatedServer(server);
                               }
                           },
                        new Consumer<Throwable>() {
                            @Override
                            public void accept(@NonNull Throwable throwable) throws Exception {
                                Crashlytics.logException(throwable);
                                view.onUpdateServerError(throwable);
                            }
                        })
        );
    }

    public void remove(final CollectServer server) {
        disposables.add(cacheWordDataSource.getDataSource()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe(new Consumer<Disposable>() {
                    @Override
                    public void accept(@NonNull Disposable disposable) throws Exception {
                        view.showLoading();
                    }
                })
                .flatMapCompletable(new Function<DataSource, CompletableSource>() {
                    @Override
                    public CompletableSource apply(DataSource dataSource) throws Exception {
                        return dataSource.removeCollectServer(server.getId());
                    }
                })
                .doFinally(new Action() {
                            @Override
                            public void run() throws Exception {
                                view.hideLoading();
                            }
                        })
                .subscribe(new Action() {
                               @Override
                               public void run() throws Exception {
                                   OpenRosaService.clearCache();
                                   view.onRemovedServer(server);
                               }
                           },
                        new Consumer<Throwable>() {
                            @Override
                            public void accept(@NonNull Throwable throwable) throws Exception {
                                Crashlytics.logException(throwable);
                                view.onRemoveServerError(throwable);
                            }
                        })
        );
    }

    @Override
    public void destroy() {
        disposables.dispose();
        cacheWordDataSource.dispose();
        view = null;
    }
}
