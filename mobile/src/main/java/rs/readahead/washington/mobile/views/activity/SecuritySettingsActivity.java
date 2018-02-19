package rs.readahead.washington.mobile.views.activity;

import android.Manifest;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.SwitchCompat;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.CompoundButton;
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
import rs.readahead.washington.mobile.data.sharedpref.SharedPrefs;
import rs.readahead.washington.mobile.util.DialogsUtil;
import rs.readahead.washington.mobile.util.PermissionUtil;


@RuntimePermissions
public class SecuritySettingsActivity extends CacheWordSubscriberBaseActivity implements
        OnPasswordCreateListener,
        CompoundButton.OnCheckedChangeListener {
    @BindView(R.id.enable_secret_mode)
    SwitchCompat mEnableSecretMode;
    @BindView(R.id.new_secret)
    LinearLayout mNewSecret;
    @BindView(R.id.change_secret)
    LinearLayout mChangeSecret;
    @BindView(R.id.change_panic)
    LinearLayout mChangePanic;
    @BindView(R.id.enable_df)
    SwitchCompat enableDfSwitch;

    private AlertDialog rationaleDialog;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_activation_settings);

        ButterKnife.bind(this);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        setupSwitches();
        setupButtons();
    }

    @Override
    protected void onDestroy() {
        if (rationaleDialog != null && rationaleDialog.isShowing()) {
            rationaleDialog.dismiss();
        }
        super.onDestroy();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        SecuritySettingsActivityPermissionsDispatcher.onRequestPermissionsResult(this, requestCode, grantResults);
    }

    @OnClick({R.id.panic_mode_layout, R.id.create_secret_mode_password,
            R.id.change_panic_mode_password, R.id.change_secret_moder_password})
    public void manage(View view) {
        switch (view.getId()) {
            case R.id.panic_mode_layout:
                startActivity(new Intent(this, PanicModeSettingsActivity.class));
                break;
            case R.id.create_secret_mode_password:
                if (SharedPrefs.getInstance().getSecretPassword().equals("")) {
                    DialogsUtil.showNewPasswordDialog(1, this, SecuritySettingsActivity.this);
                }
                break;
            case R.id.change_panic_mode_password:
                DialogsUtil.showChangePasswordDialog(2, this, mEnableSecretMode);
                break;
            case R.id.change_secret_moder_password:
                DialogsUtil.showChangePasswordDialog(1, this, mEnableSecretMode);
                break;
        }
    }

    @Override
    public void onCheckedChanged(CompoundButton v, boolean isChecked) {
        if (v.getId() == R.id.enable_df) {
            MyApplication.setDomainFronting(isChecked);
            enableDfSwitch.setChecked(isChecked);
            return;
        }

        if (isChecked && v.getId() == R.id.enable_secret_mode) {
            if (SharedPrefs.getInstance().getSecretPassword().length() == 0)  {
                DialogsUtil.showMessageOKCancelWithTitle(this, getString(R.string.secret_mode_text),
                        getString(R.string.attention), getString(R.string.ok), getString(R.string.cancel),
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                DialogsUtil.showNewPasswordDialog(1, SecuritySettingsActivity.this, SecuritySettingsActivity.this);
                                dialog.dismiss();
                            }
                        },
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                mEnableSecretMode.setChecked(false);
                                dialog.dismiss();
                            }
                        } );
            } else {
                mEnableSecretMode.setChecked(true);
                if (SharedPrefs.getInstance().isSecretModeActive()) return; //it's already hidden
                SharedPrefs.getInstance().setSecretModeActive(true);
                SecuritySettingsActivityPermissionsDispatcher.hideAppWithCheck(this);
            }
        } else {
            if (!SharedPrefs.getInstance().isSecretModeActive()) return; //it's already showing
            showApp();
            SharedPrefs.getInstance().setSecretModeActive(false);
            mEnableSecretMode.setChecked(false);
        }
    }

    @Override
    public void onPasswordCreated() {
        if (SharedPrefs.getInstance().getSecretPassword().length() > 0) {
            SecuritySettingsActivityPermissionsDispatcher.hideAppWithCheck(this);
            SharedPrefs.getInstance().setSecretModeActive(true);
            mEnableSecretMode.setChecked(true);
        }
    }

    @NeedsPermission(Manifest.permission.PROCESS_OUTGOING_CALLS)
    public void hideApp() {
        setComponentState(PackageManager.COMPONENT_ENABLED_STATE_DISABLED);
        showToast(R.string.secret_mode_on);
    }

    public void showApp() {
        setComponentState(PackageManager.COMPONENT_ENABLED_STATE_ENABLED);
        showToast(R.string.secret_mode_off);
    }

    @OnShowRationale(Manifest.permission.PROCESS_OUTGOING_CALLS)
    void showFineLocationRationale(final PermissionRequest request) {
        rationaleDialog = PermissionUtil.showRationale(this, request, getString(R.string.permission_call));
    }

    @OnPermissionDenied(Manifest.permission.PROCESS_OUTGOING_CALLS)
    void onFineLocationPermissionDenied() {
        showApp();
    }

    @OnNeverAskAgain(Manifest.permission.PROCESS_OUTGOING_CALLS)
    void onFineLocationNeverAskAgain() { hideApp(); }

    private void setComponentState(int state) {
        ComponentName componentName = new ComponentName(getApplicationContext(),
                SplashActivity.class);

        getPackageManager().setComponentEnabledSetting(
                componentName,
                state,
                PackageManager.DONT_KILL_APP);
    }

    private void setupButtons() {
        boolean secret = SharedPrefs.getInstance().getSecretPassword().length() > 0,
                panic = SharedPrefs.getInstance().getPanicPassword().length() > 0;

        mChangeSecret.setVisibility(secret ? View.VISIBLE : View.GONE);
        mNewSecret.setVisibility(secret ? View.GONE : View.VISIBLE);
        mChangePanic.setVisibility(panic ? View.VISIBLE : View.GONE);
    }

    private void setupSwitches() {
        mEnableSecretMode.setOnCheckedChangeListener(this);
        mEnableSecretMode.setChecked(SharedPrefs.getInstance().isSecretModeActive());

        enableDfSwitch.setOnCheckedChangeListener(this);
        enableDfSwitch.setChecked(MyApplication.isDomainFronting());
    }
}
