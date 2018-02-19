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
import rs.readahead.washington.mobile.media.MediaFileHandler;
import rs.readahead.washington.mobile.mvp.contract.ICollectFormAttachmentsPresenterContract;
import rs.readahead.washington.mobile.mvp.presenter.CollectFormAttachmentsPresenter;
import rs.readahead.washington.mobile.presentation.entity.MediaFileThumbnailData;
import rs.readahead.washington.mobile.util.C;
import rs.readahead.washington.mobile.util.DialogsUtil;
import rs.readahead.washington.mobile.util.FileUtil;
import rs.readahead.washington.mobile.util.PermissionUtil;
import rs.readahead.washington.mobile.views.adapters.GalleryRecycleViewAdapter;
import rs.readahead.washington.mobile.views.interfaces.IGalleryMediaHandler;
import timber.log.Timber;


@RuntimePermissions
public class CollectFormAttachmentsActivity extends MetadataActivity implements
        ICollectFormAttachmentsPresenterContract.IView,
        IGalleryMediaHandler {
    @BindView(R.id.attachmentsRecyclerView)
    RecyclerView recyclerView;
    @BindView(R.id.menu)
    FloatingActionMenu fabMenu;
    @BindView(R.id.noAttachments)
    TextView noAttachmentsText;
    @BindView(R.id.progressBar)
    ProgressBar progressBar;

    private CacheWordDataSource cacheWordDataSource;
    private CollectFormAttachmentsPresenter presenter;
    private GalleryRecycleViewAdapter adapter;
    private CompositeDisposable disposables;

    // acquiring media file
    private AlertDialog alertDialog;
    private ProgressDialog progressDialog;

    private int selectedNum;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_collect_form_attachments);
        ButterKnife.bind(this);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        cacheWordDataSource = new CacheWordDataSource(this);
        adapter = new GalleryRecycleViewAdapter(this, this, new MediaFileHandler(cacheWordDataSource), null);

        RecyclerView.LayoutManager mLayoutManager = new GridLayoutManager(this, 2);
        recyclerView.setLayoutManager(mLayoutManager);
        recyclerView.setAdapter(adapter);

        disposables = new CompositeDisposable();

        updateFabMenu();

        startPresenter();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (selectedNum > 0) {
            getMenuInflater().inflate(R.menu.collect_form_attachments_menu, menu);
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == android.R.id.home) {
            onBackPressed();
            return true;
        }

        if (id == R.id.removeAttachment) {
            showRemoveAttachmentsDialog();
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
        CollectFormAttachmentsActivityPermissionsDispatcher.onRequestPermissionsResult(this, requestCode, grantResults);
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
        CollectFormAttachmentsActivityPermissionsDispatcher.startCameraCaptureActivityWithCheck(this);
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
    public void onActiveFormInstanceMediaFiles(List<MediaFile> mediaFiles) {
        noAttachmentsText.setText(mediaFiles.size() > 0 ?
                getString(R.string.ra_multimedia_attached_to_form) :
                getString(R.string.ra_no_multimedia_attached_to_this_form));
        adapter.setFiles(mediaFiles);
    }

    @Override
    public void onMediaFilesAttached(List<MediaFile> mediaFile) {
        showToast(getString(R.string.ra_media_attached_to_form));
        presenter.getActiveFormInstanceAttachments();
    }

    @Override
    public void onMediaFilesAttachedError(Throwable error) {
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
    public void onMediaImported(MediaFile mediaFile, MediaFileThumbnailData thumbnailData) {
        presenter.attachNewMediaFile(mediaFile, thumbnailData);
    }

    @Override
    public void onImportError(Throwable error) {
        showToast(R.string.ra_import_media_error);
        Timber.d(error, getClass().getName());
    }

    /*@Override
    public void onTmpVideoEncrypted(MediaFileBundle mediaFileBundle) {
        presenter.attachNewMediaFile(mediaFileBundle.getMediaFile(), mediaFileBundle.getMediaFileThumbnailData());
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
        boolean current = selectedNum > 0, next = num > 0;
        selectedNum = num;

        if (current != next) {
            invalidateOptionsMenu();
        }
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
                        presenter.attachRegisteredMediaFiles(mediaFileIds);
                    }
                }
                break;

            case C.CAMERA_CAPTURE:
            case C.RECORDED_AUDIO:
                if (data == null) break;

                long mediaFileId = data.getLongExtra(C.CAPTURED_MEDIA_FILE_ID, 0);
                if (mediaFileId == 0) break;

                presenter.attachRegisteredMediaFiles(new long[] {mediaFileId});

                break;
        }
    }

    private void showRemoveAttachmentsDialog() {
        alertDialog = new AlertDialog.Builder(this)
                .setTitle(R.string.ra_remove_gallery_att_dialog_title)
                .setMessage(R.string.ra_remove_gallery_att_dialog_message)
                .setPositiveButton(R.string.ra_remove, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        removeAttachments();
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
    private void removeAttachments() {
        List<MediaFile> selected = adapter.getSelectedMediaFiles();
        adapter.clearSelected();

        for (MediaFile mediaFile: selected) {
            adapter.removeMediaFile(mediaFile);
        }

        if (adapter.getItemCount() == 0) {
            noAttachmentsText.setVisibility(View.VISIBLE);
        }
    }

    private void startPresenter() {
        destroyPresenter();
        presenter = new CollectFormAttachmentsPresenter(this);
        presenter.getActiveFormInstanceAttachments();
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
}
