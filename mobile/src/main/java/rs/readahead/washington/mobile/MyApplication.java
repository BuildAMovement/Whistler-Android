package rs.readahead.washington.mobile;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.StrictMode;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.crashlytics.android.Crashlytics;
import com.evernote.android.job.JobManager;
import com.google.gson.Gson;
import com.squareup.leakcanary.LeakCanary;

import io.fabric.sdk.android.Fabric;
import rs.readahead.washington.mobile.bus.WhistlerBus;
import rs.readahead.washington.mobile.data.rest.BaseApi;
import rs.readahead.washington.mobile.queue.EvidenceQueue;
import rs.readahead.washington.mobile.sharedpref.SharedPrefs;
import rs.readahead.washington.mobile.util.upload.WhislterJobCreator;
import rs.readahead.washington.mobile.views.activity.LockScreenActivity;
import timber.log.Timber;


public class MyApplication extends Application {
    private static WhistlerBus bus;
    private static EvidenceQueue evidenceQueue;

    @Override
    public void onCreate() {
        super.onCreate();

        if (LeakCanary.isInAnalyzerProcess(this)) {
            return;
        }
        LeakCanary.install(this);

        if (BuildConfig.DEBUG) {
            StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder().detectAll().penaltyLog()/*.penaltyDeath()*/.build()); // todo: catch those..
        }
        // todo: implement dagger2

        SharedPrefs.getInstance().init(this);

        Fabric.with(this, new Crashlytics());

        bus = WhistlerBus.create();

        BaseApi.Builder apiBuilder = new BaseApi.Builder();

        if (BuildConfig.DEBUG) {
            Timber.plant(new Timber.DebugTree());
            apiBuilder.setLogLevelFull();
        }

        apiBuilder.build(getString(R.string.api_base_url));

        // evernote jobs
        JobManager.create(this).addJobCreator(new WhislterJobCreator());
        //JobManager.instance().cancelAll(); // for testing, kill them all for now..

        // queue
        evidenceQueue = EvidenceQueue.create(this, new Gson(), bus());
    }

    @NonNull
    public static WhistlerBus bus() {
        return bus;
    }

    @Nullable
    public static EvidenceQueue evidenceQueue() {
        return evidenceQueue;
    }

    public static boolean isConnectedToInternet(Context context) {
        ConnectivityManager cm = (ConnectivityManager)
                context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = cm.getActiveNetworkInfo();
        return networkInfo != null && networkInfo.isConnected();
    }

    public static void showLockScreen(Context context) {
        Activity activity = (Activity) context;
        Intent intent = new Intent(activity, LockScreenActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
//        intent.putExtra("originalIntent", activity.getComponentName());
        context.startActivity(intent);
    }

    public static String getPath(Context context) {
        return context.getDir("vfs", MODE_PRIVATE).getAbsolutePath();
    }


}
