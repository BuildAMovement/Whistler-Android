package rs.readahead.washington.mobile.mvp.contract;

import android.content.Context;
import android.support.annotation.Nullable;

import java.util.Date;

import rs.readahead.washington.mobile.presentation.entity.EvidenceData;
import rs.readahead.washington.mobile.presentation.entity.ReportRecipientData;
import rs.readahead.washington.mobile.presentation.entity.ReportViewType;


public class INewReportPresenterContract {
    public interface IView {
        void onReportLoadError(Throwable throwable);
        void onDraftSaved();
        void onDraftSaveError(Throwable throwable);
        void onSentReport(String msg);
        void onSendValidationFailed();
        void onSendReportStart();
        void onSendReportDone();
        void onSendReportError(Throwable throwable);
        Context getContext();

        void showNoMetadataInfo(boolean show);
        void setTitleText(String title);
        void setDescription(String description);
        void setContactInfo(boolean useContactInfo);
        void setDate(Date date);
        void setPublicInfo(boolean isPublic, boolean isContactInfo);
        void onGetRecipientsCount(int count);
        void onGetRecipientsCountError(Throwable throwable);
        void onEvidenceCounted(String text);
        void setTitleIndicatorColor(int color);
        void setDateIndicatorColor(int color);
        void setEvidenceIndicatorColor(int color);
        void setRecipientsIndicatorColor(int color);
        void onContactInfoAvailable(boolean available);
        void setActivityTitle();
        void setPreviewMode(boolean previewMode);
    }

    public interface IPresenter extends IBasePresenter {
        void startNewReport(@Nullable EvidenceData evidenceData);
        void loadReport(long id);
        void saveDraft();
        void sendReport();

        void checkAnonymousState();
        void setReportTitle(String title);
        void setReportDate(Date date);
        void setReportDescription(String description);
        void setContactInfo(boolean useContactInfo);
        void setPublicInfo(boolean isReportPublic);
        boolean isReportTitleEmpty();
        boolean isReportDescriptionEmpty();
        ReportRecipientData getRecipientData();
        void setRecipientData(ReportRecipientData recipientData);
        void setEvidenceData(EvidenceData evidencedata);
        void getRecipientsCount();
        EvidenceData getEvidenceData();
        void checkContactInfo();
        void setReportType(ReportViewType type);
        ReportViewType getReportType();
        boolean isInPreviewMode();
        boolean isReportChanged();
    }
}
