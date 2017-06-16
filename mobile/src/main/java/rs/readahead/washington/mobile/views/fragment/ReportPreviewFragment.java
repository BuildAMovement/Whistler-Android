package rs.readahead.washington.mobile.views.fragment;

import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.File;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import rs.readahead.washington.mobile.MyApplication;
import rs.readahead.washington.mobile.R;
import rs.readahead.washington.mobile.bus.event.ShowEvidenceEvent;
import rs.readahead.washington.mobile.domain.entity.Evidence;
import rs.readahead.washington.mobile.models.MediaRecipient;
import rs.readahead.washington.mobile.models.Report;
import rs.readahead.washington.mobile.util.CreateViewUtil;
import rs.readahead.washington.mobile.util.DateUtil;
import rs.readahead.washington.mobile.util.StringUtils;


public class ReportPreviewFragment extends Fragment implements View.OnClickListener {

    @BindView(R.id.evidence_list) LinearLayout mEvidenceList;
    @BindView(R.id.recipients_list) LinearLayout mRecipientList;
    @BindView(R.id.preview_date) TextView mDate;
    @BindView(R.id.preview_title) TextView mTitle;
    @BindView(R.id.preview_content) TextView mContent;
    @BindView(R.id.metadata_status) TextView mMetadata;
    @BindView(R.id.preview_location) TextView mLocation;
    @BindView(R.id.header_picture) ImageView mHeader;

    private Unbinder unbinder;
    private Report report;
    private boolean backfire = false;


    public ReportPreviewFragment() {
    }

    public void setData(Report report) {
        this.report = report;
    }

    public void setReport(Report report) {
        this.report = report;
        backfire = true;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_oppression_preview, container, false);
        unbinder = ButterKnife.bind(this, view);
        return view;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        populateViews();
    }

    private void populateViews() {
        if (backfire) {
            mHeader.setVisibility(View.GONE);
        }

        if (report == null) {
            return;
        }

        mTitle.setText(StringUtils.orDefault(report.getTitle(), getString(R.string.title_not_included)));
        mContent.setText(StringUtils.orDefault(report.getContent(), getString(R.string.content_not_included)));
        mLocation.setText(StringUtils.orDefault(report.getLocation(), getString(R.string.not_included)));
        mMetadata.setText(report.isMetadataSelected() ? getString(R.string.included) : getString(R.string.not_included));
        mDate.setText(DateUtil.getStringFromDate(report.getDate()));
        createFileViews();
        createRecipients();

    }

    private void createFileViews() {
        if (report.getEvidences().size() > 0) {
            for (Evidence evidence : report.getEvidences()) {
                View view = CreateViewUtil.getEvidenceItem(evidence.getPath(), getContext());
                view.setOnClickListener(this);
                mEvidenceList.addView(view);
            }
        } else {
            mEvidenceList.addView(CreateViewUtil.createTextView(getString(R.string.not_included), getContext()));
        }
    }

    private void createRecipients() {
        for (MediaRecipient mediaRecipient: report.getRecipients().values()) {
            mRecipientList.addView(CreateViewUtil.createTextView(mediaRecipient.getTitle(), getContext()));
        }

        if (mRecipientList.getChildCount() == 0) {
            mRecipientList.addView(CreateViewUtil.createTextView(getString(R.string.not_included), getContext()));
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        unbinder.unbind();
    }

    @Override
    public void onClick(View v) {
        String path = (String) v.getContentDescription();
        File file = new File(path);
        MyApplication.bus().post(new ShowEvidenceEvent(getActivity(), Uri.fromFile(file)));
    }
}
