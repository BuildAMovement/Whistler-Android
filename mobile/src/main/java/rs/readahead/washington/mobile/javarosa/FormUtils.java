package rs.readahead.washington.mobile.javarosa;

import android.content.Context;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import org.javarosa.core.model.FormDef;
import org.javarosa.core.model.FormIndex;
import org.javarosa.core.model.instance.FormInstance;
import org.javarosa.form.api.FormEntryController;
import org.javarosa.form.api.FormEntryModel;
import org.javarosa.form.api.FormEntryPrompt;
import org.javarosa.model.xform.XFormSerializingVisitor;

import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import rs.readahead.washington.mobile.R;
import rs.readahead.washington.mobile.domain.entity.IErrorBundle;
import rs.readahead.washington.mobile.domain.entity.IErrorCode;
import rs.readahead.washington.mobile.domain.entity.collect.OpenRosaResponse;
import rs.readahead.washington.mobile.util.StringUtils;


public class FormUtils {
    @Nullable
    static String getFormValuesHash(FormDef formDef) {
        FormInstance formInstance = formDef.getInstance();
        XFormSerializingVisitor serializer = new XFormSerializingVisitor();
        try {
            byte[] payload = serializer.serializeInstance(formInstance);

            MessageDigest md = java.security.MessageDigest.getInstance("MD5");
            md.update(payload);

            return StringUtils.hexString(md.digest());
        } catch (Exception ignored) {
        }

        return null;
    }

    @Nullable
    static FormIndex findWhistlerAttachmentFieldIndex(Context context, FormDef formDef) {
        FormEntryModel formEntryModel = new FormEntryModel(formDef);
        FormEntryController formEntryController = new FormEntryController(formEntryModel);

        String attachmentFieldName = context.getString(R.string.ra_config_form_attachment_field);
        FormIndex index = null;
        int event;

        formEntryController.jumpToIndex(FormIndex.createBeginningOfFormIndex());

        while ((event = formEntryController.stepToNextEvent()) != FormEntryController.EVENT_END_OF_FORM) {
            if (event == FormEntryController.EVENT_QUESTION) {
                FormIndex questionFormIndex = formEntryController.getModel().getFormIndex();
                String fieldName = questionFormIndex.getReference().getNameLast();

                if (attachmentFieldName.equals(fieldName)) {
                    index = questionFormIndex;
                    break;
                }
            }
        }

        //fec.jumpToIndex(FormIndex.createBeginningOfFormIndex());
        return index;
    }

    public static boolean isWhistlerAttachmentField(Context context, FormEntryPrompt fep) {
        String fieldName = fep.getIndex().getReference().getNameLast();
        return context.getString(R.string.ra_config_form_attachment_field).equals(fieldName);
    }


    public static String getFormSubmitSuccessMessage(Context context, OpenRosaResponse response) {
        List<String> texts = new ArrayList<>();
        for (OpenRosaResponse.Message msg: response.getMessages()) {
            if (! TextUtils.isEmpty(msg.getText())) {
                texts.add(msg.getText());
            }
        }

        String messages, successMessage;
        boolean hasMessages = texts.size() > 0;
        messages = hasMessages ? TextUtils.join("; ", texts) : "";

        switch(response.getStatusCode()) {
            case OpenRosaResponse.StatusCode.FORM_RECEIVED:
                if (hasMessages) {
                    successMessage = context.getString(R.string.ra_form_received_reply) + messages;
                } else {
                    successMessage = context.getString(R.string.ra_form_received_no_reply);
                }
                break;

            case OpenRosaResponse.StatusCode.ACCEPTED:
                if (hasMessages) {
                    successMessage = context.getString(R.string.ra_form_accepted_reply) + messages;
                } else {
                    successMessage = context.getString(R.string.ra_form_accepted_no_reply);
                }
                break;

            case OpenRosaResponse.StatusCode.UNUSED:
            default:
                successMessage = String.format(Locale.US,
                        context.getString(R.string.ra_form_unused_reply),
                        response.getStatusCode());
                break;
        }

        return successMessage;
    }

    public static String getFormSubmitErrorMessage(Context context, Throwable error) {
        String errorMessage = context.getString(R.string.ra_error_submitting_form);

        if (error instanceof IErrorBundle) {
            IErrorBundle errorBundle = (IErrorBundle) error;

            switch(errorBundle.getCode()) {
                case IErrorCode.UNAUTHORIZED:
                    errorMessage = String.format(context.getString(R.string.ra_error_submitting_form_tmp),
                            context.getString(R.string.ra_unauthorized));
                    break;
            }
        }

        return errorMessage;
    }

    public static String getFormDefErrorMessage(Context context, Throwable error) {
        String errorMessage = context.getString(R.string.ra_error_get_form_def);

        if (error instanceof IErrorBundle) {
            IErrorBundle errorBundle = (IErrorBundle) error;

            switch(errorBundle.getCode()) {
                case IErrorCode.NOT_FOUND:
                    errorMessage = String.format(context.getString(R.string.ra_error_get_form_def_tmp),
                            context.getString(R.string.ra_not_found));
                    break;
            }
        }

        return errorMessage;
    }
}
