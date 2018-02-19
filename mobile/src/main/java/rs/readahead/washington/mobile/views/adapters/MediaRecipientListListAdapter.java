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
import rs.readahead.washington.mobile.domain.entity.MediaRecipientList;
import rs.readahead.washington.mobile.views.interfaces.IRecipientListsHandler;


public class MediaRecipientListListAdapter extends ArrayAdapter<MediaRecipientList> {
    private IRecipientListsHandler recipientListsHandler;


    public MediaRecipientListListAdapter(
            Context context,
            final IRecipientListsHandler recipientListsHandler,
            final List<MediaRecipientList> mediaRecipientLists) {
        super(context, 0, mediaRecipientLists);
        this.recipientListsHandler = recipientListsHandler;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        final MediaRecipientList mediaRecipientList = getItem(position);

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
                    recipientListsHandler.removeMediaRecipientList(mediaRecipientList);
                }
            });

            editRecipient.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    recipientListsHandler.updateMediaRecipientList(mediaRecipientList);
                }
            });
        }

        return convertView;
    }
}
