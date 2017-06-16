package rs.readahead.washington.mobile.models;

import android.annotation.SuppressLint;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import rs.readahead.washington.mobile.domain.entity.Evidence;


/**
 *  This class serves as context for NewReport wizard.
 */
public class Report implements Serializable {
    private static final long UNASSIGNED_REPORT_ID = -1;

    private long id = UNASSIGNED_REPORT_ID;
    private String uid; // remote report uid
    private String title;
    private String content;
    private String location;
    private Date date = new Date();
    private boolean metadataSelected = true;
    private boolean keptInArchive = false;
    private boolean keptInDrafts = false;
    private boolean reportPublic = true;

    @SuppressLint("UseSparseArrays")
    private Map<Long, MediaRecipient> recipients = new HashMap<>();
    private List<Evidence> evidences = new ArrayList<>();

    // wizard properties
    private Set<Long> selectedRecipients = new HashSet<>();
    private Set<Integer> selectedRecipientLists = new HashSet<>();

    // all defined ones
    private List<MediaRecipient> allRecipients;
    private List<MediaRecipientList> allRecipientList;


    public boolean isReportPublic() {
        return reportPublic;
    }

    public void setReportPublic(boolean reportPublic) {
        this.reportPublic = reportPublic;
    }

    public boolean isKeptInDrafts() {
        return keptInDrafts;
    }

    public void setKeptInDrafts(boolean keptInDrafts) {
        this.keptInDrafts = keptInDrafts;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
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

    public boolean isMetadataSelected() {
        return metadataSelected;
    }

    public void setMetadataSelected(boolean metadataSelected) {
        this.metadataSelected = metadataSelected;
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

    public boolean isKeptInArchive() {
        return keptInArchive;
    }

    public void setKeptInArchive(boolean keptInArchive) {
        this.keptInArchive = keptInArchive;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public List<Evidence> getEvidences() {
        return evidences;
    }

    public void setEvidences(List<Evidence> evidences) {
        this.evidences = evidences;
    }

    public void addEvidence(Evidence evidence) {
        evidences.add(evidence);
    }

    public boolean containsEvidence(String path) {
        return evidences.contains(new Evidence(path));
    }

    public void removeEvidence(String path) {
        evidences.remove(new Evidence(path));
    }

    public boolean isPersisted() {
        return id != UNASSIGNED_REPORT_ID;
    }

    public Map<Long, MediaRecipient> getRecipients() {
        return recipients;
    }

    public void setRecipients(Map<Long, MediaRecipient> recipients) {
        this.recipients = recipients;
    }

    public boolean containsRecipient(long id) {
        return recipients.containsKey(id);
    }

    public void addRecipient(MediaRecipient recipient) {
        recipients.put(recipient.getId(), recipient);
    }

    public void removeRecipient(long id) {
        recipients.remove(id);
    }

    public void addSelectedRecipient(long id) {
        selectedRecipients.add(id);
    }

    public void removeSelectedRecipient(long id) {
        selectedRecipients.remove(id);
    }

    public Set<Long> getSelectedRecipients() {
        return selectedRecipients;
    }

    public void addSelectedRecipientList(int id) {
        selectedRecipientLists.add(id);
    }

    public void removeSelectedRecipientList(int id) {
        selectedRecipientLists.remove(id);
    }

    public Set<Integer> getSelectedRecipientLists() {
        return selectedRecipientLists;
    }

    public List<MediaRecipient> getAllRecipients() {
        return allRecipients;
    }

    public void setAllRecipients(List<MediaRecipient> allRecipients) {
        this.allRecipients = allRecipients;
    }

    public List<MediaRecipientList> getAllRecipientList() {
        return allRecipientList;
    }

    public void setAllRecipientList(List<MediaRecipientList> allRecipientList) {
        this.allRecipientList = allRecipientList;
    }

    public void generateSelectedRecipients() {
        for (Map.Entry<Long, MediaRecipient> entry: recipients.entrySet()) {
            addSelectedRecipient(entry.getKey());
        }
    }
}
