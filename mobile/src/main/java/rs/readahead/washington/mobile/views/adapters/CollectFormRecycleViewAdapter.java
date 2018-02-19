package rs.readahead.washington.mobile.views.adapters;

import android.graphics.drawable.Drawable;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Collections;
import java.util.List;

import butterknife.BindString;
import butterknife.BindView;
import butterknife.ButterKnife;
import rs.readahead.washington.mobile.MyApplication;
import rs.readahead.washington.mobile.R;
import rs.readahead.washington.mobile.bus.event.DownloadBlankFormEntryEvent;
import rs.readahead.washington.mobile.bus.event.ShowBlankFormEntryEvent;
import rs.readahead.washington.mobile.bus.event.ToggleBlankFormFavoriteEvent;
import rs.readahead.washington.mobile.domain.entity.collect.CollectForm;
import rs.readahead.washington.mobile.util.ViewUtil;


public class CollectFormRecycleViewAdapter extends RecyclerView.Adapter<CollectFormRecycleViewAdapter.ViewHolder> {
    private List<CollectForm> forms = Collections.emptyList();

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.blank_collect_form_row, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        final CollectForm form = forms.get(position);

        holder.name.setText(form.getForm().getName());
        holder.organization.setText(form.getServerName());

        holder.setFavoritesButton(form.isFavorite());

        if (form.isDownloaded()) {
            holder.setButtonOpenText();
        } else {
            holder.setButtonDownloadText();
        }

        holder.dlOpenButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (form.isDownloaded()) {
                    MyApplication.bus().post(new ShowBlankFormEntryEvent(form));
                } else {
                    MyApplication.bus().post(new DownloadBlankFormEntryEvent(form));
                }
            }
        });

        holder.favoritesButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MyApplication.bus().post(new ToggleBlankFormFavoriteEvent(form));
            }
        });
    }

    @Override
    public int getItemCount() {
        return forms.size();
    }

    public void setForms(List<CollectForm> forms) {
        this.forms = forms;
        notifyDataSetChanged();
    }

    public void updateForm(CollectForm form) {
        // ok for now..
        for (int i = 0; i < forms.size(); i++) {
            if (forms.get(i).getId() == form.getId()) {
                forms.set(i, form);
                notifyDataSetChanged(); // wipe them out.. all of them :)
                return;
            }
        }
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        @BindView(R.id.form_row)
        ViewGroup row;
        @BindView(R.id.name)
        TextView name;
        @BindView(R.id.organization)
        TextView organization;
        @BindView(R.id.dl_open_button)
        Button dlOpenButton;
        @BindString(R.string.ra_download)
        String downloadString;
        @BindString(R.string.ra_open)
        String openString;
        @BindView(R.id.favorites_button)
        ImageButton favoritesButton;


        public ViewHolder(View itemView) {
            super(itemView);

            ButterKnife.bind(this, itemView);
        }

        void setButtonDownloadText() {
            dlOpenButton.setText(downloadString);
        }

        void setButtonOpenText() {
            dlOpenButton.setText(openString);
        }

        void setFavoritesButton(boolean favorite) {
            Drawable drawable;
            if (favorite) {
                drawable = ViewUtil.getTintedDrawable(row.getContext(), R.drawable.ic_star, R.color.wa_yellow);
            } else {
                drawable = ViewUtil.getTintedDrawable(row.getContext(), R.drawable.ic_star_outline, R.color.wa_gray);
            }

            favoritesButton.setImageDrawable(drawable);
        }
    }
}
