package rs.readahead.washington.mobile.views.interfaces;

import rs.readahead.washington.mobile.models.MediaRecipientList;


public interface IRecipientListsHandler {
    void removeMediaRecipientList(MediaRecipientList list);
    void updateMediaRecipientList(MediaRecipientList list);
}
