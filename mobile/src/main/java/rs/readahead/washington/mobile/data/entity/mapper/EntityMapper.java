package rs.readahead.washington.mobile.data.entity.mapper;

import android.support.annotation.NonNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import rs.readahead.washington.mobile.data.entity.FeedbackEntity;
import rs.readahead.washington.mobile.data.entity.FormMediaFileRegisterEntity;
import rs.readahead.washington.mobile.data.entity.MediaFileEntity;
import rs.readahead.washington.mobile.data.entity.MetadataEntity;
import rs.readahead.washington.mobile.data.entity.ReportEntity;
import rs.readahead.washington.mobile.data.entity.TrainModuleEntity;
import rs.readahead.washington.mobile.domain.entity.EvidenceLocation;
import rs.readahead.washington.mobile.domain.entity.Feedback;
import rs.readahead.washington.mobile.domain.entity.MediaFile;
import rs.readahead.washington.mobile.domain.entity.Metadata;
import rs.readahead.washington.mobile.domain.entity.TrainModule;
import rs.readahead.washington.mobile.domain.entity.MediaRecipient;
import rs.readahead.washington.mobile.domain.entity.Report;
import rs.readahead.washington.mobile.presentation.entity.DownloadState;


// @Singleton
public class EntityMapper {
    public FeedbackEntity transform(Feedback feedback) {
        FeedbackEntity entity = null;

        if (feedback != null) {
            entity = new FeedbackEntity();
            entity.name = feedback.getName();
            entity.email = feedback.getEmail();
            entity.message = feedback.getMessage();
        }

        return entity;
    }

    public ReportEntity transform(Report report, @NonNull Map<Long, MediaRecipient> combined) {
        ReportEntity entity = null;

        if (report != null) {
            entity = new ReportEntity();
            entity.setEvidences(transformEvidences(report.getEvidences()));
            entity.setRecipients(transformMediaRecipients(combined.values()));
            entity.setTitle(report.getTitle());
            entity.setContent(report.getContent());
            entity.setDate(report.getDate().getTime()/1000L); // to utc unix_timestamp, erase TZ
            entity.setLocation(report.getLocation());
            entity.setPublicReport(report.isReportPublic());
            entity.setContactInformation(report.getContactInformationData());
        }

        return entity;
    }

    private ReportEntity.EvidenceEntity transformEvidence(MediaFile evidence) {
        ReportEntity.EvidenceEntity entity = null;

        if (evidence != null) {
            entity = new ReportEntity.EvidenceEntity();
            entity.setName(evidence.getUid());
            entity.setPath(evidence.getFileName());
            entity.setMetadata(transform(evidence.getMetadata()));
        }

        return entity;
    }

    private List<ReportEntity.EvidenceEntity> transformEvidences(Collection<MediaFile> evidences) {
        final List<ReportEntity.EvidenceEntity> entities = new ArrayList<>(evidences.size());

        for (MediaFile evidence: evidences) {
            final ReportEntity.EvidenceEntity entity = transformEvidence(evidence);
            if (entity != null) {
                entities.add(entity);
            }
        }
        return entities;
    }

    private ReportEntity.RecipientEntity transform(MediaRecipient mediaRecipient) {
        ReportEntity.RecipientEntity entity = null;

        if (mediaRecipient != null) {
            entity = new ReportEntity.RecipientEntity();
            entity.setEmail(mediaRecipient.getMail());
            entity.setTitle(mediaRecipient.getTitle());
        }

        return entity;
    }

    private List<ReportEntity.RecipientEntity> transformMediaRecipients(Collection<MediaRecipient> mediaRecipients) {
        final List<ReportEntity.RecipientEntity> entities = new ArrayList<>(mediaRecipients.size());

        for (MediaRecipient mediaRecipient: mediaRecipients) {
            final ReportEntity.RecipientEntity entity = transform(mediaRecipient);
            if (entity != null) {
                entities.add(entity);
            }
        }
        return entities;
    }

    public MetadataEntity transform(Metadata metadata) {
        MetadataEntity entity = null;

        if (metadata != null) {
            entity = new MetadataEntity();
            entity.setWifis(metadata.getWifis());
            entity.setCells(metadata.getCells());
            entity.setAmbientTemperature(metadata.getAmbientTemperature());
            entity.setLight(metadata.getLight());
            entity.setTimestamp(metadata.getTimestamp());
            entity.setLocation(transform(metadata.getEvidenceLocation()));
        }

        return entity;
    }

    public Metadata transform(MetadataEntity metadataEntity) {
        Metadata metadata = null;

        if (metadataEntity != null) {
            metadata = new Metadata();
            metadata.setWifis(metadataEntity.getWifis());
            metadata.setCells(metadataEntity.getCells());
            metadata.setAmbientTemperature(metadataEntity.getAmbientTemperature());
            metadata.setLight(metadataEntity.getLight());
            metadata.setTimestamp(metadataEntity.getTimestamp());
            metadata.setEvidenceLocation(transform(metadataEntity.getLocation()));
        }

        return metadata;
    }

    private MetadataEntity.LocationEntity transform(EvidenceLocation location) {
        MetadataEntity.LocationEntity entity = null;

        if (location != null) {
            entity = new MetadataEntity.LocationEntity();
            entity.setLatitude(location.getLatitude());
            entity.setLongitude(location.getLongitude());
            entity.setAccuracy(location.getAccuracy());
            entity.setAltitude(location.getAltitude());
        }

        return entity;
    }

    private EvidenceLocation transform(MetadataEntity.LocationEntity locationEntity) {
        EvidenceLocation location = null;

        if (locationEntity != null) {
            location = new EvidenceLocation();
            location.setLatitude(locationEntity.getLatitude());
            location.setLongitude(locationEntity.getLongitude());
            location.setAccuracy(locationEntity.getAccuracy());
            location.setAltitude(locationEntity.getAltitude());
        }

        return location;
    }

    private MediaFileEntity transform(MediaFile mediaFile) {
        if (mediaFile == null) {
            return null;
        }

        MediaFileEntity mediaFileEntity = new MediaFileEntity();
        mediaFileEntity.id = mediaFile.getId();
        mediaFileEntity.path = mediaFile.getPath();
        mediaFileEntity.uid = mediaFile.getUid();
        mediaFileEntity.fileName = mediaFile.getFileName();
        mediaFileEntity.metadata = transform(mediaFile.getMetadata());
        mediaFileEntity.created = mediaFile.getCreated();

        return mediaFileEntity;
    }

    public FormMediaFileRegisterEntity transformMediaFiles(Collection<MediaFile> mediaFiles) {
        FormMediaFileRegisterEntity entity = new FormMediaFileRegisterEntity();

        if (mediaFiles != null) {
            entity.attachments = new ArrayList<>(mediaFiles.size());
            for (MediaFile mediaFile: mediaFiles) {
                entity.attachments.add(transform(mediaFile));
            }
        }

        return entity;
    }

    public List<TrainModule> transform(List<TrainModuleEntity> entities) {
        List<TrainModule> modules = new ArrayList<>(entities.size());

        for (TrainModuleEntity entity: entities) {
            modules.add(transform(entity));
        }

        return modules;
    }

    private TrainModule transform(TrainModuleEntity entity) {
        TrainModule module = new TrainModule();

        module.setId(entity.id);
        module.setName(entity.name);
        module.setUrl(entity.url);
        module.setDownloaded(DownloadState.NOT_DOWNLOADED);
        module.setOrganization(entity.organization);
        module.setType(entity.type);
        module.setSize(entity.size);

        return module;
    }
}
