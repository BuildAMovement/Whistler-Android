package rs.readahead.washington.mobile.views.activity;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.crashlytics.android.Crashlytics;
import com.otaliastudios.cameraview.CameraException;
import com.otaliastudios.cameraview.CameraListener;
import com.otaliastudios.cameraview.CameraOptions;
import com.otaliastudios.cameraview.CameraUtils;
import com.otaliastudios.cameraview.CameraView;
import com.otaliastudios.cameraview.Facing;
import com.otaliastudios.cameraview.Flash;
import com.otaliastudios.cameraview.Gesture;
import com.otaliastudios.cameraview.GestureAction;
import com.otaliastudios.cameraview.SessionType;

import java.io.File;
import java.util.Set;

import butterknife.BindView;
import butterknife.ButterKnife;
import rs.readahead.washington.mobile.R;
import rs.readahead.washington.mobile.domain.entity.Metadata;
import rs.readahead.washington.mobile.domain.entity.TempMediaFile;
import rs.readahead.washington.mobile.media.MediaFileHandler;
import rs.readahead.washington.mobile.mvp.contract.ICameraCapturePresenterContract;
import rs.readahead.washington.mobile.mvp.contract.IMetadataAttachPresenterContract;
import rs.readahead.washington.mobile.mvp.presenter.CameraCapturePresenter;
import rs.readahead.washington.mobile.mvp.presenter.MetadataAttacher;
import rs.readahead.washington.mobile.util.C;
import rs.readahead.washington.mobile.util.DialogsUtil;
import rs.readahead.washington.mobile.views.custom.CameraCaptureButton;
import rs.readahead.washington.mobile.views.custom.CameraDurationTextView;
import rs.readahead.washington.mobile.views.custom.CameraFlashButton;
import rs.readahead.washington.mobile.views.custom.CameraModeButton;
import rs.readahead.washington.mobile.views.custom.CameraSwitchButton;
import timber.log.Timber;


public class CameraActivity extends MetadataActivity implements
        ICameraCapturePresenterContract.IView,
        IMetadataAttachPresenterContract.IView {
    public static String CAMERA_MODE = "cm";

    @BindView(R.id.camera)
    CameraView cameraView;
    @BindView(R.id.confirmLayout)
    ViewGroup confirmLayout;
    @BindView(R.id.confirmImageView)
    ImageView confirmImageView;
    @BindView(R.id.confirmRetry)
    Button confirmRetry;
    @BindView(R.id.confirmOK)
    Button confirmOK;
    @BindView(R.id.switchButton)
    CameraSwitchButton switchButton;
    @BindView(R.id.flashButton)
    CameraFlashButton flashButton;
    @BindView(R.id.captureButton)
    CameraCaptureButton captureButton;
    @BindView(R.id.durationView)
    CameraDurationTextView durationView;
    @BindView(R.id.modeButton)
    CameraModeButton modeButton;

    private CameraCapturePresenter presenter;
    private MetadataAttacher metadataAttacher;
    private Mode mode;
    private boolean videoRecording;
    private ProgressDialog progressDialog;

    public enum Mode {
        PHOTO,
        VIDEO
    }


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_camera);
        ButterKnife.bind(this);

        presenter = new CameraCapturePresenter(this);
        metadataAttacher = new MetadataAttacher(this);

        mode = Mode.PHOTO;
        if (getIntent().hasExtra(CAMERA_MODE)) {
            mode = Mode.valueOf(getIntent().getStringExtra(CAMERA_MODE));
        }

        setupCameraView();
        setupCameraCaptureButton();
        setupCameraModeButton();
        setupConfirmLayout();
    }

    @Override
    protected void onResume() {
        super.onResume();
        cameraView.start();
    }

    @Override
    protected void onStart() {
        super.onStart();
        startLocationMetadataListening();
    }

    @Override
    protected void onPause() {
        super.onPause();
        cameraView.stop();
    }

    @Override
    protected void onStop() {
        stopLocationMetadataListening();
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopPresenter();
        hideProgressDialog();
        cameraView.destroy();
    }

    @Override
    public void onAddingStart() {
        progressDialog = DialogsUtil.showProgressDialog(this, getString(R.string.ra_import_media_progress));
    }

    @Override
    public void onAddingEnd() {
        hideProgressDialog();
    }

    @Override
    public void onAddSuccess(long mediaFileId, String primaryType) {
        attachMediaFileMetadata(mediaFileId, metadataAttacher);
    }

    @Override
    public void onAddError(Throwable error) {
        closeConfirmLayout();
        showToast(R.string.ra_capture_error);
    }

    @Override
    public void onVideoThumbSuccess(@NonNull Bitmap thumb) {
        confirmImageView.setImageBitmap(thumb);
    }

    @Override
    public void onVideoThumbError(Throwable throwable) {
        Timber.d(throwable, getClass().getName());
    }

    @Override
    public void onMetadataAttached(long mediaFileId, @Nullable Metadata metadata) {
        Intent data = new Intent();
        data.putExtra(C.CAPTURED_MEDIA_FILE_ID, mediaFileId);

        setResult(RESULT_OK, data);
        finish();
    }

    @Override
    public void onMetadataAttachError(Throwable throwable) {
        onAddError(throwable);
    }

    @Override
    public Context getContext() {
        return this;
    }

    private void stopPresenter() {
        if (presenter != null) {
            presenter.destroy();
            presenter = null;
        }
    }

    private void showConfirmPhotoView(final byte[] jpeg) {
        Glide.with(this)
                .load(jpeg)
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .skipMemoryCache(true)
                .into(confirmImageView);

        confirmLayout.setVisibility(View.VISIBLE);

        confirmOK.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                CameraUtils.decodeBitmap(jpeg, 400, 400, new CameraUtils.BitmapCallback() { // todo: 200?
                    @Override
                    public void onBitmapReady(Bitmap bitmap) { // todo: reasoning?
                        presenter.addJpegPhoto(jpeg, bitmap);
                    }
                });
            }
        });
    }

    private void showConfirmVideoView(final File video) {
        presenter.getVideoThumb(video);

        captureButton.displayStartVideo();
        durationView.stop();
        confirmLayout.setVisibility(View.VISIBLE);

        confirmOK.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                presenter.addMp4Video(video);
            }
        });
    }

    private void setupCameraView() {
        if (mode == Mode.PHOTO) {
            cameraView.setSessionType(SessionType.PICTURE);
            modeButton.displayPhoto();
        } else {
            cameraView.setSessionType(SessionType.VIDEO);
            modeButton.displayVideo();
        }

        cameraView.mapGesture(Gesture.PINCH, GestureAction.ZOOM);
        cameraView.mapGesture(Gesture.TAP, GestureAction.FOCUS_WITH_MARKER);
        //cameraView.mapGesture(Gesture.LONG_TAP, GestureAction.CAPTURE);

        cameraView.addCameraListener(new CameraListener() {
            @Override
            public void onPictureTaken(byte[] jpeg) {
                showConfirmPhotoView(jpeg);
            }

            @Override
            public void onVideoTaken(File video) {
                showConfirmVideoView(video);
            }

            @Override
            public void onCameraError(@NonNull CameraException exception) {
                Crashlytics.logException(exception);
            }

            @Override
            public void onCameraOpened(CameraOptions options) {
                if (options.getSupportedFacing().size() < 2) {
                    switchButton.setEnabled(false);
                } else {
                    switchButton.setEnabled(true);
                    setupCameraSwitchButton();
                }

                if (options.getSupportedFlash().size() < 2) {
                    flashButton.setEnabled(false);
                } else {
                    flashButton.setEnabled(true);
                    setupCameraFlashButton(options.getSupportedFlash());
                }
                // options object has info
                super.onCameraOpened(options);
            }
        });
    }

    private void setupCameraCaptureButton() {
        if (cameraView.getSessionType() == SessionType.PICTURE) {
            captureButton.displayTakePhoto();
        } else {
            captureButton.displayStartVideo();
        }

        captureButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                modeButton.setVisibility(View.GONE);

                if (cameraView.getSessionType() == SessionType.PICTURE) {
                    cameraView.capturePicture();
                } else {
                    if (videoRecording) {
                        cameraView.stopCapturingVideo();
                        videoRecording = false;
                    } else {
                        TempMediaFile tmp = TempMediaFile.newMp4();
                        File file = MediaFileHandler.getTempFile(CameraActivity.this, tmp);
                        cameraView.startCapturingVideo(file);
                        captureButton.displayStopVideo();
                        durationView.start();
                        videoRecording = true;
                    }
                }
            }
        });
    }

    private void setupCameraModeButton() {
        if (cameraView.getSessionType() == SessionType.PICTURE) {
            modeButton.displayVideo();
        } else {
            modeButton.displayPhoto();
        }

        modeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (cameraView.getSessionType() == SessionType.PICTURE) {
                    cameraView.setSessionType(SessionType.VIDEO);
                    turnFlashDown();
                    modeButton.displayPhoto();
                    captureButton.displayStartVideo();
                } else {
                    cameraView.setSessionType(SessionType.PICTURE);
                    if (cameraView.getFlash() == Flash.TORCH) {
                        cameraView.setFlash(Flash.AUTO);
                    }
                    modeButton.displayVideo();
                    captureButton.displayTakePhoto();
                }
            }
        });
    }

    private void setupCameraSwitchButton() {
        if (cameraView.getFacing() == Facing.FRONT) {
            switchButton.displayFrontCamera();
        } else {
            switchButton.displayBackCamera();
        }

        switchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (cameraView.getFacing() == Facing.BACK) {
                    cameraView.setFacing(Facing.FRONT);
                    switchButton.displayFrontCamera();
                } else {
                    cameraView.setFacing(Facing.BACK);
                    switchButton.displayBackCamera();
                }
            }
        });
    }

    private void setupCameraFlashButton(final Set<Flash> supported) {
        if (cameraView.getFlash() == Flash.AUTO) {
            flashButton.displayFlashAuto();
        } else if (cameraView.getFlash() == Flash.OFF) {
            flashButton.displayFlashOff();
        } else {
            flashButton.displayFlashOn();
        }

        flashButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (cameraView.getSessionType() == SessionType.VIDEO){
                    if (cameraView.getFlash() == Flash.OFF && supported.contains(Flash.TORCH)) {
                        flashButton.displayFlashOn();
                        cameraView.setFlash(Flash.TORCH);
                    } else {
                        turnFlashDown();
                    }
                } else {
                    if (cameraView.getFlash() == Flash.ON || cameraView.getFlash() == Flash.TORCH) {
                        turnFlashDown();
                    } else if (cameraView.getFlash() == Flash.OFF && supported.contains(Flash.AUTO)) {
                        flashButton.displayFlashAuto();
                        cameraView.setFlash(Flash.AUTO);
                    } else {
                        flashButton.displayFlashOn();
                        cameraView.setFlash(Flash.ON);
                    }
                }
            }
        });
    }

    private void turnFlashDown(){
        flashButton.displayFlashOff();
        cameraView.setFlash(Flash.OFF);
    }

    private void setupConfirmLayout() {
        confirmRetry.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                closeConfirmLayout();
            }
        });
    }

    private void closeConfirmLayout() {
        confirmImageView.setImageDrawable(null);
        confirmLayout.setVisibility(View.GONE);
        modeButton.setVisibility(View.VISIBLE);
    }

    private void hideProgressDialog() {
        if (progressDialog != null) {
            progressDialog.dismiss();
            progressDialog = null;
        }
    }
}
