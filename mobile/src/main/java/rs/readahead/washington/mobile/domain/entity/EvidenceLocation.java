package rs.readahead.washington.mobile.domain.entity;

import java.io.Serializable;


public final class EvidenceLocation implements Serializable {
    private static final long UNKNOWN = -1;

    private long timestamp; // UTC
    private double latitude;
    private double longitude;
    private Double altitude;
    private Float accuracy;


    public EvidenceLocation() {
        this.timestamp = UNKNOWN;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public Double getAltitude() {
        return altitude;
    }

    public void setAltitude(Double altitude) {
        this.altitude = altitude;
    }

    public Float getAccuracy() {
        return accuracy;
    }

    public void setAccuracy(Float accuracy) {
        this.accuracy = accuracy;
    }

    public boolean isEmpty() {
        return timestamp == UNKNOWN;
    }

    public static EvidenceLocation createEmpty() {
        EvidenceLocation empty = new EvidenceLocation();
        empty.setTimestamp(UNKNOWN);

        return empty;
    }

    @Override
    public String toString() {
        return "EvidenceLocation[timestamp=" + timestamp + " gps " +
                latitude + "," + longitude + " alt=" + altitude + " acc=" + accuracy +"]";
    }
}
