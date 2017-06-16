package rs.readahead.washington.mobile.domain.entity;


public interface IErrorBundle {
    Throwable getExeption();
    int getCode();
    String getMessage();
}
