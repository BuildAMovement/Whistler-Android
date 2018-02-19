package rs.readahead.washington.mobile.views.dialog;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import rs.readahead.washington.mobile.R;
import rs.readahead.washington.mobile.domain.entity.ContactSettingMethod;
import rs.readahead.washington.mobile.mvp.contract.IContactMethodPresenterContract;
import rs.readahead.washington.mobile.mvp.presenter.ContactMethodPresenter;
import rs.readahead.washington.mobile.util.ViewUtil;


public class ContactMethodDialogFragment extends DialogFragment implements
        IContactMethodPresenterContract.IView {
    public static final String TAG = ContactMethodDialogFragment.class.getSimpleName();

    public interface ContactSettingMethodChangeHandler {
        void onContactMethodChanged(int contactSettingMethodId);
    }

    private static final String CONTACT_METHOD_ID_KEY = "cmik";

    @BindView(R.id.radioGroup)
    RadioGroup radioGroup;

    private Unbinder unbinder;


    public static ContactMethodDialogFragment newInstance(@NonNull ContactSettingMethod method) {
        ContactMethodDialogFragment frag = new ContactMethodDialogFragment();

        Bundle args = new Bundle();
        args.putInt(CONTACT_METHOD_ID_KEY, method.getId());

        frag.setArguments(args);

        return frag;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        @SuppressLint("InflateParams")
        View dialogView = LayoutInflater.from(getActivity())
                .inflate(R.layout.dialog_contact_method, null);
        unbinder = ButterKnife.bind(this, dialogView);

        int currentMethodId = 0;
        if (getArguments() != null) {
            currentMethodId = getArguments().getInt(CONTACT_METHOD_ID_KEY);
        }

        ContactMethodPresenter presenter = new ContactMethodPresenter(this);
        List<ContactSettingMethod> recommended = presenter.getRecommendedMethods();
        List<ContactSettingMethod> other = presenter.getOtherMethods();
        presenter.destroy();

        radioGroup.addView(createContactMethodTitle(R.string.ra_recommended));

        for (ContactSettingMethod method: recommended) {
            RadioButton radioButton = createContactMethodRadio(method);
            radioGroup.addView(radioButton);
            if (method.getId() == currentMethodId) {
                radioButton.setChecked(true);
            }
        }

        radioGroup.addView(createContactMethodTitle(R.string.ra_other));

        for (ContactSettingMethod method: other) {
            RadioButton radioButton = createContactMethodRadio(method);
            radioGroup.addView(radioButton);
            if (method.getId() == currentMethodId) {
                radioButton.setChecked(true);
            }
        }

        AlertDialog dialog = new AlertDialog.Builder(getContext())
                .setTitle(R.string.ra_select_preferred_method_of_contact)
                .setView(dialogView)
                .setPositiveButton(R.string.save, null)
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
                        ContactSettingMethodChangeHandler activity = (ContactSettingMethodChangeHandler) getActivity();
                        if (activity != null) {
                            activity.onContactMethodChanged(getSelectedId());
                        }

                        dialog.dismiss();
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

    private RadioButton createContactMethodRadio(final ContactSettingMethod contactMethod) {
        @SuppressLint("InflateParams")
        RadioButton radioButton = (RadioButton) LayoutInflater.from(getContext())
                .inflate(R.layout.contact_method_item, null);
        radioButton.setText(contactMethod.getNameResId());
        radioButton.setTag(contactMethod.getId());

        return radioButton;
    }

    private View createContactMethodTitle(@StringRes int titleResId) {
        @SuppressLint("InflateParams")
        TextView textView = (TextView) LayoutInflater.from(getContext())
                .inflate(R.layout.contact_method_title, null);
        textView.setText(titleResId);

        return textView;
    }

    private int getSelectedId() {
        RadioButton checked = radioGroup.findViewById(radioGroup.getCheckedRadioButtonId());
        if (checked == null) {
            return 0;
        }

        Object tag = checked.getTag();
        if (tag == null || !(tag instanceof Integer)) {
            return 0;
        }

        return (int) tag;
    }
}
