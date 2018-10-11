package rs.readahead.washington.mobile.util;


public class C {
    public static final String APPSPOT_REFLECTOR = "whistler-191516.appspot.com";
    public static final int REQUEST_CODE_ASK_PERMISSIONS = 123;
    public static final String GOOGLE_MAPS_TEST = "www.maps.google.com/maps?q=";
    public static final String MEDIA_DIR = "media";
    public static final String TMP_DIR = "tmp";
    public static final String TRAIN_DIR = "train";
    public static final String TRAIN_PACKAGES_DIR = TRAIN_DIR + "/packages"; // here we place <module.id>.zip on download
    public static final String TRAIN_MODULES_DIR = TRAIN_DIR + "/modules"; // here we unpack modules /<module.id>/

    // onActivityResult requestCode
    public static final int PICKED_IMAGE                = 10001;
    public static final int CAPTURED_IMAGE              = 10002;
    public static final int CAPTURED_VIDEO              = 10003;
    public static final int RECORDED_AUDIO              = 10004;
    public static final int IMPORT_IMAGE                = 10009;
    public static final int IMPORT_VIDEO                = 10010;
    public static final int IMPORT_MEDIA                = 10011;
    public static final int CAMERA_CAPTURE              = 10012;
    public static final int START_CAMERA_CAPTURE        = 10013; // return from location settings handling
    public static final int START_AUDIO_RECORD          = 10014; // return from location settings handling
    public static final int RECIPIENT_IDS               = 10015;
    public static final int EVIDENCE_IDS                = 10016;

    // "global" intent keys
    public static final String CAPTURED_MEDIA_FILE_ID = "cmfi";
    public static final String SMS_SENT = "SMS_SENT";
    public static final String SMS_DELIVERED = "SMS_DELIVERED";

    // notification ids
    public static final int TRAIN_DOWNLOADER_NOTIFICATION_ID = 10001;
}
