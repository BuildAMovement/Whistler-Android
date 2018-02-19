package rs.readahead.washington.mobile.views.activity;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import info.guardianproject.cacheword.ICacheWordSubscriber;
import rs.readahead.washington.mobile.R;
import rs.readahead.washington.mobile.domain.entity.Report;
import rs.readahead.washington.mobile.mvp.contract.IReportListPresenterContract;
import rs.readahead.washington.mobile.mvp.presenter.ReportListPresenter;
import rs.readahead.washington.mobile.presentation.entity.ReportViewType;
import rs.readahead.washington.mobile.util.DialogsUtil;
import rs.readahead.washington.mobile.views.adapters.ArchiveRecyclerViewAdapter;
import rs.readahead.washington.mobile.views.adapters.DraftsRecyclerViewAdapter;
import rs.readahead.washington.mobile.views.interfaces.IOnReportInteractionListener;


public class ReportListActivity extends CacheWordSubscriberBaseActivity implements
        ICacheWordSubscriber,
        IReportListPresenterContract.IReportListView,
        IOnReportInteractionListener {

    public static final String DRAFT_ID_KEY = "dik";
    public static final String REPORT_VIEW_TYPE = "type";

    @BindView(R.id.toolbar)
    Toolbar toolbar;
    @BindView(R.id.report_list)
    RecyclerView mReportList;
    @BindView(R.id.empty_list_archived)
    TextView mEmptyListTextView;

    private ReportListPresenter presenter;
    private boolean isListOfDraft = false;
    private AlertDialog mRemoveDialog;
    private ArchiveRecyclerViewAdapter mSentAdapter;
    private DraftsRecyclerViewAdapter mDraftAdapter;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_archive);
        ButterKnife.bind(this);

        isListOfDraft = getIntent().hasExtra(DRAFT_ID_KEY);

        presenter = new ReportListPresenter(this);

        setActivityTitle();
        setToolbar();
    }

    @Override
    protected void onResume() {
        super.onResume();
        listReports();
    }

    @Override
    protected void onDestroy() {
        presenter.destroy();

        if (mRemoveDialog != null && mRemoveDialog.isShowing()) {
            mRemoveDialog.dismiss();
        }

        super.onDestroy();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == android.R.id.home) {
            onBackPressed();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void setToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    private void listReports() {
        if (isListOfDraft) {
            presenter.listDraftReports();
        } else {
            presenter.listArchivedReports();
        }
    }

    private void setActivityTitle() {
        toolbar.setTitle(getString(isListOfDraft ? R.string.draft_reports : R.string.sent_reports));
    }

    @Override
    public void onReportList(List<Report> reportList) {
        setListViews(reportList.isEmpty());
        if (reportList.isEmpty()) return;
        if (isListOfDraft) {
            setDraftList(reportList);
        } else {
            setSentList(reportList);
        }
    }

    private void setDraftList(List<Report> reportList) {
        mDraftAdapter = new DraftsRecyclerViewAdapter(reportList, this);
        mReportList.setAdapter(mDraftAdapter);
    }

    private void setSentList(List<Report> reportList) {
        mSentAdapter = new ArchiveRecyclerViewAdapter(reportList, this);
        mReportList.setAdapter(mSentAdapter);
    }

    private void setListViews(boolean isEmpty) {
        mEmptyListTextView.setText(getString(isListOfDraft ? R.string.empty_list_drafts : R.string.sent_reports_empty));
        mEmptyListTextView.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        mReportList.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
    }

    private void showDeleteDialog(final Report report, final int position) {
        mRemoveDialog = DialogsUtil.showMessageOKCancelWithTitle(this,
                !TextUtils.isEmpty(report.getTitle()) ? "'" + report.getTitle() + "'" : getString(R.string.title_not_included),
                getString(isListOfDraft ? R.string.delete_draft : R.string.delete_sent_report),
                getString(R.string.delete),
                getString(R.string.cancel),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (presenter != null) {
                            presenter.deleteReport(report.getId(), position);
                        }
                        dialog.dismiss();
                    }
                },
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
    }

    @Override
    public void onReportListError(Throwable throwable) {
    }

    @Override
    public void onDeleteReportSuccess(int position) {
        removeReport(position);
    }

    private void removeReport(int position) {
        boolean isEmpty;

        if (isListOfDraft) {
            isEmpty = mDraftAdapter.removeDraft(position);
        } else {
            isEmpty = mSentAdapter.removeReport(position);
        }

        setListViews(isEmpty);
    }

    @Override
    public void onDeleteReportError(Throwable throwable) {
        // todo: inform user..
    }

    @Override
    public Context getContext() {
        return this;
    }

    @Override
    public void onSendReport(Report report) {
        onEditReport(report.getId());
    }

    @Override
    public void onPreviewReport(long id) {
        startActivity(new Intent(this, NewReportActivity.class)
                .putExtra(REPORT_VIEW_TYPE, ReportViewType.PREVIEW)
                .putExtra(NewReportActivity.REPORT_ID_KEY, id));
    }

    @Override
    public void onDeleteReport(Report report, int position) {
        showDeleteDialog(report, position);
    }

    @Override
    public void onEditReport(long id) {
        startActivity(new Intent(this, NewReportActivity.class)
                .putExtra(REPORT_VIEW_TYPE, ReportViewType.EDIT)
                .putExtra(NewReportActivity.REPORT_ID_KEY, id));
    }
}
