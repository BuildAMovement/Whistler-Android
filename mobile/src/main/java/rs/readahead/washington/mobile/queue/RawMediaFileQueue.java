package rs.readahead.washington.mobile.queue;

import android.content.Context;

import com.google.gson.Gson;
import com.squareup.tape.FileObjectQueue;
import com.squareup.tape.ObjectQueue;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.Callable;

import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;
import rs.readahead.washington.mobile.R;
import rs.readahead.washington.mobile.bus.RxBus;
import rs.readahead.washington.mobile.bus.event.RawMediaFileQueueAddedEvent;
import rs.readahead.washington.mobile.domain.entity.RawMediaFile;


public class RawMediaFileQueue implements ObjectQueue<RawMediaFile> {
    private final RxBus bus;
    private final ObjectQueue<RawMediaFile> delegate;


    private RawMediaFileQueue(ObjectQueue<RawMediaFile> delegate, RxBus bus) {
        this.delegate = delegate;
        this.bus = bus;
    }

    public static RawMediaFileQueue create(final Context context, final Gson gson, final RxBus bus) {
        // not much sense to this, but it keeps StrictMode quiet
        return Single.fromCallable(new Callable<RawMediaFileQueue>() {
                    @Override
                    public RawMediaFileQueue call() throws Exception {
                        File file = new File(context.getFilesDir(), context.getString(R.string.ra_media_file_queue_file));
                        FileObjectQueue.Converter<RawMediaFile> converter = new GsonConverter<>(gson, RawMediaFile.class);
                        FileObjectQueue<RawMediaFile> delegate;

                        try {
                            delegate = new FileObjectQueue<>(file, converter);
                        } catch (IOException e) {
                            throw new RuntimeException("Unable to create file queue.", e);
                        }

                        return new RawMediaFileQueue(delegate, bus);
                    }
                })
                .subscribeOn(Schedulers.io())
                .blockingGet();
    }

    @Override
    public int size() {
        synchronized (RawMediaFileQueue.class) {
            return delegate.size();
        }
    }

    @Override
    public void add(RawMediaFile entry) {
        synchronized (RawMediaFileQueue.class) {
            delegate.add(entry);
        }
    }

    @Override
    public RawMediaFile peek() {
        synchronized (RawMediaFileQueue.class) {
            return delegate.peek();
        }
    }

    @Override
    public void remove() {
        synchronized (RawMediaFileQueue.class) {
            delegate.remove();
        }
    }

    @Override
    public void setListener(Listener<RawMediaFile> listener) {
        synchronized (RawMediaFileQueue.class) {
            delegate.setListener(listener);
        }
    }

    public void addAndStartUpload(RawMediaFile entry) {
        add(entry);
        bus.post(new RawMediaFileQueueAddedEvent(size()));
    }
}
