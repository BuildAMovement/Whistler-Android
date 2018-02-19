package rs.readahead.washington.mobile.domain.repository;

import org.javarosa.core.model.FormDef;

import io.reactivex.Single;
import rs.readahead.washington.mobile.domain.entity.collect.CollectForm;
import rs.readahead.washington.mobile.domain.entity.collect.CollectFormInstance;
import rs.readahead.washington.mobile.domain.entity.collect.CollectServer;
import rs.readahead.washington.mobile.domain.entity.collect.ListFormResult;
import rs.readahead.washington.mobile.domain.entity.collect.NegotiatedCollectServer;
import rs.readahead.washington.mobile.domain.entity.collect.OpenRosaResponse;


public interface IOpenRosaRepository {
    Single<ListFormResult> formList(CollectServer server);
    Single<FormDef> getFormDef(CollectServer server, CollectForm form);
    Single<NegotiatedCollectServer> submitFormNegotiate(CollectServer server);
    Single<OpenRosaResponse> submitForm(NegotiatedCollectServer server, CollectFormInstance instance);
}
