package rs.readahead.washington.mobile.presentation.entity;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import rs.readahead.washington.mobile.domain.entity.MediaFile;


public class EvidenceData implements Serializable {
    private List<MediaFile> evidences = new ArrayList<>();


    public EvidenceData(List<MediaFile> evidences) {
        this.evidences = evidences;
    }

    public List<MediaFile> getEvidences() {
        return evidences;
    }

    public void setEvidences(List<MediaFile> evidences) {
        this.evidences = evidences;
    }

}
