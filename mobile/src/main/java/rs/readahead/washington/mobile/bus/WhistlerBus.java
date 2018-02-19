package rs.readahead.washington.mobile.bus;

import android.widget.Toast;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;
import rs.readahead.washington.mobile.R;
import rs.readahead.washington.mobile.bus.event.PendingFormQueueAddedEvent;
import rs.readahead.washington.mobile.bus.event.RawMediaFileQueueAddedEvent;
import rs.readahead.washington.mobile.bus.event.TrainModuleDownloadErrorEvent;
import rs.readahead.washington.mobile.bus.event.TrainingModuleDownloadedEvent;
import rs.readahead.washington.mobile.util.jobs.MediaFileUploadJob;
import rs.readahead.washington.mobile.util.jobs.PendingFormSendJob;


public class WhistlerBus extends RxBus {
    private final CompositeDisposable disposables;


    private WhistlerBus() {
        disposables = new CompositeDisposable();
    }

    public static WhistlerBus create() {
        WhistlerBus bus = new WhistlerBus();

        // application lifecycle events and handlers..
        bus.wireApplicationEvents();

        return bus;
    }

    public EventCompositeDisposable createCompositeDisposable() {
        return new EventCompositeDisposable(this);
    }

    private void wireApplicationEvents() {
        disposables.add(observe(RawMediaFileQueueAddedEvent.class)
                .subscribeOn(Schedulers.io()) // hmm..
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(new EventObserver<RawMediaFileQueueAddedEvent>() {
                    @Override
                    public void onNext(RawMediaFileQueueAddedEvent value) {
                        MediaFileUploadJob.scheduleJob();
                    }
                })
        );

        disposables.add(observe(PendingFormQueueAddedEvent.class)
                .subscribeOn(Schedulers.io()) // hmm..
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(new EventObserver<PendingFormQueueAddedEvent>() {
                    @Override
                    public void onNext(PendingFormQueueAddedEvent value) {
                        PendingFormSendJob.scheduleJob();
                    }
                })
        );

        disposables.add(observe(TrainingModuleDownloadedEvent.class)
                .subscribeOn(AndroidSchedulers.mainThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(new EventObserver<TrainingModuleDownloadedEvent>() {
                    @Override
                    public void onNext(TrainingModuleDownloadedEvent event) {
                        Toast.makeText(event.getContext(),
                                R.string.ra_train_module_downloaded, Toast.LENGTH_SHORT).show();
                    }
                })
        );

        disposables.add(observe(TrainModuleDownloadErrorEvent.class)
                .subscribeOn(AndroidSchedulers.mainThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(new EventObserver<TrainModuleDownloadErrorEvent>() {
                    @Override
                    public void onNext(TrainModuleDownloadErrorEvent event) {
                        Toast.makeText(event.getContext(),
                                event.getError(), Toast.LENGTH_SHORT).show();
                    }
                })
        );
    }
}
