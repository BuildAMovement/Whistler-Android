package rs.readahead.washington.mobile.util;

import android.app.IntentService;
import android.app.NotificationManager;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import rs.readahead.washington.mobile.R;
import rs.readahead.washington.mobile.sharedpref.SharedPrefs;

public class DownloadTrainingService extends IntentService {

    private NotificationManager mNotificationManager;
    private NotificationCompat.Builder mBuilder;
    private int mLastUpdate = 0;
    private String mNotificationTitle;
    private String mNotificationContent;
    private String mNotificationEnd;
    private InputStream input = null;
    private FileOutputStream output = null;
    private boolean mDownloadFinished;
    private long downloaded;
    private String path = FileUtil.getFolderPath(C.FOLDER_TRAINING_MATERIALS) + "/";


    public DownloadTrainingService() {
        super("DownloadTrainingService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {

        mDownloadFinished = false;

        mNotificationContent = getString(R.string.download_in_progress);
        mNotificationTitle = getString(R.string.download_starting);
        mNotificationEnd = getString(R.string.download_completed);


        mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        mBuilder = new NotificationCompat.Builder(this);
        mBuilder.setContentTitle(mNotificationTitle)
                .setColor(getResources().getColor(R.color.colorPrimary))
                .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher))
                .setAutoCancel(true)
                .setSmallIcon(R.drawable.ic_notification_small);
        mNotificationManager.notify(12, mBuilder.build());
        SharedPrefs.getInstance().setTrainingDownloadStarted(true);
        downloadZip();
    }

    private void downloadZip() {

        try {
            File file = new File(FileUtil.getFolderPath(C.FOLDER_TRAINING_MATERIALS), C.ZIP_NAME);
            if (file.exists()) {
                downloaded = file.length();
                if (downloaded == SharedPrefs.getInstance().getTrainingMaterialFileSize()) {
                    unpackZip();
                    return;
                }
            }

            OkHttpClient client = new OkHttpClient();
            Request request = new Request.Builder()
                    .url(C.TRAINING_URL)
                    .addHeader("Range", "bytes=" + downloaded + "-")
                    .build();
            Response response = client.newCall(request).execute();

            if (response.code() == 206) {
                long spaceNeeded = response.body().contentLength();
                if (getFreeSpaceSize() > spaceNeeded) {
                    int fileLength = (int) (spaceNeeded + downloaded);
                    try {
                        if (downloaded == 0) {
                            SharedPrefs.getInstance().setTrainingMaterialFileSize(spaceNeeded);
                            createFile();
                            output = new FileOutputStream(file);
                        } else {
                            output = new FileOutputStream(file, true);
                        }
                        input = response.body().byteStream();
                        byte[] data = new byte[4096];
                        int count;
                        int progress;
                        mNotificationTitle = getString(R.string.downloading_files);
                        while ((count = input.read(data)) > 0) {
                            downloaded += count;
                            progress = (int) ((downloaded * 100) / fileLength);
                            if (fileLength > 0)
                                SharedPrefs.getInstance().setProgressValue(progress);
                            progressChange(progress, 1);
                            try {
                                output.write(data, 0, count);
                            } catch (OutOfMemoryError error) {
                                insufficientSpace();
                            }
                        }
                        mDownloadFinished = true;
                        output.flush();
                        output.close();
                        input.close();
                    } catch (Exception e) {
                        e.printStackTrace();
                    } finally {
                        try {
                            if (output != null)
                                output.close();
                            if (input != null)
                                input.close();
                            if (mDownloadFinished)
                                unpackZip();
                        } catch (IOException ignored) {
                        }
                    }
                } else insufficientSpace();
            }

        } catch (Exception e) {
            e.printStackTrace();

        }
    }

    private boolean unpackZip() {

        InputStream is;
        ZipInputStream zis;
        try {
            String filename;
            is = new FileInputStream(path + C.ZIP_NAME);
            zis = new ZipInputStream(new BufferedInputStream(is));
            ZipEntry mZipEntry;
            SharedPrefs.getInstance().setProgressValue(101);
            while ((mZipEntry = zis.getNextEntry()) != null) {
                filename = mZipEntry.getName();
                mNotificationTitle = getResources().getString(R.string.unzipping_file);
                mNotificationEnd = getResources().getString(R.string.unzipping_finished);
                mNotificationContent = filename;
                int fileLength = (int) mZipEntry.getSize();
                if (fileLength < 0) {
                    fileLength = (int) 0xffffffffl + fileLength;
                }

                if (mZipEntry.isDirectory()) {
                    File fmd = new File(path + filename);
                    fmd.mkdirs();
                    continue;
                }

                FileOutputStream output = new FileOutputStream(path + filename);
                byte[] buffer = new byte[4096];
                int count;
                long total = 0;
                while ((count = zis.read(buffer)) != -1) {
                    total += count;
                    if (fileLength > 0)
                        progressChange((int) ((total * 100) / fileLength), 2);
                    try {
                        output.write(buffer, 0, count);
                    } catch (OutOfMemoryError error) {
                        insufficientSpace();
                    }
                }
                output.close();
                zis.closeEntry();
            }
            zis.close();
        } catch (Exception e) {
            e.printStackTrace();
            finishWithZipError();
            return false;
        }
        progressChange(100, 1);
        SharedPrefs.getInstance().setProgressValue(102);
        finish();
        return true;
    }

    private void createFile() {
        FileUtil.checkFolders(C.FOLDER_TRAINING_MATERIALS);

        File fileDown = new File(FileUtil.getFolderPath(C.FOLDER_TRAINING_MATERIALS), C.ZIP_NAME);
        try {
            fileDown.createNewFile();
        } catch (IOException e1) {
            e1.printStackTrace();
        }
    }

    void progressChange(int progress, int caller) {

        if (mLastUpdate != progress) {
            mLastUpdate = progress;
            if (progress < 100) {

                mBuilder.setContentTitle(mNotificationTitle).
                        setContentText(mNotificationContent)
                        .setAutoCancel(true).
                        setProgress(100, progress, false).
                        setContentInfo(progress + "%");

                mNotificationManager.notify(12, mBuilder.build());
            } else if (caller == 1) {
                mBuilder.setContentTitle(mNotificationTitle)
                        .setContentText(mNotificationEnd)
                        .setAutoCancel(true)
                        .setProgress(0, 0, false)
                        .setOngoing(false)
                        .setContentInfo("")
                        .setAutoCancel(true);
                mNotificationManager.notify(12, mBuilder.build());
            }
        }
    }

    private void finish() {
        File file = new File(FileUtil.getFolderPath(C.FOLDER_TRAINING_MATERIALS), C.ZIP_NAME);
        if (file.exists()) {
            file.delete();
        }
        mNotificationManager.cancel(12);
        SharedPrefs.getInstance().setTrainingDownloaded(true);
        SharedPrefs.getInstance().setTrainingDownloadStarted(false);
        Intent intent = new Intent(C.TRAINING_RECEIVER);
        intent.putExtra(C.DOWNLOAD_RESULT, 1);
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
    }

    private void finishWithZipError() {
        SharedPrefs.getInstance().setProgressValue(102);
        File file = new File(FileUtil.getFolderPath(C.FOLDER_TRAINING_MATERIALS), C.ZIP_NAME);
        if (file.exists()) {
            file.delete();
        }
        mNotificationManager.cancel(12);
        SharedPrefs.getInstance().setTrainingDownloaded(false);
        Intent intent = new Intent(C.TRAINING_RECEIVER);
        intent.putExtra(C.DOWNLOAD_RESULT, 3);
        sendBroadcast(intent);
    }

    private void insufficientSpace() {
        SharedPrefs.getInstance().setProgressValue(102);
        mNotificationManager.cancel(12);
        Intent intent = new Intent(C.TRAINING_RECEIVER);
        intent.putExtra(C.DOWNLOAD_RESULT, 2);
        sendBroadcast(intent);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mNotificationManager.cancel(12);

    }

    private long getFreeSpaceSize() {
        return new File(getExternalFilesDir(null).toString()).getFreeSpace();
    }

}
