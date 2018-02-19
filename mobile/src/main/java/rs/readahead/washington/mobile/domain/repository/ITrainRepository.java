package rs.readahead.washington.mobile.domain.repository;

import java.util.List;

import io.reactivex.Single;
import rs.readahead.washington.mobile.domain.entity.TrainModule;


public interface ITrainRepository {
    Single<List<TrainModule>> getModules();
    Single<List<TrainModule>> searchModules(String ident);
}
