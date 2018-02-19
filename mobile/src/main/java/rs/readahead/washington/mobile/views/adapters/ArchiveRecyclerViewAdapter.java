package rs.readahead.washington.mobile.views.adapters;

import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import rs.readahead.washington.mobile.R;
import rs.readahead.washington.mobile.domain.entity.Report;
import rs.readahead.washington.mobile.util.DateUtil;
import rs.readahead.washington.mobile.views.interfaces.IOnReportInteractionListener;


public class ArchiveRecyclerViewAdapter extends RecyclerView.Adapter<ArchiveRecyclerViewAdapter.ViewHolder> {
    private final List<Report> mValues;
    private final IOnReportInteractionListener mListener;

    public ArchiveRecyclerViewAdapter(List<Report> items, IOnReportInteractionListener listener) {
        mValues = items;
        mListener = listener;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.archive_report_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, final int position) {
        final Report report = mValues.get(position);

        String title = report.getTitle();
        if (TextUtils.isEmpty(title)) {
            title = holder.mTitle.getContext().getString(R.string.title_not_included);
        }
        holder.mTitle.setText(title);

        if (report.getDate() != null) {
            holder.mDate.setText(DateUtil.getStringFromDate(report.getDate()));
        }

        holder.mResend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                    mListener.onSendReport(report);
            }
        });

        holder.mDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mListener.onDeleteReport(report, holder.getAdapterPosition());
            }
        });

        holder.mLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mListener.onPreviewReport(report.getId());
            }
        });
    }

    public boolean removeReport(int position) {
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
        @BindView(R.id.report_title) TextView mTitle;
        @BindView(R.id.report_date) TextView mDate;
        @BindView(R.id.send_report) ImageView mResend;
        @BindView(R.id.delete_report) ImageView mDelete;
        @BindView(R.id.report_layout) ViewGroup mLayout;

        ViewHolder(View view) {
            super(view);
            ButterKnife.bind(this, itemView);
        }
    }
}
