package rs.readahead.washington.mobile.views.activity;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.CheckedTextView;
import android.widget.LinearLayout;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import info.guardianproject.netcipher.proxy.OrbotHelper;
import rs.readahead.washington.mobile.R;
import rs.readahead.washington.mobile.sharedpref.SharedPrefs;
import rs.readahead.washington.mobile.util.DialogsUtil;
import rs.readahead.washington.mobile.util.PermissionHandler;


public class ActivationSettingsActivity extends AppCompatActivity implements OnPasswordCreateListener {

    @BindView(R.id.enable_secret_mode) CheckedTextView mEnableSecretMode;
    @BindView(R.id.new_secret) LinearLayout mNewSecret;
    @BindView(R.id.change_secret) LinearLayout mChangeSecret;
    //@BindView(R.id.panic_password) LinearLayout mNewPanic;
    @BindView(R.id.change_panic) LinearLayout mChangePanic;
    //@BindView(R.id.enable_tor) CheckedTextView mEnabledTor;

    private Context context = ActivationSettingsActivity.this;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_activation_settings);
        ButterKnife.bind(this);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        setButtons();
    }

    @OnClick({R.id.enable_secret_mode, /*R.id.panic_mode_password, R.id.enable_tor,*/ R.id.create_secret_mode_password,
            R.id.change_panic_mode_password, R.id.change_secret_moder_password})
    public void manage(View view) {
        switch (view.getId()) {
            case R.id.enable_secret_mode:
                handleSecretMode();
                break;
            /*case R.id.panic_mode_password:
                if (SharedPrefs.getInstance().getPanicPassword().equals("")) {
                    DialogsUtil.showNewPasswordDialog(2, context, ActivationSettingsActivity.this);
                }
                break;
            case R.id.enable_tor:
                handleTorMode();
                break;*/
            case R.id.create_secret_mode_password:
                if (SharedPrefs.getInstance().getSecretPassword().equals("")) {
                    DialogsUtil.showNewPasswordDialog(1, context, ActivationSettingsActivity.this);
                }
                break;
            case R.id.change_panic_mode_password:
                DialogsUtil.showChangePasswordDialog(2, context, mEnableSecretMode);
                break;
            case R.id.change_secret_moder_password:
                DialogsUtil.showChangePasswordDialog(1, context, mEnableSecretMode);
                break;
        }
    }

    private void checkNetworkSecurity() {
        if (OrbotHelper.isOrbotInstalled(context)) {
            OrbotHelper.requestStartTor(context);
            //mEnabledTor.setChecked(true);
            SharedPrefs.getInstance().setToreModeActive(true);
        } else
            DialogsUtil.showOrbotDialog(context);
    }

    private void setButtons() {
        mEnableSecretMode.setChecked(SharedPrefs.getInstance().isSecretModeActive());
        //mEnabledTor.setChecked(SharedPrefs.getInstance().isTorModeActive());

        mChangeSecret.setVisibility(SharedPrefs.getInstance().getSecretPassword().length() > 0 ? View.VISIBLE : View.GONE);
        mNewSecret.setVisibility(SharedPrefs.getInstance().getSecretPassword().length() > 0 ? View.GONE : View.VISIBLE);

        mChangePanic.setVisibility(SharedPrefs.getInstance().getPanicPassword().length() > 0 ? View.VISIBLE : View.GONE);
        //mNewPanic.setVisibility(SharedPrefs.getInstance().getPanicPassword().length() > 0 ? View.GONE : View.VISIBLE);

    }

    private void handleTorMode() {
        /*if (mEnabledTor.isChecked()) {
            mEnabledTor.setChecked(false);
            SharedPrefs.getInstance().setToreModeActive(false);
            SharedPrefs.getInstance().setAskForTorOnStart(true);
            showApp();
        } else {
            checkNetworkSecurity();
        }*/
    }

    private void handleSecretMode() {
        if (PermissionHandler.checkPermission(context, Manifest.permission.PROCESS_OUTGOING_CALLS, getString(R.string.permission_call))) {
            if (mEnableSecretMode.isChecked()) {
                mEnableSecretMode.setChecked(false);
                SharedPrefs.getInstance().setSecretModeActive(false);
                showApp();
            } else {
                if (SharedPrefs.getInstance().getSecretPassword().length() > 0) {
                    mEnableSecretMode.setChecked(true);
                    SharedPrefs.getInstance().setSecretModeActive(true);
                    DialogsUtil.showProgressDialog(context);
                    hideApp();
                } else {
                    DialogsUtil.showMessageOKCancelWithTitle(context, getString(R.string.secret_mode_text),
                            getString(R.string.attention), getString(R.string.ok), getString(R.string.cancel),
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    DialogsUtil.showNewPasswordDialog(1, context, ActivationSettingsActivity.this);
                                    dialog.dismiss();
                                }
                            }, null);
                }
            }
        }
    }

    public void hideApp() {

        ComponentName componentName = new ComponentName(getApplicationContext(),
                SplashActivity.class);

        context.getPackageManager().setComponentEnabledSetting(
                componentName,
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP);

    }

    public void showApp() {

        ComponentName componentName = new ComponentName(getApplicationContext(),
                SplashActivity.class);

        context.getPackageManager().setComponentEnabledSetting(
                componentName,
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP);

    }

    @Override
    public void onPasswordCreated() {
        setButtons();
    }
}
