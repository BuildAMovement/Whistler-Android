package rs.readahead.washington.mobile.util;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.design.widget.Snackbar;
import android.telephony.SmsManager;

import android.view.View;

import java.util.List;

import rs.readahead.washington.mobile.R;

public class SmsUtil {

    public static void sendSMS(final List<String> phoneNumbers, final Context context, final String message, final View view) {

        String SENT = "SMS_SENT";
        String DELIVERED = "SMS_DELIVERED";
        final String phoneNumber = phoneNumbers.get(0);

        final PendingIntent sentPI = PendingIntent.getBroadcast(context, 0,
                new Intent(SENT), 0);

        PendingIntent deliveredPI = PendingIntent.getBroadcast(context, 0,
                new Intent(DELIVERED), 0);

        context.registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context arg0, Intent arg1) {
                switch (getResultCode()) {
                    case Activity.RESULT_OK:
                        phoneNumbers.remove(phoneNumber);
                        if (phoneNumbers.size() > 0) {
                            context.unregisterReceiver(this);
                            sendSMS(phoneNumbers, context, message, view);
                            break;
                        } else {
                            context.unregisterReceiver(this);
                            Snackbar.make(view, context.getString(R.string.panic_sent), Snackbar.LENGTH_SHORT).show();
                            break;
                        }
                    case SmsManager.RESULT_ERROR_GENERIC_FAILURE:
                        Snackbar.make(view, context.getString(R.string.panic_sent_error_generic), Snackbar.LENGTH_SHORT).show();
                        break;
                    case SmsManager.RESULT_ERROR_NO_SERVICE:
                        Snackbar.make(view, context.getString(R.string.panic_sent_error_service), Snackbar.LENGTH_SHORT).show();
                        break;
                    case SmsManager.RESULT_ERROR_NULL_PDU:
                        Snackbar.make(view, context.getString(R.string.panic_sent_error_service), Snackbar.LENGTH_SHORT).show();
                        break;
                    case SmsManager.RESULT_ERROR_RADIO_OFF:
                        Snackbar.make(view, context.getString(R.string.panic_sent_error_service), Snackbar.LENGTH_SHORT).show();
                        break;
                }

            }
        }, new IntentFilter(SENT));

//        context.registerReceiver(new BroadcastReceiver() {
//            @Override
//            public void onReceive(Context arg0, Intent arg1) {
//                switch (getResultCode()) {
//                    case Activity.RESULT_OK:
//                        Snackbar.make(view, "SMS delivered",
//                               Snackbar.LENGTH_SHORT).show();
//                        break;
//                    case Activity.RESULT_CANCELED:
//                        Snackbar.make(view, "SMS not delivered",
//                                Snackbar.LENGTH_SHORT).show();
//                        break;
//                }
//            }
//        }, new IntentFilter(DELIVERED));

        SmsManager sms = SmsManager.getDefault();
        sms.sendTextMessage(phoneNumber, null, message, sentPI, deliveredPI);
    }


}
