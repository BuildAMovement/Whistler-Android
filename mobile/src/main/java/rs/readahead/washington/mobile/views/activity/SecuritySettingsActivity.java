package rs.readahead.washington.mobile.views.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.SwitchCompat;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.CompoundButton;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

import rs.readahead.washington.mobile.R;
import rs.readahead.washington.mobile.data.sharedpref.Preferences;


public class SecuritySettingsActivity extends CacheWordSubscriberBaseActivity {
    @BindView(R.id.show_camera_preview)
    SwitchCompat cameraPreviewSwitch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_security_settings);

        ButterKnife.bind(this);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        setupCameraPreviewSwitch();

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.ra_security_settings);
        }
    }

    @OnClick({R.id.camouflage_settings, R.id.panic_mode_layout/*, R.id.advanced_security_settings*/})
    public void manage(View view) {
        switch (view.getId()) {
            case R.id.camouflage_settings:
                startActivity(new Intent(this, CamouflageSettingsActivity.class));
                break;
            case R.id.panic_mode_layout:
                startActivity(new Intent(this, PanicModeSettingsActivity.class));
                break;
           /* case R.id.advanced_security_settings:
                startActivity(new Intent(this, AdvancedSecuritySettingsActivity.class));
                break; */
        }
    }

    private void setupCameraPreviewSwitch() {
        cameraPreviewSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, final boolean isChecked) {
                Preferences.setCameraPreviewEnabled(isChecked);
            }
        });

        cameraPreviewSwitch.setChecked(Preferences.isCameraPreviewEnabled());
    }
}
