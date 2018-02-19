package rs.readahead.washington.mobile.mvp.presenter;

import java.util.List;

import io.reactivex.SingleSource;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.annotations.NonNull;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;
import rs.readahead.washington.mobile.data.database.CacheWordDataSource;
import rs.readahead.washington.mobile.data.database.DataSource;
import rs.readahead.washington.mobile.domain.entity.MediaFile;
import rs.readahead.washington.mobile.domain.exception.NotFountException;
import rs.readahead.washington.mobile.mvp.contract.IAudioPlayPresenterContract;


public class AudioPlayPresenter implements
        IAudioPlayPresenterContract.IPresenter {
    private IAudioPlayPresenterContract.IView view;
    private CacheWordDataSource cacheWordDataSource;
    private CompositeDisposable disposables = new CompositeDisposable();


    public AudioPlayPresenter(IAudioPlayPresenterContract.IView view) {
        this.view = view;
        this.cacheWordDataSource = new CacheWordDataSource(view.getContext());
    }

    @Override
    public void getMediaFile(final long id) {
        disposables.add(cacheWordDataSource.getDataSource()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .flatMapSingle(new Function<DataSource, SingleSource<List<MediaFile>>>() {
                    @Override
                    public SingleSource<List<MediaFile>> apply(@NonNull DataSource dataSource) throws Exception {
                        return dataSource.getMediaFiles(new long[]{id});
                    }
                })
                .subscribe(new Consumer<List<MediaFile>>() {
                    @Override
                    public void accept(List<MediaFile> mediaFiles) throws Exception {
                        if (mediaFiles.size() != 1) {
                            view.onMediaFileError(new NotFountException());
                        } else {
                            view.onMediaFileSuccess(mediaFiles.get(0));
                        }
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) throws Exception {
                        view.onMediaFileError(throwable);
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
}
