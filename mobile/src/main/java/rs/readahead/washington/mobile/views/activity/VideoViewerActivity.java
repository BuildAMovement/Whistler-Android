package rs.readahead.washington.mobile.views.activity;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.Toolbar;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelection;
import com.google.android.exoplayer2.ui.PlaybackControlView;
import com.google.android.exoplayer2.ui.SimpleExoPlayerView;
import com.google.android.exoplayer2.upstream.BandwidthMeter;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;

import java.util.Collections;

import butterknife.ButterKnife;
import permissions.dispatcher.NeedsPermission;
import permissions.dispatcher.OnNeverAskAgain;
import permissions.dispatcher.OnPermissionDenied;
import permissions.dispatcher.OnShowRationale;
import permissions.dispatcher.PermissionRequest;
import permissions.dispatcher.RuntimePermissions;
import rs.readahead.washington.mobile.MyApplication;
import rs.readahead.washington.mobile.R;
import rs.readahead.washington.mobile.bus.event.MediaFileDeletedEvent;
import rs.readahead.washington.mobile.domain.entity.MediaFile;
import rs.readahead.washington.mobile.media.MediaFileHandler;
import rs.readahead.washington.mobile.media.exo.ExoEventListener;
import rs.readahead.washington.mobile.media.exo.MediaFileDataSourceFactory;
import rs.readahead.washington.mobile.mvp.contract.IMediaFileViewerPresenterContract;
import rs.readahead.washington.mobile.mvp.presenter.MediaFileViewerPresenter;
import rs.readahead.washington.mobile.presentation.entity.EvidenceData;
import rs.readahead.washington.mobile.presentation.entity.ReportViewType;
import rs.readahead.washington.mobile.util.DialogsUtil;
import rs.readahead.washington.mobile.util.PermissionUtil;

@RuntimePermissions
public class VideoViewerActivity extends CacheWordSubscriberBaseActivity implements
        PlaybackControlView.VisibilityListener,
        IMediaFileViewerPresenterContract.IView {
    public static final String VIEW_VIDEO = "vv";
    public static final String NO_SHARE = "ns";

    public static final int SDK_INT =
            (Build.VERSION.SDK_INT == 25 && Build.VERSION.CODENAME.charAt(0) == 'O') ? 26
                    : Build.VERSION.SDK_INT;

    private SimpleExoPlayerView simpleExoPlayerView;
    private SimpleExoPlayer player;
    private DefaultTrackSelector trackSelector;

    private boolean needRetrySource;
    private boolean shouldAutoPlay;
    private int resumeWindow;
    private long resumePosition;

    private MediaFile mediaFile;
    private Toolbar toolbar;
    private boolean actionsDisabled = false;
    private MediaFileViewerPresenter presenter;
    private AlertDialog alertDialog;
    private ProgressDialog progressDialog;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_video_viewer);
        ButterKnife.bind(this);

        if (getIntent().hasExtra(NO_SHARE)) {
            actionsDisabled = true;
        }

        setupToolbar();

        shouldAutoPlay = true;
        clearResumePosition();

        simpleExoPlayerView = findViewById(R.id.player_view);
        simpleExoPlayerView.setControllerVisibilityListener(this);
        simpleExoPlayerView.requestFocus();

        presenter = new MediaFileViewerPresenter(this);
    }

    @Override
    public void onNewIntent(Intent intent) {
        releasePlayer();
        shouldAutoPlay = true;
        clearResumePosition();
        setIntent(intent);
    }

    @Override
    public void onStart() {
        super.onStart();
        if (SDK_INT > 23) {
            initializePlayer();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if ((SDK_INT <= 23 || player == null)) {
            initializePlayer();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (SDK_INT <= 23) {
            releasePlayer();
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (SDK_INT > 23) {
            releasePlayer();
        }
    }

    @Override
    protected void onDestroy() {
        if (alertDialog != null && alertDialog.isShowing()) {
            alertDialog.dismiss();
        }

        hideProgressDialog();

        if (presenter != null) {
            presenter.destroy();
        }

        super.onDestroy();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        VideoViewerActivityPermissionsDispatcher.onRequestPermissionsResult(this, requestCode, grantResults);
    }

    @OnPermissionDenied(Manifest.permission.WRITE_EXTERNAL_STORAGE)
    void onWriteExternalStoragePermissionDenied() {
    }

    @OnNeverAskAgain(Manifest.permission.WRITE_EXTERNAL_STORAGE)
    void onWriteExternalStorageNeverAskAgain() {
    }

    @NeedsPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
    void exportMediaFile() {
        if (mediaFile != null && presenter != null) {
            presenter.exportNewMediaFile(mediaFile);
        }
    }

    @OnShowRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE)
    void showWriteExternalStorageRationale(final PermissionRequest request) {
        alertDialog = PermissionUtil.showRationale(this, request, getString(R.string.ra_media_export_rationale));
    }


    @Override
    public void onMediaExported() {
        showToast(R.string.ra_media_exported);
    }

    @Override
    public void onExportError(Throwable error) {
        showToast(R.string.ra_media_export_error);
    }

    @Override
    public void onExportStarted() {
        progressDialog = DialogsUtil.showProgressDialog(this, getString(R.string.ra_export_media_progress));
    }

    @Override
    public void onExportEnded() {
        hideProgressDialog();
    }

    @Override
    public void onMediaFileDeleted() {
        MyApplication.bus().post(new MediaFileDeletedEvent());
        finish();
    }

    @Override
    public void onMediaFileDeletionError(Throwable throwable) {
        showToast(R.string.ra_media_deleted_error);
    }

    @Override
    public Context getContext() {
        return this;
    }

    @SuppressWarnings("MethodOnlyUsedFromInnerClass")
    private void showExportDialog() {
        alertDialog = DialogsUtil.showExportMediaDialog(this, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                VideoViewerActivityPermissionsDispatcher.exportMediaFileWithCheck(VideoViewerActivity.this);
            }
        });
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        // Show the controls on any key event.
        simpleExoPlayerView.showController();
        // If the event was not handled then see if the player view can handle it as a media key event.
        return super.dispatchKeyEvent(event) || simpleExoPlayerView.dispatchMediaKeyEvent(event);
    }

    private void initializePlayer() {
        boolean needNewPlayer = player == null;

        if (needNewPlayer) {
            BandwidthMeter bandwidthMeter = new DefaultBandwidthMeter();
            TrackSelection.Factory videoTrackSelectionFactory = new AdaptiveTrackSelection.Factory(bandwidthMeter);
            trackSelector = new DefaultTrackSelector(videoTrackSelectionFactory);

            player = ExoPlayerFactory.newSimpleInstance(this, trackSelector);
            player.addListener(new ExoEventListener());
            simpleExoPlayerView.setPlayer(player);

            player.setPlayWhenReady(shouldAutoPlay);
        }

        if (needNewPlayer || needRetrySource) {
            if (getIntent().hasExtra(VIEW_VIDEO) && getIntent().getExtras() != null) {
                MediaFile mediaFile = (MediaFile) getIntent().getExtras().get(VIEW_VIDEO);
                if (mediaFile != null) {
                    this.mediaFile = mediaFile;
                }
            }

            MediaFileDataSourceFactory mediaFileDataSourceFactory = new MediaFileDataSourceFactory(this, mediaFile, null);
            MediaSource mediaSource = new ExtractorMediaSource(
                    MediaFileHandler.getEncryptedUri(this, mediaFile),
                    mediaFileDataSourceFactory,
                    new DefaultExtractorsFactory(),
                    null, null);

            boolean haveResumePosition = resumeWindow != C.INDEX_UNSET;
            if (haveResumePosition) {
                player.seekTo(resumeWindow, resumePosition);
            }
            player.prepare(mediaSource, !haveResumePosition, false);
            needRetrySource = false;
        }
    }

    private void releasePlayer() {
        if (player != null) {
            shouldAutoPlay = player.getPlayWhenReady();
            //updateResumePosition(); // todo: fix source skipping..
            player.release();
            player = null;
            trackSelector = null;
            clearResumePosition();
        }
    }

    /*private void updateResumePosition() {
        resumeWindow = player.getCurrentWindowIndex();
        resumePosition = player.isCurrentWindowSeekable() ? Math.max(0, player.getCurrentPosition())
                : C.TIME_UNSET;
    }*/

    private void clearResumePosition() {
        resumeWindow = C.INDEX_UNSET;
        resumePosition = C.TIME_UNSET;
    }

    @Override
    public void onVisibilityChange(int visibility) {
        toolbar.setVisibility(visibility);
    }

    private void setupToolbar() {
        toolbar = findViewById(R.id.player_toolbar);
        toolbar.setNavigationIcon(R.drawable.ic_arrow_back_white_24dp);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

        if (!actionsDisabled) {
            toolbar.inflateMenu(R.menu.video_view_menu);
            toolbar.getMenu().findItem(R.id.menu_item_share).setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    if (mediaFile != null) {
                        MediaFileHandler.startShareActivity(VideoViewerActivity.this, mediaFile);
                    }
                    return false;
                }
            });
            toolbar.getMenu().findItem(R.id.menu_item_export).setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    if (mediaFile != null) {
                        showExportDialog();
                    }
                    return false;
                }
            });
            toolbar.getMenu().findItem(R.id.menu_item_report).setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    if (mediaFile != null) {
                        shareMediaToReport();
                    }
                    return false;
                }
            });
            toolbar.getMenu().findItem(R.id.menu_item_delete).setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    if (mediaFile != null) {
                        showDeleteMediaDialog();
                    }
                    return false;
                }
            });
        }
    }

    private void showDeleteMediaDialog() {
        alertDialog = new AlertDialog.Builder(this)
                .setTitle(R.string.ra_delete_media)
                .setMessage(R.string.ra_media_will_be_deleted)
                .setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (mediaFile != null && presenter != null) {
                            presenter.deleteMediaFiles(mediaFile);
                        }
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

    private void shareMediaToReport() {
        Intent intent = new Intent(this, NewReportActivity.class);
        intent.putExtra(NewReportActivity.REPORT_VIEW_TYPE, ReportViewType.NEW);
        intent.putExtra(NewReportActivity.MEDIA_FILES_KEY, new EvidenceData(Collections.singletonList(mediaFile)));

        startActivity(intent);
    }

    private void hideProgressDialog() {
        if (progressDialog != null) {
            progressDialog.dismiss();
            progressDialog = null;
        }
    }
}
