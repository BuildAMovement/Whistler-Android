package rs.readahead.washington.mobile.domain.entity;

import android.support.annotation.Nullable;
import android.webkit.MimeTypeMap;

import java.io.Serializable;
import java.util.UUID;

import rs.readahead.washington.mobile.util.C;


public class MediaFile extends RawMediaFile implements Serializable {
    public static final MediaFile NONE = new MediaFile(-1);

    private Metadata metadata;
    private long duration; // milliseconds


    public static MediaFile newJpeg() {
        String uid = UUID.randomUUID().toString();
        return new MediaFile(C.MEDIA_DIR, uid, uid + ".jpg");
    }

    public static MediaFile newAac() {
        String uid = UUID.randomUUID().toString();
        return new MediaFile(C.MEDIA_DIR, uid, uid + ".aac");
    }

    public static MediaFile newMp4() {
        String uid = UUID.randomUUID().toString();
        return new MediaFile(C.MEDIA_DIR, uid, uid + ".mp4");
    }

    public MediaFile(String path, String uid, String filename) {
        this.uid = uid;
        this.path = path;
        this.fileName = filename;
    }

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

    @Nullable
    public String getMimeType() {
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(
                MimeTypeMap.getFileExtensionFromUrl(getFileName().toLowerCase())
        );
    }

    @Nullable
    public String getPrimaryMimeType() {
        String mimeType = getMimeType();

        if (mimeType == null) {
            return null;
        }

        //noinspection LoopStatementThatDoesntLoop
        for (String token: mimeType.split("/")) {
            return token.toLowerCase();
        }

        return mimeType;
    }
}
