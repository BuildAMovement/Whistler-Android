package rs.readahead.washington.mobile.views.activity;

import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.TabLayout;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import info.guardianproject.cacheword.CacheWordHandler;
import info.guardianproject.cacheword.ICacheWordSubscriber;
import rs.readahead.washington.mobile.MyApplication;
import rs.readahead.washington.mobile.R;
import rs.readahead.washington.mobile.bus.EventCompositeDisposable;
import rs.readahead.washington.mobile.bus.EventObserver;
import rs.readahead.washington.mobile.bus.event.MediaRecipientAddedEvent;
import rs.readahead.washington.mobile.bus.event.MediaRecipientListAddedEvent;
import rs.readahead.washington.mobile.bus.event.MediaRecipientListRemovedEvent;
import rs.readahead.washington.mobile.bus.event.MediaRecipientListUpdatedEvent;
import rs.readahead.washington.mobile.bus.event.MediaRecipientListsReadyEvent;
import rs.readahead.washington.mobile.bus.event.MediaRecipientRemovedEvent;
import rs.readahead.washington.mobile.bus.event.MediaRecipientUpdatedEvent;
import rs.readahead.washington.mobile.bus.event.MediaRecipientsReadyEvent;
import rs.readahead.washington.mobile.database.DataSource;
import rs.readahead.washington.mobile.models.MediaRecipient;
import rs.readahead.washington.mobile.models.MediaRecipientList;
import rs.readahead.washington.mobile.util.DialogsUtil;
import rs.readahead.washington.mobile.util.StringUtils;
import rs.readahead.washington.mobile.views.adapters.MediaRecipientListAdapter;
import rs.readahead.washington.mobile.views.adapters.MediaRecipientListListAdapter;
import rs.readahead.washington.mobile.views.adapters.ViewPagerAdapter;
import rs.readahead.washington.mobile.views.interfaces.IRecipientListsHandler;
import rs.readahead.washington.mobile.views.interfaces.IRecipientsHandler;


public class MediaRecipients2Activity extends AppCompatActivity implements
        ICacheWordSubscriber, IRecipientsHandler, IRecipientListsHandler {
    private ViewPager mViewPager;

    private DataSource dataSource;
    private CacheWordHandler cacheWord;
    private List<MediaRecipient> recipientList;
    private List<MediaRecipientList> mediaRecipientLists;

    private AlertDialog dialog;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_media_recipients2);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        ViewPagerAdapter adapter = new ViewPagerAdapter(getSupportFragmentManager());
        adapter.addFragment(RecipientsFragment.newInstance(), getString(R.string.recipients));
        adapter.addFragment(RecipientListsFragment.newInstance(), getString(R.string.lists));

        mViewPager = (ViewPager) findViewById(R.id.container);
        mViewPager.setAdapter(adapter);

        TabLayout tabLayout = (TabLayout) findViewById(R.id.tabs);
        tabLayout.setupWithViewPager(mViewPager);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mViewPager.getCurrentItem() == 0) { // fixed..
                    showUpdateRecipientUI(null);
                } else if (mViewPager.getCurrentItem() == 1) {
                    showAddRecipientListUI();
                }
            }
        });

        cacheWord = new CacheWordHandler(this);
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

        if (dialog != null) {
            dialog.dismiss();
        }
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
        dataSource = DataSource.getInstance(cacheWord, getApplicationContext());
        initializeFragments();
    }

    @Override
    public void removeMediaRecipient(@NonNull final MediaRecipient recipient) {
        showRemoveRecipientUI(recipient);
    }

    @Override
    public void updateMediaRecipient(@NonNull final MediaRecipient recipient) {
        showUpdateRecipientUI(recipient);
    }

    @Override
    public void removeMediaRecipientList(@NonNull final MediaRecipientList list) {
        showRemoveRecipientListUI(list);
    }

    @Override
    public void updateMediaRecipientList(@NonNull final MediaRecipientList list) {
        showUpdateRecipientListUI(list);
    }

    private void initializeFragments() {
        recipientList = dataSource.getMediaRecipients();
        sendMediaRecipientsReadyEvent();

        mediaRecipientLists = dataSource.getMediaRecipientLists();
        sendMediaRecipientListsReadyEvent();
    }

    public void sendMediaRecipientsReadyEvent() {
        if (recipientList != null) {
            MyApplication.bus().post(new MediaRecipientsReadyEvent(this, recipientList));
        }
    }

    public void sendMediaRecipientListsReadyEvent() {
        if (mediaRecipientLists != null) {
            MyApplication.bus().post(new MediaRecipientListsReadyEvent(this, mediaRecipientLists));
        }
    }

    private void showRemoveRecipientUI(@NonNull final MediaRecipient mediaRecipient) {
        String formattedTitle = String.format(getString(R.string.recipient_remove_all_text), mediaRecipient.getTitle());

        dialog = DialogsUtil.showMessageOKCancelWithTitle(this,
                formattedTitle,
                getString(R.string.attention),
                getString(R.string.ok),
                getString(R.string.cancel),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        deleteMediaRecipient(mediaRecipient);
                        dialog.dismiss();
                    }
                }, null);
    }

    private void showRemoveRecipientListUI(@NonNull final MediaRecipientList list) {
        String formattedTitle = String.format(getString(R.string.recipient_list_remove_all_text), list.getTitle());

        dialog = DialogsUtil.showMessageOKCancelWithTitle(this,
                formattedTitle,
                getString(R.string.attention),
                getString(R.string.ok),
                getString(R.string.cancel),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        deleteMediaRecipientList(list);
                        dialog.dismiss();
                    }
                }, null);
    }

    private void showUpdateRecipientUI(final MediaRecipient mediaRecipient) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        @SuppressLint("InflateParams")
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_create_media_recipient, null);

        final TextInputLayout titleLayout = (TextInputLayout) dialogView.findViewById(R.id.recipient_title_layout);
        final EditText title = (EditText) dialogView.findViewById(R.id.recipient_title);
        final TextInputLayout emailLayout = (TextInputLayout) dialogView.findViewById(R.id.recipient_email_layout);
        final EditText email = (EditText) dialogView.findViewById(R.id.recipient_email);

        assert titleLayout != null;
        assert emailLayout != null;

        if (mediaRecipient != null) {
            title.setText(mediaRecipient.getTitle());
            email.setText(mediaRecipient.getMail());
        }

        titleLayout.setError(null);
        emailLayout.setError(null);

        builder.setView(dialogView);
        builder.setTitle(mediaRecipient != null ? R.string.update_recipient : R.string.add_new_recipient);
        builder.setCancelable(true);
        builder.setPositiveButton(getString(mediaRecipient != null ? R.string.action_update : R.string.action_add), null);
        builder.setNegativeButton(getString(R.string.cancel), null);

        dialog = builder.show();

        // implement this right after show to remove dismiss dialog on click
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String titleText = title.getText().toString();
                String emailText = email.getText().toString();

                if (validateValues(titleText, emailText)) {
                    return;
                }

                if (mediaRecipient == null) {
                    insertMediaRecipient(titleText, emailText);
                } else {
                    mediaRecipient.setTitle(titleText);
                    mediaRecipient.setMail(emailText);
                    changeMediaRecipient(mediaRecipient);
                }

                dialog.dismiss();
            }

            private boolean validateValues(String titleText, String emailText) {
                boolean errors = false;

                if (titleText.length() == 0) {
                    titleLayout.setError(getString(R.string.empty_field_error));
                    errors = true;
                } else {
                    titleLayout.setError(null);
                }

                if (emailText.length() == 0) {
                    emailLayout.setError(getString(R.string.empty_field_error));
                    errors = true;
                } else if (! StringUtils.isEmailValid(emailText)) {
                    emailLayout.setError(getString(R.string.email_field_error));
                    errors = true;
                } else {
                    emailLayout.setError(null);
                }

                return errors;
            }
        });
    }

    void showAddRecipientListUI() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        @SuppressLint("InflateParams")
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_create_media_recipient_list, null);

        final TextInputLayout titleLayout = (TextInputLayout) dialogView.findViewById(R.id.recipient_title_layout);
        final EditText title = (EditText) dialogView.findViewById(R.id.recipient_title);

        assert titleLayout != null;

        titleLayout.setError(null);

        builder.setView(dialogView);
        builder.setTitle(R.string.add_new_recipient_list);
        builder.setCancelable(true);
        builder.setPositiveButton(getString(R.string.action_add), null);
        builder.setNegativeButton(getString(R.string.cancel), null);

        dialog = builder.show();

        // implemented this right after show to remove dismiss dialog on click
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String titleText = title.getText().toString();

                if (titleText.length() == 0) {
                    titleLayout.setError(getString(R.string.empty_field_error));
                    return;
                }

                insertMediaRecipientList(titleText);

                dialog.dismiss();
            }
        });
    }

    private void showUpdateRecipientListUI(@NonNull final MediaRecipientList mediaRecipientList) {
        Intent intent = new Intent(this, EditMediaRecipientListActivity.class);
        intent.putExtra(EditMediaRecipientListActivity.RECIPIENT_LIST_ID, mediaRecipientList.getId());
        startActivity(intent);
    }

    void deleteMediaRecipient(@NonNull final MediaRecipient recipient) {
        dataSource.deleteRecipient(recipient.getId());
        MyApplication.bus().post(new MediaRecipientRemovedEvent(recipient));
    }

    void insertMediaRecipient(@NonNull String title, @NonNull String email) {
        MediaRecipient recipient = new MediaRecipient(title, email);
        dataSource.insertRecipient(recipient);
        MyApplication.bus().post(new MediaRecipientAddedEvent(recipient));
    }

    void changeMediaRecipient(@NonNull final MediaRecipient mediaRecipient) {
        dataSource.updateRecipient(mediaRecipient);
        MyApplication.bus().post(new MediaRecipientUpdatedEvent());
    }

    void deleteMediaRecipientList(@NonNull final MediaRecipientList mediaRecipientList) {
        dataSource.deleteList(mediaRecipientList.getId());
        MyApplication.bus().post(new MediaRecipientListRemovedEvent(mediaRecipientList));
    }

    void insertMediaRecipientList(@NonNull String title) {
        MediaRecipientList mediaRecipientList = new MediaRecipientList(title);
        dataSource.insertMediaRecipientList(mediaRecipientList);
        MyApplication.bus().post(new MediaRecipientListAddedEvent(mediaRecipientList));
    }

    public static class RecipientsFragment extends Fragment {
        @BindView(R.id.recipients)
        ListView listView;

        private Unbinder unbinder;
        private IRecipientsHandler recipientsHandler;
        private List<MediaRecipient> mediaRecipients;
        private boolean initialized = false;
        private EventCompositeDisposable disposables;


        public RecipientsFragment() {
        }

        public static RecipientsFragment newInstance() {
            return new RecipientsFragment();
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_media_recipients2, container, false);
            unbinder = ButterKnife.bind(this, rootView);

            disposables = MyApplication.bus().createCompositeDisposable();
            disposables.wire(MediaRecipientsReadyEvent.class, new EventObserver<MediaRecipientsReadyEvent>() {
                @Override
                public void onNext(MediaRecipientsReadyEvent event) {
                    initialize(event.getRecipientsHandler(), event.getMediaRecipients());
                }
            })
            .wire(MediaRecipientAddedEvent.class, new EventObserver<MediaRecipientAddedEvent>() {
                @Override
                public void onNext(MediaRecipientAddedEvent event) {
                    addMediaRecipient(event.getMediaRecipient());
                }
            })
            .wire(MediaRecipientUpdatedEvent.class, new EventObserver<MediaRecipientUpdatedEvent>() {
                @Override
                public void onNext(MediaRecipientUpdatedEvent event) {
                    updateMediaRecipient();
                }
            })
            .wire(MediaRecipientRemovedEvent.class, new EventObserver<MediaRecipientRemovedEvent>() {
                @Override
                public void onNext(MediaRecipientRemovedEvent event) {
                    removeMediaRecipient(event.getMediaRecipient());
                }
            });

            return rootView;
        }

        @Override
        public void onStart() {
            super.onStart();

            if (! initialized) {
                ((MediaRecipients2Activity) getActivity()).sendMediaRecipientsReadyEvent();
            }
        }

        @Override
        public void onDestroyView() {
            super.onDestroyView();

            if (! disposables.isDisposed()) {
                disposables.dispose();
            }

            unbinder.unbind();
        }

        void initialize(IRecipientsHandler recipientsHandler, List<MediaRecipient> mediaRecipients) {
            this.recipientsHandler = recipientsHandler;
            this.mediaRecipients = mediaRecipients;
            updateRecipientsLayout();
        }

        void removeMediaRecipient(MediaRecipient mediaRecipient) {
            getAdapter().remove(mediaRecipient);
            showToast(R.string.removed_recipient);
        }

        void addMediaRecipient(MediaRecipient mediaRecipient) {
            getAdapter().add(mediaRecipient);
            showToast(R.string.recipient_added);
        }

        void updateMediaRecipient() {
            getAdapter().notifyDataSetChanged();
            showToast(R.string.recipient_updated);
        }

        private void updateRecipientsLayout() {
            if (initialized) return;

            MediaRecipientListAdapter adapter =
                    new MediaRecipientListAdapter(getContext(), recipientsHandler, mediaRecipients);
            listView.setAdapter(adapter);

            initialized = true;
        }

        private void showToast(@StringRes int resId) {
            Toast.makeText(getContext(), getString(resId), Toast.LENGTH_SHORT).show();
        }

        private MediaRecipientListAdapter getAdapter() {
            return (MediaRecipientListAdapter) listView.getAdapter();
        }
    }

    public static class RecipientListsFragment extends Fragment {
        @BindView(R.id.recipient_lists)
        ListView listView;

        private Unbinder unbinder;
        private IRecipientListsHandler recipientListsHandler;
        private List<MediaRecipientList> mediaRecipientLists;
        private View rootView;
        private boolean initialized;
        private EventCompositeDisposable disposables;


        public RecipientListsFragment() {
        }

        public static RecipientListsFragment newInstance() {
            return new RecipientListsFragment();
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            rootView = inflater.inflate(R.layout.fragment_media_recipient_lists2, container, false);
            unbinder = ButterKnife.bind(this, rootView);

            disposables = MyApplication.bus().createCompositeDisposable();
            disposables.wire(MediaRecipientListsReadyEvent.class, new EventObserver<MediaRecipientListsReadyEvent>() {
                @Override
                public void onNext(MediaRecipientListsReadyEvent event) {
                    initialize(event.getRecipientListsHandler(), event.getMediaRecipientLists());
                }
            })
            .wire(MediaRecipientListAddedEvent.class, new EventObserver<MediaRecipientListAddedEvent>() {
                @Override
                public void onNext(MediaRecipientListAddedEvent event) {
                    addMediaRecipientList(event.getMediaRecipientList());
                }
            })
            .wire(MediaRecipientListRemovedEvent.class, new EventObserver<MediaRecipientListRemovedEvent>() {
                @Override
                public void onNext(MediaRecipientListRemovedEvent event) {
                    removeMediaRecipientList(event.getMediaRecipientList());
                }
            })
            .wire(MediaRecipientListUpdatedEvent.class, new EventObserver<MediaRecipientListUpdatedEvent>() {
                @Override
                public void onNext(MediaRecipientListUpdatedEvent event) {
                    updateMediaRecipientList(event.getMediaRecipientList());
                }
            });

            return rootView;
        }

        @Override
        public void onStart() {
            super.onStart();

            if (! initialized) {
                ((MediaRecipients2Activity) getActivity()).sendMediaRecipientListsReadyEvent();
            } else {
                getAdapter().notifyDataSetChanged();
            }
        }

        @Override
        public void onDestroyView() {
            super.onDestroyView();

            if (! disposables.isDisposed()) {
                disposables.dispose();
            }

            unbinder.unbind();
        }

        public void initialize(IRecipientListsHandler recipientListsHandler, List<MediaRecipientList> mediaRecipientLists) {
            this.recipientListsHandler = recipientListsHandler;
            this.mediaRecipientLists = mediaRecipientLists;
            updateRecipientListsLayout();
        }

        void removeMediaRecipientList(MediaRecipientList mediaRecipientList) {
            getAdapter().remove(mediaRecipientList);
            showToast(R.string.removed_recipient_list);
        }

        void addMediaRecipientList(MediaRecipientList mediaRecipientList) {
            getAdapter().add(mediaRecipientList);
            showToast(R.string.recipient_list_added);
        }

        void updateMediaRecipientList(MediaRecipientList mediaRecipientList) {
            for(int i = 0, count = getAdapter().getCount(); i < count; i++) {
                MediaRecipientList current = getAdapter().getItem(i);

                // update object in adapter..
                if (current != null && current.getId() == mediaRecipientList.getId()) {
                    current.setTitle(mediaRecipientList.getTitle());
                    getAdapter().notifyDataSetChanged();
                    showToast(R.string.recipient_list_updated);
                    break;
                }
            }
        }

        private void updateRecipientListsLayout() {
            if (initialized) return;

            MediaRecipientListListAdapter adapter =
                    new MediaRecipientListListAdapter(getContext(), recipientListsHandler, mediaRecipientLists);
            listView.setAdapter(adapter);

            initialized = true;
        }

        private void showToast(@StringRes int resId) {
            Toast.makeText(getContext(), getString(resId), Toast.LENGTH_SHORT).show();
        }

        private MediaRecipientListListAdapter getAdapter() {
            return (MediaRecipientListListAdapter) listView.getAdapter();
        }
    }
}
