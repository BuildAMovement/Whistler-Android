package rs.readahead.washington.mobile.mvp.contract;

import android.content.Context;

import java.util.List;

import rs.readahead.washington.mobile.domain.entity.MediaRecipient;
import rs.readahead.washington.mobile.domain.entity.MediaRecipientList;
import rs.readahead.washington.mobile.presentation.entity.ReportRecipientData;
import rs.readahead.washington.mobile.presentation.entity.ReportViewType;


public class IReportRecipientsPresenterContract {
    public interface IView {
        void onAllRecipient(List<MediaRecipient> mediaRecipientList);
        void onAllRecipientError(Throwable throwable);
        void onListNonEmptyRecipientList(List<MediaRecipientList> mediaRecipientLists);
        void onListNonEmptyRecipientListError(Throwable throwable);
        void onAddMediaRecipient(MediaRecipient mediaRecipient);
        void onAddMediaRecipientError(Throwable throwable);
        void onAddMediaRecipientList(MediaRecipientList mediaRecipientList);
        void onAddMediaRecipientListError(Throwable throwable);
        void onPreviewMode();
        Context getContext();
    }

    public interface IPresenter extends IBasePresenter {
        void listAllRecipients();
        void listNonEmptyRecipientLists();
        void addMediaRecipient(MediaRecipient mediaRecipient);
        void addMediaRecipientList(MediaRecipientList mediaRecipientList);
        void setRecipientData(ReportRecipientData recipientData);
        boolean ifRecipientIsSelected(MediaRecipient mediaRecipient);
        boolean ifRecipientListIsSelected(MediaRecipientList mediaRecipientList);
        ReportRecipientData getRecipientsData();
        void addRecipientToReport(MediaRecipient mediaRecipient);
        void removeRecipientFromReport(MediaRecipient mediaRecipient);
        void addRecipientListToReport(MediaRecipientList mediaRecipientList);
        void removeRecipientListFromReport(MediaRecipientList mediaRecipientList);
//        boolean checkSelectMenuItemStatus();
        void setReportType(ReportViewType type);
        ReportViewType getReportType();
        boolean isInPreviewMode();
    }
}
