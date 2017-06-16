package rs.readahead.washington.mobile.sharedpref;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;


public class SharedPrefs {
    private static final String SHARED_PREFS_NAME = "washington_shared_prefs";

    private static final String TRAINING_MATERIAL = "training_materials";
    public static final String PROGRESS_VALUE = "progress_value"; // todo: proper event for this..
    private static final String TRAINING_MATERIAL_FILE_SIZE = "training_material_file_size";
    private static final String SECRET_PASSWORD = "secret_password";
    private static final String SECRET_MODE_ENABLED = "secret_password_enabled";
    private static final String TOR_MODE_ENABLED = "tor_password_enabled";
    private static final String ASK_FOR_TOR = "ask_for_tor";
    private static final String PANIC_PASSWORD = "panic_password";
    private static final String PANIC_MESSAGE = "panic_message";
    private static final String PANIC_GEOLOCATION = "panic_geolocation";
    private static final String ERASE_DATABASE = "erase_database";
    private static final String ERASE_CONTACTS = "erase_contacts";
    private static final String ERASE_MEDIA = "erase_media";
    private static final String ERASE_MATERIALS = "erase_materials";
    private static final String ERASE_VIDEO = "erase_video";
    private static final String ERASE_AUDIO = "erase_audio";
    private static final String ERASE_PHOTO = "erase_photo";
    private static final String DOWNLOAD_STARTED = "download_started";
    private static final String SERVICE_INTERRUPTED = "service_interrupted";
    private static final String PASSPHRASE = "passphrase";
    private static final String FIRST_RUN = "first_run";

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

    public void registerOnSharedPreferenceChangeListener(SharedPreferences.OnSharedPreferenceChangeListener listener) {
        pref.registerOnSharedPreferenceChangeListener(listener);
    }

    public void unregisterOnSharedPreferenceChangeListener(SharedPreferences.OnSharedPreferenceChangeListener listener) {
        pref.unregisterOnSharedPreferenceChangeListener(listener);
    }

    @SuppressLint("CommitPrefEdits")
    public void init(Context context) {
        pref = context.getSharedPreferences(SHARED_PREFS_NAME, Context.MODE_PRIVATE);
        editor = pref.edit();
    }

    public boolean isTrainingMaterialDownloaded() {
        return pref.getBoolean(TRAINING_MATERIAL, false);
    }

    public void setTrainingDownloaded(boolean downloaded) {
        editor.putBoolean(TRAINING_MATERIAL, downloaded);
        editor.apply();

    }

    public boolean isTrainingMaterialDownloadStarted() {
        return pref.getBoolean(DOWNLOAD_STARTED, false);
    }

    public void setTrainingDownloadStarted(boolean downloaded) {
        editor.putBoolean(DOWNLOAD_STARTED, downloaded);
        editor.apply();
    }

    public boolean isServiceInterrupted() {
        return pref.getBoolean(SERVICE_INTERRUPTED, false);
    }

    public void setServiceInterrupted(boolean downloaded) {
        editor.putBoolean(SERVICE_INTERRUPTED, downloaded);
        editor.apply();
    }

    public void setProgressValue(int progress) {
        editor.putInt(PROGRESS_VALUE, progress);
        editor.apply();
    }

    public int getProgressValue() {
        return pref.getInt(PROGRESS_VALUE, 0);
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

    public void setPassphrase(String password) {
        editor.putString(PASSPHRASE, password);
        editor.apply();
    }

    public String getPassphrase() {
        return pref.getString(PASSPHRASE, "");
    }

    public boolean isTorModeActive() {
        return pref.getBoolean(TOR_MODE_ENABLED, false);
    }

    public void setToreModeActive(boolean activated) {
        editor.putBoolean(TOR_MODE_ENABLED, activated);
        editor.apply();
    }

    public long getTrainingMaterialFileSize() {
        return pref.getLong(TRAINING_MATERIAL_FILE_SIZE, 0);
    }

    public void setTrainingMaterialFileSize(long size) {
        editor.putLong(TRAINING_MATERIAL_FILE_SIZE, size);
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

    public boolean isEraseContactsActive() {
        return pref.getBoolean(ERASE_CONTACTS, true);
    }

    public void setEraseContactsActive(boolean activated) {
        editor.putBoolean(ERASE_CONTACTS, activated);
        editor.apply();
    }

    public boolean isEraseMediaActive() {
        return pref.getBoolean(ERASE_MEDIA, true);
    }

    public void setEraseMediaActive(boolean activated) {
        editor.putBoolean(ERASE_MEDIA, activated);
        editor.apply();
    }

    public boolean isEraseMaterialsActive() {
        return pref.getBoolean(ERASE_MATERIALS, true);
    }

    public void setEraseMaterialsActive(boolean activated) {
        editor.putBoolean(ERASE_MATERIALS, activated);
        editor.apply();
    }

    public boolean isEraseVideosActive() {
        return pref.getBoolean(ERASE_VIDEO, true);
    }

    public void setEraseVideosActive(boolean activated) {
        editor.putBoolean(ERASE_VIDEO, activated);
        editor.apply();
    }

    public boolean isEraseAudiosActive() {
        return pref.getBoolean(ERASE_AUDIO, true);
    }

    public void setEraseAudiosActive(boolean activated) {
        editor.putBoolean(ERASE_AUDIO, activated);
        editor.apply();
    }

    public boolean isErasePhotosActive() {
        return pref.getBoolean(ERASE_PHOTO, true);
    }

    public void setErasePhotosActive(boolean activated) {
        editor.putBoolean(ERASE_PHOTO, activated);
        editor.apply();
    }

    public boolean isFirstRun() {
        return pref.getBoolean(FIRST_RUN, true);
    }

    public void setFirstRun(boolean firstRun) {
        editor.putBoolean(FIRST_RUN, firstRun);
        editor.apply();
    }


    private SharedPrefs() {
    }
}
