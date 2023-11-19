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
package net.server.channel.handlers;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import client.Character;
import client.Client;
import net.AbstractPacketHandler;
import net.packet.InPacket;
import net.server.guild.GuildPackets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.DatabaseConnection;

public final class BBSOperationHandler extends AbstractPacketHandler {
    private static final Logger log = LoggerFactory.getLogger(BBSOperationHandler.class);

    private String correctLength(String in, int maxSize) {
        return in.length() > maxSize ? in.substring(0, maxSize) : in;
    }

    @Override
    public void handlePacket(InPacket p, Client c) {
        if (c.getPlayer().getGuildId() < 1) {
            return;
        }
        byte mode = p.readByte();
        int localthreadid = 0;
        String title;
        String text;
        switch (mode) {
            case 0:
                boolean bEdit = p.readByte() == 1;
                if (bEdit) {
                    localthreadid = p.readInt();
                }
                boolean bNotice = p.readByte() == 1;
                title = correctLength(p.readString(), 25);
                text = correctLength(p.readString(), 600);
                int icon = p.readInt();
                if (icon >= 0x64 && icon <= 0x6a) {
                    if (!c.getPlayer().haveItemWithId(5290000 + icon - 0x64, false)) {
                        return;
                    }
                } else if (icon < 0 || icon > 3) {
                    return;
                }
                if (!bEdit) {
                    newBBSThread(c, title, text, icon, bNotice);
                } else {
                    editBBSThread(c, title, text, icon, localthreadid);
                }
                break;
            case 1:
                localthreadid = p.readInt();
                deleteBBSThread(c, localthreadid);
                break;
            case 2:
                int start = p.readInt();
                listBBSThreads(c, start * 10);
                break;
            case 3: // list thread + reply, following by id (int)
                localthreadid = p.readInt();
                displayThread(c, localthreadid);
                break;
            case 4: // reply
                localthreadid = p.readInt();
                text = correctLength(p.readString(), 25);
                newBBSReply(c, localthreadid, text);
                break;
            case 5: // delete reply
                p.readInt(); // we don't use this
                int replyid = p.readInt();
                deleteBBSReply(c, replyid);
                break;
            default:
                //System.out.println("Unhandled BBS mode: " + slea.toString());
        }
    }

    private static void listBBSThreads(Client c, int start) {
        SQLiteDatabase con = DatabaseConnection.getConnection();
        try (Cursor cursor = con.rawQuery("SELECT * FROM bbs_threads WHERE guildid = ? ORDER BY localthreadid DESC",
                     new String[]{String.valueOf(c.getPlayer().getGuildId())})) {
                c.sendPacket(GuildPackets.BBSThreadList(cursor, start));
        } catch (SQLiteException se) {
            log.error("listBBSThreads error", se);
        }
    }

    private static void newBBSReply(Client c, int localthreadid, String text) {
        if (c.getPlayer().getGuildId() <= 0) {
            return;
        }
        SQLiteDatabase con = DatabaseConnection.getConnection();
        try {
            int threadid = -1;
            try (Cursor cursor = con.rawQuery("SELECT threadid FROM bbs_threads WHERE guildid = ? AND localthreadid = ?",
                    new String[]{String.valueOf(c.getPlayer().getGuildId()), String.valueOf(localthreadid)})) {
                    if (!cursor.moveToNext()) {
                        return;
                    }
                    int threadidIdx = cursor.getColumnIndex("threadid");
                    if (threadidIdx != -1) {
                        threadid = cursor.getInt(threadidIdx);
                    }
            }
            ContentValues values = new ContentValues();
            values.put("threadid", threadid);
            values.put("postercid", c.getPlayer().getId());
            values.put("timestamp", currentServerTime());
            values.put("content", text);

            con.insert("bbs_replies", null, values);

            ContentValues values1 = new ContentValues();
            values1.put("replycount", "replycount + 1");
            con.update("bbs_threads", values, "threadid = ?", new String[]{String.valueOf(threadid)});
            displayThread(c, localthreadid);
        } catch (SQLiteException se) {
            log.error("newBBSReply error", se);
        }
    }

    private static void editBBSThread(Client client, String title, String text, int icon, int localthreadid) {
        Character chr = client.getPlayer();
        if (chr.getGuildId() < 1) {
            return;
        }
        ContentValues values = new ContentValues();
        values.put("name", title);
        values.put("timestamp", currentServerTime());
        values.put("icon", icon);
        values.put("startpost", text);

        String selection = "guildid = ? AND localthreadid = ? AND (postercid = ? OR ?)";
        String[] selectionArgs = new String[]{String.valueOf(chr.getGuildId()), String.valueOf(localthreadid), String.valueOf(chr.getId()), String.valueOf(chr.getGuildRank() < 3)};
        SQLiteDatabase con = DatabaseConnection.getConnection();
        try {
            con.update("bbs_threads", values, selection, selectionArgs);
            displayThread(client, localthreadid);
        } catch (SQLiteException se) {
            log.error("editBBSThread error", se);
        }
    }

    private static void newBBSThread(Client client, String title, String text, int icon, boolean bNotice) {
        Character chr = client.getPlayer();
        if (chr.getGuildId() <= 0) {
            return;
        }
        int nextId = 0;
        SQLiteDatabase con = DatabaseConnection.getConnection();
        try {
            if (!bNotice) {
                try (Cursor cursor = con.rawQuery("SELECT MAX(localthreadid) AS lastLocalId FROM bbs_threads WHERE guildid = ?", new String[]{String.valueOf(chr.getGuildId())})) {
                    if (cursor.moveToNext()) {
                        int lastLocalIdIdx = cursor.getColumnIndex("lastLocalId");
                        if (lastLocalIdIdx != -1) {
                            int lastLocalId = cursor.getInt(lastLocalIdIdx);
                            nextId = lastLocalId + 1;
                        }
                    }
                }
            }

            ContentValues values = new ContentValues();
            values.put("postercid", chr.getId());
            values.put("name", title);
            values.put("timestamp", currentServerTime());
            values.put("icon", icon);
            values.put("startpost", text);
            values.put("guildid", chr.getGuildId());
            values.put("localthreadid", nextId);

            con.insert("bbs_threads", null, values);

            displayThread(client, nextId);
        } catch (SQLiteException se) {
            log.error("newBBSThread error", se);
        }

    }

    public static void deleteBBSThread(Client client, int localthreadid) {
        Character mc = client.getPlayer();
        if (mc.getGuildId() <= 0) {
            return;
        }
        SQLiteDatabase con = DatabaseConnection.getConnection();
        try {
            final int threadid;
            try (Cursor cursor = con.rawQuery("SELECT threadid, postercid FROM bbs_threads WHERE guildid = ? AND localthreadid = ?",
                    new String[]{String.valueOf(mc.getGuildId()), String.valueOf(localthreadid)})) {
                    int threadidIdx = cursor.getColumnIndex("threadid");
                    int postercidIdx = cursor.getColumnIndex("postercid");

                    if (mc.getId() != cursor.getInt(postercidIdx) && mc.getGuildRank() > 2) {
                        return;
                    }
                    threadid = cursor.getInt(threadidIdx);
            }

            String whereClause = "threadid = ?";
            String[] whereArgs = {String.valueOf(threadid)};
            con.delete("bbs_replies", whereClause, whereArgs);

            String whereClause1 = "threadid = ?";
            String[] whereArgs1 = {String.valueOf(threadid)};
            con.delete("bbs_threads", whereClause1, whereArgs1);
        } catch (SQLiteException se) {
            log.error("deleteBBSThread error", se);
        }
    }

    public static void deleteBBSReply(Client client, int replyid) {
        Character mc = client.getPlayer();
        if (mc.getGuildId() <= 0) {
            return;
        }

        int threadid = -1;
        SQLiteDatabase con = DatabaseConnection.getConnection();
        try {
            try (Cursor cursor = con.rawQuery("SELECT postercid, threadid FROM bbs_replies WHERE replyid = ?",
                    new String[]{String.valueOf(replyid)})) {
                if (cursor.moveToNext()) {
                    int postercidIdx = cursor.getColumnIndex("postercid");
                    int threadidIdx = cursor.getColumnIndex("threadid");

                    if (mc.getId() != cursor.getInt(postercidIdx) && mc.getGuildRank() > 2) {
                        return;
                    }

                    threadid = cursor.getInt(threadidIdx);
                }
            }

            String whereClause = "replyid = ?";
            String[] whereArgs = {String.valueOf(replyid)};
            con.delete("bbs_replies", whereClause, whereArgs);

            ContentValues values = new ContentValues();
            values.put("replycount", "replycount - 1");

            String whereClause1 = "threadid = ?";
            String[] whereArgs1 = {String.valueOf(threadid)};

            con.update("bbs_threads", values, whereClause1, whereArgs1);


            displayThread(client, threadid, false);
        } catch (SQLiteException se) {
            log.error("deleteBBSReply error", se);
        }
    }

    public static void displayThread(Client client, int threadid) {
        displayThread(client, threadid, true);
    }

    public static void displayThread(Client client, int threadid, boolean bIsThreadIdLocal) {
        Character mc = client.getPlayer();
        if (mc.getGuildId() <= 0) {
            return;
        }
        SQLiteDatabase con = DatabaseConnection.getConnection();
        try {
            // TODO clean up this block and use try-with-resources
            try (Cursor threadCursor = con.rawQuery("SELECT * FROM bbs_threads WHERE guildid = ? AND " + (bIsThreadIdLocal ? "local" : "") + "threadid = ?",
                    new String[]{String.valueOf(mc.getGuildId()), String.valueOf(threadid)})) {
                if (threadCursor.moveToNext()) {
                    int replycountIdx = threadCursor.getColumnIndex("replycount");
                    int replycount = threadCursor.getInt(replycountIdx);
                    Cursor repliesCursor = null;
                    if (replycount >= 0) {
                        int threadidIdx = threadCursor.getColumnIndex("threadid");
                        int actualThreadId = !bIsThreadIdLocal ? threadid : threadCursor.getInt(threadidIdx);
                        repliesCursor = con.rawQuery("SELECT * FROM bbs_replies WHERE threadid = ?",
                                new String[]{String.valueOf(actualThreadId)});
                    }
                    int localthreadidIdx = threadCursor.getColumnIndex("localthreadid");
                    client.sendPacket(GuildPackets.showThread(bIsThreadIdLocal ? threadid : threadCursor.getInt(localthreadidIdx), threadCursor, repliesCursor));
                    if (repliesCursor != null) {
                        repliesCursor.close();
                    }
                }
            }
        } catch (SQLiteException se) {
            log.error("Error displaying thread", se);
        } catch (RuntimeException re) {//btw we get this everytime for some reason, but replies work!
            log.error("The number of reply rows does not match the replycount in thread.", re);
        }
    }
}
