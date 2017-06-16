package rs.readahead.washington.mobile.domain.interactor;

import android.support.annotation.NonNull;

import io.reactivex.Observable;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.observers.DisposableObserver;


public abstract class BaseUseCase<T, Params> {
    private final CompositeDisposable disposables;


    BaseUseCase() {
        this.disposables = new CompositeDisposable();
    }

    abstract Observable<T> observeUseCase(Params params);

    public void execute(DisposableObserver<T> observer, Params params) {
        final Observable<T> observable = this.observeUseCase(params);

        addDisposable(observable.subscribeWith(observer));
    }

    public void dispose() {
        if (! disposables.isDisposed()) {
            disposables.dispose();
        }
    }

    private void addDisposable(@NonNull Disposable disposable) {
        disposables.add(disposable);
    }
}
