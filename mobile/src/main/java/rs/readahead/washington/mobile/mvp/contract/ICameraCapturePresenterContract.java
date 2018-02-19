package rs.readahead.washington.mobile.mvp.contract;

import android.content.Context;
import android.graphics.Bitmap;
import android.support.annotation.NonNull;

import java.io.File;

import rs.readahead.washington.mobile.presentation.entity.MediaFileThumbnailData;


public class ICameraCapturePresenterContract {
    public interface IView {
        void onAddingStart();
        void onAddingEnd();
        void onAddSuccess(long mediaFileId, String primaryMime);
        void onAddError(Throwable error);
        void onVideoThumbSuccess(@NonNull Bitmap thumb);
        void onVideoThumbError(Throwable throwable);
        Context getContext();
    }

    public interface IPresenter extends IBasePresenter {
        void addJpegPhoto(byte[] jpeg, Bitmap thumb);
        void addMp4Video(File file);
        void getVideoThumb(File file);
    }
}
