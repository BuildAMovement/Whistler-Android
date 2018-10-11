
package rs.readahead.washington.mobile.mvp.presenter;

import android.net.Uri;

import com.crashlytics.android.Crashlytics;

import java.util.ArrayList;
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
import rs.readahead.washington.mobile.data.database.CacheWordDataSource;
import rs.readahead.washington.mobile.data.database.DataSource;
import rs.readahead.washington.mobile.domain.entity.MediaFile;
import rs.readahead.washington.mobile.domain.repository.IMediaFileRecordRepository;
import rs.readahead.washington.mobile.media.MediaFileBundle;
import rs.readahead.washington.mobile.media.MediaFileHandler;
import rs.readahead.washington.mobile.mvp.contract.IAttachmentsPresenterContract;


public class AttachmentsPresenter implements IAttachmentsPresenterContract.IPresenter {
    private IAttachmentsPresenterContract.IView view;
    private CompositeDisposable disposables = new CompositeDisposable();
    private CacheWordDataSource cacheWordDataSource;
    private MediaFileHandler mediaFileHandler;

    private List<MediaFile> attachments = new ArrayList<>();


    public AttachmentsPresenter(IAttachmentsPresenterContract.IView view) {
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
    public void setAttachments(List<MediaFile> attachments) {
        this.attachments = attachments;
    }

    @Override
    public List<MediaFile> getAttachments() {
        return attachments;
    }

    @Override
    public void attachNewEvidence(MediaFileBundle mediaFileBundle) {
        disposables.add(mediaFileHandler.registerMediaFile(mediaFileBundle)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<MediaFile>() {
                    @Override
                    public void accept(@NonNull MediaFile mediaFile) throws Exception {
                        if (!attachments.contains(mediaFile)) {
                            attachments.add(mediaFile);
                            view.onEvidenceAttached(mediaFile);
                        }
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(@NonNull Throwable throwable) throws Exception {
                        view.onEvidenceAttachedError(throwable);
                    }
                })
        );
    }

    @Override
    public void attachRegisteredEvidence(final long id) {
        disposables.add(cacheWordDataSource.getDataSource()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .flatMapSingle(new Function<DataSource, SingleSource<MediaFile>>() {
                    @Override
                    public SingleSource<MediaFile> apply(@NonNull DataSource dataSource) throws Exception {
                        return dataSource.getMediaFile(id);
                    }
                })
                .subscribe(new Consumer<MediaFile>() {
                    @Override
                    public void accept(MediaFile mediaFile) throws Exception {
                        if (!attachments.contains(mediaFile)) {
                            attachments.add(mediaFile);
                            view.onEvidenceAttached(mediaFile);
                        }
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) throws Exception {
                        view.onEvidenceAttachedError(throwable);
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
                        view.onEvidenceImported(mediaHolder);
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
                        view.onEvidenceImported(mediaHolder);
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
    public void destroy() {
        disposables.dispose();
        cacheWordDataSource.dispose();
        view = null;
    }
}
