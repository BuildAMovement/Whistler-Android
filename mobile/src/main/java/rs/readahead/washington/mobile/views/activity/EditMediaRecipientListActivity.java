package rs.readahead.washington.mobile.views.activity;

import android.content.Context;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import info.guardianproject.cacheword.CacheWordHandler;
import info.guardianproject.cacheword.ICacheWordSubscriber;
import rs.readahead.washington.mobile.MyApplication;
import rs.readahead.washington.mobile.R;
import rs.readahead.washington.mobile.bus.event.MediaRecipientListUpdatedEvent;
import rs.readahead.washington.mobile.data.database.DataSource;
import rs.readahead.washington.mobile.domain.entity.MediaRecipient;
import rs.readahead.washington.mobile.domain.entity.MediaRecipientList;
import rs.readahead.washington.mobile.presentation.entity.MediaRecipientSelection;
import rs.readahead.washington.mobile.presentation.entity.mapper.MediaRecipientMapper;
import rs.readahead.washington.mobile.util.CommonUtils;
import rs.readahead.washington.mobile.views.adapters.MediaRecipientSelectorListAdapter;


public class EditMediaRecipientListActivity extends BaseActivity implements
        ICacheWordSubscriber {
    public static String RECIPIENT_LIST_ID = "recipient_list_id";

    @BindView(R.id.root)
    View rootView;
    @BindView(R.id.recipients)
    ListView recipientsListView;
    @BindView(R.id.edit_recipient_list)
    EditText editListView;
    @BindView(R.id.edit_enable_image)
    ImageView editEnableImage;

    private DataSource dataSource;
    private CacheWordHandler cacheWord;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_media_recipient_list);

        ButterKnife.bind(this);

        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationIcon(R.drawable.ic_close_white_24dp);
        setSupportActionBar(toolbar);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        cacheWord = new CacheWordHandler(this);

        CommonUtils.hideKeyboard(this, rootView);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.recipient_list_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == android.R.id.home) {
            navigateBack();
            return true;
        }

        if (id == R.id.action_select) {
            saveMediaRecipientList();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();
        cacheWord.connectToService();
    }

    @Override
    protected void onPause() {
        super.onPause();
        cacheWord.disconnectFromService();
    }

    @Override
    public void onCacheWordUninitialized() {
        MyApplication.showLockScreen(this);
        finish();
    }

    @Override
    public void onCacheWordLocked() {
        MyApplication.showLockScreen(this);
        finish();
    }

    @Override
    public void onCacheWordOpened() {
        dataSource = DataSource.getInstance(this, cacheWord.getEncryptionKey());
        initViews();
    }

    private void navigateBack() {
        onBackPressed();
    }

    @OnClick(R.id.edit_enable_image)
    public void activateEditText() {
        editEnableImage.setVisibility(View.GONE);

        editListView.setEnabled(true);
        editListView.requestFocus();

        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.showSoftInput(editListView, InputMethodManager.SHOW_IMPLICIT);
    }

    private void saveMediaRecipientList() {
        int listId = getIntent().getExtras().getInt(RECIPIENT_LIST_ID);

        List<Integer> selectedRecipients = new ArrayList<>();
        MediaRecipientSelectorListAdapter adapter = (MediaRecipientSelectorListAdapter) recipientsListView.getAdapter();
        int count = adapter.getCount();

        for (int i = 0; i < count; i++) {
            MediaRecipientSelection selection = adapter.getItem(i);
            if (selection != null && selection.isChecked()) {
                selectedRecipients.add(selection.getId());
            }
        }

        MediaRecipientList mediaRecipientList = new MediaRecipientList(listId, editListView.getText().toString());
        dataSource.updateMediaRecipientList(mediaRecipientList, selectedRecipients);

        MyApplication.bus().post(new MediaRecipientListUpdatedEvent(mediaRecipientList));

        navigateBack();
    }

    private void initViews() {
        initEditText();
        initRecipientsListView();
    }

    private void initEditText() {
        int listId = getIntent().getExtras().getInt(RECIPIENT_LIST_ID);
        MediaRecipientList list = dataSource.getMediaRecipientList(listId);

        if (list == null) {
            return; // wtf
        }

        editListView.setText(list.getTitle());
        editListView.setSelection(editListView.getText().length());
    }

    private void initRecipientsListView() {
        int listId = getIntent().getExtras().getInt(RECIPIENT_LIST_ID);

        List<MediaRecipient> recipients = dataSource.getMediaRecipients();
        List<Integer> recipientIds = dataSource.getRecipientIdsByListId(listId);
        List<MediaRecipientSelection> selections = new MediaRecipientMapper().transform(recipients);

        for (MediaRecipientSelection selection : selections) {
            if (recipientIds.contains(selection.getId())) {
                selection.setChecked(true);
            }
        }

        MediaRecipientSelectorListAdapter adapter = new MediaRecipientSelectorListAdapter(this, selections);
        recipientsListView.setAdapter(adapter);
    }
}
