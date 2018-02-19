package rs.readahead.washington.mobile.mvp.contract;

import java.util.List;

import rs.readahead.washington.mobile.domain.entity.ContactSettingMethod;


public class IContactMethodPresenterContract {
    public interface IView {
    }

    public interface IPresenter extends IBasePresenter {
        List<ContactSettingMethod> getRecommendedMethods();
        List<ContactSettingMethod> getOtherMethods();
    }
}
