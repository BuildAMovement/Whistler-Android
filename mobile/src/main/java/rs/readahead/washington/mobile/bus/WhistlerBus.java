package rs.readahead.washington.mobile.bus;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;
import rs.readahead.washington.mobile.bus.event.EvidenceQueueAddedEvent;
import rs.readahead.washington.mobile.bus.event.ShowEvidenceEvent;
import rs.readahead.washington.mobile.domain.interactor.DefaultObserver;
import rs.readahead.washington.mobile.util.CommonUtils;
import rs.readahead.washington.mobile.util.upload.EvidenceUploadJob;


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
        disposables.add(observe(EvidenceQueueAddedEvent.class)
                .subscribeOn(Schedulers.io()) // hmm..
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(new DefaultObserver<EvidenceQueueAddedEvent>() {
                    @Override
                    public void onNext(EvidenceQueueAddedEvent value) {
                        EvidenceUploadJob.scheduleJob();
                    }
                })
        );

        disposables.add(observe(ShowEvidenceEvent.class)
                .subscribeOn(AndroidSchedulers.mainThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(new DefaultObserver<ShowEvidenceEvent>() {
                    @Override
                    public void onNext(ShowEvidenceEvent event) {
                        CommonUtils.openUri(event.getContext(), event.getUri());
                    }
                })
        );
    }
}
