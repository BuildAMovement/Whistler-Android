package rs.readahead.washington.mobile.domain.entity;

import android.support.annotation.StringRes;

import rs.readahead.washington.mobile.R;


public enum ContactSettingMethod {
    OTHER(0, R.string.ra_contact_other, R.string.ra_contact_other_hint, true),
    SIGNAL(1, R.string.ra_contact_signal, R.string.ra_contact_signal_hint, true),
    WICKR(2, R.string.ra_contact_wickr, R.string.ra_contact_wickr_hint, true),
    WIRE(3, R.string.ra_contact_wire,  R.string.ra_contact_wire_hint, true),
    EMAIL(4, R.string.ra_contact_email, R.string.ra_contact_email_hint, true),
    WHATSAPP(5, R.string.ra_contact_whatsapp, R.string.ra_contact_whatsapp_hint, true),
    TELEGRAM(6, R.string.ra_contact_telegram, R.string.ra_contact_telegram_hint, true),
    FACEBOOK(7, R.string.ra_contact_facebook, R.string.ra_contact_facebook_hint, true),
    TWITTER(8, R.string.ra_contact_twitter, R.string.ra_contact_twitter_hint, true);

    private int id;
    private int nameResId;
    private int hintResId;
    private boolean active;


    ContactSettingMethod(int id, @StringRes int resId,  @StringRes int hintResId, boolean active) {
        this.id = id;
        this.nameResId = resId;
        this.hintResId = hintResId;
        this.active = active;
    }

    public int getId() {
        return id;
    }

    @StringRes
    public int getNameResId() {
        return nameResId;
    }

    @StringRes
    public int getHintResId() {
        return hintResId;
    }

    public static ContactSettingMethod getMethod(final int id) {
        for (ContactSettingMethod method: ContactSettingMethod.values()) {
            if (method.id == id && method.active) {
                return method;
            }
        }

        return ContactSettingMethod.OTHER;
    }
}
