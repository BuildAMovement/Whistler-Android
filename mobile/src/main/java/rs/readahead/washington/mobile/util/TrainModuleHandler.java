package rs.readahead.washington.mobile.util;

import android.content.Context;

import com.crashlytics.android.Crashlytics;

import java.io.File;

import rs.readahead.washington.mobile.domain.entity.TrainModule;


public class TrainModuleHandler {
    public static boolean init(Context context) {
        try {
            File packagesPath = new File(context.getFilesDir(), C.TRAIN_PACKAGES_DIR);
            boolean ret = FileUtil.mkdirs(packagesPath);
            File modulesPath = new File(context.getFilesDir(), C.TRAIN_MODULES_DIR);
            return FileUtil.mkdirs(modulesPath) && ret;
        } catch (Exception e) {
            Crashlytics.logException(e);
            return false;
        }
    }

    public static File getModuleZipFile(Context context, TrainModule module) {
        File packagesPath = new File(context.getFilesDir(), C.TRAIN_PACKAGES_DIR);
        return new File(packagesPath, module.getId() + ".zip");
    }

    public static File initModuleDir(Context context, TrainModule module) {
        File moduleDir = getModuleDir(context, module.getId());

        FileUtil.emptyDir(moduleDir);
        FileUtil.mkdirs(moduleDir);

        return moduleDir;
    }

    public static void removeModuleFiles(final Context context, final TrainModule module) {
        FileUtil.delete(getModuleZipFile(context, module));
        FileUtil.emptyDir(getModuleDir(context, module.getId()));
    }

    public static File getModuleDir(Context context, long id) {
        File modulesPath = new File(context.getFilesDir(), C.TRAIN_MODULES_DIR);
        return new File(modulesPath, id + "/");
    }

    public static void destroyTrainingModules(Context context) {
        try {
            FileUtil.emptyDir(new File(context.getFilesDir(), C.TRAIN_PACKAGES_DIR));
            FileUtil.emptyDir(new File(context.getFilesDir(), C.TRAIN_MODULES_DIR));
        } catch (Throwable ignored) {
        }
    }
}
