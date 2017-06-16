package rs.readahead.washington.mobile.views.fragment;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckedTextView;
import android.widget.LinearLayout;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import rs.readahead.washington.mobile.R;
import rs.readahead.washington.mobile.models.MediaRecipient;
import rs.readahead.washington.mobile.models.MediaRecipientList;
import rs.readahead.washington.mobile.models.Report;
import rs.readahead.washington.mobile.views.activity.NewReportActivity;


public class ReportFragmentStep1 extends Fragment implements View.OnClickListener {
    @BindView(R.id.recipientListsList)
    LinearLayout recipientListsList;
    @BindView(R.id.recipientList)
    LinearLayout recipientList;

    private Unbinder unbinder;
    private Report report; // wizard context
    private boolean recipientSelectionDirty;
    private IReportWizardHandler wizardHandler;


    public static ReportFragmentStep1 newInstance() {
        return new ReportFragmentStep1();
    }

    public ReportFragmentStep1() {
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        if (! (context instanceof IReportWizardHandler)) {
            throw new IllegalArgumentException();
        }

        wizardHandler = (IReportWizardHandler) context;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_report_step1, container, false);
        unbinder = ButterKnife.bind(this, rootView);
        return rootView;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        this.report = ((NewReportActivity) getActivity()).getReport();
    }

    @Override
    public void onStart() {
        super.onStart();
        updateFragmentLayout();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        unbinder.unbind();
    }

    @Override
    public void onDetach() {
        wizardHandler = null;
        super.onDetach();
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);

        if (! isVisibleToUser && recipientSelectionDirty) {
            wizardHandler.onRecipientSelectionChanged();
        }
    }

    private void updateFragmentLayout() {
        createRecipientListCheckBoxes();
        createRecipientCheckBoxes();
        recipientSelectionDirty = false;
    }

    private void createRecipientCheckBoxes() {
        LayoutInflater inflater = LayoutInflater.from(getContext());

        recipientList.removeAllViews();

        for (MediaRecipient recipient: report.getAllRecipients()) {
            CheckedTextView item = (CheckedTextView) createRecipientCheckBox(inflater, recipient);

            if (report.getSelectedRecipients().contains(recipient.getId())) {
                item.setChecked(true);
            }

            recipientList.addView(item);
        }
    }

    private void createRecipientListCheckBoxes() {
        LayoutInflater inflater = LayoutInflater.from(getContext());

        recipientListsList.removeAllViews();

        for (MediaRecipientList mediaRecipientList: report.getAllRecipientList()) {
            CheckedTextView item = (CheckedTextView) createRecipientListCheckBox(inflater, mediaRecipientList);

            if (report.getSelectedRecipientLists().contains(mediaRecipientList.getId())) {
                item.setChecked(true);
            }

            recipientListsList.addView(item);
        }
    }

    private View createRecipientCheckBox(LayoutInflater inflater, MediaRecipient recipient) {
        CheckedTextView item = (CheckedTextView) inflater
                .inflate(R.layout.media_recipient_checked_text_view, recipientList, false);

        item.setText(recipient.getTitle());
        item.setTag(recipient);
        item.setOnClickListener(this);

        return item;
    }

    private View createRecipientListCheckBox(LayoutInflater inflater, MediaRecipientList mediaRecipientList) {
        CheckedTextView item = (CheckedTextView) inflater
                .inflate(R.layout.media_recipient_checked_text_view, recipientListsList, false);

        item.setText(mediaRecipientList.getTitle());
        item.setTag(mediaRecipientList);
        item.setOnClickListener(this);

        return item;
    }

    @Override
    public void onClick(View v) {
        if (! (v instanceof CheckedTextView)) return;

        Object tag = v.getTag();
        CheckedTextView checkedTextView = (CheckedTextView) v;

        checkedTextView.toggle();

        if (tag instanceof MediaRecipient) {
            long id = ((MediaRecipient) tag).getId();

            if (checkedTextView.isChecked()) {
                report.addSelectedRecipient(id);
            } else {
                report.removeSelectedRecipient(id);
            }
        } else if (tag instanceof MediaRecipientList) {
            int id = ((MediaRecipientList) tag).getId();

            if (checkedTextView.isChecked()) {
                report.addSelectedRecipientList(id);
            } else {
                report.removeSelectedRecipientList(id);
            }
        }

        recipientSelectionDirty = true;
    }
}
