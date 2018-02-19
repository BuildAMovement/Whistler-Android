package rs.readahead.washington.mobile.util.jobs;

import com.evernote.android.job.Job;
import com.evernote.android.job.JobCreator;


public class WhistlerJobCreator implements JobCreator {
    @Override
    public Job create(String tag) {
        switch (tag) {
            case EvidenceUploadJob.TAG:
                return new EvidenceUploadJob();

            case MediaFileUploadJob.TAG:
                return new MediaFileUploadJob();

            case PendingFormSendJob.TAG:
                return new PendingFormSendJob();

            case TrainModuleDownloadJob.TAG:
                return new TrainModuleDownloadJob();

            default:
                return null;
        }
    }
}
