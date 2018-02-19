package rs.readahead.washington.mobile.views.activity;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.widget.SwitchCompat;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.text.method.LinkMovementMethod;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.Calendar;
import java.util.Date;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnCheckedChanged;
import butterknife.OnClick;
import butterknife.OnFocusChange;
import butterknife.OnTextChanged;
import rs.readahead.washington.mobile.R;
import rs.readahead.washington.mobile.mvp.contract.INewReportPresenterContract;
import rs.readahead.washington.mobile.mvp.presenter.NewReportPresenter;
import rs.readahead.washington.mobile.presentation.entity.EvidenceData;
import rs.readahead.washington.mobile.presentation.entity.ReportRecipientData;
import rs.readahead.washington.mobile.presentation.entity.ReportViewType;
import rs.readahead.washington.mobile.util.C;
import rs.readahead.washington.mobile.util.DateUtil;
import rs.readahead.washington.mobile.util.DialogsUtil;
import rs.readahead.washington.mobile.util.StringUtils;


public class NewReportActivity extends CacheWordSubscriberBaseActivity implements
        INewReportPresenterContract.IView {

    public static final String REPORT_ID_KEY = "rik";
    public static final String RECIPIENTS_ID_KEY = "recipients_id_key";
    public static final String MEDIA_FILES_KEY = "mfk";
    public static final String REPORT_VIEW_TYPE = "type";

    @BindView(R.id.toolbar)
    Toolbar toolbar;
    @BindView(R.id.metadata_text)
    TextView mMetadataTextView;
    @BindView(R.id.metadata_layout)
    LinearLayout mMetadataLayout;
    @BindView(R.id.title_hint)
    TextView mTitleHintTextView;
    @BindView(R.id.title_text)
    TextView mTitleTextView;
    @BindView(R.id.title_indicator_text)
    TextView mTitleIndicatorTextView;
    @BindView(R.id.title_completed)
    ImageView mTitleCompletedImageView;
    @BindView(R.id.date_hint)
    TextView mDateHintTextView;
    @BindView(R.id.date_text)
    TextView mDateTextView;
    @BindView(R.id.date_layout)
    RelativeLayout mDateLayout;
    @BindView(R.id.evidence_layout)
    RelativeLayout mEvidenceLayout;
    @BindView(R.id.date_indicator_text)
    TextView mDateIndicatorTextView;
    @BindView(R.id.date_completed)
    ImageView mDateCompletedImageView;
    @BindView(R.id.description_hint)
    TextView mDescriptionHintTextView;
    @BindView(R.id.description_text)
    EditText mDescriptionTextView;
    @BindView(R.id.description_info)
    TextView mDescriptionInfoTextView;
    @BindView(R.id.description_indicator_text)
    TextView mDescriptionIndicatorTextView;
    @BindView(R.id.description_completed)
    ImageView mDescriptionCompletedImageView;
    @BindView(R.id.evidence_hint)
    TextView mEvidenceHintTextView;
    @BindView(R.id.evidence_text)
    TextView mEvidenceTextView;
    @BindView(R.id.evidence_indicator_text)
    TextView mEvidenceIndicatorTextView;
    @BindView(R.id.evidence_completed)
    ImageView mEvidenceCompletedImageView;
    @BindView(R.id.recipient_hint)
    TextView mRecipientHintTextView;
    @BindView(R.id.recipient_text)
    TextView mRecipientTextView;
    @BindView(R.id.recipient_indicator_text)
    TextView mRecipientIndicatorTextView;
    @BindView(R.id.recipient_completed)
    ImageView mRecipientCompletedImageView;
    @BindView(R.id.anonymous_switch)
    SwitchCompat mContactInfoSwitch;
    @BindView(R.id.contact_text)
    TextView mContactInfoTextView;
    @BindView(R.id.send_report)
    Button sendButton;
    @BindView(R.id.public_switch)
    SwitchCompat mPublicSwitch;
    @BindView(R.id.public_text)
    TextView mPublicText;

    private NewReportPresenter presenter;
    private Calendar mCalendar;
    private ProgressDialog progressDialog;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_report);
        ButterKnife.bind(this);

        presenter = new NewReportPresenter(this);

        setToolbar();
        startReport();
    }

    private void setToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.oppression_menu, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem edit = menu.findItem(R.id.edit_report);
        MenuItem draft = menu.findItem(R.id.save_to_drafts);
        edit.setVisible(presenter.isInPreviewMode());
        draft.setVisible(!presenter.isInPreviewMode());
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == android.R.id.home) {
            onBackPressed();
            return true;
        }

        if (id == R.id.save_to_drafts) {
            presenter.saveDraft();
            return true;
        }

        if (id == R.id.edit_report) {
            presenter.setReportType(ReportViewType.EDIT);
            presenter.checkContactInfo();
            invalidateOptionsMenu();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        if (presenter != null && presenter.isReportChanged()) {
            showReportChangedDialog();
        } else {
            onBackPressedWithoutCheck();
        }
    }

    private void onBackPressedWithoutCheck() {
        super.onBackPressed();
    }

    @OnFocusChange(R.id.title_text)
    void onTitleFocusChanged() {
        handleTitle();
    }

    @OnTextChanged(R.id.title_text)
    void onTitleTextChanged() {
        presenter.setReportTitle(mTitleTextView.getText().toString());
        handleTitle();
    }

    @OnFocusChange(R.id.description_text)
    void onDescriptionFocusChanged() {
        handleDescription();
    }

    @OnTextChanged(R.id.description_text)
    void onDescriptionTextChanged() {
        presenter.setReportDescription(mDescriptionTextView.getText().toString());
        handleDescription();
    }

    @OnClick(R.id.date_layout)
    void onDateClicked() {
        clearFocus();
        showCalendarDialog();
    }

    @OnClick(R.id.evidence_layout)
    void onEvidenceClicked() {
        clearFocus();
        setEvidenceIndicatorColor(Color.GRAY);
        startActivityForResult(new Intent(this, ReportEvidencesActivity.class)
                .putExtra(REPORT_VIEW_TYPE, presenter.getReportType())
                .putExtra(ReportEvidencesActivity.MEDIA_FILES_KEY, presenter.getEvidenceData()), C.EVIDENCE_IDS);
    }

    @OnClick(R.id.send_report)
    void sendReport() {
        clearFocus();
        presenter.sendReport();
    }

    @OnClick(R.id.recipient_layout)
    void onRecipientClicked() {
        clearFocus();
        setRecipientsIndicatorColor(Color.GRAY);
        startActivityForResult(new Intent(this, RecipientsActivity.class)
                .putExtra(REPORT_VIEW_TYPE, presenter.getReportType())

                .putExtra(RECIPIENTS_ID_KEY, presenter.getRecipientData()), C.RECIPIENT_IDS);
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        presenter.destroy();
        hideSendProgressBarDialog();
        super.onDestroy();
    }

    @OnCheckedChanged(R.id.anonymous_switch)
    void onContactInfoClicked() {
        presenter.setContactInfo(mContactInfoSwitch.isChecked());
        clearFocus();
    }

    @OnCheckedChanged(R.id.public_switch)
    void onPublicClicked() {
        presenter.setPublicInfo(mPublicSwitch.isChecked());
        clearFocus();
    }

    @Override
    public void onReportLoadError(Throwable throwable) {
        showToast(R.string.error); // todo: more informative error msg
    }

    @Override
    public void onDraftSaved() {
        showToast(R.string.saved_to_drafts);
    }

    @Override
    public void onDraftSaveError(Throwable throwable) {
        showToast(R.string.error); // todo: more informative error msg
    }

    @Override
    public void onSendReportStart() {
        showSendProgressBarDialog();
    }

    @Override
    public void onSendReportDone() {
        hideSendProgressBarDialog();
    }

    @Override
    public void onSentReport(String msg) {
        showToast(msg);
        finish();
    }

    @Override
    public void onSendReportError(Throwable throwable) {
        showToast(R.string.sending_report_error);
    }

    @Override
    public void onSendValidationFailed() {
        showToast(R.string.ra_report_send_validation_msg);
    }

    @Override
    public void showNoMetadataInfo(boolean show) {
        if (show) {
            mMetadataTextView.setText(Html.fromHtml(getString(R.string.no_metadata_description)));
            mMetadataTextView.setMovementMethod(LinkMovementMethod.getInstance());
            StringUtils.stripUnderlines(mMetadataTextView);
        }
        mMetadataLayout.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    @Override
    public void setTitleText(String title) {
        mTitleTextView.setText(title);
    }

    @Override
    public void setDescription(String description) {
        mDescriptionTextView.setText(description);
    }

    @Override
    public void setContactInfo(boolean useContactInfo) {
        mContactInfoSwitch.setChecked(useContactInfo);
    }

    @Override
    public void setDate(Date date) {
        if (date == null) return;
        mDateTextView.setText(DateUtils.isToday(date.getTime()) ? getString(R.string.today) :
                DateUtil.isYesterday(date) ? getString(R.string.yesterday) : DateUtil.getStringFromDate(date));
        onDateCompleted();
    }

    @Override
    public void setPublicInfo(boolean isPublic, boolean isContactInfo) {
        mPublicSwitch.setEnabled(!isContactInfo && !presenter.isInPreviewMode());
        mPublicSwitch.setChecked(isPublic && !isContactInfo);
        mPublicText.setText(isContactInfo ? R.string.make_report_public_disabled_desc :
                R.string.make_report_public_desc);
    }

    private void onDateCompleted() {
        mDateHintTextView.setVisibility(View.VISIBLE);
        mDateCompletedImageView.setVisibility(View.VISIBLE);
        mDateIndicatorTextView.setVisibility(View.GONE);
    }

    @Override
    public void onGetRecipientsCount(int count) {
        mRecipientTextView.setText(count == 0 ? getString(R.string.recipients) : String.format(getResources()
                .getQuantityString(R.plurals.recipients_number, count), String.valueOf(count)));
        mRecipientHintTextView.setVisibility(count == 0 ? View.GONE : View.VISIBLE);
        mRecipientIndicatorTextView.setVisibility(count == 0 ? View.VISIBLE : View.GONE);
        mRecipientCompletedImageView.setVisibility(count == 0 ? View.GONE : View.VISIBLE);
    }

    @Override
    public void onGetRecipientsCountError(Throwable throwable) {
        // todo: react..
    }

    @Override
    public void onEvidenceCounted(String text) {
        mEvidenceTextView.setText(TextUtils.isEmpty(text) ? getString(R.string.evidence) : text);
        mEvidenceHintTextView.setVisibility(TextUtils.isEmpty(text) ? View.GONE : View.VISIBLE);
        mEvidenceIndicatorTextView.setVisibility(TextUtils.isEmpty(text) ? View.VISIBLE : View.GONE);
        mEvidenceCompletedImageView.setVisibility(TextUtils.isEmpty(text) ? View.GONE : View.VISIBLE);
    }

    @Override
    public void setTitleIndicatorColor(int color) {
        mTitleIndicatorTextView.setTextColor(color);
    }

    @Override
    public void setDateIndicatorColor(int color) {
        mDateIndicatorTextView.setTextColor(color);
    }

    @Override
    public void setEvidenceIndicatorColor(int color) {
        mEvidenceIndicatorTextView.setTextColor(color);
    }

    @Override
    public void setRecipientsIndicatorColor(int color) {
        mRecipientIndicatorTextView.setTextColor(color);
    }

    @Override
    public void onContactInfoAvailable(boolean available) {
        mContactInfoSwitch.setEnabled(available && !presenter.isInPreviewMode());
        mContactInfoTextView.setText(available ? getString(R.string.contact_info) : Html.fromHtml(getString(R.string.contact_info_disabled)));
        mContactInfoTextView.setMovementMethod(LinkMovementMethod.getInstance());
        StringUtils.stripUnderlines(mContactInfoTextView);
    }

    @Override
    public void setActivityTitle() {
        toolbar.setTitle(getString(presenter.isInPreviewMode() ? R.string.report_preview : R.string.edit_report));
    }

    @Override
    public void setPreviewMode(boolean previewMode) {
        mTitleTextView.setEnabled(!previewMode);
        mDateLayout.setEnabled(!previewMode);
        mDescriptionTextView.setEnabled(!previewMode);
        mPublicSwitch.setEnabled(!previewMode);
        mPublicSwitch.setEnabled(!previewMode);
        sendButton.setVisibility(previewMode ? View.GONE : View.VISIBLE);
    }

    @Override
    public Context getContext() {
        return this;
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (presenter != null) {
            presenter.checkAnonymousState();
            presenter.checkContactInfo();
        }
    }

    private void startReport() {
        long reportId = 0;

        presenter.setReportType((ReportViewType) getIntent().getSerializableExtra(REPORT_VIEW_TYPE));
        presenter.checkAnonymousState();

        if (getIntent().hasExtra(REPORT_ID_KEY)) {
            reportId = getIntent().getLongExtra(REPORT_ID_KEY, 0);
        }

        if (reportId == 0) {
            presenter.startNewReport((EvidenceData) getIntent().getSerializableExtra(MEDIA_FILES_KEY));
        } else {
            presenter.loadReport(reportId);
        }
    }

    private void clearFocus() {
        mTitleTextView.clearFocus();
        mDescriptionTextView.clearFocus();
    }

    private void handleTitle() {
        showTitleHint(!presenter.isReportTitleEmpty());
        if (mTitleTextView.isFocused()) {
            showTitleIndicator(false);
            showTitleCompleted(false);
            setTitleIndicatorColor(Color.GRAY);
        } else {
            hideKeyboard(mTitleTextView);
            showTitleIndicator(presenter.isReportTitleEmpty());
            showTitleCompleted(!presenter.isReportTitleEmpty());
        }
    }

    public void showTitleHint(boolean show) {
        mTitleHintTextView.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    public void showTitleIndicator(boolean show) {
        mTitleIndicatorTextView.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    public void showTitleCompleted(boolean show) {
        mTitleCompletedImageView.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    private void handleDescription() {

        showDescriptionHint(!presenter.isReportDescriptionEmpty());
        showDescriptionInfo(mDescriptionTextView.isFocused());

        if (mDescriptionTextView.isFocused()) {
            showDescriptionIndicator(false);
            showDescriptionCompleted(false);
        } else {
            hideKeyboard(mDescriptionTextView);
            showDescriptionIndicator(presenter.isReportDescriptionEmpty());
            showDescriptionCompleted(!presenter.isReportDescriptionEmpty());
        }
    }

    public void showDescriptionHint(boolean show) {
        mDescriptionHintTextView.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    public void showDescriptionIndicator(boolean show) {
        mDescriptionIndicatorTextView.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    public void showDescriptionCompleted(boolean show) {
        mDescriptionCompletedImageView.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    public void showDescriptionInfo(boolean show) {
        mDescriptionInfoTextView.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    private void hideKeyboard(View view) {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    private void showCalendarDialog() {
        mCalendar = Calendar.getInstance();
        DatePickerDialog mDatePickerDialog = new DatePickerDialog(getContext(), new DatePickerDialog.OnDateSetListener() {
            public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                mCalendar = Calendar.getInstance();
                mCalendar.set(year, monthOfYear, dayOfMonth);
                presenter.setReportDate(mCalendar.getTime());
                setDateIndicatorColor(Color.GRAY);
            }
        }, mCalendar.get(Calendar.YEAR), mCalendar.get(Calendar.MONTH), mCalendar.get(Calendar.DAY_OF_MONTH));
        mDatePickerDialog.getDatePicker().setMaxDate(mCalendar.getTimeInMillis());
        mDatePickerDialog.show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (resultCode == Activity.RESULT_OK) {

            if (requestCode == C.RECIPIENT_IDS) {
                presenter.setRecipientData((ReportRecipientData) data.getSerializableExtra(RECIPIENTS_ID_KEY));
            }
            if (requestCode == C.EVIDENCE_IDS) {
                presenter.setEvidenceData((EvidenceData) data.getSerializableExtra(MEDIA_FILES_KEY));
            }
        }
    }

    private void showSendProgressBarDialog() {
        progressDialog = ProgressDialog.show(this, null, getString(R.string.sending_report_progress), true);
    }

    private void hideSendProgressBarDialog() {
        if (progressDialog != null) {
            progressDialog.dismiss();
        }
    }

    private void showReportChangedDialog() {
        String message = getString(R.string.your_draft_will_be_lost);

        DialogsUtil.showMessageOKCancelWithTitle(this,
                message,
                getString(R.string.attention),
                getString(R.string.save_and_exit),
                getString(R.string.exit_anyway),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (presenter != null) {
                            presenter.saveDraft();
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
