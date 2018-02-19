package rs.readahead.washington.mobile.views.adapters;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.graphics.drawable.VectorDrawableCompat;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import butterknife.BindView;
import butterknife.ButterKnife;
import rs.readahead.washington.mobile.R;
import rs.readahead.washington.mobile.domain.entity.MediaFile;
import rs.readahead.washington.mobile.media.MediaFileHandler;
import rs.readahead.washington.mobile.media.MediaFileUrlLoader;
import rs.readahead.washington.mobile.presentation.entity.EvidenceData;
import rs.readahead.washington.mobile.presentation.entity.MediaFileLoaderModel;
import rs.readahead.washington.mobile.presentation.entity.ReportViewType;
import rs.readahead.washington.mobile.util.CommonUtils;
import rs.readahead.washington.mobile.views.interfaces.IGalleryMediaHandler;


public class GalleryRecycleViewAdapter extends RecyclerView.Adapter<GalleryRecycleViewAdapter.ViewHolder> {
    private List<MediaFile> files = Collections.emptyList();
    private MediaFileUrlLoader glideLoader;
    private IGalleryMediaHandler galleryMediaHandler;
    private Set<MediaFile> selected;
    private ReportViewType type;

    public GalleryRecycleViewAdapter(Context context, IGalleryMediaHandler galleryMediaHandler,
            MediaFileHandler mediaFileHandler, ReportViewType type) {
        this.glideLoader = new MediaFileUrlLoader(context.getApplicationContext(), mediaFileHandler);
        this.galleryMediaHandler = galleryMediaHandler;
        this.selected = new HashSet<>();
        this.type = type;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.card_media_file, parent, false);
        return new ViewHolder(v, type);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, final int position) {
        final MediaFile mediaFile = files.get(position);

        holder.setRemoveButton();


        checkItemState(holder,mediaFile);
//        holder.checkBox.setChecked(selected.contains(mediaFile));

        String type = mediaFile.getPrimaryMimeType();

        if ("image".equals(type)) {
            holder.showImageInfo();
            Glide.with(holder.mediaView.getContext())
                    .using(glideLoader)
                    .load(new MediaFileLoaderModel(mediaFile, MediaFileLoaderModel.LoadType.THUMBNAIL))
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                    .skipMemoryCache(true)
                    .into(holder.mediaView);
        } else if ("audio".equals(type)) {
            holder.showAudioInfo(mediaFile);
            Drawable drawable = VectorDrawableCompat.create(holder.itemView.getContext().getResources(),
                    R.drawable.ic_mic_white, null);
            holder.mediaView.setImageDrawable(drawable);
        } else if ("video".equals(type)) {
            holder.showVideoInfo(mediaFile);
            Glide.with(holder.mediaView.getContext())
                    .using(glideLoader)
                    .load(new MediaFileLoaderModel(mediaFile, MediaFileLoaderModel.LoadType.THUMBNAIL))
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                    .skipMemoryCache(true)
                    .into(holder.mediaView);
        }

        holder.mediaView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                galleryMediaHandler.playMedia(mediaFile);
            }
        });

        holder.checkBox.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkboxClickHandler(holder, mediaFile);
            }
        });

        holder.removeFile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                selected.add(mediaFile);
                galleryMediaHandler.onSelectionNumChange(selected.size());
            }
        });
    }

    @Override
    public int getItemCount() {
        return files.size();
    }

    public void setFiles(List<MediaFile> files) {
        this.files = files;
        notifyDataSetChanged();
    }

    public List<MediaFile> getSelectedMediaFiles() {
        List<MediaFile> selectedList = new ArrayList<>(selected.size());
        selectedList.addAll(selected);

        return selectedList;
    }

    public EvidenceData getEvidenceFiles() {
        return new EvidenceData(files);
    }

    public void clearSelected() {
        selected.clear();
        galleryMediaHandler.onSelectionNumChange(selected.size());
    }

    public void clearSelectedEvidence() {
        selected.clear();
    }

    public void removeMediaFile(MediaFile mediaFile) {
        int position = files.indexOf(mediaFile);

        if (position != -1) {
            files.remove(position);
            notifyItemRemoved(position);
        }
    }

    private void checkboxClickHandler(ViewHolder holder, MediaFile mediaFile) {
        if (selected.contains(mediaFile)) {
            selected.remove(mediaFile);
        } else {
            selected.add(mediaFile);
        }

        checkItemState(holder,mediaFile);
        galleryMediaHandler.onSelectionNumChange(selected.size());
    }

    private void checkItemState(ViewHolder holder, MediaFile mediaFile) {
        boolean checked = selected.contains(mediaFile);
        holder.selectionDimmer.setVisibility(checked ? View.VISIBLE : View.GONE);
        holder.checkBox.setImageResource(checked ? R.drawable.ic_check_box_on : R.drawable.ic_check_box_off);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        @BindView(R.id.mediaView) ImageView mediaView;
        @BindView(R.id.checkBox) ImageView checkBox;
        @BindView(R.id.videoInfo) ViewGroup videoInfo;
        @BindView(R.id.videoDuration) TextView videoDuration;
        @BindView(R.id.audioInfo) ViewGroup audioInfo;
        @BindView(R.id.audioDuration) TextView audioDuration;
        @BindView(R.id.selectionDimmer) View selectionDimmer;
        @BindView(R.id.checkboxOuter) View checkboxOuter;
        @BindView(R.id.remove_file) ImageView removeFile;

        private ReportViewType type;


        public ViewHolder(View itemView, ReportViewType type) {
            super(itemView);
            this.type = type;
            ButterKnife.bind(this, itemView);
        }

        void showVideoInfo(MediaFile mediaFile) {
            audioInfo.setVisibility(View.GONE);
            videoInfo.setVisibility(View.VISIBLE);
            if (mediaFile.getDuration() > 0) {
                videoDuration.setText(getDuration(mediaFile));
                videoDuration.setVisibility(View.VISIBLE);
            } else {
                videoDuration.setVisibility(View.INVISIBLE);
            }
        }

        void showAudioInfo(MediaFile mediaFile) {
            videoInfo.setVisibility(View.GONE);
            audioInfo.setVisibility(View.VISIBLE);
            if (mediaFile.getDuration() > 0) {
                audioDuration.setText(getDuration(mediaFile));
                audioDuration.setVisibility(View.VISIBLE);
            } else {
                audioDuration.setVisibility(View.INVISIBLE);
            }
        }

        void showImageInfo() {
            videoInfo.setVisibility(View.GONE);
            audioInfo.setVisibility(View.GONE);
        }

        void setRemoveButton() {
            if (type == null) return;
            checkBox.setVisibility(View.GONE);
            removeFile.setVisibility(type == ReportViewType.PREVIEW ? View.GONE : View.VISIBLE);
        }

        private String getDuration(MediaFile mediaFile) {
            return CommonUtils.getVideoDuration((int) (mediaFile.getDuration() / 1000));
        }
    }
}
