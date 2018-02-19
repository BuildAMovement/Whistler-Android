package rs.readahead.washington.mobile.views.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;
import android.view.View;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import rs.readahead.washington.mobile.R;


public class ReportSettingsActivity extends CacheWordSubscriberBaseActivity {
    @BindView(R.id.toolbar)
    Toolbar toolbar;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report_settings);
        ButterKnife.bind(this);
        setSupportActionBar(toolbar);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setTitle(R.string.ra_report);
        }
    }

    @OnClick({R.id.recipients_settings, R.id.contact_settings})
    public void startActivity(View view) {
        switch (view.getId()) {
            case R.id.recipients_settings:
                startActivity(new Intent(this, MediaRecipients2Activity.class));
                break;
            case R.id.contact_settings:
                startActivity(new Intent(this, ContactSettingsActivity.class));
                break;
        }
    }
}
