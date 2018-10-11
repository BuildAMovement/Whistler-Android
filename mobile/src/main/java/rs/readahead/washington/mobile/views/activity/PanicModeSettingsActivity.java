package rs.readahead.washington.mobile.views.activity;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.SwitchCompat;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.LinearLayout;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import info.guardianproject.cacheword.CacheWordHandler;
import info.guardianproject.cacheword.ICacheWordSubscriber;
import rs.readahead.washington.mobile.MyApplication;
import rs.readahead.washington.mobile.R;
import rs.readahead.washington.mobile.data.sharedpref.Preferences;
import rs.readahead.washington.mobile.views.fragment.CircleOfTrustFragment;
import rs.readahead.washington.mobile.views.fragment.EraseSensitiveDataFragment;
import rs.readahead.washington.mobile.views.fragment.PanicMessageFragment;


public class PanicModeSettingsActivity extends BaseActivity implements
        ICacheWordSubscriber,
        CompoundButton.OnCheckedChangeListener {
    @BindView(R.id.panic_message)
    LinearLayout mPanicMessage;
    @BindView(R.id.circle_of_trust)
    LinearLayout mCircleOfTrust;
    @BindView(R.id.sensitive_data)
    LinearLayout mSensitiveData;
    @BindView(R.id.toolbar)
    Toolbar toolbar;
    @BindView(R.id.uninstall_switch)
    SwitchCompat uninstallSwitch;

    private CacheWordHandler mCacheWord;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_panic_mode);
        ButterKnife.bind(this);
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.title_activity_panic_mode);
        }

        mCacheWord = new CacheWordHandler(this);

        setupSwitches();
    }

    @OnClick({R.id.panic_message, R.id.circle_of_trust, R.id.sensitive_data})
    public void showMail(View view) {
        switch (view.getId()) {
            case R.id.panic_message:
                addFragment(new PanicMessageFragment(), getString(R.string.panic_message));
                toolbar.setNavigationIcon(R.drawable.ic_close_white_24dp);
                break;
            case R.id.circle_of_trust:
                CircleOfTrustFragment fragment = new CircleOfTrustFragment();
                fragment.setWordHandler(mCacheWord);
                addFragment(fragment, getString(R.string.circle_of_trust));
                break;
            case R.id.sensitive_data:
                addFragment(new EraseSensitiveDataFragment(), getString(R.string.erasing_data));
                break;
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onCheckedChanged(CompoundButton v, boolean isChecked) {
        switch (v.getId()) {
            case R.id.uninstall_switch:
                Preferences.setUninstallOnPanic(isChecked);
                uninstallSwitch.setChecked(isChecked);
                break;
        }
    }

    private void addFragment(Fragment fragment, String title) {
        toolbar.setTitle(title);
        getSupportFragmentManager().beginTransaction()
                .setCustomAnimations(R.anim.slide_in_up, R.anim.slide_out_up)
                .add(R.id.container, fragment)
                .addToBackStack(null)
                .commit();
    }

    @Override
    public void onBackPressed() {
        toolbar.setNavigationIcon(R.drawable.ic_arrow_back_white_24dp);
        if (!toolbar.getTitle().equals(getString(R.string.title_activity_panic_mode))) {
            toolbar.setTitle(R.string.title_activity_panic_mode);
            Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.container);
            getSupportFragmentManager().beginTransaction()
                    .setCustomAnimations(R.anim.slide_in_up, R.anim.slide_out_up)
                    .remove(fragment).commit();
        }

        super.onBackPressed();
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
        MyApplication.startLockScreenActivity(this);
        finish();
    }

    @Override
    public void onCacheWordLocked() {
        MyApplication.startLockScreenActivity(this);
        finish();
    }

    @Override
    public void onCacheWordOpened() {
    }

    private void setupSwitches() {
        uninstallSwitch.setOnCheckedChangeListener(this);
        uninstallSwitch.setChecked(Preferences.isUninstallOnPanic());
    }
}
