package rs.readahead.washington.mobile.bus.event;


import rs.readahead.washington.mobile.bus.IEvent;

public class EvidenceQueueAddedEvent implements IEvent {
    private int size;

    public EvidenceQueueAddedEvent(int size) {
        this.size = size;
    }

    public int getSize() {
        return size;
    }
}
