package rs.readahead.washington.mobile.data.repository;

import org.apache.commons.io.IOUtils;
import org.javarosa.core.model.FormDef;
import org.javarosa.core.model.IDataReference;
import org.javarosa.core.model.SubmissionProfile;
import org.javarosa.core.model.instance.FormInstance;
import org.javarosa.core.services.transport.payload.ByteArrayPayload;
import org.javarosa.model.xform.XFormSerializingVisitor;
import org.javarosa.model.xform.XPathReference;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.reactivex.Single;
import io.reactivex.SingleSource;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.annotations.NonNull;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Response;
import rs.readahead.washington.mobile.data.entity.OpenRosaResponseEntity;
import rs.readahead.washington.mobile.data.entity.XFormEntity;
import rs.readahead.washington.mobile.data.entity.XFormsEntity;
import rs.readahead.washington.mobile.data.entity.mapper.OpenRosaDataMapper;
import rs.readahead.washington.mobile.data.openrosa.OpenRosaService;
import rs.readahead.washington.mobile.domain.entity.IErrorBundle;
import rs.readahead.washington.mobile.domain.entity.collect.CollectForm;
import rs.readahead.washington.mobile.domain.entity.collect.CollectFormInstance;
import rs.readahead.washington.mobile.domain.entity.collect.CollectServer;
import rs.readahead.washington.mobile.domain.entity.collect.ListFormResult;
import rs.readahead.washington.mobile.domain.entity.collect.NegotiatedCollectServer;
import rs.readahead.washington.mobile.domain.entity.collect.OpenRosaResponse;
import rs.readahead.washington.mobile.domain.repository.IOpenRosaRepository;
import rs.readahead.washington.mobile.util.StringUtils;


public class OpenRosaRepository implements IOpenRosaRepository {
    private static final String FORM_LIST_PATH = "formList";
    private static final String SUBMISSION_PATH = "submission";


    @Override
    public Single<ListFormResult> formList(final CollectServer server) {
        return OpenRosaService.newInstance(server.getUsername(), server.getPassword())
                .getServices().formList(null, StringUtils.append('/',server.getUrl(), FORM_LIST_PATH))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .map(new Function<XFormsEntity, ListFormResult>() {
                    @Override
                    public ListFormResult apply(@NonNull XFormsEntity formsEntity) throws Exception {
                        List<CollectForm> forms = new ArrayList<>();
                        OpenRosaDataMapper mapper = new OpenRosaDataMapper();

                        for (XFormEntity form: formsEntity.xforms) {
                            forms.add(new CollectForm(server.getId(), mapper.transform(form)));
                        }

                        ListFormResult listFormResult = new ListFormResult();
                        listFormResult.setForms(forms);

                        return listFormResult;
                    }
                })
                .onErrorResumeNext(new Function<Throwable, SingleSource<? extends ListFormResult>>() {
                    @Override
                    public SingleSource<? extends ListFormResult> apply(@NonNull Throwable throwable) throws Exception {
                        ListFormResult listFormResult = new ListFormResult();
                        ErrorBundle errorBundle = new ErrorBundle(throwable);
                        errorBundle.setServerId(server.getId());
                        errorBundle.setServerName(server.getName());
                        listFormResult.setErrors(Collections.<IErrorBundle>singletonList(errorBundle));

                        return Single.just(listFormResult);
                    }
                });
    }

    @Override
    public Single<FormDef> getFormDef(CollectServer server, CollectForm form) {
        return OpenRosaService.newInstance(server.getUsername(), server.getPassword())
                .getServices().getFormDef(null, form.getForm().getDownloadUrl())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .map(new Function<ResponseBody, FormDef>() {
                    @Override
                    public FormDef apply(@NonNull ResponseBody response) throws Exception {
                        return new OpenRosaDataMapper().transform(response);
                    }
                })
                .onErrorResumeNext(new Function<Throwable, SingleSource<? extends FormDef>>() {
                    @Override
                    public SingleSource<? extends FormDef> apply(@NonNull Throwable throwable) throws Exception {
                        XErrorBundle errorBundle = new XErrorBundle(throwable);
                        return Single.error(errorBundle);
                    }
                });
    }

    @Override
    public Single<NegotiatedCollectServer> submitFormNegotiate(final CollectServer server) {
        // todo: InstanceColumns.SUBMISSION_URI? url in form
        return OpenRosaService.newInstance(server.getUsername(), server.getPassword())
                .getServices().submitFormNegotiate(null, StringUtils.append('/',server.getUrl(), SUBMISSION_PATH))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .map(new Function<Response<Void>, NegotiatedCollectServer>() {
                    @Override
                    public NegotiatedCollectServer apply(@NonNull Response<Void> response) throws Exception {
                        return new OpenRosaDataMapper().transform(server, response);
                    }
                })
                .onErrorResumeNext(new Function<Throwable, SingleSource<? extends NegotiatedCollectServer>>() {
                    @Override
                    public SingleSource<? extends NegotiatedCollectServer> apply(@NonNull Throwable throwable) throws Exception {
                        return Single.error(throwable);
                    }
                });
    }

    @Override
    public Single<OpenRosaResponse> submitForm(NegotiatedCollectServer server, CollectFormInstance instance) {
        Map<String, RequestBody> parts = new HashMap<>(1);

        try {
            FormDef formDef = instance.getFormDef();
            FormInstance formInstance = formDef.getInstance();
            XFormSerializingVisitor serializer = new XFormSerializingVisitor();
            ByteArrayPayload payload = (ByteArrayPayload) serializer.createSerializedPayload(formInstance, getSubmissionDataReference(formDef));
            //payload.getLength()
            parts.put("xml_submission_file\"; filename=\"" + instance.getInstanceName() + ".xml", // wtf, OkHttp3 :)
                    RequestBody.create(MediaType.parse("text/xml"), IOUtils.toByteArray(payload.getPayloadStream())));
        } catch (IOException e) {
            return Single.error(e);
        }

        String url = server.isUrlNegotiated() ? server.getUrl() : StringUtils.append('/',server.getUrl(), SUBMISSION_PATH);

        return OpenRosaService.newInstance(server.getUsername(), server.getPassword())
                .getServices().submitForm(null, url, parts)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .map(new Function<Response<OpenRosaResponseEntity>, OpenRosaResponse>() {
                    @Override
                    public OpenRosaResponse apply(@NonNull Response<OpenRosaResponseEntity> response) throws Exception {
                        OpenRosaResponseEntity entity = response.body();
                        if (entity != null) {
                            entity.statusCode = response.code();
                        }

                        return new OpenRosaDataMapper().transform(entity);
                    }
                })
                .onErrorResumeNext(new Function<Throwable, SingleSource<? extends OpenRosaResponse>>() {
                    @Override
                    public SingleSource<? extends OpenRosaResponse> apply(@NonNull Throwable throwable) throws Exception {
                        return Single.error(throwable);
                    }
                });
    }

    // todo: submission profile for SMS? check it out
    private IDataReference getSubmissionDataReference(FormDef formDef) {
        // Determine the information about the submission...
        SubmissionProfile p = formDef.getSubmissionProfile();
        if (p == null || p.getRef() == null) {
            return new XPathReference("/");
        } else {
            return p.getRef();
        }
    }
}
