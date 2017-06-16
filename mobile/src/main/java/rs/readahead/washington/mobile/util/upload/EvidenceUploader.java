package rs.readahead.washington.mobile.util.upload;

import android.content.Context;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.google.gson.Gson;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;
import com.google.gson.annotations.SerializedName;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;
import rs.readahead.washington.mobile.BuildConfig;
import rs.readahead.washington.mobile.R;
import rs.readahead.washington.mobile.domain.entity.Evidence;
import timber.log.Timber;


public class EvidenceUploader {
    private static final int CHUNK_SIZE = 100 * 1024;

    private final OkHttpClient okHttpClient;
    private final Gson gson;
    private final String baseUrl;

    private String name;
    private RandomAccessFile file;
    private int size, offset;
    private byte[] buffer = new byte[CHUNK_SIZE];
    private UploadRequirementChecker checker;


    EvidenceUploader(Context context, UploadRequirementChecker checker) {
        okHttpClient = buildOkHttpClient();
        gson = new Gson();
        baseUrl = context.getResources().getString(R.string.upload_url);
        this.checker = checker;
    }

    public Result upload(Evidence evidence) {
        if (TextUtils.isEmpty(evidence.getUid()) ||
                TextUtils.isEmpty(evidence.getPath())) {
            return Result.FATAL;
        }

        name = evidence.getUid();
        offset = 0;

        try {
            file = new RandomAccessFile(evidence.getPath(), "r");
            size = (int) file.length(); // IOException
        } catch (FileNotFoundException e) {
            return Result.FATAL;
        } catch (SecurityException e) {
            return Result.ERROR;
        } catch (IOException e) {
            return Result.ERROR;
        }

        Result result = uploadStart();

        if (result == Result.ERROR || result == Result.RETRY) {
            return result;
        }

        if (result == Result.UPLOADING) {
            result = uploadLoop();

            if (result != Result.UPLOADED) {
                return result;
            }
        }

        return uploadComplete();
    }

    private Result uploadStart() {
        Response response;
        FileInfo fileInfo;

        try {
            // get current position from server, file size
            Request request = new Request.Builder().url(getInfoUrl(name)).get().build();
            response = okHttpClient.newCall(request).execute();
        } catch (IOException e) {
            Timber.d(e, "uploadStart");
            return Result.RETRY;
        }

        if (response.isSuccessful()) {
            try {
                fileInfo = gson.fromJson(response.body().charStream(), FileInfo.class);
                Timber.d("****** fileInfo %s %s", fileInfo.name, fileInfo.size);
            } catch (JsonIOException | JsonSyntaxException e) {
                Timber.d(e, "uploadStart");
                return Result.RETRY;
            }

            if (fileInfo.size >= size) {
                Timber.d("****** file already uploaded %s %s", fileInfo.size, size);
                return Result.UPLOADED;
            }

            offset = Math.min(fileInfo.size, size); // maybe server is fucked up..

            try {
                skipBytes(offset);
            } catch (IOException e) {
                Timber.d(e, "uploadStart");
                return Result.ERROR;
            }
        }

        return Result.UPLOADING;
    }

    private Result uploadLoop() {
        int read;

        try {
            do {
                if (! checker.isRequirementMet()) {
                    return Result.RETRY;
                }

                read = file.read(buffer, 0, /*Math.min(CHUNK_SIZE, size - offset)*/CHUNK_SIZE);
                if (read == -1 || read == 0) {
                    continue;
                }

                // continue upload in loop
                Request request = new Request.Builder()
                        .url(getPostUrl(name))
                        .post(RequestBody.create(MediaType.parse("application/octet-stream"), buffer, 0, read))
                        .build();

                Response response = okHttpClient.newCall(request).execute();
                if (! response.isSuccessful()) {
                    return Result.RETRY;
                }

                // update current pos?, emit event?
                offset += read;
            } while (read != -1 && offset < size);
        } catch (IOException e) {
            Timber.d(e, "uploadLoop");
            return Result.ERROR;
        }

        return Result.UPLOADED;
    }

    private Result uploadComplete() {
        try {
            // inform server we're done
            Request request = new Request.Builder()
                    .url(getDoneUrl(name))
                    .post(RequestBody.create(null, new byte[0]))
                    .header("Content-Length", "0")
                    .build();

            Response response = okHttpClient.newCall(request).execute();
            if (! response.isSuccessful()) {
                return Result.RETRY;
            }

        } catch (IOException e) {
            Timber.d(e, "uploadComplete");
            return Result.RETRY;
        }

        Timber.d("***** FILE UPLOAD DONE");
        return Result.COMPLETED;
    }

    private long skipBytes(int numBytes) throws IOException { // todo: check this..
        int n = numBytes;

        while (n > 0) {
            int s = file.skipBytes(numBytes);
            if (s < 0) {
                break;
            }
            n -= s;
        }

        return numBytes - n;
    }

    @NonNull
    private OkHttpClient buildOkHttpClient() {
        final OkHttpClient.Builder okClientBuilder = new OkHttpClient.Builder();

        if (BuildConfig.DEBUG) {
            okClientBuilder.addInterceptor(new HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY));
        }

        return okClientBuilder.build();
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
        boolean isRequirementMet();
    }

    private static class FileInfo {
        @SerializedName("name")
        public String name;

        @SerializedName("size")
        public int size;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public int getSize() {
            return size;
        }

        public void setSize(int size) {
            this.size = size;
        }
    }
}
