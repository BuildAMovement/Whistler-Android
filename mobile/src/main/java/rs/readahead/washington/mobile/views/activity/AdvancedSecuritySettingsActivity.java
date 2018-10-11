package rs.readahead.washington.mobile.views.activity;

import android.os.Bundle;
import android.support.v7.widget.SwitchCompat;
import android.support.v7.widget.Toolbar;
import android.widget.CompoundButton;

import butterknife.BindView;
import butterknife.ButterKnife;

import rs.readahead.washington.mobile.R;
import rs.readahead.washington.mobile.data.sharedpref.Preferences;


public class AdvancedSecuritySettingsActivity extends CacheWordSubscriberBaseActivity implements
        CompoundButton.OnCheckedChangeListener {
    @BindView(R.id.enable_df)
    SwitchCompat enableDfSwitch;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_advanced_security_settings);

        ButterKnife.bind(this);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.advanced);
        }

        setupSwitch();
    }

    @Override
    public void onCheckedChanged(CompoundButton v, boolean isChecked) {
        switch (v.getId()) {
            case R.id.enable_df:
                Preferences.setDomainFronting(isChecked);
                enableDfSwitch.setChecked(isChecked);
                break;
        }
    }

    private void setupSwitch() {
        enableDfSwitch.setOnCheckedChangeListener(this);
        enableDfSwitch.setChecked(Preferences.isDomainFronting());
    }
}
