package rs.readahead.washington.mobile.mvp.contract;

import android.content.Context;
import android.graphics.Bitmap;
import android.support.annotation.NonNull;

import java.io.File;


public class ICameraCapturePresenterContract {
    public interface IView {
        void onAddingStart();
        void onAddingEnd();
        void onAddSuccess(long mediaFileId);
        void onAddError(Throwable error);
        void onVideoThumbSuccess(@NonNull Bitmap thumb);
        void onVideoThumbError(Throwable throwable);
        void rotateViews(int rotation);
        Context getContext();
    }

    public interface IPresenter extends IBasePresenter {
        void addJpegPhoto(byte[] jpeg, Bitmap thumb);
        void addMp4Video(File file);
        void getVideoThumb(File file);
        void handleRotation(int orientation);
    }
}
