package rs.readahead.washington.mobile.domain.repository;

import android.support.annotation.NonNull;

import io.reactivex.Observable;
import rs.readahead.washington.mobile.models.Report;


public interface IReportRepository {
    Observable<String> createReport(@NonNull Report report);
}
