package rs.readahead.washington.mobile.views.adapters;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.support.design.widget.Snackbar;
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
import rs.readahead.washington.mobile.domain.entity.TrustedPerson;
import rs.readahead.washington.mobile.util.DialogsUtil;
import rs.readahead.washington.mobile.util.StringUtils;
import rs.readahead.washington.mobile.views.activity.OnTrustedPersonChangeListener;
import rs.readahead.washington.mobile.views.activity.OnTrustedPersonInteractionListener;

public class CircleOfTrustAdapter extends RecyclerView.Adapter<CircleOfTrustAdapter.DataHolder> {

    private Context context;
    private List<TrustedPerson> mRecipients;
    private OnTrustedPersonChangeListener listener;

    public CircleOfTrustAdapter(List<TrustedPerson> myDataSet, Context context, OnTrustedPersonChangeListener listener) {
        mRecipients = myDataSet;
        this.context = context;
        this.listener = listener;
    }

    static class DataHolder extends RecyclerView.ViewHolder {

        @BindView(R.id.recipient_title) TextView mTitle;
        @BindView(R.id.recipient_phone_number) TextView mPhoneNumber;
        @BindView(R.id.edit) ImageView mEdit;
        @BindView(R.id.delete) ImageView mTrash;
        @BindView(R.id.recipient_main) LinearLayout mMainLayout;
        @BindView(R.id.mail_layout) LinearLayout mMailLayout;

        DataHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }
    }

    @Override
    public DataHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.trusted_person_row, null);
        return new DataHolder(view);
    }

    @Override
    public void onBindViewHolder(final CircleOfTrustAdapter.DataHolder holder, @SuppressLint("RecyclerView") final int position) {
        final TrustedPerson trustedPerson = mRecipients.get(position);
        holder.mTitle.setText(trustedPerson.getName());
        holder.mPhoneNumber.setText(StringUtils.orDefault(trustedPerson.getPhoneNumber(), context.getString(R.string.not_included)));
        holder.mTrash.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                String message = String.format(context.getString(R.string.trusted_person_remove_text), mRecipients.get(position).getName());
                DialogsUtil.showMessageOKCancelWithTitle(context, message, context.getString(R.string.attention),
                        context.getString(R.string.ok), context.getString(R.string.cancel),
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                deleteTrustedPerson(position, v);
                                dialog.dismiss();
                            }
                        }, null);
            }
        });
        holder.mMainLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                holder.mMailLayout.setVisibility(holder.mMailLayout.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
            }
        });
        holder.mEdit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                editTrustedContactDialog(trustedPerson);
            }
        });
    }

    @Override
    public int getItemCount() {
        return (null != mRecipients ? mRecipients.size() : 0);
    }

    @Override
    public void onViewDetachedFromWindow(DataHolder holder) {
        super.onViewDetachedFromWindow(holder);
    }

    public void updateAdapter(List<TrustedPerson> contacts) {

        mRecipients = contacts;
        notifyDataSetChanged();
    }

    private void deleteTrustedPerson(int position, View v) {
        final ImageView delete = (ImageView) v;

        listener.onContactDeleted(mRecipients.get(position).getColumnId());

        mRecipients.remove(position);
        notifyItemRemoved(position);
        notifyItemRangeChanged(position, getItemCount());
        Snackbar.make(delete, context.getString(R.string.removed_trusted), Snackbar.LENGTH_SHORT).show();

    }

    private void editTrustedContactDialog(final TrustedPerson trustedPerson) {
        DialogsUtil.showTrustedContactDialog(R.string.edit_recipient, context, trustedPerson, new OnTrustedPersonInteractionListener() {
            @Override
            public void onTrustedPersonInteraction(TrustedPerson trustedPerson) {
                listener.onContactEdited(trustedPerson);
            }
        });

    }
}