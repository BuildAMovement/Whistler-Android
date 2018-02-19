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
import rs.readahead.washington.mobile.domain.entity.RawMediaFile;


public class EvidenceQueue implements ObjectQueue<RawMediaFile> {
    private final ObjectQueue<RawMediaFile> delegate;


    private EvidenceQueue(ObjectQueue<RawMediaFile> delegate) {
        this.delegate = delegate;
    }

    public static EvidenceQueue create(final Context context, final Gson gson) {
        // not much sense to this, but it keeps StrictMode quiet
        return Single.fromCallable(new Callable<EvidenceQueue>() {
                    @Override
                    public EvidenceQueue call() throws Exception {
                        File file = new File(context.getFilesDir(), context.getString(R.string.evidence_queue_file));
                        FileObjectQueue.Converter<RawMediaFile> converter = new GsonConverter<>(gson, RawMediaFile.class);
                        FileObjectQueue<RawMediaFile> delegate;

                        try {
                            delegate = new FileObjectQueue<>(file, converter);
                        } catch (IOException e) {
                            throw new RuntimeException("Unable to create file queue.", e);
                        }

                        return new EvidenceQueue(delegate);
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
    public void add(RawMediaFile entry) {
        synchronized (EvidenceQueue.class) {
            delegate.add(entry);
        }
    }

    @Override
    public RawMediaFile peek() {
        synchronized (EvidenceQueue.class) {
            return delegate.peek();
        }
    }

    @Override
    public void remove() {
        synchronized (EvidenceQueue.class) {
            delegate.remove();
        }
    }

    @Override
    public void setListener(Listener<RawMediaFile> listener) {
        synchronized (EvidenceQueue.class) {
            delegate.setListener(listener);
        }
    }
}
