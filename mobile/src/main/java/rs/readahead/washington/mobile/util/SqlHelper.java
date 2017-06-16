package rs.readahead.washington.mobile.util;

import android.database.DatabaseUtils;


public class SqlHelper {
    private static final String OBJ_QUOTE = "`";

    public static String objQuote(String str) {
        return OBJ_QUOTE + str + OBJ_QUOTE;
    }

    public static String strQuote(String str) {
        return DatabaseUtils.sqlEscapeString(str);
    }

    public static String columnDdl(String columnName, String columnType) {
        return objQuote(columnName) + " " + columnType;
    }

    private SqlHelper() {}
}
