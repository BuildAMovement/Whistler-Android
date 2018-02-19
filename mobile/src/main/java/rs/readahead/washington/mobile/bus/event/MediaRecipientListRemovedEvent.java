package rs.readahead.washington.mobile.bus.event;

import rs.readahead.washington.mobile.bus.IEvent;
import rs.readahead.washington.mobile.domain.entity.MediaRecipientList;


public class MediaRecipientListRemovedEvent implements IEvent {
    private final MediaRecipientList mediaRecipientList;


    public MediaRecipientListRemovedEvent(MediaRecipientList mediaRecipientList) {
        this.mediaRecipientList = mediaRecipientList;
    }

    public MediaRecipientList getMediaRecipientList() {
        return mediaRecipientList;
    }
}
