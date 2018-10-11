package rs.readahead.washington.mobile.views.fragment;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import rs.readahead.washington.mobile.R;
import rs.readahead.washington.mobile.domain.entity.IErrorBundle;
import rs.readahead.washington.mobile.domain.entity.collect.CollectForm;
import rs.readahead.washington.mobile.domain.entity.collect.ListFormResult;
import rs.readahead.washington.mobile.mvp.contract.ICollectBlankFormListPresenterContract;
import rs.readahead.washington.mobile.mvp.presenter.CollectBlankFormListPresenter;
import rs.readahead.washington.mobile.util.DialogsUtil;
import rs.readahead.washington.mobile.views.adapters.CollectFormRecycleViewAdapter;
import timber.log.Timber;


public class BlankFormsListFragment extends FormListFragment implements
        ICollectBlankFormListPresenterContract.IView {
    @BindView(R.id.blankForms)
    RecyclerView recyclerView;
    @BindView(R.id.blank_forms_info)
    TextView blankFormsInfo;

    private CollectBlankFormListPresenter presenter;
    private Unbinder unbinder;
    private CollectFormRecycleViewAdapter adapter;

    private ProgressDialog progressDialog;

    public static BlankFormsListFragment newInstance() {
        return new BlankFormsListFragment();
    }

    @Override
    public Type getFormListType() {
        return Type.BLANK;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        adapter = new CollectFormRecycleViewAdapter();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_blank_forms_list, container, false);
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
    }

    @Override
    public void onStart() {
        super.onStart();
        listBlankForms();
    }

    @Override
    public void onDestroy() {
        destroyPresenter();
        hideProgressDialog();
        super.onDestroy();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        unbinder.unbind();
    }

    @Override
    public void showBlankFormRefreshLoading() {
        progressDialog = DialogsUtil.showProgressDialog(getActivity(), getString(R.string.ra_getting_blank_forms));
    }

    @Override
    public void hideBlankFormRefreshLoading() {
        hideProgressDialog();
    }

    @Override
    public void onBlankFormsListResult(ListFormResult listFormResult) {
        blankFormsInfo.setVisibility(listFormResult.getForms().isEmpty() ? View.VISIBLE : View.GONE);
        adapter.setForms(listFormResult.getForms());

        // todo: make this multiply errors friendly
        for (IErrorBundle error : listFormResult.getErrors()) {
            Toast.makeText(getActivity(), String.format("%s %s", getString(R.string.ra_error_getting_forms), error.getServerName()), Toast.LENGTH_SHORT).show();
            Timber.d(error.getException(), getClass().getName());
        }
    }

    @Override
    public void onBlankFormsListError(Throwable throwable) {
        Timber.d(throwable, getClass().getName());
    }

    @Override
    public void onNoConnectionAvailable() {
        Toast.makeText(getActivity(), R.string.ra_no_connection_available, Toast.LENGTH_SHORT).show();
    }

    public void listBlankForms() {
        if (presenter != null) {
            presenter.listBlankForms();
        }
    }

    public void refreshBlankForms() {
        if (presenter != null) {
            presenter.refreshBlankForms();
        }
    }

    public void updateForm(CollectForm form) {
        adapter.updateForm(form);
    }

    private void hideProgressDialog() {
        if (progressDialog != null) {
            progressDialog.dismiss();
            progressDialog = null;
        }
    }

    private void createPresenter() {
        if (presenter == null) {
            presenter = new CollectBlankFormListPresenter(this); // todo: move presenter creation out of fragments
        }
    }

    private void destroyPresenter() {
        if (presenter != null) {
            presenter.destroy();
            presenter = null;
        }
    }
}
