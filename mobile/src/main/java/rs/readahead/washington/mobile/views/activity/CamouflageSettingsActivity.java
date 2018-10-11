package rs.readahead.washington.mobile.views.activity;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;

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
import rs.readahead.washington.mobile.bus.event.CamouflageAliasChangedEvent;
import rs.readahead.washington.mobile.data.sharedpref.Preferences;
import rs.readahead.washington.mobile.util.CamouflageManager;
import rs.readahead.washington.mobile.util.DialogsUtil;
import rs.readahead.washington.mobile.util.PermissionUtil;


@RuntimePermissions
public class CamouflageSettingsActivity extends CacheWordSubscriberBaseActivity implements
        OnPasswordCreateListener {
    private static final long DIALOG_TIMEOUT_MS = 5000L;

    @BindView(R.id.enable_secret_mode)
    LinearLayout mEnableSecretMode;
    @BindView(R.id.camouflage_layout)
    LinearLayout camouflageLayout;
    @BindView(R.id.regular_appearance)
    LinearLayout regularAppearance;
    @BindView(R.id.secret_check)
    ImageView secretCheck;
    @BindView(R.id.appearance_check)
    ImageView appearanceCheck;
    @BindView(R.id.default_check)
    ImageView defaultCheck;

    private AlertDialog alertDialog;
    private CamouflageManager camouflageManager = CamouflageManager.getInstance();
    private EventCompositeDisposable disposables;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camouflage_settings);

        ButterKnife.bind(this);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.camouflage);
        }

        disposables = MyApplication.bus().createCompositeDisposable();
        disposables.wire(CamouflageAliasChangedEvent.class, new EventObserver<CamouflageAliasChangedEvent>() {
            @Override
            public void onNext(CamouflageAliasChangedEvent event) {
                camouflageChanged();
            }
        });

        setupCheck();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (disposables != null) {
            disposables.dispose();
        }

        if (alertDialog != null && alertDialog.isShowing()) {
            alertDialog.dismiss();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        CamouflageSettingsActivityPermissionsDispatcher.onRequestPermissionsResult(this, requestCode, grantResults);
    }

    @OnClick({R.id.regular_appearance, R.id.camouflage_layout, R.id.enable_secret_mode})
    public void handleClick(View view) {
        switch (view.getId()) {
            case R.id.regular_appearance:
                camouflageManager.setDefaultLauncherActivityAlias(this);
                if (Preferences.isSecretModeActive()) {
                    stopSecretMode();
                }
                setupCheck();
                break;

            case R.id.camouflage_layout:
                startActivity(new Intent(this, CamouflageAliasActivity.class));
                break;

            case R.id.enable_secret_mode:
                if (Preferences.getSecretPassword().length() == 0) {
                    DialogsUtil.showNewPasswordDialog(1, CamouflageSettingsActivity.this, CamouflageSettingsActivity.this);
                } else {
                    DialogsUtil.showChangePasswordDialog(1, CamouflageSettingsActivity.this, mEnableSecretMode);
                }
                break;
        }
    }

    @Override
    public void onPasswordCreated() {
        if (Preferences.getSecretPassword().length() > 0) {
            startSecretMode();
        }
    }

    @NeedsPermission(Manifest.permission.PROCESS_OUTGOING_CALLS)
    void hideApp() {
        showToast(R.string.secret_mode_on);
        camouflageManager.setDefaultLauncherActivityAlias(this);
        camouflageManager.enableSecretMode(this);
        setupCheck();
        showExitDialog();
    }

    @OnShowRationale(Manifest.permission.PROCESS_OUTGOING_CALLS)
    void showProcessOutgoingCallsRationale(final PermissionRequest request) {
        alertDialog = PermissionUtil.showRationale(this, request, getString(R.string.permission_call));
    }

    @OnPermissionDenied(Manifest.permission.PROCESS_OUTGOING_CALLS)
    void onProcessOutgoingCallsPermissionDenied() {
    }

    @OnNeverAskAgain(Manifest.permission.PROCESS_OUTGOING_CALLS)
    void onProcessOutgoingCallsNeverAskAgain() {
    }

    private void setupCheck() {
        if (Preferences.isSecretModeActive()) {
            checkSecret();
        } else {
            if (camouflageManager.isDefaultLauncherActivityAlias()) {
                checkDefault();
            } else {
                checkAppearance();
            }
        }
    }

    private void camouflageChanged() {
        if (Preferences.isSecretModeActive()) {
            stopSecretMode();
        }

        setupCheck();
    }

    private void startSecretMode() {
        CamouflageSettingsActivityPermissionsDispatcher.hideAppWithCheck(this);
    }

    private void stopSecretMode() {
        showToast(R.string.secret_mode_off);

        Preferences.setSecretPassword("");
        camouflageManager.disableSecretMode(this);
        showExitDialog();
    }

    private void checkAppearance() {
        appearanceCheck.setVisibility(View.VISIBLE);
        defaultCheck.setVisibility(View.INVISIBLE);
        secretCheck.setVisibility(View.INVISIBLE);
    }

    private void checkDefault() {
        appearanceCheck.setVisibility(View.INVISIBLE);
        defaultCheck.setVisibility(View.VISIBLE);
        secretCheck.setVisibility(View.INVISIBLE);
    }

    private void checkSecret() {
        appearanceCheck.setVisibility(View.INVISIBLE);
        defaultCheck.setVisibility(View.INVISIBLE);
        secretCheck.setVisibility(View.VISIBLE);
    }

    private void lockCacheWord() {
        if (getCacheWordHandler() == null) {
            return;
        }

        if (!getCacheWordHandler().isLocked()) {
            getCacheWordHandler().lock();
        }
    }

    private void showExitDialog() {
        alertDialog = new AlertDialog.Builder(this)
                .setMessage(getString(R.string.whistler_will_now_close))
                .setPositiveButton(getString(R.string.exit_now), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        MyApplication.exit(CamouflageSettingsActivity.this);
                        lockCacheWord();
                    }
                })
                .setCancelable(false)
                .create();

        alertDialog.show();

        // auto close it
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if (alertDialog != null && alertDialog.isShowing()) {
                    alertDialog.getButton(DialogInterface.BUTTON_POSITIVE).performClick();
                    alertDialog = null;
                }
            }
        }, DIALOG_TIMEOUT_MS);
    }
}
