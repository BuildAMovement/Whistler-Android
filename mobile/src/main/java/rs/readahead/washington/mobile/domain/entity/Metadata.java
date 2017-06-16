package rs.readahead.washington.mobile.domain.entity;

import java.io.Serializable;
import java.util.List;


public final class Metadata implements Serializable {
    private List<String> cells; // sync
    private List<String> wifis; // async
    private long timestamp; // sync, UTC unix timestamp
    private Float ambientTemperature; // semi-sync, nullable
    private Float light; // semi-sync, nullable
    private EvidenceLocation location; // async, nullable


    public void clear() {
        cells = null;
        wifis = null;
        // we leave timestamp
        ambientTemperature = null;
        light = null;
        location = null;
    }

    public List<String> getCells() {
        return cells;
    }

    public void setCells(List<String> cells) {
        this.cells = cells;
    }

    public List<String> getWifis() {
        return wifis;
    }

    public void setWifis(List<String> wifis) {
        this.wifis = wifis;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public Float getAmbientTemperature() {
        return ambientTemperature;
    }

    public void setAmbientTemperature(Float ambientTemperature) {
        this.ambientTemperature = ambientTemperature;
    }

    public Float getLight() {
        return light;
    }

    public void setLight(Float light) {
        this.light = light;
    }

    public EvidenceLocation getEvidenceLocation() {
        return location;
    }

    public void setEvidenceLocation(EvidenceLocation location) {
        this.location = location;
    }
}
