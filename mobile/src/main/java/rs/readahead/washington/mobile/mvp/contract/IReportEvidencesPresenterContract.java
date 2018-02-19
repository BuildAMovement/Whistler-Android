package rs.readahead.washington.mobile.mvp.contract;

import android.content.Context;
import android.net.Uri;

import java.util.List;

import rs.readahead.washington.mobile.domain.entity.MediaFile;
import rs.readahead.washington.mobile.media.MediaFileBundle;
import rs.readahead.washington.mobile.presentation.entity.EvidenceData;
import rs.readahead.washington.mobile.presentation.entity.ReportViewType;


public class IReportEvidencesPresenterContract {
    public interface IView {
        void onEvidencesAttached(List<MediaFile> mediaFile);
        void onEvidencesAttachedError(Throwable error);
        void onEvidenceImported(MediaFileBundle mediaFileBundle);
        void onImportError(Throwable error);
        void onImportStarted();
        void onImportEnded();
        void hideFAB();
        Context getContext();
    }

    public interface IPresenter extends IBasePresenter {
        void setEvidences(EvidenceData evidenceData);
        List<MediaFile> getEvidences();
        void attachNewEvidence(MediaFileBundle mediaFileBundle);
        void attachRegisteredEvidences(long[] ids);
        void importImage(Uri uri);
        void importVideo(Uri uri);
        void setReportType(ReportViewType type);
        ReportViewType getReportType();
        boolean isInPreviewMode();
    }
}
