package rs.readahead.washington.mobile.views.adapters;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

import rs.readahead.washington.mobile.R;
import rs.readahead.washington.mobile.domain.entity.MediaRecipient;
import rs.readahead.washington.mobile.views.interfaces.IRecipientsHandler;


public class MediaRecipientListAdapter extends ArrayAdapter<MediaRecipient> {
    private IRecipientsHandler recipientsHandler;


    public MediaRecipientListAdapter(
            Context context,
            final IRecipientsHandler recipientListsHandler,
            final List<MediaRecipient> mediaRecipients) {
        super(context, 0, mediaRecipients);
        this.recipientsHandler = recipientListsHandler;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        final MediaRecipient mediaRecipientList = getItem(position);

        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.recipient_row_for_list, parent, false);
        }

        TextView titleView = convertView.findViewById(R.id.recipient_title);
        ImageView editRecipient = convertView.findViewById(R.id.edit);
        ImageView removeRecipient = convertView.findViewById(R.id.delete);

        if (mediaRecipientList != null) {
            titleView.setText(mediaRecipientList.getTitle());

            removeRecipient.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    recipientsHandler.removeMediaRecipient(mediaRecipientList);
                }
            });

            editRecipient.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    recipientsHandler.updateMediaRecipient(mediaRecipientList);
                }
            });
        }

        return convertView;
    }
}
