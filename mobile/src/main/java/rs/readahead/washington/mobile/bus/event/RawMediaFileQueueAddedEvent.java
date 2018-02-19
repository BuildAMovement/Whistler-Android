package rs.readahead.washington.mobile.bus.event;

import rs.readahead.washington.mobile.bus.IEvent;


public class RawMediaFileQueueAddedEvent implements IEvent {
    private int size;

    public RawMediaFileQueueAddedEvent(int size) {
        this.size = size;
    }

    public int getSize() {
        return size;
    }
}
