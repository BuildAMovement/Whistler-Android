package rs.readahead.washington.mobile.mvp.presenter;

import java.util.List;

import info.guardianproject.cacheword.CacheWordHandler;
import info.guardianproject.cacheword.ICacheWordSubscriber;
import io.reactivex.SingleSource;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.annotations.NonNull;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.AsyncSubject;
import rs.readahead.washington.mobile.data.database.DataSource;
import rs.readahead.washington.mobile.domain.entity.collect.CollectFormInstance;
import rs.readahead.washington.mobile.mvp.contract.ICollectFormInstanceListPresenterContract;


public class CollectFormInstanceListPresenter implements
        ICollectFormInstanceListPresenterContract.IPresenter,
        ICacheWordSubscriber {
    private ICollectFormInstanceListPresenterContract.IView view;
    private CacheWordHandler cacheWordHandler;
    private CompositeDisposable disposables = new CompositeDisposable();
    private AsyncSubject<DataSource> asyncDataSource = AsyncSubject.create();


    public CollectFormInstanceListPresenter(ICollectFormInstanceListPresenterContract.IView view) {
        this.view = view;
        cacheWordHandler = new CacheWordHandler(view.getContext().getApplicationContext(), this);
        cacheWordHandler.connectToService();
    }

    @Override
    public void listDraftFormInstances() {
        disposables.add(asyncDataSource
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .flatMapSingle(new Function<DataSource, SingleSource<List<CollectFormInstance>>>() {
                    @Override
                    public SingleSource<List<CollectFormInstance>> apply(@NonNull DataSource dataSource) throws Exception {
                        return dataSource.listDraftForms();
                    }
                })
                .subscribe(new Consumer<List<CollectFormInstance>>() {
                    @Override
                    public void accept(@NonNull List<CollectFormInstance> forms) throws Exception {
                        view.onFormInstanceListSuccess(forms);
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(@NonNull Throwable throwable) throws Exception {
                        view.onFormInstanceListError(throwable);
                    }
                })
        );
    }

    @Override
    public void listSubmitFormInstances() {
        disposables.add(asyncDataSource
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .flatMapSingle(new Function<DataSource, SingleSource<List<CollectFormInstance>>>() {
                    @Override
                    public SingleSource<List<CollectFormInstance>> apply(@NonNull DataSource dataSource) throws Exception {
                        return dataSource.listSentForms();
                    }
                })
                .subscribe(new Consumer<List<CollectFormInstance>>() {
                    @Override
                    public void accept(@NonNull List<CollectFormInstance> forms) throws Exception {
                        view.onFormInstanceListSuccess(forms);
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(@NonNull Throwable throwable) throws Exception {
                        view.onFormInstanceListError(throwable);
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
}
