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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.DatabaseConnection;

import java.sql.Timestamp;

/**
 * @author Matze
 * @author Quit
 * @author Ronan
 */
public class RankingLoginTask implements Runnable {
    private long lastUpdate = System.currentTimeMillis();
    private static final Logger log = LoggerFactory.getLogger(RankingLoginTask.class);

    private void resetMoveRank(boolean job) throws SQLiteException {
        ContentValues values = new ContentValues();
        if (job) {
            values.put("jobRankMove", 0);
        } else {
            values.put("rankMove", 0);
        }
        SQLiteDatabase con = DatabaseConnection.getConnection();
        con.update("characters", values, null, null);
    }

    private void updateRanking(int job, int world) throws SQLiteException {
        String sqlCharSelect = "SELECT c.id, " + (job != -1 ? "c.jobRank, c.jobRankMove" : "c.rank, c.rankMove") + ", a.lastlogin AS lastlogin, a.loggedin FROM characters AS c LEFT JOIN accounts AS a ON c.accountid = a.id WHERE c.gm < 2 AND c.world = ? ";
        if (job != -1) {
            sqlCharSelect += "AND c.job/100 = ? ";
        }
        sqlCharSelect += "ORDER BY c.level DESC , c.exp DESC , c.lastExpGainTime ASC, c.fame DESC , c.meso DESC";
        SQLiteDatabase con = DatabaseConnection.getConnection();
        try (Cursor cursor = (job == -1)? con.rawQuery(sqlCharSelect, new String[]{String.valueOf(world)}) :
                     con.rawQuery(sqlCharSelect, new String[]{String.valueOf(world), String.valueOf(job)})) {
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

                final String lastlogin = cursor.getString(lastloginIdx);
                if (Timestamp.valueOf(lastlogin).getTime() < lastUpdate || cursor.getInt(loggedinIdx) > 0) {
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
        }
    }

    @Override
    public void run() {
        SQLiteDatabase con = DatabaseConnection.getConnection();
        try {
            con.beginTransaction();
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
        } catch (SQLiteException e) {
            log.error("Run RankingLoginTask error", e);
        } finally {
            con.endTransaction();
        }
    }
}
