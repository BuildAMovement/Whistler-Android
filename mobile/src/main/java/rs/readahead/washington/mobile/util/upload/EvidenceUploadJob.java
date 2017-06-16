package rs.readahead.washington.mobile.util.upload;

import android.support.annotation.NonNull;

import com.evernote.android.job.Job;
import com.evernote.android.job.JobRequest;

import rs.readahead.washington.mobile.MyApplication;
import rs.readahead.washington.mobile.domain.entity.Evidence;
import rs.readahead.washington.mobile.queue.EvidenceQueue;
import timber.log.Timber;


public class EvidenceUploadJob extends Job implements EvidenceUploader.UploadRequirementChecker {
    static final String TAG = "EvidenceUploadJob";
    private static boolean running = false;


    @NonNull
    @Override
    protected Result onRunJob(Params params) {
        if (! enter()) {
            Timber.d("***** Job running, quitting this instance %s", params.getId());
            return Result.SUCCESS;
        }

        Timber.d("***** Entering job %s", params.getId());

        final EvidenceQueue queue = MyApplication.evidenceQueue();

        if (queue == null) {
            return exit(Result.SUCCESS);
        }

        final EvidenceUploader uploader = new EvidenceUploader(getContext(), this);

        for (;;) {
            Evidence evidence = queue.peek();

            if (evidence == null) {
                break;
            }

            EvidenceUploader.Result result = uploader.upload(evidence);

            // TODO: maybe quit jobs with retry count > max retry count
            if (result == EvidenceUploader.Result.ERROR) {
                evidence.incUploadRetryCount();
            }

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
        Timber.d("***** EvidenceUploadJob.onReschedule");
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
        Timber.d("***** EvidenceUploadJob.scheduleJob()");

        /*Set<Job> jobs = JobManager.instance().getAllJobsForTag(TAG);

        if (jobs.size() > 0) {
            Timber.d("***** Job already active %s", jobs.size());
            return;
        }*/

        new JobRequest.Builder(EvidenceUploadJob.TAG)
                .setExecutionWindow(1_000L, 10_000L) // start between 1-10sec from now
                .setBackoffCriteria(10_000L, JobRequest.BackoffPolicy.LINEAR) // todo: exponential?
                .setRequiredNetworkType(JobRequest.NetworkType.UNMETERED)
                .setRequirementsEnforced(true)
                .setPersisted(true) // android.Manifest.permission.RECEIVE_BOOT_COMPLETED
                .setUpdateCurrent(false) // jobs will sync them self
                .build()
                .schedule();
    }


    @Override
    public boolean isRequirementMet() {
        /*if (!isRequirementChargingMet()) {
            return false;
        }

        if (!isRequirementDeviceIdleMet()) {
            return false;
        }*/

        if (! isRequirementNetworkTypeMet()) {
            return false;
        }

        return true;
    }
}
