package rs.readahead.washington.mobile.domain.repository;

import java.util.List;
import java.util.Map;

import io.reactivex.Single;
import rs.readahead.washington.mobile.domain.entity.MediaRecipient;
import rs.readahead.washington.mobile.domain.entity.MediaRecipientList;


public interface IMediaRecipientsRepository {
    Single<List<MediaRecipient>> listMediaRecipients();
    Single<List<MediaRecipient>> getDifferentMediaRecipientsFromRecipientList(MediaRecipientList mediaRecipientList, List<MediaRecipientList> lists);
    Single<List<MediaRecipientList>> listNonEmptyMediaRecipientLists();
    Single<MediaRecipient> addMediaRecipient(MediaRecipient mediaRecipient);
    Single<MediaRecipientList> addMediaRecipientList(MediaRecipientList mediaRecipientList);
    Single<Map<Long, MediaRecipient>> getCombinedMediaRecipients(List<MediaRecipient> recipients, List<MediaRecipientList> lists);
}
