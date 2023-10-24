package net.server.coordinator.session;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.DatabaseConnection;

import java.sql.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class SessionDAO {
    private static final Logger log = LoggerFactory.getLogger(SessionDAO.class);

    public static void deleteExpiredHwidAccounts() {
        final String query = "DELETE FROM hwidaccounts WHERE expiresat < CURRENT_TIMESTAMP";
        try (SQLiteDatabase con = DatabaseConnection.getConnection()) {
             con.rawQuery(query, null);
        } catch (SQLiteException e) {
            log.warn("Failed to delete expired hwidaccounts", e);
        }
    }

    public static List<Hwid> getHwidsForAccount(SQLiteDatabase con, int accountId) throws SQLiteException {
        final List<Hwid> hwids = new ArrayList<>();

        final String query = "SELECT hwid FROM hwidaccounts WHERE accountid = ?";
        try (Cursor ps = con.rawQuery(query, new String[] { String.valueOf(accountId) })) {
            while (ps.moveToNext()) {
                int hwidIdx = ps.getColumnIndex("hwid");
                if (hwidIdx != -1) {
                    hwids.add(new Hwid(ps.getString(hwidIdx)));
                }
            }
        }

        return hwids;
    }

    public static void registerAccountAccess(SQLiteDatabase con, int accountId, Hwid hwid, Instant expiry)
            throws SQLiteException {
        if (hwid == null) {
            throw new IllegalArgumentException("Hwid must not be null");
        }

        final String query = "INSERT INTO hwidaccounts (accountid, hwid, expiresat) VALUES (?, ?, ?)";
        String tableName = "hwidaccounts";
        ContentValues values = new ContentValues();
        values.put("accountid", accountId);
        values.put("hwid", hwid.hwid());
        values.put("expiresat", expiry.toString());
        con.insert(tableName, null, values);
    }

    public static List<HwidRelevance> getHwidRelevance(SQLiteDatabase con, int accountId) throws SQLiteException {
        final List<HwidRelevance> hwidRelevances = new ArrayList<>();

        String tableName = "hwidaccounts";
        String[] columns = { "hwid", "relevance" };
        String selection = "accountid = ?";
        String[] selectionArgs = { String.valueOf(accountId) };
        try (Cursor cursor = con.query(tableName, columns, selection, selectionArgs, null, null, null)) {
            while (cursor.moveToNext()) {
                int hwidIdx = cursor.getColumnIndex("hwid");
                int relevanceIdx = cursor.getColumnIndex("relevance");
                if (hwidIdx != -1 && relevanceIdx != -1) {
                    String hwid = cursor.getString(hwidIdx);
                    int relevance = cursor.getInt(relevanceIdx);
                    hwidRelevances.add(new HwidRelevance(hwid, relevance));
                }
            }
        }

        return hwidRelevances;
    }

    public static void updateAccountAccess(SQLiteDatabase con, Hwid hwid, int accountId, Instant expiry, int loginRelevance)
            throws SQLiteException {
        String tableName = "hwidaccounts";
        ContentValues values = new ContentValues();
        values.put("relevance", loginRelevance);
        values.put("expiresat", Timestamp.from(expiry).getTime());
        String whereClause = "accountid = ? AND hwid LIKE ?";
        String[] whereArgs = { String.valueOf(accountId), hwid.hwid() };
        con.update(tableName, values, whereClause, whereArgs);
    }
}
