package rs.readahead.washington.mobile.util;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.provider.CallLog;

import rs.readahead.washington.mobile.MyApplication;
import rs.readahead.washington.mobile.sharedpref.SharedPrefs;
import rs.readahead.washington.mobile.views.activity.MainActivity;

public class OpenPassword extends BroadcastReceiver {

    private String password;
    private Context context;


    @Override
    public void onReceive(Context context, final Intent intent) {

        this.context = context;

        if (intent.getAction().equals(Intent.ACTION_NEW_OUTGOING_CALL)) {
            password = intent.getExtras().getString(Intent.EXTRA_PHONE_NUMBER);
        }
        if (SharedPrefs.getInstance().isSecretModeActive()) {
            if (password.equals(SharedPrefs.getInstance().getSecretPassword())) {
                setResultData(null);
                deleteNumber();

                Intent intent1 = new Intent(context, MainActivity.class);
                intent1.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent1);
                context.stopService(intent);

            }
        }

    }

    private void deleteNumber() {
        try {
            String strNumberOne[] = { SharedPrefs.getInstance().getSecretPassword() };
            Cursor cursor = context.getContentResolver().query(CallLog.Calls.CONTENT_URI, null, CallLog.Calls.NUMBER + " = ? ", strNumberOne, "");
            boolean bol = cursor.moveToFirst();
            if (bol) {
                do {
                    int idOfRowToDelete = cursor.getInt(cursor.getColumnIndex(CallLog.Calls._ID));
                    context.getContentResolver().delete(Uri.withAppendedPath(CallLog.Calls.CONTENT_URI, String.valueOf(idOfRowToDelete)), "", null);
                } while (cursor.moveToNext());
            }
        } catch (SecurityException  ex) {
           ex.printStackTrace();
        }
    }
}
