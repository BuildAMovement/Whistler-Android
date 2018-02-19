package rs.readahead.washington.mobile.data.database;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import net.sqlcipher.database.SQLiteDatabase;
import net.sqlcipher.database.SQLiteQueryBuilder;

import org.javarosa.core.model.FormDef;
import org.javarosa.core.util.externalizable.DeserializationException;
import org.javarosa.core.util.externalizable.ExtUtil;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;

import io.reactivex.Completable;
import io.reactivex.Maybe;
import io.reactivex.Single;
import io.reactivex.subjects.ReplaySubject;
import rs.readahead.washington.mobile.data.entity.MetadataEntity;
import rs.readahead.washington.mobile.data.entity.mapper.EntityMapper;
import rs.readahead.washington.mobile.domain.entity.ContactSetting;
import rs.readahead.washington.mobile.domain.entity.IErrorBundle;
import rs.readahead.washington.mobile.domain.entity.MediaFile;
import rs.readahead.washington.mobile.domain.entity.MediaRecipient;
import rs.readahead.washington.mobile.domain.entity.MediaRecipientList;
import rs.readahead.washington.mobile.domain.entity.Metadata;
import rs.readahead.washington.mobile.domain.entity.Report;
import rs.readahead.washington.mobile.domain.entity.Settings;
import rs.readahead.washington.mobile.domain.entity.TrainModule;
import rs.readahead.washington.mobile.domain.entity.TrustedPerson;
import rs.readahead.washington.mobile.domain.entity.collect.CollectForm;
import rs.readahead.washington.mobile.domain.entity.collect.CollectFormInstance;
import rs.readahead.washington.mobile.domain.entity.collect.CollectFormInstanceStatus;
import rs.readahead.washington.mobile.domain.entity.collect.CollectServer;
import rs.readahead.washington.mobile.domain.entity.collect.ListFormResult;
import rs.readahead.washington.mobile.domain.entity.collect.OdkForm;
import rs.readahead.washington.mobile.domain.exception.NotFountException;
import rs.readahead.washington.mobile.domain.repository.ICollectFormsRepository;
import rs.readahead.washington.mobile.domain.repository.ICollectServersRepository;
import rs.readahead.washington.mobile.domain.repository.ILocalReportRepository;
import rs.readahead.washington.mobile.domain.repository.IMediaFileRecordRepository;
import rs.readahead.washington.mobile.domain.repository.IMediaRecipientsRepository;
import rs.readahead.washington.mobile.domain.repository.ISettingsRepository;
import rs.readahead.washington.mobile.domain.repository.ITrainModuleRepository;
import rs.readahead.washington.mobile.presentation.entity.DownloadState;
import rs.readahead.washington.mobile.presentation.entity.MediaFileThumbnailData;
import rs.readahead.washington.mobile.util.CommonUtils;
import timber.log.Timber;


public class DataSource implements ICollectServersRepository, ICollectFormsRepository,
        IMediaFileRecordRepository, ITrainModuleRepository, ILocalReportRepository, IMediaRecipientsRepository, ISettingsRepository {
    private static DataSource dataSource;
    private SQLiteDatabase database;
    private static Gson gson = new GsonBuilder().create();

    private static volatile ReplaySubject<DataSource> subject = ReplaySubject.createWithSize(1);


    public static synchronized DataSource getInstance(Context context, byte[] key) {
        if (dataSource == null) {
            dataSource = new DataSource(context.getApplicationContext(), key);
            subject.onNext(dataSource);
        }

        return dataSource;
    }

    private DataSource(Context context, byte[] key) {
        WashingtonSQLiteOpenHelper sqLiteOpenHelper = new WashingtonSQLiteOpenHelper(context);
        SQLiteDatabase.loadLibs(context);
        database = sqLiteOpenHelper.getWritableDatabase(key);
    }

    private String[] reportColumns = {
            D.C_ID,
            D.C_TITLE,
            D.C_CONTENT,
            D.C_LOCATION,
            D.C_DATE,
            D.C_CONTACT_INFORMATION_ENABLED,
            D.C_METADATA,
            D.C_SAVED,
            D.C_PUBLIC
    };

    @Override
    public Single<ContactSetting> getContactSetting() {
        return Single.fromCallable(new Callable<ContactSetting>() {
            @Override
            public ContactSetting call() throws Exception {
                return dbGetContactSetting();
            }
        });
    }

    @Override
    public Completable saveContactSetting(final ContactSetting contactSetting) {
        return Completable.fromCallable(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                dbSaveContactSetting(contactSetting);
                return null;
            }
        });
    }

    @Override
    public Completable removeContactSetting() {
        return Completable.fromCallable(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                dbRemoveSetting(Settings.CONTACT_SETTINGS.name());
                return null;
            }
        });
    }

    @Override
    public Single<List<MediaRecipient>> listMediaRecipients() {
        return Single.fromCallable(new Callable<List<MediaRecipient>>() {
            @Override
            public List<MediaRecipient> call() throws Exception {
                return getMediaRecipients();
            }
        });
    }

    @Override
    public Single<List<MediaRecipientList>> listNonEmptyMediaRecipientLists() {
        return Single.fromCallable(new Callable<List<MediaRecipientList>>() {
            @Override
            public List<MediaRecipientList> call() throws Exception {
                return getMediaRecipientListsWithRecipients();
            }
        });
    }

    @Override
    public Single<MediaRecipient> addMediaRecipient(final MediaRecipient mediaRecipient) {
        return Single.fromCallable(new Callable<MediaRecipient>() {
            @Override
            public MediaRecipient call() throws Exception {
                return insertRecipient(mediaRecipient);
            }
        });
    }

    @Override
    public Single<MediaRecipientList> addMediaRecipientList(final MediaRecipientList mediaRecipientList) {
        return Single.fromCallable(new Callable<MediaRecipientList>() {
            @Override
            public MediaRecipientList call() throws Exception {
                return insertMediaRecipientList(mediaRecipientList);
            }
        });
    }

    @Override
    public Single<Map<Long, MediaRecipient>> getCombinedMediaRecipients(final List<MediaRecipient> recipients, final List<MediaRecipientList> lists) {
        return Single.fromCallable(new Callable<Map<Long, MediaRecipient>>() {
            @Override
            public Map<Long, MediaRecipient> call() throws Exception {
                Set<Long> recipientIds = new HashSet<>(recipients.size());
                Set<Integer> listIds = new HashSet<>(lists.size());

                for (MediaRecipient recipient: recipients) {
                    recipientIds.add(recipient.getId());
                }

                for (MediaRecipientList list: lists) {
                    listIds.add(list.getId());
                }

                return getCombinedMediaRecipients(recipientIds, listIds);
            }
        });
    }

    @Override
    public Single<Report> loadReport(final long id) {
        return Single.fromCallable(new Callable<Report>() {
            @Override
            public Report call() throws Exception {
                return dbGetReport(id);
            }
        });
    }

    @Override
    public Single<Report> saveReport(final Report report, final Report.Saved saved) {
        return Single.fromCallable(new Callable<Report>() {
            @Override
            public Report call() throws Exception {
                return insertOnDuplicateUpdateReport(report);
            }
        });
    }

    @Override
    public Completable deleteReport(final long id) {
        return Completable.fromCallable(new Callable<Report>() {
            @Override
            public Report call() throws Exception {
                dbDeleteReport(id);
                return null;
            }
        });
    }

    @Override
    public Single<List<Report>> listReports(final Report.Saved saved) {
        return Single.fromCallable(new Callable<List<Report>>() {
            @Override
            public List<Report> call() throws Exception {
                return dbListReports(saved);
            }
        });
    }

    @Override
    public Single<List<TrainModule>> listTrainModules() {
        return Single.fromCallable(new Callable<List<TrainModule>>() {
            @Override
            public List<TrainModule> call() throws Exception {
                return dataSource.listDBTrainModules();
            }
        });
    }

    @Override
    public Single<List<TrainModule>> updateTrainDBModules(final List<TrainModule> modules) {
        return Single.fromCallable(new Callable<List<TrainModule>>() {
            @Override
            public List<TrainModule> call() throws Exception {
                dataSource.updateTrainModules(modules);
                return dataSource.listDBTrainModules();
            }
        });
    }

    @Override
    public Completable startDownloadTrainModule(final TrainModule module) {
        return Completable.fromCallable(new Callable<List<TrainModule>>() {
            @Override
            public List<TrainModule> call() throws Exception {
                dataSource.startTrainModuleDownload(module);
                return null;
            }
        });
    }

    @Override
    public Completable removeTrainDBModule(final long id) {
        return Completable.fromCallable(new Callable<List<TrainModule>>() {
            @Override
            public List<TrainModule> call() throws Exception {
                dataSource.removeTrainModule(id);
                return null;
            }
        });
    }

    @Override
    public Completable setTrainModuleDownloadState(final long id, final DownloadState state) {
        return Completable.fromCallable(new Callable<Integer>() {
            @Override
            public Integer call() throws Exception {
                dataSource.setTrainModuleDownloadedState(id, state);
                return null;
            }
        });
    }

    @Override
    public Single<List<TrainModule>> listDownloadingModules() {
        return Single.fromCallable(new Callable<List<TrainModule>>() {
            @Override
            public List<TrainModule> call() throws Exception {
                return dataSource.listDBDownloadingTrainModules();
            }
        });
    }

    @Override
    public Single<TrainModule> getTrainModule(final long id) {
        return Single.fromCallable(new Callable<TrainModule>() {
            @Override
            public TrainModule call() throws Exception {
                return dataSource.getDBTrainModule(id);
            }
        });
    }

    @Override
    public Single<List<CollectServer>> listCollectServers() {
        return Single.fromCallable(new Callable<List<CollectServer>>() {
            @Override
            public List<CollectServer> call() throws Exception {
                return dataSource.getServers();
            }
        });
    }

    @Override
    public Single<CollectServer> createCollectServer(final CollectServer server) {
        return Single.fromCallable(new Callable<CollectServer>() {
            @Override
            public CollectServer call() throws Exception {
                return dataSource.createServer(server);
            }
        });
    }

    @Override
    public Single<CollectServer> updateCollectServer(final CollectServer server) {
        return Single.fromCallable(new Callable<CollectServer>() {
            @Override
            public CollectServer call() throws Exception {
                return dataSource.updateServer(server);
            }
        });
    }

    @Override
    public Single<CollectServer> getCollectServer(final long id) {
        return Single.fromCallable(new Callable<CollectServer>() {
            @Override
            public CollectServer call() throws Exception {
                return getServer(id);
            }
        });
    }

    @Override
    public Completable removeCollectServer(final long id) {
        return Completable.fromCallable(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                dataSource.removeServer(id);
                return null;
            }
        });
    }

    @Override
    public Single<Long> countCollectServers() {
        return Single.fromCallable(new Callable<Long>() {
            @Override
            public Long call() throws Exception {
                return dataSource.countDBCollectServers();
            }
        });
    }

    @Override
    public Single<List<CollectForm>> listBlankForms() {
        return Single.fromCallable(new Callable<List<CollectForm>>() {
            @Override
            public List<CollectForm> call() throws Exception {
                return dataSource.getBlankCollectForms();
            }
        });
    }

    @Override
    public Single<ListFormResult> updateBlankForms(final ListFormResult listFormResult) {
        return Single.fromCallable(new Callable<ListFormResult>() {
            @Override
            public ListFormResult call() throws Exception {
                dataSource.updateBlankCollectForms(listFormResult);
                listFormResult.setForms(dataSource.getBlankCollectForms());
                return listFormResult;
            }
        });
    }

    @Override
    public Single<CollectForm> toggleFavorite(final CollectForm form) {
        return Single.fromCallable(new Callable<CollectForm>() {
            @Override
            public CollectForm call() throws Exception {
                return dataSource.toggleFavoriteCollectForm(form);
            }
        });
    }

    @Override
    public Maybe<FormDef> getBlankFormDef(final CollectForm form) {
        return Maybe.fromCallable(new Callable<FormDef>() {
            @Override
            public FormDef call() throws Exception {
                return getCollectFormDef(form);
            }
        });
    }

    @Override
    public Single<FormDef> updateBlankFormDef(final CollectForm form, final FormDef formDef) {
        return Single.fromCallable(new Callable<FormDef>() {
            @Override
            public FormDef call() throws Exception {
                return updateCollectFormDef(form, formDef);
            }
        });
    }

    @Override
    public Single<List<CollectFormInstance>> listDraftForms() {
        return Single.fromCallable(new Callable<List<CollectFormInstance>>() {
            @Override
            public List<CollectFormInstance> call() throws Exception {
                return getDraftCollectFormInstances();
            }
        });
    }

    @Override
    public Single<List<CollectFormInstance>> listSentForms() {
        return Single.fromCallable(new Callable<List<CollectFormInstance>>() {
            @Override
            public List<CollectFormInstance> call() throws Exception {
                return getSubmitCollectFormInstances();
            }
        });
    }

    @Override
    public Single<List<CollectFormInstance>> listPendingForms() {
        return Single.fromCallable(new Callable<List<CollectFormInstance>>() {
            @Override
            public List<CollectFormInstance> call() throws Exception {
                return getPendingCollectFormInstances();
            }
        });
    }

    @Override
    public Single<CollectFormInstance> saveInstance(final CollectFormInstance instance) {
        return Single.fromCallable(new Callable<CollectFormInstance>() {
            @Override
            public CollectFormInstance call() throws Exception {
                return updateCollectFormInstance(instance);
            }
        });
    }

    @Override
    public Single<CollectFormInstance> getInstance(final long id) {
        return Single.fromCallable(new Callable<CollectFormInstance>() {
            @Override
            public CollectFormInstance call() throws Exception {
                return getCollectFormInstance(id);
            }
        });
    }

    @Override
    public Completable deleteInstance(final long id) {
        return Completable.fromCallable(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                deleteCollectFormInstance(id);
                return null;
            }
        });
    }

    @Override
    public Single<MediaFile> registerMediaFile(final MediaFile mediaFile, final MediaFileThumbnailData thumbnailData) {
        return Single.fromCallable(new Callable<MediaFile>() {
            @Override
            public MediaFile call() throws Exception {
                return registerMediaFileRecord(mediaFile, thumbnailData);
            }
        });
    }

    @Override
    public Single<List<MediaFile>> listMediaFiles() {
        return Single.fromCallable(new Callable<List<MediaFile>>() {
            @Override
            public List<MediaFile> call() throws Exception {
                return getMediaFiles();
            }
        });
    }

    @Override
    public Maybe<MediaFileThumbnailData> getMediaFileThumbnail(final long id) {
        return Maybe.fromCallable(new Callable<MediaFileThumbnailData>() {
            @Override
            public MediaFileThumbnailData call() throws Exception {
                return getThumbnail(id);
            }
        });
    }

    @Override
    public Single<MediaFileThumbnailData> updateMediaFileThumbnail(final long id, final MediaFileThumbnailData data) {
        return Single.fromCallable(new Callable<MediaFileThumbnailData>() {
            @Override
            public MediaFileThumbnailData call() throws Exception {
                return updateThumbnail(id, data);
            }
        });
    }

    @Override
    public Single<List<MediaFile>> getMediaFiles(final long[] ids) {
        return Single.fromCallable(new Callable<List<MediaFile>>() {
            @Override
            public List<MediaFile> call() throws Exception {
                return getMediaFilesFromDb(ids);
            }
        });
    }

    @Override
    public Single<MediaFile> deleteMediaFile(final MediaFile mediaFile, final IMediaFileDeleter deleter) {
        return Single.fromCallable(new Callable<MediaFile>() {
            @Override
            public MediaFile call() throws Exception {
                return deleteMediaFileFromDb(mediaFile, deleter);
            }
        });
    }

    @Override
    public Single<MediaFile> attachMetadata(final long mediaFileId, final Metadata metadata) {
        return Single.fromCallable(new Callable<MediaFile>() {
            @Override
            public MediaFile call() throws Exception {
                return attachMediaFileMetadataDb(mediaFileId, metadata);
            }
        });
    }

    private MediaFile registerMediaFileRecord(MediaFile mediaFile, MediaFileThumbnailData thumbnailData) {
        if (mediaFile.getCreated() == 0) {
            mediaFile.setCreated(CommonUtils.currentTimestamp());
        }

        try {
            database.beginTransaction();

            ContentValues values = new ContentValues();
            values.put(D.C_PATH, mediaFile.getPath());
            values.put(D.C_UID, mediaFile.getUid());
            values.put(D.C_FILE_NAME, mediaFile.getFileName());
            values.put(D.C_METADATA, new GsonBuilder().create().toJson(new EntityMapper().transform(mediaFile.getMetadata())));
            values.put(D.C_CREATED, mediaFile.getCreated());
            if (mediaFile.getDuration() > 0) {
                values.put(D.C_DURATION, mediaFile.getDuration());
            }
            values.put(D.C_ANONYMOUS, mediaFile.isAnonymous() ? 1 : 0);

            mediaFile.setId(database.insert(D.T_MEDIA_FILE, null, values));

            if (! MediaFileThumbnailData.NONE.equals(thumbnailData)) {
                updateThumbnail(mediaFile.getId(), thumbnailData);
            }

            database.setTransactionSuccessful();
        } finally {
            database.endTransaction();
        }

        return mediaFile;
    }

    private long countDBCollectServers() {
        return net.sqlcipher.DatabaseUtils.queryNumEntries(database, D.T_COLLECT_SERVER);
    }

    private List<CollectServer> getServers() {
        Cursor cursor = null;
        List<CollectServer> servers = new ArrayList<>();

        try {
            cursor = database.query(
                    D.T_COLLECT_SERVER,
                    new String[] {D.C_ID, D.C_NAME, D.C_URL, D.C_USERNAME, D.C_PASSWORD},
                    null,
                    null,
                    null, null,
                    D.C_ID + " ASC",
                    null);

            for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
                CollectServer collectServer = cursorToCollectServer(cursor);
                servers.add(collectServer);
            }
        } catch (Exception e) {
            Timber.d(e, getClass().getName());
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return servers;
    }

    @Nullable
    private CollectServer getServer(long id) {
        Cursor cursor = null;

        try {
            cursor = database.query(
                    D.T_COLLECT_SERVER,
                    new String[] {D.C_ID, D.C_NAME, D.C_URL, D.C_USERNAME, D.C_PASSWORD},
                    D.C_ID + "= ?",
                    new String[] {Long.toString(id)},
                    null, null, null, null);

            if (cursor.moveToFirst()) {
                return cursorToCollectServer(cursor);
            }
        } catch (Exception e) {
            Timber.d(e, getClass().getName());
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return CollectServer.NONE;
    }

    @Nullable
    private CollectForm getBlankCollectForm(String formID) {
        Cursor cursor = null;

        try {
            final String query = SQLiteQueryBuilder.buildQueryString(
                    false,
                    D.T_COLLECT_BLANK_FORM + " JOIN " + D.T_COLLECT_SERVER + " ON " +
                            D.T_COLLECT_BLANK_FORM + "." + D.C_COLLECT_SERVER_ID + " = " + D.T_COLLECT_SERVER + "." + D.C_ID,
                    new String[] {
                            cn(D.T_COLLECT_BLANK_FORM, D.C_ID, D.A_COLLECT_BLANK_FORM_ID),
                            D.C_COLLECT_SERVER_ID,
                            D.C_FORM_ID,
                            D.T_COLLECT_BLANK_FORM + "." + D.C_NAME,
                            D.C_VERSION,
                            D.C_HASH,
                            D.C_DOWNLOADED,
                            D.C_FAVORITE,
                            D.C_DOWNLOAD_URL,
                            cn(D.T_COLLECT_SERVER, D.C_NAME, D.A_SERVER_NAME),
                            cn(D.T_COLLECT_SERVER, D.C_USERNAME, D.A_SERVER_USERNAME)},
                    D.C_FORM_ID + " = ?",
                    null, null, null, null
            );

            cursor = database.rawQuery(query, new String[] {formID});

            if (cursor.moveToFirst()) {
                OdkForm form = cursorToOdkForm(cursor);

                long id = cursor.getLong(cursor.getColumnIndexOrThrow(D.A_COLLECT_BLANK_FORM_ID));
                long serverId = cursor.getLong(cursor.getColumnIndexOrThrow(D.C_COLLECT_SERVER_ID));
                boolean downloaded = cursor.getInt(cursor.getColumnIndexOrThrow(D.C_DOWNLOADED)) == 1;
                boolean favorite = cursor.getInt(cursor.getColumnIndexOrThrow(D.C_FAVORITE)) == 1;
                String serverName = cursor.getString(cursor.getColumnIndexOrThrow(D.A_SERVER_NAME));
                String username = cursor.getString(cursor.getColumnIndexOrThrow(D.A_SERVER_USERNAME));

                CollectForm collectForm = new CollectForm(serverId, form);
                collectForm.setId(id);
                collectForm.setServerName(serverName);
                collectForm.setUsername(username);
                collectForm.setDownloaded(downloaded);
                collectForm.setFavorite(favorite);

                return collectForm;
            }
        } catch (Exception e) {
            Timber.d(e, getClass().getName());
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return null;
    }

    private List<CollectForm> getBlankCollectForms() {
        Cursor cursor = null;
        List<CollectForm> forms = new ArrayList<>();

        try {
            final String query = SQLiteQueryBuilder.buildQueryString(
                    false,
                    D.T_COLLECT_BLANK_FORM + " JOIN " + D.T_COLLECT_SERVER + " ON " +
                            D.T_COLLECT_BLANK_FORM + "." + D.C_COLLECT_SERVER_ID + " = " + D.T_COLLECT_SERVER + "." + D.C_ID,
                    new String[] {
                            cn(D.T_COLLECT_BLANK_FORM, D.C_ID, D.A_COLLECT_BLANK_FORM_ID),
                            D.C_COLLECT_SERVER_ID,
                            D.C_FORM_ID,
                            D.T_COLLECT_BLANK_FORM + "." + D.C_NAME,
                            D.C_VERSION,
                            D.C_HASH,
                            D.C_DOWNLOADED,
                            D.C_FAVORITE,
                            D.C_DOWNLOAD_URL,
                            cn(D.T_COLLECT_SERVER, D.C_NAME, D.A_SERVER_NAME),
                            cn(D.T_COLLECT_SERVER, D.C_USERNAME, D.A_SERVER_USERNAME)},
                    null, null, null,
                    cn(D.T_COLLECT_BLANK_FORM, D.C_FAVORITE) + " DESC, " + cn(D.T_COLLECT_BLANK_FORM, D.C_ID) + " DESC",
                    null
            );

            cursor = database.rawQuery(query, null);

            for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
                OdkForm form = cursorToOdkForm(cursor);

                // todo: implement cursorToCollectForm
                long id = cursor.getLong(cursor.getColumnIndexOrThrow(D.A_COLLECT_BLANK_FORM_ID));
                long serverId = cursor.getLong(cursor.getColumnIndexOrThrow(D.C_COLLECT_SERVER_ID));
                boolean downloaded = cursor.getInt(cursor.getColumnIndexOrThrow(D.C_DOWNLOADED)) == 1;
                boolean favorite = cursor.getInt(cursor.getColumnIndexOrThrow(D.C_FAVORITE)) == 1;
                String serverName = cursor.getString(cursor.getColumnIndexOrThrow(D.A_SERVER_NAME));
                String username = cursor.getString(cursor.getColumnIndexOrThrow(D.A_SERVER_USERNAME));

                CollectForm collectForm = new CollectForm(serverId, form);
                collectForm.setId(id);
                collectForm.setServerName(serverName);
                collectForm.setUsername(username);
                collectForm.setDownloaded(downloaded);
                collectForm.setFavorite(favorite);

                forms.add(collectForm);
            }
        } catch (Exception e) {
            Timber.d(e, getClass().getName());
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return forms;
    }

    private CollectServer createServer(final CollectServer server) {
        ContentValues values = new ContentValues();
        values.put(D.C_NAME, server.getName());
        values.put(D.C_URL, server.getUrl());
        values.put(D.C_USERNAME, server.getUsername());
        values.put(D.C_PASSWORD, server.getPassword());

        server.setId(database.insert(D.T_COLLECT_SERVER, null, values));

        return server;
    }

    private void updateBlankCollectForms(final ListFormResult result) {
        List<CollectForm> forms = result.getForms();
        List<IErrorBundle> errors = result.getErrors();

        List<String> formIDs = new ArrayList<>(forms.size());
        List<String> errorServerIDs = new ArrayList<>(errors.size());

        for (CollectForm form: forms) {
            formIDs.add(DatabaseUtils.sqlEscapeString(form.getForm().getFormID()));

            CollectForm current = getBlankCollectForm(form.getForm().getFormID());
            ContentValues values = new ContentValues();

            if (current != null) {
                // if hashes are same, do nothing
                if (TextUtils.equals(form.getForm().getHash(), current.getForm().getHash())) {
                    continue;
                }

                values.put(D.C_FAVORITE, current.isFavorite() ? 1 : 0);
            }

            values.put(D.C_COLLECT_SERVER_ID, form.getServerId());
            values.put(D.C_FORM_ID, form.getForm().getFormID());
            values.put(D.C_VERSION, form.getForm().getVersion());
            values.put(D.C_HASH, form.getForm().getHash());
            values.put(D.C_NAME, form.getForm().getName());
            values.put(D.C_DOWNLOAD_URL, form.getForm().getDownloadUrl());

            long id = database.insert(D.T_COLLECT_BLANK_FORM, null, values);
            if (id != -1) {
                form.setId(id);
            }
        }

        // get serverIds with errors in form list
        for (IErrorBundle error: errors) {
            errorServerIDs.add(Long.toString(error.getServerId()));
        }

        // construct where clause for deletion
        String whereClause = "1 = 1";

        if (formIDs.size() > 0) {
            whereClause += " AND (" + D.C_FORM_ID + " NOT IN (" + TextUtils.join(",", formIDs) + "))";
        }

        if (errorServerIDs.size() > 0) {
            whereClause += " AND (" + D.C_COLLECT_SERVER_ID + " NOT IN (" + TextUtils.join(",", errorServerIDs) + "))";
        }

        // delete all forms not sent by server, leave forms from servers with error
        database.delete(D.T_COLLECT_BLANK_FORM, whereClause, null);

        // todo: mark them with updated num, delete everyone not updated..
    }

    private CollectForm toggleFavoriteCollectForm(CollectForm form) {
        ContentValues values = new ContentValues();
        values.put(D.C_FAVORITE, !form.isFavorite());

        int num = database.update(D.T_COLLECT_BLANK_FORM, values, D.C_ID + "= ?", new String[] {Long.toString(form.getId())});
        if (num > 0) {
            form.setFavorite(!form.isFavorite());
        }

        return form;
    }

    @Nullable
    private FormDef getCollectFormDef(CollectForm form) {
        Cursor cursor = null;

        try {
            cursor = database.query(
                    D.T_COLLECT_BLANK_FORM,
                    new String[] {D.C_FORM_DEF},
                    D.C_FORM_ID + "= ? AND " + D.C_VERSION + " = ?",
                    new String[] {form.getForm().getFormID(), form.getForm().getVersion()},
                    null, null, null, null);

            if (cursor.moveToFirst()) {
                // todo: check if byte[] is empty and return null
                return deserializeFormDef(cursor.getBlob(cursor.getColumnIndexOrThrow(D.C_FORM_DEF)));
            }
        } catch (Exception e) {
            Timber.d(e, getClass().getName());
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return null; // let rx crash for this..
    }

    private List<MediaFile> getMediaFilesFromDb(long[] ids) {
        List<MediaFile> mediaFiles = new ArrayList<>();
        Cursor cursor = null;

        if (ids.length == 0) {
            return mediaFiles;
        }

        String[] stringIds = new String[ids.length];
        for(int i = 0; i < ids.length; i++) {
            stringIds[i] = Long.toString(ids[i]);
        }

        try {
            final String query = SQLiteQueryBuilder.buildQueryString(
                    false,
                    D.T_MEDIA_FILE,
                    new String[] {
                            cn(D.T_MEDIA_FILE, D.C_ID, D.A_MEDIA_FILE_ID),
                            D.C_PATH,
                            D.C_UID,
                            D.C_FILE_NAME,
                            D.C_METADATA,
                            D.C_CREATED,
                            D.C_DURATION,
                            D.C_ANONYMOUS},
                    cn(D.T_MEDIA_FILE, D.C_ID) + " IN (" + TextUtils.join(", ", stringIds) + ")",
                    null, null, null, null
            );

            cursor = database.rawQuery(query, null);

            for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
                mediaFiles.add(cursorToMediaFile(cursor));
            }
        } catch (Exception e) {
            Timber.d(e, getClass().getName());
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return mediaFiles;
    }

    private MediaFile getMediaFileFromDb(long id) throws NotFountException {
        Cursor cursor = null;

        try {
            cursor = database.query(
                    D.T_MEDIA_FILE,
                    new String[] {
                            cn(D.T_MEDIA_FILE, D.C_ID, D.A_MEDIA_FILE_ID),
                            D.C_PATH,
                            D.C_UID,
                            D.C_FILE_NAME,
                            D.C_METADATA,
                            D.C_CREATED,
                            D.C_DURATION,
                            D.C_ANONYMOUS},
                    cn(D.T_MEDIA_FILE, D.C_ID) + " = ?",
                    new String[] {Long.toString(id)},
                    null, null, null, null
            );

            if (cursor.moveToFirst()) {
                return cursorToMediaFile(cursor);
            }
        } catch (Exception e) {
            Timber.d(e, getClass().getName());
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        throw new NotFountException();
    }

    private MediaFile deleteMediaFileFromDb(MediaFile mediaFile, IMediaFileDeleter deleter) throws NotFountException {
        try {
            database.beginTransaction();

            int count = database.delete(D.T_MEDIA_FILE, D.C_ID + " = ?", new String[]{Long.toString(mediaFile.getId())});

            if (count != 1) {
                throw new NotFountException();
            }

            if (deleter.delete(mediaFile)) {
                database.setTransactionSuccessful();
            }

            return mediaFile;
        } finally {
            database.endTransaction();
        }
    }

    private MediaFile attachMediaFileMetadataDb(long mediaFileId, @Nullable Metadata metadata) throws NotFountException {
        ContentValues values = new ContentValues();
        values.put(D.C_METADATA, new GsonBuilder().create().toJson(new EntityMapper().transform(metadata)));

        database.update(D.T_MEDIA_FILE, values, D.C_ID + " = ?",
                new String[] {Long.toString(mediaFileId)});

        return getMediaFileFromDb(mediaFileId);
    }

    private CollectFormInstance getCollectFormInstance(long id) {
        Cursor cursor = null;

        try {
            final String query = SQLiteQueryBuilder.buildQueryString(
                    false,
                    D.T_COLLECT_FORM_INSTANCE + " JOIN " + D.T_COLLECT_SERVER + " ON " +
                            cn(D.T_COLLECT_FORM_INSTANCE, D.C_COLLECT_SERVER_ID) + " = " + cn(D.T_COLLECT_SERVER, D.C_ID),
                    new String[] {
                            cn(D.T_COLLECT_FORM_INSTANCE, D.C_ID, D.A_COLLECT_FORM_INSTANCE_ID),
                            D.C_COLLECT_SERVER_ID,
                            D.C_STATUS,
                            D.C_UPDATED,
                            D.C_FORM_ID,
                            D.C_VERSION,
                            D.C_FORM_NAME,
                            D.C_INSTANCE_NAME,
                            D.C_FORM_DEF,
                            cn(D.T_COLLECT_SERVER, D.C_NAME, D.A_SERVER_NAME),
                            cn(D.T_COLLECT_SERVER, D.C_USERNAME, D.A_SERVER_USERNAME)},
                    cn(D.T_COLLECT_FORM_INSTANCE, D.C_ID) + "= ?",
                    null, null, null, null
            );

            cursor = database.rawQuery(query, new String[] {Long.toString(id)});

            if (cursor.moveToFirst()) {
                CollectFormInstance instance = cursorToCollectFormInstance(cursor);

                instance.setFormDef(deserializeFormDef(cursor.getBlob(cursor.getColumnIndexOrThrow(D.C_FORM_DEF))));

                List<MediaFile> mediaFiles = getFormInstanceMediaFilesFromDb(instance.getId());
                instance.setMediaFiles(mediaFiles);

                return instance;
            }
        } catch (Exception e) {
            Timber.d(e, getClass().getName());
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return CollectFormInstance.NONE;
    }

    @SuppressWarnings("MethodOnlyUsedFromInnerClass")
    private void deleteCollectFormInstance(long id) throws NotFountException {
        int count = database.delete(D.T_COLLECT_FORM_INSTANCE, D.C_ID + " = ?", new String[] {Long.toString(id)});

        if (count != 1) {
            throw new NotFountException();
        }
    }

    private List<MediaFile> getFormInstanceMediaFilesFromDb(long instanceId) {
        List<MediaFile> mediaFiles = new ArrayList<>();
        Cursor cursor = null;

        try {
            final String query = SQLiteQueryBuilder.buildQueryString(
                    false,
                    D.T_MEDIA_FILE + " JOIN " + D.T_COLLECT_FORM_INSTANCE_MEDIA_FILE + " ON " +
                            cn(D.T_MEDIA_FILE, D.C_ID) + " = " + cn(D.T_COLLECT_FORM_INSTANCE_MEDIA_FILE, D.C_MEDIA_FILE_ID),
                    new String[] {
                            cn(D.T_MEDIA_FILE, D.C_ID, D.A_MEDIA_FILE_ID),
                            D.C_PATH,
                            D.C_UID,
                            D.C_FILE_NAME,
                            D.C_METADATA,
                            D.C_CREATED,
                            D.C_DURATION,
                            D.C_ANONYMOUS},
                    cn(D.T_COLLECT_FORM_INSTANCE_MEDIA_FILE, D.C_COLLECT_FORM_INSTANCE_ID) + "= ?",
                    null, null, null, null
            );

            cursor = database.rawQuery(query, new String[] {Long.toString(instanceId)});

            for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
                mediaFiles.add(cursorToMediaFile(cursor));
            }
        } catch (Exception e) {
            Timber.d(e, getClass().getName());
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return mediaFiles;
    }

    @Nullable
    private MediaFileThumbnailData getThumbnail(long id) {
        Cursor cursor = null;

        try {
            final String query = SQLiteQueryBuilder.buildQueryString(
                    false,
                    D.T_MEDIA_FILE,
                    new String[] {D.C_THUMBNAIL},
                    D.C_ID + "= ?",
                    null, null, null, null
            );

            cursor = database.rawQuery(query, new String[] {Long.toString(id)});

            if (cursor.moveToFirst()) {
                MediaFileThumbnailData mediaFileThumbnailData =
                        new MediaFileThumbnailData(cursor.getBlob(cursor.getColumnIndexOrThrow(D.C_THUMBNAIL)));
                if (mediaFileThumbnailData.getData() != null) {
                    return mediaFileThumbnailData;
                }
            }
        } catch (Exception e) {
            Timber.d(e, getClass().getName());
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        //return MediaFileThumbnailData.NONE;
        return null;
    }

    private MediaFileThumbnailData updateThumbnail(long mediaFileId, MediaFileThumbnailData thumbnailData) {
        if (thumbnailData.getData() == null) {
            return thumbnailData;
        }

        try {
            ContentValues values = new ContentValues();
            values.put(D.C_THUMBNAIL, thumbnailData.getData());

            database.update(D.T_MEDIA_FILE, values,
                    D.C_ID + "= ?",
                    new String[] {Long.toString(mediaFileId)});
        } catch (Exception e) {
            Timber.d(e, getClass().getName());
        }

        return thumbnailData;
    }

    private List<MediaFile> getMediaFiles() {
        Cursor cursor = null;
        List<MediaFile> mediaFiles = new ArrayList<>();

        try {
            final String query = SQLiteQueryBuilder.buildQueryString(
                    false,
                    D.T_MEDIA_FILE,
                    new String[] {
                            cn(D.T_MEDIA_FILE, D.C_ID, D.A_MEDIA_FILE_ID),
                            D.C_PATH,
                            D.C_UID,
                            D.C_FILE_NAME,
                            D.C_METADATA,
                            D.C_CREATED,
                            D.C_DURATION,
                            D.C_ANONYMOUS},
                    null, null, null,
                    D.C_ID + " DESC",
                    null
            );

            cursor = database.rawQuery(query, null);

            for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
                mediaFiles.add(cursorToMediaFile(cursor));
            }
        } catch (Exception e) {
            Timber.d(e, getClass().getName());
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return mediaFiles;
    }

    private FormDef updateCollectFormDef(CollectForm form, FormDef formDef) {
        try {
            ContentValues values = new ContentValues();
            values.put(D.C_FORM_DEF, serializeFormDef(formDef));
            values.put(D.C_DOWNLOADED, 1);

            database.update(D.T_COLLECT_BLANK_FORM, values,
                    D.C_FORM_ID + "= ? AND " + D.C_VERSION + " = ?",
                    new String[] { form.getForm().getFormID(), form.getForm().getVersion() });

            form.setDownloaded(true);
        } catch (IOException e) {
            Timber.d(e, getClass().getName());
        }

        return formDef;
    }

    private List<CollectFormInstance> getDraftCollectFormInstances() {
        return getCollectFormInstances(new CollectFormInstanceStatus[] {
                CollectFormInstanceStatus.UNKNOWN,
                CollectFormInstanceStatus.DRAFT,
                CollectFormInstanceStatus.FINALIZED
        });
    }

    private List<CollectFormInstance> getSubmitCollectFormInstances() {
        return getCollectFormInstances(new CollectFormInstanceStatus[] {
                CollectFormInstanceStatus.SUBMITTED,
                CollectFormInstanceStatus.SUBMISSION_ERROR,
                CollectFormInstanceStatus.SUBMISSION_PENDING
        });
    }

    private List<CollectFormInstance> getPendingCollectFormInstances() {
        return getCollectFormInstances(new CollectFormInstanceStatus[] {
                CollectFormInstanceStatus.SUBMISSION_PENDING
        });
    }

    private List<CollectFormInstance> getCollectFormInstances(CollectFormInstanceStatus[] statuses) {
        Cursor cursor = null;
        List<CollectFormInstance> instances = new ArrayList<>();

        List<String> s = new ArrayList<>(statuses.length);
        for (CollectFormInstanceStatus status: statuses) {
            s.add(Integer.toString(status.ordinal()));
        }
        String selection = "(" + TextUtils.join(", ", s) + ")";

        try {
            final String query = SQLiteQueryBuilder.buildQueryString(
                    false,
                    D.T_COLLECT_FORM_INSTANCE +
                            " JOIN " + D.T_COLLECT_SERVER + " ON " +
                            cn(D.T_COLLECT_FORM_INSTANCE, D.C_COLLECT_SERVER_ID) + " = " + cn(D.T_COLLECT_SERVER, D.C_ID),
                    new String[] {
                            cn(D.T_COLLECT_FORM_INSTANCE, D.C_ID, D.A_COLLECT_FORM_INSTANCE_ID),
                            D.C_COLLECT_SERVER_ID,
                            D.C_STATUS,
                            D.C_UPDATED,
                            D.C_FORM_ID,
                            D.C_VERSION,
                            D.C_FORM_NAME,
                            D.C_INSTANCE_NAME,
                            D.C_FORM_DEF,
                            cn(D.T_COLLECT_SERVER, D.C_NAME, D.A_SERVER_NAME),
                            cn(D.T_COLLECT_SERVER, D.C_USERNAME, D.A_SERVER_USERNAME)},
                    D.C_STATUS + " IN " + selection,
                    null, null,
                    cn(D.T_COLLECT_FORM_INSTANCE, D.C_ID) + " DESC",
                    null
            );

            cursor = database.rawQuery(query, null);

            for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
                // todo: this is bad, we need to make this not loading everything in loop
                CollectFormInstance instance = cursorToCollectFormInstance(cursor);
                instance.setFormDef(deserializeFormDef(cursor.getBlob(cursor.getColumnIndexOrThrow(D.C_FORM_DEF))));

                List<MediaFile> mediaFiles = getFormInstanceMediaFilesFromDb(instance.getId());
                instance.setMediaFiles(mediaFiles);

                instances.add(instance);
            }
        } catch (Exception e) {
            Timber.d(e, getClass().getName());
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return instances;
    }

    private CollectFormInstance updateCollectFormInstance(CollectFormInstance instance) {
        try {
            int statusOrdinal;
            ContentValues values = new ContentValues();

            if (instance.getId() > 0) {
                values.put(D.C_ID, instance.getId());
            }

            values.put(D.C_COLLECT_SERVER_ID, instance.getServerId());
            values.put(D.C_FORM_ID, instance.getFormID());
            values.put(D.C_VERSION, instance.getVersion());
            values.put(D.C_FORM_NAME, instance.getFormName());
            values.put(D.C_UPDATED, CommonUtils.currentTimestamp());
            values.put(D.C_INSTANCE_NAME, instance.getInstanceName());

            if (instance.getStatus() == CollectFormInstanceStatus.UNKNOWN) {
                statusOrdinal = CollectFormInstanceStatus.DRAFT.ordinal();
            } else {
                statusOrdinal = instance.getStatus().ordinal();
            }
            values.put(D.C_STATUS, statusOrdinal);

            if (instance.getFormDef() != null) {
                values.put(D.C_FORM_DEF, serializeFormDef(instance.getFormDef()));
            }

            database.beginTransaction();

            // insert/update form instance
            long id = database.insertWithOnConflict(
                    D.T_COLLECT_FORM_INSTANCE,
                    null,
                    values,
                    SQLiteDatabase.CONFLICT_REPLACE);
            instance.setId(id);

            // clear MediaFiles
            database.delete(
                    D.T_COLLECT_FORM_INSTANCE_MEDIA_FILE,
                    D.C_COLLECT_FORM_INSTANCE_ID + " = ?",
                    new String[] {Long.toString(id)});

            // insert MediaFiles
            List<MediaFile> mediaFiles = instance.getMediaFiles();
            for (MediaFile mediaFile: mediaFiles) {
                values = new ContentValues();
                values.put(D.C_COLLECT_FORM_INSTANCE_ID, id);
                values.put(D.C_MEDIA_FILE_ID, mediaFile.getId());

                database.insert(D.T_COLLECT_FORM_INSTANCE_MEDIA_FILE, null, values);
            }

            database.setTransactionSuccessful();
        } catch (IOException e) {
            Timber.d(e, getClass().getName());
        } finally {
            database.endTransaction();
        }

        return instance;
    }

    private byte[] serializeFormDef(FormDef formDef) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(bos);

        formDef.writeExternal(dos);
        dos.flush();
        dos.close();

        return bos.toByteArray();
    }

    private FormDef deserializeFormDef(byte[] data) throws DeserializationException, IOException {
        ByteArrayInputStream bis = new ByteArrayInputStream(data);
        DataInputStream dis = new DataInputStream(bis);
        FormDef formDef = new FormDef();

        formDef.readExternal(dis, ExtUtil.defaultPrototypes());
        dis.close();

        return formDef;
    }

    private CollectServer updateServer(final CollectServer server) {
        ContentValues values = new ContentValues();
        values.put(D.C_NAME, server.getName());
        values.put(D.C_URL, server.getUrl());
        values.put(D.C_USERNAME, server.getUsername());
        values.put(D.C_PASSWORD, server.getPassword());

        database.update(D.T_COLLECT_SERVER, values, D.C_ID + "= ?", new String[] {Long.toString(server.getId())});

        return server;
    }

    private void removeServer(long id) {
        try {
            database.beginTransaction();

            database.delete(D.T_COLLECT_FORM_INSTANCE, D.C_COLLECT_SERVER_ID + " = ?",  new String[] {Long.toString(id)});
            database.delete(D.T_COLLECT_SERVER, D.C_ID + " = ?", new String[] {Long.toString(id)});

            database.setTransactionSuccessful();
        } finally {
            database.endTransaction();
        }
    }

    private void deleteTable(String table) {
        database.execSQL("DELETE FROM " + table);
    }

    public void deleteDatabase() {
        deleteTable(D.T_RECIPIENT);
        deleteTable(D.T_TRUSTED_PERSON);
        deleteTable(D.T_REPORT);
        deleteTable(D.T_EVIDENCE);
        deleteTable(D.T_RECIPIENT_LIST);
        deleteTable(D.T_REPORT_RECIPIENT);
        deleteTable(D.T_COLLECT_BLANK_FORM);
        deleteTable(D.T_COLLECT_FORM_INSTANCE);
        deleteTable(D.T_COLLECT_FORM_INSTANCE_MEDIA_FILE);
        deleteTable(D.T_MEDIA_FILE);
        deleteTable(D.T_COLLECT_SERVER);
    }

    public void deleteContacts() {
        deleteTable(D.T_TRUSTED_PERSON);
    }

    public void deleteMediaRecipients() {
        deleteTable(D.T_RECIPIENT);
        deleteTable(D.T_RECIPIENT_LIST);
        deleteTable(D.T_REPORT_RECIPIENT);
    }

    public void deleteTrainModules() {
        deleteTable(D.T_TRAIN_MODULE);
    }

    public void deleteMediaFiles() {
        deleteTable(D.T_MEDIA_FILE);
    }

    public MediaRecipient insertRecipient(final MediaRecipient mediaRecipient) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(D.C_MAIL, mediaRecipient.getMail());
        contentValues.put(D.C_TITLE, mediaRecipient.getTitle());

        long id = database.insert(D.T_RECIPIENT, null, contentValues);
        mediaRecipient.setId(id);

        return mediaRecipient;
    }

    public void updateRecipient(MediaRecipient mediaRecipient) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(D.C_MAIL, mediaRecipient.getMail());
        contentValues.put(D.C_TITLE, mediaRecipient.getTitle());

        database.update(D.T_RECIPIENT, contentValues, D.C_ID + " = ? ", new String[]{String.valueOf(mediaRecipient.getId())});
        database.delete(D.T_REPORT_RECIPIENT, D.C_RECIPIENT_ID + " = ? ", new String[]{String.valueOf(mediaRecipient.getId())});
    }

    public void deleteRecipient(long columnId) {
        database.delete(D.T_RECIPIENT, D.C_ID + " = ?", new String[]{String.valueOf(columnId)});
    }

    @NonNull
    public List<MediaRecipient> getMediaRecipients() {
        List<MediaRecipient> recipients = new ArrayList<>();
        Cursor cursor = null;

        try {
            cursor = database.query(D.T_RECIPIENT, null, null, null, null, null, null);

            for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
                MediaRecipient recipient = cursorToRecipient(cursor);
                recipients.add(recipient);
            }
        } catch (Exception e) {
            Timber.d(e, getClass().getName());
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return recipients;
    }

    @SuppressLint("UseSparseArrays")
    @NonNull
    private Map<Long, MediaRecipient> getCombinedMediaRecipients(
            @NonNull Set<Long> selectedRecipients,
            @NonNull Set<Integer> selectedRecipientLists) {
        Cursor cursor = null;
        Map<Long, MediaRecipient> recipients = new HashMap<>();

        if (selectedRecipients.isEmpty() && selectedRecipientLists.isEmpty()) {
            return recipients;
        }

        List<String> wheres = new ArrayList<>();

        if (! selectedRecipients.isEmpty()) {
            wheres.add(D.C_ID + " IN (" + TextUtils.join(",", selectedRecipients) + ")");
        }

        if (! selectedRecipientLists.isEmpty()) {
            wheres.add(D.C_ID + " IN (SELECT " + D.C_RECIPIENT_ID +
                    " FROM " + D.T_RECIPIENT_RECIPIENT_LIST +
                    " WHERE " + D.C_RECIPIENT_LIST_ID +
                    " IN (" + TextUtils.join(",", selectedRecipientLists) + "))");
        }

        try {
            cursor = database.query(
                    D.T_RECIPIENT, null,
                    TextUtils.join(" OR ", wheres),
                    null, null, null, null);

            for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
                MediaRecipient recipient = cursorToRecipient(cursor);
                recipients.put(recipient.getId(), recipient);
            }
        } catch (Exception e) {
            Timber.d(e, getClass().getName());
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return recipients;
    }

    private MediaRecipient cursorToRecipient(Cursor cursor) {
        MediaRecipient mediaRecipient = new MediaRecipient();

        mediaRecipient.setId(cursor.getInt(cursor.getColumnIndexOrThrow(D.C_ID)));
        mediaRecipient.setTitle(cursor.getString(cursor.getColumnIndexOrThrow(D.C_TITLE)));
        mediaRecipient.setMail(cursor.getString(cursor.getColumnIndexOrThrow(D.C_MAIL)));

        return mediaRecipient;
    }

    private MediaRecipientList cursorToMediaRecipientList(Cursor cursor) {
        MediaRecipientList mediaRecipientList = new MediaRecipientList();

        mediaRecipientList.setId(cursor.getInt(cursor.getColumnIndexOrThrow(D.C_ID)));
        mediaRecipientList.setTitle(cursor.getString(cursor.getColumnIndexOrThrow(D.C_TITLE)));

        return mediaRecipientList;
    }

    public MediaRecipientList insertMediaRecipientList(MediaRecipientList mediaRecipientList) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(D.C_TITLE, mediaRecipientList.getTitle());

        long id = database.insert(D.T_RECIPIENT_LIST, null, contentValues);
        mediaRecipientList.setId((int) id);

        return mediaRecipientList;
    }

    private void updateList(String name, int listId) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(D.C_TITLE, name);

        database.update(D.T_RECIPIENT_LIST, contentValues, D.C_ID + " = ? ", new String[] {Integer.toString(listId)});
    }

    private void deleteMediaRecipientListRecipients(int id) {
        database.delete(D.T_RECIPIENT_RECIPIENT_LIST, D.C_RECIPIENT_LIST_ID + " = ?", new String[] {Integer.toString(id)});
    }

    private void insertMediaRecipientListRecipient(long recipientId, int listId) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(D.C_RECIPIENT_LIST_ID, listId);
        contentValues.put(D.C_RECIPIENT_ID, recipientId);

        database.insert(D.T_RECIPIENT_RECIPIENT_LIST, null, contentValues);
    }

    public void updateMediaRecipientList(MediaRecipientList mediaRecipientList, List<Integer> recipientIds) {
        try {
            database.beginTransaction();

            updateList(mediaRecipientList.getTitle(), mediaRecipientList.getId());

            deleteMediaRecipientListRecipients(mediaRecipientList.getId());
            if (recipientIds.size() > 0) {
                for (Integer recipientId: recipientIds) {
                    insertMediaRecipientListRecipient(recipientId, mediaRecipientList.getId());
                }
            }

            database.setTransactionSuccessful();
        } catch (Exception e) {
            Timber.d(e, getClass().getName());
        } finally {
            database.endTransaction();
        }
    }

    public void deleteList(int listId) {
        database.delete(D.T_RECIPIENT_LIST, D.C_ID + " = ?", new String[]{String.valueOf(listId)});
    }

    private void deleteReportRecipients(Report report) {
        database.delete(D.T_REPORT_RECIPIENT, D.C_REPORT_ID + " = ?", new String[]{String.valueOf(report.getId())});
    }

    private void insertReportRecipients(Report report) {
        for (MediaRecipient recipient: report.getRecipients()) {
            ContentValues contentValues = new ContentValues();
            contentValues.put(D.C_RECIPIENT_ID, recipient.getId());
            contentValues.put(D.C_REPORT_ID, report.getId());

            database.insert(D.T_REPORT_RECIPIENT, null, contentValues);
        }
    }

    @NonNull
    public List<MediaRecipientList> getMediaRecipientLists() {
        List<MediaRecipientList> mediaRecipientLists = new ArrayList<>();
        Cursor cursor = null;

        try {
            cursor = database.query(D.T_RECIPIENT_LIST, null, null, null, null, null, null);

            for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
                mediaRecipientLists.add(cursorToMediaRecipientList(cursor));
            }
        } catch (Exception e) {
            Timber.d(e, getClass().getName());
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return mediaRecipientLists;
    }

    @NonNull
    private List<MediaRecipientList> getMediaRecipientListsWithRecipients() {
        List<MediaRecipientList> mediaRecipientLists = new ArrayList<>();
        Cursor cursor = null;

        try {
            cursor = database.query(D.T_RECIPIENT_LIST, null,
                    D.C_ID + " IN (SELECT DISTINCT " + D.C_RECIPIENT_LIST_ID + " FROM " + D.T_RECIPIENT_RECIPIENT_LIST + ")",
                    null, null, null, null);

            for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
                mediaRecipientLists.add(cursorToMediaRecipientList(cursor));
            }
        } catch (Exception e) {
            Timber.d(e, getClass().getName());
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return mediaRecipientLists;
    }

    @NonNull
    public List<Integer> getRecipientIdsByListId(long listId) {
        List<Integer> ids = new ArrayList<>();
        Cursor cursor = null;

        try {
            cursor = database.query(
                    D.T_RECIPIENT_RECIPIENT_LIST,
                    new String[] {D.C_RECIPIENT_ID},
                    D.C_RECIPIENT_LIST_ID + " = ?", new String[] {Long.toString(listId)},
                    null, null, null);

            for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
                ids.add(cursor.getInt(cursor.getColumnIndexOrThrow(D.C_RECIPIENT_ID)));
            }
        } catch (Exception e) {
            Timber.d(e, getClass().getName());
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return ids;
    }

    public void insertTrusted(TrustedPerson trustedPerson) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(D.C_MAIL, trustedPerson.getMail());
        contentValues.put(D.C_NAME, trustedPerson.getName());
        contentValues.put(D.C_PHONE, trustedPerson.getPhoneNumber());

        database.insert(D.T_TRUSTED_PERSON, null, contentValues);
    }

    public void updateTrusted(TrustedPerson trustedPerson) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(D.C_MAIL, trustedPerson.getMail());
        contentValues.put(D.C_NAME, trustedPerson.getName());
        contentValues.put(D.C_PHONE, trustedPerson.getPhoneNumber());

        database.update(D.T_TRUSTED_PERSON, contentValues, D.C_ID + " = ? ", new String[]{String.valueOf(trustedPerson.getColumnId())});
    }

    public void deleteTrusted(int columnId) {
        database.delete(D.T_TRUSTED_PERSON, D.C_ID + " = ?", new String[]{String.valueOf(columnId)});
    }

    public List<TrustedPerson> getAllTrusted() {
        List<TrustedPerson> trustedPersons = new ArrayList<>();

        Cursor cursor = database.query(D.T_TRUSTED_PERSON, null, null, null, null, null, null);
        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            trustedPersons.add(cursorToTrusted(cursor));
            cursor.moveToNext();
        }
        cursor.close();

        return trustedPersons;
    }

    public List<String> getTrustedPhones() {
        List<String> trustedPersonsPhones = new ArrayList<>();

        Cursor cursor = database.query(D.T_TRUSTED_PERSON, new String[]{D.C_PHONE},
                D.C_PHONE + " IS NOT NULL OR " + D.C_PHONE + " != ''", null, null, null, null);
        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            trustedPersonsPhones.add(cursor.getString(cursor.getColumnIndexOrThrow(D.C_PHONE)));
            cursor.moveToNext();
        }
        cursor.close();

        return trustedPersonsPhones;
    }

    private TrustedPerson cursorToTrusted(Cursor cursor) {
        TrustedPerson trustedPerson = new TrustedPerson();

        trustedPerson.setColumnId(cursor.getInt(cursor.getColumnIndexOrThrow(D.C_ID)));
        trustedPerson.setName(cursor.getString(cursor.getColumnIndexOrThrow(D.C_NAME)));
        trustedPerson.setMail(cursor.getString(cursor.getColumnIndexOrThrow(D.C_MAIL)));
        trustedPerson.setPhoneNumber(cursor.getString(cursor.getColumnIndexOrThrow(D.C_PHONE)));

        return trustedPerson;
    }

    private Report insertOnDuplicateUpdateReport(Report report) {
        ContentValues contentValues = new ContentValues();

        if (report.getId() != Report.UNSAVED_REPORT_ID) {
            contentValues.put(D.C_ID, report.getId());
        }

        contentValues.put(D.C_TITLE, report.getTitle());
        contentValues.put(D.C_CONTENT, report.getContent());
        contentValues.put(D.C_LOCATION, report.getLocation());
        contentValues.put(D.C_CONTACT_INFORMATION_ENABLED, report.isContactInformation() ? 1 : 0);
        contentValues.put(D.C_DATE, report.getDate() != null ? report.getDate().getTime() : null);
        contentValues.put(D.C_METADATA, report.isMetadata() ? 1 : 0);
        contentValues.put(D.C_SAVED, report.getSaved().ordinal());
        contentValues.put(D.C_PUBLIC, report.isReportPublic() ? 1 : 0);

        try {
            database.beginTransaction();

            long reportId = database.insertWithOnConflict(D.T_REPORT, null, contentValues, SQLiteDatabase.CONFLICT_REPLACE);

            report.setId(reportId);

            deleteReportEvidences(report.getId());
            if (report.getEvidences().size() > 0) {
                insertReportEvidences(report);
            }

            deleteReportRecipients(report);
            if (report.getRecipients().size() > 0) {
                insertReportRecipients(report);
            }

            database.setTransactionSuccessful();
        } catch (Exception e) {
            Timber.d(e, getClass().getName());
        } finally {
            database.endTransaction();
        }

        return report;
    }

    private void insertReportEvidences(Report report) {
        final String sql = createSQLInsert(D.T_REPORT_MEDIA_FILE, new String[] {
                D.C_REPORT_ID,
                D.C_MEDIA_FILE_ID
        });

        // todo: close resources
        net.sqlcipher.database.SQLiteStatement stmt = database.compileStatement(sql);

        for (MediaFile mediaFile: report.getEvidences()) {
            stmt.bindLong(1, report.getId());
            stmt.bindLong(2, mediaFile.getId());
            stmt.execute();
        }
    }

    private void dbDeleteReport(long id) {
        database.delete(D.T_REPORT, D.C_ID + " = ? ", new String[]{String.valueOf(id)});
        // everything connected (recipients, media_file) is on delete cascade
    }

    private List<Report> dbListReports(Report.Saved saved) {
        List<Report> reports = new ArrayList<>();
        Cursor cursor = null;

        try {
            cursor = database.query(
                    D.T_REPORT,
                    reportColumns,
                    D.C_SAVED + " = ?",
                    new String[] { Integer.toString(saved.ordinal()) },
                    null, null, D.C_ID + " DESC");

            for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
                Report report = cursorToReport(cursor);
                report.setEvidences(getReportEvidences(report.getId()));
                report.setRecipients(getReportRecipients(report.getId()));

                reports.add(report);
            }
        } catch (Exception e) {
            Timber.d(e, getClass().getName());
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return reports;
    }

    private Report dbGetReport(long id) {
        Cursor cursor = null;

        try {
            cursor = database.query(
                    D.T_REPORT,
                    reportColumns,
                    D.C_ID + " = ?",
                    new String[] { Long.toString(id) },
                    null, null, null);

            if (cursor.getCount() == 1) {
                cursor.moveToFirst();

                Report report = cursorToReport(cursor);
                report.setEvidences(getReportEvidences(report.getId()));
                report.setRecipients(getReportRecipients(report.getId()));

                return report;
            }
        } catch (Exception e) {
            Timber.d(e, getClass().getName());
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return Report.NONE;
    }

    private List<MediaFile> getReportEvidences(long reportId) {
        List<MediaFile> mediaFiles = new ArrayList<>();
        Cursor cursor = null;

        try {
            final String query = SQLiteQueryBuilder.buildQueryString(
                    false,
                    D.T_MEDIA_FILE + " JOIN " + D.T_REPORT_MEDIA_FILE + " ON " +
                            cn(D.T_MEDIA_FILE, D.C_ID) + " = " + cn(D.T_REPORT_MEDIA_FILE, D.C_MEDIA_FILE_ID),
                    new String[] {
                            cn(D.T_MEDIA_FILE, D.C_ID, D.A_MEDIA_FILE_ID),
                            D.C_PATH,
                            D.C_UID,
                            D.C_FILE_NAME,
                            D.C_METADATA,
                            D.C_CREATED,
                            D.C_DURATION,
                            D.C_ANONYMOUS},
                    cn(D.T_REPORT_MEDIA_FILE, D.C_REPORT_ID) + "= ?",
                    null, null, null, null
            );

            cursor = database.rawQuery(query, new String[] {Long.toString(reportId)});

            for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
                mediaFiles.add(cursorToMediaFile(cursor));
            }
        } catch (Exception e) {
            Timber.d(e, getClass().getName());
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return mediaFiles;
    }

    @SuppressLint("UseSparseArrays")
    private List<MediaRecipient> getReportRecipients(long reportId) {
        Cursor cursor = null;
        List<MediaRecipient> recipients = new ArrayList<>();

        final String query = SQLiteQueryBuilder.buildQueryString(
                true,
                D.T_RECIPIENT + " JOIN " + D.T_REPORT_RECIPIENT + " AS RR ON " +
                        D.T_RECIPIENT + "." + D.C_ID + " = RR." + D.C_RECIPIENT_ID,
                new String[] {D.C_ID, D.C_TITLE, D.C_MAIL},
                "RR." + D.C_REPORT_ID + " = " + String.valueOf(reportId),
                null, null, null, null
        );

        try {
            cursor = database.rawQuery(query, null);

            for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
                recipients.add(cursorToRecipient(cursor));
            }
        } catch (Exception e) {
            Timber.d(e, getClass().getName());
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return recipients;
    }

    @Nullable
    public MediaRecipientList getMediaRecipientList(int id) {
        Cursor cursor = null;

        try {
            cursor = database.query(
                    D.T_RECIPIENT_LIST,
                    new String[] {D.C_ID, D.C_TITLE},
                    D.C_ID + "= ?", new String[] {Long.toString(id)},
                    null, null, null);

            if (cursor.getCount() == 1) {
                cursor.moveToFirst();
                return cursorToMediaRecipientList(cursor);
            }
        } catch (Exception e) {
            Timber.d(e, getClass().getName());
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return null;
    }

    private Report cursorToReport(Cursor cursor) {
        Report report = new Report();
        Date date = null;

        int dateColumnIndex = cursor.getColumnIndexOrThrow(D.C_DATE);
        if (! cursor.isNull(dateColumnIndex)) {
            date = new Date(cursor.getLong(dateColumnIndex));
        }
        report.setDate(date);

        report.setId(cursor.getInt(cursor.getColumnIndexOrThrow(D.C_ID)));
        report.setTitle(cursor.getString(cursor.getColumnIndexOrThrow(D.C_TITLE)));
        report.setContent(cursor.getString(cursor.getColumnIndexOrThrow(D.C_CONTENT)));
        report.setLocation(cursor.getString(cursor.getColumnIndexOrThrow(D.C_LOCATION)));
        report.setContactInformation(cursor.getInt(cursor.getColumnIndexOrThrow(D.C_CONTACT_INFORMATION_ENABLED)) == 1);
        report.setMetadata(cursor.getLong(cursor.getColumnIndexOrThrow(D.C_METADATA)) == 1);
        report.setSaved(Report.Saved.values()[cursor.getInt(cursor.getColumnIndexOrThrow(D.C_SAVED))]);
        report.setReportPublic(cursor.getLong(cursor.getColumnIndexOrThrow(D.C_PUBLIC)) == 1);

        return report;
    }

    private CollectServer cursorToCollectServer(Cursor cursor) {
        CollectServer collectServer = new CollectServer();
        collectServer.setId(cursor.getLong(cursor.getColumnIndexOrThrow(D.C_ID)));
        collectServer.setName(cursor.getString(cursor.getColumnIndexOrThrow(D.C_NAME)));
        collectServer.setUrl(cursor.getString(cursor.getColumnIndexOrThrow(D.C_URL)));
        collectServer.setUsername(cursor.getString(cursor.getColumnIndexOrThrow(D.C_USERNAME)));
        collectServer.setPassword(cursor.getString(cursor.getColumnIndexOrThrow(D.C_PASSWORD)));

        return collectServer;
    }

    private OdkForm cursorToOdkForm(Cursor cursor) {
        OdkForm odkForm = new OdkForm();
        odkForm.setFormID(cursor.getString(cursor.getColumnIndexOrThrow(D.C_FORM_ID)));
        odkForm.setName(cursor.getString(cursor.getColumnIndexOrThrow(D.C_NAME)));
        odkForm.setVersion(cursor.getString(cursor.getColumnIndexOrThrow(D.C_VERSION)));
        odkForm.setHash(cursor.getString(cursor.getColumnIndexOrThrow(D.C_HASH)));
        odkForm.setDownloadUrl(cursor.getString(cursor.getColumnIndexOrThrow(D.C_DOWNLOAD_URL)));

        return odkForm;
    }

    private CollectFormInstance cursorToCollectFormInstance(Cursor cursor) {
        CollectFormInstance instance = new CollectFormInstance();
        instance.setId(cursor.getLong(cursor.getColumnIndexOrThrow(D.A_COLLECT_FORM_INSTANCE_ID)));
        instance.setServerId(cursor.getLong(cursor.getColumnIndexOrThrow(D.C_COLLECT_SERVER_ID)));
        instance.setServerName(cursor.getString(cursor.getColumnIndexOrThrow(D.A_SERVER_NAME)));
        instance.setUsername(cursor.getString(cursor.getColumnIndexOrThrow(D.A_SERVER_USERNAME)));
        int statusOrdinal = cursor.getInt(cursor.getColumnIndexOrThrow(D.C_STATUS));
        instance.setStatus(CollectFormInstanceStatus.values()[statusOrdinal]);
        instance.setUpdated(cursor.getLong(cursor.getColumnIndexOrThrow(D.C_UPDATED)));
        instance.setFormID(cursor.getString(cursor.getColumnIndexOrThrow(D.C_FORM_ID)));
        instance.setVersion(cursor.getString(cursor.getColumnIndexOrThrow(D.C_VERSION)));
        instance.setFormName(cursor.getString(cursor.getColumnIndexOrThrow(D.C_FORM_NAME)));
        instance.setInstanceName(cursor.getString(cursor.getColumnIndexOrThrow(D.C_INSTANCE_NAME)));

        return instance;
    }

    private MediaFile cursorToMediaFile(Cursor cursor) {
        String path = cursor.getString(cursor.getColumnIndexOrThrow(D.C_PATH));
        String uid = cursor.getString(cursor.getColumnIndexOrThrow(D.C_UID));
        String fileName = cursor.getString(cursor.getColumnIndexOrThrow(D.C_FILE_NAME));
        MetadataEntity metadataEntity = new Gson().fromJson(cursor.getString(cursor.getColumnIndexOrThrow(D.C_METADATA)), MetadataEntity.class);

        MediaFile mediaFile = new MediaFile(path, uid, fileName);
        mediaFile.setId(cursor.getLong(cursor.getColumnIndexOrThrow(D.A_MEDIA_FILE_ID)));
        mediaFile.setMetadata(new EntityMapper().transform(metadataEntity));
        mediaFile.setCreated(cursor.getLong(cursor.getColumnIndexOrThrow(D.C_CREATED)));
        mediaFile.setDuration(cursor.getLong(cursor.getColumnIndexOrThrow(D.C_DURATION)));
        mediaFile.setAnonymous(cursor.getInt(cursor.getColumnIndexOrThrow(D.C_ANONYMOUS)) == 1);

        // todo: MediaFile.Builder

        return mediaFile;
    }

    private TrainModule cursorToTrainModule(Cursor cursor) {
        int downloaded = cursor.getInt(cursor.getColumnIndexOrThrow(D.C_DOWNLOADED));

        TrainModule trainModule = new TrainModule();
        trainModule.setId(cursor.getLong(cursor.getColumnIndexOrThrow(D.C_TRAIN_MODULE_ID)));
        trainModule.setDownloaded(DownloadState.values()[downloaded]);
        trainModule.setName(cursor.getString(cursor.getColumnIndexOrThrow(D.C_NAME)));
        trainModule.setUrl(cursor.getString(cursor.getColumnIndexOrThrow(D.C_URL)));
        trainModule.setOrganization(cursor.getString(cursor.getColumnIndexOrThrow(D.C_ORGANIZATION)));
        trainModule.setType(cursor.getString(cursor.getColumnIndexOrThrow(D.C_TYPE)));
        trainModule.setSize(cursor.getLong(cursor.getColumnIndexOrThrow(D.C_SIZE)));

        return trainModule;
    }

    private void deleteReportEvidences(long reportId) {
        database.delete(D.T_REPORT_MEDIA_FILE, D.C_REPORT_ID + " = ? ", new String[]{String.valueOf(reportId)});
    }

    private static String createSQLInsert(final String tableName, final String[] columnNames) {
        if (tableName == null || columnNames == null || columnNames.length == 0) {
            throw new IllegalArgumentException();
        }

        return "INSERT INTO " + tableName + " (" +
                TextUtils.join(", ", columnNames) +
                ") VALUES( " +
                TextUtils.join(", ", Collections.nCopies(columnNames.length, "?")) +
                ")";
    }

    private String cn(String table, String column) {
        return table + "." + column;
    }

    private String cn(String table, String column, String as) {
        return table + "." + column + " AS " + as;
    }

    private List<TrainModule> listDBTrainModules() {
        Cursor cursor = null;
        List<TrainModule> modules = new ArrayList<>();

        try {
            cursor = database.query(
                    D.T_TRAIN_MODULE,
                    new String[] {D.C_TRAIN_MODULE_ID, D.C_DOWNLOADED, D.C_NAME, D.C_URL, D.C_ORGANIZATION, D.C_TYPE, D.C_SIZE},
                    null,
                    null,
                    null, null,
                    D.C_DOWNLOADED + " DESC",
                    null);

            for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
                TrainModule trainModule = cursorToTrainModule(cursor);
                modules.add(trainModule);
            }
        } catch (Exception e) {
            Timber.d(e, getClass().getName());
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return modules;
    }

    private List<TrainModule> listDBDownloadingTrainModules() {
        Cursor cursor = null;
        List<TrainModule> modules = new ArrayList<>();

        try {
            cursor = database.query(
                    D.T_TRAIN_MODULE,
                    new String[] { D.C_TRAIN_MODULE_ID, D.C_DOWNLOADED, D.C_NAME, D.C_URL, D.C_ORGANIZATION, D.C_TYPE, D.C_SIZE },
                    D.C_DOWNLOADED + "= ?",
                    new String[] { Integer.toString(DownloadState.DOWNLOADING.ordinal()) },
                    null,
                    null,
                    null,
                    null);

            for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
                TrainModule trainModule = cursorToTrainModule(cursor);
                modules.add(trainModule);
            }
        } catch (Exception e) {
            Timber.d(e, getClass().getName());
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return modules;
    }

    private void removeTrainModule(long id) {
        database.delete(D.T_TRAIN_MODULE, D.C_TRAIN_MODULE_ID + "= ?", new String[] {Long.toString(id)});
    }

    private void insertOrIgnoreTrainModule(final TrainModule module) {
        ContentValues values = new ContentValues();

        values.put(D.C_TRAIN_MODULE_ID, module.getId());
        values.put(D.C_NAME, module.getName());
        values.put(D.C_URL, module.getUrl());
        values.put(D.C_DOWNLOADED, module.getDownloaded().ordinal());
        values.put(D.C_ORGANIZATION, module.getOrganization());
        values.put(D.C_TYPE, module.getType());
        values.put(D.C_SIZE, module.getSize());

        database.insertWithOnConflict(D.T_TRAIN_MODULE, null, values, SQLiteDatabase.CONFLICT_IGNORE);
    }

    private void startTrainModuleDownload(final TrainModule module) {
        ContentValues values = new ContentValues();

        values.put(D.C_TRAIN_MODULE_ID, module.getId());
        values.put(D.C_NAME, module.getName());
        values.put(D.C_URL, module.getUrl());
        values.put(D.C_DOWNLOADED, DownloadState.DOWNLOADING.ordinal());
        values.put(D.C_ORGANIZATION, module.getOrganization());
        values.put(D.C_TYPE, module.getType());
        values.put(D.C_SIZE, module.getSize());

        database.replace(D.T_TRAIN_MODULE, null, values);
    }

    private void setTrainModuleDownloadedState(long id, DownloadState state) {
        ContentValues values = new ContentValues();
        values.put(D.C_DOWNLOADED, state.ordinal());

        database.update(D.T_TRAIN_MODULE, values, D.C_TRAIN_MODULE_ID + " = ? ", new String[] {Long.toString(id)});
    }

    private void updateTrainModules(final List<TrainModule> modules) {
        for (TrainModule module: modules) {
            insertOrIgnoreTrainModule(module);
        }
    }

    @Nullable
    private ContactSetting dbGetContactSetting() {
        Setting setting = dbGetSetting(Settings.CONTACT_SETTINGS.name());

        if (setting == null) {
            return ContactSetting.NONE;
        }

        return gson.fromJson(setting.stringValue, ContactSetting.class);
    }

    private void dbSaveContactSetting(ContactSetting setting) {
        dbSaveSetting(Settings.CONTACT_SETTINGS.name(), null,
                gson.toJson(setting));
    }

    @Nullable
    private Setting dbGetSetting(String name) {
        Cursor cursor = null;

        try {
            cursor = database.query(
                    D.T_SETTINGS,
                    new String[] {D.C_NAME, D.C_INT_VALUE, D.C_TEXT_VALUE },
                    D.C_NAME + "= ?", new String[] {name},
                    null, null, null);

            if (cursor.getCount() == 1) {
                cursor.moveToFirst();
                return cursorToSetting(cursor);
            }
        } catch (Exception e) {
            Timber.d(e, getClass().getName());
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return null;
    }

    @SuppressWarnings("SameParameterValue")
    private void dbSaveSetting(String name, @Nullable Integer intValue, @Nullable String stringValue) {
        ContentValues values = new ContentValues();

        values.put(D.C_NAME, name);
        values.put(D.C_INT_VALUE, intValue);
        values.put(D.C_TEXT_VALUE, stringValue);

        database.replace(D.T_SETTINGS, null, values);
    }

    private void dbRemoveSetting(String name) {
        database.delete(D.T_SETTINGS, D.C_NAME + " = ?", new String[] {name});
    }

    /*private boolean dbExistsSetting(String name) {
        Cursor cursor = null;

        try {
            cursor = database.query(D.T_SETTINGS, new String[] {D.C_NAME},
                    D.C_NAME + "= ?", new String[] {name},
                    null, null, null);

            return cursor.getCount() > 0;
        } catch (Exception e) {
            Timber.d(e, getClass().getName());
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return false;
    }*/

    private Setting cursorToSetting(Cursor cursor) {
        int intIndex = cursor.getColumnIndexOrThrow(D.C_INT_VALUE),
                stringIndex = cursor.getColumnIndexOrThrow(D.C_TEXT_VALUE);

        Setting setting = new Setting();

        if (! cursor.isNull(intIndex)) {
            setting.intValue = cursor.getInt(intIndex);
        }

        if (! cursor.isNull(stringIndex)) {
            setting.stringValue = cursor.getString(stringIndex);
        }

        return setting;
    }

    private TrainModule getDBTrainModule(long id) {
        Cursor cursor = null;

        try {
            cursor = database.query(
                    D.T_TRAIN_MODULE,
                    new String[] { D.C_TRAIN_MODULE_ID, D.C_DOWNLOADED, D.C_NAME, D.C_URL, D.C_ORGANIZATION, D.C_TYPE, D.C_SIZE },
                    D.C_TRAIN_MODULE_ID + "= ?",
                    new String[] { Long.toString(id) },
                    null,
                    null,
                    null,
                    null);

            if (cursor.moveToFirst()) {
                return cursorToTrainModule(cursor);
            }
        } catch (Exception e) {
            Timber.d(e, getClass().getName());
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return TrainModule.NONE;
    }

    private static class Setting {
        Integer intValue;
        String stringValue;
    }
}
