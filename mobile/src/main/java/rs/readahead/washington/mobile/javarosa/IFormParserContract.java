package rs.readahead.washington.mobile.javarosa;

import android.content.Context;

import org.javarosa.form.api.FormEntryCaption;
import org.javarosa.form.api.FormEntryPrompt;

import java.util.List;

import rs.readahead.washington.mobile.domain.entity.MediaFile;
import rs.readahead.washington.mobile.mvp.contract.IBasePresenter;


public interface IFormParserContract {
    interface IView {
        void formBeginning(String title);
        void formEnd(String title);
        void formQuestion(FormEntryPrompt[] prompts, FormEntryCaption[] groups);
        void formGroup(FormEntryPrompt[] prompts, FormEntryCaption[] groups);
        void formRepeat(FormEntryPrompt[] prompts, FormEntryCaption[] groups);
        void formPromptNewRepeat();
        void formParseError(Throwable error);
        void formPropertiesChecked(boolean enableAttachments, boolean enableDelete);
        Context getContext();
    }

    interface IFormParser extends IBasePresenter {
        void parseForm();
        void stepToNextScreen();
        void stepToPrevScreen();
        boolean isFirstScreen();
        boolean isFormChanged();
        void startFormChangeTracking();
        List<MediaFile> getFormAttachments();
    }
}
