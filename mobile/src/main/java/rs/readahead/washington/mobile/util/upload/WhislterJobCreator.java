package rs.readahead.washington.mobile.util.upload;

import com.evernote.android.job.Job;
import com.evernote.android.job.JobCreator;


public class WhislterJobCreator implements JobCreator {
    @Override
    public Job create(String tag) {
        switch (tag) {
            case EvidenceUploadJob.TAG:
                return new EvidenceUploadJob();
            default:
                return null;
        }
    }
}
