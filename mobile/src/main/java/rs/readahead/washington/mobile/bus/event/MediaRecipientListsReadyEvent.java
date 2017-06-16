package rs.readahead.washington.mobile.bus.event;

import java.util.List;

import rs.readahead.washington.mobile.bus.IEvent;
import rs.readahead.washington.mobile.models.MediaRecipientList;
import rs.readahead.washington.mobile.views.interfaces.IRecipientListsHandler;


public class MediaRecipientListsReadyEvent implements IEvent {
    private final IRecipientListsHandler recipientsHandler;
    private final List<MediaRecipientList> mediaRecipientLists;


    public MediaRecipientListsReadyEvent(IRecipientListsHandler recipientsHandler, List<MediaRecipientList> mediaRecipients) {
        this.recipientsHandler = recipientsHandler;
        this.mediaRecipientLists = mediaRecipients;
    }

    public IRecipientListsHandler getRecipientListsHandler() {
        return recipientsHandler;
    }

    public List<MediaRecipientList> getMediaRecipientLists() {
        return mediaRecipientLists;
    }
}
