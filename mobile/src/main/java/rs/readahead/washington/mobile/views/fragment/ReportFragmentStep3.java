package rs.readahead.washington.mobile.views.fragment;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckedTextView;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;
import rs.readahead.washington.mobile.R;
import rs.readahead.washington.mobile.models.Report;
import rs.readahead.washington.mobile.views.activity.NewReportActivity;


public class ReportFragmentStep3 extends Fragment {
    @BindView(R.id.keep_record) CheckedTextView mKeepRecord;
    @BindView(R.id.report_public) CheckedTextView mPublicReport;
    @BindView(R.id.send) Button mSend;

    private Unbinder unbinder;
    private OnSendInteractionListener mSendListener;
    private Report mReport;


    public ReportFragmentStep3() {
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        if (! (context instanceof OnSendInteractionListener)) {
            throw new IllegalArgumentException();
        }

        mSendListener = (OnSendInteractionListener) context;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_report_step3, container, false);
        unbinder = ButterKnife.bind(this, view);
        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mReport = ((NewReportActivity) getActivity()).getReport();
    }

    @Override
    public void onStart() {
        super.onStart();
        populateViews();
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        if (isVisibleToUser) {
            updatePreviewFragment();
        }
    }

    private void updatePreviewFragment() {
        if (getActivity() != null) {
            ReportPreviewFragment fragment = new ReportPreviewFragment();
            fragment.setReport(mReport);
            getActivity().getSupportFragmentManager().beginTransaction()
                    .add(R.id.fragment_container, fragment).commit();
        }
    }

    private void populateViews() {
        updatePreviewFragment();
        mKeepRecord.post(new Runnable() {
            @Override
            public void run() {
                mKeepRecord.setChecked(mReport.isKeptInArchive());
                mPublicReport.setChecked(mReport.isReportPublic());
            }
        });
    }

    public interface OnSendInteractionListener {
        void onSendInteraction();
    }

    @OnClick({R.id.keep_record, R.id.send, R.id.report_public})
    public void handleClick(View view) {
        switch (view.getId()) {
            case R.id.keep_record:
                CheckedTextView checkedTextView = (CheckedTextView) view;
                mReport.setKeptInArchive(!checkedTextView.isChecked());
                mKeepRecord.setChecked(!checkedTextView.isChecked());
                break;
            case R.id.send:
                mSendListener.onSendInteraction();
                break;
            case R.id.report_public:
                CheckedTextView publicReport = (CheckedTextView) view;
                mReport.setReportPublic(!publicReport.isChecked());
                mPublicReport.setChecked(!publicReport.isChecked());
                break;
        }
    }

    @Override
    public void onDestroyView() {
        unbinder.unbind();
        super.onDestroyView();
    }

    @Override
    public void onDetach() {
        mSendListener = null;
        super.onDetach();
    }
}
