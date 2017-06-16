package rs.readahead.washington.mobile.util;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.webkit.MimeTypeMap;


public class CommonUtils {
    private CommonUtils() {}

    public static void startBrowserIntent(Context context, String url) {
        final PackageManager packageManager = context.getPackageManager();

        try {
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            if (browserIntent.resolveActivity(packageManager) != null) {
                context.startActivity(browserIntent);
            }
        } catch (ActivityNotFoundException ignore) {
        }
    }

    public static void openUri(Context context, Uri uri) {
        final PackageManager packageManager = context.getPackageManager();

        MimeTypeMap mimeTypeMap = MimeTypeMap.getSingleton();
        String mime = "*/*";

        if (mimeTypeMap.hasExtension(MimeTypeMap.getFileExtensionFromUrl(uri.toString()))) {
            mime = mimeTypeMap.getMimeTypeFromExtension(MimeTypeMap.getFileExtensionFromUrl(uri.toString()));
        }

        try {
            Intent intent = new Intent(android.content.Intent.ACTION_VIEW);
            intent.setDataAndType(uri, mime);

            if (intent.resolveActivity(packageManager) != null) {
                context.startActivity(intent);
            }
        } catch (ActivityNotFoundException ignore) {
        }
    }
}
