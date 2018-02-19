package rs.readahead.washington.mobile.util.jobs;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

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


class EvidenceUploader {
    private static final int CHUNK_SIZE = 100 * 1024;

    private final OkHttpClient okHttpClient;
    private final Gson gson;
    private final String baseUrl;

    private byte[] buffer = new byte[CHUNK_SIZE];
    private UploadRequirementChecker checker;

    private Context context;
    private KeyBundle keyBundle;


    EvidenceUploader(Context context, UploadRequirementChecker checker) {
        this.context = context.getApplicationContext();

        okHttpClient = buildOkHttpClient();
        gson = new Gson();
        baseUrl = context.getResources().getString(R.string.upload_url);
        this.checker = checker;
    }

    Result upload(RawMediaFile evidence) {
        if (TextUtils.isEmpty(evidence.getUid()) ||
                TextUtils.isEmpty(evidence.getPath())) {
            return Result.FATAL;
        }

        keyBundle = getKeyBundle();
        if (keyBundle == null) {
            return EvidenceUploader.Result.RETRY;
        }

        byte[] key = keyBundle.getKey();
        if (key == null) {
            return EvidenceUploader.Result.RETRY;
        }

        // check file info on server
        UploadStartResult usr = uploadStart(evidence);
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
            File mediaPath = new File(context.getFilesDir(), evidence.getPath());
            file = new File(mediaPath, evidence.getFileName());
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
                result = uploadLoop(inputStream, evidence);

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

        return uploadComplete(evidence);
    }

    private UploadStartResult uploadStart(RawMediaFile evidence) {
        Response response;
        MediaFileInfo fileInfo = null;

        try {
            Request request = new Request.Builder().url(getInfoUrl(evidence.getUid())).get().build();
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
            if (response.code() == HttpStatus.NOT_FOUND_404) { // evidence upload returns 404 on first attempt
                fileInfo = new MediaFileInfo();
                fileInfo.setName(evidence.getUid());
                fileInfo.setSize(0L);

                return new UploadStartResult(Result.UPLOADING, fileInfo);
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

    public enum Result {
        UPLOADING,
        RETRY, // retry forever..
        UPLOADED,
        COMPLETED,
        ERROR, // retry count..
        FATAL // do not try to reschedule
    }

    public interface UploadRequirementChecker {
        boolean isUploadCanceled();
        boolean isRequirementMet();
    }

    private static class MediaFileInfo {
        @SerializedName("name")
        public String name;

        @SerializedName("size")
        public long size;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public long getSize() {
            return size;
        }

        public void setSize(long size) {
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
