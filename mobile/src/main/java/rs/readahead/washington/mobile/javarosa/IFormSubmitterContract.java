package rs.readahead.washington.mobile.javarosa;

import android.content.Context;

import rs.readahead.washington.mobile.domain.entity.collect.CollectFormInstance;
import rs.readahead.washington.mobile.domain.entity.collect.OpenRosaResponse;
import rs.readahead.washington.mobile.mvp.contract.IBasePresenter;


public interface IFormSubmitterContract {
    interface IView {
        void formSubmitError(Throwable error);
        void formSubmitNoConnectivity();
        void formSubmitSuccess(CollectFormInstance instance, OpenRosaResponse response);
        void showFormSubmitLoading();
        void hideFormSubmitLoading();
        Context getContext();
    }

    interface IFormSubmitter extends IBasePresenter {
        void submitActiveFormInstance(String name);
        void submitFormInstance(CollectFormInstance instance);
    }
}
