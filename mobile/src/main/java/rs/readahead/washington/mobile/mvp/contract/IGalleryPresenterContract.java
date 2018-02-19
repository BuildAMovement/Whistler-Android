package rs.readahead.washington.mobile.mvp.contract;

import android.content.Context;
import android.net.Uri;

import java.util.List;

import rs.readahead.washington.mobile.domain.entity.MediaFile;
import rs.readahead.washington.mobile.presentation.entity.MediaFileThumbnailData;


public class IGalleryPresenterContract {
    public interface IView {
        void onGetFilesStart();
        void onGetFilesEnd();
        void onGetFilesSuccess(List<MediaFile> files);
        void onGetFilesError(Throwable error);
        void onMediaImported(MediaFile mediaFile, MediaFileThumbnailData thumbnailData);
        void onImportError(Throwable error);
        void onImportStarted();
        void onImportEnded();
        void onMediaFilesAdded(MediaFile mediaFile);
        void onMediaFilesAddingError(Throwable error);
        void onMediaFilesDeleted(int num);
        void onMediaFilesDeletionError(Throwable throwable);
        void onMediaExported(int num);
        void onExportError(Throwable error);
        void onExportStarted();
        void onExportEnded();
        //void onTmpVideoEncrypted(MediaFileBundle mediaFileBundle);
        //void onTmpVideoEncryptionError(Throwable error);
        Context getContext();
    }

    public interface IPresenter extends IBasePresenter {
        void getFiles();
        void importImage(Uri uri);
        void importVideo(Uri uri);
        void addNewMediaFile(MediaFile mediaFile, MediaFileThumbnailData thumbnailData);
        void deleteMediaFiles(List<MediaFile> mediaFiles);
        void exportMediaFiles(List<MediaFile> mediaFiles);
        //void encryptTmpVideo(Uri uri);
    }
}
