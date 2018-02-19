package rs.readahead.washington.mobile.presentation.entity;


import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import rs.readahead.washington.mobile.domain.entity.MediaRecipient;
import rs.readahead.washington.mobile.domain.entity.MediaRecipientList;

public class ReportRecipientData implements Serializable{

    private List<MediaRecipient> mediaRecipients = new ArrayList<>();
    private List<MediaRecipientList> mediaRecipientLists = new ArrayList<>();

    public List<MediaRecipient> getMediaRecipients() {
        return mediaRecipients;
    }

    public void setMediaRecipients(List<MediaRecipient> mediaRecipients) {
        this.mediaRecipients = mediaRecipients;
    }

    public List<MediaRecipientList> getMediaRecipientLists() {
        return mediaRecipientLists;
    }

    public void setMediaRecipientLists(List<MediaRecipientList> mediaRecipientLists) {
        this.mediaRecipientLists = mediaRecipientLists;
    }


    public ReportRecipientData(List<MediaRecipient> mediaRecipients, List<MediaRecipientList> mediaRecipientLists) {
        this.mediaRecipients = mediaRecipients;
        this.mediaRecipientLists = mediaRecipientLists;
    }

    public ReportRecipientData() {
    }
}
