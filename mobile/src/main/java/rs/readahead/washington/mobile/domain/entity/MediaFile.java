package rs.readahead.washington.mobile.domain.entity;

import java.io.Serializable;
import java.util.UUID;

import rs.readahead.washington.mobile.util.C;


public class MediaFile extends RawMediaFile implements Serializable {
    public static final MediaFile NONE = new MediaFile(-1);

    public enum Type {
        UNKNOWN,
        IMAGE,
        AUDIO,
        VIDEO
    }

    private Metadata metadata;
    private long duration; // milliseconds
    private Type type;


    public static MediaFile newJpeg() {
        String uid = UUID.randomUUID().toString();
        return new MediaFile(C.MEDIA_DIR, uid, uid + ".jpg", Type.IMAGE);
    }

    public static MediaFile newAac() {
        String uid = UUID.randomUUID().toString();
        return new MediaFile(C.MEDIA_DIR, uid, uid + ".aac", Type.AUDIO);
    }

    public static MediaFile newMp4() {
        String uid = UUID.randomUUID().toString();
        return new MediaFile(C.MEDIA_DIR, uid, uid + ".mp4", Type.VIDEO);
    }

    // todo: this should be private and DataSource should load type from storage
    public MediaFile(String path, String uid, String filename, Type type) {
        this.uid = uid;
        this.path = path;
        this.fileName = filename;
        this.type = type;
    }

    private MediaFile() {}

    private MediaFile(long id) {
        setId(id);
    }

    public Metadata getMetadata() {
        return metadata;
    }

    public void setMetadata(Metadata metadata) {
        this.metadata = metadata;
    }

    public long getDuration() {
        return duration;
    }

    public void setDuration(long duration) {
        this.duration = duration;
    }

    public Type getType() {
        return type;
    }
}
