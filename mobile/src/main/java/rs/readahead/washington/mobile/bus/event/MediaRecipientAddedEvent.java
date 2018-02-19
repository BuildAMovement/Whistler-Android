package rs.readahead.washington.mobile.bus.event;

import rs.readahead.washington.mobile.bus.IEvent;
import rs.readahead.washington.mobile.domain.entity.MediaRecipient;


public class MediaRecipientAddedEvent implements IEvent {
    private final MediaRecipient mediaRecipient;


    public MediaRecipientAddedEvent(MediaRecipient mediaRecipient) {
        this.mediaRecipient = mediaRecipient;
    }

    public MediaRecipient getMediaRecipient() {
        return mediaRecipient;
    }
}
