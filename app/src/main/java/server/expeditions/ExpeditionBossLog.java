/*
    This file is part of the HeavenMS MapleStory Server
    Copyleft (L) 2016 - 2019 RonanLana

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
package server.expeditions;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import config.YamlConfig;
import tools.DatabaseConnection;
import tools.Pair;

import java.sql.Timestamp;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;

import static java.util.concurrent.TimeUnit.DAYS;
import static java.util.concurrent.TimeUnit.HOURS;

/**
 * @author Conrad
 * @author Ronan
 */
public class ExpeditionBossLog {

    public enum BossLogEntry {
        ZAKUM(2, 1, false),
        HORNTAIL(2, 1, false),
        PINKBEAN(1, 1, false),
        SCARGA(1, 1, false),
        PAPULATUS(2, 1, false);

        private final int entries;
        private final int timeLength;
        private final int minChannel;
        private final int maxChannel;
        private final boolean week;

        BossLogEntry(int entries, int timeLength, boolean week) {
            this(entries, 0, Integer.MAX_VALUE, timeLength, week);
        }

        BossLogEntry(int entries, int minChannel, int maxChannel, int timeLength, boolean week) {
            this.entries = entries;
            this.minChannel = minChannel;
            this.maxChannel = maxChannel;
            this.timeLength = timeLength;
            this.week = week;
        }

        private static List<Pair<Timestamp, BossLogEntry>> getBossLogResetTimestamps(Calendar timeNow, boolean week) {
            List<Pair<Timestamp, BossLogEntry>> resetTimestamps = new LinkedList<>();

            Timestamp ts = new Timestamp(timeNow.getTime().getTime());  // reset all table entries actually, thanks Conrad
            for (BossLogEntry b : BossLogEntry.values()) {
                if (b.week == week) {
                    resetTimestamps.add(new Pair<>(ts, b));
                }
            }

            return resetTimestamps;
        }

        private static BossLogEntry getBossEntryByName(String name) {
            for (BossLogEntry b : BossLogEntry.values()) {
                if (name.contentEquals(b.name())) {
                    return b;
                }
            }

            return null;
        }

    }

    public static void resetBossLogTable() {
        /*
        Boss logs resets 12am, weekly thursday 12AM - thanks Smitty Werbenjagermanjensen (superadlez) - https://www.reddit.com/r/Maplestory/comments/61tiup/about_reset_time/
        */
        Calendar thursday = Calendar.getInstance();
        thursday.set(Calendar.DAY_OF_WEEK, Calendar.THURSDAY);
        thursday.set(Calendar.HOUR, 0);
        thursday.set(Calendar.MINUTE, 0);
        thursday.set(Calendar.SECOND, 0);

        Calendar now = Calendar.getInstance();

        long weekLength = DAYS.toMillis(7);
        long halfDayLength = HOURS.toMillis(12);

        long deltaTime = now.getTime().getTime() - thursday.getTime().getTime();    // 2x time: get Date into millis
        deltaTime += halfDayLength;
        deltaTime %= weekLength;
        deltaTime -= halfDayLength;

        if (deltaTime < halfDayLength) {
            ExpeditionBossLog.resetBossLogTable(true, thursday);
        }

        now.set(Calendar.HOUR, 0);
        now.set(Calendar.MINUTE, 0);
        now.set(Calendar.SECOND, 0);

        ExpeditionBossLog.resetBossLogTable(false, now);
    }

    private static void resetBossLogTable(boolean week, Calendar c) {
        List<Pair<Timestamp, BossLogEntry>> resetTimestamps = BossLogEntry.getBossLogResetTimestamps(c, week);
        SQLiteDatabase con = DatabaseConnection.getConnection();
        try {
            for (Pair<Timestamp, BossLogEntry> p : resetTimestamps) {
                con.delete(getBossLogTable(week), "attempttime <= ? AND bosstype LIKE ?",
                        new String[]{String.valueOf(p.getLeft()), p.getRight().name()});
            }
        } catch (SQLiteException e) {
            e.printStackTrace();
        }
    }

    private static String getBossLogTable(boolean week) {
        return week ? "bosslog_weekly" : "bosslog_daily";
    }

    private static int countPlayerEntries(int cid, BossLogEntry boss) {
        int ret_count = 0;
        try (SQLiteDatabase con = DatabaseConnection.getConnection();
             Cursor ps = con.rawQuery("SELECT COUNT(*) FROM " + getBossLogTable(boss.week) + " WHERE characterid = ? AND bosstype LIKE ?",
                     new String[]{String.valueOf(cid), boss.name()})) {
            if (ps != null) {
                if (ps.moveToNext()) {
                    ret_count = ps.getInt(0);
                } else {
                    ret_count = -1;
                }
            }
            return ret_count;
        } catch (SQLiteException e) {
            e.printStackTrace();
            return -1;
        }
    }

    private static void insertPlayerEntry(int cid, BossLogEntry boss) {
        ContentValues values = new ContentValues();
        values.put("characterid", cid);
        values.put("bosstype", boss.name());

        try (SQLiteDatabase con = DatabaseConnection.getConnection()) {
            con.insert(getBossLogTable(boss.week), null, values);
        } catch (SQLiteException e) {
            e.printStackTrace();
        }
    }

    public static boolean attemptBoss(int cid, int channel, Expedition exped, boolean log) {
        if (!YamlConfig.config.server.USE_ENABLE_DAILY_EXPEDITIONS) {
            return true;
        }

        BossLogEntry boss = BossLogEntry.getBossEntryByName(exped.getType().name());
        if (boss == null) {
            return true;
        }

        if (channel < boss.minChannel || channel > boss.maxChannel) {
            return false;
        }

        if (countPlayerEntries(cid, boss) >= boss.entries) {
            return false;
        }

        if (log) {
            insertPlayerEntry(cid, boss);
        }
        return true;
    }
}
