package rs.readahead.washington.mobile.bus.event;

import android.content.Context;
import android.net.Uri;

import rs.readahead.washington.mobile.bus.IEvent;


public class ShowEvidenceEvent implements IEvent {
    private Context context;
    private Uri uri;

    public ShowEvidenceEvent(Context context, Uri uri) {
        this.context = context;
        this.uri = uri;
    }

    public Context getContext() {
        return context;
    }

    public Uri getUri() {
        return uri;
    }
}
