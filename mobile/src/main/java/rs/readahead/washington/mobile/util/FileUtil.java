package rs.readahead.washington.mobile.util;

import android.os.Environment;

import java.io.File;

public class FileUtil {

    public static String getMainFolderPath() {
        return Environment.getExternalStorageDirectory().toString() + "/" + C.FOLDER_NAME;
    }

    public static String getFolderPath(String folderName) {
        return Environment.getExternalStorageDirectory().toString() + "/" + C.FOLDER_NAME
                + "/" + folderName;
    }

    public static void checkFolders(String folderName) {
        File mainFolder = new File(getMainFolderPath());
        if (!mainFolder.exists()) {
            mainFolder.mkdir();
            mainFolder.mkdirs();
        }

        File folder = new File(getFolderPath(folderName));
        if (!folder.exists()) {
            folder.mkdir();
            folder.mkdirs();
        }

    }

    public static void checkForMainFolder() {
        File folder = new File(getMainFolderPath());
        if (!folder.exists()) {
            folder.mkdir();
            folder.mkdirs();
        }
    }

    public static boolean deleteFolder(String folderName) {
        File folder = new File(getFolderPath(folderName));
        if (folder.exists()) {
            File[] files = folder.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        deleteFolder(file.getPath());
                    } else {
                        file.delete();
                    }
                }
            }
        }
        return (folder.delete());
    }

    public static long folderSize(File folder) {

        if (folder.exists()) {
            long result = 0;
            File[] fileList = folder.listFiles();
            for (File aFileList : fileList) {
                result += aFileList.length();
            }
            return result;
        }
        return 0;
    }

    public static String getEvidenceFileDisplayText(final String path) {
        String[] separated = path.split(File.separator);

        if (separated.length < 2) {
            return path;
        }

        return File.separator + separated[separated.length - 2] +
                File.separator + separated[separated.length - 1];
    }
}
