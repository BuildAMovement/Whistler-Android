package rs.readahead.washington.mobile.bus.event;

import rs.readahead.washington.mobile.bus.IEvent;
import rs.readahead.washington.mobile.domain.entity.Evidence;


public class EvidenceAttachLocationMetadataEvent implements IEvent {
    private Evidence evidence;

    public EvidenceAttachLocationMetadataEvent(Evidence evidence) {
        this.evidence = evidence;
    }

    public Evidence getEvidence() {
        return evidence;
    }
}
