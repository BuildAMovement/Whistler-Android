package rs.readahead.washington.mobile.views.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import rs.readahead.washington.mobile.R;


public class SettingsActivity extends AppCompatActivity {
    @BindView(R.id.toolbar)
    Toolbar toolbar;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        ButterKnife.bind(this);
        setSupportActionBar(toolbar);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayShowHomeEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    @OnClick({R.id.make_oppression_backfire_layout, R.id.panic_mode_layout, R.id.activation_settings_layout, R.id.about_n_help_layout})
    public void startActivity(View view) {
        switch (view.getId()) {
            case R.id.make_oppression_backfire_layout:
                startActivity(new Intent(this, MediaRecipients2Activity.class));
                break;
            case R.id.panic_mode_layout:
                startActivity(new Intent(this, PanicModeActivity.class));
                break;
            case R.id.activation_settings_layout:
                startActivity(new Intent(this, ActivationSettingsActivity.class));
                break;
            case R.id.about_n_help_layout:
                startActivity(new Intent(this, AboutHelpActivity.class));
                break;
        }
    }
}
