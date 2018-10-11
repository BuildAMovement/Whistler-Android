package rs.readahead.washington.mobile.mvp.presenter;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.net.Uri;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.telephony.SmsManager;
import android.text.TextUtils;
import android.widget.Toast;

import java.util.List;

import info.guardianproject.cacheword.CacheWordHandler;
import io.reactivex.Completable;
import io.reactivex.CompletableSource;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;
import rs.readahead.washington.mobile.BuildConfig;
import rs.readahead.washington.mobile.MyApplication;
import rs.readahead.washington.mobile.R;
import rs.readahead.washington.mobile.data.database.CacheWordDataSource;
import rs.readahead.washington.mobile.data.database.DataSource;
import rs.readahead.washington.mobile.data.sharedpref.Preferences;
import rs.readahead.washington.mobile.data.sharedpref.SharedPrefs;
import rs.readahead.washington.mobile.media.MediaFileHandler;
import rs.readahead.washington.mobile.mvp.contract.IHomeScreenPresenterContract;
import rs.readahead.washington.mobile.util.C;
import rs.readahead.washington.mobile.util.LocationProvider;
import rs.readahead.washington.mobile.util.LocationUtil;
import rs.readahead.washington.mobile.util.ThreadUtil;
import rs.readahead.washington.mobile.util.TrainModuleHandler;


public class HomeScreenPresenter implements IHomeScreenPresenterContract.IPresenter {
    private IHomeScreenPresenterContract.IView view;
    private CacheWordHandler cacheWordHandler;
    private CacheWordDataSource cacheWordDataSource;
    private CompositeDisposable disposable;
    private final Context appContext;
    private String smsMessage;


    public HomeScreenPresenter(IHomeScreenPresenterContract.IView view, CacheWordHandler cacheWordHandler) {
        this.view = view;
        this.cacheWordHandler = cacheWordHandler;
        appContext = view.getContext().getApplicationContext();
        cacheWordDataSource = new CacheWordDataSource(appContext);
        disposable = new CompositeDisposable();
    }

    @Override
    public void executePanicMode() {
        cacheWordDataSource.getDataSource()
                .subscribeOn(Schedulers.io())
                .flatMapCompletable(new Function<DataSource, CompletableSource>() {
                    @Override
                    public CompletableSource apply(DataSource dataSource) {
                        List<String> phoneNumbers = dataSource.getTrustedPhones();

                        if (phoneNumbers.size() > 0) {
                            String panicMessage = SharedPrefs.getInstance().getPanicMessage();

                            smsMessage = TextUtils.isEmpty(panicMessage) ?
                                    appContext.getResources().getString(R.string.default_panic_message) : panicMessage;
                            smsMessage += "\n\n" + appContext.getResources().getString(R.string.location_info) + "\n";

                            sendPanicMessagesOnMain(phoneNumbers, Preferences.isPanicGeolocationActive());
                        }

                        if (Preferences.isEraseEverything()) {
                            MediaFileHandler.destroyGallery(appContext);
                            TrainModuleHandler.destroyTrainingModules(appContext);
                            dataSource.deleteDatabase();
                        } else {
                            if (SharedPrefs.getInstance().isEraseGalleryActive()) {
                                MediaFileHandler.destroyGallery(appContext);
                                dataSource.deleteMediaFiles();
                            }

                            if (SharedPrefs.getInstance().isEraseMaterialsActive()) {
                                TrainModuleHandler.destroyTrainingModules(appContext);
                                dataSource.deleteTrainModules();
                            }

                            if (SharedPrefs.getInstance().isEraseContactsActive()) {
                                dataSource.deleteContacts();
                            }

                            if (SharedPrefs.getInstance().isEraseMediaRecipientsActive()) {
                                dataSource.deleteMediaRecipients();
                            }

                            if (Preferences.isEraseReports()) {
                                dataSource.deleteReports();
                            }

                            if (Preferences.isEraseForms()) {
                                dataSource.deleteForms();
                            }
                        }

                        clearSharedPreferences();

                        MyApplication.exit(view.getContext());

                        lockCacheWord();

                        if (Preferences.isUninstallOnPanic()) {
                            uninstallWhistler(view.getContext());
                        }

                        return Completable.complete();
                    }
                })
                .blockingAwait();
    }

    @Override
    public void destroy() {
        disposable.dispose();
        cacheWordDataSource.dispose();
        view = null;
        cacheWordHandler = null;
    }

    private void lockCacheWord() {
        if (cacheWordHandler != null && !cacheWordHandler.isLocked()) {
            cacheWordHandler.lock();
        }
    }

    private void clearSharedPreferences() {
        SharedPrefs.getInstance().setPanicMessage(null);
    }

    private void sendPanicMessagesOnMain(final List<String> phoneNumbers, final boolean isPanicGeolocationActive) {
        ThreadUtil.runOnMain(new Runnable() {
            @Override
            public void run() {
                sendPanicMessages(phoneNumbers, isPanicGeolocationActive);
            }
        });
    }

    private void sendPanicMessages(final List<String> phoneNumbers, boolean isPanicGeolocationActive) {
        if (isPanicGeolocationActive) {
            LocationProvider.requestSingleUpdate(appContext, new LocationProvider.LocationCallback() {
                @Override
                public void onNewLocationAvailable(@Nullable Location location) {
                    if (location != null) {
                        smsMessage += LocationUtil.getLocationData(location);
                    } else {
                        smsMessage += appContext.getResources().getString(R.string.unknown);
                    }
                    sendSMS(phoneNumbers);
                }
            });
        } else {
            smsMessage += appContext.getResources().getString(R.string.unknown);
            sendSMS(phoneNumbers);
        }
    }

    private void sendSMS(final List<String> phoneNumbers) {
        final String phoneNumber = phoneNumbers.get(0);

        final PendingIntent sentPI = PendingIntent.getBroadcast(appContext, 0,
                new Intent(C.SMS_SENT), 0);

        final PendingIntent deliveredPI = PendingIntent.getBroadcast(appContext, 0,
                new Intent(C.SMS_DELIVERED), 0);

        appContext.registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context arg0, Intent arg1) {
                switch (getResultCode()) {
                    case Activity.RESULT_OK:
                        appContext.unregisterReceiver(this);

                        phoneNumbers.remove(phoneNumber);
                        if (phoneNumbers.size() > 0) {
                            sendSMS(phoneNumbers);
                            break;
                        } else {
                            showToast(R.string.panic_sent);
                            break;
                        }
                    case SmsManager.RESULT_ERROR_GENERIC_FAILURE:
                        showToast(R.string.panic_sent_error_generic);
                        break;
                    case SmsManager.RESULT_ERROR_NO_SERVICE:
                        showToast(R.string.panic_sent_error_service);
                        break;
                    case SmsManager.RESULT_ERROR_NULL_PDU:
                        showToast(R.string.panic_sent_error_service);
                        break;
                    case SmsManager.RESULT_ERROR_RADIO_OFF:
                        showToast(R.string.panic_sent_error_service);
                        break;
                }

            }
        }, new IntentFilter(C.SMS_SENT));

        SmsManager sms = SmsManager.getDefault();
        sms.sendTextMessage(phoneNumber, null, smsMessage, sentPI, deliveredPI);
    }

    private void showToast(@StringRes int resId) {
        Toast.makeText(appContext, appContext.getString(resId), Toast.LENGTH_SHORT).show();
    }

    private void uninstallWhistler(Context context) {
        Uri packageUri = Uri.parse("package:" + BuildConfig.APPLICATION_ID);
        Intent intent = new Intent(Intent.ACTION_UNINSTALL_PACKAGE, packageUri);
        context.startActivity(intent);
    }
}
