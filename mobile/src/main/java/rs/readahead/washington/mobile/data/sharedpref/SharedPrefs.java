package rs.readahead.washington.mobile.data.sharedpref;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;


public class SharedPrefs {
    private static final String SHARED_PREFS_NAME = "washington_shared_prefs";

    private static final String SECRET_PASSWORD = "secret_password";
    private static final String SECRET_MODE_ENABLED = "secret_password_enabled";
    private static final String TOR_MODE_ENABLED = "tor_password_enabled";
    private static final String ASK_FOR_TOR = "ask_for_tor";
    private static final String PANIC_PASSWORD = "panic_password";
    private static final String PANIC_MESSAGE = "panic_message";
    private static final String PANIC_GEOLOCATION = "panic_geolocation";
    private static final String ERASE_DATABASE = "erase_database";
    private static final String ERASE_GALLERY = "erase_gallery";
    private static final String ERASE_CONTACTS = "erase_contacts";
    private static final String ERASE_MEDIA_RECIPIENTS = "erase_media";
    private static final String ERASE_MATERIALS = "erase_materials";
    private static final String ANONYMOUS_MODE = "anonymous_mode";
    private static final String AUTO_SAVE_DRAFT_FORM = "auto_save_draft_form";
    private static final String LANGUAGE = "language";
    private static final String DOMAIN_FRONTING = "df";
    private static final String WIFI_ATTACHMENTS = "wifi_attachments";

    private static SharedPrefs instance;
    private SharedPreferences pref;
    private SharedPreferences.Editor editor;

    // small cache
    private Boolean df = null;


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

    public boolean isSecretModeActive() {
        return pref.getBoolean(SECRET_MODE_ENABLED, false);
    }

    public void setSecretModeActive(boolean activated) {
        editor.putBoolean(SECRET_MODE_ENABLED, activated);
        editor.apply();
    }

    public void setSecretPassword(String password) {
        editor.putString(SECRET_PASSWORD, password);
        editor.apply();
    }

    public String getSecretPassword() {
        return pref.getString(SECRET_PASSWORD, "");
    }

    public void setPanicPassword(String password) {
        editor.putString(PANIC_PASSWORD, password);
        editor.apply();
    }

    public String getPanicPassword() {
        return pref.getString(PANIC_PASSWORD, "");
    }

    public boolean isTorModeActive() {
        return pref.getBoolean(TOR_MODE_ENABLED, false);
    }

    public void setToreModeActive(boolean activated) {
        editor.putBoolean(TOR_MODE_ENABLED, activated);
        editor.apply();
    }

    public boolean askForTorOnStart() {
        return pref.getBoolean(ASK_FOR_TOR, true);
    }

    public void setAskForTorOnStart(boolean activated) {
        editor.putBoolean(ASK_FOR_TOR, activated);
        editor.apply();
    }

    public void setPanicMessage(String message) {
        editor.putString(PANIC_MESSAGE, message);
        editor.apply();
    }

    public String getPanicMessage() {
        return pref.getString(PANIC_MESSAGE, "");
    }

    public boolean isPanicGeolocationActive() {
        return pref.getBoolean(PANIC_GEOLOCATION, false);
    }

    public void setPanicGeolocationActive(boolean activated) {
        editor.putBoolean(PANIC_GEOLOCATION, activated);
        editor.apply();
    }

    public boolean isEraseDatabaseActive() {
        return pref.getBoolean(ERASE_DATABASE, true);
    }

    public void setEraseDatabaseActive(boolean activated) {
        editor.putBoolean(ERASE_DATABASE, activated);
        editor.apply();
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

    public boolean isAnonymousMode() {
        return pref.getBoolean(ANONYMOUS_MODE, false);
    }

    public void setAnonymousMode(boolean anonymousMode) {
        editor.putBoolean(ANONYMOUS_MODE, anonymousMode);
        editor.apply();
    }

    public boolean isAutoSaveDrafts() {
        //return pref.getBoolean(AUTO_SAVE_DRAFT_FORM, false);
        return true; // for now this is default, no settings..
    }

    public void setAutoSaveDrafts(boolean autoSaveDrafts) {
        //editor.putBoolean(AUTO_SAVE_DRAFT_FORM, autoSaveDrafts);
        //editor.apply();
    }

    public void setAppLanguage(String language) {
        editor.putString(LANGUAGE, language);
        editor.apply();
    }

    public String getAppLanguage() {
        return pref.getString(LANGUAGE, null);
    }

    public boolean isDomainFronting() {
        if (df == null) {
            return df = pref.getBoolean(DOMAIN_FRONTING, false);
        }

        return df;
    }

    public boolean isWiFiAttachments() {
        return pref.getBoolean(WIFI_ATTACHMENTS, false);
    }

    public void setWifiAttachments(boolean wifiReportAttachments) {
        editor.putBoolean(WIFI_ATTACHMENTS, wifiReportAttachments);
        editor.apply();
    }

    public void setDomainFronting(boolean domainFronting) {
        editor.putBoolean(DOMAIN_FRONTING, domainFronting);
        editor.apply();
        df = null;
    }

    private SharedPrefs() {
    }
}
