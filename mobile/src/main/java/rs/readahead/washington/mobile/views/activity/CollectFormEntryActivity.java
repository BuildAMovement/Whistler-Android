package rs.readahead.washington.mobile.views.activity;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import org.javarosa.core.model.FormIndex;
import org.javarosa.form.api.FormEntryCaption;
import org.javarosa.form.api.FormEntryPrompt;

import butterknife.BindView;
import butterknife.ButterKnife;
import rs.readahead.washington.mobile.MyApplication;
import rs.readahead.washington.mobile.R;
import rs.readahead.washington.mobile.bus.EventCompositeDisposable;
import rs.readahead.washington.mobile.bus.EventObserver;
import rs.readahead.washington.mobile.bus.event.CollectFormInstanceDeletedEvent;
import rs.readahead.washington.mobile.bus.event.CollectFormSavedEvent;
import rs.readahead.washington.mobile.bus.event.CollectFormSubmissionErrorEvent;
import rs.readahead.washington.mobile.bus.event.CollectFormSubmittedEvent;
import rs.readahead.washington.mobile.bus.event.FormAttachmentsUpdatedEvent;
import rs.readahead.washington.mobile.domain.entity.collect.CollectFormInstance;
import rs.readahead.washington.mobile.domain.entity.collect.CollectFormInstanceStatus;
import rs.readahead.washington.mobile.domain.entity.collect.OpenRosaResponse;
import rs.readahead.washington.mobile.javarosa.FormParser;
import rs.readahead.washington.mobile.javarosa.FormSaver;
import rs.readahead.washington.mobile.javarosa.FormSubmitter;
import rs.readahead.washington.mobile.javarosa.FormUtils;
import rs.readahead.washington.mobile.javarosa.IFormParserContract;
import rs.readahead.washington.mobile.javarosa.IFormSaverContract;
import rs.readahead.washington.mobile.javarosa.IFormSubmitterContract;
import rs.readahead.washington.mobile.odk.FormController;
import rs.readahead.washington.mobile.presentation.entity.EvidenceData;
import rs.readahead.washington.mobile.util.C;
import rs.readahead.washington.mobile.util.Util;
import rs.readahead.washington.mobile.util.DialogsUtil;
import rs.readahead.washington.mobile.views.collect.CollectFormEndView;
import rs.readahead.washington.mobile.views.collect.CollectFormView;
import timber.log.Timber;


public class CollectFormEntryActivity extends MetadataActivity implements
        IFormParserContract.IView,
        IFormSaverContract.IView,
        IFormSubmitterContract.IView {
    @BindView(R.id.screenFormView)
    ViewGroup screenFormView;
    @BindView(R.id.prevSection)
    Button prevSectionButton;
    @BindView(R.id.nextSection)
    Button nextSectionButton;
    @BindView(R.id.submit_button)
    Button submitButton;
    @BindView(R.id.go_back_button)
    Button goBackButton;
    @BindView(R.id.toolbar)
    Toolbar toolbar;
    @BindView(R.id.button_bottom_layout)
    ViewGroup buttonBottomLayout;

    private View currentScreenView;
    //private int sectionIndex;
    private String formTitle;
    private FormParser formParser;
    private FormSaver formSaver;
    private FormSubmitter formSubmitter;
    private EventCompositeDisposable disposables;

    private ProgressDialog progressDialog;
    private CollectFormEndView endView;
    private AlertDialog alertDialog;
    private boolean attachmentsEnabled = false;
    private boolean deleteEnabled = false;
    private boolean draftAutoSaved = false;


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_collect_form_entry);
        ButterKnife.bind(this);

        currentScreenView = null;
        //sectionIndex = 0;

        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        prevSectionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showPrevScreen();
            }
        });

        nextSectionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showNextScreen();
            }
        });

        submitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (formSubmitter != null) {
                    formSubmitter.submitActiveFormInstance(formTitle + " " + Util.getDateTimeString());
                }
            }
        });

        goBackButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hideSubmitButtons();
                showPrevScreen();
            }
        });

        endView = new CollectFormEndView(this);

        disposables = MyApplication.bus().createCompositeDisposable();
        disposables.wire(FormAttachmentsUpdatedEvent.class, new EventObserver<FormAttachmentsUpdatedEvent>() {
            @Override
            public void onNext(FormAttachmentsUpdatedEvent event) {
                formAttachmentsChanged();
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        getMenuInflater().inflate(R.menu.collect_form_entry_menu, menu);

        if (attachmentsEnabled) {
            int attNum;

            final MenuItem menuItem = menu.findItem(R.id.formAttachmentsMenuItem);
            menuItem.setVisible(true);

            TextView textView = menuItem.getActionView().findViewById(R.id.badgeText);
            if (formParser != null && (attNum = formParser.getFormAttachments().size()) > 0) {
                textView.setVisibility(View.VISIBLE);
                textView.setText(String.format("%d", attNum));
            } else {
                textView.setVisibility(View.INVISIBLE);
            }

            View badge = menuItem.getActionView().findViewById(R.id.badgeItem);
            badge.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    menu.performIdentifierAction(menuItem.getItemId(), 0);
                }
            });
        }

        if (deleteEnabled) {
            final MenuItem menuItem = menu.findItem(R.id.deleteFormMenuItem);
            menuItem.setVisible(true);
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.saveFormMenuItem) {
            if (formSaver != null) {
                saveCurrentScreen(false);
                formSaver.saveActiveFormInstance();
            }
            return true;
        }

        if (id == R.id.formAttachmentsMenuItem) {
            showAttachmentsActivity();
            return true;
        }

        if (id == R.id.deleteFormMenuItem) {
            deleteFormInstance();
            return true;
        }

        if (id == android.R.id.home) {
            onBackPressed();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void showAttachmentsActivity() {
        EvidenceData data = new EvidenceData(FormController.getActive().getCollectFormInstance().getMediaFiles());
        startActivityForResult(new Intent(this, AttachmentsActivity.class)
                .putExtra(AttachmentsActivity.MEDIA_FILES_KEY, data), C.EVIDENCE_IDS);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode != RESULT_OK) {
            return;
        }

        if (requestCode == C.EVIDENCE_IDS) {
            EvidenceData evidenceData = (EvidenceData) data.getSerializableExtra((AttachmentsActivity.MEDIA_FILES_KEY));
            FormController.getActive().getCollectFormInstance().setMediaFiles(evidenceData.getEvidences());
        }
    }

    private void deleteFormInstance() {
        if (formSaver == null) return;

        boolean cloned = formSaver.isActiveInstanceCloned();

        alertDialog = DialogsUtil.showFormInstanceDeleteDialog(
                this,
                cloned ? CollectFormInstanceStatus.SUBMITTED : CollectFormInstanceStatus.UNKNOWN,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (formSaver != null) {
                            formSaver.deleteActiveFormInstance();
                        }
                    }
                });
    }

    @Override
    protected void onStart() {
        super.onStart();
        startLocationMetadataListening();
    }

    @Override
    protected void onPause() {
        if (alertDialog != null && alertDialog.isShowing()) {
            alertDialog.dismiss();
        }

        super.onPause();
    }

    @Override
    protected void onStop() {
        stopLocationMetadataListening();
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        if (draftAutoSaved) {
            MyApplication.bus().post(new CollectFormSavedEvent());
        }

        if (disposables != null) {
            disposables.dispose();
        }

        destroyFormParser();
        destroyFormSubmitter();
        destroyFormSaver();

        hideProgressDialog();

        super.onDestroy();
    }

    @Override
    public void onCacheWordOpened() {
        super.onCacheWordOpened();
        formSaver = new FormSaver(this);
        formSubmitter = new FormSubmitter(this);
        formParser = new FormParser(this);
        formParser.parseForm();
    }

    @Override
    public void onBackPressed() {
        if (formParser != null && formParser.isFormChanged()) {
            showFormChangedDialog();
        } else {
            onBackPressedWithoutCheck();
        }
    }

    private void onBackPressedWithoutCheck() {
        if (draftAutoSaved) {
            MyApplication.bus().post(new CollectFormSavedEvent());
        }

        super.onBackPressed();
    }

    @Override
    public void formBeginning(String title) {
        formTitle = title;
        setTitle(formTitle);
        formParser.stepToNextScreen();
    }

    @Override
    public void formEnd(String title) {
        Util.hideKeyboard(this, endView);
        showFormEndView();
        hideSectionButtons();
    }

    @Override
    public void formQuestion(FormEntryPrompt[] prompts, FormEntryCaption[] groups) {
        showFormView(new CollectFormView(this, prompts, groups));
        setSectionButtons();
    }

    @Override
    public void formGroup(FormEntryPrompt[] prompts, FormEntryCaption[] groups) {
        showFormView(new CollectFormView(this, prompts, groups));
        setSectionButtons();
    }

    @Override
    public void formRepeat(FormEntryPrompt[] prompts, FormEntryCaption[] groups) {
        showFormView(new CollectFormView(this, prompts, groups));
        setSectionButtons();
    }

    @Override
    public void formPromptNewRepeat(int lastRepeatCount, String groupText) {
        createPromptDialog(lastRepeatCount, groupText);
    }

    @Override
    public void formParseError(Throwable error) {
        showToast(R.string.ra_form_parse_error);
    }

    @Override
    public void formSaveError(Throwable error) {
        Timber.d(error, getClass().getName());
    }

    @Override
    public void showSaveFormInstanceLoading() {
    }

    @Override
    public void hideSaveFormInstanceLoading() {
    }

    @Override
    public void showDeleteFormInstanceStart() {
    }

    @Override
    public void hideDeleteFormInstanceEnd() {
    }

    @Override
    public void formInstanceSaveError(Throwable throwable) {
    }

    @Override
    public void formInstanceSaveSuccess(CollectFormInstance instance) {
        Toast.makeText(this, getFormSaveMsg(instance), Toast.LENGTH_SHORT).show();
        formParser.startFormChangeTracking();
        MyApplication.bus().post(new CollectFormSavedEvent());
    }

    @Override
    public void formInstanceAutoSaveSuccess(CollectFormInstance instance) {
        Toast.makeText(this, getFormSaveMsg(instance), Toast.LENGTH_SHORT).show();
        formParser.startFormChangeTracking();
        draftAutoSaved = true;
    }

    private String getFormSaveMsg(CollectFormInstance instance) {
        switch (instance.getStatus()) {
            case UNKNOWN:
            case DRAFT:
                return getString(R.string.ra_draft_saved);

            default:
                return getString(R.string.ra_form_saved);
        }
    }

    @Override
    public void formInstanceDeleteSuccess(boolean cloned) {
        MyApplication.bus().post(new CollectFormInstanceDeletedEvent(cloned));
        finish();
    }

    @Override
    public void formInstanceDeleteError(Throwable throwable) {
        Timber.d(throwable, getClass().getName());
    }

    @Override
    public void showFormSubmitLoading() {
        progressDialog = DialogsUtil.showProgressDialog(this, getString(R.string.ra_submitting_form));
    }

    @Override
    public void hideFormSubmitLoading() {
        hideProgressDialog();
    }

    @Override
    public void formSubmitError(Throwable error) {
        String errorMessage = FormUtils.getFormSubmitErrorMessage(this, error);

        Toast.makeText(getApplicationContext(), errorMessage, Toast.LENGTH_LONG).show();

        MyApplication.bus().post(new CollectFormSubmissionErrorEvent()); // refresh form lists..
        finish();
    }

    @Override
    public void formSubmitNoConnectivity() {
        Toast.makeText(getApplicationContext(), R.string.ra_form_send_submission_pending, Toast.LENGTH_LONG).show();
        MyApplication.bus().post(new CollectFormSubmittedEvent());
        finish();
    }

    @Override
    public void formSubmitSuccess(CollectFormInstance instance, OpenRosaResponse response) {
        String successMessage = FormUtils.getFormSubmitSuccessMessage(this, response);

        Toast.makeText(getApplicationContext(), successMessage, Toast.LENGTH_LONG).show();

        MyApplication.bus().post(new CollectFormSubmittedEvent());
        finish();
    }

    @Override
    public void formConstraintViolation(FormIndex formIndex, String errorString) {
        if (currentScreenView != null && currentScreenView instanceof CollectFormView) {
            ((CollectFormView) currentScreenView).setValidationConstraintText(formIndex, errorString);
            showToast(getString(R.string.ra_answers_validation_errors));
        }
    }

    @Override
    public void formPropertiesChecked(boolean enableAttachments, boolean enableDelete) {
        boolean invalidateOptionsMenu = false;

        if (enableAttachments) {
            attachmentsEnabled = true;
            invalidateOptionsMenu = true;
        }

        if (enableDelete) {
            deleteEnabled = true;
            invalidateOptionsMenu = true;
        }

        if (invalidateOptionsMenu) {
            invalidateOptionsMenu();
        }
    }

    @Override
    public Context getContext() {
        return this;
    }

    private void createPromptDialog(int lastRepeatCount, String groupText) {
        if (alertDialog != null && alertDialog.isShowing()) {
            return;
        }

        alertDialog = new AlertDialog.Builder(this)
                .setPositiveButton(R.string.ra_add_group_dialog_positive, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        formParser.executeRepeat();
                    }
                })
                .setNegativeButton(R.string.ra_do_not_add_group, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        formParser.cancelRepeat();
                    }
                })
                .setCancelable(false)
                .create();

        if (lastRepeatCount > 0) {
            alertDialog.setTitle(getString(R.string.ra_leaving_repeat_ask));
            alertDialog.setMessage(getString(R.string.ra_add_another_repeat, groupText));
        } else {
            alertDialog.setTitle(getString(R.string.ra_entering_repeat_ask));
            alertDialog.setMessage(getString(R.string.ra_add_repeat, groupText));
        }

        alertDialog.show();
    }

    @SuppressWarnings("MethodOnlyUsedFromInnerClass")
    private void formAttachmentsChanged() {
        invalidateOptionsMenu();
    }

    private void showScreenView(View view) {
        if (currentScreenView != null) {
            screenFormView.removeView(currentScreenView);
        }
        currentScreenView = view;
        screenFormView.addView(currentScreenView);
    }

    private void showFormView(CollectFormView view) {
        hideKeyboard();
        showScreenView(view);
        view.setFocus(this);
    }

    private void showFormEndView() {
        hideKeyboard();
        showFormEndButtons();
        showScreenView(endView);
    }

    @SuppressWarnings("MethodOnlyUsedFromInnerClass")
    private void showNextScreen() {
        if (saveCurrentScreen(true)) {
            formSaver.autoSaveFormInstance();
            formParser.stepToNextScreen();
        }
    }

    private void showPrevScreen() {
        if (saveCurrentScreen(false)) {
            formSaver.autoSaveFormInstance();
            formParser.stepToPrevScreen();
        }
    }

    private boolean saveCurrentScreen(boolean checkConstraints) {
        if (currentScreenView != null && currentScreenView instanceof CollectFormView) {
            CollectFormView cfv = (CollectFormView) currentScreenView;

            if (checkConstraints) {
                cfv.clearValidationConstraints();
            }

            return formSaver.saveScreenAnswers(cfv.getAnswers(), checkConstraints);
        }

        return true;
    }

    private void hideKeyboard() {
        View v = getCurrentFocus();
        if (v != null) {
            Util.hideKeyboard(this, v);
        }
    }

    private void destroyFormSaver() {
        if (formSaver != null) {
            formSaver.destroy();
            formSaver = null;
        }
    }

    private void destroyFormSubmitter() {
        if (formSubmitter != null) {
            formSubmitter.destroy();
            formSubmitter = null;
        }
    }

    private void destroyFormParser() {
        if (formParser != null) {
            formParser.destroy();
            formParser = null;
        }
    }

    private void hideProgressDialog() {
        if (progressDialog != null) {
            progressDialog.dismiss();
            progressDialog = null;
        }
    }

    // this bottom buttons on/off thing looks stupid :)
    private void setFirstSectionButtons() {
        hideSubmitButtons();
        hidePrevSectionButton();
    }

    private void setSectionButtons() {
        buttonBottomLayout.setVisibility(View.VISIBLE);

        showNextSectionButton();

        if (formParser.isFirstScreen()) {
            setFirstSectionButtons();
            return;
        }

        prevSectionButton.setEnabled(true);
        prevSectionButton.setVisibility(View.VISIBLE);
    }

    private void showFormEndButtons() {
        goBackButton.setEnabled(true);
        goBackButton.setVisibility(View.VISIBLE);
        submitButton.setEnabled(true);
        submitButton.setVisibility(View.VISIBLE);
    }

    private void hidePrevSectionButton() {
        prevSectionButton.setEnabled(false);
        prevSectionButton.setVisibility(View.GONE);
    }

    private void hideSectionButtons() {
        hidePrevSectionButton();
        nextSectionButton.setEnabled(false);
        nextSectionButton.setVisibility(View.GONE);
    }

    private void hideSubmitButtons() {
        submitButton.setEnabled(false);
        submitButton.setVisibility(View.GONE);
        goBackButton.setEnabled(false);
        goBackButton.setVisibility(View.GONE);
    }

    private void showNextSectionButton() {
        nextSectionButton.setEnabled(true);
        nextSectionButton.setVisibility(View.VISIBLE);
    }

    private void showFormChangedDialog() {
        String message = getString(R.string.your_draft_will_be_lost);

        DialogsUtil.showMessageOKCancelWithTitle(this,
                message,
                getString(R.string.attention),
                getString(R.string.save_and_exit),
                getString(R.string.exit_anyway),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (formSaver != null) {
                            formSaver.saveActiveFormInstanceOnExit();
                            MyApplication.bus().post(new CollectFormSavedEvent());
                        }
                        dialog.dismiss();
                        onBackPressedWithoutCheck();
                    }
                },
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        onBackPressedWithoutCheck();
                    }
                });
    }
}
