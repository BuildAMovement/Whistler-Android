package rs.readahead.washington.mobile.util;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;

import permissions.dispatcher.PermissionRequest;
import rs.readahead.washington.mobile.R;


public class PermissionHandler {

    public static boolean checkPermission(final Context context, final String permission, String infoText) {
        final Activity activity = (Activity) context;
        if ((ContextCompat.checkSelfPermission(context, permission)) != PackageManager.PERMISSION_GRANTED) {

            DialogsUtil.showMessageOKCancel(context, infoText,
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                if (!ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)) {
                                    activity.requestPermissions(new String[]{permission},
                                            C.REQUEST_CODE_ASK_PERMISSIONS);
                                } else {
                                    Intent intent = new Intent();
                                    intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                    Uri uri = Uri.fromParts("package", activity.getPackageName(), null);
                                    intent.setData(uri);
                                    context.startActivity(intent);
                                }
                            }
                            dialog.dismiss();
                        }
                    });

            return false;
        }
        return true;
    }

    public static AlertDialog showRationale(final Context context, final PermissionRequest request, final String message) {
        final Activity activity = (Activity) context;

        return new AlertDialog.Builder(activity)
                .setPositiveButton(activity.getResources().getString(R.string.ok), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        request.proceed();
                    }
                })
                .setNegativeButton(activity.getResources().getString(R.string.cancel), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        request.cancel();
                    }
                })
                .setCancelable(false)
                .setMessage(message)
                .show();
    }
}
