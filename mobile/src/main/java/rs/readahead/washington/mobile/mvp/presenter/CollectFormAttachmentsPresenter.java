package rs.readahead.washington.mobile.mvp.presenter;

import android.net.Uri;

import com.crashlytics.android.Crashlytics;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Callable;

import io.reactivex.Observable;
import io.reactivex.SingleSource;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.annotations.NonNull;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Action;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;
import rs.readahead.washington.mobile.MyApplication;
import rs.readahead.washington.mobile.bus.event.FormAttachmentsUpdatedEvent;
import rs.readahead.washington.mobile.data.database.CacheWordDataSource;
import rs.readahead.washington.mobile.data.database.DataSource;
import rs.readahead.washington.mobile.domain.entity.MediaFile;
import rs.readahead.washington.mobile.domain.entity.collect.CollectFormInstance;
import rs.readahead.washington.mobile.media.MediaFileBundle;
import rs.readahead.washington.mobile.media.MediaFileHandler;
import rs.readahead.washington.mobile.mvp.contract.ICollectFormAttachmentsPresenterContract;
import rs.readahead.washington.mobile.odk.FormController;
import rs.readahead.washington.mobile.presentation.entity.MediaFileThumbnailData;


public class CollectFormAttachmentsPresenter implements
        ICollectFormAttachmentsPresenterContract.IPresenter {
    private ICollectFormAttachmentsPresenterContract.IView view;
    private MediaFileHandler mediaFileHandler;
    private CacheWordDataSource cacheWordDataSource;
    private CompositeDisposable disposables = new CompositeDisposable();


    public CollectFormAttachmentsPresenter(ICollectFormAttachmentsPresenterContract.IView view) {
        this.view = view;
        this.cacheWordDataSource = new CacheWordDataSource(view.getContext());
        this.mediaFileHandler = new MediaFileHandler(cacheWordDataSource);
    }

    @Override
    public void getActiveFormInstanceAttachments() {
        view.onActiveFormInstanceMediaFiles(FormController.getActive().getCollectFormInstance().getMediaFiles());
    }

    @Override
    public void attachNewMediaFile(MediaFile mediaFile, MediaFileThumbnailData thumbnailData) {
        disposables.add(mediaFileHandler.registerMediaFile(mediaFile, thumbnailData)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<MediaFile>() {
                    @Override
                    public void accept(@NonNull MediaFile mediaFile) throws Exception {
                        if (FormController.getActive().getCollectFormInstance().addMediaFile(mediaFile)) {
                            MyApplication.bus().post(new FormAttachmentsUpdatedEvent());
                            view.onMediaFilesAttached(Collections.singletonList(mediaFile));
                        }
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(@NonNull Throwable throwable) throws Exception {
                        view.onMediaFilesAttachedError(throwable);
                    }
                })
        );
    }

    @Override
    public void attachRegisteredMediaFiles(final long[] ids) {
        disposables.add(cacheWordDataSource.getDataSource()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .flatMapSingle(new Function<DataSource, SingleSource<List<MediaFile>>>() {
                    @Override
                    public SingleSource<List<MediaFile>> apply(@NonNull DataSource dataSource) throws Exception {
                        return dataSource.getMediaFiles(ids);
                    }
                })
                .subscribe(new Consumer<List<MediaFile>>() {
                    @Override
                    public void accept(List<MediaFile> mediaFiles) throws Exception {
                        final CollectFormInstance instance = FormController.getActive().getCollectFormInstance();

                        for (Iterator<MediaFile> iterator = mediaFiles.iterator(); iterator.hasNext();) {
                            MediaFile mediaFile = iterator.next();
                            if (! instance.addMediaFile(mediaFile)) {
                                iterator.remove();
                            }
                        }

                        if (! mediaFiles.isEmpty()) {
                            MyApplication.bus().post(new FormAttachmentsUpdatedEvent());
                            view.onMediaFilesAttached(mediaFiles);
                        }
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) throws Exception {
                        view.onMediaFilesAttachedError(throwable);
                    }
                })
        );
    }

    @Override
    public void importImage(final Uri uri) {
        disposables.add(Observable
                .fromCallable(new Callable<MediaFileBundle>() {
                    @Override
                    public MediaFileBundle call() throws Exception {
                        return MediaFileHandler.importPhotoUri(view.getContext(), uri);
                    }
                })
                .subscribeOn(Schedulers.computation())
                .doOnSubscribe(new Consumer<Disposable>() {
                    @Override
                    public void accept(Disposable disposable) throws Exception {
                        view.onImportStarted();
                    }
                })
                .observeOn(AndroidSchedulers.mainThread())
                .doFinally(new Action() {
                    @Override
                    public void run() throws Exception {
                        view.onImportEnded();
                    }
                })
                .subscribe(new Consumer<MediaFileBundle>() {
                    @Override
                    public void accept(MediaFileBundle mediaHolder) throws Exception {
                        view.onMediaImported(mediaHolder.getMediaFile(), mediaHolder.getMediaFileThumbnailData());
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) throws Exception {
                        Crashlytics.logException(throwable);
                        view.onImportError(throwable);
                    }
                })
        );
    }

    @Override
    public void importVideo(final Uri uri) {
        disposables.add(Observable
                .fromCallable(new Callable<MediaFileBundle>() {
                    @Override
                    public MediaFileBundle call() throws Exception {
                        return MediaFileHandler.importVideoUri(view.getContext(), uri);
                    }
                })
                .subscribeOn(Schedulers.computation())
                .doOnSubscribe(new Consumer<Disposable>() {
                    @Override
                    public void accept(Disposable disposable) throws Exception {
                        view.onImportStarted();
                    }
                })
                .observeOn(AndroidSchedulers.mainThread())
                .doFinally(new Action() {
                    @Override
                    public void run() throws Exception {
                        view.onImportEnded();
                    }
                })
                .subscribe(new Consumer<MediaFileBundle>() {
                    @Override
                    public void accept(MediaFileBundle mediaHolder) throws Exception {
                        view.onMediaImported(mediaHolder.getMediaFile(), mediaHolder.getMediaFileThumbnailData());
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) throws Exception {
                        Crashlytics.logException(throwable);
                        view.onImportError(throwable);
                    }
                })
        );
    }

    /*@Override
    public void encryptTmpVideo(final Uri uri) {
        disposables.add(Observable
                .fromCallable(new Callable<MediaFileBundle>() {
                    @Override
                    public MediaFileBundle call() throws Exception {
                        MediaFileBundle mediaFileBundle = MediaFileHandler.importVideoUri(view.getContext(), uri);
                        view.getContext().getContentResolver().delete(uri, null, null);
                        PermissionUtil.revokeUriPermissions(view.getContext(), uri);

                        return mediaFileBundle;
                    }
                })
                .subscribeOn(Schedulers.computation())
                .doOnSubscribe(new Consumer<Disposable>() {
                    @Override
                    public void accept(Disposable disposable) throws Exception {
                        view.onImportStarted(); // share this for now..
                    }
                })
                .observeOn(AndroidSchedulers.mainThread())
                .doFinally(new Action() {
                    @Override
                    public void run() throws Exception {
                        view.onImportEnded(); // share this for now..
                    }
                })
                .subscribe(new Consumer<MediaFileBundle>() {
                    @Override
                    public void accept(MediaFileBundle mediaFileBundle) throws Exception {
                        view.onTmpVideoEncrypted(mediaFileBundle);
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) throws Exception {
                        Crashlytics.logException(throwable);
                        view.onTmpVideoEncryptionError(throwable);
                    }
                })
        );
    }*/

    @Override
    public void destroy() {
        cacheWordDataSource.dispose();
        disposables.dispose();
        view = null;
    }
}
