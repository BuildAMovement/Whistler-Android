package rs.readahead.washington.mobile.views.adapters;

import android.content.Context;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.RecyclerView;
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
import rs.readahead.washington.mobile.models.Report;
import rs.readahead.washington.mobile.util.DateUtil;
import rs.readahead.washington.mobile.util.DialogsUtil;
import rs.readahead.washington.mobile.views.fragment.DraftsListFragment;

public class DraftsRecyclerViewAdapter extends RecyclerView.Adapter<DraftsRecyclerViewAdapter.ViewHolder> {
    private final List<Report> mValues;
    private final DraftsListFragment.OnListFragmentInteractionListener mListener;
    private final DraftsListFragment.OnEditInteractionListener mEditListener;
    private final DraftsListFragment.OnDeleteInteractionListener mDeleteListener;
    private final Context context;
    private AlertDialog mRemoveDialog;

    public DraftsRecyclerViewAdapter(List<Report> items, DraftsListFragment.OnListFragmentInteractionListener listener,
                                     DraftsListFragment.OnEditInteractionListener editListener,DraftsListFragment.OnDeleteInteractionListener deleteListener,
                                     Context context) {
        mValues = items;
        mListener = listener;
        mEditListener = editListener;
        mDeleteListener = deleteListener;
        this.context = context;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.draft_report_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, final int position) {
        final Report report = mValues.get(position);
        holder.mTitle.setText(report.getTitle());
        holder.mDate.setText(DateUtil.getStringFromDate(report.getDate()));

        holder.mEdit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (null != mEditListener) {
                    mEditListener.onEditFragmentInteraction(report);
                }
            }
        });

        holder.mDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDraftDeleteDialog(report.getTitle(), position);
            }
        });

        holder.mLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mListener.onListFragmentInteraction(report);
            }
        });
    }

    private void deleteDraft(int position) {
        mDeleteListener.onDeleteFragmentInteraction(mValues.get(position));
        mValues.remove(position);
        notifyItemRemoved(position);
        notifyItemRangeChanged(position, getItemCount());
    }

    private void showDraftDeleteDialog(String title, final int position) {
        mRemoveDialog = DialogsUtil.showRemoveReportDialog(title, context, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                deleteDraft(position);
                mRemoveDialog.dismiss();
            }
        });
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
        @BindView(R.id.draft_layout) LinearLayout mLayout;

        ViewHolder(View view) {
            super(view);
            ButterKnife.bind(this, itemView);
        }

    }
}
