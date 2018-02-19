package rs.readahead.washington.mobile.util.jobs;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipInputStream;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import rs.readahead.washington.mobile.BuildConfig;
import rs.readahead.washington.mobile.R;
import rs.readahead.washington.mobile.data.rest.FileOkHttpClientBuilder;
import rs.readahead.washington.mobile.domain.entity.TrainModule;
import rs.readahead.washington.mobile.util.C;
import rs.readahead.washington.mobile.util.FileUtil;
import rs.readahead.washington.mobile.util.TrainModuleHandler;
import timber.log.Timber;

import static android.content.Context.NOTIFICATION_SERVICE;
import static rs.readahead.washington.mobile.util.C.TRAIN_DOWNLOADER_NOTIFICATION_ID;


class TrainModuleDownloader {
    private Context context;
    private RequirementChecker checker;
    private final OkHttpClient okHttpClient;

    private NotificationManager notificationManager;
    private NotificationCompat.Builder builder;
    private long lastUpdate = 0;


    TrainModuleDownloader(Context context, RequirementChecker checker) {
        this.context = context.getApplicationContext();
        this.checker = checker;
        okHttpClient = buildOkHttpClient();
    }

    Result download(TrainModule module) {
        Response response = null;
        InputStream input;
        OutputStream output = null;
        Result returnResult = null;

        try {
            File zip = TrainModuleHandler.getModuleZipFile(context, module);
            long downloaded = zip.length();

            if (downloaded >= module.getSize()) {
                return returnResult = unzip(module, zip);
            }

            progressStart(context.getString(R.string.download_starting));

            Request.Builder builder = new Request.Builder().url(module.getUrl());

            if (downloaded > 0) {
                builder.addHeader("Range", "bytes=" + downloaded + "-");
            }

            response = okHttpClient.newCall(builder.build()).execute();

            //noinspection ConstantConditions
            input = response.body().byteStream();
            output = new FileOutputStream(zip, downloaded > 0);

            long downloading = downloaded;
            byte[] buffer = new byte[4096];
            int count;

            while ((count = input.read(buffer)) != -1) {
                if (! checker.isRequirementMet()) {
                    progressPaused(context.getString(R.string.ra_train_module_dl_paused_title),
                            context.getString(R.string.ra_train_module_dl_paused_content));
                    return returnResult = Result.RETRY;
                }

                downloading += count;
                output.write(buffer, 0, count);

                if (module.getSize() > 0) {
                    progressChange((downloading * 100) / module.getSize(), context.getString(R.string.download_in_progress), module.getName());
                }
            }

            progressEnd(context.getString(R.string.download_completed), module.getName());

            output.flush();
            FileUtil.close(output);
            FileUtil.close(response);

            // unzip package
            return returnResult = unzip(module, zip);

        } catch (OutOfMemoryError e) {
            Timber.d(e, getClass().getName());
            return returnResult = Result.FATAL.setError(context.getString(R.string.ra_out_of_disk));
        } catch (IOException e) {
            Timber.d(e, getClass().getName());
            return returnResult = Result.FATAL;
        } catch (Exception e) {
            Timber.d(e, getClass().getName());
            return returnResult = Result.ERROR;
        } finally {
            FileUtil.close(output);
            FileUtil.close(response);

            if (returnResult != Result.RETRY) {
                notificationManager.cancel(C.TRAIN_DOWNLOADER_NOTIFICATION_ID);
            }
        }
    }

    @NonNull
    private OkHttpClient buildOkHttpClient() {
        final FileOkHttpClientBuilder builder = new FileOkHttpClientBuilder();

        if (BuildConfig.DEBUG) {
            builder.setLogLevelHeader();
        }

        return builder.build();
    }

    private Result unzip(TrainModule module, File zip) {
        ZipEntry zipEntry;
        ZipInputStream zis = null;
        File moduleDir;
        FileOutputStream output = null;

        byte[] buffer = new byte[4096];
        int count;

        try {
            moduleDir = TrainModuleHandler.initModuleDir(context, module);
            zis = new ZipInputStream(new BufferedInputStream(new FileInputStream(zip)));

            while ((zipEntry = zis.getNextEntry()) != null) {
                File zipFile = new File(moduleDir, zipEntry.getName());

                if (zipEntry.isDirectory()) {
                    FileUtil.mkdirs(zipFile);
                    continue;
                }

                long fileLength = zipEntry.getSize();

                output = new FileOutputStream(zipFile);
                long total = 0;

                while ((count = zis.read(buffer)) != -1) {
                    output.write(buffer, 0, count);
                    total += count;

                    if (fileLength > 0) {
                        progressChange((total * 100) / fileLength, context.getString(R.string.unzipping_file), zipEntry.getName());
                    }
                }
                output.flush();

                FileUtil.close(output);
                zis.closeEntry();

                progressEnd(context.getString(R.string.unzipping_finished), module.getName());
            }

            FileUtil.close(zis);

        } catch (OutOfMemoryError e) {
            Timber.d(e, getClass().getName());
            return Result.FATAL.setError(context.getString(R.string.ra_out_of_disk));
        } catch (FileNotFoundException e) {
            Timber.d(e, getClass().getName());
            return Result.FATAL;
        } catch (ZipException e) {
            Timber.d(e, getClass().getName());
            Result.FATAL.setError(context.getString(R.string.ra_zip_corrupted));
        } catch (IOException e) {
            Timber.d(e, getClass().getName());
            return Result.FATAL;
        } finally {
            FileUtil.close(output);
            FileUtil.close(zis);
            removeZip(zip);
        }

        return Result.COMPLETED;
    }

    private void removeZip(File zip) {
        FileUtil.delete(zip);
    }

    enum Result {
        RETRY, // retry forever..
        COMPLETED, // done
        ERROR, // retry count..
        FATAL; // do not try to reschedule

        String error = null;

        @Nullable
        public String getError() {
            return error;
        }

        public Result setError(String error) {
            this.error = error;
            return this;
        }
    }

    interface RequirementChecker {
        boolean isRequirementMet();
    }

    private void progressStart(String start) {
        notificationManager = (NotificationManager) context.getSystemService(NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            builder = new NotificationCompat.Builder(context, Notification.CATEGORY_STATUS);
        } else {
            //noinspection deprecation
            builder = new NotificationCompat.Builder(context);
        }

        builder.setContentTitle(start)
                .setColor(context.getResources().getColor(R.color.colorPrimary))
                .setLargeIcon(BitmapFactory.decodeResource(context.getResources(), R.mipmap.ic_launcher))
                .setAutoCancel(true)
                .setSmallIcon(R.drawable.ic_notification_small);

        notificationManager.notify(TRAIN_DOWNLOADER_NOTIFICATION_ID, builder.build());
    }

    private void progressChange(long progress, String title, String content) {
        if (lastUpdate != progress) {
            lastUpdate = progress;

            if (progress < 100) {
                builder.setContentTitle(title)
                        .setContentText(content)
                        .setAutoCancel(true)
                        .setProgress(100, (int) progress, false)
                        .setContentInfo(progress + "%");

                notificationManager.notify(TRAIN_DOWNLOADER_NOTIFICATION_ID, builder.build());
            }
        }
    }

    private void progressEnd(String title, String content) {
        builder.setContentTitle(title)
                .setContentText(content)
                .setAutoCancel(true)
                .setProgress(100, 100, false)
                .setContentInfo(100 + "%")
                .setOngoing(false)
                .setAutoCancel(true);

        notificationManager.notify(TRAIN_DOWNLOADER_NOTIFICATION_ID, builder.build());
    }

    private void progressPaused(String title, String content) {
        builder.setContentTitle(title)
                .setContentText(content)
                .setAutoCancel(true)
                .setOngoing(false)
                .setAutoCancel(true);

        notificationManager.notify(TRAIN_DOWNLOADER_NOTIFICATION_ID, builder.build());
    }
}
