package rs.readahead.washington.mobile.domain.repository;

import org.javarosa.core.model.FormDef;

import java.util.List;

import io.reactivex.Completable;
import io.reactivex.Maybe;
import io.reactivex.Single;
import rs.readahead.washington.mobile.domain.entity.MediaFile;
import rs.readahead.washington.mobile.domain.entity.Metadata;
import rs.readahead.washington.mobile.domain.entity.collect.CollectForm;
import rs.readahead.washington.mobile.domain.entity.collect.CollectFormInstance;
import rs.readahead.washington.mobile.domain.entity.collect.ListFormResult;


public interface ICollectFormsRepository {
    Single<List<CollectForm>> listBlankForms();
    Single<ListFormResult> updateBlankForms(ListFormResult listFormResult);
    Maybe<FormDef> getBlankFormDef(CollectForm form);
    Single<FormDef> updateBlankFormDef(CollectForm form, FormDef formDef);
    Single<CollectForm> toggleFavorite(CollectForm form);

    Single<List<CollectFormInstance>> listDraftForms();
    Single<List<CollectFormInstance>> listSentForms();
    Single<List<CollectFormInstance>> listPendingForms();
    Single<CollectFormInstance> saveInstance(CollectFormInstance instance);
    Single<CollectFormInstance> getInstance(long id);
    Completable deleteInstance(long id);
    Single<MediaFile> attachMetadata(long mediaFileId, Metadata metadata);
}
