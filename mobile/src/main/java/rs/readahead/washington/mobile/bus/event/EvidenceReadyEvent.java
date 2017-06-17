package rs.readahead.washington.mobile.bus.event;

import rs.readahead.washington.mobile.bus.IEvent;
import rs.readahead.washington.mobile.domain.entity.Evidence;


public class EvidenceReadyEvent implements IEvent {
    private Evidence evidence;

    public EvidenceReadyEvent(Evidence evidence) {
        this.evidence = evidence;
    }

    public Evidence getEvidence() {
        return evidence;
    }
}
