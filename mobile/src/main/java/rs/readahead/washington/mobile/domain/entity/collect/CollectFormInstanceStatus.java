package rs.readahead.washington.mobile.domain.entity.collect;


public enum CollectFormInstanceStatus {
    UNKNOWN,
    DRAFT,
    FINALIZED,
    SUBMITTED,
    SUBMISSION_ERROR,
    DELETED,
    SUBMISSION_PENDING // no connection on sending
}
