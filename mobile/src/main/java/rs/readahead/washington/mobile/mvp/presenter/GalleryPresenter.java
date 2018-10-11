package rs.readahead.washington.mobile.mvp.presenter;

import android.content.Context;
import android.net.Uri;

import com.crashlytics.android.Crashlytics;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import io.reactivex.Observable;
import io.reactivex.Single;
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
import rs.readahead.washington.mobile.domain.entity.MediaFile;
import rs.readahead.washington.mobile.domain.repository.IMediaFileRecordRepository;
import rs.readahead.washington.mobile.media.MediaFileBundle;
import rs.readahead.washington.mobile.media.MediaFileHandler;
import rs.readahead.washington.mobile.mvp.contract.IGalleryPresenterContract;
import rs.readahead.washington.mobile.presentation.entity.MediaFileThumbnailData;


public class GalleryPresenter implements IGalleryPresenterContract.IPresenter {
    private IGalleryPresenterContract.IView view;
    private CompositeDisposable disposables = new CompositeDisposable();
    private CacheWordDataSource cacheWordDataSource;
    private MediaFileHandler mediaFileHandler;


    public GalleryPresenter(IGalleryPresenterContract.IView view) {
        this.view = view;
        this.cacheWordDataSource = new CacheWordDataSource(view.getContext());
        this.mediaFileHandler = new MediaFileHandler(cacheWordDataSource);
    }

    @Override
    public void getFiles(final IMediaFileRecordRepository.Filter filter, final IMediaFileRecordRepository.Sort sort) {
        disposables.add(
                cacheWordDataSource.getDataSource().flatMapSingle(new Function<DataSource, SingleSource<List<MediaFile>>>() {
                    @Override
                    public SingleSource<List<MediaFile>> apply(@NonNull DataSource dataSource) throws Exception {
                        return dataSource.listMediaFiles(filter, sort);
                    }
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe(new Consumer<Disposable>() {
                    @Override
                    public void accept(@NonNull Disposable disposable) throws Exception {
                        view.onGetFilesStart();
                    }
                })
                .doFinally(new Action() {
                    @Override
                    public void run() throws Exception {
                        view.onGetFilesEnd();
                    }
                })
                .subscribe(new Consumer<List<MediaFile>>() {
                    @Override
                    public void accept(@NonNull List<MediaFile> mediaFiles) throws Exception {
                        //checkMediaFolder(view.getContext(), mediaFiles);
                        view.onGetFilesSuccess(mediaFiles);
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(@NonNull Throwable throwable) throws Exception {
                        view.onGetFilesError(throwable);
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

    @Override
    public void addNewMediaFile(MediaFile mediaFile, MediaFileThumbnailData thumbnailData) {
        disposables.add(mediaFileHandler.registerMediaFile(mediaFile, thumbnailData)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<MediaFile>() {
                    @Override
                    public void accept(@NonNull MediaFile mediaFile) throws Exception {
                        view.onMediaFilesAdded(mediaFile);
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(@NonNull Throwable throwable) throws Exception {
                        view.onMediaFilesAddingError(throwable);
                    }
                })
        );
    }

    @Override
    public void deleteMediaFiles(final List<MediaFile> mediaFiles) {
        disposables.add(cacheWordDataSource.getDataSource()
                .subscribeOn(Schedulers.io())
                .flatMapSingle(new Function<DataSource, SingleSource<Integer>>() {
                    @Override
                    public SingleSource<Integer> apply(@NonNull DataSource dataSource) throws Exception {
                        List<Single<MediaFile>> completables = new ArrayList<>();
                        for (MediaFile mediafile: mediaFiles) {
                            completables.add(dataSource.deleteMediaFile(mediafile, new IMediaFileRecordRepository.IMediaFileDeleter() {
                                @Override
                                public boolean delete(MediaFile mediaFile) {
                                    return MediaFileHandler.deleteMediaFile(view.getContext(), mediaFile);
                                }
                            }));
                        }
                        
                        return Single.zip(completables, new Function<Object[], Integer>() {
                            @Override
                            public Integer apply(@NonNull Object[] objects) throws Exception {
                                return objects.length;
                            }
                        });
                    }
                })
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<Integer>() {
                    @Override
                    public void accept(Integer num) throws Exception {
                        view.onMediaFilesDeleted(num);
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) throws Exception {
                        Crashlytics.logException(throwable);
                        view.onMediaFilesDeletionError(throwable);
                    }
                })
        );
    }

    @Override
    public void exportMediaFiles(final List<MediaFile> mediaFiles) {
        final Context context = view.getContext().getApplicationContext();

        disposables.add(Single
                .fromCallable(new Callable<Integer>() {
                    @Override
                    public Integer call() throws Exception {
                        for (MediaFile mediaFile: mediaFiles) {
                            MediaFileHandler.exportMediaFile(context, mediaFile);
                        }

                        return mediaFiles.size();
                    }
                })
                .subscribeOn(Schedulers.computation())
                .doOnSubscribe(new Consumer<Disposable>() {
                    @Override
                    public void accept(Disposable disposable) throws Exception {
                        view.onExportStarted();
                    }
                })
                .observeOn(AndroidSchedulers.mainThread())
                .doFinally(new Action() {
                    @Override
                    public void run() throws Exception {
                        view.onExportEnded();
                    }
                })
                .subscribe(new Consumer<Integer>() {
                    @Override
                    public void accept(Integer num) throws Exception {
                        view.onMediaExported(num);
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) throws Exception {
                        Crashlytics.logException(throwable);
                        view.onExportError(throwable);
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

    /*@SuppressWarnings("MethodOnlyUsedFromInnerClass")
    private void checkMediaFolder(Context context, List<MediaFile> mediaFilesFromDb) {
        //List<String> mediaFilesFromDir = listMediaFiles(context);
        // todo: check in separate thread that disk and db are in sync..
        // todo: if not, call getFiles() (?)
    }*/

    /*private List<String> listMediaFiles(Context context) {
        File mediaPath = new File(context.getFilesDir(), C.MEDIA_DIR);
        File[] files = mediaPath.listFiles();
        List<String> fileList = new ArrayList<>(files.length);

        for (File file: files) {
            if (! file.isDirectory()) {
                fileList.add(file.getName());
            }
        }

        return fileList;
    }*/
}
