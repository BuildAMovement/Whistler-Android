package rs.readahead.washington.mobile.views.activity;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.Bundle;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import rs.readahead.washington.mobile.R;
import rs.readahead.washington.mobile.domain.entity.Feedback;
import rs.readahead.washington.mobile.mvp.contract.IFeedbackPresenterContract;
import rs.readahead.washington.mobile.mvp.presenter.FeedbackPresenter;
import rs.readahead.washington.mobile.util.DialogsUtil;


public class FeedbackActivity extends CacheWordSubscriberBaseActivity implements
        IFeedbackPresenterContract.IView {
    @BindView(R.id.toolbar)
    Toolbar toolbar;
    @BindView(R.id.name)
    EditText name;
    @BindView(R.id.email)
    EditText email;
    @BindView(R.id.email_layout)
    TextInputLayout emailLayout;
    @BindView(R.id.feedback)
    EditText feedbackMessage;
    @BindView(R.id.feedback_layout)
    TextInputLayout feedbackLayout;
    @BindView(R.id.send_feedback)
    Button sendFeedbackMessage;

    private FeedbackPresenter presenter;
    private ProgressDialog progressDialog;
    private boolean valid;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_feedback);

        ButterKnife.bind(this);

        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        presenter = new FeedbackPresenter(this);
    }

    @Override
    protected void onDestroy() {
        presenter.destroy();
        hideProgressDialog();
        super.onDestroy();
    }

    @OnClick(R.id.send_feedback)
    public void sendFeedback() {
        if (validate()) {
            presenter.sendFeedback(new Feedback(name.getText().toString(),
                    email.getText().toString(),
                    feedbackMessage.getText().toString())
            );
        }
    }

    @Override
    public void onFeedbackSendStarted() {
        progressDialog = DialogsUtil.showProgressDialog(this, getString(R.string.ra_sending_feedback));
    }

    @Override
    public void onFeedbackSendFinished() {
        hideProgressDialog();
    }

    @Override
    public void onSentFeedback() {
        showToast(R.string.feedback_sent);
        finish();
    }

    @Override
    public void onSendFeedbackError(Throwable throwable) {
        showToast(R.string.feedback_sending_error);
    }

    @Override
    public Context getContext() {
        return this;
    }

    private boolean validate() {
        valid = true;
        validateRequired(feedbackMessage, feedbackLayout);
        validateEmail(email, emailLayout);

        return valid;
    }

    private void validateRequired(EditText field, TextInputLayout layout) {
        layout.setError(null);
        if (TextUtils.isEmpty(field.getText().toString())) {
            layout.setError(this.getString(R.string.empty_field_error));
            valid = false;
        }
    }

    private void validateEmail(EditText field, TextInputLayout layout) {
        layout.setError(null);

        String email = field.getText().toString();

        if (TextUtils.isEmpty(email)) {
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            layout.setError(getString(R.string.email_field_error));
            valid = false;
        }
    }

    private void hideProgressDialog() {
        if (progressDialog != null) {
            progressDialog.dismiss();
            progressDialog = null;
        }
    }
}
