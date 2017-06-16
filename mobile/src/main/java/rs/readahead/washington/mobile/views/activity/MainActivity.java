package rs.readahead.washington.mobile.views.activity;

import android.Manifest;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomSheetBehavior;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.io.File;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import info.guardianproject.cacheword.CacheWordHandler;
import info.guardianproject.cacheword.ICacheWordSubscriber;
import info.guardianproject.netcipher.proxy.OrbotHelper;
import rs.readahead.washington.mobile.MyApplication;
import rs.readahead.washington.mobile.R;
import rs.readahead.washington.mobile.database.DataSource;
import rs.readahead.washington.mobile.sharedpref.SharedPrefs;
import rs.readahead.washington.mobile.util.CommonUtils;
import rs.readahead.washington.mobile.util.C;
import rs.readahead.washington.mobile.util.DialogsUtil;
import rs.readahead.washington.mobile.util.DownloadTrainingService;
import rs.readahead.washington.mobile.util.FileUtil;
import rs.readahead.washington.mobile.util.LocationProvider;
import rs.readahead.washington.mobile.util.LocationUtil;
import rs.readahead.washington.mobile.util.PermissionHandler;
import rs.readahead.washington.mobile.util.SmsUtil;
import rs.readahead.washington.mobile.views.custom.CountdownImageView;
import timber.log.Timber;


public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener,
        SharedPreferences.OnSharedPreferenceChangeListener, ICacheWordSubscriber {

    @BindView(R.id.toolbar) Toolbar toolbar;
    @BindView(R.id.drawer_layout) DrawerLayout drawer;
    @BindView(R.id.nav_view) NavigationView navigationView;
    @BindView(R.id.make_oppression_backfire) LinearLayout makeOppressionBackfireImage;
    //@BindView(R.id.secure_messaging) LinearLayout secureMessagingImage;
    @BindView(R.id.bottom_sheet) View bottomSheet;
    @BindView(R.id.collapsed_view) RelativeLayout bottomSheetCollapsedView;
    @BindView(R.id.panic_mode_view) View bottomSheetPanicModeView;
    @BindView(R.id.countdown_timer) CountdownImageView countdownImage;
    @BindView(R.id.training_room) LinearLayout mTrainingRoom;
    @BindView(R.id.training_room_text) TextView mTrainingRoomText;


    private NotificationManager mNotificationManager;
    private NotificationCompat.Builder mBuilder;
    private CountDownTimer timer;
    private boolean mDownloadReceiverRegistered;
    private boolean mMonitorReceiverRegistered;
    private boolean mFirstRun;
    private boolean mReconnected;
    private boolean mDownloading;
    private BottomSheetBehavior mBottomSheetBehavior;
    private CacheWordHandler mCacheWord;
    private DataSource dataSource;
    private boolean mExit = false;
    private String message = "";
    private Handler handler;
    private boolean panicActivated;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        setSupportActionBar(toolbar);
        mCacheWord = new CacheWordHandler(this);
        handler = new Handler();
        panicActivated = false;
        initSetup();
    }

    private void initSetup() {
        handleOrbot();
        handler = new Handler();
        mReconnected = false;
        mDownloading = false;
        mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        mBuilder = new NotificationCompat.Builder(this);

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        navigationView.setNavigationItemSelectedListener(this);

        mDownloadReceiverRegistered = false;
        mMonitorReceiverRegistered = false;

        mBottomSheetBehavior = BottomSheetBehavior.from(bottomSheet);
        mBottomSheetBehavior.setBottomSheetCallback(new MyBottomSheetBehavior());
    }

    @OnClick({R.id.make_oppression_backfire, /*R.id.secure_messaging, */R.id.training_room, R.id.panic_mode_view})
    public void startActivity(View view) {
        switch (view.getId()) {
            case R.id.make_oppression_backfire:
                startActivity(new Intent(MainActivity.this, ReportSelectorActivity.class));
                break;
            //case R.id.secure_messaging:
            //    startActivity(new Intent(MainActivity.this, SecureMessagingActivity.class));
            //    break;
            case R.id.training_room:
                handleTrainingRoom();
                break;
            case R.id.panic_mode_view:
                mBottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
                break;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        if (item.getItemId() == R.id.action_settings) {
            startActivity(new Intent(MainActivity.this, SettingsActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {

        int id = item.getItemId();

        switch (id) {
            //case R.id.nav_secure_messaging:
            //    startActivity(new Intent(MainActivity.this, SecureMessagingActivity.class));
            //    break;
            case R.id.nav_make_oppression_backfire:
                startActivity(new Intent(MainActivity.this, ReportSelectorActivity.class));
                break;

            case R.id.nav_settings:
                startActivity(new Intent(MainActivity.this, SettingsActivity.class));
                break;

            //case R.id.nav_help:
            //    startBrowserHelpUrl();
            //    break;

            case R.id.nav_about_n_help:
                startAboutHelpActivity();
                break;

            case R.id.nav_log_out:
                finish();
                break;

            case R.id.nav_training_room:
                if (mDownloading)
                    Snackbar.make(bottomSheetCollapsedView, R.string.download_in_progress, Snackbar.LENGTH_SHORT).show();
                else handleTrainingRoom();
                break;

            default:
                break;
        }

        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    private void startAboutHelpActivity() {
        startActivity(new Intent(MainActivity.this, AboutHelpActivity.class));
    }

    private void startBrowserHelpUrl() {
        CommonUtils.startBrowserIntent(this, getString(R.string.config_help_url));
    }

    private BroadcastReceiver torStatusReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (TextUtils.equals(intent.getAction(), OrbotHelper.ACTION_STATUS)) {
                String status = intent.getStringExtra(OrbotHelper.EXTRA_STATUS);
                /*TODO See what is going on with TOR*/
                boolean enabled = status.equals(OrbotHelper.STATUS_ON);
            }
        }
    };

    private BroadcastReceiver downloadBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Bundle bundle = intent.getExtras();
            int resultCode = bundle.getInt(C.DOWNLOAD_RESULT);
            unregisterDownloadReceivers();
            mTrainingRoom.setEnabled(true);
            mDownloading = false;

            switch (resultCode) {
                case 1:
                    mTrainingRoomText.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            mTrainingRoomText.setText(getString(R.string.training_room));
                        }
                    }, 800);
                    Snackbar.make(bottomSheetCollapsedView, getString(R.string.training_room_downloaded), Snackbar.LENGTH_SHORT).show();
                    break;

                case 2:
                    mTrainingRoomText.setText(getString(R.string.training_room));
                    DialogsUtil.showInfoDialog(context, getString(R.string.error), getString(R.string.insufficient_space));
                    break;

                case 3:
                    mTrainingRoomText.setText(getString(R.string.training_room));
                    DialogsUtil.showInfoDialog(context, getString(R.string.error), getString(R.string.zip_corrupted));
                    break;

                default:
                    break;
            }
        }
    };

    private BroadcastReceiver internetMonitor = new BroadcastReceiver() {

        @Override
        public void onReceive(final Context context, Intent intent) {
            Bundle extras = intent.getExtras();
            NetworkInfo info = extras.getParcelable("networkInfo");

            assert info != null;
            NetworkInfo.State state = info.getState();

            if (mDownloading) {
                if (state == NetworkInfo.State.CONNECTED) {
                    handleInternetReconnected();
                } else {
                    handleInternetLoss();
                }
            }
        }
    };

    private void monitorInternetConnection() {
        mFirstRun = true;

        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);

        registerReceiver(internetMonitor, intentFilter);
        mMonitorReceiverRegistered = true;
    }

    private void handleInternetReconnected() {
        if (!mFirstRun && !mDownloading) {
            mReconnected = true;
            mDownloading = true;
            mTrainingRoom.setEnabled(false);
            SharedPrefs.getInstance().setServiceInterrupted(false);
            startService(new Intent(this, DownloadTrainingService.class));
            Snackbar.make(bottomSheetCollapsedView, R.string.resuming_download, Snackbar.LENGTH_SHORT).show();

            mBuilder.setContentTitle(getString(R.string.resuming_download))
                    .setAutoCancel(false)
                    .setSmallIcon(R.mipmap.ic_launcher);
            mNotificationManager.notify(12, mBuilder.build());
        }
    }

    private void handleInternetLoss() {
        mFirstRun = false;
        mReconnected = false;
        stopService(new Intent(this, DownloadTrainingService.class));
        SharedPrefs.getInstance().setServiceInterrupted(true);
        mBuilder.setContentTitle(getString(R.string.waiting_connection))
                .setAutoCancel(false)
                .setSmallIcon(R.mipmap.ic_launcher);
        mNotificationManager.notify(12, mBuilder.build());
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (!mReconnected) {
                    Snackbar.make(bottomSheetCollapsedView, R.string.internet_lost, Snackbar.LENGTH_SHORT).show();
                    mTrainingRoom.setEnabled(true);
                    mTrainingRoomText.setText(getString(R.string.training_room));
                    mDownloading = false;
                }
            }
        }, 10000);
    }

    private void handleOrbot() {
        // todo: for now we don't want to ask for TOR at application first start
        SharedPrefs.getInstance().setAskForTorOnStart(false);

        if (SharedPrefs.getInstance().isTorModeActive()) {
            checkNetworkSecurity();
        } else if (SharedPrefs.getInstance().askForTorOnStart()) {
            DialogsUtil.showMessageOKCancelWithTitle(this, getString(R.string.orbot_activation),
                    getString(R.string.attention), getString(R.string.ok), getString(R.string.cancel),
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            checkNetworkSecurity();
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
    }

    private void checkNetworkSecurity() {
        if (OrbotHelper.isOrbotInstalled(this)) {
            OrbotHelper.requestStartTor(this);
            SharedPrefs.getInstance().setToreModeActive(true);
        } else
            DialogsUtil.showOrbotDialog(this);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case C.REQUEST_CODE_ASK_PERMISSIONS: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (permissions[0].equals(Manifest.permission.SEND_SMS)) {
                        mBottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                    }
                    if (permissions[0].equals(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                        handleTrainingRoom();
                    }
                }
            }
        }
    }

    private void handleTrainingRoom() {
        if (PermissionHandler.checkPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE, getString(R.string.permission_storage))) {
            if (!SharedPrefs.getInstance().isTrainingMaterialDownloaded()) {
                if (MyApplication.isConnectedToInternet(this)) {
                    DialogsUtil.showMessageOKCancelWithTitle(this, getString(R.string.download_materials_over_mobile_info),
                            getString(R.string.attention), getString(R.string.yes), getString(R.string.cancel),
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    downloadMaterials();
                                    dialog.dismiss();
                                }
                            }, null);

                } else DialogsUtil.showInternetErrorDialog(this);
            } else {
                if (getFolder().exists() && FileUtil.folderSize(getFolder()) > 0)
                    startActivity(new Intent(this, TrainingRoomActivity.class));
                else {
                    SharedPrefs.getInstance().setTrainingDownloaded(false);
                    downloadMaterials();
                }
            }
        }
    }

    private File getFolder() {
        return new File(FileUtil.getFolderPath(C.FOLDER_TRAINING_MATERIALS));
    }

    private void downloadMaterials() {
        mTrainingRoom.setEnabled(false);
        //mDownloadReceiverRegistered = true;
        mDownloading = true;
        Intent downloadIntent = new Intent(this, DownloadTrainingService.class);
        startService(downloadIntent);
        Snackbar.make(bottomSheetCollapsedView, R.string.getting_data, Snackbar.LENGTH_SHORT).show();
        monitorInternetConnection();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals(SharedPrefs.PROGRESS_VALUE)) {
            int progress = SharedPrefs.getInstance().getProgressValue();
            if (progress < 100)
                setProgressText(progress);
            else {
                mTrainingRoomText.setText(getString(R.string.unzipping_file));
            }
        }
    }

    public void setProgressText(int progress) {
        String progressText = getString(R.string.download_in_progress) + " " + String.valueOf(progress) + "%";
        mTrainingRoomText.setText(progressText);
    }

    public void unregisterDownloadReceivers() {
        if (mMonitorReceiverRegistered) {
            unregisterReceiver(internetMonitor);
            mMonitorReceiverRegistered = false;
        }

        if (mDownloadReceiverRegistered) {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(downloadBroadcastReceiver);
            mDownloadReceiverRegistered = false;
        }
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
    }

    @Override
    public void onBackPressed() {

        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            if (mExit) {
                mBottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
                finish();
                if (!mCacheWord.isLocked()) {
                    mCacheWord.lock();
                }

            } else {
                mExit = true;
                Snackbar.make(mTrainingRoom, getString(R.string.exit), Snackbar.LENGTH_SHORT).show();

                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mExit = false;
                    }
                }, 3 * 1000);
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        mBottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
        SharedPrefs.getInstance().unregisterOnSharedPreferenceChangeListener(this);

        LocalBroadcastManager.getInstance(this).unregisterReceiver(downloadBroadcastReceiver);
        mDownloadReceiverRegistered = false;

        mCacheWord.disconnectFromService();

        if (timer != null) {
            mBottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
            panicActivated = true;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        LocalBroadcastManager.getInstance(this).registerReceiver(downloadBroadcastReceiver, new IntentFilter(C.TRAINING_RECEIVER));
        mDownloadReceiverRegistered = true;

        registerReceiver(torStatusReceiver, new IntentFilter(OrbotHelper.ACTION_STATUS));

        mCacheWord.connectToService();

        if (panicActivated) {
            mBottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
            panicActivated = false;
        }

        SharedPrefs.getInstance().registerOnSharedPreferenceChangeListener(this);

        handleDownloadService();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        SharedPrefs.getInstance().unregisterOnSharedPreferenceChangeListener(this);

        //LocalBroadcastManager.getInstance(this).unregisterReceiver(downloadBroadcastReceiver);
        //if (mMonitorReceiverRegistered)
        //    unregisterReceiver(internetMonitor);
        unregisterDownloadReceivers();

        unregisterReceiver(torStatusReceiver);

        stopService(new Intent(this, DownloadTrainingService.class));
    }

    private void handleDownloadService() {
        int progress = SharedPrefs.getInstance().getProgressValue();
        if (SharedPrefs.getInstance().isTrainingMaterialDownloadStarted() && !SharedPrefs.getInstance().isServiceInterrupted()) {
            setTrainingRoomAvailable(false);
            if (progress == 101) {
                mTrainingRoomText.setText(getString(R.string.unzipping_file));
            } else if (progress <= 100) {
                mTrainingRoomText.setText(getString(R.string.training_room));
            }
        } else {
            setTrainingRoomAvailable(true);
            if (SharedPrefs.getInstance().isTrainingMaterialDownloadStarted() && SharedPrefs.getInstance().isServiceInterrupted()) {
                monitorInternetConnection();
                SharedPrefs.getInstance().setServiceInterrupted(false);
                if (progress == 101) {
                    mTrainingRoomText.setText(getString(R.string.unzipping_file));
                    setTrainingRoomAvailable(false);
                    downloadMaterials();
                } else if (progress <= 100) {
                    mTrainingRoomText.setText(getString(R.string.continue_download));
                }
            } else if (progress == 102) {
                mTrainingRoomText.setText(getString(R.string.training_room));
            }
        }
    }

    private void setTrainingRoomAvailable(final boolean available) {
        mTrainingRoom.post(new Runnable() {
            @Override
            public void run() {
                mTrainingRoom.setEnabled(available);
            }
        });
    }

    private class MyBottomSheetBehavior extends BottomSheetBehavior.BottomSheetCallback {

        final int timerDuration = getResources().getInteger(R.integer.panic_countdown_duration);

        @Override
        public void onStateChanged(@NonNull View bottomSheet, int newState) {
            if (newState == BottomSheetBehavior.STATE_EXPANDED) {
                if (PermissionHandler.checkPermission(MainActivity.this, Manifest.permission.SEND_SMS, getString(R.string.permission_sms))) {
                    // todo: move timer to CountdownImage too..
                    timer = new CountDownTimer(timerDuration * 1000L, 200L) {
                        public void onTick(long millisUntilFinished) {
                            Timber.d("******* %s: %s", this, millisUntilFinished);
                            countdownImage.setCountdownNumber(Math.round(millisUntilFinished * 0.001f));
                        }

                        public void onFinish() {
                            Timber.d("******* onFinish");
                            if (!mCacheWord.isLocked()) {

                                List<String> phoneNumbers = dataSource.getTrustedPhones();

                                if (phoneNumbers.size() > 0) {
                                    prepareSMS(phoneNumbers);
                                }
                                deleteFiles();
                            }
                            mBottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
                        }
                    }.start();
                } else {
                    mBottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
                }
            } else if (newState == BottomSheetBehavior.STATE_COLLAPSED) {
                if (timer != null) {
                    timer.cancel();
                    timer = null;
                }
                countdownImage.setCountdownNumber(timerDuration);
            }
        }

        @Override
        public void onSlide(@NonNull View bottomSheet, float slideOffset) {

            if (timer != null) {
                timer.cancel();
                timer = null;
            }
            bottomSheetCollapsedView.setAlpha(Math.abs(1 - slideOffset));
            bottomSheetPanicModeView.setAlpha(slideOffset);

        }
    }

    private void prepareSMS(final List<String> phoneNumbers) {

        message = SharedPrefs.getInstance().getPanicMessage();
        message += "\n\n" + getString(R.string.location_info) + "\n";

        if (SharedPrefs.getInstance().isPanicGeolocationActive()) {
            LocationProvider.requestSingleUpdate(this, new LocationProvider.LocationCallback() {
                @Override
                public void onNewLocationAvailable(Location location) {
                    if (location.getLatitude() != 0.0 && location.getLongitude() != 0.0) {
                        message += LocationUtil.getLocationData(location);
                    } else {
                        message += getString(R.string.unknown);
                    }
                    sendMessages(phoneNumbers);
                }
            });
        } else {
            message += getString(R.string.unknown);
            sendMessages(phoneNumbers);
        }
    }

    private void sendMessages(final List<String> phoneNumbers) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        SmsUtil.sendSMS(phoneNumbers, MainActivity.this, message, mTrainingRoom);
                    }
                }).start();
            }
        });
    }

    private void deleteFiles() {
        if (!mCacheWord.isLocked()) {

            if (SharedPrefs.getInstance().isEraseDatabaseActive()) {
                dataSource.deleteDatabase();
            } else {
                if (SharedPrefs.getInstance().isEraseContactsActive()) {
                    dataSource.deleteContacts();
                } else if (SharedPrefs.getInstance().isEraseMediaActive()) {
                    dataSource.deleteMedia();
                }
            }

            if (SharedPrefs.getInstance().isEraseMaterialsActive()) {
                FileUtil.deleteFolder(C.FOLDER_TRAINING_MATERIALS);
            }
            if (SharedPrefs.getInstance().isEraseVideosActive()) {
                FileUtil.deleteFolder(C.FOLDER_VIDEOS);
            }
            if (SharedPrefs.getInstance().isEraseAudiosActive()) {
                FileUtil.deleteFolder(C.FOLDER_AUDIO);
            }
            if (SharedPrefs.getInstance().isErasePhotosActive()) {
                FileUtil.deleteFolder(C.FOLDER_IMAGES);
            }

            Snackbar.make(mTrainingRoom, getString(R.string.data_erased), Snackbar.LENGTH_SHORT).show();
        } else {
            panicActivated = true;
        }
    }
}
