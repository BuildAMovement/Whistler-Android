package rs.readahead.washington.mobile.views.fragment;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import rs.readahead.washington.mobile.R;
import rs.readahead.washington.mobile.domain.entity.collect.CollectFormInstance;
import rs.readahead.washington.mobile.mvp.contract.ICollectFormInstanceListPresenterContract;
import rs.readahead.washington.mobile.mvp.presenter.CollectFormInstanceListPresenter;
import rs.readahead.washington.mobile.views.adapters.CollectDraftFormInstanceRecycleViewAdapter;
import timber.log.Timber;


public class DraftFormsListFragment extends FormListFragment implements
        ICollectFormInstanceListPresenterContract.IView {
    @BindView(R.id.draftFormInstances)
    RecyclerView recyclerView;

    private Unbinder unbinder;
    private CollectDraftFormInstanceRecycleViewAdapter adapter;
    private CollectFormInstanceListPresenter presenter;


    public static DraftFormsListFragment newInstance() {
        return new DraftFormsListFragment();
    }

    @Override
    public Type getFormListType() {
        return Type.DRAFT;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        adapter = new CollectDraftFormInstanceRecycleViewAdapter();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_draft_forms_list, container, false);
        unbinder = ButterKnife.bind(this, rootView);

        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(getActivity());
        recyclerView.setLayoutManager(mLayoutManager);
        recyclerView.setAdapter(adapter);

        createPresenter();

        return rootView;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        listDraftForms();
    }

    @Override
    public void onDestroy() {
        destroyPresenter();
        super.onDestroy();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        unbinder.unbind();
    }

    @Override
    public void onFormInstanceListSuccess(List<CollectFormInstance> instances) {
        adapter.setInstances(instances);
    }

    @Override
    public void onFormInstanceListError(Throwable error) {
        Timber.d(error, getClass().getName());
    }

    public void listDraftForms() {
        if (presenter != null) {
            presenter.listDraftFormInstances();
        }
    }

    private void createPresenter() {
        if (presenter == null) {
            presenter = new CollectFormInstanceListPresenter(this);
        }
    }

    private void destroyPresenter() {
        if (presenter != null) {
            presenter.destroy();
            presenter = null;
        }
    }
}
