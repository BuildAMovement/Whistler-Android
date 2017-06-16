package rs.readahead.washington.mobile.views.activity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import butterknife.BindView;
import butterknife.ButterKnife;
import info.guardianproject.cacheword.CacheWordHandler;
import info.guardianproject.cacheword.ICacheWordSubscriber;
import rs.readahead.washington.mobile.MyApplication;
import rs.readahead.washington.mobile.R;
import rs.readahead.washington.mobile.database.DataSource;
import rs.readahead.washington.mobile.models.Report;
import rs.readahead.washington.mobile.views.fragment.DraftsListFragment;
import rs.readahead.washington.mobile.views.fragment.ReportPreviewFragment;


public class DraftReportsActivity extends AppCompatActivity implements
        DraftsListFragment.OnListFragmentInteractionListener,
        DraftsListFragment.OnEditInteractionListener,
        DraftsListFragment.OnDeleteInteractionListener,
        ICacheWordSubscriber {
    @BindView(R.id.toolbar)
    Toolbar mToolbar;

    private Report selectedReport;
    private Menu mMenu;
    private CacheWordHandler mCacheWord;
    private DataSource dataSource;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_draft_reports);
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
                editSelectedReport();
                return true;
            case R.id.action_delete:
                showDraftDeleteDialog();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void showOptionsMenu(boolean showMenu) {
        if (mMenu == null) return;

        mMenu.setGroupVisible(R.id.report_menu_group, showMenu);
    }

    @Override
    public void onListFragmentInteraction(Report report) {
        selectedReport = report;

        mToolbar.setTitle(getString(R.string.report_preview));

        ReportPreviewFragment reportPreviewFragment = new ReportPreviewFragment();
        reportPreviewFragment.setData(report);
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.container, reportPreviewFragment).commit();
        showOptionsMenu(true);
    }

    private void editSelectedReport() {
        Intent intent = new Intent(this, NewReportActivity.class);
        intent.putExtra(NewReportActivity.REPORT_KEY, selectedReport);
        startActivity(intent);
    }

    public void setDraftListFragment() {
        mToolbar.setTitle(getString(R.string.draft_reports));
        showOptionsMenu(false);

        DraftsListFragment draftsListFragment = new DraftsListFragment();
        draftsListFragment.setData(dataSource.getDraftReports());
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.container, draftsListFragment)
                .commit();
    }

    @Override
    public void onEditFragmentInteraction(Report report) {
        selectedReport = report;
        editSelectedReport();
    }

    @Override
    public void onDeleteFragmentInteraction(Report report) {
        selectedReport = report;
        deleteSelectedReport();
    }

    private void showDraftDeleteDialog() {
        LayoutInflater inflater = getLayoutInflater();
        @SuppressLint("InflateParams")
        View dialogView = inflater.inflate(R.layout.delete_draft_dialog, null);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(dialogView);

        final AlertDialog alertDialog = builder.create();
        alertDialog.setCancelable(true);
        alertDialog.show();

        final TextView mTitle = (TextView) dialogView.findViewById(R.id.dialog_draft_title);
        final Button delete = (Button) dialogView.findViewById(R.id.dialog_draft_delete);
        final TextView cancel = (Button) dialogView.findViewById(R.id.dialog_draft_cancel);

        String titleText = "'" + selectedReport.getTitle() + "'";
        mTitle.setText(titleText);
        delete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                deleteSelectedReport();
                setDraftListFragment();
                alertDialog.dismiss();
            }
        });
        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                alertDialog.dismiss();
            }
        });
    }

    private void deleteSelectedReport() {
        dataSource.deleteReport(selectedReport);
        Snackbar.make(mToolbar, getString(R.string.removed_draft), Snackbar.LENGTH_SHORT).show();
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
        if (! (fragment instanceof ReportPreviewFragment)) {
            setDraftListFragment();
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
        if (fragment instanceof DraftsListFragment) {
            super.onBackPressed();
        } else {
            setDraftListFragment();
        }
    }
}
