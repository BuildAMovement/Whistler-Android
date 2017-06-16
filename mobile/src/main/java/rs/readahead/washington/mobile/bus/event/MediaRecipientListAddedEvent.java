package rs.readahead.washington.mobile.bus.event;

import rs.readahead.washington.mobile.bus.IEvent;
import rs.readahead.washington.mobile.models.MediaRecipientList;


public class MediaRecipientListAddedEvent implements IEvent {
    private final MediaRecipientList mediaRecipientList;


    public MediaRecipientListAddedEvent(MediaRecipientList mediaRecipientList) {
        this.mediaRecipientList = mediaRecipientList;
    }

    public MediaRecipientList getMediaRecipientList() {
        return mediaRecipientList;
    }
}
