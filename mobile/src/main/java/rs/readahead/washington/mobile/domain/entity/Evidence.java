package rs.readahead.washington.mobile.domain.entity;

import android.support.annotation.IntDef;
import android.support.annotation.Nullable;

import java.io.Serializable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.UUID;

import rs.readahead.washington.mobile.util.C;


public final class Evidence implements Serializable {
    private static final int SOURCE_UNKNOWN = -1;
    public static final int PICKED_IMAGE = C.PICKED_IMAGE;
    public static final int CAPTURED_IMAGE = C.CAPTURED_IMAGE;
    public static final int CAPTURED_VIDEO = C.CAPTURED_VIDEO;
    public static final int RECORDED_AUDIO = C.RECORDED_AUDIO;


    @Retention(RetentionPolicy.SOURCE)
    @IntDef({SOURCE_UNKNOWN, CAPTURED_IMAGE, PICKED_IMAGE, RECORDED_AUDIO, CAPTURED_VIDEO})
    public @interface EvidenceSource {}

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
        this(uuid(), path, source);
    }

    private Evidence(String uid, String path, @EvidenceSource int source) {
        this.uid = uid;
        this.path = path;
        this.source = source;
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

    @Nullable
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

    public void refreshUid() {
        setUid(uuid());
    }

    private static String uuid() {
        return UUID.randomUUID().toString();
    }
}
