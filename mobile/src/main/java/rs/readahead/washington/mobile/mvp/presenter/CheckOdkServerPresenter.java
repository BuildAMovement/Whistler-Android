package rs.readahead.washington.mobile.mvp.presenter;

import com.crashlytics.android.Crashlytics;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.annotations.NonNull;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Action;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;
import rs.readahead.washington.mobile.MyApplication;
import rs.readahead.washington.mobile.data.repository.OpenRosaRepository;
import rs.readahead.washington.mobile.domain.entity.IErrorBundle;
import rs.readahead.washington.mobile.domain.entity.collect.CollectServer;
import rs.readahead.washington.mobile.domain.entity.collect.ListFormResult;
import rs.readahead.washington.mobile.domain.repository.IOpenRosaRepository;
import rs.readahead.washington.mobile.mvp.contract.ICheckOdkServerContract;


public class CheckOdkServerPresenter implements
        ICheckOdkServerContract.IPresenter {
    private IOpenRosaRepository odkRepository;
    private ICheckOdkServerContract.IView view;
    private CompositeDisposable disposables = new CompositeDisposable();
    private boolean saveAnyway = false;


    public CheckOdkServerPresenter(ICheckOdkServerContract.IView view) {
        this.odkRepository = new OpenRosaRepository();
        this.view = view;
    }

    @Override
    public void checkServer(final CollectServer server, boolean connectionRequired) {
        if (! MyApplication.isConnectedToInternet(view.getContext())) {
            if (saveAnyway && !connectionRequired) {
                view.onServerCheckSuccess(server);
            } else {
                view.onNoConnectionAvailable();
                setSaveAnyway(true);
            }
            return;
        } else {
            if (saveAnyway) {
                setSaveAnyway(false);
            }
        }

        disposables.add(odkRepository.formList(server)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe(new Consumer<Disposable>() {
                    @Override
                    public void accept(@NonNull Disposable disposable) throws Exception {
                        view.showServerCheckLoading();
                    }
                })
                .doFinally(new Action() {
                    @Override
                    public void run() throws Exception {
                        view.hideServerCheckLoading();
                    }
                })
                .subscribe(new Consumer<ListFormResult>() {
                    @Override
                    public void accept(@NonNull ListFormResult listFormResult) throws Exception {
                        if (listFormResult.getErrors().size() > 0) {
                            IErrorBundle errorBundle = listFormResult.getErrors().get(0);
                            view.onServerCheckFailure(errorBundle);
                        } else {
                            view.onServerCheckSuccess(server);
                        }
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(@NonNull Throwable throwable) throws Exception {
                        Crashlytics.logException(throwable);
                        view.onServerCheckError(throwable);
                    }
                })
        );
    }

    @Override
    public void destroy() {
        disposables.dispose();
        view = null;
    }

    private void setSaveAnyway(boolean enable) {
        saveAnyway = enable;
        view.setSaveAnyway(enable);
    }
}
