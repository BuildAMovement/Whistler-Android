package rs.readahead.washington.mobile.mvp.view;

import rs.readahead.washington.mobile.domain.entity.IErrorBundle;


public interface ISendReportView {
    void showSendingReport();
    void hideSendingReport();
    void onSendReportSuccess(String uid);
    void onSendReportError(IErrorBundle errorBundle);
}
