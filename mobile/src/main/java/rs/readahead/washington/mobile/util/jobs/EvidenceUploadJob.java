package rs.readahead.washington.mobile.util.jobs;

import android.support.annotation.NonNull;

import com.evernote.android.job.Job;
import com.evernote.android.job.JobRequest;
import com.evernote.android.job.util.support.PersistableBundleCompat;

import rs.readahead.washington.mobile.MyApplication;
import rs.readahead.washington.mobile.domain.entity.RawMediaFile;
import rs.readahead.washington.mobile.queue.EvidenceQueue;


public class EvidenceUploadJob extends UploadJob implements EvidenceUploader.UploadRequirementChecker {
    static final String TAG = "EvidenceUploadJob";
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

        final EvidenceQueue queue = MyApplication.evidenceQueue();

        if (queue == null) {
            return exit(Result.SUCCESS);
        }

        final EvidenceUploader uploader = new EvidenceUploader(getContext(), this);

        for (;;) {
            RawMediaFile evidence = queue.peek();

            if (evidence == null) {
                break;
            }

            EvidenceUploader.Result result = uploader.upload(evidence);

            if (result == EvidenceUploader.Result.ERROR || result == EvidenceUploader.Result.RETRY) {
                // rotate so it wont stuck the queue..
                queue.add(evidence);
                queue.remove();

                return exit(Result.RESCHEDULE);
            }

            if (result == EvidenceUploader.Result.COMPLETED ||
                    result == EvidenceUploader.Result.FATAL) {
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
        synchronized (EvidenceUploadJob.class) {
            boolean current = running;
            running = true;
            return !current;
        }
    }

    private Job.Result exit(Job.Result result) {
        synchronized (EvidenceUploadJob.class) {
            running = false;
            return result;
        }
    }

    public static void scheduleJob() {
        final JobRequest.NetworkType networkType = getCurrentNetworkType();
        final PersistableBundleCompat extras = new PersistableBundleCompat();
        extras.putString(NET_REQ_KEY, networkType.name());

        new JobRequest.Builder(EvidenceUploadJob.TAG)
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
