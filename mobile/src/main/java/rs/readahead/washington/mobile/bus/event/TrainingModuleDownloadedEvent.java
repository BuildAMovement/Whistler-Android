package rs.readahead.washington.mobile.bus.event;

import android.content.Context;

import rs.readahead.washington.mobile.bus.IEvent;
import rs.readahead.washington.mobile.domain.entity.TrainModule;


public class TrainingModuleDownloadedEvent implements IEvent {
    private final Context context;
    private final TrainModule module;

    public TrainingModuleDownloadedEvent(Context appContext, TrainModule module) {
        this.context = appContext;
        this.module = module;
    }

    public TrainModule getModule() {
        return module;
    }

    public Context getContext() {
        return context;
    }
}
