package rs.readahead.washington.mobile.util;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.provider.CallLog;

import rs.readahead.washington.mobile.data.sharedpref.SharedPrefs;
import rs.readahead.washington.mobile.views.activity.MainActivity;
import timber.log.Timber;


public class OpenPassword extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, final Intent intent) {
        if (intent.getAction().equals(Intent.ACTION_NEW_OUTGOING_CALL)) {
            String password = intent.getExtras().getString(Intent.EXTRA_PHONE_NUMBER);

            if (password != null && SharedPrefs.getInstance().isSecretModeActive()) {
                if (password.equals(SharedPrefs.getInstance().getSecretPassword())) {
                    setResultData(null);
                    deleteNumber(context);

                    Intent intent1 = new Intent(context, MainActivity.class);
                    intent1.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(intent1);
                    context.stopService(intent);
                }
            }
        }
    }

    private void deleteNumber(Context context) {
        String strNumberOne[] = { SharedPrefs.getInstance().getSecretPassword() };
        Cursor cursor = null;

        try {
            cursor = context.getContentResolver().query(CallLog.Calls.CONTENT_URI, null, CallLog.Calls.NUMBER + " = ? ", strNumberOne, "");

            if (cursor != null && cursor.moveToFirst()) {
                do {
                    int idOfRowToDelete = cursor.getInt(cursor.getColumnIndex(CallLog.Calls._ID));
                    context.getContentResolver().delete(Uri.withAppendedPath(CallLog.Calls.CONTENT_URI, Integer.toString(idOfRowToDelete)), "", null);
                } while (cursor.moveToNext());
            }
        } catch (SecurityException ex) {
           Timber.d(ex, getClass().getName());
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }
}
