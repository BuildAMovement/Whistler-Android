package rs.readahead.washington.mobile.domain.repository;

import java.util.List;

import io.reactivex.Completable;
import io.reactivex.Single;
import rs.readahead.washington.mobile.domain.entity.TrainModule;
import rs.readahead.washington.mobile.presentation.entity.DownloadState;


public interface ITrainModuleRepository {
    Single<List<TrainModule>> listTrainModules();
    Single<List<TrainModule>> updateTrainDBModules(List<TrainModule> modules);
    Completable startDownloadTrainModule(TrainModule module);
    Completable removeTrainDBModule(long id);
    Single<List<TrainModule>> listDownloadingModules();
    Completable setTrainModuleDownloadState(long id, DownloadState state);
    Single<TrainModule> getTrainModule(long id);
}
