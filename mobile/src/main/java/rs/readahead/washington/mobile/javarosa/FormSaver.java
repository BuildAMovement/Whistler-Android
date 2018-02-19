package rs.readahead.washington.mobile.javarosa;

import org.javarosa.core.model.FormIndex;
import org.javarosa.core.model.data.IAnswerData;
import org.javarosa.form.api.FormEntryController;

import java.util.LinkedHashMap;

import info.guardianproject.cacheword.CacheWordHandler;
import info.guardianproject.cacheword.ICacheWordSubscriber;
import io.reactivex.CompletableSource;
import io.reactivex.SingleSource;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.annotations.NonNull;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Action;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.AsyncSubject;
import rs.readahead.washington.mobile.R;
import rs.readahead.washington.mobile.data.database.DataSource;
import rs.readahead.washington.mobile.domain.entity.collect.CollectFormInstance;
import rs.readahead.washington.mobile.odk.FormController;
import rs.readahead.washington.mobile.odk.FormController.FailedConstraint;
import rs.readahead.washington.mobile.odk.exception.JavaRosaException;


public class FormSaver implements IFormSaverContract.IFormSaver,
        ICacheWordSubscriber {
    private IFormSaverContract.IView view;
    private AsyncSubject<DataSource> asyncDataSource = AsyncSubject.create();
    private CacheWordHandler cacheWordHandler;
    private CompositeDisposable disposables = new CompositeDisposable();
    private boolean autoSaveDraft;


    public FormSaver(IFormSaverContract.IView view) {
        this.view = view;
        this.cacheWordHandler = new CacheWordHandler(view.getContext().getApplicationContext(), this);
        cacheWordHandler.connectToService();

        /*Single.fromCallable(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                return SharedPrefs.getInstance().isAutoSaveDrafts();
            }
        }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<Boolean>() {
                    @Override
                    public void accept(Boolean aBoolean) throws Exception {
                        autoSaveDraft = aBoolean;
                    }
                });*/
        autoSaveDraft = false; // this is requested default..
    }

    @Override
    public boolean saveScreenAnswers(LinkedHashMap<FormIndex, IAnswerData> answers, boolean checkConstraints) {
        FormController formController = FormController.getActive();

        try {
            if (! formController.currentPromptIsQuestion()) { // bad name for method..
                return true;
            }

            FailedConstraint constraint = formController.saveAllScreenAnswers(answers, checkConstraints);

            if (constraint != null) {
                showFailedConstraint(constraint);
                return false;
            }
        } catch (JavaRosaException e) {
            view.formSaveError(e);
        }

        return true;
    }

    @Override
    public void saveActiveFormInstance() {
        disposables.add(asyncDataSource
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe(new Consumer<Disposable>() {
                    @Override
                    public void accept(@NonNull Disposable disposable) throws Exception {
                        view.showSaveFormInstanceLoading();
                    }
                })
                .flatMapSingle(new Function<DataSource, SingleSource<CollectFormInstance>>() {
                    @Override
                    public SingleSource<CollectFormInstance> apply(@NonNull DataSource dataSource) throws Exception {
                        return dataSource.saveInstance(FormController.getActive().getCollectFormInstance());
                    }
                })
                .doFinally(new Action() {
                    @Override
                    public void run() throws Exception {
                        view.hideSaveFormInstanceLoading();
                    }
                })
                .subscribe(new Consumer<CollectFormInstance>() {
                    @Override
                    public void accept(@NonNull CollectFormInstance instance) throws Exception {
                        view.formInstanceSaveSuccess(instance);
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(@NonNull Throwable throwable) throws Exception {
                        view.formInstanceSaveError(throwable);
                    }
                })
        );
    }

    public void saveActiveFormInstanceOnExit() {
        asyncDataSource
                .subscribeOn(Schedulers.io())
                .flatMapCompletable(new Function<DataSource, CompletableSource>() {
                    @Override
                    public CompletableSource apply(@NonNull DataSource dataSource) throws Exception {
                        return dataSource.saveInstance(FormController.getActive().getCollectFormInstance()).toCompletable();
                    }
                }).blockingAwait();
    }

    @Override
    public void autoSaveFormInstance() {
        if (! autoSaveDraft) return;

        final CollectFormInstance instance = FormController.getActive().getCollectFormInstance();

        /*if (!CollectFormInstanceStatus.UNKNOWN.equals(instance.getStatus()) &&
                !CollectFormInstanceStatus.UNKNOWN.equals(instance.getStatus())) { // only auto save draft and unknown
            return;
        }*/ // todo: check this!

        disposables.add(asyncDataSource
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .flatMapSingle(new Function<DataSource, SingleSource<CollectFormInstance>>() {
                    @Override
                    public SingleSource<CollectFormInstance> apply(@NonNull DataSource dataSource) throws Exception {
                        return dataSource.saveInstance(instance);
                    }
                })
                .subscribe(new Consumer<CollectFormInstance>() {
                    @Override
                    public void accept(CollectFormInstance instance) throws Exception {
                        view.formInstanceAutoSaveSuccess(instance);
                    }
                })
        );
    }

    @Override
    public boolean isAutoSaveDraft() {
        return autoSaveDraft;
    }

    @Override
    public void deleteActiveFormInstance() {
        final CollectFormInstance instance = FormController.getActive().getCollectFormInstance();
        final boolean cloned = instance.getClonedId() > 0;

        disposables.add(asyncDataSource
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe(new Consumer<Disposable>() {
                    @Override
                    public void accept(@NonNull Disposable disposable) throws Exception {
                        view.showDeleteFormInstanceStart();
                    }
                })
                .flatMapCompletable(new Function<DataSource, CompletableSource>() {
                    @Override
                    public CompletableSource apply(@NonNull DataSource dataSource) throws Exception {
                        return dataSource.deleteInstance(cloned ? instance.getClonedId() : instance.getId());
                    }
                })
                .doFinally(new Action() {
                    @Override
                    public void run() throws Exception {
                        view.hideDeleteFormInstanceEnd();
                    }
                })
                .subscribe(new Action() {
                    @Override
                    public void run() throws Exception {
                        view.formInstanceDeleteSuccess(cloned);
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) throws Exception {
                        view.formInstanceDeleteError(throwable);
                    }
                })
        );
    }

    @Override
    public boolean isActiveInstanceCloned() {
        return FormController.getActive().getCollectFormInstance().getClonedId() > 0;
    }

    @Override
    public void destroy() {
        if (cacheWordHandler != null) {
            cacheWordHandler.disconnectFromService();
        }
        disposables.dispose();
        view = null;
    }

    @Override
    public void onCacheWordUninitialized() {
    }

    @Override
    public void onCacheWordLocked() {
    }

    @Override
    public void onCacheWordOpened() {
        if (view != null) {
            DataSource dataSource = DataSource.getInstance(view.getContext(), cacheWordHandler.getEncryptionKey());
            asyncDataSource.onNext(dataSource);
            asyncDataSource.onComplete();
        }

        cacheWordHandler.disconnectFromService();
        cacheWordHandler = null;
    }

    private void showFailedConstraint(FailedConstraint constraint) {
        FormController formController = FormController.getActive();
        String constraintText;

        switch (constraint.status) {
            case FormEntryController.ANSWER_CONSTRAINT_VIOLATED:
                constraintText = formController.getQuestionPromptConstraintText(constraint.index);
                if (constraintText == null) {
                    constraintText = formController.getQuestionPrompt(constraint.index).getSpecialFormQuestionText("constraintMsg");
                    if (constraintText == null) {
                        constraintText = view.getContext().getString(R.string.ra_odk_invalid_answer_error);
                    }
                }
                break;

            case FormEntryController.ANSWER_REQUIRED_BUT_EMPTY:
                constraintText = formController.getQuestionPromptRequiredText(constraint.index);
                if (constraintText == null) {
                    constraintText = formController.getQuestionPrompt(constraint.index).getSpecialFormQuestionText("requiredMsg");
                    if (constraintText == null) {
                        constraintText = view.getContext().getString(R.string.ra_odk_required_answer_error);
                    }
                }
                break;

            default:
                return; // ignore this..
        }

        view.formConstraintViolation(constraint.index, constraintText);
    }
}
