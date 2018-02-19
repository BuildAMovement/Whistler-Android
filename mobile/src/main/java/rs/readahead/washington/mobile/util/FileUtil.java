package rs.readahead.washington.mobile.util;

import android.support.annotation.Nullable;
import android.text.TextUtils;

import java.io.Closeable;
import java.io.File;
import java.util.concurrent.Callable;

import io.reactivex.Completable;
import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;
import timber.log.Timber;


public class FileUtil {
    public static boolean delete(final File file) {
        return Single.fromCallable(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                try {
                    return file.delete();
                } catch (Exception e) {
                    Timber.w(e, FileUtil.class.getName());
                }

                return false;
            }
        }).subscribeOn(Schedulers.io()).blockingGet();
    }

    public static void emptyDir(final File path) {
        Completable.fromCallable(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                deletePathChildren(path);
                return null;
            }
        }).subscribeOn(Schedulers.io()).blockingAwait();
    }

    public static void close(Closeable closeable) {
        try {
            closeable.close();
        } catch (Exception e) {
            Timber.w(e, FileUtil.class.getName());
        }
    }

    @Nullable
    public static String getPrimaryMime(String mimeType) {
        if (TextUtils.isEmpty(mimeType)) {
            return null;
        }

        //noinspection LoopStatementThatDoesntLoop
        for (String token : mimeType.split("/")) {
            return token.toLowerCase();
        }

        return null;
    }

    public static boolean mkdirs(File path) {
        return path.exists() || path.mkdirs();
    }

    private static boolean deletePath(File path) {
        if (path.isDirectory()) {
            File[] children = path.listFiles();

            if (children != null) {
                for (File child: children) {
                    deletePath(child);
                }
            }
        }

        return path.delete();
    }

    private static void deletePathChildren(File path) {
        if (path.isDirectory()) {
            File[] children = path.listFiles();

            if (children != null) {
                for (File child: children) {
                    deletePath(child);
                }
            }
        }
    }
}
