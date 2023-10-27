/*
	This file is part of the OdinMS Maple Story Server
    Copyright (C) 2008 Patrick Huy <patrick.huy@frz.cc>
		       Matthias Butz <matze@odinms.de>
		       Jan Christian Meyer <vimes@odinms.de>

    Copyleft (L) 2016 - 2019 RonanLana (HeavenMS)

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
package client.processor.npc;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteStatement;
import client.Character;
import client.Client;
import client.inventory.Inventory;
import client.inventory.InventoryType;
import client.inventory.Item;
import client.inventory.ItemFactory;
import client.inventory.manipulator.InventoryManipulator;
import net.server.Server;
import net.server.world.World;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import server.ItemInformationProvider;
import server.maps.HiredMerchant;
import service.NoteService;
import tools.DatabaseConnection;
import tools.PacketCreator;
import tools.Pair;

import java.sql.*;
import java.util.LinkedList;
import java.util.List;

import static java.util.concurrent.TimeUnit.DAYS;

/**
 * @author RonanLana - synchronization of Fredrick modules and operation results
 */
public class FredrickProcessor {
    private static final Logger log = LoggerFactory.getLogger(FredrickProcessor.class);
    private static final int[] dailyReminders = new int[]{2, 5, 10, 15, 30, 60, 90, Integer.MAX_VALUE};

    private final NoteService noteService;

    public FredrickProcessor(NoteService noteService) {
        this.noteService = noteService;
    }

    private static byte canRetrieveFromFredrick(Character chr, List<Pair<Item, InventoryType>> items) {
        if (!Inventory.checkSpotsAndOwnership(chr, items)) {
            List<Integer> itemids = new LinkedList<>();
            for (Pair<Item, InventoryType> it : items) {
                itemids.add(it.getLeft().getItemId());
            }

            if (chr.canHoldUniques(itemids)) {
                return 0x22;
            } else {
                return 0x20;
            }
        }

        int netMeso = chr.getMerchantNetMeso();
        if (netMeso > 0) {
            if (!chr.canHoldMeso(netMeso)) {
                return 0x1F;
            }
        } else {
            if (chr.getMeso() < -1 * netMeso) {
                return 0x21;
            }
        }

        return 0x0;
    }

    public static int timestampElapsedDays(Timestamp then, long timeNow) {
        return (int) ((timeNow - then.getTime()) / DAYS.toMillis(1));
    }

    private static String fredrickReminderMessage(int daynotes) {
        String msg;

        if (daynotes < 4) {
            msg = "Hi customer! I am Fredrick, the Union Chief of the Hired Merchant Union. A reminder that " + dailyReminders[daynotes] + " days have passed since you used our service. Please reclaim your stored goods at FM Entrance.";
        } else {
            msg = "Hi customer! I am Fredrick, the Union Chief of the Hired Merchant Union. " + dailyReminders[daynotes] + " days have passed since you used our service. Consider claiming back the items before we move them away for refund.";
        }

        return msg;
    }

    public static void removeFredrickLog(int cid) {
        try (SQLiteDatabase con = DatabaseConnection.getConnection()) {
            removeFredrickLog(con, cid);
        } catch (SQLiteException sqle) {
            sqle.printStackTrace();
        }
    }

    private static void removeFredrickLog(SQLiteDatabase con, int cid) throws SQLiteException {
        try (SQLiteStatement stmt = con.compileStatement("DELETE FROM `fredstorage` WHERE `cid` = ?")) {
            stmt.bindLong(1, cid);
            stmt.executeUpdateDelete();
        }
    }

    public static void insertFredrickLog(int cid) {
        try (SQLiteDatabase con = DatabaseConnection.getConnection()) {
            removeFredrickLog(con, cid);
            try (SQLiteStatement stmt = con.compileStatement("INSERT INTO `fredstorage` (`cid`, `daynotes`, `timestamp`) VALUES (?, 0, ?)")) {
                stmt.bindLong(1, cid);
                stmt.bindLong(2, System.currentTimeMillis() / 1000);
                stmt.executeInsert();
            }
        } catch (SQLiteException sqle) {
            sqle.printStackTrace();
        }
    }

    private static void removeFredrickReminders(List<Pair<Integer, Integer>> expiredCids) {
        List<String> expiredCnames = new LinkedList<>();
        for (Pair<Integer, Integer> id : expiredCids) {
            String name = Character.getNameById(id.getLeft());
            if (name != null) {
                expiredCnames.add(name);
            }
        }

        SQLiteDatabase con = DatabaseConnection.getConnection();
        try {
            con.beginTransaction();
            SQLiteStatement stmt = con.compileStatement("DELETE FROM `notes` WHERE `from` LIKE ? AND `to` LIKE ?");
            stmt.bindString(1, "FREDRICK");

            for (String cname : expiredCnames) {
                stmt.bindString(2, cname);
                stmt.executeUpdateDelete();
            }
            con.setTransactionSuccessful();
        } catch (SQLiteException e) {
            e.printStackTrace();
        } finally {
            con.endTransaction();
        }
    }

    public void runFredrickSchedule() {
        try (SQLiteDatabase con = DatabaseConnection.getConnection()) {
            List<Pair<Integer, Integer>> expiredCids = new LinkedList<>();
            List<Pair<Pair<Integer, String>, Integer>> notifCids = new LinkedList<>();
            try (Cursor cursor = con.rawQuery("SELECT * FROM fredstorage f LEFT JOIN (SELECT id, name, world, lastLogoutTime FROM characters) AS c ON c.id = f.cid", null)) {
                long curTime = System.currentTimeMillis();

                while (cursor.moveToNext()) {
                    int cidIndex = cursor.getColumnIndex("cid");
                    int worldIndex = cursor.getColumnIndex("world");
                    int timestampIndex = cursor.getColumnIndex("timestamp");
                    int daynotesIndex = cursor.getColumnIndex("daynotes");
                    int lastLogoutTimeIndex = cursor.getColumnIndex("lastLogoutTime");
                    int nameIndex = cursor.getColumnIndex("name");

                    int cid = (cidIndex != -1) ? cursor.getInt(cidIndex) : -1;
                    int world = (worldIndex != -1) ? cursor.getInt(worldIndex) : -1;
                    long timestamp = (timestampIndex != -1) ? cursor.getLong(timestampIndex) : -1;
                    int daynotes = (daynotesIndex != -1) ? cursor.getInt(daynotesIndex) : -1;
                    long logoutTimestamp = (lastLogoutTimeIndex != -1) ? cursor.getLong(lastLogoutTimeIndex) : -1;
                    String name = (nameIndex != -1) ? cursor.getString(nameIndex) : null;

                    Timestamp ts = new Timestamp(timestamp);
                    daynotes = Math.min(dailyReminders.length - 1, daynotes);

                    int elapsedDays = timestampElapsedDays(ts, curTime);
                    if (elapsedDays > 100) {
                        expiredCids.add(new Pair<>(cid, world));
                    } else {
                        int notifDay = dailyReminders[daynotes];

                        if (elapsedDays >= notifDay) {
                            do {
                                daynotes++;
                                notifDay = dailyReminders[daynotes];
                            } while (elapsedDays >= notifDay);

                            Timestamp logoutTs = new Timestamp(logoutTimestamp);
                            int inactivityDays = timestampElapsedDays(logoutTs, curTime);

                            if (inactivityDays < 7 || daynotes >= dailyReminders.length - 1) {  // don't spam inactive players
                                notifCids.add(new Pair<>(new Pair<>(cid, name), daynotes));
                            }
                        }
                    }
                }

            }

            if (!expiredCids.isEmpty()) {
                con.beginTransaction();
                try {
                    for (Pair<Integer, Integer> cid : expiredCids) {
                        String deleteQuery = "DELETE FROM inventoryitems WHERE type = " + ItemFactory.MERCHANT.getValue() +
                                " AND characterid = " + cid.getLeft();
                        con.execSQL(deleteQuery);
                    }
                    con.setTransactionSuccessful();
                } finally {
                    con.endTransaction();
                }

                con.beginTransaction();
                try {
                    for (Pair<Integer, Integer> cid : expiredCids) {
                        String updateQuery = "UPDATE characters SET MerchantMesos = 0 WHERE id = " + cid.getLeft();
                        con.execSQL(updateQuery);

                        World wserv = Server.getInstance().getWorld(cid.getRight());
                        if (wserv != null) {
                            Character chr = wserv.getPlayerStorage().getCharacterById(cid.getLeft());
                            if (chr != null) {
                                chr.setMerchantMeso(0);
                            }
                        }
                    }
                    con.setTransactionSuccessful();
                } finally {
                    con.endTransaction();
                }

                removeFredrickReminders(expiredCids);

                con.beginTransaction();
                try {
                    for (Pair<Integer, Integer> cid : expiredCids) {
                        String deleteQuery = "DELETE FROM fredstorage WHERE cid = " + cid.getLeft();
                        con.execSQL(deleteQuery);
                    }
                    con.setTransactionSuccessful();
                } finally {
                    con.endTransaction();
                }
            }

            if (!notifCids.isEmpty()) {
                con.beginTransaction();
                try {
                    for (Pair<Pair<Integer, String>, Integer> cid : notifCids) {
                        String updateQuery = "UPDATE fredstorage SET daynotes = " + cid.getRight() + " WHERE cid = " + cid.getLeft().getLeft();
                        con.execSQL(updateQuery);

                        String msg = fredrickReminderMessage(cid.getRight() - 1);
                        noteService.sendNormal(msg, "FREDRICK", cid.getLeft().getRight());
                    }
                    con.setTransactionSuccessful();
                } finally {
                    con.endTransaction();
                }
            }
        } catch (SQLiteException e) {
            e.printStackTrace();
        }
    }

    private static boolean deleteFredrickItems(int cid) {
        try (SQLiteDatabase con = DatabaseConnection.getConnection()) {
            String deleteQuery = "DELETE FROM inventoryitems WHERE type = ? AND characterid = ?";
            Object[] bindArgs = { ItemFactory.MERCHANT.getValue(), cid };
            con.execSQL(deleteQuery, bindArgs);
            return true;
        } catch (SQLiteException e) {
            e.printStackTrace();
            return false;
        }
    }

    public void fredrickRetrieveItems(Client c) {     // thanks Gustav for pointing out the dupe on Fredrick handling
        if (c.tryacquireClient()) {
            try {
                Character chr = c.getPlayer();

                List<Pair<Item, InventoryType>> items;
                try {
                    items = ItemFactory.MERCHANT.loadItems(chr.getId(), false);

                    byte response = canRetrieveFromFredrick(chr, items);
                    if (response != 0) {
                        chr.sendPacket(PacketCreator.fredrickMessage(response));
                        return;
                    }

                    chr.withdrawMerchantMesos();

                    if (deleteFredrickItems(chr.getId())) {
                        HiredMerchant merchant = chr.getHiredMerchant();

                        if (merchant != null) {
                            merchant.clearItems();
                        }

                        for (Pair<Item, InventoryType> it : items) {
                            Item item = it.getLeft();
                            InventoryManipulator.addFromDrop(chr.getClient(), item, false);
                            String itemName = ItemInformationProvider.getInstance().getName(item.getItemId());
                            log.debug("Chr {} gained {}x {} ({})", chr.getName(), item.getQuantity(), itemName, item.getItemId());
                        }

                        chr.sendPacket(PacketCreator.fredrickMessage((byte) 0x1E));
                        removeFredrickLog(chr.getId());
                    } else {
                        chr.message("An unknown error has occured.");
                    }
                } catch (SQLiteException ex) {
                    ex.printStackTrace();
                }
            } finally {
                c.releaseClient();
            }
        }
    }
}
