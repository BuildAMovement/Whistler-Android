package rs.readahead.washington.mobile.domain.repository;

import android.support.annotation.NonNull;

import java.util.Map;

import io.reactivex.Observable;
import rs.readahead.washington.mobile.domain.entity.MediaRecipient;
import rs.readahead.washington.mobile.domain.entity.Report;


public interface IReportRepository {
    Observable<String> createReport(@NonNull Report report, @NonNull Map<Long, MediaRecipient> combined);
}
