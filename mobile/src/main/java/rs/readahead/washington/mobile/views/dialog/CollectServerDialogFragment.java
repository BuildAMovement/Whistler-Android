package rs.readahead.washington.mobile.views.dialog;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import rs.readahead.washington.mobile.R;
import rs.readahead.washington.mobile.domain.entity.collect.CollectServer;
import rs.readahead.washington.mobile.util.ViewUtil;


public class CollectServerDialogFragment extends DialogFragment {
    public static final String TAG = CollectServerDialogFragment.class.getSimpleName();

    private static final String TITLE_KEY = "tk";
    private static final String ID_KEY = "ik";
    private static final String OBJECT_KEY = "ok";

    @BindView(R.id.name_layout)
    TextInputLayout nameLayout;
    @BindView(R.id.name)
    EditText name;
    @BindView(R.id.url_layout)
    TextInputLayout urlLayout;
    @BindView(R.id.url)
    EditText url;
    @BindView(R.id.username_layout)
    TextInputLayout usernameLayout;
    @BindView(R.id.username)
    EditText username;
    @BindView(R.id.password_layout)
    TextInputLayout passwordLayout;
    @BindView(R.id.password)
    EditText password;

    private Unbinder unbinder;
    private boolean validated = true;

    public interface CollectServerDialogHandler {
        void onCollectServerDialogCreate(CollectServer server);
        void onCollectServerDialogUpdate(CollectServer server);
    }


    public static CollectServerDialogFragment newInstance(@Nullable CollectServer server) {
        CollectServerDialogFragment frag = new CollectServerDialogFragment();

        Bundle args = new Bundle();
        if (server == null) {
            args.putInt(TITLE_KEY, R.string.ra_add_collect_server);
        } else {
            args.putInt(TITLE_KEY, R.string.ra_server_settings);
            args.putSerializable(ID_KEY, server.getId());
            args.putSerializable(OBJECT_KEY, server);
        }

        frag.setArguments(args);

        return frag;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        @SuppressLint("InflateParams")
        View dialogView = LayoutInflater.from(getActivity()).inflate(R.layout.dialog_collect_server, null);
        unbinder = ButterKnife.bind(this, dialogView);

        int title = getArguments().getInt(TITLE_KEY);
        final long serverId = getArguments().getLong(ID_KEY, 0);
        Object obj = getArguments().getSerializable(OBJECT_KEY);

        if (obj != null) {
            CollectServer server = (CollectServer) obj;
            name.setText(server.getName());
            url.setText(server.getUrl());
            username.setText(server.getUsername());
            password.setText(server.getPassword());
        }

        AlertDialog dialog = new AlertDialog.Builder(getActivity())
                .setTitle(title)
                .setView(dialogView)
                .setPositiveButton(R.string.ok, null)
                .setNegativeButton(R.string.cancel, null)
                .create();

        ViewUtil.setDialogSoftInputModeVisible(dialog);

        dialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(final DialogInterface dialog) {
                Button button = ((AlertDialog) dialog).getButton(AlertDialog.BUTTON_POSITIVE);
                button.setOnClickListener(new View.OnClickListener() {

                    @Override
                    public void onClick(View view) {
                        validate();

                        if (validated) {
                            CollectServerDialogHandler activity = (CollectServerDialogHandler) getActivity();

                            if (serverId == 0) {
                                activity.onCollectServerDialogCreate(copyFields(new CollectServer()));
                            } else {
                                activity.onCollectServerDialogUpdate(copyFields(new CollectServer(serverId)));
                            }

                            dialog.dismiss();
                        }
                    }
                });
            }
        });

        return dialog;
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        super.onDismiss(dialog);
        unbinder.unbind();
    }

    private void validate() {
        validated = true;
        validateRequired(name, nameLayout);
        validateUrl(url, urlLayout);
        validateRequired(username, usernameLayout);
        validateRequired(password, passwordLayout);
    }

    private void validateRequired(EditText field, TextInputLayout layout) {
        if (TextUtils.isEmpty(field.getText().toString())) {
            layout.setError(getString(R.string.empty_field_error));
            validated = false;
        }
    }

    private void validateUrl(EditText field, TextInputLayout layout) {
        String url = field.getText().toString();

        if (TextUtils.isEmpty(url)) {
            layout.setError(getString(R.string.empty_field_error));
            validated = false;
        } else if (!Patterns.WEB_URL.matcher(url).matches()) {
            layout.setError(getString(R.string.not_web_url_field_error));
            validated = false;
        }
    }

    @NonNull
    private CollectServer copyFields(@NonNull CollectServer server) {
        server.setName(name.getText().toString());
        server.setUrl(url.getText().toString().trim());
        server.setUsername(username.getText().toString().trim());
        server.setPassword(password.getText().toString());

        return server;
    }
}
