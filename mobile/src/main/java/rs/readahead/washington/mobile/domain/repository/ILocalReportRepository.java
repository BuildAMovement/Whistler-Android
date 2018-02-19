package rs.readahead.washington.mobile.domain.repository;

import java.util.List;

import io.reactivex.Completable;
import io.reactivex.Single;
import rs.readahead.washington.mobile.domain.entity.Report;


public interface ILocalReportRepository {
    Single<Report> loadReport(long id);
    Single<Report> saveReport(Report report, Report.Saved saved);
    Completable deleteReport(long id);
    Single<List<Report>> listReports(Report.Saved saved);
}
