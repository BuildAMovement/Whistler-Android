package rs.readahead.washington.mobile.javarosa;

import android.content.Context;

import org.javarosa.core.model.FormIndex;
import org.javarosa.core.model.data.IAnswerData;

import java.util.LinkedHashMap;

import rs.readahead.washington.mobile.domain.entity.collect.CollectFormInstance;
import rs.readahead.washington.mobile.mvp.contract.IBasePresenter;


public interface IFormSaverContract {
    interface IView {
        void formSaveError(Throwable error);
        void showSaveFormInstanceLoading();
        void hideSaveFormInstanceLoading();
        void showDeleteFormInstanceStart();
        void hideDeleteFormInstanceEnd();
        void formInstanceSaveError(Throwable throwable);
        void formInstanceSaveSuccess(CollectFormInstance instance);
        void formInstanceDeleteSuccess(boolean cloned);
        void formInstanceDeleteError(Throwable throwable);
        void formInstanceAutoSaveSuccess(CollectFormInstance instance);
        void formConstraintViolation(FormIndex formIndex, String errorString);
        Context getContext();
    }

    interface IFormSaver extends IBasePresenter {
        boolean saveScreenAnswers(LinkedHashMap<FormIndex, IAnswerData> answers, boolean checkConstraints);
        void saveActiveFormInstance();
        void autoSaveFormInstance();
        boolean isAutoSaveDraft();
        void deleteActiveFormInstance();
        boolean isActiveInstanceCloned();
    }
}
