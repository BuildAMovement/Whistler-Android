package rs.readahead.washington.mobile.views.activity;

import android.Manifest;
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
import android.support.design.widget.BottomSheetBehavior;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.Toolbar;
import android.util.TypedValue;
import android.view.MenuItem;
import android.view.OrientationEventListener;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.SeekBar;

import com.crashlytics.android.Crashlytics;
import com.otaliastudios.cameraview.CameraException;
import com.otaliastudios.cameraview.CameraListener;
import com.otaliastudios.cameraview.CameraOptions;
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
import permissions.dispatcher.NeedsPermission;
import permissions.dispatcher.OnNeverAskAgain;
import permissions.dispatcher.OnPermissionDenied;
import permissions.dispatcher.OnShowRationale;
import permissions.dispatcher.PermissionRequest;
import permissions.dispatcher.RuntimePermissions;
import rs.readahead.washington.mobile.MyApplication;
import rs.readahead.washington.mobile.R;
import rs.readahead.washington.mobile.bus.EventCompositeDisposable;
import rs.readahead.washington.mobile.bus.EventObserver;
import rs.readahead.washington.mobile.bus.event.CameraFlingUpEvent;
import rs.readahead.washington.mobile.bus.event.LocaleChangedEvent;
import rs.readahead.washington.mobile.domain.entity.Metadata;
import rs.readahead.washington.mobile.domain.entity.TempMediaFile;
import rs.readahead.washington.mobile.media.MediaFileHandler;
import rs.readahead.washington.mobile.mvp.contract.ICameraCapturePresenterContract;
import rs.readahead.washington.mobile.mvp.contract.IHomeScreenPresenterContract;
import rs.readahead.washington.mobile.mvp.contract.IMetadataAttachPresenterContract;
import rs.readahead.washington.mobile.mvp.presenter.CameraCapturePresenter;
import rs.readahead.washington.mobile.mvp.presenter.HomeScreenPresenter;
import rs.readahead.washington.mobile.mvp.presenter.MetadataAttacher;
import rs.readahead.washington.mobile.util.C;
import rs.readahead.washington.mobile.util.DialogsUtil;
import rs.readahead.washington.mobile.util.PermissionUtil;
import rs.readahead.washington.mobile.views.custom.CameraCaptureButton;
import rs.readahead.washington.mobile.views.custom.CameraDurationTextView;
import rs.readahead.washington.mobile.views.custom.CameraFlashButton;
import rs.readahead.washington.mobile.views.custom.CameraModeButton;
import rs.readahead.washington.mobile.views.custom.CameraSwitchButton;
import rs.readahead.washington.mobile.views.custom.CountdownImageView;
import rs.readahead.washington.mobile.views.custom.WaCameraView;
import timber.log.Timber;


@RuntimePermissions
public class MainActivity extends MetadataActivity implements
        NavigationView.OnNavigationItemSelectedListener,
        ICameraCapturePresenterContract.IView,
        IMetadataAttachPresenterContract.IView,
        IHomeScreenPresenterContract.IView {
    @BindView(R.id.toolbar)
    Toolbar toolbar;
    @BindView(R.id.drawer_layout)
    DrawerLayout drawer;
    @BindView(R.id.nav_view)
    NavigationView navigationView;
    @BindView(R.id.bottom_sheet)
    View bottomSheet;
    @BindView(R.id.collapsed_view)
    RelativeLayout bottomSheetCollapsedView;
    @BindView(R.id.panic_mode_view)
    View bottomSheetPanicModeView;
    @BindView(R.id.countdown_timer)
    CountdownImageView countdownImage;
    @BindView(R.id.camera)
    WaCameraView cameraView;
    @BindView(R.id.camera_overlay)
    View cameraOverlay;
    @BindView(R.id.microphone)
    View microphone;
    @BindView(R.id.bottom_layout)
    View mButtonsLayout;
    @BindView(R.id.top_layout)
    View mTopLayout;
    @BindView(R.id.app_bar)
    View mAppBar;
    @BindView(R.id.container)
    View mContainer;
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
    @BindView(R.id.captureLayout)
    View mBottomCameraLayout;
    @BindView(R.id.top_camera_layout)
    View mTopCameraLayout;
    @BindView(R.id.main_container)
    View mainView;
    @BindView(R.id.camera_zoom)
    SeekBar mSeekBar;
    @BindView(R.id.gallery_shortcut)
    ImageView mGalleryShortcut;

    private BottomSheetBehavior mBottomSheetBehavior;
    private boolean mExit = false;
    private Handler handler;
    private boolean panicActivated;
    private EventCompositeDisposable disposables;
    private boolean isInCameraMode = false;
    private AlertDialog alertDialog;
    private CameraCapturePresenter presenter;
    private MetadataAttacher metadataAttacher;
    private HomeScreenPresenter homeScreenPresenter;
    private CameraActivity.Mode mode = CameraActivity.Mode.PHOTO;
    private boolean videoRecording;
    private ProgressDialog progressDialog;
    private int currentRotation = 0;
    private OrientationEventListener mOrientationEventListener;
    private int zoomLevel = 0;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);
        ButterKnife.bind(this);

        setSupportActionBar(toolbar);

        handler = new Handler();

        presenter = new CameraCapturePresenter(this);
        metadataAttacher = new MetadataAttacher(this);
        homeScreenPresenter = new HomeScreenPresenter(this);

        panicActivated = false;

        initSetup();

        // prepare camera preview..
        cameraView.setSessionType(SessionType.PICTURE);
        cameraView.setEnabled(PermissionUtil.checkPermission(this, Manifest.permission.CAMERA));
        setOrientationListener();
    }

    private void initSetup() {
        //handleOrbot();
        setupCameraView();
        setupCameraCaptureButton();
        setupCameraModeButton();
        setupConfirmLayout();

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        navigationView.setNavigationItemSelectedListener(this);

        mTopCameraLayout.setVisibility(View.GONE);
        mTopCameraLayout.setPadding(0, (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 20, getResources().getDisplayMetrics()),
                0,0);
        mBottomCameraLayout.setVisibility(View.GONE);

        mBottomSheetBehavior = BottomSheetBehavior.from(bottomSheet);
        mBottomSheetBehavior.setBottomSheetCallback(new MyBottomSheetBehavior());

        disposables = MyApplication.bus().createCompositeDisposable();
        disposables.wire(LocaleChangedEvent.class, new EventObserver<LocaleChangedEvent>() {
            @Override
            public void onNext(LocaleChangedEvent event) {
                recreate();
            }
        });
        disposables.wire(CameraFlingUpEvent.class, new EventObserver<CameraFlingUpEvent>() {
            @Override
            public void onNext(CameraFlingUpEvent event) {
                onGalleryShortcutClicked();
            }
        });

        //setMarginsAndPadding();
    }

    @OnClick(R.id.collect)
    void onCollectClicked() {
        startActivity(new Intent(this, CollectMainActivity.class));
    }

    @OnClick(R.id.gallery_shortcut)
    void onGalleryShortcutClicked() {
        showGallery(true);
    }

    @OnClick(R.id.gallery)
    void onGalleryClicked() {
        showGallery(false);
    }

    @OnClick(R.id.panic_mode_view)
    void onPanicModeClicked() {
        mBottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
    }

    @OnClick(R.id.container)
    void onContainerClicked() {
        if (PermissionUtil.checkPermission(this, Manifest.permission.CAMERA)) {
            if (!isInCameraMode) {
                if (MyApplication.isAnonymousMode()) {
                    MainActivityPermissionsDispatcher.switchToCameraModeAnonymousWithCheck(this);
                } else {
                    MainActivityPermissionsDispatcher.switchToCameraModeLocationWithCheck(this);
                }
            }
        } else {
            MainActivityPermissionsDispatcher.enableCameraWithCheck(this);
        }
    }

    @OnClick(R.id.microphone)
    void onMicrophoneClicked() {
        if (MyApplication.isAnonymousMode()) {
            startAudioRecorderActivityAnonymous();
        } else {
            MainActivityPermissionsDispatcher.startAudioRecorderActivityLocationWithCheck(this);
        }
    }

    @NeedsPermission(Manifest.permission.CAMERA)
    void enableCamera() {
        recreate(); // we have permissions, recreate activity to show preview..
    }

    @NeedsPermission({Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO})
    public void switchToCameraModeLocation() {
        checkLocationSettings(C.START_CAMERA_CAPTURE, new MetadataActivity.LocationSettingsCheckDoneListener() {
            @Override
            public void onContinue() {
                switchToCameraModeWithLocationChecked();
            }
        });
    }

    @NeedsPermission({Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO})
    public void switchToCameraModeAnonymous() {
        switchToCameraMode();
    }

    private void switchToCameraModeWithLocationChecked() {
        startLocationMetadataListening();
        switchToCameraMode();
    }

    @NeedsPermission(Manifest.permission.ACCESS_FINE_LOCATION)
    public void startAudioRecorderActivityLocation() {
        checkLocationSettings(C.START_AUDIO_RECORD, new MetadataActivity.LocationSettingsCheckDoneListener() {
            @Override
            public void onContinue() {
                startAudioRecordActivityWithLocationChecked();
            }
        });
    }

    private void startAudioRecorderActivityAnonymous() {
        startAudioRecordActivityWithLocationChecked();
    }

    private void startAudioRecordActivityWithLocationChecked() {
        startActivityForResult(new Intent(this, AudioRecordActivity2.class), C.RECORDED_AUDIO);
    }

    @OnShowRationale(Manifest.permission.ACCESS_FINE_LOCATION)
    void showFineLocationRationale(final PermissionRequest request) {
        alertDialog = PermissionUtil.showRationale(
                this, request, getString(R.string.ra_media_location_permissions));
    }

    @OnShowRationale(Manifest.permission.CAMERA)
    void showCameraRationale(final PermissionRequest request) {
        alertDialog = PermissionUtil.showRationale(this, request, getString(R.string.ra_camera_preview));
    }

    @OnShowRationale({Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO})
    void showCameraAndAudioRationale(final PermissionRequest request) {
        alertDialog = PermissionUtil.showRationale(this, request, getString(R.string.ra_camera_rationale));
    }

    @OnShowRationale({Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO})
    void showLocationCameraAndAudioRationale(final PermissionRequest request) {
        alertDialog = PermissionUtil.showRationale(this, request, getString(R.string.ra_camera_rationale));
    }

    @OnPermissionDenied(Manifest.permission.CAMERA)
    void onCameraPermissionDenied() {
    }

    @OnPermissionDenied(Manifest.permission.ACCESS_FINE_LOCATION)
    void onFineLocationPermissionDenied() {
        startAudioRecordActivityWithLocationChecked();
    }

    @OnPermissionDenied({Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO})
    void onCameraAndAudioPermissionDenied() {
    }

    @OnPermissionDenied({Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO})
    void onLocationCameraAndAudioPermissionDenied() {
    }

    @OnNeverAskAgain(Manifest.permission.ACCESS_FINE_LOCATION)
    void onFineLocationNeverAskAgain() {
        startAudioRecordActivityWithLocationChecked();
    }

    @OnNeverAskAgain(Manifest.permission.CAMERA)
    void onCameraNeverAskAgain() {
    }

    @OnNeverAskAgain({Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO})
    void onCameraAndAudioNeverAskAgain() {
    }

    @OnNeverAskAgain({Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO})
    void onLocationCameraAndAudioNeverAskAgain() {
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        switch (id) {
            case R.id.nav_collect:
                startActivity(new Intent(MainActivity.this, CollectMainActivity.class));
                break;

            case R.id.nav_reporting:
                startActivity(new Intent(MainActivity.this, ReportActivity.class));
                break;

            case R.id.nav_gallery:
                showGallery(false);
                break;

            case R.id.nav_settings:
                startActivity(new Intent(MainActivity.this, SettingsActivity.class));
                break;

            case R.id.nav_feedback:
                startActivity(new Intent(MainActivity.this, FeedbackActivity.class));
                break;

            case R.id.nav_about_n_help:
                startAboutHelpActivity();
                break;

            case R.id.nav_log_out:
                finish();
                break;

            case R.id.nav_training_room:
                startActivity(new Intent(MainActivity.this, TrainingActivity.class));
                break;

            default:
                break;
        }

        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    private void showGallery(boolean animated) {
        if (MyApplication.isAnonymousMode()) {
            startGalleryActivity(animated);
        } else {
            MainActivityPermissionsDispatcher.startGalleryActivityWithCheck(this, animated);
        }
    }

    @NeedsPermission(Manifest.permission.ACCESS_FINE_LOCATION)
    public void startGalleryActivity(boolean animated) {
        startActivity(new Intent(MainActivity.this, GalleryActivity.class)
                .putExtra(GalleryActivity.GALLERY_MODE, GalleryActivity.Mode.GALLERY_VIEW_MEDIA.name())
                .putExtra(GalleryActivity.GALLERY_ANIMATED, animated));
    }

    private boolean isLocationSettingsRequestCode(int requestCode) {
        return requestCode == C.START_CAMERA_CAPTURE ||
                requestCode == C.START_AUDIO_RECORD;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (isLocationSettingsRequestCode(requestCode)) {
            setLocationSettingsEnabled(resultCode == RESULT_OK);
        } else if (resultCode != RESULT_OK) {
            return; // user canceled evidence acquiring
        }

        switch (requestCode) {
            case C.START_CAMERA_CAPTURE:
                switchToCameraModeWithLocationChecked();
                break;

            case C.CAMERA_CAPTURE:
            case C.RECORDED_AUDIO:
                // everything is done already..
                break;
        }
    }

    private void startAboutHelpActivity() {
        startActivity(new Intent(MainActivity.this, AboutHelpActivity.class));
    }

    /*private BroadcastReceiver torStatusReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            //if (TextUtils.equals(intent.getAction(), OrbotHelper.ACTION_STATUS)) {
            //    String status = intent.getStringExtra(OrbotHelper.EXTRA_STATUS);
            //    // TODO: See what is going on with TOR
            //    boolean enabled = status.equals(OrbotHelper.STATUS_ON);
            //}
        }
    };*/

    /*private void handleOrbot() {
        // todo: for now we don't want to ask for TOR at application first start
        SharedPrefs.getInstance().setAskForTorOnStart(false);

        if (SharedPrefs.getInstance().isTorModeActive()) {
            //checkNetworkSecurity();
        } else if (SharedPrefs.getInstance().askForTorOnStart()) {
            DialogsUtil.showMessageOKCancelWithTitle(this, getString(R.string.orbot_activation),
                    getString(R.string.attention), getString(R.string.ok), getString(R.string.cancel),
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            //checkNetworkSecurity();
                            dialog.dismiss();
                        }
                    },
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            SharedPrefs.getInstance().setAskForTorOnStart(false);
                            dialog.dismiss();
                        }
                    });
        }
    }*/

    /*private void checkNetworkSecurity() {
        if (OrbotHelper.isOrbotInstalled(this)) {
            OrbotHelper.requestStartTor(this);
            SharedPrefs.getInstance().setToreModeActive(true);
        } else
            DialogsUtil.showOrbotDialog(this);
    }*/

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        MainActivityPermissionsDispatcher.onRequestPermissionsResult(this, requestCode, grantResults);
    }

    @Override
    public void onBackPressed() {
        if (maybeCloseDrawer()) return;
        if (maybeCloseCamera()) return;
        if (!checkIfShouldExit()) return;
        closeApp();
    }

    private void closeApp() {
        mBottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
        finish();
        lockCacheWord();
    }

    private boolean checkIfShouldExit() {
        if (!mExit) {
            mExit = true;
            showToast(R.string.exit);
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mExit = false;
                }
            }, 3 * 1000);
            return false;
        }
        return true;
    }

    private boolean maybeCloseDrawer() {
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
            return true;
        }
        return false;
    }

    private boolean maybeCloseCamera() {
        if (isInCameraMode) {
            if (videoRecording) {
                captureButton.performClick();
            } else {
                switchToMainMode();
            }
            return true;
        }
        return false;
    }

    private void lockCacheWord() {
        if (!getCacheWordHandler().isLocked()) {
            getCacheWordHandler().lock();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (disposables != null) {
            disposables.dispose();
        }

        stopPresenters();
        hideProgressDialog();
        cameraView.destroy();
    }

    @Override
    protected void onResume() {
        super.onResume();

        //registerReceiver(torStatusReceiver, new IntentFilter(OrbotHelper.ACTION_STATUS));

        startLocationMetadataListening();

        mOrientationEventListener.enable();

        if (panicActivated) {
            mBottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
            panicActivated = false;
        }

        cameraView.start();
        mSeekBar.setProgress(zoomLevel);
        setCameraZoom();
    }

    @Override
    protected void onPause() {
        super.onPause();

        stopLocationMetadataListening();

        mOrientationEventListener.disable();

        if (countdownImage.isCounting()) {
            mBottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
            panicActivated = true;
        }

        if (videoRecording) {
            captureButton.performClick();
        }

        cameraView.stop();
    }

    @Override
    protected void onStop() {
        if (alertDialog != null && alertDialog.isShowing()) {
            alertDialog.dismiss();
        }

        super.onStop();
    }

    @NeedsPermission(Manifest.permission.SEND_SMS)
    public void onBottomSheetExpanded() {
        int timerDuration = getResources().getInteger(R.integer.panic_countdown_duration);
        countdownImage.start(timerDuration, new CountdownImageView.IFinishHandler() {
            @Override
            public void onFinish() {
                executePanicMode();
                mBottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
            }
        });
    }

    @OnShowRationale(Manifest.permission.SEND_SMS)
    void onBottomSheetExpandedRationale(final PermissionRequest request) {
        mBottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
        alertDialog = PermissionUtil.showRationale(this, request, getString(R.string.permission_sms));
    }

    @OnPermissionDenied(Manifest.permission.SEND_SMS)
    void onBottomSheetExpandedPermissionDenied() {
        mBottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
    }

    @OnNeverAskAgain(Manifest.permission.SEND_SMS)
    void onBottomSheetExpandedNeverAskAgain() {
        mBottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
    }

    private class MyBottomSheetBehavior extends BottomSheetBehavior.BottomSheetCallback {
        final int timerDuration = getResources().getInteger(R.integer.panic_countdown_duration);

        @Override
        public void onStateChanged(@NonNull View bottomSheet, int newState) {
            if (newState == BottomSheetBehavior.STATE_EXPANDED) {
                MainActivityPermissionsDispatcher.onBottomSheetExpandedWithCheck(MainActivity.this);
            } else if (newState == BottomSheetBehavior.STATE_COLLAPSED) {
                countdownImage.cancel();
                countdownImage.setCountdownNumber(timerDuration);
            }
        }

        @Override
        public void onSlide(@NonNull View bottomSheet, float slideOffset) {
            countdownImage.cancel();
            bottomSheetCollapsedView.setAlpha(Math.abs(1 - slideOffset));
            bottomSheetPanicModeView.setAlpha(slideOffset);
        }
    }

    void executePanicMode() {
        try {
            if (homeScreenPresenter != null) {
                homeScreenPresenter.executePanicMode(getCacheWordHandler());
            }
        } catch (Throwable ignored) {
            panicActivated = true;
        }
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

        confirmLayout.setVisibility(View.INVISIBLE);
        confirmImageView.setImageBitmap(null);
    }

    @Override
    public void onMetadataAttachError(Throwable throwable) {
        onAddError(throwable);
    }

    @Override
    public Context getContext() {
        return this;
    }

    private void stopPresenters() {
        if (presenter != null) {
            presenter.destroy();
            presenter = null;
        }

        if (homeScreenPresenter != null) {
            homeScreenPresenter.destroy();
            homeScreenPresenter = null;
        }
    }

//    private void decodeTakenPhotoView(final byte[] jpeg) {
//        CameraUtils.decodeBitmap(jpeg, 400, 400, new CameraUtils.BitmapCallback() {
//            @Override
//            public void onBitmapReady(final Bitmap bitmap) {
//                presenter.addJpegPhoto(jpeg, bitmap);
//            }
//        });
//
//    }

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

    private void animateImagePreview(final Bitmap previewImage, final byte[] jpeg) {
        Animation animation = AnimationUtils.loadAnimation(MainActivity.this, R.anim.preview_anim);
        animation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                confirmImageView.setImageBitmap(previewImage);
                confirmLayout.setVisibility(View.VISIBLE);
                showGalleryShortcut(true);
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


    private void showConfirmVideoView(final File video) {
        captureButton.displayStartVideo();
        durationView.stop();

        presenter.getVideoThumb(video);
        presenter.addMp4Video(video);
    }

    private void setupCameraView() {
        if (mode == CameraActivity.Mode.PHOTO) {
            cameraView.setSessionType(SessionType.PICTURE);
            modeButton.displayPhoto();
        } else {
            cameraView.setSessionType(SessionType.VIDEO);
            modeButton.displayVideo();
        }

        //cameraView.mapGesture(Gesture.PINCH, GestureAction.ZOOM);
        cameraView.mapGesture(Gesture.TAP, GestureAction.FOCUS_WITH_MARKER);
        //cameraView.mapGesture(Gesture.LONG_TAP, GestureAction.CAPTURE);

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

    @OnClick(R.id.captureButton)
    void onCaptureClicked() {
        if (cameraView.getSessionType() == SessionType.PICTURE) {
            showGalleryShortcut(false);
            cameraView.capturePicture();
        } else {
            switchButton.setVisibility(videoRecording ? View.VISIBLE : View.GONE);
            if (videoRecording) {
                cameraView.stopCapturingVideo();
                videoRecording = false;
                switchButton.setVisibility(View.VISIBLE);
            } else {
                TempMediaFile tmp = TempMediaFile.newMp4();
                File file = MediaFileHandler.getTempFile(MainActivity.this, tmp);
                cameraView.startCapturingVideo(file);
                captureButton.displayStopVideo();
                durationView.start();
                videoRecording = true;
                showGalleryShortcut(false);
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
    }

    private void setCameraZoom() {
        cameraView.setZoom((float) zoomLevel / 100);
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

    private void setupConfirmLayout() {
//        confirmRetry.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                closeConfirmLayout();
//            }
//        });
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

    private void switchToCameraMode() {
        hideMainViews();
        showCameraLayouts();
        isInCameraMode = true;
    }

    private void switchToMainMode() {
        showMainViews();
        hideCameraLayouts();
        isInCameraMode = false;
    }

    private void showCameraLayouts() {
        mTopCameraLayout.startAnimation(getEnterAnimation(mTopCameraLayout, false));
        mBottomCameraLayout.startAnimation(getEnterAnimation(mBottomCameraLayout, true));
        showGalleryShortcut(true);
    }

    private void hideCameraLayouts() {
        mTopCameraLayout.startAnimation(getExitAnimation(mTopCameraLayout, false));
        mBottomCameraLayout.startAnimation(getExitAnimation(mBottomCameraLayout, true));
        showGalleryShortcut(false);
    }

    private void hideMainViews() {
        enableDrawer(false);
        bottomSheet.startAnimation(getExitAnimation(bottomSheet, true));
        mButtonsLayout.startAnimation(getExitAnimation(mButtonsLayout, true));
        mTopLayout.startAnimation(getExitAnimation(mTopLayout, false));
        mAppBar.startAnimation(getExitAnimation(mAppBar, false));
        mContainer.setVisibility(View.GONE);
    }

    private void showMainViews() {
        enableDrawer(true);
        bottomSheet.startAnimation(getEnterAnimation(bottomSheet, true));
        mTopLayout.startAnimation(getEnterAnimation(mTopLayout, false));
        mButtonsLayout.startAnimation(getEnterAnimation(mButtonsLayout, true));
        mAppBar.startAnimation(getEnterAnimation(mAppBar, false));
        mContainer.setVisibility(View.VISIBLE);
    }

    private void showGalleryShortcut(boolean show) {
        if (show) {
            mGalleryShortcut.startAnimation(getEnterAnimation(mGalleryShortcut, true));
        } else {
            mGalleryShortcut.startAnimation(getExitAnimation(mGalleryShortcut, true));
        }
    }

    private void enableDrawer(boolean enable) {
        drawer.setDrawerLockMode(enable ? DrawerLayout.LOCK_MODE_UNLOCKED : DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
    }

    private Animation getExitAnimation(final View view, boolean isBottom) {
        Animation animation = AnimationUtils.loadAnimation(this, isBottom ? R.anim.slide_out_down : R.anim.slide_out_up_main);
        animation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                view.setVisibility(View.GONE);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }
        });

        return animation;
    }

    private Animation getEnterAnimation(final View view, final boolean isBottom) {
        Animation animation = AnimationUtils.loadAnimation(this, isBottom ? R.anim.slide_in_up : R.anim.slide_in_down);
        animation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                view.setVisibility(View.VISIBLE);
            }

            @Override
            public void onAnimationEnd(Animation animation) {
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }
        });

        return animation;
    }

    private void setOrientationListener() {
        mOrientationEventListener = new OrientationEventListener(
                this, SensorManager.SENSOR_DELAY_NORMAL) {

            @Override
            public void onOrientationChanged(int orientation) {
                if (!isInCameraMode) return;
                if (orientation == OrientationEventListener.ORIENTATION_UNKNOWN) return;

                int rotation = calculateRotation(orientation);

                if (currentRotation == rotation || rotation == 180/*IGNORING THIS ANGLE*/) return;
                currentRotation = rotation;
                rotateCameraViews();
            }
        };
    }

    private int calculateRotation(int orientation) {
        int degrees = 270;
        if (orientation < 45 || orientation > 315)
            degrees = 0;
        else if (orientation < 135)
            degrees = 90;
        else if (orientation < 225)
            degrees = 180;

        int rotation = (360 - degrees) % 360;
        if (rotation == 270) {
            rotation = -90;
        }

        return rotation;
    }

    private void rotateCameraViews() {
        switchButton.rotateView(currentRotation);
        flashButton.rotateView(currentRotation);
        durationView.rotateView(currentRotation);
        captureButton.rotateView(currentRotation);
        modeButton.rotateView(currentRotation);
    }

    /*private void setMarginsAndPadding() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);

            ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) toolbar.getLayoutParams();
            params.topMargin = ViewUtil.getStatusBarHeight(getResources());

            if (ViewUtil.isNavigationBarAvailable(getResources())) {
                mainView.setPadding(0, 0, 0, ViewUtil.getNavigationBarHeight(getResources()));
            }
        }
    }*/
}
