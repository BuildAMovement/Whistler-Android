package rs.readahead.washington.mobile.domain.entity;

import android.support.annotation.NonNull;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;


public class Report implements Serializable {
    public static final Report NONE = new Report();
    public static final long UNSAVED_REPORT_ID = -1;

    public enum Saved {
        UNKNOWN,
        DRAFT,
        ARCHIVE
    }

    private int startHashCode;

    private long id = UNSAVED_REPORT_ID;
    private String uid; // remote report uid
    private String title;
    private String content;
    private String location;
    private Date date;
    private boolean metadata = true;
    private Saved saved = Saved.UNKNOWN;
    private boolean reportPublic = false;
    private boolean contactInformation = false;
    private String contactInformationData;
    private List<MediaRecipientList> recipientLists = new ArrayList<>();
    private List<MediaRecipient> recipients = new ArrayList<>();
    private List<MediaFile> evidences = new ArrayList<>();


    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public boolean isMetadata() {
        return metadata;
    }

    public void setMetadata(boolean metadata) {
        this.metadata = metadata;
    }

    @NonNull
    public Saved getSaved() {
        return saved;
    }

    public void setSaved(@NonNull Saved saved) {
        this.saved = saved;
    }

    public boolean isReportPublic() {
        return reportPublic;
    }

    public void setReportPublic(boolean reportPublic) {
        this.reportPublic = reportPublic;
    }

    public boolean isContactInformation() {
        return contactInformation;
    }

    public void setContactInformation(boolean contactInformation) {
        this.contactInformation = contactInformation;
    }

    public List<MediaRecipient> getRecipients() {
        return recipients;
    }

    public void setRecipients(List<MediaRecipient> recipients) {
        this.recipients = recipients;
    }

    public List<MediaFile> getEvidences() {
        return evidences;
    }

    public void setEvidences(List<MediaFile> evidences) {
        this.evidences = evidences;
    }

    public void startTouchTracking() {
        startHashCode = hashCode();
    }

    public boolean isTouched() {
        return startHashCode != hashCode();
    }


    public List<MediaRecipientList> getRecipientLists() {
        return recipientLists;
    }

    public void setRecipientLists(List<MediaRecipientList> recipientLists) {
        this.recipientLists = recipientLists;
    }

    public String getContactInformationData() {
        return contactInformationData;
    }

    public void setContactInformationData(String contactInformationData) {
        this.contactInformationData = contactInformationData;
    }

    @Override
    public int hashCode() {
        int result = (int) (id ^ (id >>> 32));
        result = 31 * result + (uid != null ? uid.hashCode() : 0);
        result = 31 * result + (title != null ? title.hashCode() : 0);
        result = 31 * result + (content != null ? content.hashCode() : 0);
        result = 31 * result + (location != null ? location.hashCode() : 0);
        result = 31 * result + (date != null ? date.hashCode() : 0);
        result = 31 * result + (metadata ? 1 : 0);
        result = 31 * result + saved.hashCode();
        result = 31 * result + (reportPublic ? 1 : 0);
        result = 31 * result + (contactInformation ? 1 : 0);
        result = 31 * result + (recipientLists != null ? recipientLists.hashCode() : 0);
        result = 31 * result + (recipients != null ? recipients.hashCode() : 0);
        result = 31 * result + (evidences != null ? evidences.hashCode() : 0);
        return result;
    }
}
