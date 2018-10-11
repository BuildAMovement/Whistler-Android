package rs.readahead.washington.mobile.views.activity;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import rs.readahead.washington.mobile.R;
import rs.readahead.washington.mobile.domain.entity.collect.CollectServer;
import rs.readahead.washington.mobile.mvp.contract.ICollectServersPresenterContract;
import rs.readahead.washington.mobile.mvp.presenter.CollectServersPresenter;
import rs.readahead.washington.mobile.util.DialogsUtil;
import rs.readahead.washington.mobile.views.adapters.CollectServersAdapter;
import rs.readahead.washington.mobile.views.dialog.CollectServerDialogFragment;


public class CollectManageServersActivity extends CacheWordSubscriberBaseActivity implements
        ICollectServersPresenterContract.IView,
        CollectServerDialogFragment.CollectServerDialogHandler,
        CollectServersAdapter.ICollectServersAdapterHandler {
    @BindView(R.id.collect_servers_list)
    ListView listView;
    @BindView(R.id.fab)
    FloatingActionButton fabAdd;
    @BindView(R.id.collect_servers_blank_list_info)
    TextView blankFormsInfo;

    private CollectServersPresenter presenter;
    List<CollectServer> servers;
    private AlertDialog dialog;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_collect_manage_servers);
        ButterKnife.bind(this);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setTitle(R.string.ra_title_activity_collect_settings);
        }

        fabAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showCollectServerDialog(null);
            }
        });

        servers = new ArrayList<>();

        presenter = new CollectServersPresenter(this);
        presenter.getServers();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        stopPresenting();

        if (dialog != null && dialog.isShowing()) {
            dialog.dismiss();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.settings_collect_menu, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.help_item) {
            startCollectHelp();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void showLoading() {
    }

    @Override
    public void hideLoading() {
    }

    @Override
    public void onServersLoaded(List<CollectServer> servers) {
        blankFormsInfo.setVisibility(servers.isEmpty() ? View.VISIBLE : View.GONE);
        this.servers = servers;
        listView.setAdapter(new CollectServersAdapter(this, this, this.servers));
    }

    @Override
    public void onLoadServersError(Throwable throwable) {
        showToast(R.string.ra_collect_server_load_error);
    }

    @Override
    public void onCreatedServer(CollectServer server) {
        CollectServersAdapter adapter = getAdapter();
        if (adapter != null) {
            servers.add(server);
            adapter.notifyDataSetChanged();
            showToast(R.string.ra_server_created);
        }
        blankFormsInfo.setVisibility(servers.isEmpty() ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onCreateServerError(Throwable throwable) {
        showToast(R.string.ra_collect_server_create_error);
    }

    @Override
    public void onUpdatedServer(CollectServer server) {
        CollectServersAdapter adapter = getAdapter();
        int i = servers.indexOf(server);

        if (adapter != null && i != -1) {
            servers.set(i, server);
            adapter.notifyDataSetChanged();
            showToast(R.string.ra_server_updated);
        }
    }

    @Override
    public void onUpdateServerError(Throwable throwable) {
        showToast(R.string.ra_collect_server_update_error);
    }

    @Override
    public void onRemovedServer(CollectServer server) {
        CollectServersAdapter adapter = getAdapter();
        if (adapter != null) {
            servers.remove(server);
            adapter.notifyDataSetChanged();
            showToast(R.string.ra_server_removed);
        }
        blankFormsInfo.setVisibility(servers.isEmpty() ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onRemoveServerError(Throwable throwable) {
        showToast(R.string.ra_collect_server_remove_error);
    }

    @Override
    public Context getContext() {
        return this;
    }

    @Override
    public void onCollectServersAdapterEdit(CollectServer server) {
        showCollectServerDialog(server);
    }

    @Override
    public void onCollectServersAdapterRemove(final CollectServer server) {
        dialog = DialogsUtil.showDialog(this,
                getString(R.string.ra_server_remove_confirmation_text),
                getString(R.string.ra_remove),
                getString(R.string.cancel),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        presenter.remove(server);
                        dialog.dismiss();
                    }
                }, null);
    }

    @Override
    public void onCollectServerDialogCreate(CollectServer server) {
        presenter.create(server);
    }

    @Override
    public void onCollectServerDialogUpdate(CollectServer server) {
        presenter.update(server);
    }

    private void showCollectServerDialog(@Nullable CollectServer server) {
        CollectServerDialogFragment.newInstance(server)
                .show(getSupportFragmentManager(), CollectServerDialogFragment.TAG);
    }

    private void stopPresenting() {
        if (presenter != null) {
            presenter.destroy();
            presenter = null;
        }
    }

    @Nullable
    private CollectServersAdapter getAdapter() {
        return (CollectServersAdapter) listView.getAdapter();
    }

    private void startCollectHelp() {
        startActivity(new Intent(CollectManageServersActivity.this, CollectHelpActivity.class));
    }
}
