package rs.readahead.washington.mobile.views.activity;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.NavUtils;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;

import butterknife.BindView;
import butterknife.ButterKnife;
import rs.readahead.washington.mobile.R;
import rs.readahead.washington.mobile.views.fragment.SecureMessagingChatFragment;
import rs.readahead.washington.mobile.views.fragment.SecureMessagingContactListFragment;
import timber.log.Timber;


public class SecureMessagingActivity extends AppCompatActivity implements ISecureMessagingView {

    @BindView(R.id.toolbar)
    Toolbar toolbar;

    private Menu menu;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_secure_messaging);
        ButterKnife.bind(this);
        setSupportActionBar(toolbar);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayShowHomeEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        if (savedInstanceState == null) {
            if (getFragmentManager().findFragmentById(R.id.secure_messaging_fragment_container) == null) {
                try {
                    Bundle bundle = getIntent().getExtras();
                    Fragment fragment = new SecureMessagingContactListFragment();
                    fragment.setArguments(bundle);

                    FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
                    transaction.add(R.id.secure_messaging_fragment_container, fragment);
                    transaction.commit();
                } catch (Exception e) {
                    Timber.d(e, SecureMessagingActivity.class.getName());
                }
            }
        }

        getSupportFragmentManager().addOnBackStackChangedListener(new FragmentManager.OnBackStackChangedListener()
        {
            public void onBackStackChanged()
            {
                updateActionBar(getSupportFragmentManager().getBackStackEntryCount());
            }
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
            getSupportFragmentManager().popBackStack();
        } else {
            NavUtils.navigateUpFromSameTask(this);
        }
        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_secure_messaging, menu);
        this.menu = menu;
        return true;
    }

    @Override
    public void showChat() {
        getSupportFragmentManager().beginTransaction().
                //setCustomAnimations(R.anim.enter_from_right, R.anim.exit_to_left, R.anim.enter_from_left, R.anim.exit_to_right).
                replace(R.id.secure_messaging_fragment_container, new SecureMessagingChatFragment()).
                addToBackStack(null).
                commit();
    }

    private void updateActionBar(int backCount) {
        if (backCount > 0) {
            setTitle("Luke Woods");
            menu.findItem(R.id.action_search).setVisible(false).setEnabled(false);
            menu.findItem(R.id.action_add).setVisible(false).setEnabled(false);
        } else {
            setTitle(getString(R.string.secure_messaging));
            menu.findItem(R.id.action_search).setVisible(true).setEnabled(true);
            menu.findItem(R.id.action_add).setVisible(true).setEnabled(true);
        }
    }
}
