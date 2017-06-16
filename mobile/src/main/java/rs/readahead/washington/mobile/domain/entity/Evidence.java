package rs.readahead.washington.mobile.domain.entity;

import android.support.annotation.IntDef;

import java.io.Serializable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.UUID;


public final class Evidence implements Serializable {
    private static final int SOURCE_UNKNOWN = -1;
    public static final int PICKED_IMAGE = 1;
    public static final int CAPTURED_IMAGE = 2;
    public static final int CAPTURED_VIDEO = 3;
    public static final int RECORDED_AUDIO = 4;


    @Retention(RetentionPolicy.SOURCE)
    @IntDef({SOURCE_UNKNOWN, CAPTURED_IMAGE, PICKED_IMAGE, RECORDED_AUDIO, CAPTURED_VIDEO})
    @interface EvidenceSource {}

    private String uid;
    private String path;
    private int source;
    private Metadata metadata;
    private int uploadRetryCount;

    public Evidence() {
    }

    public Evidence(String path) {
        this(path, SOURCE_UNKNOWN);
    }

    public Evidence(String path, @EvidenceSource int source) {
        this(UUID.randomUUID().toString(), path, source);
    }

    public Evidence(String name, String path, Metadata metadata) {
        this(name, path, SOURCE_UNKNOWN, metadata);
    }

    public Evidence(String name, String path, @EvidenceSource int source) {
        this.uid = name;
        this.path = path;
        this.source = source;
    }

    public Evidence(String name, String path, @EvidenceSource int source, Metadata metadata) {
        this.uid = name;
        this.path = path;
        this.source = source;
        this.metadata = metadata;
    }

    public String getUid() {
        return uid;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public Metadata getMetadata() {
        return metadata;
    }

    public void setMetadata(Metadata metadata) {
        this.metadata = metadata;
    }

    public int getUploadRetryCount() {
        return uploadRetryCount;
    }

    public void incUploadRetryCount() {
        uploadRetryCount++;
    }

    public boolean isCreatedInWhistler() {
        return (source == CAPTURED_IMAGE ||
                source == CAPTURED_VIDEO ||
                source == RECORDED_AUDIO);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }

        if (this == obj) {
            return true;
        }

        if (! (obj instanceof Evidence)) {
            return false;
        }

        final Evidence that = (Evidence) obj;

        return this.getPath().equals(that.getPath());
    }

    @Override
    public int hashCode() {
        return this.getPath().hashCode();
    }

    public void setUid(String uid) {
        this.uid = uid;
    }
}
