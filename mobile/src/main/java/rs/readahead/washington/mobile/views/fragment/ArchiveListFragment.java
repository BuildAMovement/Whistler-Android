package rs.readahead.washington.mobile.views.fragment;

import android.support.v4.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import rs.readahead.washington.mobile.R;
import rs.readahead.washington.mobile.models.Report;
import rs.readahead.washington.mobile.views.adapters.ArchiveAdapter;


public class ArchiveListFragment extends Fragment {

    @BindView(R.id.report_list) RecyclerView mList;
    @BindView(R.id.empty_list_archived) TextView mEmptyList;

    private Unbinder unbinder;
    private OnSendInteractionListener mSendListener;
    private OnPreviewInteractionListener mPreviewListener;
    private OnDeleteInteractionListener mDeleteListener;
    private List<Report> reports = new ArrayList<>();


    public ArchiveListFragment() {
    }

    public void setData(List<Report> reports){
        this.reports = reports;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_archive_list, container, false);

        unbinder = ButterKnife.bind(this, view);

        Context context = view.getContext();
        mList.setLayoutManager(new LinearLayoutManager(context));

        if (reports.size() > 0) {
            mEmptyList.setVisibility(View.GONE);
            mList.setVisibility(View.VISIBLE);
            mList.setAdapter(new ArchiveAdapter(reports, mSendListener, mPreviewListener,mDeleteListener, getContext()));
        } else {
            mEmptyList.setVisibility(View.VISIBLE);
            mList.setVisibility(View.GONE);
        }

        return view;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnSendInteractionListener && context instanceof OnPreviewInteractionListener) {
            mSendListener = (OnSendInteractionListener) context;
            mPreviewListener = (OnPreviewInteractionListener) context;
            mDeleteListener = (OnDeleteInteractionListener) context;

        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener and OnEditInteractionListener and OnDeleteInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mSendListener = null;
        mPreviewListener = null;
    }

    public interface OnSendInteractionListener {
        void onSendInteraction(Report report);
    }

    public interface OnPreviewInteractionListener {
        void onPreviewInteraction(Report report);
    }

    public interface OnDeleteInteractionListener {
        void onDeleteFragmentInteraction(Report report);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        unbinder.unbind();
    }
}
