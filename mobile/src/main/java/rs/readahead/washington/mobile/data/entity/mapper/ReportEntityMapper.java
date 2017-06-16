package rs.readahead.washington.mobile.data.entity.mapper;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import rs.readahead.washington.mobile.data.entity.MetadataEntity;
import rs.readahead.washington.mobile.data.entity.ReportEntity;
import rs.readahead.washington.mobile.domain.entity.Evidence;
import rs.readahead.washington.mobile.domain.entity.EvidenceLocation;
import rs.readahead.washington.mobile.domain.entity.Metadata;
import rs.readahead.washington.mobile.models.MediaRecipient;
import rs.readahead.washington.mobile.models.Report;


// @Singleton
public class ReportEntityMapper {

    public ReportEntity transform(Report report) {
        ReportEntity entity = null;

        if (report != null) {
            entity = new ReportEntity();
            entity.setEvidences(transform(report.getEvidences()));
            entity.setRecipients(transformMediaRecipients(report.getRecipients().values()));
            entity.setTitle(report.getTitle());
            entity.setContent(report.getContent());
            entity.setDate(report.getDate().getTime()/1000L); // to utc unix_timestamp, erase TZ
            entity.setLocation(report.getLocation());
            entity.setPublicReport(report.isReportPublic());
        }

        return entity;
    }

    private ReportEntity.EvidenceEntity transform(Evidence evidence) {
        ReportEntity.EvidenceEntity entity = null;

        if (evidence != null) {
            entity = new ReportEntity.EvidenceEntity();
            entity.setName(evidence.getUid());
            entity.setPath(evidence.getPath());
            entity.setMetadata(transform(evidence.getMetadata()));
        }

        return entity;
    }

    private List<ReportEntity.EvidenceEntity> transform(Collection<Evidence> evidences) {
        final List<ReportEntity.EvidenceEntity> entities = new ArrayList<>(evidences.size());

        for (Evidence evidence: evidences) {
            final ReportEntity.EvidenceEntity entity = transform(evidence);
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

    private MetadataEntity transform(Metadata metadata) {
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
}
