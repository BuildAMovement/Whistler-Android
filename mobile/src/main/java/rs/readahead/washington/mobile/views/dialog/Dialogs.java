package rs.readahead.washington.mobile.views.dialog;

import android.annotation.SuppressLint;
import android.content.Context;
import android.support.annotation.Nullable;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

import butterknife.ButterKnife;
import rs.readahead.washington.mobile.R;
import rs.readahead.washington.mobile.domain.entity.MediaRecipient;
import rs.readahead.washington.mobile.domain.entity.MediaRecipientList;
import rs.readahead.washington.mobile.util.StringUtils;
import rs.readahead.washington.mobile.util.ViewUtil;


public class Dialogs {
    public interface IRecipientDialogListener {
        void call(MediaRecipient recipient);
    }

    public interface IRecipientListDialogListener {
        void call(MediaRecipientList recipientList);
    }

    public static AlertDialog showRecipientDialog(
            final Context context,
            @Nullable final MediaRecipient mediaRecipient,
            final IRecipientDialogListener recipientDialogListener) {
        String titleText, positiveText, negativeText;

        titleText = context.getString(mediaRecipient == null ? R.string.add_new_recipient : R.string.update_recipient);
        positiveText = context.getString(mediaRecipient == null ? R.string.action_add : R.string.action_update);
        negativeText = context.getString(R.string.cancel);

        AlertDialog.Builder builder = new AlertDialog.Builder(context);

        @SuppressLint("InflateParams")
        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_create_media_recipient, null);

        final TextInputLayout titleLayout = ButterKnife.findById(dialogView, R.id.recipient_title_layout);
        final EditText title = ButterKnife.findById(dialogView, R.id.recipient_title);
        final TextInputLayout emailLayout = ButterKnife.findById(dialogView, R.id.recipient_email_layout);
        final EditText email = ButterKnife.findById(dialogView, R.id.recipient_email);

        if (mediaRecipient != null) {
            title.setText(mediaRecipient.getTitle());
            email.setText(mediaRecipient.getMail());
        }

        titleLayout.setError(null);
        emailLayout.setError(null);

        builder.setView(dialogView);
        builder.setTitle(titleText);
        builder.setCancelable(true);
        builder.setNegativeButton(negativeText, null);
        builder.setPositiveButton(positiveText, null);

        final AlertDialog dialog = builder.show();

        ViewUtil.setDialogSoftInputModeVisible(dialog);

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String titleText = title.getText().toString();
                String emailText = email.getText().toString();

                if (validateValues(titleText, emailText)) {
                    return;
                }

                if (mediaRecipient == null) {
                    recipientDialogListener.call(new MediaRecipient(titleText, emailText));
                } else {
                    mediaRecipient.setTitle(titleText);
                    mediaRecipient.setMail(emailText);
                    recipientDialogListener.call(mediaRecipient);
                }

                dialog.dismiss();
            }

            private boolean validateValues(String titleText, String emailText) {
                if (titleText.length() == 0) {
                    titleLayout.setError(context.getString(R.string.empty_field_error));
                } else {
                    titleLayout.setError(null);
                }

                if (emailText.length() == 0) {
                    emailLayout.setError(context.getString(R.string.empty_field_error));
                } else if (! StringUtils.isEmailValid(emailText)) {
                    emailLayout.setError(context.getString(R.string.email_field_error));
                } else {
                    emailLayout.setError(null);
                }

                return (titleLayout.getError() != null || emailLayout.getError() != null);
            }
        });

        return dialog;
    }

    public static AlertDialog showAddRecipientListDialog(
            final Context context,
            final IRecipientListDialogListener recipientListDialogListener) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);

        @SuppressLint("InflateParams")
        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_create_media_recipient_list, null);

        final TextInputLayout titleLayout = ButterKnife.findById(dialogView, R.id.recipient_title_layout);
        final EditText title = ButterKnife.findById(dialogView, R.id.recipient_title);

        titleLayout.setError(null);

        builder.setView(dialogView);
        builder.setTitle(R.string.add_new_recipient_list);
        builder.setCancelable(true);
        builder.setPositiveButton(context.getString(R.string.action_add), null);
        builder.setNegativeButton(context.getString(R.string.cancel), null);

        final AlertDialog dialog = builder.show();

        ViewUtil.setDialogSoftInputModeVisible(dialog);

        // implemented this right after show to remove dismiss dialog on click
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String titleText = title.getText().toString();

                if (titleText.length() == 0) {
                    titleLayout.setError(context.getString(R.string.empty_field_error));
                    return;
                }

                recipientListDialogListener.call(new MediaRecipientList(titleText));

                dialog.dismiss();
            }
        });

        return dialog;
    }
}
