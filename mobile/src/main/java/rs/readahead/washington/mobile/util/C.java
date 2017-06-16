package rs.readahead.washington.mobile.util;


public class C {
    public static final String FOLDER_NAME = ".org.buildamovement.whistler";
    public static final String FOLDER_IMAGES = "photo";
    public static final String FOLDER_VIDEOS = "video";
    public static final String FOLDER_AUDIO = "audio";
    public static final String FOLDER_TRAINING_MATERIALS = "training_materials";
    public static final String TRAINING_URL = "https://whistlerapp.org/static/training/training-room.zip";
    public static final String DOWNLOAD_RESULT = "download_result";
    public static final String TRAINING_RECEIVER = "training_receiver";
    public static final int MIN_PASS_LENGTH = 6;
    public static final String ZIP_NAME = "training.zip";
    public static final int REQUEST_CODE_ASK_PERMISSIONS = 123;
    public static final String GOOGLE_MAPS_TEST = "www.maps.google.com/maps?q=";

    /*DEFAULT TYPES FOR DATABASE*/
    public static final String integerType = "integer";
    public static final String textType = " text ";
    public static final String realType = " real ";
    public static final String blobType = " BLOB ";
    public static final String dateType = " DATE ";
    /*DATABASE*/
    public static final String DATABASE_NAME = "washington.db";
    public static final int DATABASE_VERSION = 1;
    public static final String COLUMN_ID = "column_id";
    /*DATABASE TABLES*/
    public static final String TABLE_RECIPIENTS = "table_recipients";
    public static final String TABLE_TRUSTED_PERSONS = "table_trusted_persons";
    public static final String TABLE_REPORTS = "table_reports";
    public static final String TABLE_EVIDENCES = "table_evidences";
    public static final String TABLE_RECIPIENT_LISTS = "table_recipient_lists";
    public static final String TABLE_REPORT_RECIPIENTS = "table_report_recipients";
    public static final String TABLE_LISTS = "table_lists";
    /*TABLE RECIPIENTS*/
    public static final String COLUMN_RECIPIENT_TITLE = "column_title";
    public static final String COLUMN_RECIPIENT_MAIL = "column_mail";
    /*TABLE LISTS*/
    public static final String COLUMN_LIST_TITLE = "column_list_title";
    /*TABLE REPORT LIST*/
    public static final String COLUMN_REPORT_ID = "column_report_id";
    /*TABLE RECIPIENTS LIST*/
    public static final String COLUMN_LIST_ID = "column_list_id";
    public static final String COLUMN_RECIPIENT_ID = "column_recipient_id";
    /*TABLE TRUSTED PERSONS*/
    public static final String COLUMN_TRUSTED_NAME = "column_trusted_name";
    public static final String COLUMN_TRUSTED_MAIL = "column_trusted_mail";
    public static final String COLUMN_TRUSTED_PHONE = "column_trusted_phone";
    /*TABLE REPORTS*/
    public static final String COLUMN_REPORTS_UID = "column_reports_uid";
    public static final String COLUMN_REPORTS_TITLE = "column_reports_title";
    public static final String COLUMN_REPORTS_CONTENT = "column_reports_content";
    public static final String COLUMN_REPORTS_LOCATION = "column_reports_location";
    public static final String COLUMN_REPORTS_DATE = "column_reports_date";
    public static final String COLUMN_REPORTS_METADATA = "column_reports_metadata";
    public static final String COLUMN_REPORTS_ARCHIVED = "column_reports_archives";
    public static final String COLUMN_REPORTS_DRAFTS = "column_reports_drafts";
    public static final String COLUMN_REPORTS_PUBLIC = "column_reports_public";
    /*TABLE EVIDENCES*/
    public static final String COLUMN_EVIDENCES_REPORT_ID = "column_evidences_report_id";
    public static final String COLUMN_EVIDENCES_NAME = "column_evidences_name";
    public static final String COLUMN_EVIDENCES_PATH = "column_evidences_path";
    public static final String COLUMN_EVIDENCE_METADATA = "column_evidence_metadata";

    public static boolean validatePassword(String pattern) {
        return pattern.length() >= C.MIN_PASS_LENGTH;
    }
}
