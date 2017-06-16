package rs.readahead.washington.mobile.bus.event;


import rs.readahead.washington.mobile.bus.IEvent;

public class EvidenceQueueRemovedEvent implements IEvent {
    private int size;

    public EvidenceQueueRemovedEvent(int size) {
        this.size = size;
    }

    public int getSize() {
        return size;
    }
}
