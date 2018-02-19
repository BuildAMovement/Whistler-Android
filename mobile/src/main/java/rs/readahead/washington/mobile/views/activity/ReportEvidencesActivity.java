package rs.readahead.washington.mobile.views.activity;

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.github.clans.fab.FloatingActionMenu;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.reactivex.disposables.CompositeDisposable;
import permissions.dispatcher.NeedsPermission;
import permissions.dispatcher.OnNeverAskAgain;
import permissions.dispatcher.OnPermissionDenied;
import permissions.dispatcher.OnShowRationale;
import permissions.dispatcher.PermissionRequest;
import permissions.dispatcher.RuntimePermissions;
import rs.readahead.washington.mobile.R;
import rs.readahead.washington.mobile.data.database.CacheWordDataSource;
import rs.readahead.washington.mobile.domain.entity.MediaFile;
import rs.readahead.washington.mobile.media.MediaFileBundle;
import rs.readahead.washington.mobile.media.MediaFileHandler;
import rs.readahead.washington.mobile.mvp.contract.IReportEvidencesPresenterContract;
import rs.readahead.washington.mobile.mvp.presenter.ReportEvidencesPresenter;
import rs.readahead.washington.mobile.presentation.entity.EvidenceData;
import rs.readahead.washington.mobile.presentation.entity.ReportViewType;
import rs.readahead.washington.mobile.util.C;
import rs.readahead.washington.mobile.util.DialogsUtil;
import rs.readahead.washington.mobile.util.FileUtil;
import rs.readahead.washington.mobile.util.PermissionUtil;
import rs.readahead.washington.mobile.views.adapters.GalleryRecycleViewAdapter;
import rs.readahead.washington.mobile.views.interfaces.IGalleryMediaHandler;
import timber.log.Timber;


@RuntimePermissions
public class ReportEvidencesActivity extends MetadataActivity implements
        IReportEvidencesPresenterContract.IView,
        IGalleryMediaHandler {
    public static final String MEDIA_FILES_KEY = "mfk";
    public static final String REPORT_VIEW_TYPE = "type";

    @BindView(R.id.toolbar) Toolbar toolbar;
    @BindView(R.id.attachmentsRecyclerView) RecyclerView recyclerView;
    @BindView(R.id.menu) FloatingActionMenu fabMenu;
    @BindView(R.id.noAttachments) TextView noAttachmentsText;
    @BindView(R.id.progressBar) ProgressBar progressBar;

    private CacheWordDataSource cacheWordDataSource;
    private ReportEvidencesPresenter presenter;
    private GalleryRecycleViewAdapter adapter;
    private CompositeDisposable disposables;
    private MenuItem mSelectMenuItem;

    // acquiring media file
    private AlertDialog alertDialog;
    private ProgressDialog progressDialog;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_report_evidences);
        ButterKnife.bind(this);
        setToolbar();
        startPresenter();

        cacheWordDataSource = new CacheWordDataSource(this);
        adapter = new GalleryRecycleViewAdapter(this, this,
                new MediaFileHandler(cacheWordDataSource), presenter.getReportType());

        RecyclerView.LayoutManager mLayoutManager = new GridLayoutManager(this, 2);
        recyclerView.setLayoutManager(mLayoutManager);
        recyclerView.setAdapter(adapter);

        disposables = new CompositeDisposable();

        getEvidenceData();
        updateFabMenu();
    }

    private void setToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_close_white);
        }
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        mSelectMenuItem = menu.findItem(R.id.menu_item_select);
        setSelectVisible(false);
        return super.onPrepareOptionsMenu(menu);
    }

    public void setSelectVisible(boolean visible) {
        mSelectMenuItem.setVisible(visible);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.recipients_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == android.R.id.home) {
            onBackPressed();
            return true;
        }

        if (id == R.id.menu_item_select) {
            onEvidenceSelected();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onStart() {
        super.onStart();
        startLocationMetadataListening();
    }

    @Override
    protected void onResume() {
        super.onResume();
        overridePendingTransition(R.anim.slide_in_up, android.R.anim.fade_out);
    }

    @Override
    protected void onPause() {
        super.onPause();
        overridePendingTransition(android.R.anim.fade_in, R.anim.slide_out_down);
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

        if (alertDialog != null) {
            alertDialog.dismiss();
        }

        hideProgressDialog();

        cacheWordDataSource.dispose();

        destroyPresenter();

        super.onDestroy();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        ReportEvidencesActivityPermissionsDispatcher.onRequestPermissionsResult(this, requestCode, grantResults);
    }

    @OnPermissionDenied({Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO})
    void onCameraAndAudioPermissionDenied() {
    }

    @OnNeverAskAgain({Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO})
    void onCameraAndAudioNeverAskAgain() {
    }

    @NeedsPermission({Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO})
    public void startCameraCaptureActivity() {
        Intent intent = new Intent(this, CameraActivity.class);
        intent.putExtra(CameraActivity.CAMERA_MODE, CameraActivity.Mode.PHOTO.name());
        startActivityForResult(intent, C.CAMERA_CAPTURE);
    }

    @OnShowRationale({Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO})
    void showCameraAndAudioRationale(final PermissionRequest request) {
        alertDialog = PermissionUtil.showRationale(this, request, getString(R.string.ra_camera_rationale)); // todo: +audio?
    }

    @OnClick(R.id.camera_capture)
    public void recordVideoClick(View view) {
        fabMenu.close(true);
        ReportEvidencesActivityPermissionsDispatcher.startCameraCaptureActivityWithCheck(this);
    }

    @OnClick(R.id.record_audio)
    public void recordAudioClick(View view) {
        fabMenu.close(true);
        startActivityForResult(new Intent(this, AudioRecordActivity2.class), C.RECORDED_AUDIO);
    }

    @OnClick(R.id.choose_from_gallery)
    public void chooseFromGalleryClick(View view) {
        fabMenu.close(true);
        Intent intent = new Intent(this, GalleryActivity.class);
        intent.putExtra(GalleryActivity.GALLERY_MODE, GalleryActivity.Mode.GALLERY_SELECT_MEDIA.name());
        startActivityForResult(intent, C.SELECT_MEDIA_FROM_GALLERY);
    }

    @OnClick(R.id.import_photo_from_device)
    public void importPhotoClick(View view) {
        fabMenu.close(true);
        MediaFileHandler.startSelectMediaActivity(this, "image/*", null, C.IMPORT_IMAGE);
    }

    @OnClick(R.id.import_video_from_device)
    public void importVideoClick(View view) {
        fabMenu.close(true);
        MediaFileHandler.startSelectMediaActivity(this, "video/mp4", null, C.IMPORT_VIDEO);
    }

    @OnClick(R.id.import_media_from_device)
    public void importMediaClick(View view) {
        fabMenu.close(true);
        MediaFileHandler.startSelectMediaActivity(this, "image/*",
                new String[] {"image/*", "video/mp4"}, C.IMPORT_MEDIA);
    }

    @Override
    public void onEvidencesAttached(List<MediaFile> mediaFile) {
        showToast(getString(R.string.ra_media_attached_to_report));
        setSelectVisible(true);
        updateEvidencesGrid();
    }

    @Override
    public void onEvidencesAttachedError(Throwable error) {
        showToast(R.string.ra_media_form_attach_error);
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
    public void hideFAB() {
        fabMenu.setVisibility(View.GONE);
    }

    @Override
    public void onEvidenceImported(MediaFileBundle mediaFileBundle) {
        presenter.attachNewEvidence(mediaFileBundle);
    }

    @Override
    public void onImportError(Throwable error) {
        showToast(R.string.ra_import_media_error);
        Timber.d(error, getClass().getName());
    }

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
            intent.putExtra(PhotoViewerActivity.NO_SHARE, true);
            startActivity(intent);
        } else if ("audio".equals(type)) {
            Intent intent = new Intent(this, AudioPlayActivity.class);
            intent.putExtra(AudioPlayActivity.PLAY_MEDIA_FILE_ID_KEY, mediaFile.getId());
            intent.putExtra(AudioPlayActivity.NO_SHARE, true);
            startActivity(intent);
        } else if ("video".equals(type)) {
            Intent intent = new Intent(this, VideoViewerActivity.class);
            intent.putExtra(VideoViewerActivity.VIEW_VIDEO, mediaFile);
            intent.putExtra(VideoViewerActivity.NO_SHARE, true);
            startActivity(intent);
        }
    }

    @Override
    public void onSelectionNumChange(int num) {
        showRemoveAttachmentsDialog();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode != RESULT_OK) {
            return;
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

            case C.SELECT_MEDIA_FROM_GALLERY:
                if (data != null) {
                    long[] mediaFileIds = data.getLongArrayExtra(GalleryActivity.SELECTED_MEDIA_FILES);
                    if (mediaFileIds != null && presenter != null) {
                        presenter.attachRegisteredEvidences(mediaFileIds);
                    }
                }
                break;

            case C.CAMERA_CAPTURE:
            case C.RECORDED_AUDIO:
                if (data == null) break;

                long mediaFileId = data.getLongExtra(C.CAPTURED_MEDIA_FILE_ID, 0);
                if (mediaFileId == 0) break;

                presenter.attachRegisteredEvidences(new long[] {mediaFileId});

                break;
        }
    }

    private void updateEvidencesGrid() {
        List<MediaFile> evidences = presenter.getEvidences();

        noAttachmentsText.setText(!evidences.isEmpty() ?
                getString(presenter.isInPreviewMode() ? R.string.ra_multimedia_attached_to_evidence_preview : R.string.ra_multimedia_attached_to_evidence) :
                getString(presenter.isInPreviewMode() ? R.string.ra_no_multimedia_attached_to_this_report_preview : R.string.ra_no_multimedia_attached_to_this_report));

        if (presenter != null) {
            adapter.setFiles(evidences);
        }
    }

    private void showRemoveAttachmentsDialog() {
        alertDialog = new AlertDialog.Builder(this)
                .setTitle(R.string.ra_remove_evidence_att_dialog_title)
                .setMessage(R.string.ra_remove_evidence_att_dialog_message)
                .setPositiveButton(R.string.ra_remove, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        removeAttachment();
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        adapter.clearSelectedEvidence();
                    }
                })
                .setCancelable(true)
                .show();
    }

    @SuppressWarnings("MethodOnlyUsedFromInnerClass")
    private void removeAttachment() {
        List<MediaFile> selected = adapter.getSelectedMediaFiles();
        adapter.clearSelectedEvidence();

        for (MediaFile mediaFile: selected) {
            adapter.removeMediaFile(mediaFile);
        }

        if (adapter.getItemCount() == 0) {
            noAttachmentsText.setText(getString(R.string.ra_no_multimedia_attached_to_this_report));
        }

        setSelectVisible(true);
    }


    private void startPresenter() {
        presenter = new ReportEvidencesPresenter(this);
        presenter.setReportType((ReportViewType) getIntent().getSerializableExtra(REPORT_VIEW_TYPE));
    }

    // get initial list from intent
    private void getEvidenceData() {
        if (getIntent().hasExtra(MEDIA_FILES_KEY)) {
            presenter.setEvidences((EvidenceData) getIntent().getSerializableExtra(MEDIA_FILES_KEY));
        }

        updateEvidencesGrid();
    }

    private void destroyPresenter() {
        if (presenter != null) {
            presenter.destroy();
        }
        presenter = null;
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

    private void onEvidenceSelected() {
        setResult(Activity.RESULT_OK, new Intent().putExtra(MEDIA_FILES_KEY, adapter.getEvidenceFiles()));
        finish();
    }
}
