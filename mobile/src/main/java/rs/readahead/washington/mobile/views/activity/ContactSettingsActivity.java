package rs.readahead.washington.mobile.views.activity;

import android.content.Context;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import rs.readahead.washington.mobile.R;
import rs.readahead.washington.mobile.domain.entity.ContactSetting;
import rs.readahead.washington.mobile.domain.entity.ContactSettingMethod;
import rs.readahead.washington.mobile.mvp.contract.IContactSettingsPresenterContract;
import rs.readahead.washington.mobile.mvp.presenter.ContactSettingPresenter;
import rs.readahead.washington.mobile.views.dialog.CollectServerDialogFragment;
import rs.readahead.washington.mobile.views.dialog.ContactMethodDialogFragment;


public class ContactSettingsActivity extends CacheWordSubscriberBaseActivity implements
        IContactSettingsPresenterContract.IView,
        ContactMethodDialogFragment.ContactSettingMethodChangeHandler {
    @BindView(R.id.toolbar)
    Toolbar toolbar;
    @BindView(R.id.preferred_method)
    RelativeLayout preferredMethod;
    @BindView(R.id.contact_method)
    TextView contactMethod;
    @BindView(R.id.contact_info_layout)
    LinearLayout contactInfoLayout;
    @BindView(R.id.contact_info)
    EditText contactInfo;

    private ContactSettingPresenter presenter;
    private ContactSetting contactSetting; // yes, yes..


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contact_settings);
        ButterKnife.bind(this);

        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationIcon(R.drawable.ic_close_white_24dp);
        setSupportActionBar(toolbar);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setTitle(R.string.ra_contact_settings);
        }

        presenter = new ContactSettingPresenter(this);
        presenter.loadContactSettings();
    }

    @Override
    protected void onDestroy() {
        stopPresenter();
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.contact_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == android.R.id.home) {
            onBackPressed();
            return true;
        }

        if (id == R.id.action_select) {
            saveContactSettings();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @OnClick(R.id.preferred_method)
    public void showContactSettingDialog() {
        if (contactSetting != null) {
            ContactMethodDialogFragment.newInstance(contactSetting.getMethod())
                    .show(getSupportFragmentManager(), CollectServerDialogFragment.TAG);
        }
    }

    @Override
    public void onSavedContactSettings() {
        finish();
    }

    @Override
    public void onSaveContactSettingsError(Throwable throwable) {
    }

    @Override
    public void onLoadedContactSettings(ContactSetting setting) {
        contactSetting = setting;
        updateMethod(contactSetting.getMethod());
        updateAddress(contactSetting.getAddress());
    }

    @Override
    public void onLoadContactSettingsError(Throwable throwable) {
    }

    @Override
    public Context getContext() {
        return this;
    }

    @Override
    public void onContactMethodChanged(int contactSettingMethodId) {
        contactSetting.setMethod(ContactSettingMethod.getMethod(contactSettingMethodId));
        updateMethod(contactSetting.getMethod());
    }

    private void updateMethod(ContactSettingMethod method) {
        contactMethod.setText(method.getNameResId());
        contactInfo.setHint(method.getHintResId());
    }

    private void updateAddress(String address) {
        contactInfo.setText(address);
    }

    private void saveContactSettings() {
        contactSetting.setAddress(contactInfo.getText().toString());
        presenter.updateContactSettings(contactSetting);
    }

    private void stopPresenter() {
        if (presenter != null) {
            presenter.destroy();
        }
    }
}
