package rs.readahead.washington.mobile.data.database;


class D {
    /* DEFAULT TYPES FOR DATABASE */
    static final String INTEGER = " INTEGER ";
    static final String TEXT = " TEXT ";
    //static final String REAL = " REAL ";
    static final String BLOB = " BLOB ";
    //static final String DATE = " DATE ";

    /* DATABASE */
    static final String DATABASE_NAME = "whistler.db";
    // 1=start, 2=collect init, 3=t_report.c_contact_information, 4=odk_start, 5=media_file, 6=report_media_file
    static final int DATABASE_VERSION = 6;

    /* DATABASE TABLES */
    static final String T_RECIPIENT = "t_recipient";
    static final String T_TRUSTED_PERSON = "t_trusted_person";
    static final String T_REPORT = "t_report";
    static final String T_EVIDENCE = "t_evidence";
    static final String T_RECIPIENT_RECIPIENT_LIST = "t_recipient_recipient_list";
    static final String T_REPORT_RECIPIENT = "t_report_recipient";
    static final String T_RECIPIENT_LIST = "t_recipient_list";
    static final String T_COLLECT_SERVER = "t_collect_server";
    static final String T_COLLECT_BLANK_FORM = "t_collect_blank_xform";
    static final String T_COLLECT_FORM_INSTANCE = "t_collect_xform_instance";
    static final String T_MEDIA_FILE = "t_media_file";
    static final String T_COLLECT_FORM_INSTANCE_MEDIA_FILE = "t_collect_xform_instance_media_file";
    static final String T_REPORT_MEDIA_FILE = "t_report_media_file";
    static final String T_TRAIN_MODULE = "t_train_module";
    static final String T_SETTINGS = "t_settings";

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
    static final String C_SAVED = "c_saved";
    static final String C_PUBLIC = "c_public";
    static final String C_PATH = "c_path";
    static final String C_URL = "c_url";
    static final String C_USERNAME = "c_username";
    static final String C_PASSWORD = "c_password";
    static final String C_CONTACT_INFORMATION_ENABLED = "c_contact_information_enabled";
    static final String C_VERSION = "c_version";
    static final String C_HASH = "c_hash";
    //static final String C_DESCRIPTION_TEXT = "c_description_text";
    static final String C_DOWNLOAD_URL = "c_download_url";
    //static final String C_MANIFEST_URL = "c_manifest_url";
    static final String C_FORM_ID = "c_form_id";
    static final String C_COLLECT_SERVER_ID = "c_collect_server_id";
    static final String C_FORM_DEF = "c_form_def";
    static final String C_FORM_NAME = "c_form_name";
    static final String C_INSTANCE_NAME = "c_instance_name";
    static final String C_STATUS = "c_status";
    static final String C_UPDATED = "c_updated";
    static final String C_DOWNLOADED = "c_downloaded";
    static final String C_FAVORITE = "c_favorite";
    static final String C_THUMBNAIL = "c_thumbnail";
    static final String C_FILE_NAME = "c_file_name";
    static final String C_MEDIA_FILE_ID = "c_media_file_id";
    static final String C_CREATED = "c_created";
    static final String C_COLLECT_FORM_INSTANCE_ID = "c_collect_form_instance_id";
    static final String C_DURATION = "c_duration";
    static final String C_ANONYMOUS = "c_anonymous";
    static final String C_TRAIN_MODULE_ID = "c_train_module_id";
    static final String C_ORGANIZATION = "c_organization";
    static final String C_TYPE = "c_type";
    static final String C_SIZE = "c_size";
    static final String C_INT_VALUE = "c_int_value";
    static final String C_TEXT_VALUE = "c_text_value";

    static final String A_SERVER_NAME = "a_server_name";
    static final String A_COLLECT_BLANK_FORM_ID = "a_collect_blank_xform_id";
    static final String A_COLLECT_FORM_INSTANCE_ID = "a_collect_form_instance_id";
    static final String A_MEDIA_FILE_ID = "a_media_file_id";
    static final String A_SERVER_USERNAME = "a_server_username";
}
