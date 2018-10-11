package rs.readahead.washington.mobile;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.StrictMode;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.crashlytics.android.Crashlytics;
import com.crashlytics.android.core.CrashlyticsCore;
import com.evernote.android.job.JobManager;
import com.google.gson.Gson;
import com.squareup.leakcanary.LeakCanary;

import java.lang.ref.WeakReference;

import info.guardianproject.cacheword.CacheWordHandler;
import info.guardianproject.cacheword.ICacheWordSubscriber;
import info.guardianproject.cacheword.ICachedSecrets;
import io.fabric.sdk.android.Fabric;
import io.reactivex.functions.Consumer;
import io.reactivex.plugins.RxJavaPlugins;
import rs.readahead.washington.mobile.bus.WhistlerBus;
import rs.readahead.washington.mobile.data.rest.BaseApi;
import rs.readahead.washington.mobile.data.sharedpref.Preferences;
import rs.readahead.washington.mobile.data.sharedpref.SharedPrefs;
import rs.readahead.washington.mobile.domain.entity.KeyBundle;
import rs.readahead.washington.mobile.javarosa.JavaRosa;
import rs.readahead.washington.mobile.javarosa.PropertyManager;
import rs.readahead.washington.mobile.media.MediaFileHandler;
import rs.readahead.washington.mobile.queue.EvidenceQueue;
import rs.readahead.washington.mobile.queue.RawMediaFileQueue;
import rs.readahead.washington.mobile.util.LocaleManager;
import rs.readahead.washington.mobile.util.TrainModuleHandler;
import rs.readahead.washington.mobile.util.jobs.EvidenceUploadJob;
import rs.readahead.washington.mobile.util.jobs.MediaFileUploadJob;
import rs.readahead.washington.mobile.util.jobs.PendingFormSendJob;
import rs.readahead.washington.mobile.util.jobs.TrainModuleDownloadJob;
import rs.readahead.washington.mobile.util.jobs.WhistlerJobCreator;
import rs.readahead.washington.mobile.views.activity.ExitActivity;
import rs.readahead.washington.mobile.views.activity.LockScreenActivity;
import rs.readahead.washington.mobile.views.activity.MainActivity;
import rs.readahead.washington.mobile.views.activity.WhistlerIntroActivity;
import timber.log.Timber;


public class MyApplication extends /*MultiDexApplication*/ Application implements ICacheWordSubscriber {
    private static WhistlerBus bus;
    private static EvidenceQueue evidenceQueue;
    private static RawMediaFileQueue mediaFileQueue;

    private CacheWordHandler cacheWordHandler = null;
    private boolean detached = false;

    private static final Object keyLock = new Object();
    private static WeakReference<ICachedSecrets> cachedSecretsReference;
    private static long version = 0;


    @Override
    protected void attachBaseContext(Context newBase) {
        SharedPrefs.getInstance().init(newBase);
        super.attachBaseContext(LocaleManager.getInstance().getLocalizedContext(newBase));
    }

    @Override
    public void onCreate() {
        super.onCreate();

        if (LeakCanary.isInAnalyzerProcess(this)) {
            return;
        }
        LeakCanary.install(this);

        if (BuildConfig.DEBUG) {
            StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
                    .detectAll().penaltyLog()/*.penaltyDeath()*/.build()); // todo: catch those..
        }
        // todo: implement dagger2

        SharedPrefs.getInstance().init(this);

        configureCrashlytics();

        RxJavaPlugins.setErrorHandler(new Consumer<Throwable>() {
            @Override
            public void accept(Throwable throwable) throws Exception {
                Timber.d(throwable, getClass().getName());
                Crashlytics.logException(throwable);
            }
        });

        bus = WhistlerBus.create();

        BaseApi.Builder apiBuilder = new BaseApi.Builder();

        if (BuildConfig.DEBUG) {
            Timber.plant(new Timber.DebugTree());
            apiBuilder.setLogLevelFull();
        }

        apiBuilder.build(getString(R.string.api_base_url));

        // MediaFile init
        MediaFileHandler.init(this);
        MediaFileHandler.emptyTmp(this);

        // Train modules init
        TrainModuleHandler.init(this);

        // evernote jobs
        JobManager.create(this).addJobCreator(new WhistlerJobCreator());
        //JobManager.instance().cancelAll(); // for testing, kill them all for now..

        // queues
        evidenceQueue = EvidenceQueue.create(this, new Gson());
        mediaFileQueue = RawMediaFileQueue.create(this, new Gson(), bus());

        // Collect
        PropertyManager mgr = new PropertyManager();
        JavaRosa.initializeJavaRosa(mgr);
    }

    public static void startMainActivity(@NonNull Context context) {
        Intent intent;

        if (Preferences.isFirstStart()) {
            intent = new Intent(context, WhistlerIntroActivity.class);
        } else {
            intent = new Intent(context, MainActivity.class);
        }

        //intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        maybeExcludeIntentFromRecents(intent);
        context.startActivity(intent);
    }

    public static void startLockScreenActivity(@NonNull Context context) {
        Intent intent = new Intent(context, LockScreenActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        context.startActivity(intent);
    }

    @NonNull
    public static WhistlerBus bus() {
        return bus;
    }

    @Nullable
    public static EvidenceQueue evidenceQueue() {
        return evidenceQueue;
    }

    @Nullable
    public static RawMediaFileQueue mediaFileQueue() {
        return mediaFileQueue;
    }

    public synchronized void createCacheWordHandler() {
        if (cacheWordHandler == null) {
            detached = false;
            cacheWordHandler = new CacheWordHandler(getApplicationContext(), this);
            cacheWordHandler.connectToService();
        }
    }

    @Override
    public void onCacheWordUninitialized() {
        detachCacheWordHandler();
        synchronized (keyLock) {
            cachedSecretsReference = null;
            version++;
        }
    }

    @Override
    public void onCacheWordLocked() {
        detachCacheWordHandler();
        synchronized (keyLock) {
            cachedSecretsReference = null;
            version++;
        }
    }

    @Override
    public void onCacheWordOpened() {
        detachCacheWordHandler();
        synchronized (keyLock) {
            cachedSecretsReference = new WeakReference<>(cacheWordHandler.getCachedSecrets());
            version++;
        }

        // fire up jobs that need CacheWord secret - they will quit if nothing to do..
        PendingFormSendJob.scheduleJob();
        TrainModuleDownloadJob.scheduleJob();
        EvidenceUploadJob.scheduleJob();
        MediaFileUploadJob.scheduleJob();
    }

    @Nullable
    public static KeyBundle getKeyBundle() {
        synchronized (keyLock) {
            if (cachedSecretsReference == null) {
                return null;
            }

            ICachedSecrets cachedSecrets = cachedSecretsReference.get();
            if (cachedSecrets == null) {
                return null;
            }

            return new KeyBundle(cachedSecrets, version);
        }
    }

    public static boolean isKeyBundleValid(@NonNull final KeyBundle other) {
        synchronized (keyLock) {
            return other.getKey() != null && version == other.getVersion();
        }
    }

    public static boolean isConnectedToInternet(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return false;
        NetworkInfo networkInfo = cm.getActiveNetworkInfo();
        return networkInfo != null && networkInfo.isConnected();
    }

    public static void exit(Context context) {
        Intent intent = new Intent(context, ExitActivity.class);

        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
                Intent.FLAG_ACTIVITY_CLEAR_TASK |
                Intent.FLAG_ACTIVITY_NO_ANIMATION |
                Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);

        context.startActivity(intent);
    }

    private synchronized void detachCacheWordHandler() {
        if (!detached && cacheWordHandler != null) {
            cacheWordHandler.detach();
            detached = true;
        }
    }

    private static void maybeExcludeIntentFromRecents(Intent intent) {
        if (Preferences.isSecretModeActive()) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
                    Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS/* |
                    Intent.FLAG_ACTIVITY_MULTIPLE_TASK*/);
        }
    }

    private void configureCrashlytics(){
        CrashlyticsCore core = new CrashlyticsCore.Builder().disabled(Preferences.isSubmitingCrashReports()).build();
        Fabric.with(this, new Crashlytics.Builder().core(core).build());
    }
}
