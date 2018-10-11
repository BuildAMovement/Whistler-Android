package rs.readahead.washington.mobile.data.sharedpref;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;

import java.util.concurrent.Callable;

import io.reactivex.Completable;
import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;


public class SharedPrefs {
    public static final String NONE = "";

    private static final String SHARED_PREFS_NAME = "washington_shared_prefs";

    static final String SECRET_PASSWORD = "secret_password";
    //private static final String TOR_MODE_ENABLED = "tor_password_enabled";
    //private static final String ASK_FOR_TOR = "ask_for_tor";
    private static final String PANIC_PASSWORD = "panic_password";
    private static final String PANIC_MESSAGE = "panic_message";
    static final String PANIC_GEOLOCATION = "panic_geolocation";
    static final String ERASE_EVERYTHING = "erase_everything";
    private static final String ERASE_GALLERY = "erase_gallery";
    private static final String ERASE_CONTACTS = "erase_contacts";
    private static final String ERASE_MEDIA_RECIPIENTS = "erase_media";
    private static final String ERASE_MATERIALS = "erase_materials";
    static final String ERASE_REPORTS = "erase_reports";
    static final String ERASE_FORMS = "erase_forms";
    //private static final String AUTO_SAVE_DRAFT_FORM = "auto_save_draft_form";
    private static final String LANGUAGE = "language";
    private static final String WIFI_ATTACHMENTS = "wifi_attachments";
    static final String SECRET_MODE_ENABLED = "secret_password_enabled";
    static final String DOMAIN_FRONTING = "df";
    static final String ANONYMOUS_MODE = "anonymous_mode";
    static final String UNINSTALL_ON_PANIC = "uninstall_on_panic";
    static final String APP_FIRST_START = "app_first_start";
    static final String APP_ALIAS_NAME = "app_alias_name";
    static final String SUBMIT_CRASH_REPORTS = "submit_crash_reports";
    static final String ENABLE_CAMERA_PREVIEW = "enable_camera_preview";

    private static SharedPrefs instance;
    private SharedPreferences pref;
    private SharedPreferences.Editor editor;


    public static SharedPrefs getInstance() {
        synchronized (SharedPrefs.class) {
            if (instance == null) {
                instance = new SharedPrefs();
            }

            return instance;
        }
    }

    public SharedPreferences getPref() {
        return pref;
    }

    @SuppressLint("CommitPrefEdits")
    public void init(Context context) {
        pref = context.getSharedPreferences(SHARED_PREFS_NAME, Context.MODE_PRIVATE);
        editor = pref.edit();
    }

    public void setPanicPassword(String password) {
        editor.putString(PANIC_PASSWORD, password);
        editor.apply();
    }

    public String getPanicPassword() {
        return pref.getString(PANIC_PASSWORD, "");
    }

    /*public boolean isTorModeActive() {
        return pref.getBoolean(TOR_MODE_ENABLED, false);
    }

    public void setToreModeActive(boolean activated) {
        editor.putBoolean(TOR_MODE_ENABLED, activated);
        editor.apply();
    }*/

    /*public boolean askForTorOnStart() {
        return pref.getBoolean(ASK_FOR_TOR, true);
    }

    public void setAskForTorOnStart(boolean activated) {
        editor.putBoolean(ASK_FOR_TOR, activated);
        editor.apply();
    }*/

    public void setPanicMessage(String message) {
        editor.putString(PANIC_MESSAGE, message);
        editor.apply();
    }

    public String getPanicMessage() {
        return pref.getString(PANIC_MESSAGE, "");
    }

    public boolean isEraseGalleryActive() {
        return pref.getBoolean(ERASE_GALLERY, true);
    }

    public void setEraseGalleryActive(boolean activated) {
        editor.putBoolean(ERASE_GALLERY, activated);
        editor.apply();
    }

    public boolean isEraseContactsActive() {
        return pref.getBoolean(ERASE_CONTACTS, true);
    }

    public void setEraseContactsActive(boolean activated) {
        editor.putBoolean(ERASE_CONTACTS, activated);
        editor.apply();
    }

    public boolean isEraseMediaRecipientsActive() {
        return pref.getBoolean(ERASE_MEDIA_RECIPIENTS, true);
    }

    public void setEraseMediaRecipientsActive(boolean activated) {
        editor.putBoolean(ERASE_MEDIA_RECIPIENTS, activated);
        editor.apply();
    }

    public boolean isEraseMaterialsActive() {
        return pref.getBoolean(ERASE_MATERIALS, true);
    }

    public void setEraseMaterialsActive(boolean activated) {
        editor.putBoolean(ERASE_MATERIALS, activated);
        editor.apply();
    }

    /*public boolean isAutoSaveDrafts() {
        //return pref.getBoolean(AUTO_SAVE_DRAFT_FORM, false);
        return true; // for now this is default, no settings..
    }

    public void setAutoSaveDrafts(boolean autoSaveDrafts) {
        //editor.putBoolean(AUTO_SAVE_DRAFT_FORM, autoSaveDrafts);
        //editor.apply();
    }*/

    public void setAppLanguage(String language) {
        editor.putString(LANGUAGE, language);
        editor.apply();
    }

    public String getAppLanguage() {
        return pref.getString(LANGUAGE, null);
    }

    public boolean isWiFiAttachments() {
        return pref.getBoolean(WIFI_ATTACHMENTS, false);
    }

    public void setWifiAttachments(boolean wifiReportAttachments) {
        editor.putBoolean(WIFI_ATTACHMENTS, wifiReportAttachments);
        editor.apply();
    }

    boolean getBoolean(final String name, final boolean def) {
        return Single.fromCallable(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                return pref.getBoolean(name, def);
            }
        }).subscribeOn(Schedulers.io()).blockingGet();
    }

    boolean setBoolean(final String name, final boolean value) {
        return Single.fromCallable(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                editor.putBoolean(name, value);
                editor.apply();
                return value;
            }
        }).subscribeOn(Schedulers.io()).blockingGet();
    }

    @NonNull
    String getString(@NonNull final String name, final String def) {
        return Single.fromCallable(new Callable<String>() {
            @Override
            public String call() throws Exception {
                String str = pref.getString(name, def);
                return str != null ? str : NONE;
            }
        }).subscribeOn(Schedulers.io()).blockingGet();
    }

    void setString(@NonNull final String name, final String value) {
        Completable.fromCallable(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                editor.putString(name, value);
                editor.apply();
                return null;
            }
        }).subscribeOn(Schedulers.io()).subscribe();
    }

    private SharedPrefs() {
    }
}
