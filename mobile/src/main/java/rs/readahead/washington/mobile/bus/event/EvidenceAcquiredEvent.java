package rs.readahead.washington.mobile.bus.event;

import rs.readahead.washington.mobile.bus.IEvent;
import rs.readahead.washington.mobile.domain.entity.Evidence;


public class EvidenceAcquiredEvent implements IEvent {
    private Evidence evidence;

    public EvidenceAcquiredEvent(Evidence evidence) {
        this.evidence = evidence;
    }

    public Evidence getEvidence() {
        return evidence;
    }

    public void setEvidence(Evidence evidence) {
        this.evidence = evidence;
    }
}
