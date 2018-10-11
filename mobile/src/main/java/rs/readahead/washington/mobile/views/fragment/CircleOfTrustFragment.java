package rs.readahead.washington.mobile.views.fragment;


import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.v4.app.Fragment;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.github.clans.fab.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;
import info.guardianproject.cacheword.CacheWordHandler;
import rs.readahead.washington.mobile.R;
import rs.readahead.washington.mobile.data.database.DataSource;
import rs.readahead.washington.mobile.domain.entity.TrustedPerson;
import rs.readahead.washington.mobile.util.DialogsUtil;
import rs.readahead.washington.mobile.views.activity.OnTrustedPersonChangeListener;
import rs.readahead.washington.mobile.views.activity.OnTrustedPersonInteractionListener;
import rs.readahead.washington.mobile.views.adapters.CircleOfTrustAdapter;


public class CircleOfTrustFragment extends Fragment implements OnTrustedPersonChangeListener {

    @BindView(R.id.add_more)
    FloatingActionButton mAddMore;
    @BindView(R.id.trusted_list)
    RecyclerView mList;
    @BindView(R.id.blank_circle_info)
    TextView blankCircleInfo;

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
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
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
            dataSource = DataSource.getInstance(getContext(), mCacheWord.getEncryptionKey());
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
        blankCircleInfo.setVisibility(mPersons.isEmpty() ? View.VISIBLE : View.GONE);
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
        blankCircleInfo.setVisibility(mPersons.isEmpty() ? View.VISIBLE : View.GONE);
    }

    public void createNewTrustedContactDialog() {
        DialogsUtil.showTrustedContactDialog(R.string.new_recipient, getContext(), new TrustedPerson(), new OnTrustedPersonInteractionListener() {
            @Override
            public void onTrustedPersonInteraction(TrustedPerson trustedPerson) {
                dataSource.insertTrusted(trustedPerson);
                showToast(R.string.added_trusted);
                updateList();
            }
        });

    }

    @Override
    public void onContactEdited(TrustedPerson trustedPerson) {
        dataSource.updateTrusted(trustedPerson);
        showToast(R.string.updated_trusted);
        updateList();
    }

    @Override
    public void onContactDeleted(int id) {
        dataSource.deleteTrusted(id);
        showToast(R.string.removed_trusted);
    }

    private void showToast(@StringRes int resId) {
        Toast.makeText(getContext(), getString(resId), Toast.LENGTH_SHORT).show();
    }
}
