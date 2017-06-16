package rs.readahead.washington.mobile.views.fragment;


import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.github.clans.fab.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;
import info.guardianproject.cacheword.CacheWordHandler;
import rs.readahead.washington.mobile.R;
import rs.readahead.washington.mobile.database.DataSource;
import rs.readahead.washington.mobile.models.TrustedPerson;
import rs.readahead.washington.mobile.util.DialogsUtil;
import rs.readahead.washington.mobile.views.activity.OnTrustedPersonChangeListener;
import rs.readahead.washington.mobile.views.activity.OnTrustedPersonInteractionListener;
import rs.readahead.washington.mobile.views.adapters.CircleOfTrustAdapter;


public class CircleOfTrustFragment extends Fragment implements OnTrustedPersonChangeListener {

    @BindView(R.id.add_more) FloatingActionButton mAddMore;
    @BindView(R.id.trusted_list) RecyclerView mList;

    private Unbinder unbinder;
    private DataSource dataSource;
    private List<TrustedPerson> mPersons = new ArrayList<>();
    private CircleOfTrustAdapter adapter;
    private CacheWordHandler mCacheWord;

    public CircleOfTrustFragment() {
    }

    public void setWordHandler(CacheWordHandler mCacheWord) {
        this.mCacheWord = mCacheWord;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_circle_of_trust, container, false);
        unbinder = ButterKnife.bind(this, view);
        populateList();
        return view;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!mCacheWord.isLocked()) {
            dataSource = DataSource.getInstance(mCacheWord, getContext().getApplicationContext());
        }

    }

    private void populateList() {

        getAllTrustedContacts();

        mList.setHasFixedSize(false);
        adapter = new CircleOfTrustAdapter(mPersons, getContext(), this);
        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(getContext());
        mList.setLayoutManager(mLayoutManager);
        mList.setItemAnimator(new DefaultItemAnimator());
        mList.setAdapter(adapter);
    }

    private void getAllTrustedContacts() {
        mPersons = dataSource.getAllTrusted();
    }


    @OnClick({R.id.add_more})
    public void showDialog() {
        createNewTrustedContactDialog();

    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        unbinder.unbind();
    }

    public void updateList() {
        getAllTrustedContacts();
        adapter.updateAdapter(mPersons);

    }

    public void createNewTrustedContactDialog() {
        DialogsUtil.showTrustedContactDialog(R.string.new_recipient, getContext(), new TrustedPerson(), new OnTrustedPersonInteractionListener() {
            @Override
            public void onTrustedPersonInteraction(TrustedPerson trustedPerson) {
                dataSource.insertTrusted(trustedPerson);
                updateList();
            }
        });

    }

    @Override
    public void onContactEdited(TrustedPerson trustedPerson) {
        dataSource.updateTrusted(trustedPerson);
        updateList();
    }

    @Override
    public void onContactDeleted(int id) {
        dataSource.deleteTrusted(id);
    }
}
