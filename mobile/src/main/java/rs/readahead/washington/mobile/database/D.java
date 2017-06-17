package rs.readahead.washington.mobile.database;


class D {
    /* DEFAULT TYPES FOR DATABASE */
    static final String INTEGER = " INTEGER ";
    static final String TEXT = " TEXT ";
    //static final String REAL = " REAL ";
    //static final String BLOB = " BLOB ";
    //static final String DATE = " DATE ";

    /* DATABASE */
    static final String DATABASE_NAME = "whistler.db";
    static final int DATABASE_VERSION = 1;

    /* DATABASE TABLES */
    static final String T_RECIPIENT = "t_recipient";
    static final String T_TRUSTED_PERSON = "t_trusted_person";
    static final String T_REPORT = "t_report";
    static final String T_EVIDENCE = "t_evidence";
    static final String T_RECIPIENT_RECIPIENT_LIST = "t_recipient_recipient_list";
    static final String T_REPORT_RECIPIENT = "t_report_recipient";
    static final String T_RECIPIENT_LIST = "t_recipient_list";

    /* DATABASE COLUMNS */
    static final String C_ID = "c_id";
    static final String C_TITLE = "c_title";
    static final String C_MAIL = "c_mail";
    static final String C_REPORT_ID = "c_report_id";
    static final String C_RECIPIENT_LIST_ID = "c_recipient_list_id";
    static final String C_RECIPIENT_ID = "c_recipient_id";
    static final String C_NAME = "c_name";
    static final String C_PHONE = "c_phone";
    static final String C_UID = "c_uid";
    static final String C_CONTENT = "c_content";
    static final String C_LOCATION = "c_location";
    static final String C_DATE = "c_date";
    static final String C_METADATA = "c_metadata";
    static final String C_ARCHIVED = "c_archived";
    static final String C_DRAFT = "c_draft";
    static final String C_PUBLIC = "c_public";
    static final String C_PATH = "c_path";
}
