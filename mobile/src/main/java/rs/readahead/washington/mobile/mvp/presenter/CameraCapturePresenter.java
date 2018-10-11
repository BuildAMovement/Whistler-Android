package rs.readahead.washington.mobile.mvp.presenter;

import android.graphics.Bitmap;

import com.crashlytics.android.Crashlytics;

import java.io.File;
import java.util.concurrent.Callable;

import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Action;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;
import rs.readahead.washington.mobile.data.database.CacheWordDataSource;
import rs.readahead.washington.mobile.domain.entity.MediaFile;
import rs.readahead.washington.mobile.media.MediaFileBundle;
import rs.readahead.washington.mobile.media.MediaFileHandler;
import rs.readahead.washington.mobile.mvp.contract.ICameraCapturePresenterContract;


public class CameraCapturePresenter implements ICameraCapturePresenterContract.IPresenter {
    private ICameraCapturePresenterContract.IView view;
    private CompositeDisposable disposables = new CompositeDisposable();
    private CacheWordDataSource cacheWordDataSource;
    private MediaFileHandler mediaFileHandler;
    private int currentRotation = 0;


    public CameraCapturePresenter(ICameraCapturePresenterContract.IView view) {
        this.view = view;
        this.cacheWordDataSource = new CacheWordDataSource(view.getContext());
        this.mediaFileHandler = new MediaFileHandler(cacheWordDataSource);
    }

    @Override
    public void addJpegPhoto(final byte[] jpeg, final Bitmap thumb) {
        disposables.add(Observable.fromCallable(new Callable<MediaFileBundle>() {
                    @Override
                    public MediaFileBundle call() throws Exception {
                        return MediaFileHandler.saveJpegPhoto(view.getContext(), jpeg, thumb);
                    }
                })
                .flatMap(new Function<MediaFileBundle, ObservableSource<MediaFile>>() {
                    @Override
                    public ObservableSource<MediaFile> apply(MediaFileBundle bundle) throws Exception {
                        return mediaFileHandler.registerMediaFile(bundle.getMediaFile(),
                                bundle.getMediaFileThumbnailData());
                    }
                })
                .subscribeOn(Schedulers.io())
                .doOnSubscribe(new Consumer<Disposable>() {
                    @Override
                    public void accept(Disposable disposable) throws Exception {
                        view.onAddingStart();
                    }
                })
                .observeOn(AndroidSchedulers.mainThread())
                .doFinally(new Action() {
                    @Override
                    public void run() throws Exception {
                        view.onAddingEnd();
                    }
                })
                .subscribe(new Consumer<MediaFile>() {
                    @Override
                    public void accept(MediaFile mediaFile) throws Exception {
                        view.onAddSuccess(mediaFile.getId());
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) throws Exception {
                        Crashlytics.logException(throwable);
                        view.onAddError(throwable);
                    }
                })
        );
    }

    @Override
    public void addMp4Video(final File file) {
        disposables.add(Observable.fromCallable(new Callable<MediaFileBundle>() {
                    @Override
                    public MediaFileBundle call() throws Exception {
                        return MediaFileHandler.saveMp4Video(view.getContext(), file);
                    }
                })
                .flatMap(new Function<MediaFileBundle, ObservableSource<MediaFile>>() {
                     @Override
                     public ObservableSource<MediaFile> apply(MediaFileBundle bundle) throws Exception {
                         return mediaFileHandler.registerMediaFile(bundle.getMediaFile(),
                                 bundle.getMediaFileThumbnailData());
                     }
                 })
                .subscribeOn(Schedulers.io())
                .doOnSubscribe(new Consumer<Disposable>() {
                    @Override
                    public void accept(Disposable disposable) throws Exception {
                        view.onAddingStart();
                    }
                })
                .observeOn(AndroidSchedulers.mainThread())
                .doFinally(new Action() {
                    @Override
                    public void run() throws Exception {
                        view.onAddingEnd();
                    }
                })
                .subscribe(new Consumer<MediaFile>() {
                    @Override
                    public void accept(MediaFile mediaFile) throws Exception {
                        view.onAddSuccess(mediaFile.getId());
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) throws Exception {
                        Crashlytics.logException(throwable);
                        view.onAddError(throwable);
                    }
                })
        );
    }

    @Override
    public void getVideoThumb(final File file) {
        disposables.add(Observable.fromCallable(new Callable<Bitmap>() {
                    @Override
                    public Bitmap call() throws Exception {
                        return MediaFileHandler.getVideoBitmapThumb(file);
                    }
                })
                .subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<Bitmap>() {
                    @Override
                    public void accept(Bitmap thumb) throws Exception {
                        view.onVideoThumbSuccess(thumb);
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) throws Exception {
                        Crashlytics.logException(throwable);
                        view.onVideoThumbError(throwable);
                    }
                })
        );
    }

    @Override
    public void handleRotation(int orientation) {
        int degrees = 270;

        if (orientation < 45 || orientation > 315) {
            degrees = 0;
        } else if (orientation < 135) {
            degrees = 90;
        } else if (orientation < 225) {
            degrees = 180;
        }

        int rotation = (360 - degrees) % 360;

        if (rotation == 270) {
            rotation = -90;
        }

        if (currentRotation == rotation || rotation == 180/*IGNORING THIS ANGLE*/) {
            return;
        }

        currentRotation = rotation;

        view.rotateViews(rotation);
    }

    @Override
    public void destroy() {
        disposables.dispose();
        cacheWordDataSource.dispose();
        view = null;
    }
}
