package rs.readahead.washington.mobile.data.database;

import android.content.Context;

import net.sqlcipher.database.SQLiteDatabase;


class WashingtonSQLiteOpenHelper extends CipherOpenHelper {
    private static final String OBJ_QUOTE = "`";


    WashingtonSQLiteOpenHelper(Context context) {
        super(context, D.DATABASE_NAME, null, D.DATABASE_VERSION);
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
        // we have started from version 6

        // DBv6
        db.execSQL(createTableRecipient());
        db.execSQL(createTableTrustedPerson());
        db.execSQL(createTableReport());
        db.execSQL(createTableEvidence());
        db.execSQL(createTableRecipientRecipientList());
        db.execSQL(createTableRecipientList());
        db.execSQL(createTableReportRecipient());
        db.execSQL(createTableCollectServer());
        db.execSQL(createTableMediaFile());
        db.execSQL(createTableCollectBlankForm());
        db.execSQL(createTableCollectFormInstance());
        db.execSQL(createTableCollectFormInstanceMediaFile());
        db.execSQL(createTableTrainModule());
        db.execSQL(createTableReportMediaFile());
        db.execSQL(createTableSettings());
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // we have started from version 6, no upgrade procedures from before
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
                sq(D.C_CONTACT_INFORMATION_ENABLED) + D.INTEGER + " , " +
                sq(D.C_DATE) + D.INTEGER + " , " +
                sq(D.C_METADATA) + D.INTEGER + " , " +
                cddl(D.C_SAVED, D.INTEGER, true) + " , " +
                sq(D.C_PUBLIC) + D.INTEGER + ");";
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

    private String createTableCollectServer() {
        return "CREATE TABLE " + sq(D.T_COLLECT_SERVER) + " (" +
                cddl(D.C_ID, D.INTEGER) + " PRIMARY KEY AUTOINCREMENT, " +
                cddl(D.C_NAME, D.TEXT) + " , " +
                cddl(D.C_URL, D.TEXT) + " , " +
                cddl(D.C_USERNAME, D.TEXT) + " , " +
                cddl(D.C_PASSWORD, D.TEXT) + ");";
    }

    private String createTableMediaFile() {
        return "CREATE TABLE " + sq(D.T_MEDIA_FILE) + " (" +
                cddl(D.C_ID, D.INTEGER) + " PRIMARY KEY AUTOINCREMENT, " +
                cddl(D.C_PATH, D.TEXT, true) + " , " +
                cddl(D.C_UID, D.TEXT, true) + " , " +
                cddl(D.C_FILE_NAME, D.TEXT, true) + " , " +
                cddl(D.C_METADATA, D.TEXT) +" , " +
                cddl(D.C_THUMBNAIL, D.BLOB) +" , " +
                cddl(D.C_CREATED, D.INTEGER) +" , " +
                cddl(D.C_DURATION, D.INTEGER) +" , " +
                cddl(D.C_ANONYMOUS, D.INTEGER) +" , " +
                "UNIQUE(" + sq(D.C_PATH) + ", " + sq(D.C_FILE_NAME) + ")" +
                ");";
    }

    private String createTableCollectBlankForm() {
        return "CREATE TABLE " + sq(D.T_COLLECT_BLANK_FORM) + " (" +
                cddl(D.C_ID, D.INTEGER) + " PRIMARY KEY AUTOINCREMENT, " +
                cddl(D.C_COLLECT_SERVER_ID, D.INTEGER, true) + " , " +
                cddl(D.C_FORM_ID, D.TEXT, true) + " , " +
                cddl(D.C_VERSION, D.TEXT, true) + " , " +
                cddl(D.C_HASH, D.TEXT, true) + " , " +
                cddl(D.C_NAME, D.TEXT, true) + " , " +
                cddl(D.C_DOWNLOAD_URL, D.TEXT) +" , " +
                cddl(D.C_FORM_DEF, D.BLOB) +" , " +
                cddl(D.C_DOWNLOADED, D.INTEGER, true) + " DEFAULT 0 , " +
                cddl(D.C_FAVORITE, D.INTEGER, true) + " DEFAULT 0 , " +
                "FOREIGN KEY(" + sq(D.C_COLLECT_SERVER_ID) + ") REFERENCES " +
                    sq(D.T_COLLECT_SERVER) + "(" + sq(D.C_ID) + ") ON DELETE CASCADE, " +
                "UNIQUE(" + sq(D.C_FORM_ID) + ") ON CONFLICT REPLACE" +
                ");";
    }

    private String createTableCollectFormInstance() {
        return "CREATE TABLE " + sq(D.T_COLLECT_FORM_INSTANCE) + " (" +
                cddl(D.C_ID, D.INTEGER) + " PRIMARY KEY AUTOINCREMENT, " +
                cddl(D.C_COLLECT_SERVER_ID, D.INTEGER, true) + " , " +
                cddl(D.C_STATUS, D.INTEGER, true) + " , " +
                cddl(D.C_UPDATED, D.INTEGER, true) + " , " +
                cddl(D.C_FORM_ID, D.TEXT, true) + " , " +
                cddl(D.C_VERSION, D.TEXT, true) + " , " +
                cddl(D.C_FORM_NAME, D.TEXT, true) + " , " +
                cddl(D.C_INSTANCE_NAME, D.TEXT, true) + " , " +
                cddl(D.C_FORM_DEF, D.BLOB) +" , " +
                "FOREIGN KEY(" + sq(D.C_COLLECT_SERVER_ID) + ") REFERENCES " +
                    sq(D.T_COLLECT_SERVER) + "(" + sq(D.C_ID) + ") ON DELETE RESTRICT" +
                ");";
    }

    private String createTableCollectFormInstanceMediaFile() {
        return "CREATE TABLE " + sq(D.T_COLLECT_FORM_INSTANCE_MEDIA_FILE) + " (" +
                cddl(D.C_ID, D.INTEGER) + " PRIMARY KEY AUTOINCREMENT, " +
                cddl(D.C_COLLECT_FORM_INSTANCE_ID, D.INTEGER, true) + " , " +
                cddl(D.C_MEDIA_FILE_ID, D.INTEGER, true) + " , " +
                "FOREIGN KEY(" + sq(D.C_COLLECT_FORM_INSTANCE_ID) + ") REFERENCES " +
                    sq(D.T_COLLECT_FORM_INSTANCE) + "(" + sq(D.C_ID) + ") ON DELETE CASCADE," +
                "FOREIGN KEY(" + sq(D.C_MEDIA_FILE_ID) + ") REFERENCES " +
                    sq(D.T_MEDIA_FILE) + "(" + sq(D.C_ID) + ") ON DELETE CASCADE," +
                "UNIQUE(" + sq(D.C_COLLECT_FORM_INSTANCE_ID) + ", " + sq(D.C_MEDIA_FILE_ID) + ") ON CONFLICT IGNORE" +
                ");";
    }


    private String createTableReportMediaFile() {
        return "CREATE TABLE " + sq(D.T_REPORT_MEDIA_FILE) + " (" +
                cddl(D.C_ID, D.INTEGER) + " PRIMARY KEY AUTOINCREMENT, " +
                cddl(D.C_REPORT_ID, D.INTEGER, true) + " , " +
                cddl(D.C_MEDIA_FILE_ID, D.INTEGER, true) + " , " +
                "FOREIGN KEY(" + sq(D.C_REPORT_ID) + ") REFERENCES " +
                    sq(D.T_REPORT) + "(" + sq(D.C_ID) + ") ON DELETE CASCADE," +
                "FOREIGN KEY(" + sq(D.C_MEDIA_FILE_ID) + ") REFERENCES " +
                    sq(D.T_MEDIA_FILE) + "(" + sq(D.C_ID) + ") ON DELETE CASCADE," +
                "UNIQUE(" + sq(D.C_REPORT_ID) + ", " + sq(D.C_MEDIA_FILE_ID) + ") ON CONFLICT IGNORE" +
                ");";
    }

    private String createTableTrainModule() {
        return "CREATE TABLE " + sq(D.T_TRAIN_MODULE) + "(" +
                cddl(D.C_TRAIN_MODULE_ID, D.INTEGER) + " PRIMARY KEY AUTOINCREMENT, " +
                cddl(D.C_NAME, D.TEXT, true) + " , " +
                cddl(D.C_URL, D.TEXT, true) + " , " +
                cddl(D.C_DOWNLOADED, D.INTEGER, true) + " , " +
                cddl(D.C_ORGANIZATION, D.TEXT) + " , " +
                cddl(D.C_TYPE, D.TEXT) + " , " +
                cddl(D.C_SIZE, D.INTEGER, true) + " );";
    }

    private String createTableSettings() {
        return "CREATE TABLE " + sq(D.T_SETTINGS) + "(" +
                cddl(D.C_NAME, D.TEXT) + " PRIMARY KEY, " +
                cddl(D.C_INT_VALUE, D.INTEGER) + " , " +
                cddl(D.C_TEXT_VALUE, D.TEXT) +
                " );";
    }

    private static String objQuote(String str) {
        return OBJ_QUOTE + str + OBJ_QUOTE;
    }

    private static String sq(String unQuotedText) {
        return " " + objQuote(unQuotedText) + " ";
    }

    private static String cddl(String columnName, String columnType) {
        return objQuote(columnName) + " " + columnType;
    }

    private static String cddl(String columnName, String columnType, boolean notNull) {
        return objQuote(columnName) + " " + columnType + (notNull ? " NOT NULL" : "");
    }
}
