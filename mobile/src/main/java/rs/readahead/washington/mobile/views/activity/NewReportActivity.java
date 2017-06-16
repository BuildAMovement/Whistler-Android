package rs.readahead.washington.mobile.views.activity;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TabLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NavUtils;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.jakewharton.rxrelay2.PublishRelay;
import com.jakewharton.rxrelay2.Relay;

import java.io.File;
import java.util.List;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;
import info.guardianproject.cacheword.CacheWordHandler;
import info.guardianproject.cacheword.ICacheWordSubscriber;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Action;
import io.reactivex.functions.Consumer;
import io.reactivex.observers.DisposableObserver;
import permissions.dispatcher.NeedsPermission;
import permissions.dispatcher.OnShowRationale;
import permissions.dispatcher.PermissionRequest;
import permissions.dispatcher.RuntimePermissions;
import rs.readahead.washington.mobile.MyApplication;
import rs.readahead.washington.mobile.R;
import rs.readahead.washington.mobile.bus.EventCompositeDisposable;
import rs.readahead.washington.mobile.bus.EventObserver;
import rs.readahead.washington.mobile.bus.event.EvidenceAcquiredEvent;
import rs.readahead.washington.mobile.bus.event.EvidenceAttachLocationMetadataEvent;
import rs.readahead.washington.mobile.bus.event.EvidenceReadyEvent;
import rs.readahead.washington.mobile.bus.event.RefreshEvidenceListEvent;
import rs.readahead.washington.mobile.data.repository.ReportRepository;
import rs.readahead.washington.mobile.database.DataSource;
import rs.readahead.washington.mobile.domain.entity.Evidence;
import rs.readahead.washington.mobile.domain.entity.IErrorBundle;
import rs.readahead.washington.mobile.domain.entity.Metadata;
import rs.readahead.washington.mobile.domain.interactor.CreateReportUseCase;
import rs.readahead.washington.mobile.models.MediaRecipient;
import rs.readahead.washington.mobile.models.MediaRecipientList;
import rs.readahead.washington.mobile.models.Report;
import rs.readahead.washington.mobile.mvp.presenter.SendReportPresenter;
import rs.readahead.washington.mobile.mvp.view.ISendReportView;
import rs.readahead.washington.mobile.queue.EvidenceQueue;
import rs.readahead.washington.mobile.util.C;
import rs.readahead.washington.mobile.util.DialogsUtil;
import rs.readahead.washington.mobile.util.FileUtil;
import rs.readahead.washington.mobile.util.PermissionHandler;
import rs.readahead.washington.mobile.util.RealPathUtil;
import rs.readahead.washington.mobile.util.TelephonyUtils;
import rs.readahead.washington.mobile.views.adapters.ViewPagerAdapter;
import rs.readahead.washington.mobile.views.fragment.IReportWizardHandler;
import rs.readahead.washington.mobile.views.fragment.ReportFragmentStep0;
import rs.readahead.washington.mobile.views.fragment.ReportFragmentStep1;
import rs.readahead.washington.mobile.views.fragment.ReportFragmentStep2;
import rs.readahead.washington.mobile.views.fragment.ReportFragmentStep3;


@RuntimePermissions
public class NewReportActivity extends MetadataActivity implements
        IReportWizardHandler,
        ReportFragmentStep0.OnFragmentInteractionListener, ICacheWordSubscriber,
        ReportFragmentStep3.OnSendInteractionListener,
        ReportFragmentStep0.OnRemoveEvidenceInteractionListener,
        ISendReportView {

    @BindView(R.id.toolbar) Toolbar toolbar;
    @BindView(R.id.pager) ViewPager viewPager;
    @BindView(R.id.tabs) TabLayout mTabLayout;

    private static final String BUNDLE_POSITION = "position";
    private static final String BUNDLE_REPORT = "report";
    public static final String REPORT_KEY = "report";

    private String mFilePath;
    private Uri mFileUri;
    private Report mReport;
    private CacheWordHandler mCacheWord;
    private DataSource dataSource;
    private ProgressDialog sendingProgressDialog;
    private AlertDialog metadataProgressDialog;
    private SendReportPresenter presenter;
    private AlertDialog rationaleDialog;
    private AlertDialog backDialog;
    private int mPosition;
    private EventCompositeDisposable disposables;
    private Relay<MetadataHolder> metadataCancelRelay;
    private boolean locationMetadataEnabled = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_make_oppression_backfire);
        ButterKnife.bind(this);
        setSupportActionBar(toolbar);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayShowHomeEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        FileUtil.checkForMainFolder();

        if (savedInstanceState != null) {
            mPosition = savedInstanceState.getInt(BUNDLE_POSITION, 0);
            mReport = (Report) savedInstanceState.getSerializable(BUNDLE_REPORT);
        } else {
            Bundle bundle = getIntent().getExtras();
            if (bundle != null) {
                mReport = (Report) bundle.getSerializable(REPORT_KEY);
                if (mReport != null) {
                    mReport.generateSelectedRecipients(); // loaded from db..
                }
            }
        }

        // no report in intent or bundle, start fresh one..
        if (mReport == null) {
            mReport = new Report();
        }

        mFilePath = "";
        mCacheWord = new CacheWordHandler(this);

        metadataCancelRelay = PublishRelay.create();

        disposables = MyApplication.bus().createCompositeDisposable();
        disposables.wire(EvidenceAcquiredEvent.class, new EventObserver<EvidenceAcquiredEvent>() {
                    @Override
                    public void onNext(EvidenceAcquiredEvent value) {
                        onEvidenceAcquiredEvent(value.getEvidence());
                    }
                })
                .wire(EvidenceReadyEvent.class, new EventObserver<EvidenceReadyEvent>() {
                    @Override
                    public void onNext(EvidenceReadyEvent value) {
                        onEvidenceReadyEvent(value.getEvidence());
                    }
                })
                .wire(EvidenceAttachLocationMetadataEvent.class, new EventObserver<EvidenceAttachLocationMetadataEvent>() {
                    @Override
                    public void onNext(EvidenceAttachLocationMetadataEvent value) {
                        onEvidenceAttachLocationMetadataEvent(value.getEvidence());
                    }
                });
    }

    @Override
    protected void onResume() {
        super.onResume();
        mCacheWord.connectToService();
    }

    @Override
    protected void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        savedInstanceState.putInt(BUNDLE_POSITION, viewPager.getCurrentItem());
        savedInstanceState.putSerializable(BUNDLE_REPORT, mReport);
    }

    public Report getReport() {
        return mReport;
    }

    public DataSource getDatabase() {
        return dataSource;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.oppression_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.save_to_drafts) {
            persistReportDraft();
            return true;
        }

        if (id == android.R.id.home) {
            showBackDialog();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void persistReportDraft() {
        onRecipientSelectionChanged(); // draft save can happen anytime, so we need this..

        if (mReport.isKeptInDrafts()) {
            dataSource.updateReport(mReport);
        } else {
            mReport.setKeptInDrafts(true);
            dataSource.insertReport(mReport);
        }

        Snackbar.make(toolbar, getString(R.string.saved_to_drafts), Snackbar.LENGTH_SHORT).show();
    }

    private void setFragments() {
        ViewPagerAdapter adapter = new ViewPagerAdapter(getSupportFragmentManager());
        adapter.addFragment(new ReportFragmentStep0(), getString(R.string.evidence));
        adapter.addFragment(ReportFragmentStep1.newInstance(), getString(R.string.recipients));
        adapter.addFragment(new ReportFragmentStep2(), getString(R.string.details));
        adapter.addFragment(new ReportFragmentStep3(), getString(R.string.send));

        viewPager.setAdapter(adapter);
        mTabLayout.setupWithViewPager(viewPager);
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavUtils.navigateUpFromSameTask(this);
        return true;
    }

    @Override
    public void onBackPressed() {
        if (viewPager.getCurrentItem() == 0) {
            //super.onBackPressed();
            showBackDialog();
        } else {
            viewPager.setCurrentItem(viewPager.getCurrentItem() - 1);
        }
    }

    @Override
    public void onFragmentInteraction(int caller) {
        switch (caller) {
            case Evidence.CAPTURED_IMAGE:
                NewReportActivityPermissionsDispatcher.dispatchTakePictureIntentWithCheck(this);
                break;
            case Evidence.CAPTURED_VIDEO:
                NewReportActivityPermissionsDispatcher.dispatchTakeVideoIntentWithCheck(this);
                break;
            case Evidence.PICKED_IMAGE:
                NewReportActivityPermissionsDispatcher.dispatchSelectPictureIntentWithCheck(this);
                break;
            case Evidence.RECORDED_AUDIO:
                NewReportActivityPermissionsDispatcher.dispatchStartAudioRecordingIntentWithCheck(this);
                break;
        }
    }

    @Override
    public void onSendInteraction() {
        if (mReport.getRecipients().isEmpty()) {
            Snackbar.make(mTabLayout, getString(R.string.recipient_list_empty), Snackbar.LENGTH_SHORT).show();
            return;
        }

        // update metadata
        filterReportMetadata();

        // send report
        sendReportWithMetadata();
    }

    @Override
    public void onRecipientSelectionChanged() {
        Map<Long, MediaRecipient> calculatedRecipients =
                dataSource.getCombinedMediaRecipients(mReport.getSelectedRecipients(), mReport.getSelectedRecipientLists());
        mReport.setRecipients(calculatedRecipients);
    }

    private void filterReportMetadata() {
        if (! mReport.isMetadataSelected()) {
            for (Evidence evidence: mReport.getEvidences()) {
                evidence.getMetadata().clear();
            }
        }
    }

    @Override
    public void onRemoveEvidenceInteraction(String path) {
        if (mReport.isKeptInArchive() || mReport.isKeptInDrafts()) {
            dataSource.deleteEvidenceByPath(mReport.getId(), path);
        }
    }

    protected void startLocationListeningWrapper() {
        if (startLocationMetadataListening()) {
            locationMetadataEnabled = true;
        }
    }

    protected void stopLocationListeningWrapper() {
        stopLocationMetadataListening();
        locationMetadataEnabled = false;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        NewReportActivityPermissionsDispatcher.onRequestPermissionsResult(this, requestCode, grantResults);
    }

    /*@OnShowRationale(Manifest.permission.ACCESS_FINE_LOCATION)
    void showFineLocationRationale(final PermissionRequest request) {
        rationaleDialog = PermissionHandler.showRationale(this, request, getString(R.string.permission_location_metadata));
    }*/

    @OnShowRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE)
    void showWriteExternalStorageRationale(final PermissionRequest request) {
        rationaleDialog = PermissionHandler.showRationale(this, request, getString(R.string.permission_storage));
    }

    @OnShowRationale({Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.RECORD_AUDIO})
    void showRecordAudioRationale(final PermissionRequest request) {
        rationaleDialog = PermissionHandler.showRationale(this, request, getString(R.string.permission_audio));
    }

    /*@OnPermissionDenied(Manifest.permission.ACCESS_FINE_LOCATION)
    void onFineLocationPermissionDenied() {
    }

    @OnNeverAskAgain(Manifest.permission.ACCESS_FINE_LOCATION)
    void onFineLocationNeverAskAgain() {
    }

    @OnPermissionDenied(Manifest.permission.WRITE_EXTERNAL_STORAGE)
    void onWriteExternalStoragePermissionDenied() {
    }

    @OnNeverAskAgain(Manifest.permission.WRITE_EXTERNAL_STORAGE)
    void onWriteExternalStorageNeverAskAgain() {
    }

    @OnPermissionDenied({Manifest.permission.WRITE_EXTERNAL_STORAGE,Manifest.permission.RECORD_AUDIO})
    void onRecordAudioPermissionDenied() {
    }

    @OnNeverAskAgain({Manifest.permission.WRITE_EXTERNAL_STORAGE,Manifest.permission.RECORD_AUDIO})
    void onRecordAudioNeverAskAgain() {
    }*/

    @NeedsPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
    void dispatchTakePictureIntent() {
        startWifiScan();

        FileUtil.checkFolders(C.FOLDER_IMAGES);

        mFilePath = FileUtil.getFolderPath(C.FOLDER_IMAGES) + "/whistler_" + String.valueOf(System.currentTimeMillis()) + ".jpg";

        mFileUri = Uri.fromFile(new File(mFilePath));

        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        takePictureIntent.putExtra(android.provider.MediaStore.EXTRA_OUTPUT, mFileUri);

        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(takePictureIntent, Evidence.CAPTURED_IMAGE);
        }
    }

    @NeedsPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
    void dispatchTakeVideoIntent() {
        startWifiScan();

        FileUtil.checkFolders(C.FOLDER_VIDEOS);

        mFilePath = FileUtil.getFolderPath(C.FOLDER_VIDEOS) + "/whistler_" + String.valueOf(System.currentTimeMillis()) + ".mp4";

        mFileUri = Uri.fromFile(new File(mFilePath));

        Intent takeVideoIntent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
        takeVideoIntent.putExtra(android.provider.MediaStore.EXTRA_OUTPUT, mFileUri);

        if (takeVideoIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(takeVideoIntent, Evidence.CAPTURED_VIDEO);
        }
    }

    @NeedsPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
    void dispatchSelectPictureIntent() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);

        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(Intent.createChooser(intent, "Select Picture"), Evidence.PICKED_IMAGE);
        }
    }

    @NeedsPermission({Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.RECORD_AUDIO})
    void dispatchStartAudioRecordingIntent() {
        startWifiScan();
        startActivityForResult(new Intent(this, AudioRecordActivity.class), Evidence.RECORDED_AUDIO);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode != RESULT_OK) {
            return;
        }

        // check results..
        switch (requestCode) {
            case Evidence.CAPTURED_IMAGE:
                break;
            case Evidence.CAPTURED_VIDEO:
                break;
            case Evidence.PICKED_IMAGE:
                getRealPathFromURI(data.getData());
                if (mFilePath.length() > 0) {
                    if (mReport.containsEvidence(mFilePath)) {
                        Snackbar.make(mTabLayout, getString(R.string.file_already_added), Snackbar.LENGTH_SHORT).show();
                        return;
                    }
                } else {
                    Snackbar.make(mTabLayout, getString(R.string.file_error), Snackbar.LENGTH_SHORT).show();
                    return;
                }
                break;
            case Evidence.RECORDED_AUDIO:
                mFilePath = data.getStringExtra("uri");
                break;
        }

        Evidence evidence = new Evidence(mFilePath, requestCode);

        MyApplication.bus().post(new EvidenceAcquiredEvent(evidence));
    }

    private void onEvidenceAcquiredEvent(Evidence evidence) {
        if (!evidence.isCreatedInWhistler()) {
            MyApplication.bus().post(new EvidenceReadyEvent(evidence));
            return;
        }

        Metadata metadata = new Metadata();
        setBasicMetadata(metadata);
        setCellsMetadata(metadata);

        evidence.setMetadata(metadata);

        if (!locationMetadataEnabled) {
            MyApplication.bus().post(new EvidenceReadyEvent(evidence));
            return;
        }

        MyApplication.bus().post(new EvidenceAttachLocationMetadataEvent(evidence));
    }

    private void onEvidenceAttachLocationMetadataEvent(final Evidence evidence) {
        final Observable<MetadataHolder> observable = observeMetadata()
                .subscribeOn(AndroidSchedulers.mainThread())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe(new Consumer<Disposable>() {
                    @Override
                    public void accept(Disposable disposable) throws Exception {
                        showMetadataProgressBarDialog();
                    }
                })
                .doFinally(new Action() {
                    @Override
                    public void run() throws Exception { // needs to be thread safe
                        hideMetadataProgressBarDialog();
                    }
                });

        final DisposableObserver<MetadataHolder> disposable = observable
                .takeUntil(metadataCancelRelay) // this observable emits when user press skip in dialog.
                .subscribeWith(new DisposableObserver<MetadataHolder>() {
                    @Override
                    public void onNext(MetadataHolder value) {
                        // onComplete or onError passes evidence on
                        if (!value.getWifis().isEmpty()) {
                            evidence.getMetadata().setWifis(value.getWifis());
                        }

                        if (!value.getLocation().isEmpty()) {
                            evidence.getMetadata().setEvidenceLocation(value.getLocation());
                        }
                    }

                    @Override
                    public void onError(Throwable e) {
                        MyApplication.bus().post(new EvidenceReadyEvent(evidence));
                    }

                    @Override
                    public void onComplete() {
                        MyApplication.bus().post(new EvidenceReadyEvent(evidence));
                    }
                });

        disposables.add(disposable);
    }

    private void onEvidenceReadyEvent(Evidence evidence) {
        mReport.addEvidence(evidence);
        viewPager.getAdapter().notifyDataSetChanged();

        Snackbar.make(viewPager, R.string.evidence_selected, Snackbar.LENGTH_SHORT).show();

        MyApplication.bus().post(new RefreshEvidenceListEvent());
    }

    private void setBasicMetadata(Metadata metadata) {
        metadata.setTimestamp(System.currentTimeMillis() / 1000L);
        metadata.setAmbientTemperature(getAmbientTemperatureSensorData().hasValue() ? getAmbientTemperatureSensorData().getValue() : null);
        metadata.setLight(getLightSensorData().hasValue() ? getLightSensorData().getValue() : null);
    }

    private void setCellsMetadata(Metadata metadata) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            metadata.setCells(TelephonyUtils.getCellInfo(this));
        }
    }

    private void getRealPathFromURI(Uri contentURI) {
        if (Build.VERSION.SDK_INT < 19) {
            mFilePath = RealPathUtil.getRealPathFromURI_API11to18(this, contentURI);
        } else {
            mFilePath = RealPathUtil.getRealPathFromURI_API19(this, contentURI);
            if (mFilePath.length() == 0) {
                mFilePath = RealPathUtil.getRealPathFormCustomGallery(this, contentURI);
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        startLocationListeningWrapper();
    }

    @Override
    protected void onPause() {
        super.onPause();

        mCacheWord.disconnectFromService();
    }

    @Override
    protected void onStop() {
        stopLocationListeningWrapper();

        if (rationaleDialog != null && rationaleDialog.isShowing()) {
            rationaleDialog.dismiss();
        }

        hideBackDialog();

        super.onStop();
    }

    @Override
    protected void onDestroy() {
        if (presenter != null) {
            presenter.destroy();
        }

        if (!disposables.isDisposed()) {
            disposables.dispose();
        }

        super.onDestroy();
    }

    @Override
    public void onCacheWordUninitialized() {
        MyApplication.showLockScreen(this);
        finish();
    }

    @Override
    public void onCacheWordLocked() {
        MyApplication.showLockScreen(this);
        finish();
    }

    @Override
    public void onCacheWordOpened() {
        dataSource = DataSource.getInstance(mCacheWord, getApplicationContext());

        // get dataSource data
        List<MediaRecipient> recipients = dataSource.getMediaRecipients();
        List<MediaRecipientList> recipientLists = dataSource.getMediaRecipientListsWithRecipients();

        mReport.setAllRecipients(recipients);
        mReport.setAllRecipientList(recipientLists);

        setFragments(); // todo: move this in onCreate()
        checkFragmentPosition();
    }

    @Override
    public void showSendingReport() {
        showSendProgressBarDialog();
    }

    @Override
    public void hideSendingReport() {
        hideSendProgressBarDialog();
    }

    @Override
    public void onSendReportSuccess(String uid) {
        mReport.setUid(uid);
        updateReportPersistence();

        EvidenceQueue queue = MyApplication.evidenceQueue();
        if (queue != null) {
            for (Evidence evidence : mReport.getEvidences()) {
                queue.add(evidence);
            }
        }

        // delayed msg, db should be quick..
        Toast.makeText(getApplicationContext(),
                getString(R.string.sending_report_success) +
                        (mReport.getEvidences().isEmpty() ? "" : " " + getString(R.string.sending_report_evidences_msg)),
                Toast.LENGTH_LONG)
                .show();

        finish();
    }

    @Override
    public void onSendReportError(IErrorBundle errorBundle) {
        showSendReportError();
    }

    private void sendReportWithMetadata() {
        presenter = new SendReportPresenter(new CreateReportUseCase(new ReportRepository()), this);
        presenter.createReport(mReport);
    }

    private void updateReportPersistence() {
        if (mReport.isKeptInArchive()) {
            if (mReport.isPersisted()) {
                dataSource.updateReport(mReport);
            } else {
                dataSource.insertReport(mReport);
            }
        } else {
            if (mReport.isPersisted()) {
                dataSource.deleteReport(mReport);
            }
        }
    }

    private void showSendProgressBarDialog() {
        sendingProgressDialog = ProgressDialog.show(this, null, getString(R.string.sending_report_progress), true);
    }

    private void hideSendProgressBarDialog() {
        if (sendingProgressDialog != null) {
            sendingProgressDialog.dismiss();
        }
    }

    private void showMetadataProgressBarDialog() {
        metadataProgressDialog = DialogsUtil.showMetadataProgressBarDialog(this, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                metadataCancelRelay.accept(MetadataActivity.MetadataHolder.createEmpty());
            }
        });
    }

    private void showBackDialog() {
        backDialog = new AlertDialog.Builder(this)
                .setMessage(getString(R.string.draft_lost_on_exit))
                .setNegativeButton(getString(R.string.exit_anyway), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        NewReportActivity.super.onBackPressed();
                    }
                })
                .setPositiveButton(getString(R.string.save_and_exit), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        persistReportDraft();
                        NewReportActivity.super.onBackPressed();
                    }
                })
                .create();

        backDialog.show();
    }

    private void hideBackDialog() {
        if (backDialog != null) {
            backDialog.dismiss();
        }
    }

    private void hideMetadataProgressBarDialog() {
        if (metadataProgressDialog != null) {
            metadataProgressDialog.dismiss();
        }
    }

    private void showSendReportError() {
        Toast.makeText(this, getString(R.string.sending_report_error), Toast.LENGTH_SHORT).show();
    }

    private void checkFragmentPosition() {
            viewPager.post(new Runnable() {
                @Override
                public void run() {
                    viewPager.setCurrentItem(mPosition, false);
                }
            });
    }
}
