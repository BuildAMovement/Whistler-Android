package rs.readahead.washington.mobile.media.exo;

import android.content.Context;
import android.support.annotation.NonNull;

import com.google.android.exoplayer2.upstream.DataSource;

import rs.readahead.washington.mobile.domain.entity.MediaFile;


public class MediaFileDataSourceFactory implements DataSource.Factory {
    private final Context context;
    private final MediaFile mediaFile;


    public MediaFileDataSourceFactory(@NonNull Context context, @NonNull MediaFile mediaFile) {
        this.mediaFile = mediaFile;
        this.context = context;
    }

    @Override
    public DataSource createDataSource() {
        return new MediaFileDataSource(context, mediaFile);
    }
}
