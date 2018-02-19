package rs.readahead.washington.mobile.mvp.presenter;

import java.util.Arrays;
import java.util.List;

import io.reactivex.disposables.CompositeDisposable;
import rs.readahead.washington.mobile.domain.entity.ContactSettingMethod;
import rs.readahead.washington.mobile.mvp.contract.IContactMethodPresenterContract;


public class ContactMethodPresenter implements IContactMethodPresenterContract.IPresenter {
    private IContactMethodPresenterContract.IView view;


    public ContactMethodPresenter(IContactMethodPresenterContract.IView view) {
        this.view = view;
    }

    @Override
    public List<ContactSettingMethod> getRecommendedMethods() {
        return recommended;
    }

    @Override
    public List<ContactSettingMethod> getOtherMethods() {
        return other;
    }

    @Override
    public void destroy() {
        view = null;
    }

    private static List<ContactSettingMethod> recommended = Arrays.asList(
            ContactSettingMethod.SIGNAL,
            ContactSettingMethod.WICKR,
            ContactSettingMethod.WIRE
    );

    private static List<ContactSettingMethod> other = Arrays.asList(
            ContactSettingMethod.EMAIL,
            ContactSettingMethod.WHATSAPP,
            ContactSettingMethod.TELEGRAM,
            ContactSettingMethod.FACEBOOK,
            ContactSettingMethod.TWITTER,
            ContactSettingMethod.OTHER
    );
}
