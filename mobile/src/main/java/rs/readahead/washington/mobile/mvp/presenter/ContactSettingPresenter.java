package rs.readahead.washington.mobile.mvp.presenter;

import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.crashlytics.android.Crashlytics;

import io.reactivex.CompletableSource;
import io.reactivex.SingleSource;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.functions.Action;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;
import rs.readahead.washington.mobile.data.database.CacheWordDataSource;
import rs.readahead.washington.mobile.data.database.DataSource;
import rs.readahead.washington.mobile.domain.entity.ContactSetting;
import rs.readahead.washington.mobile.domain.entity.ContactSettingMethod;
import rs.readahead.washington.mobile.mvp.contract.IContactSettingsPresenterContract;


public class ContactSettingPresenter implements IContactSettingsPresenterContract.IPresenter {
    private IContactSettingsPresenterContract.IView view;
    private CompositeDisposable disposables = new CompositeDisposable();
    private CacheWordDataSource cacheWordDataSource;


    public ContactSettingPresenter(IContactSettingsPresenterContract.IView view) {
        this.view = view;
        this.cacheWordDataSource = new CacheWordDataSource(view.getContext());
    }

    @Override
    public void updateContactSettings(final ContactSetting setting) {
        disposables.add(cacheWordDataSource.getDataSource()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .flatMapCompletable(new Function<DataSource, CompletableSource>() {
                    @Override
                    public CompletableSource apply(DataSource dataSource) throws Exception {
                        if (TextUtils.isEmpty(setting.getAddress()) || setting.getMethod() == null) {
                            return dataSource.removeContactSetting();
                        } else {
                            return dataSource.saveContactSetting(setting);
                        }
                    }
                })
                .subscribe(new Action() {
                    @Override
                    public void run() throws Exception {
                        view.onSavedContactSettings();
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) throws Exception {
                        Crashlytics.logException(throwable);
                        view.onSaveContactSettingsError(throwable);
                    }
                })
        );
    }

    @Override
    public void loadContactSettings() {
        disposables.add(cacheWordDataSource.getDataSource()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .flatMapSingle(new Function<DataSource, SingleSource<ContactSetting>>() {
                    @Override
                    public SingleSource<ContactSetting> apply(@NonNull DataSource dataSource) throws Exception {
                        return dataSource.getContactSetting();
                    }
                })
                .subscribe(new Consumer<ContactSetting>() {
                    @Override
                    public void accept(ContactSetting setting) throws Exception {
                        if (ContactSetting.NONE.equals(setting)) {
                            setting = new ContactSetting(ContactSettingMethod.OTHER, "");
                        }

                        view.onLoadedContactSettings(setting);
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) throws Exception {
                        Crashlytics.logException(throwable);
                        view.onLoadContactSettingsError(throwable);
                    }
                })
        );
    }

    @Override
    public void destroy() {
        disposables.dispose();
        cacheWordDataSource.dispose();
        view = null;
    }
}
