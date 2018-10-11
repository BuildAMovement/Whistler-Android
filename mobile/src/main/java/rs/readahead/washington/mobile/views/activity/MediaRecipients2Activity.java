package rs.readahead.washington.mobile.views.activity;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;
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
import rs.readahead.washington.mobile.data.database.DataSource;
import rs.readahead.washington.mobile.domain.entity.MediaRecipient;
import rs.readahead.washington.mobile.domain.entity.MediaRecipientList;
import rs.readahead.washington.mobile.util.DialogsUtil;
import rs.readahead.washington.mobile.views.adapters.MediaRecipientListAdapter;
import rs.readahead.washington.mobile.views.adapters.MediaRecipientListListAdapter;
import rs.readahead.washington.mobile.views.adapters.ViewPagerAdapter;
import rs.readahead.washington.mobile.views.dialog.Dialogs;
import rs.readahead.washington.mobile.views.interfaces.IRecipientListsHandler;
import rs.readahead.washington.mobile.views.interfaces.IRecipientsHandler;


public class MediaRecipients2Activity extends BaseActivity implements
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
            actionBar.setTitle(R.string.title_activity_recipients);
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
                    showRecipientDialog(null);
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
        MyApplication.startLockScreenActivity(this);
        finish();
    }

    @Override
    public void onCacheWordLocked() {
        MyApplication.startLockScreenActivity(this);
        finish();
    }

    @Override
    public void onCacheWordOpened() {
        dataSource = DataSource.getInstance(this, cacheWord.getEncryptionKey());
        initializeFragments();
    }

    @Override
    public void removeMediaRecipient(@NonNull final MediaRecipient recipient) {
        showRemoveRecipientUI(recipient);
    }

    @Override
    public void updateMediaRecipient(@NonNull final MediaRecipient recipient) {
        showRecipientDialog(recipient);
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

    private void showRecipientDialog(final MediaRecipient mediaRecipient) {
        dialog = Dialogs.showRecipientDialog(this, mediaRecipient, new Dialogs.IRecipientDialogListener() {
            @Override
            public void call(MediaRecipient recipient) {
                if (mediaRecipient == null) {
                    insertMediaRecipient(recipient);
                } else {
                    changeMediaRecipient(recipient);
                }
            }
        });
    }

    void showAddRecipientListUI() {
        dialog = Dialogs.showAddRecipientListDialog(this, new Dialogs.IRecipientListDialogListener() {
            @Override
            public void call(MediaRecipientList recipientList) {
                insertMediaRecipientList(recipientList);
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

    void insertMediaRecipient(@NonNull final MediaRecipient recipient) {
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

    void insertMediaRecipientList(@NonNull MediaRecipientList mediaRecipientList) {
        dataSource.insertMediaRecipientList(mediaRecipientList);
        MyApplication.bus().post(new MediaRecipientListAddedEvent(mediaRecipientList));
    }

    public static class RecipientsFragment extends Fragment {
        @BindView(R.id.recipients)
        ListView listView;
        @BindView(R.id.blank_recipients_info)
        TextView blankRecipientsInfo;

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

            if (!initialized) {
                ((MediaRecipients2Activity) getActivity()).sendMediaRecipientsReadyEvent();
            }
        }

        @Override
        public void onDestroyView() {
            super.onDestroyView();

            if (!disposables.isDisposed()) {
                disposables.dispose();
            }

            unbinder.unbind();
        }

        void initialize(IRecipientsHandler recipientsHandler, List<MediaRecipient> mediaRecipients) {
            this.recipientsHandler = recipientsHandler;
            this.mediaRecipients = mediaRecipients;
            setBlankRecipientsInfo();
            updateRecipientsLayout();
        }

        void removeMediaRecipient(MediaRecipient mediaRecipient) {
            getAdapter().remove(mediaRecipient);
            showToast(R.string.removed_recipient);
            setBlankRecipientsInfo();
        }

        void addMediaRecipient(MediaRecipient mediaRecipient) {
            getAdapter().add(mediaRecipient);
            showToast(R.string.recipient_added);
            setBlankRecipientsInfo();
        }

        private void setBlankRecipientsInfo() {
            blankRecipientsInfo.setVisibility(mediaRecipients.isEmpty() ? View.VISIBLE : View.GONE);
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
        @BindView(R.id.blank_recipient_lists_info)
        TextView blankListsInfo;

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
                            if (recipientListsHandler != null) {
                                recipientListsHandler.updateMediaRecipientList(event.getMediaRecipientList());
                            }
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

            if (!initialized) {
                ((MediaRecipients2Activity) getActivity()).sendMediaRecipientListsReadyEvent();
            } else {
                getAdapter().notifyDataSetChanged();
            }
        }

        @Override
        public void onDestroyView() {
            super.onDestroyView();

            if (!disposables.isDisposed()) {
                disposables.dispose();
            }

            unbinder.unbind();
        }

        public void initialize(IRecipientListsHandler recipientListsHandler, List<MediaRecipientList> mediaRecipientLists) {
            this.recipientListsHandler = recipientListsHandler;
            this.mediaRecipientLists = mediaRecipientLists;
            updateRecipientListsLayout();
            setBlankRecipientListInfo();
        }

        void removeMediaRecipientList(MediaRecipientList mediaRecipientList) {
             getAdapter().remove(mediaRecipientList);
            showToast(R.string.removed_recipient_list);
            setBlankRecipientListInfo();
        }

        void addMediaRecipientList(MediaRecipientList mediaRecipientList) {
            getAdapter().add(mediaRecipientList);
            showToast(R.string.recipient_list_added);
            setBlankRecipientListInfo();
        }

        void updateMediaRecipientList(MediaRecipientList mediaRecipientList) {
            for (int i = 0, count = getAdapter().getCount(); i < count; i++) {
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

        public void setBlankRecipientListInfo() {
            blankListsInfo.setVisibility(mediaRecipientLists.isEmpty() ? View.VISIBLE : View.GONE);
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
