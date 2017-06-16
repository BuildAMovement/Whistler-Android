package rs.readahead.washington.mobile.util;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.util.Log;
import android.view.ViewAnimationUtils;

import static android.R.attr.data;

public class RealPathUtil {

    @SuppressLint("NewApi")
    public static String getRealPathFromURI_API19(Context context, Uri uri) {
        String filePath = "";
        String wholeID;

        try {
            wholeID = DocumentsContract.getDocumentId(uri);
        } catch (IllegalArgumentException e) {
            return filePath;
        }

        String id;
        String[] ids = wholeID.split(":");
        if (ids.length >= 2) {
            id = ids[1];
        } else {
            id = ids[0];
        }
        if (id.matches("[0-9]+")) {
            String[] column = {MediaStore.Images.Media.DATA};
            String sel = MediaStore.Images.Media._ID + "=?";

            Cursor cursor = context.getContentResolver().query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    column, sel, new String[]{id}, null);

            int columnIndex = cursor.getColumnIndex(column[0]);

            if (cursor.moveToFirst()) {
                filePath = cursor.getString(columnIndex);
            }
            cursor.close();
        }
        return filePath;
    }

    public static String getRealPathFormCustomGallery(Context context, Uri uri) {
        String value = "";
        final String[] imageColumns = {MediaStore.Images.Media.DATA};

        try {
            Cursor imageCursor = context.getContentResolver().query(uri, imageColumns,
                    null, null, null);
            assert imageCursor != null;
            if (imageCursor.moveToFirst()) {
                value = imageCursor.getString(imageCursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        if (value == null) {
            value = "";
        }
        return value;
    }

    @SuppressLint("NewApi")
    public static String getRealPathFromURI_API11to18(Context context, Uri contentUri) {
        String[] projestion = {MediaStore.Images.Media.DATA};
        String result = null;

        CursorLoader cursorLoader = new CursorLoader(context, contentUri, projestion, null, null, null);
        Cursor cursor = cursorLoader.loadInBackground();

        if (cursor != null) {
            int column_index =
                    cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            cursor.moveToFirst();
            result = cursor.getString(column_index);
        }
        return result;
    }
}
