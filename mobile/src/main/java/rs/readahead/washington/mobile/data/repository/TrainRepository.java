package rs.readahead.washington.mobile.data.repository;

import java.util.List;

import io.reactivex.Single;
import io.reactivex.SingleSource;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;
import rs.readahead.washington.mobile.data.entity.TrainModuleEntity;
import rs.readahead.washington.mobile.data.entity.mapper.EntityMapper;
import rs.readahead.washington.mobile.data.rest.TrainApi;
import rs.readahead.washington.mobile.domain.entity.TrainModule;
import rs.readahead.washington.mobile.domain.repository.ITrainRepository;


public class TrainRepository implements ITrainRepository {
    @Override
    public Single<List<TrainModule>> getModules() {
        return TrainApi.getApi().listTrainModules()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .map(new Function<List<TrainModuleEntity>, List<TrainModule>>() {
                    @Override
                    public List<TrainModule> apply(List<TrainModuleEntity> entities) throws Exception {
                        return new EntityMapper().transform(entities);
                    }
                })
                .onErrorResumeNext(new Function<Throwable, SingleSource<? extends List<TrainModule>>>() {
                    @Override
                    public SingleSource<? extends List<TrainModule>> apply(Throwable throwable) throws Exception {
                        return Single.error(new ErrorBundle(throwable));
                    }
                });
                // doOnNext can put it to cache..
    }

    @Override
    public Single<List<TrainModule>> searchModules(String ident) {
        return TrainApi.getApi().searchTrainModules(ident)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .map(new Function<List<TrainModuleEntity>, List<TrainModule>>() {
                    @Override
                    public List<TrainModule> apply(List<TrainModuleEntity> entities) throws Exception {
                        return new EntityMapper().transform(entities);
                    }
                })
                .onErrorResumeNext(new Function<Throwable, SingleSource<? extends List<TrainModule>>>() {
                    @Override
                    public SingleSource<? extends List<TrainModule>> apply(Throwable throwable) throws Exception {
                        return Single.error(new ErrorBundle(throwable));
                    }
                });
    }
}
