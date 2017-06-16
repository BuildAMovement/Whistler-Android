package rs.readahead.washington.mobile.mvp.presenter;


public interface IPresenter {
    void resume(); // todo: check these two, attach/detach case in particular..
    void pause();
    void destroy();
}
