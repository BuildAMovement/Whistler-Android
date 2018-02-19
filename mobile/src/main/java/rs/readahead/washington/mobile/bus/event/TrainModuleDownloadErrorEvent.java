package rs.readahead.washington.mobile.bus.event;

import android.content.Context;

import rs.readahead.washington.mobile.bus.IEvent;


public class TrainModuleDownloadErrorEvent implements IEvent {
    private final Context context;
    private final String error;

    public TrainModuleDownloadErrorEvent(Context appContext, String error) {
        this.context = appContext;
        this.error = error;
    }

    public String getError() {
        return error;
    }

    public Context getContext() {
        return context;
    }
}
