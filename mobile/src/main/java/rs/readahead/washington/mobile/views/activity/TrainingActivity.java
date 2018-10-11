package rs.readahead.washington.mobile.views.activity;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;
import rs.readahead.washington.mobile.MyApplication;
import rs.readahead.washington.mobile.R;
import rs.readahead.washington.mobile.bus.EventCompositeDisposable;
import rs.readahead.washington.mobile.bus.EventObserver;
import rs.readahead.washington.mobile.bus.event.DeleteTrainModuleEvent;
import rs.readahead.washington.mobile.bus.event.TrainModuleClickedEvent;
import rs.readahead.washington.mobile.bus.event.TrainModuleDownloadErrorEvent;
import rs.readahead.washington.mobile.bus.event.TrainingModuleDownloadedEvent;
import rs.readahead.washington.mobile.domain.entity.IErrorBundle;
import rs.readahead.washington.mobile.domain.entity.TrainModule;
import rs.readahead.washington.mobile.mvp.contract.ITrainListPresenterContract;
import rs.readahead.washington.mobile.mvp.presenter.TrainListPresenter;
import rs.readahead.washington.mobile.util.DialogsUtil;
import rs.readahead.washington.mobile.util.StringUtils;
import rs.readahead.washington.mobile.views.adapters.TrainModuleAdapter;


public class TrainingActivity extends CacheWordSubscriberBaseActivity implements
        ITrainListPresenterContract.IView {
    @BindView(R.id.trainings)
    RecyclerView recyclerView;
    @BindView(R.id.blank_train_list_info)
    TextView blankTrainListInfo;
    @BindView(R.id.toolbar)
    Toolbar toolbar;
    @BindView(R.id.fab)
    FloatingActionButton fab;

    private TrainListPresenter presenter;
    private EventCompositeDisposable disposables;
    private TrainModuleAdapter adapter;
    private List<TrainModule> trainModules = new ArrayList<>();
    private MenuItem searchItem;
    private ProgressDialog progressDialog;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_train);
        ButterKnife.bind(this);

        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationIcon(R.drawable.ic_arrow_back_white_24dp);
        setSupportActionBar(toolbar);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setTitle(R.string.ra_train);
        }

        adapter = new TrainModuleAdapter(trainModules);

        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(mLayoutManager);
        recyclerView.setAdapter(adapter);

        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                presenter.getModules();
            }
        });

        disposables = MyApplication.bus().createCompositeDisposable();
        disposables.wire(TrainModuleClickedEvent.class, new EventObserver<TrainModuleClickedEvent>() {
            @Override
            public void onNext(TrainModuleClickedEvent event) {
                handleTrainModuleClick(event.getModule());
            }
        });
        disposables.wire(DeleteTrainModuleEvent.class, new EventObserver<DeleteTrainModuleEvent>() {
            @Override
            public void onNext(DeleteTrainModuleEvent event) {
                showRemoveModuleDialog(event.getModule());
            }
        });
        disposables.wire(TrainingModuleDownloadedEvent.class, new EventObserver<TrainingModuleDownloadedEvent>() {
            @Override
            public void onNext(TrainingModuleDownloadedEvent event) {
                refreshTrainList();
            }
        });
        disposables.wire(TrainModuleDownloadErrorEvent.class, new EventObserver<TrainModuleDownloadErrorEvent>() {
            @Override
            public void onNext(TrainModuleDownloadErrorEvent event) {
                refreshTrainList();
            }
        });

        presenter = new TrainListPresenter(this);
        if (MyApplication.isConnectedToInternet(getContext())) {
            presenter.getModules();
        } else {
            presenter.listModules();
        }
    }

    @Override
    protected void onDestroy() {
        if (disposables != null) {
            disposables.dispose();
        }
        stopPresenter();
        hideProgressDialog();
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.train_menu, menu);

        searchItem = menu.findItem(R.id.menu_item_search);
        final SearchView searchView = (SearchView) searchItem.getActionView();

        searchView.setQueryHint(getString(R.string.ra_search_train_module));

        EditText searchEditText = searchView.findViewById(android.support.v7.appcompat.R.id.search_src_text);
        searchEditText.setTextColor(getResources().getColor(R.color.wa_black));
        searchEditText.setHintTextColor(getResources().getColor(R.color.wa_gray));
        searchEditText.setTextSize(16); //todo: move to R

        searchItem.setOnActionExpandListener(new MenuItem.OnActionExpandListener() {
            @Override
            public boolean onMenuItemActionCollapse(MenuItem item) {
                toolbar.setBackgroundColor(getResources().getColor(R.color.wa_black));
                toolbar.setNavigationIcon(R.drawable.ic_arrow_back_white_24dp);
                fab.setVisibility(View.VISIBLE);
                presenter.listModules();
                return true;
            }

            @Override
            public boolean onMenuItemActionExpand(MenuItem item) {
                toolbar.setBackgroundColor(getResources().getColor(R.color.wa_white));
                fab.setVisibility(View.INVISIBLE);
                adapter.clearList();
                return true;
            }
        });

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String s) {
                presenter.searchModules(searchView.getQuery().toString());
                return false;
            }

            @Override
            public boolean onQueryTextChange(String s) {
                return false;
            }
        });

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onTrainModulesSuccess(List<TrainModule> modules) {
        adapter.updateAdapter(modules);
        blankTrainListInfo.setVisibility(modules.isEmpty() ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onTrainModulesError(IErrorBundle error) {
    }

    @Override
    public void onTrainModulesStarted() {
        progressDialog = DialogsUtil.showProgressDialog(this, getString(R.string.ra_getting_train_modules));
    }

    @Override
    public void onTrainModulesEnded() {
        hideProgressDialog();
    }

    @Override
    public void onTrainModuleRemoved(TrainModule module) {
        showToast(R.string.ra_train_module_removed);
        presenter.listModules(); // it will work..
    }

    @Override
    public void onTrainModuleRemoveError(Throwable throwable) {
        showToast(R.string.ra_train_modeule_remove_error);
    }

    @Override
    public void onTrainModuleDownloadStarted(TrainModule module) {
        showToast(R.string.ra_train_modle_dl_started);
        refreshTrainList();
    }

    @Override
    public void onTrainModuleDownloadError(Throwable throwable) {
        showToast(R.string.ra_train_modele_dl_start_error);
    }

    @Override
    public Context getContext() {
        return this;
    }

    private void stopPresenter() {
        if (presenter != null) {
            presenter.destroy();
        }
    }

    private void hideProgressDialog() {
        if (progressDialog != null) {
            progressDialog.dismiss();
            progressDialog = null;
        }
    }

    private void handleTrainModuleClick(TrainModule module) {
        TrainModule appModule = null;

        if (isSearchActive()) {
            appModule = presenter.getLocalTrainModule(module.getId());
        }

        if (appModule == null) {
            appModule = module;
        }

        switch (appModule.getDownloaded()) {
            case DOWNLOADED:
                startTrainingRoomActivity(appModule);
                break;

            case NOT_DOWNLOADED:
                showDownloadModuleDialog(appModule);
                break;

            case DOWNLOADING:
                showToast(R.string.ra_train_module_downloading);
                break;
        }
    }

    private void startTrainingRoomActivity(TrainModule module) {
        Intent intent = new Intent(this, TrainingRoomActivity2.class);
        intent.putExtra(TrainingRoomActivity2.TRAIN_MODULE_ID, module.getId());
        startActivity(intent);
    }

    private void showDownloadModuleDialog(final TrainModule module) {
        String message = getString(R.string.ra_module_dl_size);
        message = String.format(Locale.ROOT, message, StringUtils.getFileSize(module.getSize()));

        DialogsUtil.showMessageOKCancelWithTitle(this, message, module.getName(),
                getString(R.string.ra_download),
                getString(R.string.cancel),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        presenter.downloadTrainModule(module);
                        dialog.dismiss();
                    }
                }, null);
    }

    private void showRemoveModuleDialog(final TrainModule module) {
        String message = getString(R.string.ra_train_module_will_dl);

        DialogsUtil.showMessageOKCancelWithTitle(this, message, module.getName(),
                getString(R.string.delete),
                getString(R.string.cancel),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        presenter.removeTrainModule(module);
                        dialog.dismiss();
                    }
                }, null);
    }

    private boolean isSearchActive() {
        return searchItem != null && searchItem.isActionViewExpanded();
    }

    private void refreshTrainList() {
        if (!isSearchActive() && presenter != null) {
            presenter.listModules();
        }
    }
}
