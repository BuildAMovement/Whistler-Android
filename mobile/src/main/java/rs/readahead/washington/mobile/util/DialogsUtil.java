package rs.readahead.washington.mobile.util;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.SwitchCompat;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import rs.readahead.washington.mobile.R;
import rs.readahead.washington.mobile.data.sharedpref.Preferences;
import rs.readahead.washington.mobile.data.sharedpref.SharedPrefs;
import rs.readahead.washington.mobile.domain.entity.TrustedPerson;
import rs.readahead.washington.mobile.domain.entity.collect.CollectFormInstanceStatus;
import rs.readahead.washington.mobile.views.activity.OnPasswordCreateListener;
import rs.readahead.washington.mobile.views.activity.OnTrustedPersonInteractionListener;
import rs.readahead.washington.mobile.views.custom.CameraPreviewAnonymousButton;


public class DialogsUtil {
    /*public static void showInternetErrorDialog(Context context) {
        AlertDialog.Builder builder =
                new AlertDialog.Builder(context);
        builder.setTitle(R.string.error);
        builder.setMessage(R.string.internet_error);
        builder.setNegativeButton(R.string.close, null);
        builder.setCancelable(true);
        builder.show();
    }*/

    /*public static AlertDialog showInfoDialog(Context context, String title, String message) {
        AlertDialog.Builder builder =
                new AlertDialog.Builder(context);
        builder.setTitle(title);
        builder.setMessage(message);
        builder.setNegativeButton(R.string.close, null);
        builder.setCancelable(true);
        return builder.show();
    }*/

    /*public static void showOrbotDialog(final Context context) {
        AlertDialog.Builder builder =
                new AlertDialog.Builder(context);

        @SuppressLint("InflateParams") TextView text = (TextView) LayoutInflater.from(context).inflate(R.layout.orbot_dialog, null);
        text.setText(Html.fromHtml(context.getString(R.string.orbot_install_info)));
        text.setLinksClickable(true);
        text.setMovementMethod(LinkMovementMethod.getInstance());
        text.setHighlightColor(ContextCompat.getColor(context, R.color.wa_light_gray));

        builder.setView(text);
        builder.setTitle(R.string.warning);
        builder.setNegativeButton(R.string.close_orbot, null);
        builder.setPositiveButton(R.string.orbot_install, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                context.startActivity(OrbotHelper.getOrbotInstallIntent(context));
                dialog.dismiss();
            }
        });
        builder.setCancelable(false);
        builder.show();
    }*/

    static void showMessageOKCancel(Context context, String message, DialogInterface.OnClickListener okListener) {
        AlertDialog.Builder builder =
                new AlertDialog.Builder(context);
        builder.setMessage(message);
        builder.setPositiveButton(R.string.ok, okListener);
        builder.setNegativeButton(R.string.cancel, null);
        builder.setCancelable(true);
        builder.show();
    }

    public static AlertDialog showMessageOKCancelWithTitle(Context context, String message, String title, String positiveButton, String negativeButton,
                                                           DialogInterface.OnClickListener okListener, DialogInterface.OnClickListener cancelListener) {
        return new AlertDialog.Builder(context)
                .setMessage(message)
                .setTitle(title)
                .setPositiveButton(positiveButton, okListener)
                .setNegativeButton(negativeButton, cancelListener)
                .setCancelable(true)
                .show();
    }

    public static AlertDialog showDialog(
            Context context,
            String message,
            String positiveButton,
            String negativeButton,
            DialogInterface.OnClickListener okListener,
            DialogInterface.OnClickListener cancelListener) {
        return new AlertDialog.Builder(context)
                .setMessage(message)
                .setPositiveButton(positiveButton, okListener)
                .setNegativeButton(negativeButton, cancelListener)
                .setCancelable(true)
                .show();
    }

    public static ProgressDialog showProgressDialog(Context context, String text) {
        ProgressDialog dialog = new ProgressDialog(context);
        dialog.setIndeterminate(true);
        dialog.setMessage(text);
        dialog.setCancelable(false);
        dialog.show();
        return dialog;
    }

    public static void showChangePasswordDialog(final int caller, final Context context, final View view) {

        AlertDialog.Builder builder =
                new AlertDialog.Builder(context);
        LayoutInflater inflater = LayoutInflater.from(context);
        @SuppressLint("InflateParams") View dialogView = inflater.inflate(R.layout.change_password_dialog_layout, null);
        builder.setView(dialogView);
        builder.setPositiveButton(R.string.save, null);
        builder.setNegativeButton(R.string.cancel, null);
        builder.setTitle(caller == 1 ? context.getString(R.string.change_secret_password) : context.getString(R.string.change_panic_password));

        final TextInputLayout oldPasswordLayout = dialogView.findViewById(R.id.old_password_layout);
        final TextInputLayout newPasswordLayout = dialogView.findViewById(R.id.new_password_layout);
        final TextInputLayout confirmLayout = dialogView.findViewById(R.id.password_confirm_layout);
        final EditText newPassword = dialogView.findViewById(R.id.new_password);
        final EditText oldPassword = dialogView.findViewById(R.id.old_password);
        final EditText passwordConfirm = dialogView.findViewById(R.id.password_confirm);

        assert oldPasswordLayout != null;
        oldPasswordLayout.setError(null);
        assert newPasswordLayout != null;
        newPasswordLayout.setError(null);
        assert confirmLayout != null;
        confirmLayout.setError(null);

        final AlertDialog alertDialog = builder.create();
        alertDialog.setCancelable(true);
        ViewUtil.setDialogSoftInputModeVisible(alertDialog);
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

                        newPasswordLayout.setError(null);
                        oldPasswordLayout.setError(null);
                        confirmLayout.setError(null);

                        if (oldPasswordText.length() == 0) {
                            oldPasswordLayout.setError(context.getString(R.string.empty_field_error));
                            oldPassword.requestFocus();
                            return;
                        }
                        if (newPasswordText.length() == 0) {
                            newPasswordLayout.setError(context.getString(R.string.empty_field_error));
                            newPassword.requestFocus();
                            return;
                        }
                        if (confirmText.length() == 0) {
                            confirmLayout.setError(context.getString(R.string.empty_field_error));
                            passwordConfirm.requestFocus();
                            return;
                        }
                        if (!newPasswordText.equals(confirmText)) {
                            confirmLayout.setError(context.getString(R.string.password_match_error));
                            newPasswordLayout.setError(null);
                            confirmLayout.requestFocus();
                            return;
                        }
                        if (caller == 1) {
                            if (!newPasswordText.equals(SharedPrefs.getInstance().getPanicPassword())) {
                                if (Preferences.getSecretPassword().equals(oldPasswordText)) {
                                    Preferences.setSecretPassword(confirmText);
                                    Snackbar.make(view, R.string.password_changed, Snackbar.LENGTH_SHORT).show();
                                    alertDialog.dismiss();
                                } else {
                                    oldPasswordLayout.setError(context.getString(R.string.incorrect_password));
                                    oldPassword.requestFocus();
                                    confirmLayout.setError(null);
                                    newPasswordLayout.setError(null);
                                }
                            } else {
                                newPasswordLayout.setError(context.getString(R.string.panic_secret_password_error));
                                newPassword.requestFocus();
                                confirmLayout.setError(null);
                            }
                        } else {
                            if (!newPasswordText.equals(Preferences.getSecretPassword())) {
                                if (SharedPrefs.getInstance().getPanicPassword().equals(oldPasswordText)) {
                                    SharedPrefs.getInstance().setPanicPassword(confirmText);
                                    Snackbar.make(view, R.string.password_changed, Snackbar.LENGTH_SHORT).show();
                                    alertDialog.dismiss();
                                } else {
                                    oldPasswordLayout.setError(context.getString(R.string.incorrect_password));
                                    oldPassword.requestFocus();
                                    confirmLayout.setError(null);
                                    newPasswordLayout.setError(null);
                                }
                            } else {
                                newPasswordLayout.setError(context.getString(R.string.panic_secret_password_error));
                                newPassword.requestFocus();
                                confirmLayout.setError(null);
                            }
                        }
                    }

                });
            }
        });
        alertDialog.show();

    }

    public static AlertDialog showMetadataSwitchDialog(final Context context, final CameraPreviewAnonymousButton metadataCameraButton) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        LayoutInflater inflater = LayoutInflater.from(context);

        @SuppressLint("InflateParams")
        View view = inflater.inflate(R.layout.enable_metadata_dialog_layout, null);
        builder.setView(view);

        final SwitchCompat metadataSwitch = view.findViewById(R.id.anonymous_switch);
        metadataSwitch.setChecked(!Preferences.isAnonymousMode());

        builder.setView(view)
                .setPositiveButton(R.string.save, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Preferences.setAnonymousMode(!metadataSwitch.isChecked());
                        metadataCameraButton.displayDrawable();
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .setCancelable(true);

        final AlertDialog alertDialog = builder.create();
        alertDialog.show();

        return alertDialog;
    }

    public static void showNewPasswordDialog(final int caller, final Context context, final OnPasswordCreateListener listener) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        LayoutInflater inflater = LayoutInflater.from(context);
        @SuppressLint("InflateParams") View dialogView = inflater.inflate(R.layout.create_password_dialog_layout, null);
        builder.setTitle(caller == 1 ? context.getString(R.string.create_new_secret_passcode) : context.getString(R.string.create_new_panic_password))
                .setPositiveButton(R.string.save, null)
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                })
                .setView(dialogView);

        final TextInputLayout passwordLayout = dialogView.findViewById(R.id.password_layout);
        final TextInputLayout confirmLayout = dialogView.findViewById(R.id.password_confirm_layout);
        final EditText password = dialogView.findViewById(R.id.password);
        final EditText passwordConfirm = dialogView.findViewById(R.id.password_confirm);
        assert passwordLayout != null;
        passwordLayout.setError(null);
        assert confirmLayout != null;
        confirmLayout.setError(null);

        final AlertDialog alertDialog = builder.create();

        ViewUtil.setDialogSoftInputModeVisible(alertDialog);

        alertDialog.setCancelable(true);
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

                        confirmLayout.setError(null);
                        passwordLayout.setError(null);

                        String confirmText = passwordConfirm.getText().toString();

                        if (!(passwordText.length() > 0)) {
                            passwordLayout.requestFocus();
                            passwordLayout.setError(context.getString(R.string.empty_field_error));
                            return;
                        }
                        if (!(confirmText.length() > 0)) {
                            confirmLayout.requestFocus();
                            confirmLayout.setError(context.getString(R.string.empty_field_error));
                            return;
                        }
                        if (!passwordText.equals(confirmText)) {
                            confirmLayout.requestFocus();
                            confirmLayout.setError(context.getString(R.string.password_match_error));
                            passwordLayout.setError(null);
                            return;
                        }

                        if (!passwordText.equals(SharedPrefs.getInstance().getPanicPassword())) {
                            if (caller == 1) {
                                Preferences.setSecretPassword(passwordText);
                            } else {
                                SharedPrefs.getInstance().setPanicPassword(passwordText);
                            }
                            listener.onPasswordCreated();
                            alertDialog.dismiss();
                        } else {
                            passwordLayout.requestFocus();
                            confirmLayout.setError(null);
                            passwordLayout.setError(context.getString(R.string.panic_secret_password_error));
                        }
                    }
                });
            }
        });

        alertDialog.show();
    }

    public static AlertDialog showMetadataProgressBarDialog(Context context, DialogInterface.OnClickListener listener) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);

        LayoutInflater inflater = LayoutInflater.from(context);

        @SuppressLint("InflateParams")
        View view = inflater.inflate(R.layout.metadata_dialog_layout, null);
        builder.setView(view)
                .setNegativeButton(R.string.skip, listener)
                .setCancelable(false);


        final AlertDialog alertDialog = builder.create();
        alertDialog.show();

        return alertDialog;
    }

    public static void showTrustedContactDialog(int title, final Context context, final TrustedPerson trustedPerson, final OnTrustedPersonInteractionListener listener) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(context);

        Activity activity = (Activity) context;
        LayoutInflater inflater = activity.getLayoutInflater();

        @SuppressLint("InflateParams")
        View dialogView = inflater.inflate(R.layout.add_new_trusted_person_dialog, null);
        builder.setView(dialogView);
        builder.setTitle(title);
        builder.setPositiveButton(R.string.save, null);
        builder.setNegativeButton(R.string.cancel, null);

        final TextInputLayout titleLayout = dialogView.findViewById(R.id.new_person_title_layout);
        final TextInputLayout phoneLayout = dialogView.findViewById(R.id.new_person_phone_layout);
        final EditText name = dialogView.findViewById(R.id.person_name);
        final EditText phoneNumber = dialogView.findViewById(R.id.person_phone);


        name.setText(trustedPerson.getName());
        phoneNumber.setText(trustedPerson.getPhoneNumber());

        assert titleLayout != null;
        titleLayout.setError(null);


        final AlertDialog alertDialog = builder.create();
        ViewUtil.setDialogSoftInputModeVisible(alertDialog);

        alertDialog.setCancelable(true);
        alertDialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialog) {
                Button button = alertDialog.getButton(DialogInterface.BUTTON_POSITIVE);
                button.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        String nameText = name.getText().toString();
                        phoneLayout.setError(null);
                        titleLayout.setError(null);

                        String phoneNumberText = phoneNumber.getText().toString();
                        if (nameText.length() > 0) {
                            if (phoneNumberText.length() > 0) {

                                TrustedPerson person = new TrustedPerson();
                                if (!TextUtils.isEmpty(trustedPerson.getName())) {
                                    person.setColumnId(trustedPerson.getColumnId());
                                }
                                person.setName(nameText);
                                person.setPhoneNumber(phoneNumberText);
                                listener.onTrustedPersonInteraction(person);

                                alertDialog.dismiss();
                            } else {
                                phoneLayout.setError(context.getString(R.string.empty_field_error));
                                phoneLayout.requestFocus();
                            }
                        } else {
                            titleLayout.setError(context.getString(R.string.empty_field_error));
                            titleLayout.requestFocus();
                        }
                    }
                });
            }
        });
        alertDialog.show();
    }

    public static AlertDialog showFormInstanceDeleteDialog(
            @NonNull Context context,
            CollectFormInstanceStatus status,
            @NonNull DialogInterface.OnClickListener listener) {
        int msgResId;

        switch (status) {
            case SUBMITTED:
                msgResId = R.string.ra_delete_cloned_form;
                break;

            case DRAFT:
                msgResId = R.string.ra_delete_draft_form;
                break;

            default:
                msgResId = R.string.ra_delete_form;
                break;
        }

        return new AlertDialog.Builder(context)
                .setMessage(msgResId)
                .setPositiveButton(R.string.delete, listener)
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                })
                .setCancelable(true)
                .show();
    }

    public static AlertDialog showExportMediaDialog(@NonNull Context context, @NonNull DialogInterface.OnClickListener listener) {
        return new AlertDialog.Builder(context)
                .setTitle(R.string.ra_save_to_device_storage)
                .setMessage(R.string.ra_saving_outside_whistler_message)
                .setPositiveButton(R.string.save, listener)
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                })
                .setCancelable(true)
                .show();
    }
}
