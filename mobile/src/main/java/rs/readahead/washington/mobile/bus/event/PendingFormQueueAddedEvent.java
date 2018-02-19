package rs.readahead.washington.mobile.bus.event;

import rs.readahead.washington.mobile.bus.IEvent;


public class PendingFormQueueAddedEvent implements IEvent {
    private int size;

    public PendingFormQueueAddedEvent(int size) {
        this.size = size;
    }

    public int getSize() {
        return size;
    }
}
