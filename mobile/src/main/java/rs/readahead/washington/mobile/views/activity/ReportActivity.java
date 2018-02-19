package rs.readahead.washington.mobile.views.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.widget.TextView;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import rs.readahead.washington.mobile.R;
import rs.readahead.washington.mobile.presentation.entity.ReportViewType;
import rs.readahead.washington.mobile.util.StringUtils;


public class ReportActivity extends CacheWordSubscriberBaseActivity {
    public static final String REPORT_VIEW_TYPE = "type";

    @BindView(R.id.toolbar) Toolbar toolbar;
    @BindView(R.id.data_text) TextView mSaveDataTextView;
    @BindView(R.id.meta_data_text) TextView mIncludeMetaDataTextView;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report);
        ButterKnife.bind(this);

        setToolbar();
        setDataText();
    }

    private void setToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() == null) return;
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    private void setDataText() {
        mIncludeMetaDataTextView.setText(Html.fromHtml(getString(R.string.include_geolocation_automatically)));
        mIncludeMetaDataTextView.setMovementMethod(LinkMovementMethod.getInstance());
        StringUtils.stripUnderlines(mIncludeMetaDataTextView);
        mSaveDataTextView.setText(Html.fromHtml(getString(R.string.save_data)));
        mSaveDataTextView.setMovementMethod(LinkMovementMethod.getInstance());
        StringUtils.stripUnderlines(mSaveDataTextView);
    }

    @OnClick(R.id.new_report)
    void onNewReportClicked(){
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
}
