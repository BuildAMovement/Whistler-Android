package rs.readahead.washington.mobile.util;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;
import android.support.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

import rs.readahead.washington.mobile.R;
import rs.readahead.washington.mobile.data.sharedpref.Preferences;
import rs.readahead.washington.mobile.presentation.entity.CamouflageOption;
import rs.readahead.washington.mobile.views.activity.SplashActivity;


public class CamouflageManager {
    private static CamouflageManager instance;
    private static final String defaultAlias = SplashActivity.class.getCanonicalName();
    private List<CamouflageOption> options;


    public synchronized static CamouflageManager getInstance() {
        if (instance == null) {
            instance = new CamouflageManager();
        }

        return instance;
    }

    private CamouflageManager() {
        options = new ArrayList<>();
        options.add(new CamouflageOption(defaultAlias, R.drawable.whistler_logo_black, R.string.app_name));
        options.add(new CamouflageOption(getOptionAlias("Camera"), R.drawable.camera_shutter_icon, R.string.camera));
        options.add(new CamouflageOption(getOptionAlias("CameraPro"), R.drawable.camera_pro, R.string.camera_pro));
        options.add(new CamouflageOption(getOptionAlias("SuperCam"), R.drawable.super_cam, R.string.super_cam));
        options.add(new CamouflageOption(getOptionAlias("EasyCam"), R.drawable.easy_cam, R.string.easy_cam));
    }

    public boolean setLauncherActivityAlias(@NonNull Context context, @NonNull String activityAlias) {
        String currentAlias = Preferences.getAppAlias();
        if (activityAlias.equals(currentAlias)) {
            return false;
        }

        PackageManager packageManager = context.getPackageManager();
        String packageName = context.getApplicationContext().getPackageName();

        for (CamouflageOption option: options) {
            packageManager.setComponentEnabledSetting(
                    new ComponentName(packageName, option.alias),
                    option.alias.equals(activityAlias) ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED :
                            PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                    PackageManager.DONT_KILL_APP);
        }

        Preferences.setAppAlias(activityAlias);

        return true;
    }

    public boolean isDefaultLauncherActivityAlias() {
        String currentAlias = Preferences.getAppAlias();
        return currentAlias == null || defaultAlias.equals(currentAlias);
    }

    @SuppressWarnings("UnusedReturnValue")
    public boolean setDefaultLauncherActivityAlias(@NonNull Context context) {
        //noinspection SimplifiableIfStatement
        if (isDefaultLauncherActivityAlias()) {
            return false;
        }

        return setLauncherActivityAlias(context, defaultAlias);
    }

    public List<CamouflageOption> getOptions() {
        return options;
    }

    public int getSelectedAliasPosition() {
        String currentAlias = Preferences.getAppAlias();

        for (int i = 0; i < options.size(); i++) {
            if (options.get(i).alias.equals(currentAlias)) {
                return i;
            }
        }

        return 0;
    }

    public void disableSecretMode(@NonNull Context context) {
        Preferences.setSecretModeActive(false);
        setActiveLauncherComponent(context, PackageManager.COMPONENT_ENABLED_STATE_ENABLED);
    }

    public void enableSecretMode(@NonNull Context context) {
        Preferences.setSecretModeActive(true);
        setActiveLauncherComponent(context, PackageManager.COMPONENT_ENABLED_STATE_DISABLED);
    }

    private void setActiveLauncherComponent(@NonNull Context context, int state) {
        ComponentName componentName = getLauncherComponentName(context);
        PackageManager packageManager = context.getPackageManager();

        packageManager.setComponentEnabledSetting(
                componentName,
                state,
                PackageManager.DONT_KILL_APP);
    }

    private ComponentName getLauncherComponentName(@NonNull Context context) {
        String activityAlias = Preferences.getAppAlias();

        if (activityAlias == null) {
            activityAlias = options.get(0).alias;
        }

        return new ComponentName(context.getApplicationContext().getPackageName(), activityAlias);
    }

    private String getOptionAlias(String alias) {
        return "rs.readahead.washington.mobile.views.activity.Alias" + alias;
    }
}
