/*
	This file is part of the OdinMS Maple Story Server
    Copyright (C) 2008 Patrick Huy <patrick.huy@frz.cc>
		       Matthias Butz <matze@odinms.de>
		       Jan Christian Meyer <vimes@odinms.de>

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License as
    published by the Free Software Foundation version 3 as published by
    the Free Software Foundation. You may not use, modify or distribute
    this program under any other version of the GNU Affero General Public
    License.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Affero General Public License for more details.

    You should have received a copy of the GNU Affero General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/
package net.server.task;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import client.Job;
import config.YamlConfig;
import net.server.Server;
import tools.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * @author Matze
 * @author Quit
 * @author Ronan
 */
public class RankingLoginTask implements Runnable {
    private long lastUpdate = System.currentTimeMillis();

    private void resetMoveRank(boolean job) throws SQLiteException {
        String query = "UPDATE characters SET " + (job ? "jobRankMove = 0" : "rankMove = 0");
        try (SQLiteDatabase con = DatabaseConnection.getConnection()) {
            con.rawQuery(query, null);
        }
    }

    private void updateRanking(int job, int world) throws SQLiteException {
        String sqlCharSelect = "SELECT c.id, " + (job != -1 ? "c.jobRank, c.jobRankMove" : "c.rank, c.rankMove") + ", a.lastlogin AS lastlogin, a.loggedin FROM characters AS c LEFT JOIN accounts AS a ON c.accountid = a.id WHERE c.gm < 2 AND c.world = ? ";
        if (job != -1) {
            sqlCharSelect += "AND c.job DIV 100 = ? ";
        }
        sqlCharSelect += "ORDER BY c.level DESC , c.exp DESC , c.lastExpGainTime ASC, c.fame DESC , c.meso DESC";

        try (SQLiteDatabase con = DatabaseConnection.getConnection()) {
            Cursor cursor;
            if (job != -1) {
                cursor = con.rawQuery(sqlCharSelect, new String[]{String.valueOf(world), String.valueOf(job)});
            } else {
                cursor = con.rawQuery(sqlCharSelect, new String[]{String.valueOf(world)});
            }
            int rank = 0;
            while (cursor.moveToNext()) {
                int rankMove = 0;
                rank++;

                int lastloginIdx = cursor.getColumnIndex("lastlogin");
                int loggedinIdx = cursor.getColumnIndex("loggedin");
                int jobRankMoveIdx = cursor.getColumnIndex("jobRankMove");
                int rankMoveIdx = cursor.getColumnIndex("rankMove");
                int jobRankIdx = cursor.getColumnIndex("jobRank");
                int rankIdx = cursor.getColumnIndex("rank");
                int idIdx= cursor.getColumnIndex("id");

                final long lastlogin = cursor.getLong(lastloginIdx);
                if (lastlogin < lastUpdate || cursor.getInt(loggedinIdx) > 0) {
                    rankMove = cursor.getInt(job != -1 ? jobRankMoveIdx: rankMoveIdx);
                }
                rankMove += cursor.getInt(job != -1 ? jobRankIdx : rankIdx) - rank;

                ContentValues values = new ContentValues();
                values.put(job != -1 ? "jobRank" : "rank", rank);
                values.put(job != -1 ? "jobRankMove" : "rankMove", rankMove);

                String whereClause = "id = ?";
                String[] whereArgs = {String.valueOf(cursor.getInt(idIdx))};
                con.update("characters", values, whereClause, whereArgs);
            }
            cursor.close();
        }
    }

    @Override
    public void run() {
        SQLiteDatabase con = DatabaseConnection.getConnection();
        try {
            con.beginTransaction();

            try {
                if (YamlConfig.config.server.USE_REFRESH_RANK_MOVE) {
                    resetMoveRank(true);
                    resetMoveRank(false);
                }

                for (int j = 0; j < Server.getInstance().getWorldsSize(); j++) {
                    updateRanking(-1, j);    //overall ranking
                    for (int i = 0; i <= Job.getMax(); i++) {
                        updateRanking(i, j);
                    }
                }

                con.setTransactionSuccessful();
                lastUpdate = System.currentTimeMillis();
            } catch (SQLiteException ex) {
                throw ex;
            }
        } catch (SQLiteException e) {
            e.printStackTrace();
        } finally {
            con.endTransaction();
            con.close();
        }
    }
}
