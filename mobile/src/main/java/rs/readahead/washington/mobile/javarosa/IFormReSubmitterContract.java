package rs.readahead.washington.mobile.javarosa;

import android.content.Context;

import rs.readahead.washington.mobile.domain.entity.collect.CollectFormInstance;
import rs.readahead.washington.mobile.domain.entity.collect.OpenRosaResponse;
import rs.readahead.washington.mobile.mvp.contract.IBasePresenter;


public interface IFormReSubmitterContract {
    interface IView {
        void formReSubmitError(Throwable error);
        void formReSubmitNoConnectivity();
        void formReSubmitSuccess(CollectFormInstance instance, OpenRosaResponse response);
        void showReFormSubmitLoading();
        void hideReFormSubmitLoading();
        Context getContext();
    }

    interface IFormReSubmitter extends IBasePresenter {
        void reSubmitFormInstance(CollectFormInstance instance);
    }
}
