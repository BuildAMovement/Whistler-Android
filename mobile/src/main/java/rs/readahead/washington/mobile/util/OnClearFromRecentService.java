package rs.readahead.washington.mobile.util;

import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import rs.readahead.washington.mobile.MyApplication;
import rs.readahead.washington.mobile.sharedpref.SharedPrefs;

public class OnClearFromRecentService extends Service {

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d("ClearFromRecentService", "Service Started");
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d("ClearFromRecentService", "Service Destroyed");
    }

    public void onTaskRemoved(Intent rootIntent) {
        Log.e("ClearFromRecentService", "END");

        if (SharedPrefs.getInstance().isTrainingMaterialDownloadStarted()) {
            NotificationManager mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            mNotificationManager.cancel(12);
            SharedPrefs.getInstance().setServiceInterrupted(true);
            stopSelf();
        }
    }
}
