package rs.readahead.washington.mobile.views.activity;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.TabLayout;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import org.javarosa.core.model.FormDef;

import butterknife.BindView;
import butterknife.ButterKnife;
import permissions.dispatcher.NeedsPermission;
import permissions.dispatcher.OnNeverAskAgain;
import permissions.dispatcher.OnPermissionDenied;
import permissions.dispatcher.OnShowRationale;
import permissions.dispatcher.PermissionRequest;
import permissions.dispatcher.RuntimePermissions;
import rs.readahead.washington.mobile.MyApplication;
import rs.readahead.washington.mobile.R;
import rs.readahead.washington.mobile.bus.EventCompositeDisposable;
import rs.readahead.washington.mobile.bus.EventObserver;
import rs.readahead.washington.mobile.bus.event.CancelPendingFormInstanceEvent;
import rs.readahead.washington.mobile.bus.event.CollectFormInstanceDeletedEvent;
import rs.readahead.washington.mobile.bus.event.CollectFormSavedEvent;
import rs.readahead.washington.mobile.bus.event.CollectFormSubmissionErrorEvent;
import rs.readahead.washington.mobile.bus.event.CollectFormSubmittedEvent;
import rs.readahead.washington.mobile.bus.event.DeleteFormInstanceEvent;
import rs.readahead.washington.mobile.bus.event.DownloadBlankFormEntryEvent;
import rs.readahead.washington.mobile.bus.event.ReSubmitFormInstanceEvent;
import rs.readahead.washington.mobile.bus.event.ShowBlankFormEntryEvent;
import rs.readahead.washington.mobile.bus.event.ShowFormInstanceEntryEvent;
import rs.readahead.washington.mobile.bus.event.ToggleBlankFormFavoriteEvent;
import rs.readahead.washington.mobile.data.sharedpref.Preferences;
import rs.readahead.washington.mobile.domain.entity.collect.CollectForm;
import rs.readahead.washington.mobile.domain.entity.collect.CollectFormInstance;
import rs.readahead.washington.mobile.domain.entity.collect.CollectFormInstanceStatus;
import rs.readahead.washington.mobile.domain.entity.collect.OpenRosaResponse;
import rs.readahead.washington.mobile.javarosa.FormReSubmitter;
import rs.readahead.washington.mobile.javarosa.FormUtils;
import rs.readahead.washington.mobile.javarosa.IFormReSubmitterContract;
import rs.readahead.washington.mobile.mvp.contract.ICollectCreateFormControllerContract;
import rs.readahead.washington.mobile.mvp.contract.ICollectMainPresenterContract;
import rs.readahead.washington.mobile.mvp.presenter.CollectCreateFormControllerPresenter;
import rs.readahead.washington.mobile.mvp.presenter.CollectMainPresenter;
import rs.readahead.washington.mobile.odk.FormController;
import rs.readahead.washington.mobile.util.DialogsUtil;
import rs.readahead.washington.mobile.util.PermissionUtil;
import rs.readahead.washington.mobile.util.StringUtils;
import rs.readahead.washington.mobile.views.adapters.ViewPagerAdapter;
import rs.readahead.washington.mobile.views.fragment.BlankFormsListFragment;
import rs.readahead.washington.mobile.views.fragment.DraftFormsListFragment;
import rs.readahead.washington.mobile.views.fragment.FormListFragment;
import rs.readahead.washington.mobile.views.fragment.SubmittedFormsListFragment;
import timber.log.Timber;


@RuntimePermissions
public class CollectMainActivity extends CacheWordSubscriberBaseActivity implements
        ICollectMainPresenterContract.IView,
        ICollectCreateFormControllerContract.IView,
        IFormReSubmitterContract.IView {
    private EventCompositeDisposable disposables;
    private CollectMainPresenter presenter;
    private FormReSubmitter formReSubmitter;
    private CollectCreateFormControllerPresenter formControllerPresenter;
    // todo: check the need for two presenters here..

    @BindView(R.id.fab)
    FloatingActionButton fab;
    @BindView(R.id.tabs)
    TabLayout tabLayout;
    @BindView(R.id.container)
    View formsViewPager;
    @BindView(R.id.blank_forms_layout)
    View noServersView;
    @BindView(R.id.blank_forms_text)
    TextView blankFormsText;
    @BindView(R.id.send_report_info)
    TextView sendReportInfo;


    private AlertDialog alertDialog;
    private ProgressDialog progressDialog;

    private ViewPager mViewPager;
    private ViewPagerAdapter adapter;
    private long numOfCollectServers = 0;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_collect_main);
        ButterKnife.bind(this);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setTitle(R.string.title_activity_collect_main);
        }

        presenter = new CollectMainPresenter(this);
        formReSubmitter = new FormReSubmitter(this);

        initViewPageAdapter();
        final int blankFragmentPosition = getFragmentPosition(FormListFragment.Type.BLANK);

        mViewPager = findViewById(R.id.container);
        mViewPager.setAdapter(adapter);
        mViewPager.setCurrentItem(blankFragmentPosition);

        TabLayout tabLayout = findViewById(R.id.tabs);
        tabLayout.setupWithViewPager(mViewPager);

        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mViewPager.getCurrentItem() == blankFragmentPosition) {
                    getBlankFormsListFragment().refreshBlankForms();
                }
            }
        });

        mViewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            }

            @Override
            public void onPageSelected(int position) {
                fab.setVisibility((position == blankFragmentPosition && numOfCollectServers > 0) ? View.VISIBLE : View.GONE);
            }

            @Override
            public void onPageScrollStateChanged(int state) {
            }
        });

        blankFormsText.setText(Html.fromHtml(getString(R.string.collect_servers_info)));
        blankFormsText.setMovementMethod(LinkMovementMethod.getInstance());
        StringUtils.stripUnderlines(blankFormsText);

        sendReportInfo.setText(Html.fromHtml(getString(R.string.send_report_info)));
        sendReportInfo.setMovementMethod(LinkMovementMethod.getInstance());
        StringUtils.stripUnderlines(sendReportInfo);

        disposables = MyApplication.bus().createCompositeDisposable();
        disposables.wire(ShowBlankFormEntryEvent.class, new EventObserver<ShowBlankFormEntryEvent>() {
            @Override
            public void onNext(ShowBlankFormEntryEvent event) {
                showFormEntry(event.getForm());
            }
        });
        disposables.wire(DownloadBlankFormEntryEvent.class, new EventObserver<DownloadBlankFormEntryEvent>() {
            @Override
            public void onNext(DownloadBlankFormEntryEvent event) {
                downloadFormEntry(event.getForm());
            }
        });
        disposables.wire(ToggleBlankFormFavoriteEvent.class, new EventObserver<ToggleBlankFormFavoriteEvent>() {
            @Override
            public void onNext(ToggleBlankFormFavoriteEvent event) {
                toggleFormFavorite(event.getForm());
            }
        });
        disposables.wire(ShowFormInstanceEntryEvent.class, new EventObserver<ShowFormInstanceEntryEvent>() {
            @Override
            public void onNext(ShowFormInstanceEntryEvent event) {
                showFormInstanceEntry(event.getInstanceId());
            }
        });
        disposables.wire(CollectFormSubmittedEvent.class, new EventObserver<CollectFormSubmittedEvent>() {
            @Override
            public void onNext(CollectFormSubmittedEvent event) {
                getDraftFormsListFragment().listDraftForms();
                getSubmittedFormsListFragment().listSubmittedForms();
                setPagerToSubmittedFragment();
            }
        });
        disposables.wire(CollectFormSubmissionErrorEvent.class, new EventObserver<CollectFormSubmissionErrorEvent>() {
            @Override
            public void onNext(CollectFormSubmissionErrorEvent event) {
                getDraftFormsListFragment().listDraftForms();
                getSubmittedFormsListFragment().listSubmittedForms();
                setPagerToSubmittedFragment();
            }
        });
        disposables.wire(CollectFormSavedEvent.class, new EventObserver<CollectFormSavedEvent>() {
            @Override
            public void onNext(CollectFormSavedEvent event) {
                getDraftFormsListFragment().listDraftForms();
            }
        });
        disposables.wire(CollectFormInstanceDeletedEvent.class, new EventObserver<CollectFormInstanceDeletedEvent>() {
            @Override
            public void onNext(CollectFormInstanceDeletedEvent event) {
                onFormInstanceDeleteSuccess();
            }
        });
        disposables.wire(DeleteFormInstanceEvent.class, new EventObserver<DeleteFormInstanceEvent>() {
            @Override
            public void onNext(DeleteFormInstanceEvent event) {
                showDeleteInstanceDialog(event.getInstanceId(), event.getStatus());
            }
        });
        disposables.wire(CancelPendingFormInstanceEvent.class, new EventObserver<CancelPendingFormInstanceEvent>() {
            @Override
            public void onNext(CancelPendingFormInstanceEvent event) {
                showCancelPendingFormDialog(event.getInstanceId());
            }
        });
        disposables.wire(ReSubmitFormInstanceEvent.class, new EventObserver<ReSubmitFormInstanceEvent>() {
            @Override
            public void onNext(ReSubmitFormInstanceEvent event) {
                reSubmitFormInstance(event.getInstance());
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        countServers();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onStop() {
        if (alertDialog != null && alertDialog.isShowing()) {
            alertDialog.dismiss();
        }
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        if (disposables != null) {
            disposables.dispose();
        }

        stopPresenter();
        stopCreateFormControllerPresenter();
        stopFormReSubmitter();
        hideProgressDialog();

        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.collect_menu, menu);

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
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        CollectMainActivityPermissionsDispatcher.onRequestPermissionsResult(this, requestCode, grantResults);
    }

    @Override
    public void onGetBlankFormDefSuccess(CollectForm collectForm, FormDef formDef) {
        startCreateFormControllerPresenter(collectForm, formDef);
    }

    @Override
    public void onDownloadBlankFormDefSuccess(CollectForm collectForm, FormDef formDef) {
        ((BlankFormsListFragment) adapter.getItem(1)).updateForm(collectForm);
    }

    @Override
    public void onInstanceFormDefSuccess(CollectFormInstance instance) {
        startCreateInstanceFormControllerPresenter(instance);
    }

    @Override
    public void onFormDefError(Throwable error) {
        String errorMessage = FormUtils.getFormDefErrorMessage(this, error);
        showToast(errorMessage);
    }

    @Override
    public void onFormControllerCreated(FormController formController) {
        if (Preferences.isAnonymousMode()) {
            startCollectFormEntryActivity(); // no need to check for permissions, as location won't be turned on
        } else {
            CollectMainActivityPermissionsDispatcher.startCollectFormEntryActivityWithCheck(this);
        }
    }

    @Override
    public void onFormControllerError(Throwable error) {
        Timber.d(error, getClass().getName());
    }

    @Override
    public void onToggleFavoriteSuccess(CollectForm form) {
        getBlankFormsListFragment().listBlankForms();
    }

    @Override
    public void onToggleFavoriteError(Throwable error) {
        Timber.d(error, getClass().getName());
    }

    @Override
    public void onFormInstanceDeleteSuccess() {
        Toast.makeText(this, R.string.ra_form_deleted, Toast.LENGTH_SHORT).show();
        getSubmittedFormsListFragment().listSubmittedForms();
        getDraftFormsListFragment().listDraftForms();
    }

    @Override
    public void onFormInstanceDeleteError(Throwable throwable) {
        Timber.d(throwable, getClass().getName());
    }

    @Override
    public void formReSubmitError(Throwable error) {
        String errorMessage = FormUtils.getFormSubmitErrorMessage(this, error);

        Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();

        // refresh lists..
        getDraftFormsListFragment().listDraftForms();
        getSubmittedFormsListFragment().listSubmittedForms();
    }

    @Override
    public void formReSubmitNoConnectivity() {
        Toast.makeText(this, R.string.ra_form_send_submission_pending, Toast.LENGTH_LONG).show();

        // refresh submitted list
        getSubmittedFormsListFragment().listSubmittedForms();
    }

    @Override
    public void formReSubmitSuccess(CollectFormInstance instance, OpenRosaResponse response) {
        String errorMessage = FormUtils.getFormSubmitSuccessMessage(this, response);

        Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();

        // refresh lists..
        getDraftFormsListFragment().listDraftForms();
        getSubmittedFormsListFragment().listSubmittedForms();
    }

    @Override
    public void showReFormSubmitLoading() {
        progressDialog = DialogsUtil.showProgressDialog(this, getString(R.string.ra_submitting_form));
    }

    @Override
    public void hideReFormSubmitLoading() {
        hideProgressDialog();
    }

    @Override
    public Context getContext() {
        return this;
    }

    @Override
    public void onCountCollectServersEnded(long num) {
        numOfCollectServers = num;

        if (numOfCollectServers < 1) {
            tabLayout.setVisibility(View.GONE);
            formsViewPager.setVisibility(View.GONE);
            fab.setVisibility(View.GONE);
            noServersView.setVisibility(View.VISIBLE);

        } else {
            tabLayout.setVisibility(View.VISIBLE);
            formsViewPager.setVisibility(View.VISIBLE);
            fab.setVisibility(View.VISIBLE);
            noServersView.setVisibility(View.GONE);
        }
    }

    @Override
    public void onCountCollectServersFailed(Throwable throwable) {
        Timber.d(throwable, getClass().getName());
    }

    @NeedsPermission(Manifest.permission.ACCESS_FINE_LOCATION)
    public void startCollectFormEntryActivity() {
        startActivity(new Intent(this, CollectFormEntryActivity.class));
    }

    @OnShowRationale(Manifest.permission.ACCESS_FINE_LOCATION)
    void showFineLocationRationale(final PermissionRequest request) {
        alertDialog = PermissionUtil.showRationale(this, request, getString(R.string.ra_media_location_permissions));
    }

    @OnPermissionDenied(Manifest.permission.ACCESS_FINE_LOCATION)
    void onFineLocationPermissionDenied() {
        startCollectFormEntryActivity();
    }

    @OnNeverAskAgain(Manifest.permission.ACCESS_FINE_LOCATION)
    void onFineLocationNeverAskAgain() {
        startCollectFormEntryActivity();
    }

    @SuppressWarnings("MethodOnlyUsedFromInnerClass")
    private void showFormEntry(CollectForm form) {
        startGetFormDefPresenter(form);
    }

    @SuppressWarnings("MethodOnlyUsedFromInnerClass")
    private void downloadFormEntry(CollectForm form) {
        if (MyApplication.isConnectedToInternet(getContext())) {
            startDownloadFormDefPresenter(form);
        } else {
            showToast(R.string.not_connected_message);
        }
    }

    @SuppressWarnings("MethodOnlyUsedFromInnerClass")
    private void toggleFormFavorite(CollectForm form) {
        presenter.toggleFavorite(form);
    }

    @SuppressWarnings("MethodOnlyUsedFromInnerClass")
    private void showFormInstanceEntry(long instanceId) {
        startGetInstanceFormDefPresenter(instanceId);
    }

    @SuppressWarnings("MethodOnlyUsedFromInnerClass")
    private void showDeleteInstanceDialog(final long instanceId, CollectFormInstanceStatus status) {
        alertDialog = DialogsUtil.showFormInstanceDeleteDialog(
                this,
                status,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        presenter.deleteFormInstance(instanceId);
                    }
                });
    }

    @SuppressWarnings("MethodOnlyUsedFromInnerClass")
    private void showCancelPendingFormDialog(final long instanceId) {
        alertDialog = new AlertDialog.Builder(this)
                .setMessage(R.string.ra_cancel_pending_form_msg)
                .setPositiveButton(R.string.discard, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        presenter.deleteFormInstance(instanceId);
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                })
                .setCancelable(true)
                .show();
    }

    @SuppressWarnings("MethodOnlyUsedFromInnerClass")
    private void reSubmitFormInstance(CollectFormInstance instance) {
        if (formReSubmitter != null) {
            formReSubmitter.reSubmitFormInstance(instance);
        }
    }

    private void countServers() {
        presenter.countCollectServers();
    }

    private void startGetFormDefPresenter(CollectForm form) {
        presenter.getBlankFormDef(form);
    }

    private void startDownloadFormDefPresenter(CollectForm form) {
        presenter.downloadBlankFormDef(form);
    }

    private void startGetInstanceFormDefPresenter(long instanceId) {
        presenter.getInstanceFormDef(instanceId);
    }

    private void stopPresenter() {
        if (presenter != null) {
            presenter.destroy();
            presenter = null;
        }
    }

    private void stopFormReSubmitter() {
        if (formReSubmitter != null) {
            formReSubmitter.destroy();
            formReSubmitter = null;
        }
    }

    private void hideProgressDialog() {
        if (progressDialog != null) {
            progressDialog.dismiss();
            progressDialog = null;
        }
    }

    private void startCreateFormControllerPresenter(CollectForm form, FormDef formDef) {
        stopCreateFormControllerPresenter();
        formControllerPresenter = new CollectCreateFormControllerPresenter(this);
        formControllerPresenter.createFormController(form, formDef);
    }

    private void startCreateInstanceFormControllerPresenter(CollectFormInstance instance) {
        stopCreateFormControllerPresenter();
        formControllerPresenter = new CollectCreateFormControllerPresenter(this);
        formControllerPresenter.createFormController(instance);
    }

    private void stopCreateFormControllerPresenter() {
        if (formControllerPresenter != null) {
            formControllerPresenter.destroy();
            formControllerPresenter = null;
        }
    }

    private void initViewPageAdapter() {
        adapter = new ViewPagerAdapter(getSupportFragmentManager());
        adapter.addFragment(DraftFormsListFragment.newInstance(), getString(R.string.ra_draft));
        adapter.addFragment(BlankFormsListFragment.newInstance(), getString(R.string.ra_blank));
        adapter.addFragment(SubmittedFormsListFragment.newInstance(), getString(R.string.ra_submitted));
    }

    private DraftFormsListFragment getDraftFormsListFragment() {
        return getFormListFragment(FormListFragment.Type.DRAFT);
    }

    private BlankFormsListFragment getBlankFormsListFragment() {
        return getFormListFragment(FormListFragment.Type.BLANK);
    }

    private SubmittedFormsListFragment getSubmittedFormsListFragment() {
        return getFormListFragment(FormListFragment.Type.SUBMITTED);
    }

    private <T> T getFormListFragment(FormListFragment.Type type) {
        for (int i = 0; i < adapter.getCount(); i++) {
            FormListFragment fragment = (FormListFragment) adapter.getItem(i);
            if (fragment.getFormListType() == type) {
                //noinspection unchecked
                return (T) fragment;
            }
        }

        throw new IllegalArgumentException();
    }

    private int getFragmentPosition(FormListFragment.Type type) {
        for (int i = 0; i < adapter.getCount(); i++) {
            FormListFragment fragment = (FormListFragment) adapter.getItem(i);
            if (fragment.getFormListType() == type) {
                return i;
            }
        }

        throw new IllegalArgumentException();
    }

    private void setPagerToSubmittedFragment() {
        mViewPager.setCurrentItem(getFragmentPosition(FormListFragment.Type.SUBMITTED));
    }

    private void startCollectHelp() {
        startActivity(new Intent(CollectMainActivity.this, CollectHelpActivity.class));
    }

}
