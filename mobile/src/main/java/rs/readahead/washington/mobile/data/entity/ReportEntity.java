package rs.readahead.washington.mobile.data.entity;

import com.google.gson.annotations.SerializedName;

import java.util.List;


public class ReportEntity {
    @SerializedName("uid")
    private String uid;

    @SerializedName("title")
    private String title;

    @SerializedName("content")
    private String content;

    @SerializedName("date")
    private long date;

    @SerializedName("location")
    private String location;

    @SerializedName("contactInformation")
    private String contactInformation;

    @SerializedName("public")
    private boolean publicReport;

    @SerializedName("evidences")
    private List<EvidenceEntity> evidences;

    @SerializedName("recipients")
    private List<RecipientEntity> recipients;


    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public List<EvidenceEntity> getEvidences() {
        return evidences;
    }

    public void setEvidences(List<EvidenceEntity> evidences) {
        this.evidences = evidences;
    }

    public List<RecipientEntity> getRecipients() {
        return recipients;
    }

    public void setRecipients(List<RecipientEntity> recipients) {
        this.recipients = recipients;
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

    public long getDate() {
        return date;
    }

    public void setDate(long date) {
        this.date = date;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public boolean isPublicReport() {
        return publicReport;
    }

    public void setPublicReport(boolean publicReport) {
        this.publicReport = publicReport;
    }

    public String getContactInformation() {
        return contactInformation;
    }

    public void setContactInformation(String contactInformation) {
        this.contactInformation = contactInformation;
    }

    public static class EvidenceEntity {
        @SerializedName("name")
        private String name;

        @SerializedName("path")
        private String path;

        @SerializedName("metadata")
        private MetadataEntity metadata;


        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
        }

        public MetadataEntity getMetadata() {
            return metadata;
        }

        public void setMetadata(MetadataEntity metadata) {
            this.metadata = metadata;
        }
    }

    public static class RecipientEntity {
        @SerializedName("title")
        private String title;

        @SerializedName("email")
        private String email;

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }
    }
}
