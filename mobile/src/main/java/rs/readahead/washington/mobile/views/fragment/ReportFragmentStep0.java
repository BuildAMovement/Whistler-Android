package rs.readahead.washington.mobile.views.fragment;

import android.content.Context;
import android.content.DialogInterface;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.widget.NestedScrollView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.File;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;
import rs.readahead.washington.mobile.MyApplication;
import rs.readahead.washington.mobile.R;
import rs.readahead.washington.mobile.bus.EventCompositeDisposable;
import rs.readahead.washington.mobile.bus.EventObserver;
import rs.readahead.washington.mobile.bus.event.RefreshEvidenceListEvent;
import rs.readahead.washington.mobile.bus.event.ShowEvidenceEvent;
import rs.readahead.washington.mobile.domain.entity.Evidence;
import rs.readahead.washington.mobile.models.Report;
import rs.readahead.washington.mobile.util.DialogsUtil;
import rs.readahead.washington.mobile.util.FileUtil;
import rs.readahead.washington.mobile.views.activity.NewReportActivity;


public class ReportFragmentStep0 extends Fragment implements View.OnClickListener {
    @BindView(R.id.take_photo)
    LinearLayout takePhoto;
    @BindView(R.id.record_video)
    LinearLayout recordVideo;
    @BindView(R.id.upload)
    LinearLayout uploadFromPhone;
    @BindView(R.id.record_audio)
    LinearLayout mRecordAudio;
    @BindView(R.id.evidence_list)
    LinearLayout mEvidenceList;
    @BindView(R.id.evidence_scroll)
    NestedScrollView mEvidenceScroll;

    private Unbinder unbinder;
    private Report mReport;
    private OnFragmentInteractionListener mListener;
    private OnRemoveEvidenceInteractionListener mRemoveEvidenceListener;
    private EventCompositeDisposable disposables;

    public interface OnFragmentInteractionListener {
        void onFragmentInteraction(int call);
    }

    public interface OnRemoveEvidenceInteractionListener {
        void onRemoveEvidenceInteraction(String path);
    }


    public ReportFragmentStep0() {
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener
                && context instanceof OnRemoveEvidenceInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
            mRemoveEvidenceListener = (OnRemoveEvidenceInteractionListener) context;
        } else {
            throw new IllegalArgumentException();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_report_step0, container, false);
        unbinder = ButterKnife.bind(this, view);

        disposables = MyApplication.bus().createCompositeDisposable();
        disposables.wire(RefreshEvidenceListEvent.class, new EventObserver<RefreshEvidenceListEvent>() {
            @Override
            public void onNext(RefreshEvidenceListEvent event) {
                // should be impossible for this to fire before complete fragment init
                updateEvidencesLayout();
            }
        });

        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mReport = ((NewReportActivity) getActivity()).getReport();
        updateEvidencesLayout();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        if (! disposables.isDisposed()) {
            disposables.dispose();
        }

        unbinder.unbind();
    }

    private void updateEvidencesLayout() {
        final LayoutInflater inflater = LayoutInflater.from(getContext());

        mEvidenceList.removeAllViews();

        for (Evidence evidence: mReport.getEvidences()) {
            View item = createEvidenceListItem(inflater, evidence.getPath());
            mEvidenceList.addView(item);
        }
    }

    @OnClick({R.id.take_photo, R.id.record_video, R.id.upload, R.id.record_audio})
    void startActivity(View view) {
        switch (view.getId()) {
            case R.id.record_video:
                mListener.onFragmentInteraction(Evidence.CAPTURED_VIDEO);
                break;

            case R.id.take_photo:
                mListener.onFragmentInteraction(Evidence.CAPTURED_IMAGE);
                break;

            case R.id.upload:
                mListener.onFragmentInteraction(Evidence.PICKED_IMAGE);
                break;

            case R.id.record_audio:
                mListener.onFragmentInteraction(Evidence.RECORDED_AUDIO);
                break;
        }
    }

    @Override
    public void onClick(View v) {
        String path = (String) v.getTag();
        File file = new File(path);
        MyApplication.bus().post(new ShowEvidenceEvent(getActivity(), Uri.fromFile(file)));
    }

    public View createEvidenceListItem(final LayoutInflater inflater, final String filePath) {
        final LinearLayout item = (LinearLayout) inflater.inflate(R.layout.evidence_list_item, mEvidenceList, false);

        final ImageView delete = (ImageView) item.findViewById(R.id.delete_report);
        final TextView path = (TextView) item.findViewById(R.id.evidence_item_path);

        delete.setOnClickListener(new RemoveOnClickListener());
        path.setText(FileUtil.getEvidenceFileDisplayText(filePath));

        item.setOnClickListener(this);
        item.setTag(filePath);

        return item;
    }

    private class RemoveOnClickListener implements View.OnClickListener {
        @Override
        public void onClick(final View v) {
            DialogsUtil.showMessageOKCancelWithTitle(getContext(), getString(R.string.remove_evidence),
                    getString(R.string.attention), getString(R.string.yes), getString(R.string.cancel),
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            LinearLayout linearLayout = (LinearLayout) v.getParent().getParent(); // oh dear, oh dear..
                            linearLayout.setVisibility(View.GONE);
                            String filePath = (String) linearLayout.getTag();
                            mReport.removeEvidence(filePath);
                            mRemoveEvidenceListener.onRemoveEvidenceInteraction(filePath);
                        }
                    }, null);

        }
    }
}
