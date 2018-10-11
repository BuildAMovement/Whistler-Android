package rs.readahead.washington.mobile.views.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import rs.readahead.washington.mobile.R;
import rs.readahead.washington.mobile.presentation.entity.ReportViewType;


public class ReportActivity extends CacheWordSubscriberBaseActivity {
    public static final String REPORT_VIEW_TYPE = "type";

    @BindView(R.id.toolbar)
    Toolbar toolbar;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report);
        ButterKnife.bind(this);

        setToolbar();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.report_menu, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == android.R.id.home) {
            onBackPressed();
            return true;
        }

        if (id == R.id.help_item) {
            startReportHelp();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void setToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() == null) return;
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle(R.string.title_activity_report);
    }

    @OnClick(R.id.new_report)
    void onNewReportClicked() {
        startActivity(new Intent(ReportActivity.this, NewReportActivity.class).putExtra(REPORT_VIEW_TYPE, ReportViewType.NEW));
    }

    @OnClick(R.id.drafts_reports)
    void onDraftClicked() {
        startActivity(new Intent(ReportActivity.this, ReportListActivity.class)
                .putExtra(ReportListActivity.DRAFT_ID_KEY, true));
    }

    @OnClick(R.id.sent_reports)
    void onSentClicked() {
        startActivity(new Intent(ReportActivity.this, ReportListActivity.class));
    }

    private void startReportHelp() {
        startActivity(new Intent(ReportActivity.this, ReportHelpActivity.class));
    }

}
