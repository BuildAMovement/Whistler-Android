package rs.readahead.washington.mobile.mvp.contract;

import android.content.Context;
import android.net.Uri;

import java.util.List;

import rs.readahead.washington.mobile.domain.entity.MediaFile;
import rs.readahead.washington.mobile.presentation.entity.MediaFileThumbnailData;


public class ICollectFormAttachmentsPresenterContract {
    public interface IView {
        void onActiveFormInstanceMediaFiles(List<MediaFile> mediaFiles);
        void onMediaFilesAttached(List<MediaFile> mediaFile);
        void onMediaFilesAttachedError(Throwable error);
        void onMediaImported(MediaFile mediaFile, MediaFileThumbnailData thumbnailData);
        void onImportError(Throwable error);
        void onImportStarted();
        void onImportEnded();
        //void onTmpVideoEncrypted(MediaFileBundle mediaFileBundle);
        //void onTmpVideoEncryptionError(Throwable error);
        Context getContext();
    }

    public interface IPresenter extends IBasePresenter {
        void getActiveFormInstanceAttachments();
        void attachNewMediaFile(MediaFile mediaFile, MediaFileThumbnailData thumbnailData);
        void attachRegisteredMediaFiles(long[] ids);
        void importImage(Uri uri);
        void importVideo(Uri uri);
        //void encryptTmpVideo(Uri uri);
    }
}
