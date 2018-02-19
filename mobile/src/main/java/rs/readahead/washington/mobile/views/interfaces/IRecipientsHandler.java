package rs.readahead.washington.mobile.views.interfaces;

import rs.readahead.washington.mobile.domain.entity.MediaRecipient;


public interface IRecipientsHandler {
    void removeMediaRecipient(MediaRecipient recipient);
    void updateMediaRecipient(MediaRecipient recipient);
}
