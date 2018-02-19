package rs.readahead.washington.mobile.domain.entity.collect;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.javarosa.core.model.FormDef;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import rs.readahead.washington.mobile.domain.entity.MediaFile;


public class CollectFormInstance {
    public static final CollectFormInstance NONE = new CollectFormInstance();

    private long id;
    private long serverId;
    private String serverName;
    private String username;
    private CollectFormInstanceStatus status = CollectFormInstanceStatus.UNKNOWN;
    private long updated;
    private String formID;
    private String version;
    private String formName;
    private String instanceName;
    private FormDef formDef;
    private List<MediaFile> mediaFiles = new ArrayList<>();
    private long clonedId; // id of submitted instance we are clone of


    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getServerId() {
        return serverId;
    }

    public void setServerId(long serverId) {
        this.serverId = serverId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public CollectFormInstanceStatus getStatus() {
        return status;
    }

    public void setStatus(CollectFormInstanceStatus status) {
        this.status = status;
    }

    public long getUpdated() {
        return updated;
    }

    public void setUpdated(long updated) {
        this.updated = updated;
    }

    public String getFormID() {
        return formID;
    }

    public void setFormID(String formID) {
        this.formID = formID;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getInstanceName() {
        return instanceName;
    }

    public void setInstanceName(String instanceName) {
        this.instanceName = instanceName;
    }

    public FormDef getFormDef() {
        return formDef;
    }

    public void setFormDef(FormDef formDef) {
        this.formDef = formDef;
    }

    public String getServerName() {
        return serverName;
    }

    public void setServerName(String serverName) {
        this.serverName = serverName;
    }

    public String getFormName() {
        return formName;
    }

    public void setFormName(String formName) {
        this.formName = formName;
    }

    public boolean isInstantiated() {
        return status != CollectFormInstanceStatus.UNKNOWN;
    }

    public long getClonedId() {
        return clonedId;
    }

    public void setClonedId(long clonedId) {
        this.clonedId = clonedId;
    }

    @Nullable
    public MediaFile getMediaFile(String uid) {
        for (MediaFile mediaFile: mediaFiles) {
            if (mediaFile.getUid().equals(uid)) {
                return mediaFile;
            }
        }

        return null;
    }

    public boolean addMediaFile(@NonNull MediaFile newMediaFile) {
        for (MediaFile mediaFile: mediaFiles) {
            if (mediaFile.getUid().equals(newMediaFile.getUid())) {
                return false;
            }
        }

        mediaFiles.add(newMediaFile);
        return true;
    }

    public boolean removeMediaFile(@NonNull String uid) {
        for (Iterator<MediaFile> iterator = mediaFiles.iterator(); iterator.hasNext();) {
            MediaFile mediaFile = iterator.next();
            if (mediaFile.getUid().equals(uid)) {
                iterator.remove();
                return true;
            }
        }

        return false;
    }

    public List<MediaFile> getMediaFiles() {
        return mediaFiles;
    }

    public void setMediaFiles(List<MediaFile> mediaFiles) {
        this.mediaFiles = mediaFiles;
    }
}
