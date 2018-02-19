package rs.readahead.washington.mobile.util.jobs;

import android.support.annotation.NonNull;

import com.evernote.android.job.Job;
import com.evernote.android.job.JobRequest;

import java.util.concurrent.Callable;

import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;
import rs.readahead.washington.mobile.data.sharedpref.SharedPrefs;


/**
 * Base class for "attachment" upload jobs. They are controlled on network type
 * they can upload with.
 */
public abstract class UploadJob extends Job {
    private static volatile JobRequest.NetworkType currentNetworkType;


    public static void updateNetworkType(boolean unmetered) {
        currentNetworkType = unmetered ? JobRequest.NetworkType.UNMETERED :
                JobRequest.NetworkType.CONNECTED;
    }

    @NonNull
    static JobRequest.NetworkType getCurrentNetworkType() {
        if (currentNetworkType == null) {
            boolean unmetered =
                    Single.fromCallable(new Callable<Boolean>() {
                        @Override
                        public Boolean call() throws Exception {
                            return SharedPrefs.getInstance().isWiFiAttachments();
                        }
                    })
                    .subscribeOn(Schedulers.io())
                    .blockingGet();

            updateNetworkType(unmetered);
        }

        return currentNetworkType;
    }
}
