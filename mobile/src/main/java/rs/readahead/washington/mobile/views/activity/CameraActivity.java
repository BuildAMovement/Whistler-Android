package rs.readahead.washington.mobile.views.activity;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.OrientationEventListener;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.SeekBar;

import com.crashlytics.android.Crashlytics;
import com.otaliastudios.cameraview.CameraException;
import com.otaliastudios.cameraview.CameraListener;
import com.otaliastudios.cameraview.CameraOptions;
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
import butterknife.OnClick;
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
    @BindView(R.id.camera_zoom)
    SeekBar mSeekBar;

    private CameraCapturePresenter presenter;
    private MetadataAttacher metadataAttacher;
    private Mode mode;
    private boolean videoRecording;
    private ProgressDialog progressDialog;
    private OrientationEventListener mOrientationEventListener;
    private int zoomLevel = 0;
    private Handler handler;

    public enum Mode {
        PHOTO,
        VIDEO
    }

    private final static int CLICK_DELAY = 1200;
    private long lastClickTime = System.currentTimeMillis();


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_camera);
        ButterKnife.bind(this);

        handler = new Handler();

        presenter = new CameraCapturePresenter(this);
        metadataAttacher = new MetadataAttacher(this);

        mode = Mode.PHOTO;
        if (getIntent().hasExtra(CAMERA_MODE)) {
            mode = Mode.valueOf(getIntent().getStringExtra(CAMERA_MODE));
        }

        setupCameraView();
        setupCameraCaptureButton();
        setupCameraModeButton();
    }

    @Override
    protected void onResume() {
        super.onResume();

        mOrientationEventListener.enable();

        startLocationMetadataListening();

        cameraView.start();
        mSeekBar.setProgress(zoomLevel);
        setCameraZoom();
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onPause() {
        super.onPause();

        stopLocationMetadataListening();

        mOrientationEventListener.disable();

        if (videoRecording) {
            captureButton.performClick();
        }

        cameraView.stop();
    }

    @Override
    protected void onStop() {
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
    public void onBackPressed() {
        if (maybeStopVideoRecording()) return;
        super.onBackPressed();
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
    public void onAddSuccess(long mediaFileId) {
        attachMediaFileMetadata(mediaFileId, metadataAttacher);
    }

    @Override
    public void onAddError(Throwable error) {
        closeConfirmLayout();
        showToast(R.string.ra_capture_error);
    }

    @Override
    public void onVideoThumbSuccess(@NonNull Bitmap thumb) {
        animateImagePreview(thumb, null);
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
    public void rotateViews(int rotation) {
        switchButton.rotateView(rotation);
        flashButton.rotateView(rotation);
        durationView.rotateView(rotation);
        captureButton.rotateView(rotation);
        modeButton.rotateView(rotation);
    }

    @Override
    public Context getContext() {
        return this;
    }

    @OnClick(R.id.captureButton)
    void onCaptureClicked() {
        if (cameraView.getSessionType() == SessionType.PICTURE) {
            cameraView.capturePicture();
        } else {

            switchButton.setVisibility(videoRecording ? View.VISIBLE : View.GONE);
            if (videoRecording) {
                if (System.currentTimeMillis() - lastClickTime < CLICK_DELAY) {
                    return;
                } else {
                    cameraView.stopCapturingVideo();
                    videoRecording = false;
                    switchButton.setVisibility(View.VISIBLE);
                }
            } else {
                lastClickTime = System.currentTimeMillis();
                TempMediaFile tmp = TempMediaFile.newMp4();
                File file = MediaFileHandler.getTempFile(this, tmp);
                cameraView.startCapturingVideo(file);
                captureButton.displayStopVideo();
                durationView.start();
                videoRecording = true;
                switchButton.setVisibility(View.GONE);
            }
        }
    }


    @OnClick(R.id.modeButton)
    void onModeClicked() {
        if (cameraView.getSessionType() == SessionType.PICTURE) {
            cameraView.setSessionType(SessionType.VIDEO);
            turnFlashDown();
            modeButton.displayPhoto();
            captureButton.displayStartVideo();
            mode = CameraActivity.Mode.VIDEO;
        } else {
            mode = CameraActivity.Mode.PHOTO;
            cameraView.setSessionType(SessionType.PICTURE);
            if (cameraView.getFlash() == Flash.TORCH) {
                cameraView.setFlash(Flash.AUTO);
            }
            modeButton.displayVideo();
            captureButton.displayTakePhoto();
        }

        resetZoom();
    }

    @OnClick(R.id.switchButton)
    void onSwitchClicked() {
        if (cameraView.getFacing() == Facing.BACK) {
            cameraView.setFacing(Facing.FRONT);
            switchButton.displayFrontCamera();
        } else {
            cameraView.setFacing(Facing.BACK);
            switchButton.displayBackCamera();
        }
    }

    private void resetZoom() {
        zoomLevel = 0;
        mSeekBar.setProgress(0);
        setCameraZoom();
    }

    private void setCameraZoom() {
        cameraView.setZoom((float) zoomLevel / 100);
    }

    private void animateImagePreview(final Bitmap previewImage, final byte[] jpeg) {
        Animation animation = AnimationUtils.loadAnimation(this, R.anim.preview_anim);
        animation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                confirmImageView.setImageBitmap(previewImage);
                confirmLayout.setVisibility(View.VISIBLE);
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                confirmLayout.setVisibility(View.INVISIBLE);
                confirmImageView.setImageBitmap(null);
                // TODO CHECK THIS! decodeTakenPhoto method is not called because there is already bitmap thumbnail created for preview
                // TODO check if size of the bitmap is too big and move decoding to separate thread in rx
                if (mode == CameraActivity.Mode.PHOTO) {
                    presenter.addJpegPhoto(jpeg, previewImage);
                }
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }
        });

        confirmLayout.startAnimation(animation);
    }

    private boolean maybeStopVideoRecording() {
        if (videoRecording) {
            captureButton.performClick();
            return true;
        }

        return false;
    }

    private void stopPresenter() {
        if (presenter != null) {
            presenter.destroy();
            presenter = null;
        }
    }

    private void showConfirmVideoView(final File video) {
        captureButton.displayStartVideo();
        durationView.stop();

        presenter.getVideoThumb(video);
        presenter.addMp4Video(video);
    }

    private void showPreviewPhoto(final byte[] jpeg) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                BitmapFactory.Options opt = new BitmapFactory.Options();
                opt.inSampleSize = 8;
                final Bitmap previewImage = BitmapFactory.decodeByteArray(jpeg, 0, jpeg.length, opt);

                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        animateImagePreview(previewImage, jpeg);
                    }
                });
            }
        }).start();
    }

    private void setupCameraView() {
        if (mode == Mode.PHOTO) {
            cameraView.setSessionType(SessionType.PICTURE);
            modeButton.displayPhoto();
        } else {
            cameraView.setSessionType(SessionType.VIDEO);
            modeButton.displayVideo();
        }

        //cameraView.setEnabled(PermissionUtil.checkPermission(this, Manifest.permission.CAMERA));
        cameraView.mapGesture(Gesture.TAP, GestureAction.FOCUS_WITH_MARKER);

        setOrientationListener();

        cameraView.addCameraListener(new CameraListener() {
            @Override
            public void onPictureTaken(byte[] jpeg) {
                showPreviewPhoto(jpeg);
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
                    switchButton.setVisibility(View.GONE);
                } else {
                    switchButton.setVisibility(View.VISIBLE);
                    setupCameraSwitchButton();
                }

                if (options.getSupportedFlash().size() < 2) {
                    flashButton.setVisibility(View.GONE);
                } else {
                    flashButton.setVisibility(View.VISIBLE);
                    setupCameraFlashButton(options.getSupportedFlash());
                }
                // options object has info
                super.onCameraOpened(options);
            }
        });

        mSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                zoomLevel = i;
                setCameraZoom();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
    }

    private void setupCameraCaptureButton() {
        if (cameraView.getSessionType() == SessionType.PICTURE) {
            captureButton.displayTakePhoto();
        } else {
            captureButton.displayStartVideo();
        }
    }

    private void setupCameraModeButton() {
        if (cameraView.getSessionType() == SessionType.PICTURE) {
            modeButton.displayVideo();
        } else {
            modeButton.displayPhoto();
        }
    }

    private void setupCameraSwitchButton() {
        if (cameraView.getFacing() == Facing.FRONT) {
            switchButton.displayFrontCamera();
        } else {
            switchButton.displayBackCamera();
        }
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
                if (cameraView.getSessionType() == SessionType.VIDEO) {
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

    private void turnFlashDown() {
        flashButton.displayFlashOff();
        cameraView.setFlash(Flash.OFF);
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

    private void setOrientationListener() {
        mOrientationEventListener = new OrientationEventListener(
                this, SensorManager.SENSOR_DELAY_NORMAL) {

            @Override
            public void onOrientationChanged(int orientation) {
                if (orientation != OrientationEventListener.ORIENTATION_UNKNOWN) {
                    presenter.handleRotation(orientation);
                }
            }
        };
    }
}
