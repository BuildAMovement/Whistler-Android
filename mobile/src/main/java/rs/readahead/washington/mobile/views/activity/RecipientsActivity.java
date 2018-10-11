package rs.readahead.washington.mobile.views.activity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.StringRes;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CheckedTextView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import info.guardianproject.cacheword.ICacheWordSubscriber;
import rs.readahead.washington.mobile.R;
import rs.readahead.washington.mobile.domain.entity.MediaRecipient;
import rs.readahead.washington.mobile.domain.entity.MediaRecipientList;
import rs.readahead.washington.mobile.mvp.contract.IReportRecipientsPresenterContract;
import rs.readahead.washington.mobile.mvp.presenter.ReportRecipientPresenter;
import rs.readahead.washington.mobile.presentation.entity.ReportRecipientData;
import rs.readahead.washington.mobile.presentation.entity.ReportViewType;
import rs.readahead.washington.mobile.views.dialog.Dialogs;
import timber.log.Timber;


public class RecipientsActivity extends CacheWordSubscriberBaseActivity implements
        ICacheWordSubscriber,
        IReportRecipientsPresenterContract.IView, View.OnClickListener {

    public static String RECIPIENTS_ID_KEY = "recipients_id_key";
    public static final String REPORT_VIEW_TYPE = "type";

    @BindView(R.id.toolbar)
    Toolbar toolbar;
    @BindView(R.id.new_recipient)
    Button mNewRecipientButton;
    @BindView(R.id.new_list)
    Button mNewListButton;
    @BindView(R.id.recipient_lists_list)
    LinearLayout mListLayout;
    @BindView(R.id.recipient_list)
    LinearLayout mRecipientLayout;

    private MenuItem mSelectMenuItem;
    private ReportRecipientPresenter presenter;
    private TextView mEmptyRecipientView;
    List<MediaRecipientList> checkedRecipientsLists = new ArrayList<>();
    Set<MediaRecipient> checkedRecipients = new HashSet<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recipients);
        ButterKnife.bind(this);
        setToolbar();

        getRecipientData();
    }

    @Override
    protected void onResume() {
        super.onResume();
        overridePendingTransition(R.anim.slide_in_up, android.R.anim.fade_out);

        if (!presenter.isInPreviewMode()) {
            presenter.listNonEmptyRecipientLists();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        overridePendingTransition(android.R.anim.fade_in, R.anim.slide_out_down);
    }

    private void setToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_close_white);
            getSupportActionBar().setTitle(R.string.title_activity_recipients);
        }
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        mSelectMenuItem = menu.findItem(R.id.menu_item_select);
        setSelectVisible(false);
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.recipients_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.menu_item_select) {
            onRecipientsSelected();
            return true;
        }

        if (id == android.R.id.home) {
            finish();
            return true;
        }


        if (id == R.id.help_item) {
            startRecipientsHelp();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void setSelectVisible(boolean visible) {
        mSelectMenuItem.setVisible(visible);
    }

    private void onRecipientsSelected() {
        addCheckedRecipientListsToReport();
        addCheckedRecipientsToReport();
        setResult(Activity.RESULT_OK, new Intent().putExtra(RECIPIENTS_ID_KEY, presenter.getRecipientsData()));
        finish();
    }

    private void getRecipientData() {
        presenter = new ReportRecipientPresenter(this);
        presenter.setReportType((ReportViewType) getIntent().getSerializableExtra(REPORT_VIEW_TYPE));

        if (getIntent().hasExtra(RECIPIENTS_ID_KEY)) {
            presenter.setRecipientData((ReportRecipientData) getIntent().getSerializableExtra(RECIPIENTS_ID_KEY));
        }

        if (!presenter.isInPreviewMode()) {
            getRecipientDataFromDB();
        }
    }

    private void getRecipientDataFromDB() {
        presenter.listNonEmptyRecipientLists();
        presenter.listAllRecipients();
    }

    @OnClick(R.id.new_list)
    public void onNewRecipientList() {
        Dialogs.showAddRecipientListDialog(this, new Dialogs.IRecipientListDialogListener() {
            @Override
            public void call(MediaRecipientList recipientList) {
                presenter.addMediaRecipientList(recipientList);
            }
        });
    }

    @OnClick(R.id.new_recipient)
    public void onNewRecipient() {
        Dialogs.showRecipientDialog(getContext(), null, new Dialogs.IRecipientDialogListener() {
            @Override
            public void call(MediaRecipient recipient) {
                presenter.addMediaRecipient(recipient);
            }
        });
    }

    @Override
    public void onAllRecipient(List<MediaRecipient> mediaRecipientList) {
        LayoutInflater inflater = LayoutInflater.from(getContext());

        mRecipientLayout.removeAllViews();

        if (mediaRecipientList.isEmpty()) {
            mEmptyRecipientView = createNotAvailableText(inflater, mRecipientLayout, R.string.no_recipient_available);
            mRecipientLayout.addView(mEmptyRecipientView);
            return;
        }

        for (MediaRecipient recipient : mediaRecipientList) {
            CheckedTextView item = (CheckedTextView) createRecipientCheckBox(inflater, recipient);

            if (presenter.ifRecipientIsSelected(recipient)) {
                item.setEnabled(!presenter.isInPreviewMode());
                item.setChecked(true);
            }

            mRecipientLayout.addView(item);
        }
    }

    private TextView createNotAvailableText(LayoutInflater inflater, LinearLayout linearLayout, @StringRes int resId) {
        TextView item = (TextView) inflater
                .inflate(R.layout.no_recipient_available, linearLayout, false);
        item.setText(getString(resId));
        return item;
    }

    private View createRecipientCheckBox(LayoutInflater inflater, MediaRecipient recipient) {
        CheckedTextView item = (CheckedTextView) inflater
                .inflate(R.layout.media_recipient_checked_text_view, mRecipientLayout, false);

        item.setText(recipient.getTitle());
        item.setTag(recipient);
        item.setOnClickListener(this);

        return item;
    }

    private View createRecipientListCheckBox(LayoutInflater inflater, MediaRecipientList mediaRecipientList) {
        CheckedTextView item = (CheckedTextView) inflater
                .inflate(R.layout.media_recipient_checked_text_view, mListLayout, false);
        item.setText(mediaRecipientList.getTitle());
        item.setTag(mediaRecipientList);
        item.setOnClickListener(this);

        return item;
    }

    @Override
    public void onAllRecipientError(Throwable throwable) {
        Timber.d(throwable, getClass().getName());
    }

    @Override
    public void onListNonEmptyRecipientList(List<MediaRecipientList> mediaRecipientLists) {
        LayoutInflater inflater = LayoutInflater.from(getContext());

        mListLayout.removeAllViews();

        if (mediaRecipientLists.isEmpty()) {
            TextView mEmptyListView = createNotAvailableText(inflater, mListLayout, R.string.no_recipient_list_available);
            mListLayout.addView(mEmptyListView);
            return;
        }

        for (MediaRecipientList list : mediaRecipientLists) {
            CheckedTextView item = (CheckedTextView) createRecipientListCheckBox(inflater, list);

            if (presenter.ifRecipientListIsSelected(list)) {
                item.setChecked(true);
                item.setEnabled(!presenter.isInPreviewMode());
            }

            mListLayout.addView(item);
        }
    }

    @Override
    public void onListNonEmptyRecipientListError(Throwable throwable) {
        Timber.d(throwable, getClass().getName());
    }

    @Override
    public void onAddMediaRecipient(MediaRecipient mediaRecipient) {
        if (mEmptyRecipientView != null) {
            mEmptyRecipientView.setVisibility(View.GONE);
        }
        mRecipientLayout.addView(createRecipientCheckBox(LayoutInflater.from(getContext()), mediaRecipient));
    }

    @Override
    public void onAddMediaRecipientError(Throwable throwable) {
        Timber.d(throwable, getClass().getName());
    }

    @Override
    public void onAddMediaRecipientList(MediaRecipientList mediaRecipientList) {
        Intent intent = new Intent(getContext(), EditMediaRecipientListActivity.class);
        intent.putExtra(EditMediaRecipientListActivity.RECIPIENT_LIST_ID, mediaRecipientList.getId());
        startActivity(intent);
    }

    @Override
    public void onAddMediaRecipientListError(Throwable throwable) {
        Timber.d(throwable, getClass().getName());
    }

    @Override
    public void onSelectMediaRecipientsFromListError(Throwable throwable) {
        Timber.d(throwable, getClass().getName());
    }

    @Override
    public void onPreviewMode() {
        mNewListButton.setVisibility(View.GONE);
        mNewRecipientButton.setVisibility(View.GONE);
    }

    @Override
    public Context getContext() {
        return this;
    }

    @Override
    protected void onDestroy() {
        presenter.destroy();
        super.onDestroy();
    }

    @Override
    public void onClick(View view) {
        if (!(view instanceof CheckedTextView)) return;

        Object tag = view.getTag();
        CheckedTextView checkedTextView = (CheckedTextView) view;

        checkedTextView.toggle();

        if (tag instanceof MediaRecipient) {
            MediaRecipient mediaRecipient = (MediaRecipient) tag;

            if (checkedTextView.isChecked()) {
                checkedRecipients.add(mediaRecipient);
            } else {
                checkedRecipients.remove(mediaRecipient);
            }
        } else if (tag instanceof MediaRecipientList) {
            MediaRecipientList mediaRecipientList = (MediaRecipientList) tag;

            if (checkedTextView.isChecked()) {
                checkedRecipientsLists.add(mediaRecipientList);
                presenter.selectDifferentMediaRecipientsFromList(mediaRecipientList, checkedRecipientsLists, true);
            } else {
                checkedRecipientsLists.remove(mediaRecipientList);
                presenter.selectDifferentMediaRecipientsFromList(mediaRecipientList, checkedRecipientsLists, false);
            }
        }
        setSelectVisible(true);
    }

    @Override
    public void checkRecipientsFromList(List<MediaRecipient> mediaRecipientList, boolean check) {
        for (MediaRecipient recipient : mediaRecipientList) {
            CheckedTextView item = mRecipientLayout.findViewWithTag(recipient);
            if (!checkedRecipients.contains(recipient)) {
                item.setChecked(check);
            }
        }
    }

    private void addCheckedRecipientsToReport() {
        List<MediaRecipient> checkedRecipients = new ArrayList<>();

        for (int i = 0; i < mRecipientLayout.getChildCount(); i++) {
            View view = mRecipientLayout.getChildAt(i);
            if (!(view instanceof CheckedTextView)) {
                continue;
            }

            CheckedTextView item = (CheckedTextView) view;
            if (item.isChecked() && item.getTag() instanceof MediaRecipient) {
                MediaRecipient recipient = (MediaRecipient) item.getTag();
                checkedRecipients.add(recipient);
            }
        }

        presenter.setReportRecipients(checkedRecipients);
    }

    private void addCheckedRecipientListsToReport() {
        List<MediaRecipientList> checkedRecipientLists = new ArrayList<>();

        for (int i = 0; i < mListLayout.getChildCount(); i++) {
            View view = mListLayout.getChildAt(i);
            if (!(view instanceof CheckedTextView)) {
                continue;
            }

            CheckedTextView item = (CheckedTextView) view;
            if (item.isChecked() && item.getTag() instanceof MediaRecipientList) {
                MediaRecipientList list = (MediaRecipientList) item.getTag();
                checkedRecipientLists.add(list);
            }
        }

        presenter.setReportRecipientLists(checkedRecipientLists);
    }

    private void startRecipientsHelp() {
        startActivity(new Intent(RecipientsActivity.this, RecipientsHelpActivity.class));
    }

}