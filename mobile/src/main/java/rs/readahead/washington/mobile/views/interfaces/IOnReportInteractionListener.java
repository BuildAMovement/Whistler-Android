package rs.readahead.washington.mobile.views.interfaces;


import rs.readahead.washington.mobile.domain.entity.Report;

public interface IOnReportInteractionListener {
    void onSendReport(Report report);
    void onPreviewReport(long id);
    void onDeleteReport(Report report, int position);
    void onEditReport(long id);
}
