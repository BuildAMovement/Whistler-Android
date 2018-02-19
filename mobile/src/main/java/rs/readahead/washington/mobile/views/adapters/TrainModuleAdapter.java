package rs.readahead.washington.mobile.views.adapters;

import android.support.annotation.DrawableRes;
import android.support.v7.content.res.AppCompatResources;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import rs.readahead.washington.mobile.MyApplication;
import rs.readahead.washington.mobile.R;
import rs.readahead.washington.mobile.bus.event.DeleteTrainModuleEvent;
import rs.readahead.washington.mobile.bus.event.TrainModuleClickedEvent;
import rs.readahead.washington.mobile.domain.entity.TrainModule;
import rs.readahead.washington.mobile.presentation.entity.DownloadState;
import rs.readahead.washington.mobile.util.StringUtils;


public class TrainModuleAdapter extends RecyclerView.Adapter<TrainModuleAdapter.ViewHolder> {
    private List<TrainModule> trainModules;


    public TrainModuleAdapter(List<TrainModule> TrainModules) {
        this.trainModules = TrainModules;
    }

    @Override
    public TrainModuleAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.train_session_row, parent, false);
        return new TrainModuleAdapter.ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        final TrainModule module = trainModules.get(position);

        holder.mTitle.setText(module.getName());
        holder.mOrganization.setText(module.getOrganization());
        holder.mType.setText(module.getType());
        holder.mSize.setText(StringUtils.getFileSize(module.getSize()));

        if (module.getDownloaded() == DownloadState.DOWNLOADED) {
            holder.setDownloadedIcon();
        } else if (module.getDownloaded() == DownloadState.DOWNLOADING) {
            holder.setDownloadingIcon();
        } else {
            holder.hideDownloadIcon();
        }

        holder.moreOption.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PopupMenu popup = new PopupMenu(holder.moduleRow.getContext(), v);

                popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        MyApplication.bus().post(new DeleteTrainModuleEvent(module));
                        return true;
                    }
                });
                popup.inflate(R.menu.train_module_list_item_menu);
                popup.show();
            }
        });

        holder.moduleRow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MyApplication.bus().post(new TrainModuleClickedEvent(module));
            }
        });
    }

    @Override
    public int getItemCount() {
        return (null != trainModules ? trainModules.size() : 0);
    }

    public void updateAdapter(List<TrainModule> modules) {
        trainModules = modules;
        notifyDataSetChanged();
    }

    public void clearList() {
        trainModules.clear();
        notifyDataSetChanged();
    }


    class ViewHolder extends RecyclerView.ViewHolder {
        @BindView(R.id.module_row)
        ViewGroup moduleRow;
        @BindView(R.id.title)
        TextView mTitle;
        @BindView(R.id.organization)
        TextView mOrganization;
        @BindView(R.id.downloadState)
        ImageView downloadState;
        @BindView(R.id.type)
        TextView mType;
        @BindView(R.id.size)
        TextView mSize;
        @BindView(R.id.popupMenu)
        ImageButton moreOption;


        ViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }

        void setDownloadedIcon() {
            moreOption.setVisibility(View.VISIBLE);
            setDrawable(R.drawable.ic_downloaded);
        }

        void setDownloadingIcon() {
            moreOption.setVisibility(View.INVISIBLE);
            setDrawable(R.drawable.ic_downloading);
        }

        void hideDownloadIcon() {
            moreOption.setVisibility(View.INVISIBLE);
            downloadState.setVisibility(View.GONE);
        }

        private void setDrawable(@DrawableRes int resId) {
            downloadState.setVisibility(View.VISIBLE);
            downloadState.setImageDrawable(AppCompatResources.getDrawable(moduleRow.getContext(), resId));
        }
    }
}
