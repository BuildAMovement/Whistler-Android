package rs.readahead.washington.mobile.views.activity;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;

import com.github.clans.fab.FloatingActionMenu;

import java.util.List;

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
import rs.readahead.washington.mobile.bus.event.GalleryFlingTopEvent;
import rs.readahead.washington.mobile.bus.event.MediaFileDeletedEvent;
import rs.readahead.washington.mobile.data.database.CacheWordDataSource;
import rs.readahead.washington.mobile.domain.entity.MediaFile;
import rs.readahead.washington.mobile.media.MediaFileHandler;
import rs.readahead.washington.mobile.mvp.contract.IGalleryPresenterContract;
import rs.readahead.washington.mobile.mvp.presenter.GalleryPresenter;
import rs.readahead.washington.mobile.presentation.entity.EvidenceData;
import rs.readahead.washington.mobile.presentation.entity.MediaFileThumbnailData;
import rs.readahead.washington.mobile.presentation.entity.ReportViewType;
import rs.readahead.washington.mobile.util.C;
import rs.readahead.washington.mobile.util.DialogsUtil;
import rs.readahead.washington.mobile.util.FileUtil;
import rs.readahead.washington.mobile.util.PermissionUtil;
import rs.readahead.washington.mobile.views.adapters.GalleryRecycleViewAdapter;
import rs.readahead.washington.mobile.views.custom.GalleryRecyclerView;
import rs.readahead.washington.mobile.views.interfaces.IGalleryMediaHandler;
import timber.log.Timber;


@RuntimePermissions
public class GalleryActivity extends MetadataActivity implements
        IGalleryPresenterContract.IView,
        IGalleryMediaHandler {
    public static final String SELECTED_MEDIA_FILES = "smf";
    public static final String GALLERY_MODE = "gm";
    public static final String GALLERY_ANIMATED = "ga";
    private boolean animated = false;

    enum Mode {
        GALLERY_SELECT_MEDIA,
        GALLERY_VIEW_MEDIA
    }

    @BindView(R.id.galleryRecyclerView)
    GalleryRecyclerView recyclerView;
    @BindView(R.id.attachButton)
    Button attachButton;
    @BindView(R.id.menu)
    FloatingActionMenu fabMenu;
    @BindView(R.id.progressBar)
    ProgressBar progressBar;
    @BindView(R.id.toolbar)
    Toolbar toolbar;

    private GalleryRecycleViewAdapter adapter;
    private GalleryPresenter presenter;
    private CacheWordDataSource cacheWordDataSource;
    private EventCompositeDisposable disposables;


    Mode mode;
    private int selectedNum;
    private AlertDialog alertDialog;
    private ProgressDialog progressDialog;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_gallery);
        ButterKnife.bind(this);

        presenter = new GalleryPresenter(this);

        mode = Mode.GALLERY_VIEW_MEDIA;
        if (getIntent().hasExtra(GALLERY_MODE)) {
            mode = Mode.valueOf(getIntent().getStringExtra(GALLERY_MODE));
        }

        if (getIntent().hasExtra(GALLERY_ANIMATED)) {
            animated = getIntent().getBooleanExtra(GALLERY_ANIMATED, false);
        }

        if (mode == Mode.GALLERY_VIEW_MEDIA) {
            attachButton.setVisibility(View.GONE);
            updateFabMenu();
        } else {
            fabMenu.setVisibility(View.GONE);
            attachButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    setSelectedMediaResult();
                    onBackPressed();
                }
            });
        }

        setupToolbar();
        setupFab();

        cacheWordDataSource = new CacheWordDataSource(this);

        disposables = MyApplication.bus().createCompositeDisposable();
        disposables.wire(GalleryFlingTopEvent.class, new EventObserver<GalleryFlingTopEvent>() {
            @Override
            public void onNext(GalleryFlingTopEvent event) {
                if (animated) {
                    onBackPressed();
                }
            }
        });
        disposables.wire(MediaFileDeletedEvent.class, new EventObserver<MediaFileDeletedEvent>() {
            @Override
            public void onNext(MediaFileDeletedEvent event) {
                showToast(R.string.ra_single_media_deleted_msg);
                presenter.getFiles();
            }
        });

        adapter = new GalleryRecycleViewAdapter(this, this, new MediaFileHandler(cacheWordDataSource), null);
        final RecyclerView.LayoutManager mLayoutManager = new GridLayoutManager(this, 2);
        recyclerView.setLayoutManager(mLayoutManager);
        recyclerView.setAdapter(adapter);

        presenter.getFiles();
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            if (animated) {
                actionBar.setHomeAsUpIndicator(R.drawable.ic_close_white);
            }
        }
    }

    private void setupFab() {
        if (animated) {
            fabMenu.setVisibility(View.GONE);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (mode == Mode.GALLERY_VIEW_MEDIA && selectedNum > 0) {
            getMenuInflater().inflate(R.menu.gallery_menu, menu);
            return true;
        }

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == android.R.id.home) {
            onBackPressed();
            return true;
        }

        if (id == R.id.menu_item_delete) {
            showDeleteMediaDialog();
            return true;
        }

        if (id == R.id.menu_item_share) {
            shareMediaFiles();
            return true;
        }

        if (id == R.id.menu_item_export) {
            showExportDialog();
            return true;
        }

        if (id == R.id.menu_item_report) {
            shareMediaToReport();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (animated) {
            overridePendingTransition(R.anim.slide_in_up, android.R.anim.fade_out);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        startLocationMetadataListening();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (animated) {
            overridePendingTransition(android.R.anim.fade_in, R.anim.slide_out_down);
        }
    }

    @Override
    protected void onStop() {
        stopLocationMetadataListening();
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        if (disposables != null) {
            disposables.dispose();
        }

        cacheWordDataSource.dispose();
        stopPresenter();

        if (alertDialog != null) {
            alertDialog.dismiss();
        }

        hideProgressDialog();

        super.onDestroy();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        GalleryActivityPermissionsDispatcher.onRequestPermissionsResult(this, requestCode, grantResults);
    }

    @OnPermissionDenied(Manifest.permission.WRITE_EXTERNAL_STORAGE)
    void onWriteExternalStoragePermissionDenied() {
    }

    @OnNeverAskAgain(Manifest.permission.WRITE_EXTERNAL_STORAGE)
    void onWriteExternalStorageNeverAskAgain() {
    }

    @NeedsPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
    void exportMediaFiles() {
        List<MediaFile> selected = adapter.getSelectedMediaFiles();
        presenter.exportMediaFiles(selected);
    }

    @OnPermissionDenied({Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO})
    void onCameraAndAudioPermissionDenied() {
    }

    @OnPermissionDenied(Manifest.permission.ACCESS_FINE_LOCATION)
    void onFineLocationPermissionDenied() {
        startAudioRecordActivityWithLocationChecked();
    }

    @OnNeverAskAgain(Manifest.permission.ACCESS_FINE_LOCATION)
    void onFineLocationNeverAskAgain() {
        startAudioRecordActivityWithLocationChecked();
    }

    @OnNeverAskAgain({Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO})
    void onCameraAndAudioNeverAskAgain() {
    }

    @NeedsPermission(Manifest.permission.ACCESS_FINE_LOCATION)
    public void startAudioRecorderActivity() {
        checkLocationSettings(C.START_AUDIO_RECORD, new MetadataActivity.LocationSettingsCheckDoneListener() {
            @Override
            public void onContinue() {
                startAudioRecordActivityWithLocationChecked();
            }
        });
    }

    private void startAudioRecordActivityWithLocationChecked() {
        startActivityForResult(new Intent(this, AudioRecordActivity2.class), C.RECORDED_AUDIO);
    }

    @NeedsPermission({Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO})
    public void startCameraCaptureActivity() {
        checkLocationSettings(C.START_CAMERA_CAPTURE, new LocationSettingsCheckDoneListener() {
            @Override
            public void onContinue() {
                startCameraCaptureActivityWithLocationChecked();
            }
        });
    }

    private void startCameraCaptureActivityWithLocationChecked() {
        Intent intent = new Intent(this, CameraActivity.class);
        intent.putExtra(CameraActivity.CAMERA_MODE, CameraActivity.Mode.PHOTO.name());
        startActivityForResult(intent, C.CAMERA_CAPTURE);
    }

    @OnShowRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE)
    void showWriteExternalStorageRationale(final PermissionRequest request) {
        alertDialog = PermissionUtil.showRationale(this, request, getString(R.string.ra_media_export_rationale));
    }

    @OnShowRationale({Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO})
    void showCameraAndAudioRationale(final PermissionRequest request) {
        alertDialog = PermissionUtil.showRationale(this, request, getString(R.string.ra_camera_rationale));
    }

    @OnShowRationale(Manifest.permission.ACCESS_FINE_LOCATION)
    void showFineLocationRationale(final PermissionRequest request) {
        alertDialog = PermissionUtil.showRationale(
                this, request, getString(R.string.ra_media_location_permissions));
    }

    @OnClick({R.id.camera_capture, R.id.record_audio, R.id.import_photo_from_device,
            R.id.import_video_from_device, R.id.import_media_from_device})
    public void handleFabClick(View view) {
        int id = view.getId();

        fabMenu.close(false);

        switch (id) {
            case R.id.camera_capture:
                GalleryActivityPermissionsDispatcher.startCameraCaptureActivityWithCheck(this);
                break;

            case R.id.record_audio:
                GalleryActivityPermissionsDispatcher.startAudioRecorderActivityWithCheck(this);
                break;

            case R.id.import_photo_from_device:
                MediaFileHandler.startSelectMediaActivity(this, "image/*", null, C.IMPORT_IMAGE);
                break;

            case R.id.import_video_from_device:
                MediaFileHandler.startSelectMediaActivity(this, "video/mp4", null, C.IMPORT_VIDEO);
                break;

            case R.id.import_media_from_device:
                MediaFileHandler.startSelectMediaActivity(this, "image/*",
                        new String[]{"image/*", "video/mp4"}, C.IMPORT_MEDIA);
                break;
        }
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
            case C.IMPORT_IMAGE:
                Uri image = data.getData();
                if (image != null) {
                    presenter.importImage(image);
                }
                break;

            case C.IMPORT_VIDEO:
                Uri video = data.getData();
                if (video != null) {
                    presenter.importVideo(video);
                }
                break;

            case C.IMPORT_MEDIA:
                Uri media = data.getData();
                if (media == null) break;

                String type = FileUtil.getPrimaryMime(getContentResolver().getType(media));

                if ("image".equals(type)) {
                    presenter.importImage(media);
                } else if ("video".equals(type)) {
                    presenter.importVideo(media);
                }
                break;

            case C.START_CAMERA_CAPTURE:
                startCameraCaptureActivityWithLocationChecked();
                break;

            case C.CAMERA_CAPTURE:
            case C.RECORDED_AUDIO:
                presenter.getFiles();
                break;
        }
    }

    @Override
    public void onGetFilesStart() {
    }

    @Override
    public void onGetFilesEnd() {
    }

    @Override
    public void onGetFilesSuccess(List<MediaFile> files) {
        adapter.setFiles(files);
    }

    @Override
    public void onGetFilesError(Throwable error) {
    }

    @Override
    public void onMediaImported(MediaFile mediaFile, MediaFileThumbnailData thumbnailData) {
        presenter.addNewMediaFile(mediaFile, thumbnailData);
    }

    @Override
    public void onImportError(Throwable error) {
        showToast(R.string.ra_import_media_error);
        Timber.d(error, getClass().getName());
    }

    @Override
    public void onImportStarted() {
        progressDialog = DialogsUtil.showProgressDialog(this, getString(R.string.ra_import_media_progress));
        fabMenu.setVisibility(View.INVISIBLE);
    }

    @Override
    public void onImportEnded() {
        hideProgressDialog();
        fabMenu.setVisibility(View.VISIBLE);
    }

    @Override
    public void onMediaFilesAdded(MediaFile mediaFile) {
        showToast(R.string.ra_media_added_to_gallery);
        presenter.getFiles();
    }

    @Override
    public void onMediaFilesAddingError(Throwable error) {
        Timber.d(error, getClass().getName());
    }

    @Override
    public void onMediaFilesDeleted(int num) {
        showToast(String.format(getString(R.string.ra_media_deleted_msg), num));
        presenter.getFiles();
    }

    @Override
    public void onMediaFilesDeletionError(Throwable throwable) {
        showToast(R.string.ra_media_deleted_error);
    }

    @Override
    public void onMediaExported(int num) {
        showToast(String.format(getString(R.string.ra_media_export_msg), num));
    }

    @Override

    public void onExportError(Throwable error) {
        showToast(R.string.ra_media_export_error);
        Timber.d(error, getClass().getName());
    }

    @Override
    public void onExportStarted() {
        progressDialog = DialogsUtil.showProgressDialog(this, getString(R.string.ra_export_media_progress));
        fabMenu.setVisibility(View.INVISIBLE);
    }

    @Override
    public void onExportEnded() {
        onImportEnded();
    }

    /*@Override
    public void onTmpVideoEncrypted(MediaFileBundle mediaFileBundle) {
        presenter.addNewMediaFile(mediaFileBundle.getMediaFile(), mediaFileBundle.getMediaFileThumbnailData());
    }*/

    /*@Override
    public void onTmpVideoEncryptionError(Throwable error) {
        onImportError(error);
    }*/

    @Override
    public Context getContext() {
        return this;
    }

    @Override
    public void playMedia(MediaFile mediaFile) {
        String type = mediaFile.getPrimaryMimeType();

        if ("image".equals(type)) {
            Intent intent = new Intent(this, PhotoViewerActivity.class);
            intent.putExtra(PhotoViewerActivity.VIEW_PHOTO, mediaFile);
            if (mode == Mode.GALLERY_SELECT_MEDIA) {
                intent.putExtra(PhotoViewerActivity.NO_SHARE, mediaFile);
            }
            startActivity(intent);
        } else if ("audio".equals(type)) {
            Intent intent = new Intent(this, AudioPlayActivity.class);
            intent.putExtra(AudioPlayActivity.PLAY_MEDIA_FILE_ID_KEY, mediaFile.getId());
            if (mode == Mode.GALLERY_SELECT_MEDIA) {
                intent.putExtra(AudioPlayActivity.NO_SHARE, mediaFile);
            }
            startActivity(intent);
        } else if ("video".equals(type)) {
            Intent intent = new Intent(this, VideoViewerActivity.class);
            intent.putExtra(VideoViewerActivity.VIEW_VIDEO, mediaFile);
            if (mode == Mode.GALLERY_SELECT_MEDIA) {
                intent.putExtra(VideoViewerActivity.NO_SHARE, mediaFile);
            }
            startActivity(intent);
        }
    }

    @Override
    public void onSelectionNumChange(int num) {
        boolean current = selectedNum > 0, next = num > 0;
        selectedNum = num;

        if (getSupportActionBar() != null) {
            if (selectedNum > 0) {
                getSupportActionBar().setTitle(String.valueOf(selectedNum));
            } else {
                getSupportActionBar().setTitle(R.string.ra_gallery);
            }
        }

        if (current != next) {
            invalidateOptionsMenu();
            attachButton.setEnabled(next);
        }
    }

    private void showExportDialog() {
        alertDialog = DialogsUtil.showExportMediaDialog(this, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                GalleryActivityPermissionsDispatcher.exportMediaFilesWithCheck(GalleryActivity.this);
            }
        });
    }

    private void showDeleteMediaDialog() {
        alertDialog = new AlertDialog.Builder(this)
                .setTitle(R.string.ra_delete_media)
                .setMessage(R.string.ra_media_will_be_deleted)
                .setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        removeMediaFiles();
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                })
                .setCancelable(true)
                .show();
    }

    @SuppressWarnings("MethodOnlyUsedFromInnerClass")
    private void removeMediaFiles() {
        List<MediaFile> selected = adapter.getSelectedMediaFiles();
        adapter.clearSelected();
        presenter.deleteMediaFiles(selected);
    }

    private void shareMediaFiles() {
        List<MediaFile> selected = adapter.getSelectedMediaFiles();
        MediaFileHandler.startShareActivity(this, selected);
    }

    @SuppressWarnings("MethodOnlyUsedFromInnerClass")
    private void setSelectedMediaResult() {
        Intent data = new Intent();

        if (mode == Mode.GALLERY_SELECT_MEDIA) {
            List<MediaFile> selected = adapter.getSelectedMediaFiles();
            adapter.clearSelected();

            if (selected.size() > 0) {
                long[] ids = new long[selected.size()];
                int i = 0;

                for (MediaFile mediaFile : selected) {
                    ids[i++] = mediaFile.getId();
                }

                data.putExtra(SELECTED_MEDIA_FILES, ids);
            }
        }

        setResult(RESULT_OK, data);
    }

    private void shareMediaToReport() {
        Intent intent = new Intent(this, NewReportActivity.class);
        intent.putExtra(NewReportActivity.REPORT_VIEW_TYPE, ReportViewType.NEW);
        if (selectedNum > 0) {
            intent.putExtra(NewReportActivity.MEDIA_FILES_KEY,
                    new EvidenceData(adapter.getSelectedMediaFiles()));
        }
        startActivity(intent);
    }

    private void stopPresenter() {
        if (presenter != null) {
            presenter.destroy();
        }
    }

    private void hideProgressDialog() {
        if (progressDialog != null) {
            progressDialog.dismiss();
            progressDialog = null;
        }
    }

    private void updateFabMenu() {
        boolean media = Build.VERSION.SDK_INT >= 19;

        fabMenu.findViewById(R.id.import_photo_from_device).setVisibility(media ? View.GONE : View.VISIBLE);
        fabMenu.findViewById(R.id.import_video_from_device).setVisibility(media ? View.GONE : View.VISIBLE);
        fabMenu.findViewById(R.id.import_media_from_device).setVisibility(media ? View.VISIBLE : View.GONE);

        fabMenu.setClosedOnTouchOutside(true);
    }
}
