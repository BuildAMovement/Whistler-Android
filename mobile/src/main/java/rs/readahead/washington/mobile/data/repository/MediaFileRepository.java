package rs.readahead.washington.mobile.data.repository;

import android.support.annotation.NonNull;

import java.util.Collection;

import io.reactivex.Completable;
import io.reactivex.CompletableSource;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;
import rs.readahead.washington.mobile.data.entity.mapper.EntityMapper;
import rs.readahead.washington.mobile.data.rest.ReportsApi;
import rs.readahead.washington.mobile.domain.entity.MediaFile;
import rs.readahead.washington.mobile.domain.repository.IMediaFileRepository;


public class MediaFileRepository implements IMediaFileRepository {
    @Override
    public Completable registerFormAttachments(@NonNull Collection<MediaFile> mediaFiles) {
        return ReportsApi.getApi().registerFormAttachments(new EntityMapper().transformMediaFiles(mediaFiles))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .onErrorResumeNext(new Function<Throwable, CompletableSource>() {
                    @Override
                    public CompletableSource apply(Throwable throwable) throws Exception {
                        return Completable.error(new ErrorBundle(throwable));
                    }
                });
    }
}
