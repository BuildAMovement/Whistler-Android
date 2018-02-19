package rs.readahead.washington.mobile.domain.entity;

import android.content.Context;
import android.support.annotation.Nullable;

import java.util.Locale;


public class ContactSetting {
    public static final ContactSetting NONE = new ContactSetting();

    private ContactSettingMethod method;
    private String address;


    public ContactSetting(ContactSettingMethod method, String address) {
        this.method = method;
        this.address = address;
    }

    public ContactSettingMethod getMethod() {
        return method;
    }

    public String getAddress() {
        return address;
    }

    public void setMethod(ContactSettingMethod method) {
        this.method = method;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    @Nullable
    public String getContactString(Context context) {
        if (method != null && address != null) {
            return String.format(Locale.ROOT, "%s: %s", context.getString(method.getNameResId()), address);
        }

        return null;
    }

    private ContactSetting() {
    }
}
