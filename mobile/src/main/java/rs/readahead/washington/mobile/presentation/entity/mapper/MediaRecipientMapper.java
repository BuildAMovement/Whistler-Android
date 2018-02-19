package rs.readahead.washington.mobile.presentation.entity.mapper;


import android.support.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

import rs.readahead.washington.mobile.domain.entity.MediaRecipient;
import rs.readahead.washington.mobile.presentation.entity.MediaRecipientSelection;


public class MediaRecipientMapper {
    private MediaRecipientSelection transform(@NonNull MediaRecipient recipient) {
        MediaRecipientSelection selection = new MediaRecipientSelection();

        selection.setId((int) recipient.getId()); // todo: unify ids
        selection.setTitle(recipient.getTitle());

        return selection;
    }

    public List<MediaRecipientSelection> transform(@NonNull List<MediaRecipient> recipients) {
        List<MediaRecipientSelection> selections = new ArrayList<>();

        for (MediaRecipient r: recipients) {
            selections.add(transform(r));
        }

        return selections;
    }
}
