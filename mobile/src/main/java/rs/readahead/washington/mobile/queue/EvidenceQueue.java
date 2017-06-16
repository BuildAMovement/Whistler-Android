package rs.readahead.washington.mobile.queue;

import android.content.Context;

import com.google.gson.Gson;
import com.squareup.tape.FileObjectQueue;
import com.squareup.tape.ObjectQueue;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.Callable;

import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import rs.readahead.washington.mobile.R;
import rs.readahead.washington.mobile.domain.entity.Evidence;
import rs.readahead.washington.mobile.bus.event.EvidenceQueueAddedEvent;
import rs.readahead.washington.mobile.bus.event.EvidenceQueueRemovedEvent;
import rs.readahead.washington.mobile.bus.RxBus;
import timber.log.Timber;


public class EvidenceQueue implements ObjectQueue<Evidence> {
    private final RxBus bus;
    private final ObjectQueue<Evidence> delegate;


    private EvidenceQueue(ObjectQueue<Evidence> delegate, RxBus bus) {
        this.delegate = delegate;
        this.bus = bus;
    }

    public static EvidenceQueue create(final Context context, final Gson gson, final RxBus bus) {
        // not much sense to this, but it keeps StrictMode quiet
        return Single.fromCallable(new Callable<EvidenceQueue>() {
                    @Override
                    public EvidenceQueue call() throws Exception {
                        File file = new File(context.getFilesDir(), context.getString(R.string.evidence_queue_file));
                        FileObjectQueue.Converter<Evidence> converter = new GsonConverter<>(gson, Evidence.class);
                        FileObjectQueue<Evidence> delegate;

                        try {
                            delegate = new FileObjectQueue<>(file, converter);
                        } catch (IOException e) {
                            throw new RuntimeException("Unable to create file queue.", e);
                        }

                        return new EvidenceQueue(delegate, bus);
                    }
                })
                .subscribeOn(Schedulers.io())
                .blockingGet();
    }

    @Override
    public int size() {
        synchronized (EvidenceQueue.class) {
            return delegate.size();
        }
    }

    @Override
    public void add(Evidence entry) {
        synchronized (EvidenceQueue.class) {
            delegate.add(entry);
        }
        bus.post(buildAddedEvent());
    }

    @Override
    public Evidence peek() {
        synchronized (EvidenceQueue.class) {
            return delegate.peek();
        }
    }

    @Override
    public void remove() {
        synchronized (EvidenceQueue.class) {
            delegate.remove();
        }
        bus.post(buildRemovedEvent());
    }

    @Override
    public void setListener(Listener<Evidence> listener) {
        synchronized (EvidenceQueue.class) {
            delegate.setListener(listener);
        }
    }

    private EvidenceQueueAddedEvent buildAddedEvent() {
        return new EvidenceQueueAddedEvent(size());
    }

    private EvidenceQueueRemovedEvent buildRemovedEvent() {
        return new EvidenceQueueRemovedEvent(size());
    }
}
