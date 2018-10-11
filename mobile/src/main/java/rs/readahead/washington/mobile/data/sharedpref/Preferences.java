package rs.readahead.washington.mobile.data.sharedpref;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


public class Preferences {
    private static SharedPrefs sharedPrefs = SharedPrefs.getInstance();

    // cache
    private static Map<String, Boolean> bCache = new ConcurrentHashMap<>();


    public static boolean isSecretModeActive() {
        return getBoolean(SharedPrefs.SECRET_MODE_ENABLED, false);
    }

    public static void setSecretModeActive(boolean value) {
        setBoolean(SharedPrefs.SECRET_MODE_ENABLED, value);
    }

    public static boolean isAnonymousMode() {
        return getBoolean(SharedPrefs.ANONYMOUS_MODE, true);
    }

    public static void setAnonymousMode(boolean value) {
        setBoolean(SharedPrefs.ANONYMOUS_MODE, value);
    }

    public static boolean isDomainFronting() {
        return getBoolean(SharedPrefs.DOMAIN_FRONTING, false);
    }

    public static void setDomainFronting(boolean value) {
        setBoolean(SharedPrefs.DOMAIN_FRONTING, value);
    }

    public static boolean isUninstallOnPanic() {
        return getBoolean(SharedPrefs.UNINSTALL_ON_PANIC, false);
    }

    public static void setUninstallOnPanic(boolean value) {
        setBoolean(SharedPrefs.UNINSTALL_ON_PANIC, value);
    }

    public static boolean isFirstStart() {
        return getBoolean(SharedPrefs.APP_FIRST_START, true);
    }

    public static void setFirstStart(boolean value) {
        setBoolean(SharedPrefs.APP_FIRST_START, value);
    }

    public static boolean isEraseEverything() {
        return getBoolean(SharedPrefs.ERASE_EVERYTHING, true);
    }

    public static void setEraseEverything(boolean value) {
        setBoolean(SharedPrefs.ERASE_EVERYTHING, value);
    }

    public static boolean isEraseReports() {
        return getBoolean(SharedPrefs.ERASE_REPORTS, true);
    }

    public static void setEraseReports(boolean value) {
        setBoolean(SharedPrefs.ERASE_REPORTS, value);
    }

    public static boolean isEraseForms() {
        return getBoolean(SharedPrefs.ERASE_FORMS, true);
    }

    public static void setEraseForms(boolean value) {
        setBoolean(SharedPrefs.ERASE_FORMS, value);
    }

    public static boolean isPanicGeolocationActive() {
        return getBoolean(SharedPrefs.PANIC_GEOLOCATION, true);
    }

    public static void setPanicGeolocationActive(boolean value) {
        setBoolean(SharedPrefs.PANIC_GEOLOCATION, value);
    }

    @Nullable
    public static String getAppAlias() {
        return getString(SharedPrefs.APP_ALIAS_NAME, null);
    }

    public static void setAppAlias(@NonNull String value) {
        setString(SharedPrefs.APP_ALIAS_NAME, value);
    }

    public static String getSecretPassword() {
        return getString(SharedPrefs.SECRET_PASSWORD, "");
    }

    public static void setSecretPassword(@NonNull String value) {
        setString(SharedPrefs.SECRET_PASSWORD, value);
    }

    public static boolean isSubmitingCrashReports() {
        return getBoolean(SharedPrefs.SUBMIT_CRASH_REPORTS, false);
    }

    public static void setSubmitingCrashReports(boolean value) {
        setBoolean(SharedPrefs.SUBMIT_CRASH_REPORTS, value);
    }

    public static boolean isCameraPreviewEnabled() {
        return getBoolean(SharedPrefs.ENABLE_CAMERA_PREVIEW, true);
    }

    public static void setCameraPreviewEnabled(boolean value) {
        setBoolean(SharedPrefs.ENABLE_CAMERA_PREVIEW, value);
    }

    private static boolean getBoolean(String name, boolean def) {
        Boolean value = bCache.get(name);

        if (value == null) {
            value = sharedPrefs.getBoolean(name, def);
            bCache.put(name, value);
        }

        return value;
    }

    private static void setBoolean(String name, boolean value) {
        bCache.put(name, sharedPrefs.setBoolean(name, value));
    }

    @Nullable
    private static String getString(@NonNull String name, String def) {
        String value = sharedPrefs.getString(name, def);
        //noinspection StringEquality
        return (value != SharedPrefs.NONE ? value : def);
    }

    private static void setString(@NonNull String name, String value) {
        sharedPrefs.setString(name, value);
    }

    private Preferences() {
    }
}
