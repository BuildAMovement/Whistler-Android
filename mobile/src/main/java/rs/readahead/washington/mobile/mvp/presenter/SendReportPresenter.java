package rs.readahead.washington.mobile.mvp.presenter;

import rs.readahead.washington.mobile.domain.entity.IErrorBundle;
import rs.readahead.washington.mobile.domain.interactor.CreateReportUseCase;
import rs.readahead.washington.mobile.domain.interactor.DefaultObserver;
import rs.readahead.washington.mobile.models.Report;
import rs.readahead.washington.mobile.mvp.view.ISendReportView;


public class SendReportPresenter implements IPresenter {
    private final CreateReportUseCase createReportUseCase;
    private ISendReportView view;


    //@Inject
    public SendReportPresenter(CreateReportUseCase createReportUseCase, ISendReportView view) {
        this.createReportUseCase = createReportUseCase;
        this.view = view;
    }

    public void createReport(Report report) {
        showLoading();
        this.createReportUseCase.execute(new CreateReportObserver(), report);
    }

    @Override
    public void resume() {

    }

    @Override
    public void pause() {

    }

    @Override
    public void destroy() {
        createReportUseCase.dispose();
        view = null;
    }

    private void showLoading() {
        if (view != null) {
            view.showSendingReport();
        }
    }

    private void hideLoading() {
        if (view != null) {
            view.hideSendingReport();
        }
    }

    private void onCreateReport(String uid) {
        if (view != null) {
            view.onSendReportSuccess(uid);
        }
    }

    private void onError(IErrorBundle errorBundle) {
        if (view != null) {
            view.onSendReportError(errorBundle);
        }
    }

    private final class CreateReportObserver extends DefaultObserver<String> {
        @Override
        public void onComplete() {
            SendReportPresenter.this.hideLoading();
        }

        @Override
        public void onNext(String uid) {
            SendReportPresenter.this.onCreateReport(uid);
        }

        @Override
        public void onError(Throwable exception) {
            SendReportPresenter.this.hideLoading();
            SendReportPresenter.this.onError((IErrorBundle) exception);
        }
    }
}
