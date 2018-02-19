package rs.readahead.washington.mobile.util.jobs;

import android.support.annotation.NonNull;

import com.evernote.android.job.JobRequest;
import com.evernote.android.job.util.support.PersistableBundleCompat;

import rs.readahead.washington.mobile.MyApplication;
import rs.readahead.washington.mobile.domain.entity.RawMediaFile;
import rs.readahead.washington.mobile.queue.RawMediaFileQueue;


public class MediaFileUploadJob extends UploadJob implements MediaFileUploader.UploadRequirementChecker {
    static final String TAG = "MediaFileUploadJob";
    private static final String NET_REQ_KEY = "nrk";

    private static boolean running = false;


    @NonNull
    @Override
    protected Result onRunJob(Params params) {
        if (isCanceled()) {
            return Result.SUCCESS;
        }

        if (! enter()) {
            return Result.SUCCESS;
        }

        final RawMediaFileQueue queue = MyApplication.mediaFileQueue();

        if (queue == null) {
            return exit(Result.SUCCESS);
        }

        for (;;) {
            RawMediaFile mediaFile = queue.peek();

            if (mediaFile == null) {
                break;
            }

            final MediaFileUploader uploader = new MediaFileUploader(getContext(), this);

            MediaFileUploader.Result result = uploader.upload(mediaFile);

            // TODO: maybe quit jobs with retry count > max retry count
            /*if (result == EvidenceUploader.Result.ERROR) {
                mediaFile.incUploadRetryCount();
            }*/

            if (result == MediaFileUploader.Result.ERROR || result == MediaFileUploader.Result.RETRY) {
                // rotate so it wont stuck the queue..
                queue.add(mediaFile);
                queue.remove();

                return exit(Result.RESCHEDULE);
            }

            if (result == MediaFileUploader.Result.COMPLETED || result == MediaFileUploader.Result.FATAL) {
                queue.remove();
            }
        }

        return exit(Result.SUCCESS);
    }

    @Override
    protected void onReschedule(int newJobId) {
        if (netReqChanged()) {
            cancel();
            scheduleJob();
        }
    }

    private boolean netReqChanged() {
        PersistableBundleCompat extras = getParams().getExtras();
        String jobNetReqName = extras.getString(NET_REQ_KEY, null);

        return !getCurrentNetworkType().name().equals(jobNetReqName);
    }

    private boolean enter() {
        synchronized (MediaFileUploadJob.class) {
            boolean current = running;
            running = true;
            return !current;
        }
    }

    private Result exit(Result result) {
        synchronized (MediaFileUploadJob.class) {
            running = false;
            return result;
        }
    }

    public static void scheduleJob() {
        final JobRequest.NetworkType networkType = getCurrentNetworkType();
        final PersistableBundleCompat extras = new PersistableBundleCompat();
        extras.putString(NET_REQ_KEY, networkType.name());

        new JobRequest.Builder(MediaFileUploadJob.TAG)
                .setExtras(extras)
                .setExecutionWindow(1_000L, 10_000L) // start between 1-10sec from now
                .setBackoffCriteria(10_000L, JobRequest.BackoffPolicy.LINEAR) // todo: exponential?
                .setRequiredNetworkType(networkType)
                .setRequirementsEnforced(true)
                .setPersisted(true) // android.Manifest.permission.RECEIVE_BOOT_COMPLETED
                .setUpdateCurrent(false) // jobs will sync them self
                .build()
                .schedule();
    }

    @Override
    public boolean isRequirementMet() {
        return !netReqChanged() && isRequirementNetworkTypeMet();
    }

    @Override
    public boolean isUploadCanceled() {
        return isCanceled();
    }
}
