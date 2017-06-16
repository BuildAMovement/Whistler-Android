package rs.readahead.washington.mobile.views.activity;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.functions.BiFunction;
import io.reactivex.functions.Predicate;
import io.reactivex.subjects.BehaviorSubject;
import rs.readahead.washington.mobile.domain.entity.EvidenceLocation;
import rs.readahead.washington.mobile.models.SensorData;
import rs.readahead.washington.mobile.util.LocationUtil;
import timber.log.Timber;


public class MetadataActivity extends AppCompatActivity implements SensorEventListener {
    private SensorManager mSensorManager;
    private Sensor mLight;
    private Sensor mAmbientTemperature;

    private LocationManager locationManager;
    private MetadataLocationListener locationListener;
    private WifiManager wifiManager;
    private BroadcastReceiver wifiScanResultReceiver;

    private boolean locationListenerRegistered = false;
    private boolean wifiReceiverRegistered = false;

    private static SensorData lightSensorData = new SensorData();
    private static SensorData ambientTemperatureSensorData = new SensorData();

    final BehaviorSubject<List<String>> wifiSubject = BehaviorSubject.create();
    final static BehaviorSubject<EvidenceLocation> locationSubject = BehaviorSubject.create(); // todo: keep it here for now..
    static Location currentBestLocation;

    // todo: after api level 25 change - implement registerGnssStatusCallback

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Sensors
        mSensorManager = (SensorManager) getApplicationContext().getSystemService(Context.SENSOR_SERVICE);
        mLight = mSensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
        mAmbientTemperature = mSensorManager.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE);

        // Location
        locationManager = (LocationManager) getApplicationContext().getSystemService(Context.LOCATION_SERVICE);
        locationListener = new MetadataLocationListener();

        // Wifi
        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        wifiScanResultReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Timber.d("***** wifiScanResultReceiver.onReceive %s", wifiManager.getScanResults().size());

                wifiSubject.onNext(getWifiStrings(wifiManager.getScanResults()));
            }
        };
    }

    private List<String> getWifiStrings(List<ScanResult> results) {
        List<String> wifiStrings = new ArrayList<>(results.size());

        for (ScanResult result: results) {
            wifiStrings.add(result.SSID);
        }

        return wifiStrings;
    }

    protected void startSensorListening() {
        mSensorManager.registerListener(this, mLight, SensorManager.SENSOR_DELAY_NORMAL);
        mSensorManager.registerListener(this, mAmbientTemperature, SensorManager.SENSOR_DELAY_NORMAL);
    }

    protected boolean startLocationMetadataListening() {
        startLocationListening();

        if (locationListenerRegistered) {
            startWifiListening(); // android does not return wifi data if location is off
            return true;
        }

        return false;
    }

    @SuppressWarnings("MissingPermission") // we have check
    private synchronized void startLocationListening() {
        if (isFineLocationPermissionDenied()) {
            return;
        }

        if (locationManager == null || locationListenerRegistered) {
            return;
        }

        Timber.d("***** startLocationListening executed");

        if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
            Timber.d("***** startLocationListening NETWORK_PROVIDER");
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, locationListener);
            locationListenerRegistered = true;
        }

        if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            Timber.d("***** startLocationListening GPS_PROVIDER");
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
            locationListenerRegistered = true;
        }
    }

    private synchronized void startWifiListening() {
        if (isFineLocationPermissionDenied()) {
            return;
        }

        if (wifiManager == null || wifiReceiverRegistered) {
            return;
        }

        Timber.d("***** startWifiListening executed");

        // put what you know in subject..
        Timber.d("***** startWifiListening.getScanResults %s", wifiManager.getScanResults().size());
        wifiSubject.onNext(getWifiStrings(wifiManager.getScanResults()));

        IntentFilter filter = new IntentFilter();
        filter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);

        registerReceiver(wifiScanResultReceiver, filter);
        wifiReceiverRegistered = true;
    }

    protected synchronized void startWifiScan() {
        if (wifiManager != null && wifiReceiverRegistered) {
            wifiManager.startScan();
        }
    }

    protected void stopSensorListening() {
        mSensorManager.unregisterListener(this);
    }

    protected void stopLocationMetadataListening() {
        stopLocationListening();
        stopWifiListening();
    }

    private synchronized void stopLocationListening() {
        if (locationManager == null || !locationListenerRegistered) {
            return;
        }

        Timber.d("***** stopLocationListening()");

        locationManager.removeUpdates(locationListener);
        locationListenerRegistered = false;
    }

    private synchronized void stopWifiListening() {
        if (! wifiReceiverRegistered) {
            return;
        }

        Timber.d("***** stopWifiListening()");

        unregisterReceiver(wifiScanResultReceiver);
        wifiReceiverRegistered = false;

        //wifiSubject.onComplete();
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Register Sensor listener
        startSensorListening();
    }

    @Override
    protected void onPause() {
        super.onPause();

        // Unregister Sensor listener
        stopSensorListening();
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        Sensor sensor = event.sensor;

        if (sensor.getType() == Sensor.TYPE_LIGHT) {
            lightSensorData.setValue(event.timestamp, event.values[0]);
        } else if (sensor.getType() == Sensor.TYPE_AMBIENT_TEMPERATURE) {
            ambientTemperatureSensorData.setValue(event.timestamp, event.values[0]);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        wifiSubject.onComplete();
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    private boolean isFineLocationPermissionDenied() {
        return (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_DENIED);
    }

    // todo: this should be getLast(), getBest()...
    public SensorData getLightSensorData() {
        return lightSensorData;
    }

    public SensorData getAmbientTemperatureSensorData() {
        return ambientTemperatureSensorData;
    }

    public Observable<List<String>> observeWifiData() {
        return wifiSubject;
    }

    public Observable<EvidenceLocation> observeLocationData() {
        return locationSubject;
    }

    /**
     * Will emit combined object consisting of emitted both wifi and location data
     * combined, each time one of them changes. If there is no data for one of them,
     * empty data is in MetadataHolder object.
     *
     * @return stream if metadata holder objects
     */
    public Observable<MetadataHolder> observeMetadata() {
        // todo: problem with this is that while waiting for other, first can not be improved (take(1)). can be better..
        return Observable.combineLatest(
                observeLocationData()
                        .take(1)
                        .startWith(EvidenceLocation.createEmpty()),
                observeWifiData()
                        .take(1)
                        .startWith(Collections.<String>emptyList()),
                new BiFunction<EvidenceLocation, List<String>, MetadataHolder>() {
                    @Override
                    public MetadataHolder apply(EvidenceLocation evidenceLocation, List<String> strings) throws Exception {
                        return new MetadataHolder(evidenceLocation, strings);
                    }
                })
                .filter(new Predicate<MetadataHolder>() {
                    @Override
                    public boolean test(MetadataHolder metadataHolder) throws Exception {
                        return (!metadataHolder.getLocation().isEmpty() || !metadataHolder.getWifis().isEmpty());
                    }
                });
    }

    private static class MetadataLocationListener implements LocationListener {
        public void onLocationChanged(final Location location) {
            Timber.d("###### onLocationChanged %s", location);

            if (! LocationUtil.isBetterLocation(location, currentBestLocation)) {
                Timber.d("***** onLocationChanged: not isBetterLocation %s: %s, %s", location.getProvider(), location, currentBestLocation);
                return;
            }

            currentBestLocation = location;

            EvidenceLocation el = new EvidenceLocation();
            el.setTimestamp(location.getTime());
            el.setAccuracy(location.getAccuracy());
            el.setAltitude(location.getAltitude());
            el.setLatitude(location.getLatitude());
            el.setLongitude(location.getLongitude());

            locationSubject.onNext(el);
        }

        public void onStatusChanged(String provider, int status, Bundle extras) {}

        public void onProviderEnabled(String provider) {}

        public void onProviderDisabled(String provider) {}
    }

    static class MetadataHolder {
        private EvidenceLocation location;
        private List<String> wifis;


        MetadataHolder(EvidenceLocation location, List<String> wifis) {
            this.location = location;
            setWifis(wifis);
        }

        EvidenceLocation getLocation() {
            return location;
        }

        List<String> getWifis() {
            return wifis;
        }

        private void setWifis(final List<String> wifis) {
            this.wifis = new ArrayList<>();

            for (String wifi: wifis) {
                if (! this.wifis.contains(wifi)) {
                    this.wifis.add(wifi);
                }
            }
        }

        static MetadataHolder createEmpty() {
            return new MetadataHolder(EvidenceLocation.createEmpty(), Collections.<String>emptyList());
        }
    }
}
