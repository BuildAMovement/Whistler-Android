package rs.readahead.washington.mobile.domain.repository;

import io.reactivex.Completable;
import io.reactivex.Single;
import rs.readahead.washington.mobile.domain.entity.ContactSetting;


public interface ISettingsRepository {
    Single<ContactSetting> getContactSetting();
    Completable saveContactSetting(ContactSetting contactSetting);
    Completable removeContactSetting();
}
