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
package client.newyear;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import client.Character;
import net.server.Server;
import server.TimerManager;
import tools.DatabaseConnection;
import tools.PacketCreator;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ScheduledFuture;

import static java.util.concurrent.TimeUnit.HOURS;

/**
 * @author Ronan - credits to Eric for showing the New Year opcodes and handler layout
 */
public class NewYearCardRecord {
    private int id;

    private final int senderId;
    private final String senderName;
    private boolean senderDiscardCard;

    private final int receiverId;
    private final String receiverName;
    private boolean receiverDiscardCard;
    private boolean receiverReceivedCard;

    private final String stringContent;
    private long dateSent = 0;
    private long dateReceived = 0;

    private ScheduledFuture<?> sendTask = null;

    public NewYearCardRecord(int senderid, String sender, int receiverid, String receiver, String message) {
        this.id = -1;

        this.senderId = senderid;
        this.senderName = sender;
        this.senderDiscardCard = false;

        this.receiverId = receiverid;
        this.receiverName = receiver;
        this.receiverDiscardCard = false;
        this.receiverReceivedCard = false;

        this.stringContent = message;

        this.dateSent = System.currentTimeMillis();
        this.dateReceived = 0;
    }

    private void setExtraNewYearCardRecord(int id, boolean senderDiscardCard, boolean receiverDiscardCard, boolean receiverReceivedCard, long dateSent, long dateReceived) {
        this.id = id;
        this.senderDiscardCard = senderDiscardCard;
        this.receiverDiscardCard = receiverDiscardCard;
        this.receiverReceivedCard = receiverReceivedCard;

        this.dateSent = dateSent;
        this.dateReceived = dateReceived;
    }

    public void setId(int cardid) {
        this.id = cardid;
    }

    public int getId() {
        return this.id;
    }

    public int getSenderId() {
        return senderId;
    }

    public String getSenderName() {
        return senderName;
    }

    public boolean isSenderCardDiscarded() {
        return senderDiscardCard;
    }

    public int getReceiverId() {
        return receiverId;
    }

    public String getReceiverName() {
        return receiverName;
    }

    public boolean isReceiverCardDiscarded() {
        return receiverDiscardCard;
    }

    public boolean isReceiverCardReceived() {
        return receiverReceivedCard;
    }

    public String getMessage() {
        return stringContent;
    }

    public long getDateSent() {
        return dateSent;
    }

    public long getDateReceived() {
        return dateReceived;
    }

    public static void saveNewYearCard(NewYearCardRecord newyear) {
        try (SQLiteDatabase con = DatabaseConnection.getConnection()) {
                ContentValues values = new ContentValues();
                values.put("senderId", newyear.senderId);
                values.put("senderName", newyear.senderName);
                values.put("receiverId", newyear.receiverId);
                values.put("receiverName", newyear.receiverName);
                values.put("stringContent", newyear.stringContent);
                values.put("senderDiscardCard", newyear.senderDiscardCard ? 1 : 0);
                values.put("receiverDiscardCard", newyear.receiverDiscardCard ? 1 : 0);
                values.put("receiverReceivedCard", newyear.receiverReceivedCard ? 1 : 0);
                values.put("dateSent", newyear.dateSent);
                values.put("dateReceived", newyear.dateReceived);

                long newRowId = con.insert("newyear", null, values);
                newyear.id = (int) newRowId;
        } catch (SQLiteException sqle) {
            sqle.printStackTrace();
        }
    }

    public static void updateNewYearCard(NewYearCardRecord newyear) {
        newyear.receiverReceivedCard = true;
        newyear.dateReceived = System.currentTimeMillis();

        ContentValues values = new ContentValues();
        values.put("received", 1);
        values.put("timereceived", newyear.dateReceived);

        String whereClause = "id = ?";
        String[] whereArgs = {String.valueOf(newyear.id)};

        try (SQLiteDatabase con = DatabaseConnection.getConnection()) {
            con.update("newyear", values, whereClause, whereArgs);
        } catch (SQLiteException sqle) {
            sqle.printStackTrace();
        }
    }

    public static NewYearCardRecord loadNewYearCard(int cardid) {
        NewYearCardRecord nyc = Server.getInstance().getNewYearCard(cardid);
        if (nyc != null) {
            return nyc;
        }

        try (SQLiteDatabase con = DatabaseConnection.getConnection()) {
            String[] projection = {
                    "senderid",
                    "sendername",
                    "receiverid",
                    "receivername",
                    "message",
                    "senderdiscard",
                    "receiverdiscard",
                    "received",
                    "timesent",
                    "timereceived"
            };

            String selection = "id = ?";
            String[] selectionArgs = {String.valueOf(cardid)};

            try (Cursor cursor = con.query("newyear", projection, selection, selectionArgs, null, null, null)) {
                if (cursor.moveToFirst()) {
                    int senderidIdx = cursor.getColumnIndex("senderid");
                    int sendernameIdx = cursor.getColumnIndex("sendername");
                    int receiveridIdx = cursor.getColumnIndex("receiverid");
                    int receivernameIdx = cursor.getColumnIndex("receivername");
                    int messageIdx = cursor.getColumnIndex("message");
                    int idIdx = cursor.getColumnIndex("id");
                    int senderdiscardIdx = cursor.getColumnIndex("senderdiscard");
                    int receiverdiscardIdx = cursor.getColumnIndex("receiverdiscard");
                    int receivedIdx = cursor.getColumnIndex("received");
                    int timesentIdx = cursor.getColumnIndex("timesent");
                    int timereceivedIdx = cursor.getColumnIndex("timereceived");

                    if (senderidIdx != -1 &&
                            sendernameIdx != -1 &&
                            receiveridIdx != -1 &&
                            receivernameIdx != -1 &&
                            messageIdx != -1 &&
                            idIdx != -1 &&
                            senderdiscardIdx != -1 &&
                            receiverdiscardIdx != -1 &&
                            receivedIdx != -1 &&
                            timesentIdx != -1 &&
                            timereceivedIdx != -1) {
                        int senderid = cursor.getInt(senderidIdx);
                        String sendername = cursor.getString(sendernameIdx);
                        int receiverid = cursor.getInt(receiveridIdx);
                        String receivername = cursor.getString(receivernameIdx);
                        String message = cursor.getString(messageIdx);

                        int id = cursor.getInt(idIdx);
                        boolean senderdiscard = cursor.getInt(senderdiscardIdx) != 0;
                        boolean receiverdiscard = cursor.getInt(receiverdiscardIdx) != 0;
                        boolean received = cursor.getInt(receivedIdx) != 0;
                        long timesent = cursor.getLong(timesentIdx);
                        long timereceived = cursor.getLong(timereceivedIdx);

                        NewYearCardRecord newyear = new NewYearCardRecord(senderid, sendername, receiverid, receivername, message);
                        newyear.setExtraNewYearCardRecord(id, senderdiscard, receiverdiscard, received, timesent, timereceived);

                        Server.getInstance().setNewYearCard(newyear);
                        return newyear;
                    }
                }
            }
        } catch (SQLiteException sqle) {
            sqle.printStackTrace();
        }

        return null;
    }

    public static void loadPlayerNewYearCards(Character chr) {
        SQLiteDatabase con = DatabaseConnection.getConnection();
        try {
            String[] projection = {
                    "senderid",
                    "sendername",
                    "receiverid",
                    "receivername",
                    "message",
                    "id",
                    "senderdiscard",
                    "receiverdiscard",
                    "received",
                    "timesent",
                    "timereceived"
            };

            String selection = "senderid = ? OR receiverid = ?";
            String[] selectionArgs = {String.valueOf(chr.getId()), String.valueOf(chr.getId())};

            try (Cursor cursor = con.query("newyear", projection, selection, selectionArgs, null, null, null)) {
                while (cursor.moveToNext()) {
                    int senderid = cursor.getInt(cursor.getColumnIndex("senderid"));
                    String sendername = cursor.getString(cursor.getColumnIndex("sendername"));
                    int receiverid = cursor.getInt(cursor.getColumnIndex("receiverid"));
                    String receivername = cursor.getString(cursor.getColumnIndex("receivername"));
                    String message = cursor.getString(cursor.getColumnIndex("message"));

                    int id = cursor.getInt(cursor.getColumnIndex("id"));
                    boolean senderdiscard = cursor.getInt(cursor.getColumnIndex("senderdiscard")) != 0;
                    boolean receiverdiscard = cursor.getInt(cursor.getColumnIndex("receiverdiscard")) != 0;
                    boolean received = cursor.getInt(cursor.getColumnIndex("received")) != 0;
                    long timesent = cursor.getLong(cursor.getColumnIndex("timesent"));
                    long timereceived = cursor.getLong(cursor.getColumnIndex("timereceived"));

                    NewYearCardRecord newyear = new NewYearCardRecord(senderid, sendername, receiverid, receivername, message);
                    newyear.setExtraNewYearCardRecord(id, senderdiscard, receiverdiscard, received, timesent, timereceived);

                    chr.addNewYearRecord(newyear);
                }
            }
        } catch (SQLiteException sqle) {
            sqle.printStackTrace();
        }
    }

    public static void printNewYearRecords(Character chr) {
        chr.dropMessage(5, "New Years: " + chr.getNewYearRecords().size());

        for (NewYearCardRecord nyc : chr.getNewYearRecords()) {
            chr.dropMessage(5, "-------------------------------");

            chr.dropMessage(5, "Id: " + nyc.id);

            chr.dropMessage(5, "Sender id: " + nyc.senderId);
            chr.dropMessage(5, "Sender name: " + nyc.senderName);
            chr.dropMessage(5, "Sender discard: " + nyc.senderDiscardCard);

            chr.dropMessage(5, "Receiver id: " + nyc.receiverId);
            chr.dropMessage(5, "Receiver name: " + nyc.receiverName);
            chr.dropMessage(5, "Receiver discard: " + nyc.receiverDiscardCard);
            chr.dropMessage(5, "Received: " + nyc.receiverReceivedCard);

            chr.dropMessage(5, "Message: " + nyc.stringContent);
            chr.dropMessage(5, "Date sent: " + nyc.dateSent);
            chr.dropMessage(5, "Date recv: " + nyc.dateReceived);
        }
    }

    public void startNewYearCardTask() {
        if (sendTask != null) {
            return;
        }

        sendTask = TimerManager.getInstance().register(() -> {
            Server server = Server.getInstance();

            int world = server.getCharacterWorld(receiverId);
            if (world == -1) {
                sendTask.cancel(false);
                sendTask = null;

                return;
            }

            Character target = server.getWorld(world).getPlayerStorage().getCharacterById(receiverId);
            if (target != null && target.isLoggedinWorld()) {
                target.sendPacket(PacketCreator.onNewYearCardRes(target, NewYearCardRecord.this, 0xC, 0));
            }
        }, HOURS.toMillis(1));
    }

    public void stopNewYearCardTask() {
        if (sendTask != null) {
            sendTask.cancel(false);
            sendTask = null;
        }
    }

    private static void deleteNewYearCard(int id) {
        Server.getInstance().removeNewYearCard(id);

        try (SQLiteDatabase con = DatabaseConnection.getConnection()) {
            con.delete("newyear", "id=?", new String[]{String.valueOf(id)});
        } catch (SQLiteException sqle) {
            sqle.printStackTrace();
        }
    }

    public static void removeAllNewYearCard(boolean send, Character chr) {
        int cid = chr.getId();
        
        /* not truly needed since it's going to be hard removed from the DB
        String actor = (send ? "sender" : "receiver");
        
        try (SQLiteDatabase con = DatabaseConnection.getConnection()) {
            try (PreparedStatement ps = con.prepareStatement("UPDATE newyear SET " + actor + "id = 1, received = 0 WHERE " + actor + "id = ?")) {
                ps.setInt(1, cid);
                ps.executeUpdate();
            }
        } catch(SQLiteException sqle) {
            sqle.printStackTrace();
        }
        */

        Set<NewYearCardRecord> set = new HashSet<>(chr.getNewYearRecords());
        for (NewYearCardRecord nyc : set) {
            if (send) {
                if (nyc.senderId == cid) {
                    nyc.senderDiscardCard = true;
                    nyc.receiverReceivedCard = false;

                    chr.removeNewYearRecord(nyc);
                    deleteNewYearCard(nyc.id);

                    chr.getMap().broadcastMessage(PacketCreator.onNewYearCardRes(chr, nyc, 0xE, 0));

                    Character other = chr.getClient().getWorldServer().getPlayerStorage().getCharacterById(nyc.getReceiverId());
                    if (other != null && other.isLoggedinWorld()) {
                        other.removeNewYearRecord(nyc);
                        other.getMap().broadcastMessage(PacketCreator.onNewYearCardRes(other, nyc, 0xE, 0));

                        other.dropMessage(6, "[New Year] " + chr.getName() + " threw away the New Year card.");
                    }
                }
            } else {
                if (nyc.receiverId == cid) {
                    nyc.receiverDiscardCard = true;
                    nyc.receiverReceivedCard = false;

                    chr.removeNewYearRecord(nyc);
                    deleteNewYearCard(nyc.id);

                    chr.getMap().broadcastMessage(PacketCreator.onNewYearCardRes(chr, nyc, 0xE, 0));

                    Character other = chr.getClient().getWorldServer().getPlayerStorage().getCharacterById(nyc.getSenderId());
                    if (other != null && other.isLoggedinWorld()) {
                        other.removeNewYearRecord(nyc);
                        other.getMap().broadcastMessage(PacketCreator.onNewYearCardRes(other, nyc, 0xE, 0));

                        other.dropMessage(6, "[New Year] " + chr.getName() + " threw away the New Year card.");
                    }
                }
            }
        }
    }

    public static void startPendingNewYearCardRequests(SQLiteDatabase con) throws SQLiteException {
        try (Cursor cursor = con.rawQuery("SELECT * FROM newyear WHERE timereceived = 0 AND senderdiscard = 0", null)) {
            while (cursor != null && cursor.moveToNext()) {
                int senderIdIdx = cursor.getColumnIndex("senderid");
                int senderNameIdx = cursor.getColumnIndex("sendername");
                int receiverIdIdx = cursor.getColumnIndex("receiverid");
                int receiverNameIdx = cursor.getColumnIndex("receivername");
                int messageIdx = cursor.getColumnIndex("message");
                int idIdx = cursor.getColumnIndex("id");
                int senderDiscardIdx = cursor.getColumnIndex("senderdiscard");
                int receiverDiscardIdx = cursor.getColumnIndex("receiverdiscard");
                int receivedIdx = cursor.getColumnIndex("received");
                int timeSentIdx = cursor.getColumnIndex("timesent");
                int timeReceivedIdx = cursor.getColumnIndex("timereceived");

                if (senderIdIdx != -1 &&
                        senderNameIdx != -1 &&
                        receiverIdIdx != -1 &&
                        receiverNameIdx != -1 &&
                        messageIdx != -1 &&
                        idIdx != -1 &&
                        senderDiscardIdx != -1 &&
                        receiverDiscardIdx != -1 &&
                        receivedIdx != -1 &&
                        timeSentIdx != -1 &&
                        timeReceivedIdx != -1
                ) {

                    NewYearCardRecord newyear = new NewYearCardRecord(cursor.getInt(senderIdIdx), cursor.getString(senderNameIdx), cursor.getInt(receiverIdIdx), cursor.getString(receiverNameIdx), cursor.getString(messageIdx));
                    newyear.setExtraNewYearCardRecord(cursor.getInt(idIdx), cursor.getInt(senderDiscardIdx) == 1, cursor.getInt(receiverDiscardIdx) == 1, cursor.getInt(receivedIdx) == 1, cursor.getLong(timeSentIdx), cursor.getLong(timeReceivedIdx));

                    Server.getInstance().setNewYearCard(newyear);
                    newyear.startNewYearCardTask();
                }
            }
        }
    }
}
