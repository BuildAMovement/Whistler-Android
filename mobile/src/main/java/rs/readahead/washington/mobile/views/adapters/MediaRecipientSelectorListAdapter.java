package rs.readahead.washington.mobile.views.adapters;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckedTextView;

import java.util.List;

import rs.readahead.washington.mobile.R;
import rs.readahead.washington.mobile.presentation.entity.MediaRecipientSelection;
import rs.readahead.washington.mobile.views.interfaces.IEditRecipientListHandler;


public class MediaRecipientSelectorListAdapter extends ArrayAdapter<MediaRecipientSelection> {
    private IEditRecipientListHandler editRecipientListHandler;


    public MediaRecipientSelectorListAdapter(Context context, List<MediaRecipientSelection> selections, IEditRecipientListHandler editRecipientListHandler) {
        super(context, 0, selections);
        this.editRecipientListHandler = editRecipientListHandler;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        MediaRecipientSelection selection = getItem(position);

        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.media_recipient_selector_checked_text_view, parent, false);
        }

        CheckedTextView view = (CheckedTextView) convertView;

        if (selection != null) {
            view.setText(selection.getTitle());
            view.setChecked(selection.isChecked());
            view.setTag(position);

            view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int position = (Integer) v.getTag();
                    MediaRecipientSelection s = getItem(position);
                    CheckedTextView cv = (CheckedTextView) v;

                    if (s != null) {
                        cv.toggle();
                        s.setChecked(cv.isChecked());
                    }

                    editRecipientListHandler.setSelectVisible(true);
                }
            });
        }

        return convertView;
    }
}
