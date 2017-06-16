package rs.readahead.washington.mobile.bus.event;

import java.util.List;

import rs.readahead.washington.mobile.bus.IEvent;
import rs.readahead.washington.mobile.models.MediaRecipient;
import rs.readahead.washington.mobile.views.interfaces.IRecipientsHandler;


public class MediaRecipientsReadyEvent implements IEvent {
    private final IRecipientsHandler recipientsHandler;
    private final List<MediaRecipient> mediaRecipients;


    public MediaRecipientsReadyEvent(IRecipientsHandler recipientsHandler, List<MediaRecipient> mediaRecipients) {
        this.recipientsHandler = recipientsHandler;
        this.mediaRecipients = mediaRecipients;
    }

    public IRecipientsHandler getRecipientsHandler() {
        return recipientsHandler;
    }

    public List<MediaRecipient> getMediaRecipients() {
        return mediaRecipients;
    }
}
