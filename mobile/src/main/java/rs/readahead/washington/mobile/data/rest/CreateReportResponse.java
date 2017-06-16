package rs.readahead.washington.mobile.data.rest;

import rs.readahead.washington.mobile.data.entity.ReportEntity;


public class CreateReportResponse extends Response {
    private ReportEntity data; // just get back created report uid


    public ReportEntity getData() {
        return data;
    }

    public void setData(ReportEntity data) {
        this.data = data;
    }
}
