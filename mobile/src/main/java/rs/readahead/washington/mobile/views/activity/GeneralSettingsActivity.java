package rs.readahead.washington.mobile.views.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.SwitchCompat;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.CompoundButton;

import java.util.concurrent.Callable;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.reactivex.Completable;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.functions.Action;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;
import rs.readahead.washington.mobile.MyApplication;
import rs.readahead.washington.mobile.R;
import rs.readahead.washington.mobile.bus.EventCompositeDisposable;
import rs.readahead.washington.mobile.bus.EventObserver;
import rs.readahead.washington.mobile.bus.event.LocaleChangedEvent;
import rs.readahead.washington.mobile.data.sharedpref.Preferences;
import rs.readahead.washington.mobile.data.sharedpref.SharedPrefs;
import rs.readahead.washington.mobile.util.jobs.UploadJob;


public class GeneralSettingsActivity extends CacheWordSubscriberBaseActivity {
    @BindView(R.id.toolbar)
    Toolbar toolbar;
    @BindView(R.id.anonymous_switch)
    SwitchCompat anonymousSwitch;
    @BindView(R.id.crash_report_switch)
    SwitchCompat crashReportSwitch;
    @BindView(R.id.only_wifi_switch)
    SwitchCompat onlyWiFiSwitch;

    private EventCompositeDisposable disposables;
    private CompositeDisposable disposable;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_general_settings);
        ButterKnife.bind(this);
        setSupportActionBar(toolbar);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setTitle(R.string.ra_general);
        }

        disposable = new CompositeDisposable();

        setupWifiSwitch();
        setupAnonymousSwitch();
        setupCrashReportsSwitch();

        disposables = MyApplication.bus().createCompositeDisposable();
        disposables.wire(LocaleChangedEvent.class, new EventObserver<LocaleChangedEvent>() {
            @Override
            public void onNext(LocaleChangedEvent event) {
                recreate();
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @OnClick(R.id.language_settings)
    public void startActivity(View view) {
        startActivity(new Intent(this, LanguageSettingsActivity.class));
    }

    @Override
    public void onDestroy() {
        if (disposables != null) {
            disposables.dispose();
        }

        if (disposable != null) {
            disposable.dispose();
        }

        super.onDestroy();
    }

    private void setupAnonymousSwitch() {
        anonymousSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, final boolean isChecked) {
                Preferences.setAnonymousMode(!isChecked);
            }
        });

        anonymousSwitch.setChecked(!Preferences.isAnonymousMode());
    }

    private void setupCrashReportsSwitch() {
        crashReportSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, final boolean isChecked) {
                Preferences.setSubmitingCrashReports(isChecked);
            }
        });

        crashReportSwitch.setChecked(Preferences.isSubmitingCrashReports());
    }

    private void setupWifiSwitch() {
        disposable.add(Single.fromCallable(new Callable<Boolean>() {
                    @Override
                    public Boolean call() throws Exception {
                        return SharedPrefs.getInstance().isWiFiAttachments();
                    }
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<Boolean>() {
                    @Override
                    public void accept(Boolean aBoolean) throws Exception {
                        onlyWiFiSwitch.setChecked(aBoolean);
                        addWiFiSwitchListener();
                    }
                })
        );
    }

    private void addWiFiSwitchListener() {
        onlyWiFiSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, final boolean isChecked) {
                disposable.add(Completable.fromCallable(new Callable<Boolean>() {
                            @Override
                            public Boolean call() throws Exception {
                                SharedPrefs.getInstance().setWifiAttachments(isChecked);
                                return isChecked;
                            }
                        })
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(new Action() {
                            @Override
                            public void run() throws Exception {
                                UploadJob.updateNetworkType(isChecked);
                            }
                        })
                );
            }
        });
    }
}
