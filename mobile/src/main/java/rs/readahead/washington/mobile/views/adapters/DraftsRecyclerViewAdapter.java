package rs.readahead.washington.mobile.views.adapters;

import android.annotation.SuppressLint;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import rs.readahead.washington.mobile.R;
import rs.readahead.washington.mobile.domain.entity.Report;
import rs.readahead.washington.mobile.util.DateUtil;
import rs.readahead.washington.mobile.views.interfaces.IOnReportInteractionListener;


public class DraftsRecyclerViewAdapter extends RecyclerView.Adapter<DraftsRecyclerViewAdapter.ViewHolder> {
    private final List<Report> mValues;
    private final IOnReportInteractionListener mListener;

    public DraftsRecyclerViewAdapter(List<Report> items, IOnReportInteractionListener listener) {
        mValues = items;
        mListener = listener;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.draft_report_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, @SuppressLint("RecyclerView") final int position) {
        final Report report = mValues.get(position);

        String title = report.getTitle();
        if (TextUtils.isEmpty(title)) {
            title = holder.mTitle.getContext().getString(R.string.title_not_included);
        }
        holder.mTitle.setText(title);

        if (report.getDate() != null) {
            holder.mDate.setText(DateUtil.getStringFromDate(report.getDate()));
        } else {
            holder.mDate.setText(holder.mDate.getContext().getString(R.string.date_not_included));
        }

        holder.mEdit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                    mListener.onEditReport(report.getId());
            }
        });

        holder.mDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mListener.onDeleteReport(report, position);
            }
        });

        holder.mLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mListener.onPreviewReport(report.getId());
            }
        });
    }

    public boolean removeDraft(int position) {
        mValues.remove(position);
        notifyItemRemoved(position);
        notifyItemRangeChanged(position, getItemCount());

        return getItemCount() == 0;
    }

    @Override
    public int getItemCount() {
        return mValues.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        @BindView(R.id.mob_title) TextView mTitle;
        @BindView(R.id.mob_date) TextView mDate;
        @BindView(R.id.edit_draft) ImageView mEdit;
        @BindView(R.id.delete_draft) ImageView mDelete;
        @BindView(R.id.draft_layout) ViewGroup mLayout;

        ViewHolder(View view) {
            super(view);
            ButterKnife.bind(this, itemView);
        }
    }
}