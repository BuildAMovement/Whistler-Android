package rs.readahead.washington.mobile.domain.interactor;

import android.support.annotation.NonNull;

import io.reactivex.Observable;
import rs.readahead.washington.mobile.domain.repository.IReportRepository;
import rs.readahead.washington.mobile.models.Report;


public class CreateReportUseCase extends BaseUseCase<String, Report> {
    private final IReportRepository repository;

    //@Inject
    public CreateReportUseCase(IReportRepository repository) {
        this.repository = repository;
    }

    @Override
    Observable<String> observeUseCase(@NonNull Report report) {
        return this.repository.createReport(report);
    }
}
