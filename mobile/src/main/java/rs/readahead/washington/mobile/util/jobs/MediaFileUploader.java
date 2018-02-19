package rs.readahead.washington.mobile.util.jobs;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.crashlytics.android.Crashlytics;
import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import rs.readahead.washington.mobile.BuildConfig;
import rs.readahead.washington.mobile.MyApplication;
import rs.readahead.washington.mobile.R;
import rs.readahead.washington.mobile.data.http.HttpStatus;
import rs.readahead.washington.mobile.data.provider.EncryptedFileProvider;
import rs.readahead.washington.mobile.data.rest.FileOkHttpClientBuilder;
import rs.readahead.washington.mobile.domain.entity.KeyBundle;
import rs.readahead.washington.mobile.domain.entity.RawMediaFile;
import timber.log.Timber;


class MediaFileUploader {
    private static final int CHUNK_SIZE = 128 * 1024;

    private final OkHttpClient okHttpClient;
    private final Gson gson;
    private final String baseUrl;

    private byte[] buffer = new byte[CHUNK_SIZE];
    private UploadRequirementChecker checker;

    private Context context;
    private KeyBundle keyBundle;


    MediaFileUploader(Context context, UploadRequirementChecker checker) {
        this.context = context.getApplicationContext();

        okHttpClient = buildOkHttpClient();
        gson = new Gson();
        baseUrl = context.getResources().getString(R.string.ra_media_upload_url);
        this.checker = checker;
    }

    Result upload(RawMediaFile mediaFile) {
        keyBundle = getKeyBundle();

        if (keyBundle == null) {
            return Result.RETRY;
        }

        byte[] key = keyBundle.getKey();
        if (key == null) {
            return Result.RETRY;
        }

        // check file info on server
        UploadStartResult usr = uploadStart(mediaFile);
        Result result = usr.result;

        if (result == Result.ERROR || result == Result.RETRY || result == Result.UPLOADED) {
            return result;
        }

        // get needed input stream, skip uploaded bytes
        FileInputStream fis;
        File file;
        InputStream inputStream = null;

        // access file
        try {
            File mediaPath = new File(context.getFilesDir(), mediaFile.getPath());
            file = new File(mediaPath, mediaFile.getFileName());
            fis = new FileInputStream(file);
        } catch (NullPointerException | FileNotFoundException e) {
            Crashlytics.logException(e);
            Timber.d(e, getClass().getName());
            return Result.FATAL;
        }

        // get decrypted bytes stream
        try {
            inputStream = EncryptedFileProvider.getDecryptedInputStream(key, fis, file.getName());

            long skipped = skipBytes(inputStream, usr.mediaFileInfo.getSize());
            if (skipped != usr.mediaFileInfo.getSize()) {
                return Result.ERROR;
            }

            // check key availability again (every stream operation had the key)
            if (! isKeyBundleValid(keyBundle)) {
                return Result.RETRY;
            }

            // start loop..
            if (result == Result.UPLOADING) {
                result = uploadLoop(inputStream, mediaFile);

                if (result != Result.UPLOADED) {
                    return result;
                }
            }
        } catch (IOException e) {
            return Result.ERROR;
        } finally {
            try {
                if (inputStream != null) {
                    inputStream.close();
                }
            } catch (Exception ignored) {
            }
        }

        return uploadComplete(mediaFile);
    }

    private UploadStartResult uploadStart(RawMediaFile mediaFile) {
        Response response;
        MediaFileInfo fileInfo = null;

        try {
            Request request = new Request.Builder().url(getInfoUrl(mediaFile.getUid())).get().build();
            response = okHttpClient.newCall(request).execute();
        } catch (IOException e) {
            Timber.d(e, getClass().getName());
            return new UploadStartResult(Result.RETRY);
        }

        if (response.isSuccessful()) {
            try {
                //noinspection ConstantConditions
                fileInfo = gson.fromJson(response.body().charStream(), MediaFileInfo.class);
            } catch (Exception e) {
                Timber.d(e, getClass().getName());
                //noinspection ConstantConditions
                return new UploadStartResult(Result.RETRY, fileInfo);
            }

            // todo: MediaFileInfo sanity check

            return new UploadStartResult(Result.UPLOADING, fileInfo);
        } else {
            if (response.code() == HttpStatus.NOT_FOUND_404) {
                return new UploadStartResult(Result.FATAL);
            }

            return new UploadStartResult(Result.RETRY);
        }
    }

    private Result uploadLoop(InputStream inputStream, RawMediaFile mediaFile) {
        int read;

        try {
            do {
                if (checker.isUploadCanceled()) {
                    return Result.RETRY;
                }

                if (! checker.isRequirementMet()) {
                    return Result.RETRY;
                }

                int total = 0;
                while((read = inputStream.read(buffer, total, CHUNK_SIZE - total)) != -1) {
                    if (! isKeyBundleValid(keyBundle)) {
                        return Result.RETRY;
                    }

                    total += read;

                    if (total >= CHUNK_SIZE) {
                        break;
                    }
                }

                // continue upload in loop
                Request request = new Request.Builder()
                        .url(getPostUrl(mediaFile.getUid()))
                        .post(RequestBody.create(MediaType.parse("application/octet-stream"), buffer, 0, total))
                        .build();

                Response response = okHttpClient.newCall(request).execute();
                if (! response.isSuccessful()) {
                    if (response.code() == HttpStatus.FORBIDDEN_403) {
                        return Result.FATAL;
                    }

                    return Result.RETRY;
                }
            } while (read != -1);
        } catch (IOException e) {
            Timber.d(e, getClass().getName());
            return Result.ERROR;
        }

        return Result.UPLOADED;
    }

    private Result uploadComplete(RawMediaFile mediaFile) {
        try {
            // inform server we're done
            Request request = new Request.Builder()
                    .url(getDoneUrl(mediaFile.getUid()))
                    .post(RequestBody.create(null, new byte[0]))
                    .header("Content-Length", "0")
                    .build();

            Response response = okHttpClient.newCall(request).execute();
            if (! response.isSuccessful()) {
                return Result.RETRY;
            }

            // todo: update database with upload info

        } catch (IOException e) {
            Timber.d(e, getClass().getName());
            return Result.RETRY;
        }

        return Result.COMPLETED;
    }

    private long skipBytes(InputStream inputStream, long numBytes) throws IOException {
        if (numBytes <= 0) {
            return 0;
        }

        long n = numBytes;
        int nr;

        while (n > 0) {
            // todo: implement AES RandomAccessFile - it is possible with CTR/NoPadding
            nr = inputStream.read(buffer, 0, (int) Math.min(CHUNK_SIZE, n));
            if (nr < 0) {
                break;
            }
            n -= nr;
        }

        return numBytes - n;
    }

    @Nullable
    private KeyBundle getKeyBundle() {
        return MyApplication.getKeyBundle();
    }

    private boolean isKeyBundleValid(KeyBundle keyBundle) {
        return keyBundle != null && MyApplication.isKeyBundleValid(keyBundle);
    }

    @NonNull
    private OkHttpClient buildOkHttpClient() {
        final FileOkHttpClientBuilder builder = new FileOkHttpClientBuilder();

        if (BuildConfig.DEBUG) {
            builder.setLogLevelHeader();
        }

        return builder.build();
    }

    @NonNull
    private String getPostUrl(String name) {
        return baseUrl + name;
    }

    @NonNull
    private String getInfoUrl(String name) {
        return baseUrl + name + "/info";
    }

    @NonNull
    private String getDoneUrl(String name) {
        return baseUrl + name + "/done";
    }


    enum Result {
        UPLOADING,
        RETRY, // retry forever..
        UPLOADED,
        COMPLETED,
        ERROR, // retry count..
        FATAL // do not try to reschedule
    }

    interface UploadRequirementChecker {
        boolean isUploadCanceled();
        boolean isRequirementMet();
    }

    private static class MediaFileInfo {
        @SerializedName("uid")
        public String uid;

        @SerializedName("size")
        public int size;


        public String getUid() {
            return uid;
        }

        public void setUid(String uid) {
            this.uid = uid;
        }

        public int getSize() {
            return size;
        }

        public void setSize(int size) {
            this.size = size;
        }
    }

    private static class UploadStartResult {
        Result result;
        MediaFileInfo mediaFileInfo;


        UploadStartResult(Result result) {
            this.result = result;
        }

        UploadStartResult(Result result, MediaFileInfo mediaFileInfo) {
            this.result = result;
            this.mediaFileInfo = mediaFileInfo;
        }
    }
}
