package rs.readahead.washington.mobile.domain.repository;

import java.util.List;

import io.reactivex.Maybe;
import io.reactivex.Single;
import rs.readahead.washington.mobile.domain.entity.MediaFile;
import rs.readahead.washington.mobile.presentation.entity.MediaFileThumbnailData;


public interface IMediaFileRecordRepository {
    interface IMediaFileDeleter {
        boolean delete(MediaFile mediaFile);
    }

    Single<MediaFile> registerMediaFile(MediaFile mediaFile, MediaFileThumbnailData thumbnailData);
    Single<List<MediaFile>> listMediaFiles();
    Maybe<MediaFileThumbnailData> getMediaFileThumbnail(long id);
    Single<MediaFileThumbnailData> updateMediaFileThumbnail(long id, MediaFileThumbnailData data);
    Single<List<MediaFile>> getMediaFiles(long[] ids);
    Single<MediaFile> deleteMediaFile(MediaFile mediaFile, IMediaFileDeleter deleter);
}
