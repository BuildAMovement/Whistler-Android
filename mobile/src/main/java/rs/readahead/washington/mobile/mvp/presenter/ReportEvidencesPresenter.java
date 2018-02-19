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
import rs.readahead.washington.mobile.data.database.CacheWordDataSource;
import rs.readahead.washington.mobile.data.database.DataSource;
import rs.readahead.washington.mobile.domain.entity.MediaFile;
import rs.readahead.washington.mobile.media.MediaFileBundle;
import rs.readahead.washington.mobile.media.MediaFileHandler;
import rs.readahead.washington.mobile.mvp.contract.IReportEvidencesPresenterContract;
import rs.readahead.washington.mobile.presentation.entity.EvidenceData;
import rs.readahead.washington.mobile.presentation.entity.ReportViewType;


public class ReportEvidencesPresenter implements
        IReportEvidencesPresenterContract.IPresenter {
    private IReportEvidencesPresenterContract.IView view;
    private MediaFileHandler mediaFileHandler;
    private CacheWordDataSource cacheWordDataSource;
    private CompositeDisposable disposables = new CompositeDisposable();
    private ReportViewType type;

    private List<MediaFile> evidences = Collections.emptyList();


    public ReportEvidencesPresenter(IReportEvidencesPresenterContract.IView view) {
        this.view = view;
        this.cacheWordDataSource = new CacheWordDataSource(view.getContext());
        this.mediaFileHandler = new MediaFileHandler(cacheWordDataSource);
    }

    @Override
    public void setEvidences(EvidenceData evidenceData) {
        evidences = evidenceData.getEvidences();
    }

    @Override
    public List<MediaFile> getEvidences() {
        return evidences;
    }

    @Override
    public void attachNewEvidence(MediaFileBundle mediaFileBundle) {
        disposables.add(mediaFileHandler.registerMediaFile(mediaFileBundle)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<MediaFile>() {
                    @Override
                    public void accept(@NonNull MediaFile mediaFile) throws Exception {
                        if (!evidences.contains(mediaFile)) {
                            evidences.add(mediaFile);
                            view.onEvidencesAttached(Collections.singletonList(mediaFile));
                        }
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(@NonNull Throwable throwable) throws Exception {
                        view.onEvidencesAttachedError(throwable);
                    }
                })
        );
    }

    @Override
    public void attachRegisteredEvidences(final long[] ids) {
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
                        for (Iterator<MediaFile> iterator = mediaFiles.iterator(); iterator.hasNext();) {
                            MediaFile mediaFile = iterator.next();
                            if (evidences.contains(mediaFile)) {
                                iterator.remove();
                            }
                        }

                        if (!mediaFiles.isEmpty()) {
                            evidences.addAll(mediaFiles);
                            view.onEvidencesAttached(mediaFiles);
                        }
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) throws Exception {
                        view.onEvidencesAttachedError(throwable);
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
    public void setReportType(ReportViewType type) {
        this.type = type;
        if (isInPreviewMode()) {
            hideFAB();
        }
    }

    private void hideFAB(){
        if(view == null) return;
        view.hideFAB();
    }

    @Override
    public ReportViewType getReportType() {
        return type;
    }

    @Override
    public boolean isInPreviewMode() {
        return type == ReportViewType.PREVIEW;
    }

    @Override
    public void destroy() {
        cacheWordDataSource.dispose();
        disposables.dispose();
        view = null;
    }
}
