package rs.readahead.washington.mobile.mvp.contract;

import android.content.Context;

import rs.readahead.washington.mobile.domain.entity.ContactSetting;


public class IContactSettingsPresenterContract {
    public interface IView {
        void onSavedContactSettings();
        void onSaveContactSettingsError(Throwable throwable);
        void onLoadedContactSettings(ContactSetting setting);
        void onLoadContactSettingsError(Throwable throwable);
        Context getContext();
    }

    public interface IPresenter extends IBasePresenter {
        void updateContactSettings(ContactSetting setting);
        void loadContactSettings();
    }
}
