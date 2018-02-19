package rs.readahead.washington.mobile.mvp.presenter;

import com.crashlytics.android.Crashlytics;

import java.util.List;

import io.reactivex.CompletableSource;
import io.reactivex.ObservableSource;
import io.reactivex.SingleSource;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.annotations.NonNull;
import io.reactivex.annotations.Nullable;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Action;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;
import rs.readahead.washington.mobile.data.repository.TrainRepository;
import rs.readahead.washington.mobile.data.database.CacheWordDataSource;
import rs.readahead.washington.mobile.data.database.DataSource;
import rs.readahead.washington.mobile.domain.entity.IErrorBundle;
import rs.readahead.washington.mobile.domain.entity.TrainModule;
import rs.readahead.washington.mobile.domain.repository.ITrainRepository;
import rs.readahead.washington.mobile.mvp.contract.ITrainListPresenterContract;
import rs.readahead.washington.mobile.util.TrainModuleHandler;
import rs.readahead.washington.mobile.util.jobs.TrainModuleDownloadJob;


public class TrainListPresenter implements ITrainListPresenterContract.IPresenter {
    private ITrainListPresenterContract.IView view;
    private ITrainRepository trainRepository;
    private CacheWordDataSource cacheWordDataSource;
    private CompositeDisposable disposables;


    public TrainListPresenter(ITrainListPresenterContract.IView view) {
        this.view = view;
        disposables = new CompositeDisposable();
        trainRepository = new TrainRepository();
        cacheWordDataSource = new CacheWordDataSource(view.getContext());
    }

    @Override
    public void getModules() {
        disposables.add(trainRepository.getModules()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe(new Consumer<Disposable>() {
                    @Override
                    public void accept(Disposable disposable) throws Exception {
                        view.onTrainModulesStarted();
                    }
                })
                .flatMapObservable(new Function<List<TrainModule>, ObservableSource<List<TrainModule>>>() {
                    @Override
                    public ObservableSource<List<TrainModule>> apply(final List<TrainModule> trainModules) throws Exception {
                        return cacheWordDataSource.getDataSource().flatMap(new Function<DataSource, ObservableSource<List<TrainModule>>>() {
                            @Override
                            public ObservableSource<List<TrainModule>> apply(DataSource dataSource) throws Exception {
                                return dataSource.updateTrainDBModules(trainModules).toObservable();
                            }
                        });
                    }
                })
                .doFinally(new Action() {
                    @Override
                    public void run() throws Exception {
                        view.onTrainModulesEnded();
                    }
                })
                .subscribe(new Consumer<List<TrainModule>>() {
                    @Override
                    public void accept(List<TrainModule> trainModules) throws Exception {
                        view.onTrainModulesSuccess(trainModules);
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) throws Exception {
                        Crashlytics.logException(throwable);
                        view.onTrainModulesError((IErrorBundle) throwable);
                    }
                })
        );
    }

    @Override
    public void searchModules(String ident) {
        disposables.add(trainRepository.searchModules(ident)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe(new Consumer<Disposable>() {
                    @Override
                    public void accept(Disposable disposable) throws Exception {
                        view.onTrainModulesStarted();
                    }
                })
                .doFinally(new Action() {
                    @Override
                    public void run() throws Exception {
                        view.onTrainModulesEnded();
                    }
                })
                .subscribe(new Consumer<List<TrainModule>>() {
                    @Override
                    public void accept(List<TrainModule> trainModules) throws Exception {
                        view.onTrainModulesSuccess(trainModules);
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) throws Exception {
                        Crashlytics.logException(throwable);
                        view.onTrainModulesError((IErrorBundle) throwable);
                    }
                })
        );
    }

    @Override
    public void listModules() {
        disposables.add(cacheWordDataSource.getDataSource()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .flatMap(new Function<DataSource, ObservableSource<List<TrainModule>>>() {
                    @Override
                    public ObservableSource<List<TrainModule>> apply(@NonNull DataSource dataSource) throws Exception {
                        return dataSource.listTrainModules().toObservable();
                    }
                })
                .subscribe(new Consumer<List<TrainModule>>() {
                    @Override
                    public void accept(@NonNull List<TrainModule> modules) throws Exception {
                        view.onTrainModulesSuccess(modules);
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(@NonNull Throwable throwable) throws Exception {
                        Crashlytics.logException(throwable);
                        view.onTrainModulesError((IErrorBundle) throwable);
                    }
                })
        );
    }

    @Override
    public void downloadTrainModule(final TrainModule module) {
        disposables.add(cacheWordDataSource.getDataSource()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .flatMapCompletable(new Function<DataSource, CompletableSource>() {
                    @Override
                    public CompletableSource apply(DataSource dataSource) throws Exception {
                        return dataSource.startDownloadTrainModule(module);
                    }
                }).subscribe(new Action() {
                    @Override
                    public void run() throws Exception {
                        TrainModuleDownloadJob.scheduleJob();
                        view.onTrainModuleDownloadStarted(module);
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) throws Exception {
                        Crashlytics.logException(throwable);
                        view.onTrainModuleDownloadError(throwable);
                    }
                })
        );
    }

    @Override
    public void removeTrainModule(final TrainModule module) {
        disposables.add(cacheWordDataSource.getDataSource()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .flatMapCompletable(new Function<DataSource, CompletableSource>() {
                    @Override
                    public CompletableSource apply(DataSource dataSource) throws Exception {
                        return dataSource.removeTrainDBModule(module.getId());
                    }
                })
                .subscribe(new Action() {
                    @Override
                    public void run() throws Exception {
                        // there is no way now to cancel download of training module
                        // if delete is added while downloading, download has to be stopped first
                        TrainModuleHandler.removeModuleFiles(view.getContext(), module);
                        view.onTrainModuleRemoved(module);
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) throws Exception {
                        Crashlytics.logException(throwable);
                        view.onTrainModuleRemoveError(throwable);
                    }
                })
        );
    }

    @Override
    @Nullable
    public TrainModule getLocalTrainModule(final long id) {
        TrainModule module = cacheWordDataSource.getDataSource()
                .subscribeOn(Schedulers.io())
                .flatMapSingle(new Function<DataSource, SingleSource<TrainModule>>() {
                    @Override
                    public SingleSource<TrainModule> apply(DataSource dataSource) throws Exception {
                        return dataSource.getTrainModule(id);
                    }
                })
                .blockingSingle();

        return TrainModule.NONE.equals(module) ? null : module;
    }

    @Override
    public void destroy() {
        cacheWordDataSource.dispose();
        disposables.dispose();
        view = null;
    }
}
