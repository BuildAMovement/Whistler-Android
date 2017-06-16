package rs.readahead.washington.mobile.bus.event;

import rs.readahead.washington.mobile.bus.IEvent;
import rs.readahead.washington.mobile.models.MediaRecipient;


public class MediaRecipientRemovedEvent implements IEvent {
    private final MediaRecipient mediaRecipient;


    public MediaRecipientRemovedEvent(MediaRecipient mediaRecipient) {
        this.mediaRecipient = mediaRecipient;
    }

    public MediaRecipient getMediaRecipient() {
        return mediaRecipient;
    }
}
