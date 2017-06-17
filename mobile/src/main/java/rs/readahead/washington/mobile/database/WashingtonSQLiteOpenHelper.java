package rs.readahead.washington.mobile.database;

import android.content.Context;

import net.sqlcipher.database.SQLiteDatabase;

import info.guardianproject.cacheword.CacheWordHandler;


class WashingtonSQLiteOpenHelper extends CipherOpenHelper {
    private static final String OBJ_QUOTE = "`";


    WashingtonSQLiteOpenHelper(CacheWordHandler cacheWord, Context context) {
        super(cacheWord, context, D.DATABASE_NAME, null, D.DATABASE_VERSION);
    }

    @Override
    public void onOpen(SQLiteDatabase db) {
        super.onOpen(db);
        if (!db.isReadOnly()) {
            db.execSQL("PRAGMA foreign_keys=ON;");
        }
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(createTableRecipient());
        db.execSQL(createTableTrustedPerson());
        db.execSQL(createTableReport());
        db.execSQL(createTableEvidence());
        db.execSQL(createTableRecipientRecipientList());
        db.execSQL(createTableRecipientList());
        db.execSQL(createTableReportRecipient());
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    }

    private String createTableRecipient() {
        return "CREATE TABLE " + sq(D.T_RECIPIENT) + " (" +
                cddl(D.C_ID, D.INTEGER) + " PRIMARY KEY AUTOINCREMENT, " +
                cddl(D.C_TITLE, D.TEXT) + " , " +
                cddl(D.C_MAIL, D.TEXT) + ");";
    }

    private String createTableTrustedPerson() {
        return "CREATE TABLE " + sq(D.T_TRUSTED_PERSON) + "(" +
                sq(D.C_ID) + D.INTEGER + " PRIMARY KEY AUTOINCREMENT, " +
                sq(D.C_MAIL) + D.TEXT + " , " +
                sq(D.C_PHONE) + D.TEXT + " , " +
                sq(D.C_NAME) + D.TEXT + ");";
    }

    private String createTableReport() {
        return "CREATE TABLE " + sq(D.T_REPORT) + "(" +
                sq(D.C_ID) + D.INTEGER + " PRIMARY KEY AUTOINCREMENT, " +
                cddl(D.C_UID, D.TEXT) + ", " +
                sq(D.C_TITLE) + D.TEXT + " , " +
                sq(D.C_CONTENT) + D.TEXT + " , " +
                sq(D.C_LOCATION) + D.TEXT + " , " +
                sq(D.C_DATE) + D.INTEGER + " , " +
                sq(D.C_METADATA) + D.INTEGER + " , " +
                sq(D.C_ARCHIVED) + D.INTEGER + " , " +
                sq(D.C_PUBLIC) + D.INTEGER + " , " +
                sq(D.C_DRAFT) + D.INTEGER + ");";
    }

    private String createTableEvidence() {
        return "CREATE TABLE " + sq(D.T_EVIDENCE) + " (" +
                sq(D.C_ID) + D.INTEGER + " PRIMARY KEY AUTOINCREMENT, " +
                sq(D.C_REPORT_ID) + D.INTEGER + " , " +
                sq(D.C_NAME) + D.TEXT + " , " +
                sq(D.C_METADATA) + D.TEXT + " , " +
                sq(D.C_PATH) + D.TEXT + ");";
    }

    private String createTableRecipientList() {
        return "CREATE TABLE " + sq(D.T_RECIPIENT_LIST) + "(" +
                sq(D.C_ID) + D.INTEGER + " PRIMARY KEY AUTOINCREMENT, " +
                sq(D.C_TITLE) + D.TEXT + ");";
    }

    private String createTableRecipientRecipientList() {
        return "CREATE TABLE " + sq(D.T_RECIPIENT_RECIPIENT_LIST) + "(" +
                    sq(D.C_RECIPIENT_ID) + D.INTEGER + " , " +
                    sq(D.C_RECIPIENT_LIST_ID) + D.INTEGER + " , " +
                "PRIMARY KEY(" + sq(D.C_RECIPIENT_ID) + " , " + sq(D.C_RECIPIENT_LIST_ID) + ") , " +
                "FOREIGN KEY(" + sq(D.C_RECIPIENT_LIST_ID) + ") REFERENCES " +
                    sq(D.T_RECIPIENT_LIST) + "(" + sq(D.C_ID) + ") ON DELETE CASCADE , " +
                "FOREIGN KEY(" + sq(D.C_RECIPIENT_ID) + ") REFERENCES " +
                    sq(D.T_RECIPIENT) + "(" + sq(D.C_ID) + ") ON DELETE CASCADE);";
    }

    private String createTableReportRecipient() {
        return "CREATE TABLE " + sq(D.T_REPORT_RECIPIENT) + "(" +
                    sq(D.C_REPORT_ID) + D.INTEGER + " , " +
                    sq(D.C_RECIPIENT_ID) + D.INTEGER + " , " +
                "PRIMARY KEY(" + sq(D.C_REPORT_ID) + " , " + sq(D.C_RECIPIENT_ID) + "), " +
                "FOREIGN KEY(" + sq(D.C_REPORT_ID) + ") REFERENCES " +
                    sq(D.T_REPORT) + "(" + sq(D.C_ID) + ") ON DELETE CASCADE , " +
                "FOREIGN KEY(" + sq(D.C_RECIPIENT_ID) + ") REFERENCES " +
                    sq(D.T_RECIPIENT) + "(" + sq(D.C_ID) + ") ON DELETE CASCADE );";
    }

    private static String objQuote(String str) {
        return OBJ_QUOTE + str + OBJ_QUOTE;
    }

    private static String sq(String unQuotedText) {
        return " " +objQuote(unQuotedText) + " ";
    }

    private static String cddl(String columnName, String columnType) {
        return objQuote(columnName) + " " + columnType;
    }
}
