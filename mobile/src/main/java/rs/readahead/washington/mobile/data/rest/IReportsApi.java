package rs.readahead.washington.mobile.data.rest;

import io.reactivex.Observable;
import retrofit2.http.Body;
import retrofit2.http.POST;
import rs.readahead.washington.mobile.data.entity.ReportEntity;


public interface IReportsApi {
    /**
     * Creates new Report on server.
     *
     * @param report Report
     * @return response
     */
    @POST("reports")
    Observable<CreateReportResponse> createReport(@Body ReportEntity report);
}
