package rs.readahead.washington.mobile.views.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import butterknife.BindView;
import butterknife.ButterKnife;
import info.guardianproject.cacheword.CacheWordHandler;
import info.guardianproject.cacheword.ICacheWordSubscriber;
import rs.readahead.washington.mobile.MyApplication;
import rs.readahead.washington.mobile.R;
import rs.readahead.washington.mobile.database.DataSource;
import rs.readahead.washington.mobile.models.Report;
import rs.readahead.washington.mobile.util.DialogsUtil;
import rs.readahead.washington.mobile.views.fragment.ArchiveListFragment;
import rs.readahead.washington.mobile.views.fragment.ReportPreviewFragment;


public class ArchiveReportsActivity extends AppCompatActivity implements
        ArchiveListFragment.OnSendInteractionListener,
        ArchiveListFragment.OnPreviewInteractionListener,
        ArchiveListFragment.OnDeleteInteractionListener,
        ICacheWordSubscriber {

    @BindView(R.id.toolbar)
    Toolbar mToolbar;

    private Report mReport;
    private Menu mMenu;
    private CacheWordHandler mCacheWord;
    private DataSource dataSource;
    private AlertDialog mRemoveDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_archive_list);
        ButterKnife.bind(this);
        setSupportActionBar(mToolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        mCacheWord = new CacheWordHandler(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        mMenu = menu;
        getMenuInflater().inflate(R.menu.report_preview_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case R.id.action_edit:
                editReport();
                return true;
            case R.id.action_delete:
                showDeleteDialog();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void showOptionsMenu(boolean showMenu) {
        if (mMenu == null)
            return;
        mMenu.setGroupVisible(R.id.report_menu_group, showMenu);
    }

    public void editReport() {
        Intent intent = new Intent(this, NewReportActivity.class);
        intent.putExtra(NewReportActivity.REPORT_KEY, mReport);
        startActivity(intent);
    }

    private void deleteReport() {
        dataSource.deleteReport(mReport);
        Snackbar.make(mToolbar, getString(R.string.removed_archived), Snackbar.LENGTH_SHORT).show();
    }

    private void showDeleteDialog() {
        mRemoveDialog = DialogsUtil.showRemoveReportDialog(mReport.getTitle(), this, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                deleteReport();
                setArchiveListFragment();
                mRemoveDialog.dismiss();
            }
        });
    }

    public void setArchiveListFragment() {
        showOptionsMenu(false);
        mToolbar.setTitle(getString(R.string.archived_reports));

        ArchiveListFragment archiveListFragment = new ArchiveListFragment();
        archiveListFragment.setData(dataSource.getArchivedReports());
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.container, archiveListFragment)
                .commit();
    }

    @Override
    public void onPreviewInteraction(Report report) {
        mReport = report;
        ReportPreviewFragment reportPreviewFragment = new ReportPreviewFragment();
        mToolbar.setTitle(getString(R.string.report_preview));
        reportPreviewFragment.setData(report);
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.container, reportPreviewFragment).commit();
        showOptionsMenu(true);
    }

    @Override
    public void onDeleteFragmentInteraction(Report report) {
        mReport = report;
        deleteReport();
    }

    @Override
    public void onSendInteraction(Report report) {

    }

    @Override
    protected void onResume() {
        super.onResume();
        mCacheWord.connectToService();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mCacheWord.disconnectFromService();
    }

    @Override
    public void onCacheWordUninitialized() {
        MyApplication.showLockScreen(this);
        finish();
    }

    @Override
    public void onCacheWordLocked() {
        MyApplication.showLockScreen(this);
        finish();
    }

    @Override
    public void onCacheWordOpened() {
        dataSource = DataSource.getInstance(mCacheWord, getApplicationContext());

        Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.container);
        if (!(fragment instanceof ReportPreviewFragment)) {
            setArchiveListFragment();
            mToolbar.post(new Runnable() {
                @Override
                public void run() {
                    showOptionsMenu(false);
                }
            });
        }
    }

    @Override
    public void onBackPressed() {
        Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.container);
        if (fragment instanceof ArchiveListFragment) {
            super.onBackPressed();
        } else {
            setArchiveListFragment();
        }
    }
}
