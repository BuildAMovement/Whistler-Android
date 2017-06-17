package rs.readahead.washington.mobile.database;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import net.sqlcipher.database.SQLiteDatabase;
import net.sqlcipher.database.SQLiteQueryBuilder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import info.guardianproject.cacheword.CacheWordHandler;
import rs.readahead.washington.mobile.domain.entity.Evidence;
import rs.readahead.washington.mobile.domain.entity.Metadata;
import rs.readahead.washington.mobile.models.MediaRecipient;
import rs.readahead.washington.mobile.models.MediaRecipientList;
import rs.readahead.washington.mobile.models.Report;
import rs.readahead.washington.mobile.models.TrustedPerson;
import rs.readahead.washington.mobile.util.StringUtils;
import timber.log.Timber;


public class DataSource {
    private static DataSource dataSource;
    private WashingtonSQLiteOpenHelper sqLiteOpenHelper;
    private SQLiteDatabase database;


    public static synchronized DataSource getInstance(CacheWordHandler cacheWord, Context context) {
        if (dataSource == null) {
            dataSource = new DataSource(cacheWord, context.getApplicationContext());
            dataSource.open(context);
        }

        return dataSource;
    }

    private DataSource(CacheWordHandler cacheWord, Context context) {
        sqLiteOpenHelper = new WashingtonSQLiteOpenHelper(cacheWord, context);
    }

    private void open(Context context) throws SQLException {
        SQLiteDatabase.loadLibs(context);
        database = sqLiteOpenHelper.getWritableDatabase();
    }

    private String[] allColumnsReports = {
            D.C_ID,
            D.C_TITLE,
            D.C_CONTENT,
            D.C_LOCATION,
            D.C_DATE,
            D.C_METADATA,
            D.C_ARCHIVED,
            D.C_DRAFT,
            D.C_PUBLIC
    };

    /*private String[] allColumnsRecipients = {
            D.C_ID,
            D.C_TITLE,
            D.C_MAIL
    };*/

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
    }

    public void deleteContacts() {
        deleteTable(D.T_TRUSTED_PERSON);
    }

    public void deleteMedia() {
        deleteTable(D.T_RECIPIENT);
        deleteTable(D.T_RECIPIENT_LIST);
        deleteTable(D.T_REPORT_RECIPIENT);
    }

    public void insertRecipient(final MediaRecipient mediaRecipient) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(D.C_MAIL, mediaRecipient.getMail());
        contentValues.put(D.C_TITLE, mediaRecipient.getTitle());

        long id = database.insert(D.T_RECIPIENT, null, contentValues);

        mediaRecipient.setId(id);
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
    public Map<Long, MediaRecipient> getCombinedMediaRecipients(
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

    public void insertMediaRecipientList(MediaRecipientList mediaRecipientList) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(D.C_TITLE, mediaRecipientList.getTitle());

        long id = database.insert(D.T_RECIPIENT_LIST, null, contentValues);

        mediaRecipientList.setId((int) id);
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
        final String sql = createSQLInsert(D.T_REPORT_RECIPIENT, new String[]{
                D.C_RECIPIENT_ID,
                D.C_REPORT_ID
        });

        net.sqlcipher.database.SQLiteStatement stmt = database.compileStatement(sql);

        for (long recipientId: report.getRecipients().keySet()) {
            stmt.bindLong(1, recipientId);
            stmt.bindLong(2, report.getId());
            stmt.execute();
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
    public List<MediaRecipientList> getMediaRecipientListsWithRecipients() {
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

    public void insertReport(Report report) {
        ContentValues contentValues = new ContentValues();

        contentValues.put(D.C_TITLE, StringUtils.orDefault(report.getTitle(), ""));
        contentValues.put(D.C_CONTENT, StringUtils.orDefault(report.getContent(), ""));
        contentValues.put(D.C_LOCATION, StringUtils.orDefault(report.getLocation(), ""));
        contentValues.put(D.C_DATE, report.getDate().getTime());
        contentValues.put(D.C_METADATA, report.isMetadataSelected() ? 1 : 0);
        contentValues.put(D.C_ARCHIVED, report.isKeptInArchive() ? 1 : 0);
        contentValues.put(D.C_DRAFT, report.isKeptInDrafts() ? 1 : 0);
        contentValues.put(D.C_PUBLIC, report.isReportPublic() ? 1 : 0);

        try {
            database.beginTransaction();

            long reportId = database.insert(D.T_REPORT, null, contentValues);

            report.setId(reportId);

            if (report.getEvidences().size() > 0) {
                insertReportEvidences(report);
            }

            if (report.getRecipients().size() > 0) {
                insertReportRecipients(report);
            }

            database.setTransactionSuccessful();
        } catch (Exception e) {
            Timber.d(e, getClass().getName());
        } finally {
            database.endTransaction();
        }
    }

    private void insertReportEvidences(Report report) {
        final String sql = createSQLInsert(D.T_EVIDENCE, new String[]{
                D.C_REPORT_ID,
                D.C_NAME,
                D.C_PATH,
                D.C_METADATA
        });

        net.sqlcipher.database.SQLiteStatement stmt = database.compileStatement(sql);

        for (Evidence evidence : report.getEvidences()) {
            Metadata metadata = evidence.getMetadata();
            Gson gson = new GsonBuilder().create();
            String json = gson.toJson(metadata);
            stmt.bindLong(1, report.getId());
            stmt.bindString(2, evidence.getUid());
            stmt.bindString(3, evidence.getPath());
            stmt.bindString(4, json);

            stmt.execute();
        }
    }

    public void deleteReport(Report report) {
        database.delete(D.T_REPORT, D.C_ID + " = ? ", new String[]{String.valueOf(report.getId())});

        if (report.getEvidences().size() > 0) {
            deleteReportEvidences(report.getId());
        }
    }

    public void updateReport(Report report) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(D.C_TITLE, StringUtils.orDefault(report.getTitle(), ""));
        contentValues.put(D.C_CONTENT, StringUtils.orDefault(report.getContent(), ""));
        contentValues.put(D.C_LOCATION, StringUtils.orDefault(report.getLocation(), ""));
        contentValues.put(D.C_DATE, report.getDate().getTime());
        contentValues.put(D.C_METADATA, report.isMetadataSelected() ? 1 : 0);
        contentValues.put(D.C_ARCHIVED, report.isKeptInArchive() ? 1 : 0);
        contentValues.put(D.C_DRAFT, report.isKeptInDrafts() ? 1 : 0);
        contentValues.put(D.C_PUBLIC, report.isReportPublic() ? 1 : 0);

        try {
            database.beginTransaction();

            database.update(D.T_REPORT, contentValues, D.C_ID + " = ? ", new String[]{String.valueOf(report.getId())});

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
    }

    public List<Report> getDraftReports() {
        List<Report> reports = new ArrayList<>();

        Cursor cursor = database.query(D.T_REPORT, allColumnsReports, D.C_DRAFT + " = 1", null, null, null, null);
        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            Report report;
            report = cursorToReport(cursor);
            report.setEvidences(getEvidences(report.getId()));
            report.setRecipients(getRecipients(report.getId()));
            reports.add(report);
            cursor.moveToNext();
        }
        cursor.close();

        return reports;
    }

    public List<Report> getArchivedReports() {
        List<Report> reports = new ArrayList<>();

        Cursor cursor = database.query(D.T_REPORT, allColumnsReports, D.C_ARCHIVED + " = 1", null, null, null, null);
        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            Report report;
            report = cursorToReport(cursor);
            report.setEvidences(getEvidences(report.getId()));
            report.setRecipients(getRecipients(report.getId()));
            reports.add(report);
            cursor.moveToNext();
        }
        cursor.close();

        return reports;
    }

    private List<Evidence> getEvidences(long reportId) {
        List<Evidence> evidences = new ArrayList<>();

        Cursor cursor = database.query(
                D.T_EVIDENCE,
                null,
                D.C_REPORT_ID + " = " + reportId,
                null, null, null, null);

        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            Evidence evidence = new Evidence();
            evidence.setPath(cursor.getString(cursor.getColumnIndexOrThrow(D.C_PATH)));
            evidence.setUid(cursor.getString(cursor.getColumnIndexOrThrow(D.C_NAME)));
            evidence.setMetadata( new Gson().fromJson(cursor.getString(cursor.getColumnIndexOrThrow(D.C_METADATA)), Metadata.class));
            evidences.add(evidence);
            cursor.moveToNext();
        }
        cursor.close();

        return evidences;
    }

    @SuppressLint("UseSparseArrays")
    private Map<Long, MediaRecipient> getRecipients(long reportId) {
        Cursor cursor = null;
        Map<Long, MediaRecipient> recipients = new HashMap<>();

        final String query = SQLiteQueryBuilder.buildQueryString(
                false,
                D.T_RECIPIENT + " JOIN " + D.T_REPORT_RECIPIENT + " AS RR ON " +
                        D.T_RECIPIENT + "." + D.C_ID + " = RR." + D.C_RECIPIENT_ID,
                new String[] {D.C_ID, D.C_TITLE, D.C_MAIL},
                "RR." + D.C_REPORT_ID + " = " + String.valueOf(reportId),
                null, null, null, null
        );

        try {
            cursor = database.rawQuery(query, null);

            for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
                MediaRecipient mediaRecipient = cursorToRecipient(cursor);
                recipients.put(mediaRecipient.getId(), mediaRecipient);
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
        report.setId(cursor.getInt(cursor.getColumnIndexOrThrow(D.C_ID)));
        report.setTitle(cursor.getString(cursor.getColumnIndexOrThrow(D.C_TITLE)));
        report.setContent(cursor.getString(cursor.getColumnIndexOrThrow(D.C_CONTENT)));
        report.setLocation(cursor.getString(cursor.getColumnIndexOrThrow(D.C_LOCATION)));
        report.setDate(new Date(cursor.getLong(cursor.getColumnIndexOrThrow(D.C_DATE))));
        report.setMetadataSelected(cursor.getLong(cursor.getColumnIndexOrThrow(D.C_METADATA)) == 1);
        report.setKeptInArchive(cursor.getLong(cursor.getColumnIndexOrThrow(D.C_ARCHIVED)) == 1);
        report.setKeptInDrafts(cursor.getLong(cursor.getColumnIndexOrThrow(D.C_DRAFT)) == 1);
        report.setReportPublic(cursor.getLong(cursor.getColumnIndexOrThrow(D.C_PUBLIC)) == 1);

        return report;
    }

    private void deleteReportEvidences(long reportId) {
        database.delete(D.T_EVIDENCE, D.C_REPORT_ID + " = ? ", new String[]{String.valueOf(reportId)});
    }

    public void deleteEvidenceByPath(long reportId, String filePath) {
        database.delete(D.T_EVIDENCE,
                D.C_REPORT_ID + " = ? AND " + D.C_PATH + " LIKE '%" + filePath + "%'",
                new String[]{String.valueOf(reportId)});
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
}
