package rs.readahead.washington.mobile.mvp.contract;

import android.content.Context;

import java.util.List;

import rs.readahead.washington.mobile.domain.entity.Report;


public class IReportListPresenterContract {
    public interface IReportListView {
        void onReportList(List<Report> reportList);
        void onReportListError(Throwable throwable);
        void onDeleteReportSuccess( int position);
        void onDeleteReportError(Throwable throwable);
        Context getContext();
    }

    public interface IReportListPresenter extends IBasePresenter {
        void listDraftReports();
        void listArchivedReports();
        void deleteReport(long id, int position);
    }
}
