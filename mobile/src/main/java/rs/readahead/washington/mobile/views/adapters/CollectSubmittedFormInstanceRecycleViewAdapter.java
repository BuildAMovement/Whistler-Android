package rs.readahead.washington.mobile.views.adapters;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;
import rs.readahead.washington.mobile.MyApplication;
import rs.readahead.washington.mobile.R;
import rs.readahead.washington.mobile.bus.event.CancelPendingFormInstanceEvent;
import rs.readahead.washington.mobile.bus.event.DeleteFormInstanceEvent;
import rs.readahead.washington.mobile.bus.event.ReSubmitFormInstanceEvent;
import rs.readahead.washington.mobile.bus.event.ShowFormInstanceEntryEvent;
import rs.readahead.washington.mobile.domain.entity.collect.CollectFormInstance;
import rs.readahead.washington.mobile.domain.entity.collect.CollectFormInstanceStatus;
import rs.readahead.washington.mobile.util.StringUtils;
import rs.readahead.washington.mobile.util.ViewUtil;


public class CollectSubmittedFormInstanceRecycleViewAdapter extends RecyclerView.Adapter<CollectSubmittedFormInstanceRecycleViewAdapter.ViewHolder> {
    private List<CollectFormInstance> instances = Collections.emptyList();

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.submitted_collect_form_instance_row, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        final CollectFormInstance instance = instances.get(position);

        final Context context = holder.name.getContext();

        holder.name.setText(instance.getInstanceName());
        holder.organization.setText(instance.getServerName());
        holder.setDates(instance.getUpdated());

        if (instance.getStatus() == CollectFormInstanceStatus.SUBMITTED) {
            holder.setSubmittedIcon();
        } else if (instance.getStatus() == CollectFormInstanceStatus.SUBMISSION_ERROR) {
            holder.setSubmitErrorIcon();
        } else if (instance.getStatus() == CollectFormInstanceStatus.SUBMISSION_PENDING) {
            holder.setPendingIcon();
        }

        holder.instanceRow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MyApplication.bus().post(new ShowFormInstanceEntryEvent(instance.getId()));
            }
        });
        holder.popupMenu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PopupMenu popup = new PopupMenu(context, v);

                popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        if (item.getItemId() == R.id.delete) {
                            MyApplication.bus().post(new DeleteFormInstanceEvent(instance.getId(), instance.getStatus()));
                            return true;
                        }

                        if (item.getItemId() == R.id.discard) {
                            MyApplication.bus().post(new CancelPendingFormInstanceEvent(instance.getId()));
                            return true;
                        }

                        if (item.getItemId() == R.id.resubmit) {
                            MyApplication.bus().post(new ReSubmitFormInstanceEvent(instance));
                            return true;
                        }

                        return false;
                    }
                });

                if (instance.getStatus() == CollectFormInstanceStatus.SUBMISSION_PENDING) {
                    popup.inflate(R.menu.pending_forms_list_item_menu);
                } else if (instance.getStatus() == CollectFormInstanceStatus.SUBMISSION_ERROR) {
                    popup.inflate(R.menu.submit_error_forms_list_item_menu);
                } else {
                    popup.inflate(R.menu.submitted_forms_list_item_menu);
                }

                popup.show();
            }
        });
    }

    @Override
    public int getItemCount() {
        return instances.size();
    }

    public void setInstances(List<CollectFormInstance> forms) {
        this.instances = forms;
        notifyDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        @BindView(R.id.instanceRow)
        ViewGroup instanceRow;
        @BindView(R.id.name)
        TextView name;
        @BindView(R.id.organization)
        TextView organization;
        @BindView(R.id.date_month)
        TextView dateMonth;
        @BindView(R.id.date_day)
        TextView dateDay;
        @BindView(R.id.date_year)
        TextView dateYear;
        @BindView(R.id.icon)
        ImageView icon;
        @BindView(R.id.popupMenu)
        ImageButton popupMenu;

        ViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }

        void setDates(long timestamp) {
            Date date = new Date(timestamp);

            String month = StringUtils.first(new SimpleDateFormat("MMM", Locale.getDefault()).format(date), 3);
            String day = new SimpleDateFormat("dd", Locale.US).format(date);
            String year = new SimpleDateFormat("yyyy", Locale.US).format(date);

            dateMonth.setText(month);
            dateDay.setText(day);
            dateYear.setText(year);
        }

        private void setSubmittedIcon() {
            Drawable drawable = ViewUtil.getTintedDrawable(instanceRow.getContext(), R.drawable.ic_check_circle, R.color.wa_green);
            if (drawable != null) {
                icon.setImageDrawable(drawable);
            }
        }

        private void setSubmitErrorIcon() {
            Drawable drawable = ViewUtil.getTintedDrawable(instanceRow.getContext(), R.drawable.ic_error, R.color.wa_red);
            if (drawable != null) {
                icon.setImageDrawable(drawable);
            }
        }

        private void setPendingIcon() {
            Drawable drawable = ViewUtil.getTintedDrawable(instanceRow.getContext(), R.drawable.ic_query_builder, R.color.wa_yellow);
            if (drawable != null) {
                icon.setImageDrawable(drawable);
            }
        }
    }
}
