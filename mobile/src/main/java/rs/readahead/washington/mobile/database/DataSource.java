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
import rs.readahead.washington.mobile.util.C;
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
            C.COLUMN_ID,
            C.COLUMN_REPORTS_TITLE,
            C.COLUMN_REPORTS_CONTENT,
            C.COLUMN_REPORTS_LOCATION,
            C.COLUMN_REPORTS_DATE,
            C.COLUMN_REPORTS_METADATA,
            C.COLUMN_REPORTS_ARCHIVED,
            C.COLUMN_REPORTS_DRAFTS,
            C.COLUMN_REPORTS_PUBLIC

    };

    /*private String[] allColumnsRecipients = {
            C.COLUMN_ID,
            C.COLUMN_RECIPIENT_TITLE,
            C.COLUMN_RECIPIENT_MAIL
    };*/

    public void deleteDatabase() {
        database.execSQL("DELETE FROM " + C.TABLE_RECIPIENTS);
        database.execSQL("DELETE FROM " + C.TABLE_TRUSTED_PERSONS);
        database.execSQL("DELETE FROM " + C.TABLE_REPORTS);
        database.execSQL("DELETE FROM " + C.TABLE_EVIDENCES);
        database.execSQL("DELETE FROM " + C.TABLE_LISTS);
        database.execSQL("DELETE FROM " + C.TABLE_REPORT_RECIPIENTS);
    }

    public void deleteContacts() {
        database.execSQL("DELETE FROM " + C.TABLE_TRUSTED_PERSONS);
    }

    public void deleteMedia() {
        database.execSQL("DELETE FROM " + C.TABLE_RECIPIENTS);
        database.execSQL("DELETE FROM " + C.TABLE_LISTS);
        database.execSQL("DELETE FROM " + C.TABLE_REPORT_RECIPIENTS);
    }

    public void insertRecipient(final MediaRecipient mediaRecipient) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(C.COLUMN_RECIPIENT_MAIL, mediaRecipient.getMail());
        contentValues.put(C.COLUMN_RECIPIENT_TITLE, mediaRecipient.getTitle());

        long id = database.insert(C.TABLE_RECIPIENTS, null, contentValues);

        mediaRecipient.setId(id);
    }

    public void updateRecipient(MediaRecipient mediaRecipient) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(C.COLUMN_RECIPIENT_MAIL, mediaRecipient.getMail());
        contentValues.put(C.COLUMN_RECIPIENT_TITLE, mediaRecipient.getTitle());

        database.update(C.TABLE_RECIPIENTS, contentValues, C.COLUMN_ID + " = ? ", new String[]{String.valueOf(mediaRecipient.getId())});
        database.delete(C.TABLE_REPORT_RECIPIENTS, C.COLUMN_RECIPIENT_ID + " = ? ", new String[]{String.valueOf(mediaRecipient.getId())});
    }

    public void deleteRecipient(long columnId) {
        database.delete(C.TABLE_RECIPIENTS, C.COLUMN_ID + " = ?", new String[]{String.valueOf(columnId)});
    }

    @NonNull
    public List<MediaRecipient> getMediaRecipients() {
        List<MediaRecipient> recipients = new ArrayList<>();
        Cursor cursor = null;

        try {
            cursor = database.query(C.TABLE_RECIPIENTS, null, null, null, null, null, null);

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
            wheres.add(C.COLUMN_ID + " IN (" + TextUtils.join(",", selectedRecipients) + ")");
        }

        if (! selectedRecipientLists.isEmpty()) {
            wheres.add(C.COLUMN_ID + " IN (SELECT " + C.COLUMN_RECIPIENT_ID +
                    " FROM " + C.TABLE_RECIPIENT_LISTS +
                    " WHERE " + C.COLUMN_LIST_ID +
                    " IN (" + TextUtils.join(",", selectedRecipientLists) + "))");
        }

        try {
            cursor = database.query(
                    C.TABLE_RECIPIENTS, null,
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

        mediaRecipient.setId(cursor.getInt(cursor.getColumnIndexOrThrow(C.COLUMN_ID)));
        mediaRecipient.setTitle(cursor.getString(cursor.getColumnIndexOrThrow(C.COLUMN_RECIPIENT_TITLE)));
        mediaRecipient.setMail(cursor.getString(cursor.getColumnIndexOrThrow(C.COLUMN_RECIPIENT_MAIL)));

        return mediaRecipient;
    }

    private MediaRecipientList cursorToMediaRecipientList(Cursor cursor) {
        MediaRecipientList mediaRecipientList = new MediaRecipientList();

        mediaRecipientList.setId(cursor.getInt(cursor.getColumnIndexOrThrow(C.COLUMN_ID)));
        mediaRecipientList.setTitle(cursor.getString(cursor.getColumnIndexOrThrow(C.COLUMN_LIST_TITLE)));

        return mediaRecipientList;
    }

    public void insertMediaRecipientList(MediaRecipientList mediaRecipientList) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(C.COLUMN_LIST_TITLE, mediaRecipientList.getTitle());

        long id = database.insert(C.TABLE_LISTS, null, contentValues);

        mediaRecipientList.setId((int) id);
    }

    private void updateList(String name, int listId) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(C.COLUMN_LIST_TITLE, name);

        database.update(C.TABLE_LISTS, contentValues, C.COLUMN_ID + " = ? ", new String[] {Integer.toString(listId)});
    }

    private void deleteMediaRecipientListRecipients(int id) {
        database.delete(C.TABLE_RECIPIENT_LISTS, C.COLUMN_LIST_ID + " = ?", new String[] {Integer.toString(id)});
    }

    private void insertMediaRecipientListRecipient(long recipientId, int listId) { // todo: rotate args
        ContentValues contentValues = new ContentValues();
        contentValues.put(C.COLUMN_LIST_ID, listId);
        contentValues.put(C.COLUMN_RECIPIENT_ID, recipientId);

        database.insert(C.TABLE_RECIPIENT_LISTS, null, contentValues);
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
        database.delete(C.TABLE_LISTS, C.COLUMN_ID + " = ?", new String[]{String.valueOf(listId)});
    }

    private void deleteReportRecipients(Report report) {
        database.delete(C.TABLE_REPORT_RECIPIENTS, C.COLUMN_REPORT_ID + " = ?", new String[]{String.valueOf(report.getId())});
    }

    private void insertReportRecipients(Report report) {
        final String sql = createSQLInsert(C.TABLE_REPORT_RECIPIENTS, new String[]{
                C.COLUMN_RECIPIENT_ID,
                C.COLUMN_REPORT_ID
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
            cursor = database.query(C.TABLE_LISTS, null, null, null, null, null, null);

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
            cursor = database.query(C.TABLE_LISTS, null,
                    C.COLUMN_ID + " IN (SELECT DISTINCT " + C.COLUMN_LIST_ID + " FROM " + C.TABLE_RECIPIENT_LISTS + ")",
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
                    C.TABLE_RECIPIENT_LISTS,
                    new String[] {C.COLUMN_RECIPIENT_ID},
                    C.COLUMN_LIST_ID + " = ?", new String[] {Long.toString(listId)},
                    null, null, null);

            for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
                ids.add(cursor.getInt(cursor.getColumnIndexOrThrow(C.COLUMN_RECIPIENT_ID)));
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
        contentValues.put(C.COLUMN_TRUSTED_MAIL, trustedPerson.getMail());
        contentValues.put(C.COLUMN_TRUSTED_NAME, trustedPerson.getName());
        contentValues.put(C.COLUMN_TRUSTED_PHONE, trustedPerson.getPhoneNumber());

        database.insert(C.TABLE_TRUSTED_PERSONS, null, contentValues);
    }

    public void updateTrusted(TrustedPerson trustedPerson) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(C.COLUMN_TRUSTED_MAIL, trustedPerson.getMail());
        contentValues.put(C.COLUMN_TRUSTED_NAME, trustedPerson.getName());
        contentValues.put(C.COLUMN_TRUSTED_PHONE, trustedPerson.getPhoneNumber());

        database.update(C.TABLE_TRUSTED_PERSONS, contentValues, C.COLUMN_ID + " = ? ", new String[]{String.valueOf(trustedPerson.getColumnId())});
    }

    public void deleteTrusted(int columnId) {
        database.delete(C.TABLE_TRUSTED_PERSONS, C.COLUMN_ID + " = ?", new String[]{String.valueOf(columnId)});
    }

    public List<TrustedPerson> getAllTrusted() {
        List<TrustedPerson> trustedPersons = new ArrayList<>();

        Cursor cursor = database.query(C.TABLE_TRUSTED_PERSONS, null, null, null, null, null, null);
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

        Cursor cursor = database.query(C.TABLE_TRUSTED_PERSONS, new String[]{C.COLUMN_TRUSTED_PHONE},
                C.COLUMN_TRUSTED_PHONE + " IS NOT NULL OR " + C.COLUMN_TRUSTED_PHONE + " != ''", null, null, null, null);
        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            trustedPersonsPhones.add(cursor.getString(cursor.getColumnIndexOrThrow(C.COLUMN_TRUSTED_PHONE)));
            cursor.moveToNext();
        }
        cursor.close();

        return trustedPersonsPhones;
    }

    private TrustedPerson cursorToTrusted(Cursor cursor) {
        TrustedPerson trustedPerson = new TrustedPerson();

        trustedPerson.setColumnId(cursor.getInt(cursor.getColumnIndexOrThrow(C.COLUMN_ID)));
        trustedPerson.setName(cursor.getString(cursor.getColumnIndexOrThrow(C.COLUMN_TRUSTED_NAME)));
        trustedPerson.setMail(cursor.getString(cursor.getColumnIndexOrThrow(C.COLUMN_TRUSTED_MAIL)));
        trustedPerson.setPhoneNumber(cursor.getString(cursor.getColumnIndexOrThrow(C.COLUMN_TRUSTED_PHONE)));

        return trustedPerson;
    }

    public void insertReport(Report report) {
        ContentValues contentValues = new ContentValues();

        contentValues.put(C.COLUMN_REPORTS_TITLE, StringUtils.orDefault(report.getTitle(), ""));
        contentValues.put(C.COLUMN_REPORTS_CONTENT, StringUtils.orDefault(report.getContent(), ""));
        contentValues.put(C.COLUMN_REPORTS_LOCATION, StringUtils.orDefault(report.getLocation(), ""));
        contentValues.put(C.COLUMN_REPORTS_DATE, report.getDate().getTime());
        contentValues.put(C.COLUMN_REPORTS_METADATA, report.isMetadataSelected() ? 1 : 0);
        contentValues.put(C.COLUMN_REPORTS_ARCHIVED, report.isKeptInArchive() ? 1 : 0);
        contentValues.put(C.COLUMN_REPORTS_DRAFTS, report.isKeptInDrafts() ? 1 : 0);
        contentValues.put(C.COLUMN_REPORTS_PUBLIC, report.isReportPublic() ? 1 : 0);

        try {
            database.beginTransaction();

            long reportId = database.insert(C.TABLE_REPORTS, null, contentValues);

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
        final String sql = createSQLInsert(C.TABLE_EVIDENCES, new String[]{
                C.COLUMN_EVIDENCES_REPORT_ID,
                C.COLUMN_EVIDENCES_NAME,
                C.COLUMN_EVIDENCES_PATH,
                C.COLUMN_EVIDENCE_METADATA
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
        database.delete(C.TABLE_REPORTS, C.COLUMN_ID + " = ? ", new String[]{String.valueOf(report.getId())});

        if (report.getEvidences().size() > 0) {
            deleteReportEvidences(report.getId());
        }
    }

    public void updateReport(Report report) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(C.COLUMN_REPORTS_TITLE, StringUtils.orDefault(report.getTitle(), ""));
        contentValues.put(C.COLUMN_REPORTS_CONTENT, StringUtils.orDefault(report.getContent(), ""));
        contentValues.put(C.COLUMN_REPORTS_LOCATION, StringUtils.orDefault(report.getLocation(), ""));
        contentValues.put(C.COLUMN_REPORTS_DATE, report.getDate().getTime());
        contentValues.put(C.COLUMN_REPORTS_METADATA, report.isMetadataSelected() ? 1 : 0);
        contentValues.put(C.COLUMN_REPORTS_ARCHIVED, report.isKeptInArchive() ? 1 : 0);
        contentValues.put(C.COLUMN_REPORTS_DRAFTS, report.isKeptInDrafts() ? 1 : 0);
        contentValues.put(C.COLUMN_REPORTS_PUBLIC, report.isReportPublic() ? 1 : 0);

        try {
            database.beginTransaction();

            database.update(C.TABLE_REPORTS, contentValues, C.COLUMN_ID + " = ? ", new String[]{String.valueOf(report.getId())});

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

        Cursor cursor = database.query(C.TABLE_REPORTS, allColumnsReports, C.COLUMN_REPORTS_DRAFTS + " = 1", null, null, null, null);
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

        Cursor cursor = database.query(C.TABLE_REPORTS, allColumnsReports, C.COLUMN_REPORTS_ARCHIVED + " = 1", null, null, null, null);
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
                C.TABLE_EVIDENCES,
                null,
                C.COLUMN_EVIDENCES_REPORT_ID + " = " + reportId,
                null, null, null, null);

        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            Evidence evidence = new Evidence();
            evidence.setPath(cursor.getString(cursor.getColumnIndexOrThrow(C.COLUMN_EVIDENCES_PATH)));
            evidence.setUid(cursor.getString(cursor.getColumnIndexOrThrow(C.COLUMN_EVIDENCES_NAME)));
            evidence.setMetadata( new Gson().fromJson(cursor.getString(cursor.getColumnIndexOrThrow(C.COLUMN_EVIDENCE_METADATA)), Metadata.class));
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
                C.TABLE_RECIPIENTS + " JOIN " + C.TABLE_REPORT_RECIPIENTS + " AS RR ON " +
                        C.TABLE_RECIPIENTS + "." + C.COLUMN_ID + " = RR." + C.COLUMN_RECIPIENT_ID,
                new String[] {C.COLUMN_ID, C.COLUMN_RECIPIENT_TITLE, C.COLUMN_RECIPIENT_MAIL},
                "RR." + C.COLUMN_REPORT_ID + " = " + String.valueOf(reportId),
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
                    C.TABLE_LISTS,
                    new String[] {C.COLUMN_ID, C.COLUMN_LIST_TITLE},
                    C.COLUMN_ID + "= ?", new String[] {Long.toString(id)},
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
        report.setId(cursor.getInt(cursor.getColumnIndexOrThrow(C.COLUMN_ID)));
        report.setTitle(cursor.getString(cursor.getColumnIndexOrThrow(C.COLUMN_REPORTS_TITLE)));
        report.setContent(cursor.getString(cursor.getColumnIndexOrThrow(C.COLUMN_REPORTS_CONTENT)));
        report.setLocation(cursor.getString(cursor.getColumnIndexOrThrow(C.COLUMN_REPORTS_LOCATION)));
        report.setDate(new Date(cursor.getLong(cursor.getColumnIndexOrThrow(C.COLUMN_REPORTS_DATE))));
        report.setMetadataSelected(cursor.getLong(cursor.getColumnIndexOrThrow(C.COLUMN_REPORTS_METADATA)) == 1);
        report.setKeptInArchive(cursor.getLong(cursor.getColumnIndexOrThrow(C.COLUMN_REPORTS_ARCHIVED)) == 1);
        report.setKeptInDrafts(cursor.getLong(cursor.getColumnIndexOrThrow(C.COLUMN_REPORTS_DRAFTS)) == 1);
        report.setReportPublic(cursor.getLong(cursor.getColumnIndexOrThrow(C.COLUMN_REPORTS_PUBLIC)) == 1);

        return report;
    }

    private void deleteReportEvidences(long reportId) {
        database.delete(C.TABLE_EVIDENCES, C.COLUMN_EVIDENCES_REPORT_ID + " = ? ", new String[]{String.valueOf(reportId)});
    }

    public void deleteEvidenceByPath(long reportId, String filePath) {
        database.delete(C.TABLE_EVIDENCES,
                C.COLUMN_EVIDENCES_REPORT_ID + " = ? AND " + C.COLUMN_EVIDENCES_PATH + " LIKE '%" + filePath + "%'",
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
