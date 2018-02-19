package rs.readahead.washington.mobile.util.jobs;

import android.support.annotation.NonNull;

import com.evernote.android.job.Job;
import com.evernote.android.job.JobRequest;

import java.util.List;

import rs.readahead.washington.mobile.MyApplication;
import rs.readahead.washington.mobile.R;
import rs.readahead.washington.mobile.bus.event.TrainModuleDownloadErrorEvent;
import rs.readahead.washington.mobile.bus.event.TrainingModuleDownloadedEvent;
import rs.readahead.washington.mobile.data.database.DataSource;
import rs.readahead.washington.mobile.domain.entity.KeyBundle;
import rs.readahead.washington.mobile.domain.entity.TrainModule;
import rs.readahead.washington.mobile.presentation.entity.DownloadState;
import rs.readahead.washington.mobile.util.ThreadUtil;
import rs.readahead.washington.mobile.util.TrainModuleHandler;
import timber.log.Timber;


public class TrainModuleDownloadJob extends Job implements TrainModuleDownloader.RequirementChecker {
    public static final String TAG = "TrainModuleDownloadJob";
    private static boolean running = false;


    @NonNull
    @Override
    protected Result onRunJob(Params params) {
        if (! enter()) {
            return Result.SUCCESS;
        }

        for(;;) {
            KeyBundle keyBundle = MyApplication.getKeyBundle();
            if (keyBundle == null) { // CacheWord is unavailable
                return exit(Result.RESCHEDULE);
            }

            byte[] key = keyBundle.getKey();
            if (key == null) { // key disappeared
                return exit(Result.RESCHEDULE);
            }

            final DataSource dataSource = DataSource.getInstance(getContext(), key);
            List<TrainModule> modules = dataSource.listDownloadingModules().blockingGet();

            if (modules.size() == 0) {
                return exit(Result.SUCCESS);
            }

            for (final TrainModule module : modules) {
                TrainModuleDownloader downloader = new TrainModuleDownloader(getContext(), this);

                TrainModuleDownloader.Result result = downloader.download(module);

                switch (result) {
                    case RETRY:
                    case ERROR:
                        return exit(Result.RESCHEDULE);

                    case COMPLETED:
                        Throwable throwable = dataSource.setTrainModuleDownloadState(
                                module.getId(), DownloadState.DOWNLOADED).blockingGet();

                        if (throwable == null) {
                            postTrainingModuleDownloaded(module);
                        } else {
                            Timber.e(throwable, getClass().getName());
                            return exit(Result.RESCHEDULE);
                        }
                        break;

                    case FATAL:
                        TrainModuleHandler.removeModuleFiles(getContext(), module);
                        //noinspection ThrowableNotThrown
                        dataSource.setTrainModuleDownloadState(
                                module.getId(), DownloadState.NOT_DOWNLOADED).blockingGet();

                        postTrainModuleDownloadError(result.getError() != null ?
                                result.getError() : getContext().getString(R.string.ra_train_dwownload_error));

                        break;
                }
            }
        }
    }

    @Override
    protected void onReschedule(int newJobId) {
    }

    @Override
    public boolean isRequirementMet() {
        return isRequirementNetworkTypeMet();
    }

    private boolean enter() {
        synchronized (TrainModuleDownloadJob.class) {
            boolean current = running;
            running = true;
            return !current;
        }
    }

    private Result exit(Result result) {
        synchronized (TrainModuleDownloadJob.class) {
            running = false;
            return result;
        }
    }

    public static void scheduleJob() {
        new JobRequest.Builder(TrainModuleDownloadJob.TAG)
                .setExecutionWindow(1_000L, 10_000L) // start between 1-10sec from now
                .setBackoffCriteria(10_000L, JobRequest.BackoffPolicy.LINEAR)
                .setRequiredNetworkType(JobRequest.NetworkType.UNMETERED) // fixed
                .setRequirementsEnforced(true)
                .setPersisted(true) // android.Manifest.permission.RECEIVE_BOOT_COMPLETED
                .setUpdateCurrent(false) // jobs will sync them self while running
                .build()
                .schedule();
    }

    private void postTrainingModuleDownloaded(final TrainModule module) {
        ThreadUtil.runOnMain(new Runnable() {
            @Override
            public void run() {
                MyApplication.bus().post(new TrainingModuleDownloadedEvent(
                        getContext().getApplicationContext(), module));
            }
        });
    }

    private void postTrainModuleDownloadError(final String error) {
        ThreadUtil.runOnMain(new Runnable() {
            @Override
            public void run() {
                MyApplication.bus().post(new TrainModuleDownloadErrorEvent(
                        getContext().getApplicationContext(), error));
            }
        });
    }
}
