package rs.readahead.washington.mobile.views.fragment;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
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
import rs.readahead.washington.mobile.views.adapters.DraftsRecyclerViewAdapter;


public class DraftsListFragment extends Fragment {
    @BindView(R.id.draft_list) RecyclerView mList;
    @BindView(R.id.empty_list) TextView mEmptyList;

    private Unbinder unbinder;
    private OnListFragmentInteractionListener mListener;
    private OnEditInteractionListener mEditListener;
    private OnDeleteInteractionListener mDeleteListener;
    private List<Report> reports = new ArrayList<>();


    public DraftsListFragment() {
    }

    public void setData(List<Report> reports) {
        this.reports = reports;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_item_list, container, false);

        unbinder = ButterKnife.bind(this, view);

        Context context = view.getContext();
        mList.setLayoutManager(new LinearLayoutManager(context));

        if (reports.size() > 0) {
            mEmptyList.setVisibility(View.GONE);
            mList.setVisibility(View.VISIBLE);
            mList.setAdapter(new DraftsRecyclerViewAdapter(reports, mListener, mEditListener, mDeleteListener, getContext()));
        } else {
            mEmptyList.setVisibility(View.VISIBLE);
            mList.setVisibility(View.GONE);
        }

        return view;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnListFragmentInteractionListener) {
            mListener = (OnListFragmentInteractionListener) context;
            mEditListener = (OnEditInteractionListener) context;
            mDeleteListener = (OnDeleteInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnListFragmentInteractionListener and OnEditInteractionListener and OnDeleteInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
        mEditListener = null;
    }

    public interface OnListFragmentInteractionListener {
        void onListFragmentInteraction(Report report);
    }

    public interface OnEditInteractionListener {
        void onEditFragmentInteraction(Report report);
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
