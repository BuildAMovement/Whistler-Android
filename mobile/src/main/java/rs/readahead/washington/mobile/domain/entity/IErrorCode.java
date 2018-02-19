package rs.readahead.washington.mobile.domain.entity;


public class IErrorCode {
    public static final int UNAUTHORIZED            = 401;
    public static final int NOT_FOUND               = 404;

    // OpenRosa Negotiation
    public static final int ORN_NOT_204_RESPONSE    = 10000; // negotiation returned !204 status
    public static final int ORN_BAD_HOST            = 10001; // server's uri host is bad
    public static final int ORN_NOT_HTTPS_HOST      = 10002; // server's scheme is not https
    public static final int ORN_BAD_LOCATION_HEADER = 10003; // negotiation returned bad location in header
}
