package rs.readahead.washington.mobile.data.repository;

import android.support.annotation.NonNull;

import java.util.Map;

import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;
import rs.readahead.washington.mobile.data.entity.mapper.EntityMapper;
import rs.readahead.washington.mobile.data.rest.CreateReportResponse;
import rs.readahead.washington.mobile.data.rest.ReportsApi;
import rs.readahead.washington.mobile.domain.repository.IReportRepository;
import rs.readahead.washington.mobile.domain.entity.MediaRecipient;
import rs.readahead.washington.mobile.domain.entity.Report;


public class ReportRepository implements IReportRepository {
    @Override
    public Observable<String> createReport(@NonNull final Report report, @NonNull Map<Long, MediaRecipient> combined) {
        return ReportsApi.getApi().createReport(new EntityMapper().transform(report, combined))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .map(new Function<CreateReportResponse, String>() {
                    @Override
                    public String apply(CreateReportResponse response) throws Exception {
                        return response.getData().getUid();
                    }
                })
                .onErrorResumeNext(new Function<Throwable, ObservableSource<? extends String>>() {
                   @Override
                   public ObservableSource<? extends String> apply(Throwable throwable) throws Exception {
                       return Observable.error(new ErrorBundle(throwable));
                   }
                });
                // doOnNext can put it to cache..
    }
}
