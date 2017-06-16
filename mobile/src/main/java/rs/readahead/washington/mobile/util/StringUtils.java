package rs.readahead.washington.mobile.util;

import android.text.TextUtils;


public class StringUtils {
    public static String orDefault(final String str, final String def) {
        return TextUtils.isEmpty(str) ? def : str;
    }

    public static boolean isEmailValid(CharSequence email) {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    private StringUtils() {
    }
}
