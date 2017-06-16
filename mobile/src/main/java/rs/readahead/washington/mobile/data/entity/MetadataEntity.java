package rs.readahead.washington.mobile.data.entity;

import com.google.gson.annotations.SerializedName;

import java.util.List;


public class MetadataEntity {
    @SerializedName("cells")
    private List<String> cells;

    @SerializedName("wifis")
    private List<String> wifis;

    @SerializedName("timestamp")
    private long timestamp;

    @SerializedName("ambientTemperature")
    private Float ambientTemperature;

    @SerializedName("light")
    private Float light;

    @SerializedName("location")
    private LocationEntity location;


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

    public LocationEntity getLocation() {
        return location;
    }

    public void setLocation(LocationEntity location) {
        this.location = location;
    }


    public static class LocationEntity {
        @SerializedName("latitude")
        private double latitude;

        @SerializedName("longitude")
        private double longitude;

        @SerializedName("altitude")
        private Double altitude;

        @SerializedName("accuracy")
        private Float accuracy;


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
    }
}
