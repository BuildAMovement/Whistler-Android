package rs.readahead.washington.mobile.views.fragment;


import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckedTextView;
import android.widget.EditText;
import android.widget.LinearLayout;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;
import info.guardianproject.netcipher.proxy.OrbotHelper;
import rs.readahead.washington.mobile.R;
import rs.readahead.washington.mobile.sharedpref.SharedPrefs;
import rs.readahead.washington.mobile.util.DialogsUtil;
import rs.readahead.washington.mobile.util.PermissionHandler;
import rs.readahead.washington.mobile.views.activity.SplashActivity;

public class ActivationSettingsFragment extends Fragment {

    @BindView(R.id.enable_secret_mode) CheckedTextView mEnableSecretMode;
    @BindView(R.id.new_secret) LinearLayout mNewSecret;
    @BindView(R.id.change_secret) LinearLayout mChangeSecret;
    //@BindView(R.id.panic_password) LinearLayout mNewPanic;
    @BindView(R.id.change_panic) LinearLayout mChangePanic;
    //@BindView(R.id.enable_tor) CheckedTextView mEnabledTor;

    private Context context;
    private Unbinder unbinder;

    public ActivationSettingsFragment() {

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_activation_settings, container, false);
        context = getContext();
        unbinder = ButterKnife.bind(this, view);
        setButtons();
        return view;
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
                    newPasswordDialog(2);
                }
                break;
            case R.id.enable_tor:
                handleTorMode();
                break;*/
            case R.id.create_secret_mode_password:
                if (SharedPrefs.getInstance().getSecretPassword().equals("")) {
                    newPasswordDialog(1);
                }
                break;
            case R.id.change_panic_mode_password:
                changePasswordDialog(2);
                break;
            case R.id.change_secret_moder_password:
                changePasswordDialog(1);
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
        if (SharedPrefs.getInstance().getSecretPassword().length() > 0) {
            mChangeSecret.setVisibility(View.VISIBLE);
            mNewSecret.setVisibility(View.GONE);
        } else {
            mChangeSecret.setVisibility(View.GONE);
            mNewSecret.setVisibility(View.VISIBLE);
        }

        if (SharedPrefs.getInstance().getPanicPassword().length() > 0) {
            mChangePanic.setVisibility(View.VISIBLE);
            //mNewPanic.setVisibility(View.GONE);
        } else {
            mChangePanic.setVisibility(View.GONE);
            //mNewPanic.setVisibility(View.VISIBLE);
        }
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
        if (PermissionHandler.checkPermission(getContext(), Manifest.permission.PROCESS_OUTGOING_CALLS, getString(R.string.permission_call))) {
            if (mEnableSecretMode.isChecked()) {
                mEnableSecretMode.setChecked(false);
                SharedPrefs.getInstance().setSecretModeActive(false);
                showApp();
            } else {
                if (SharedPrefs.getInstance().getSecretPassword().length() > 0) {
                    mEnableSecretMode.setChecked(true);
                    SharedPrefs.getInstance().setSecretModeActive(true);
                    hideApp();
                } else {
                    DialogsUtil.showMessageOKCancelWithTitle(context, getString(R.string.secret_mode_text),
                            getString(R.string.attention), getString(R.string.ok),getString(R.string.cancel),
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    newPasswordDialog(1);
                                    dialog.dismiss();
                                }
                            }, null);
                }
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        unbinder.unbind();
    }

    public void newPasswordDialog(final int caller) {

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.create_password_dialog_layout, null);
        builder.setTitle(caller == 1 ? context.getString(R.string.create_new_secret_password) : context.getString(R.string.create_new_panic_password));
        builder.setPositiveButton(R.string.save, null);
        builder.setView(dialogView);

        final TextInputLayout passwordLayout = (TextInputLayout) dialogView.findViewById(R.id.password_layout);
        final TextInputLayout confirmLayout = (TextInputLayout) dialogView.findViewById(R.id.password_confirm_layout);
        final EditText password = (EditText) dialogView.findViewById(R.id.password);
        final EditText passwordConfirm = (EditText) dialogView.findViewById(R.id.password_confirm);
        assert passwordLayout != null;
        passwordLayout.setError(null);
        assert confirmLayout != null;
        confirmLayout.setError(null);

        final AlertDialog alertDialog = builder.create();
        alertDialog.setCancelable(true);
        alertDialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
        alertDialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialog) {
                Button b = alertDialog.getButton(AlertDialog.BUTTON_POSITIVE);
                b.setOnClickListener(new View.OnClickListener() {

                    @Override
                    public void onClick(View view) {

                        assert password != null;
                        String passwordText = password.getText().toString();
                        assert passwordConfirm != null;
                        String confirmText = passwordConfirm.getText().toString();

                        if (passwordText.length() > 0) {
                            if (confirmText.length() > 0) {
                                if (passwordText.equals(confirmText)) {
                                    if (caller == 1) {
                                        if (!passwordText.equals(SharedPrefs.getInstance().getPanicPassword())) {
                                            SharedPrefs.getInstance().setSecretPassword(passwordText);
                                            setButtons();
                                            alertDialog.dismiss();
                                        } else {
                                            confirmLayout.setError(null);
                                            passwordLayout.setError(context.getString(R.string.panic_secret_password_error));
                                        }
                                    } else {
                                        if (!passwordText.equals(SharedPrefs.getInstance().getSecretPassword())) {
                                            SharedPrefs.getInstance().setPanicPassword(passwordText);
                                            setButtons();
                                            alertDialog.dismiss();
                                        } else {
                                            confirmLayout.setError(null);
                                            passwordLayout.setError(context.getString(R.string.panic_secret_password_error));
                                        }
                                    }
                                } else {
                                    confirmLayout.setError(context.getString(R.string.password_match_error));
                                    passwordLayout.setError(null);
                                }

                            } else {
                                confirmLayout.setError(context.getString(R.string.empty_field_error));
                            }
                        } else {
                            passwordLayout.setError(context.getString(R.string.empty_field_error));
                        }
                    }
                });
            }
        });

        alertDialog.show();

    }

    public void changePasswordDialog(final int caller) {

        AlertDialog.Builder builder =
                new AlertDialog.Builder(context);
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.change_password_dialog_layout, null);
        builder.setView(dialogView);
        builder.setPositiveButton(R.string.save, null);
        builder.setTitle(caller == 1 ? context.getString(R.string.change_secret_password) : context.getString(R.string.change_panic_password));

        final TextInputLayout oldPasswordLayout = (TextInputLayout) dialogView.findViewById(R.id.old_password_layout);
        final TextInputLayout newPasswordLayout = (TextInputLayout) dialogView.findViewById(R.id.new_password_layout);
        final TextInputLayout confirmLayout = (TextInputLayout) dialogView.findViewById(R.id.password_confirm_layout);
        final EditText newPassword = (EditText) dialogView.findViewById(R.id.new_password);
        final EditText oldPassword = (EditText) dialogView.findViewById(R.id.old_password);
        final EditText passwordConfirm = (EditText) dialogView.findViewById(R.id.password_confirm);

        assert oldPasswordLayout != null;
        oldPasswordLayout.setError(null);
        assert newPasswordLayout != null;
        newPasswordLayout.setError(null);
        assert confirmLayout != null;
        confirmLayout.setError(null);

        final AlertDialog alertDialog = builder.create();
        alertDialog.setCancelable(true);
        alertDialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
        alertDialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialog) {
                Button button = alertDialog.getButton(DialogInterface.BUTTON_POSITIVE);
                button.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        assert oldPassword != null;
                        String oldPasswordText = oldPassword.getText().toString();
                        assert newPassword != null;
                        String newPasswordText = newPassword.getText().toString();
                        assert passwordConfirm != null;
                        String confirmText = passwordConfirm.getText().toString();

                        if (oldPasswordText.length() > 0) {
                            if (newPasswordText.length() > 0) {
                                if (confirmText.length() > 0) {
                                    if (newPasswordText.equals(confirmText)) {
                                        if (caller == 1) {
                                            if (!newPasswordText.equals(SharedPrefs.getInstance().getPanicPassword())) {
                                                if (SharedPrefs.getInstance().getSecretPassword().equals(oldPasswordText)) {
                                                    SharedPrefs.getInstance().setSecretPassword(confirmText);
                                                    //Snackbar.make(mEnabledTor, R.string.password_changed, Snackbar.LENGTH_SHORT).show();
                                                    alertDialog.dismiss();
                                                } else {
                                                    oldPasswordLayout.setError(context.getString(R.string.incorrect_password));
                                                    confirmLayout.setError(null);
                                                    newPasswordLayout.setError(null);
                                                }
                                            } else {
                                                newPasswordLayout.setError(context.getString(R.string.panic_secret_password_error));
                                                confirmLayout.setError(null);
                                            }
                                        } else {
                                            if (!newPasswordText.equals(SharedPrefs.getInstance().getSecretPassword())) {
                                                if (SharedPrefs.getInstance().getPanicPassword().equals(oldPasswordText)) {
                                                    SharedPrefs.getInstance().setPanicPassword(confirmText);
                                                    //Snackbar.make(mEnabledTor, R.string.password_changed, Snackbar.LENGTH_SHORT).show();
                                                    alertDialog.dismiss();
                                                } else {
                                                    oldPasswordLayout.setError(context.getString(R.string.incorrect_password));
                                                    confirmLayout.setError(null);
                                                    newPasswordLayout.setError(null);
                                                }
                                            } else {
                                                newPasswordLayout.setError(context.getString(R.string.panic_secret_password_error));
                                                confirmLayout.setError(null);
                                            }
                                        }
                                    } else {
                                        confirmLayout.setError(context.getString(R.string.password_match_error));
                                        newPasswordLayout.setError(null);
                                    }
                                } else {
                                    confirmLayout.setError(context.getString(R.string.empty_field_error));
                                }
                            } else {
                                newPasswordLayout.setError(context.getString(R.string.empty_field_error));
                            }
                        } else {
                            oldPasswordLayout.setError(context.getString(R.string.empty_field_error));
                        }
                    }
                });
            }
        });
        alertDialog.show();

    }

    public void hideApp() {

        ComponentName componentName = new ComponentName(context.getApplicationContext(),
                SplashActivity.class);

        getActivity().getPackageManager().setComponentEnabledSetting(
                componentName,
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP);

    }

    public void showApp() {

        ComponentName componentName = new ComponentName(context.getApplicationContext(),
                SplashActivity.class);

        getActivity().getPackageManager().setComponentEnabledSetting(
                componentName,
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP);

    }

}
