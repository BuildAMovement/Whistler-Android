package rs.readahead.washington.mobile.mvp.presenter;

import com.crashlytics.android.Crashlytics;

import java.util.List;

import io.reactivex.CompletableSource;
import io.reactivex.SingleSource;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.functions.Action;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;
import rs.readahead.washington.mobile.data.database.CacheWordDataSource;
import rs.readahead.washington.mobile.data.database.DataSource;
import rs.readahead.washington.mobile.domain.entity.Report;
import rs.readahead.washington.mobile.mvp.contract.IReportListPresenterContract;


public class ReportListPresenter implements IReportListPresenterContract.IReportListPresenter {
    private IReportListPresenterContract.IReportListView view;
    private CacheWordDataSource cacheWordDataSource;
    private CompositeDisposable disposable;


    public ReportListPresenter(IReportListPresenterContract.IReportListView view) {
        this.view = view;
        cacheWordDataSource = new CacheWordDataSource(view.getContext().getApplicationContext());
        disposable = new CompositeDisposable();
    }

    @Override
    public void listDraftReports() {
        listReports(Report.Saved.DRAFT);
    }

    @Override
    public void listArchivedReports() {
        listReports(Report.Saved.ARCHIVE);
    }

    @Override
    public void deleteReport(final long id, final int position) {
        disposable.add(cacheWordDataSource.getDataSource()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .flatMapCompletable(new Function<DataSource, CompletableSource>() {
                    @Override
                    public CompletableSource apply(DataSource dataSource) throws Exception {
                        return dataSource.deleteReport(id);
                    }
                })
                // todo: loading indicators..
                .subscribe(new Action() {
                    @Override
                    public void run() throws Exception {
                        view.onDeleteReportSuccess(position);
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) throws Exception {
                        view.onDeleteReportError(throwable);
                    }
                })
        );
    }

    @Override
    public void destroy() {
        view = null;
        disposable.dispose();
        cacheWordDataSource.dispose();
    }

    private void listReports(final Report.Saved saved) {
        disposable.add(cacheWordDataSource.getDataSource()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .flatMapSingle(new Function<DataSource, SingleSource<List<Report>>>() {
                    @Override
                    public SingleSource<List<Report>> apply(DataSource dataSource) throws Exception {
                        return dataSource.listReports(saved);
                    }
                })
                // todo: loading indicators..
                .subscribe(new Consumer<List<Report>>() {
                    @Override
                    public void accept(final List<Report> reports) throws Exception {
                        view.onReportList(reports);
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) throws Exception {
                        Crashlytics.logException(throwable);
                        view.onReportListError(throwable);
                    }
                })
        );
    }
}
