package rs.readahead.washington.mobile.database;

import android.content.Context;

import net.sqlcipher.database.SQLiteDatabase;

import info.guardianproject.cacheword.CacheWordHandler;
import rs.readahead.washington.mobile.util.C;
import rs.readahead.washington.mobile.util.SqlHelper;


class WashingtonSQLiteOpenHelper extends CipherOpenHelper {

    WashingtonSQLiteOpenHelper(CacheWordHandler cacheWord, Context context) {
        super(cacheWord, context, C.DATABASE_NAME, null, C.DATABASE_VERSION);
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
        db.execSQL(createTableRecipients());
        db.execSQL(createTableTrustedPersons());
        db.execSQL(createTableReports());
        db.execSQL(createTableEvidences());
        db.execSQL(createTableRecipientLists());
        db.execSQL(createTableLists());
        db.execSQL(createTableReportRecipients());
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    }

    private String createTableRecipients() {
        return "CREATE TABLE " + SqlHelper.objQuote(C.TABLE_RECIPIENTS) + " (" +
                SqlHelper.columnDdl(C.COLUMN_ID, C.integerType) + " PRIMARY KEY AUTOINCREMENT, " +
                SqlHelper.columnDdl(C.COLUMN_RECIPIENT_TITLE, C.textType) + " , " +
                SqlHelper.columnDdl(C.COLUMN_RECIPIENT_MAIL, C.textType) + ");";
    }

    private String createTableTrustedPersons() {
        return "CREATE TABLE " + sq(C.TABLE_TRUSTED_PERSONS) + "(" +
                sq(C.COLUMN_ID) + C.integerType + " PRIMARY KEY AUTOINCREMENT, " +
                sq(C.COLUMN_TRUSTED_MAIL) + C.textType + " , " +
                sq(C.COLUMN_TRUSTED_PHONE) + C.textType + " , " +
                sq(C.COLUMN_TRUSTED_NAME) + C.textType + ");";
    }

    private String createTableReports() {
        return "CREATE TABLE " + sq(C.TABLE_REPORTS) + "(" +
                sq(C.COLUMN_ID) + C.integerType + " PRIMARY KEY AUTOINCREMENT, " +
                SqlHelper.columnDdl(C.COLUMN_REPORTS_UID, C.textType) + ", " +
                sq(C.COLUMN_REPORTS_TITLE) + C.textType + " , " +
                sq(C.COLUMN_REPORTS_CONTENT) + C.textType + " , " +
                sq(C.COLUMN_REPORTS_LOCATION) + C.textType + " , " +
                sq(C.COLUMN_REPORTS_DATE) + C.integerType + " , " +
                sq(C.COLUMN_REPORTS_METADATA) + C.integerType + " , " +
                sq(C.COLUMN_REPORTS_ARCHIVED) + C.integerType + " , " +
                sq(C.COLUMN_REPORTS_PUBLIC) + C.integerType + " , " +
                sq(C.COLUMN_REPORTS_DRAFTS) + C.integerType + ");";
    }

    private String createTableEvidences() {
        return "CREATE TABLE " + sq(C.TABLE_EVIDENCES) + " (" +
                sq(C.COLUMN_ID) + C.integerType + " PRIMARY KEY AUTOINCREMENT, " +
                sq(C.COLUMN_EVIDENCES_REPORT_ID) + C.integerType + " , " +
                sq(C.COLUMN_EVIDENCES_NAME) + C.textType + " , " +
                sq(C.COLUMN_EVIDENCE_METADATA) + C.textType + " , " +
                sq(C.COLUMN_EVIDENCES_PATH) + C.textType + ");";

    }

    private String createTableLists() {
        return "CREATE TABLE " + sq(C.TABLE_LISTS) + "(" +
                sq(C.COLUMN_ID) + C.integerType + " PRIMARY KEY AUTOINCREMENT, " +
                sq(C.COLUMN_LIST_TITLE) + C.textType + ");";
    }

    private String createTableRecipientLists() {
        return "CREATE TABLE " + sq(C.TABLE_RECIPIENT_LISTS) + "(" +
                    sq(C.COLUMN_LIST_ID) + C.integerType + " , " +
                    sq(C.COLUMN_RECIPIENT_ID) + C.integerType + " , " +
                "PRIMARY KEY(" + sq(C.COLUMN_LIST_ID) + " , " + sq(C.COLUMN_RECIPIENT_ID) + ") , " +
                "FOREIGN KEY(" + sq(C.COLUMN_LIST_ID) + ") REFERENCES " +
                    sq(C.TABLE_LISTS) + "(" + sq(C.COLUMN_ID) + ") ON DELETE CASCADE , " +
                "FOREIGN KEY(" + sq(C.COLUMN_RECIPIENT_ID) + ") REFERENCES " +
                    sq(C.TABLE_RECIPIENTS) + "(" + sq(C.COLUMN_ID) + ") ON DELETE CASCADE );";
    }

    private String createTableReportRecipients() {
        return "CREATE TABLE " + sq(C.TABLE_REPORT_RECIPIENTS) + "(" +
                    sq(C.COLUMN_REPORT_ID) + C.integerType + " , " +
                    sq(C.COLUMN_RECIPIENT_ID) + C.integerType + " , " +
                "PRIMARY KEY(" + sq(C.COLUMN_REPORT_ID) + " , " + sq(C.COLUMN_RECIPIENT_ID) + "), " +
                "FOREIGN KEY(" + sq(C.COLUMN_REPORT_ID) + ") REFERENCES " +
                    sq(C.TABLE_REPORTS) + "(" + sq(C.COLUMN_ID) + ") ON DELETE CASCADE , " +
                "FOREIGN KEY(" + sq(C.COLUMN_RECIPIENT_ID) + ") REFERENCES " +
                    sq(C.TABLE_RECIPIENTS) + "(" + sq(C.COLUMN_ID) + ") ON DELETE CASCADE );";
    }

    private static String sq(String unQuotedText) {
        return " `" + unQuotedText + "` ";
    }
}
