package rs.readahead.washington.mobile.models;

import rs.readahead.washington.mobile.bus.IEvent;


public final class SensorData implements IEvent {
    private static long UNDEFINED = -1;
    private long timestamp = UNDEFINED; // UTC
    private float value;

    public SensorData() {
    }

    public void setValue(long timestamp, float value) {
        this.timestamp = timestamp;
        this.value = value;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public float getValue() {
        return value;
    }

    public boolean hasValue() {
        return timestamp != UNDEFINED;
    }
}
