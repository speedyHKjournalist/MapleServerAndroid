/*
 This file is part of the OdinMS Maple Story Server
 Copyright (C) 2008 Patrick Huy <patrick.huy@frz.cc>
 Matthias Butz <matze@odinms.de>
 Jan Christian Meyer <vimes@odinms.de>

 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU Affero General Public License version 3
 as published by the Free Software Foundation. You may not use, modify
 or distribute this program under any other version of the
 GNU Affero General Public License.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package client.inventory;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import database.MapleDBHelper;
import net.server.Server;
import tools.DatabaseConnection;
import tools.Pair;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author Flav
 */
public enum ItemFactory {

    INVENTORY(1, false),
    STORAGE(2, true),
    CASH_EXPLORER(3, true),
    CASH_CYGNUS(4, true),
    CASH_ARAN(5, true),
    MERCHANT(6, false),
    CASH_OVERALL(7, true),
    MARRIAGE_GIFTS(8, false),
    DUEY(9, false);
    private final int value;
    private final boolean account;

    private static final int lockCount = 400;
    private static final Lock[] locks = new Lock[lockCount];  // thanks Masterrulax for pointing out a bottleneck issue here

    static {
        for (int i = 0; i < lockCount; i++) {
            locks[i] = new ReentrantLock(true);
        }
    }

    ItemFactory(int value, boolean account) {
        this.value = value;
        this.account = account;
    }

    public int getValue() {
        return value;
    }

    public List<Pair<Item, InventoryType>> loadItems(int id, boolean login) throws SQLiteException {
        if (value != 6) {
            return loadItemsCommon(id, login);
        } else {
            return loadItemsMerchant(id, login);
        }
    }

    public void saveItems(List<Pair<Item, InventoryType>> items, int id, SQLiteDatabase con) throws SQLiteException {
        saveItems(items, null, id, con);
    }

    public void saveItems(List<Pair<Item, InventoryType>> items, List<Short> bundlesList, int id, SQLiteDatabase con) throws SQLiteException {
        // thanks Arufonsu, MedicOP, BHB for pointing a "synchronized" bottleneck here

        if (value != 6) {
            saveItemsCommon(items, id, con);
        } else {
            saveItemsMerchant(items, bundlesList, id, con);
        }
    }

    private static Equip loadEquipFromResultSet(Cursor cursor) throws SQLiteException {
        int itemidIndex = cursor.getColumnIndex("itemid");
        int positionIndex = cursor.getColumnIndex("position");
        int ownerIndex = cursor.getColumnIndex("owner");
        int quantityIndex = cursor.getColumnIndex("quantity");
        int accIndex = cursor.getColumnIndex("acc");
        int avoidIndex = cursor.getColumnIndex("avoid");
        int dexIndex = cursor.getColumnIndex("dex");
        int handsIndex = cursor.getColumnIndex("hands");
        int hpIndex = cursor.getColumnIndex("hp");
        int intIndex = cursor.getColumnIndex("int");
        int jumpIndex = cursor.getColumnIndex("jump");
        int viciousIndex = cursor.getColumnIndex("vicious");
        int flagIndex = cursor.getColumnIndex("flag");
        int lukIndex = cursor.getColumnIndex("luk");
        int matkIndex = cursor.getColumnIndex("matk");
        int mdefIndex = cursor.getColumnIndex("mdef");
        int mpIndex = cursor.getColumnIndex("mp");
        int speedIndex = cursor.getColumnIndex("speed");
        int strIndex = cursor.getColumnIndex("str");
        int watkIndex = cursor.getColumnIndex("watk");
        int wdefIndex = cursor.getColumnIndex("wdef");
        int upgradeSlotsIndex = cursor.getColumnIndex("upgradeslots");
        int levelIndex = cursor.getColumnIndex("level");
        int itemExpIndex = cursor.getColumnIndex("itemexp");
        int itemLevelIndex = cursor.getColumnIndex("itemlevel");
        int expirationIndex = cursor.getColumnIndex("expiration");
        int giftFromIndex = cursor.getColumnIndex("giftFrom");
        int ringIdIndex = cursor.getColumnIndex("ringid");

        int itemid = itemidIndex != -1 ? cursor.getInt(itemidIndex) : 0;
        short position = (short) (positionIndex != -1 ? cursor.getInt(positionIndex) : 0);

        Equip equip = new Equip(itemid, position);
        equip.setOwner(ownerIndex != -1 ? cursor.getString(ownerIndex) : null);
        equip.setQuantity((short) (quantityIndex != -1 ? cursor.getInt(quantityIndex) : 0));
        equip.setAcc((short) (accIndex != -1 ? cursor.getInt(accIndex) : 0));
        equip.setAvoid((short) (avoidIndex != -1 ? cursor.getInt(avoidIndex) : 0));
        equip.setDex((short) (dexIndex != -1 ? cursor.getInt(dexIndex) : 0));
        equip.setHands((short) (handsIndex != -1 ? cursor.getInt(handsIndex) : 0));
        equip.setHp((short) (hpIndex != -1 ? cursor.getInt(hpIndex) : 0));
        equip.setInt((short) (intIndex != -1 ? cursor.getInt(intIndex) : 0));
        equip.setJump((short) (jumpIndex != -1 ? cursor.getInt(jumpIndex) : 0));
        equip.setVicious((short) (viciousIndex != -1 ? cursor.getInt(viciousIndex) : 0));
        equip.setFlag((short) (flagIndex != -1 ? cursor.getInt(flagIndex) : 0));
        equip.setLuk((short) (lukIndex != -1 ? cursor.getInt(lukIndex) : 0));
        equip.setMatk((short) (matkIndex != -1 ? cursor.getInt(matkIndex) : 0));
        equip.setMdef((short) (mdefIndex != -1 ? cursor.getInt(mdefIndex) : 0));
        equip.setMp((short) (mpIndex != -1 ? cursor.getInt(mpIndex) : 0));
        equip.setSpeed((short) (speedIndex != -1 ? cursor.getInt(speedIndex) : 0));
        equip.setStr((short) (strIndex != -1 ? cursor.getInt(strIndex) : 0));
        equip.setWatk((short) (watkIndex != -1 ? cursor.getInt(watkIndex) : 0));
        equip.setWdef((short) (wdefIndex != -1 ? cursor.getInt(wdefIndex) : 0));
        equip.setUpgradeSlots((byte) (upgradeSlotsIndex != -1 ? cursor.getInt(upgradeSlotsIndex) : 0));
        equip.setLevel((byte) (levelIndex != -1 ? cursor.getInt(levelIndex) : 0));
        equip.setItemExp(itemExpIndex != -1 ? cursor.getInt(itemExpIndex) : 0);
        equip.setItemLevel((byte) (itemLevelIndex != -1 ? cursor.getInt(itemLevelIndex) : 0));
        equip.setExpiration(expirationIndex != -1 ? cursor.getLong(expirationIndex) : 0);
        equip.setGiftFrom(giftFromIndex != -1 ? cursor.getString(giftFromIndex) : null);
        equip.setRingId(ringIdIndex != -1 ? cursor.getInt(ringIdIndex) : 0);

        return equip;
    }

    public static List<Pair<Item, Integer>> loadEquippedItems(int id, boolean isAccount, boolean login) throws SQLiteException {
        List<Pair<Item, Integer>> items = new ArrayList<>();

        StringBuilder query = new StringBuilder();
        query.append("SELECT * FROM ");
        query.append("(SELECT id, accountid FROM characters) AS accountterm ");
        query.append("RIGHT JOIN ");
        query.append("(SELECT * FROM (`inventoryitems` LEFT JOIN `inventoryequipment` USING(`inventoryitemid`))) AS equipterm");
        query.append(" ON accountterm.id=equipterm.characterid ");
        query.append("WHERE accountterm.`");
        query.append(isAccount ? "accountid" : "characterid");
        query.append("` = ?");
        query.append(login ? " AND `inventorytype` = " + InventoryType.EQUIPPED.getType() : "");

        try (SQLiteDatabase con = DatabaseConnection.getConnection()) {
            String[] selectionArgs = {String.valueOf(id)};
            try (Cursor cursor = con.rawQuery(query.toString(), selectionArgs)) {
                while (cursor.moveToNext()) {
                    int characteridIdx = cursor.getColumnIndex("characterid");
                    if (characteridIdx != -1) {
                        Integer cid = cursor.getInt(characteridIdx);
                        items.add(new Pair<>(loadEquipFromResultSet(cursor), cid));
                    }
                }
            }
        } catch (SQLiteException e) {
            e.printStackTrace();
        }
        return items;
    }

    private List<Pair<Item, InventoryType>> loadItemsCommon(int id, boolean login) throws SQLiteException {
        List<Pair<Item, InventoryType>> items = new ArrayList<>();

        StringBuilder query = new StringBuilder();
        query.append("SELECT * FROM `inventoryitems` LEFT JOIN `inventoryequipment` USING(`inventoryitemid`) WHERE `type` = ? AND `");
        query.append(account ? "accountid" : "characterid").append("` = ?");

        if (login) {
            query.append(" AND `inventorytype` = ").append(InventoryType.EQUIPPED.getType());
        }
        String[] selectionArgs = {String.valueOf(value), String.valueOf(id)};
        SQLiteDatabase con = DatabaseConnection.getConnection();
        try (Cursor cursor = con.rawQuery(query.toString(), selectionArgs)) {
            while (cursor.moveToNext()) {
                int inventorytypeIdx = cursor.getColumnIndex("inventorytype");
                if (inventorytypeIdx != -1) {
                    InventoryType mit = InventoryType.getByType((byte) cursor.getShort(inventorytypeIdx));

                    int petid = -1;
                    if (mit.equals(InventoryType.EQUIP) || mit.equals(InventoryType.EQUIPPED)) {
                        items.add(new Pair<>(loadEquipFromResultSet(cursor), mit));
                    } else {
                        int petidIdx = cursor.getColumnIndex("petid");
                        if (petidIdx != -1) {
                            petid = cursor.getInt(petidIdx);
                            if (petid == 0) {
                                petid = -1;
                            }
                        }

                        int itemidIdx = cursor.getColumnIndex("itemid");
                        int positionIdx = cursor.getColumnIndex("position");
                        int quantityIdx = cursor.getColumnIndex("quantity");

                        int ownerIdx = cursor.getColumnIndex("owner");
                        int expirationIdx = cursor.getColumnIndex("expiration");
                        int giftFromIdx = cursor.getColumnIndex("giftFrom");
                        int flagIdx = cursor.getColumnIndex("flag");

                        if (itemidIdx != -1 &&
                                positionIdx != -1 &&
                                quantityIdx != -1 &&
                                ownerIdx != -1 &&
                                expirationIdx != -1 &&
                                giftFromIdx != -1 &&
                                flagIdx != -1) {
                            Item item = new Item(
                                    cursor.getInt(itemidIdx),
                                    (byte) cursor.getShort(positionIdx),
                                    cursor.getShort(quantityIdx),
                                    petid
                            );

                            item.setOwner(cursor.getString(ownerIdx));
                            item.setExpiration(cursor.getLong(expirationIdx));
                            item.setGiftFrom(cursor.getString(giftFromIdx));
                            item.setFlag(cursor.getShort(flagIdx));
                            items.add(new Pair<>(item, mit));
                        }
                    }
                }
            }
        }

        return items;
    }

    private void saveItemsCommon(List<Pair<Item, InventoryType>> items, int id, SQLiteDatabase con) throws SQLiteException {
        Lock lock = locks[id % lockCount];
        lock.lock();
        try {
            String query = "DELETE FROM inventoryitems " +
                    "WHERE inventoryitemid IN (SELECT inventoryitemid " +
                    "FROM inventoryequipment " +
                    "WHERE type = ? AND " + (account ? "accountid" : "characterid") + " = ?);";
            con.execSQL(query, new String[] { String.valueOf(value), String.valueOf(id) });

            try {
                if (!items.isEmpty()) {
                    for (Pair<Item, InventoryType> pair : items) {
                        Item item = pair.getLeft();
                        InventoryType mit = pair.getRight();
                        ContentValues itemValues = new ContentValues();
                        itemValues.put("type", value);
                        itemValues.put("characterid", account ? null : String.valueOf(id));
                        itemValues.put("accountid", account ? String.valueOf(id) : null);
                        itemValues.put("itemid", item.getItemId());
                        itemValues.put("inventorytype", mit.getType());
                        itemValues.put("position", item.getPosition());
                        itemValues.put("quantity", item.getQuantity());
                        itemValues.put("owner", item.getOwner());
                        itemValues.put("petid", item.getPetId());
                        itemValues.put("flag", item.getFlag());
                        itemValues.put("expiration", item.getExpiration());
                        itemValues.put("giftFrom", item.getGiftFrom());

                        long newRowId = con.insert("inventoryitems", null, itemValues);

                        if (mit.equals(InventoryType.EQUIP) || mit.equals(InventoryType.EQUIPPED)) {
                            ContentValues equipValues = new ContentValues();
                            if (newRowId != -1) {
                                equipValues.put("inventoryitemid", newRowId);
                            } else {
                                throw new RuntimeException("Inserting item failed.");
                            }

                            Equip equip = (Equip) item;
                            equipValues.put("upgradeslots", equip.getUpgradeSlots());
                            equipValues.put("level", equip.getLevel());
                            equipValues.put("str", equip.getStr());
                            equipValues.put("dex", equip.getDex());
                            equipValues.put("int", equip.getInt());
                            equipValues.put("luk", equip.getLuk());
                            equipValues.put("hp", equip.getHp());
                            equipValues.put("mp", equip.getMp());
                            equipValues.put("watk", equip.getWatk());
                            equipValues.put("matk", equip.getMatk());
                            equipValues.put("wdef", equip.getWdef());
                            equipValues.put("mdef", equip.getMdef());
                            equipValues.put("acc", equip.getAcc());
                            equipValues.put("avoid", equip.getAvoid());
                            equipValues.put("hands", equip.getHands());
                            equipValues.put("speed", equip.getSpeed());
                            equipValues.put("jump", equip.getJump());
                            equipValues.put("locked", 0);
                            equipValues.put("vicious", equip.getVicious());
                            equipValues.put("itemlevel", equip.getItemLevel());
                            equipValues.put("itemexp", equip.getItemExp());
                            equipValues.put("ringid", equip.getRingId());

                            con.insert("inventoryequipment", null, equipValues);
                        }
                    }
                }
            } catch (SQLiteException e) {
                throw new RuntimeException(e);
            }
        } finally {
            lock.unlock();
        }
    }

    private List<Pair<Item, InventoryType>> loadItemsMerchant(int id, boolean login) throws SQLiteException {
        List<Pair<Item, InventoryType>> items = new ArrayList<>();
        StringBuilder query = new StringBuilder();
        query.append("SELECT * FROM `inventoryitems` LEFT JOIN `inventoryequipment` USING(`inventoryitemid`) WHERE `type` = ? AND `");
        query.append(account ? "accountid" : "characterid").append("` = ?");

        if (login) {
            query.append(" AND `inventorytype` = ").append(InventoryType.EQUIPPED.getType());
        }
        String[] selectionArgs = { String.valueOf(value), String.valueOf(id) };
        SQLiteDatabase con = DatabaseConnection.getConnection();
        try (Cursor cursor = con.rawQuery(query.toString(), selectionArgs)) {

            while (cursor.moveToNext()) {
                short bundles = 0;
                int inventoryitemidIdx = cursor.getColumnIndex("inventoryitemid");
                if (inventoryitemidIdx != -1) {
                    try(Cursor cursor2 = con.rawQuery("SELECT bundles FROM inventorymerchant WHERE inventoryitemid = ?",
                            new String[]{String.valueOf(cursor.getInt(inventoryitemidIdx))})) {
                        if (cursor2.moveToNext()) {
                            int bundlesIdx = cursor2.getColumnIndex("bundles");
                            if (bundlesIdx != -1) {
                                bundles = cursor2.getShort(bundlesIdx);
                            }
                        }
                    }
                    int inventorytypeIdx = cursor.getColumnIndex("inventorytype");
                    if (inventorytypeIdx != -1) {
                        InventoryType mit = InventoryType.getByType((byte) cursor.getInt(inventorytypeIdx));

                        if (mit.equals(InventoryType.EQUIP) || mit.equals(InventoryType.EQUIPPED)) {
                            items.add(new Pair<>(loadEquipFromResultSet(cursor), mit));
                        } else {
                            if (bundles > 0) {
                                int petidIdx = cursor.getColumnIndex("petid");
                                int itemidIdx = cursor.getColumnIndex("itemid");
                                int positionIdx = cursor.getColumnIndex("position");
                                int quantityIdx = cursor.getColumnIndex("quantity");
                                int ownerIdx = cursor.getColumnIndex("owner");
                                int expirationIdx = cursor.getColumnIndex("expiration");
                                int giftFromIdx = cursor.getColumnIndex("giftFrom");
                                int flagIdx = cursor.getColumnIndex("flag");

                                int petid = cursor.getInt(petidIdx);
                                if (petidIdx != -1 &&
                                        itemidIdx != -1 &&
                                        positionIdx != -1 &&
                                        quantityIdx != -1 &&
                                        ownerIdx != -1 &&
                                        expirationIdx != -1 &&
                                        giftFromIdx != -1 &&
                                        flagIdx != -1) {
                                    if (cursor.isNull(petidIdx)) {
                                        petid = -1;
                                    }

                                    Item item = new Item(cursor.getInt(itemidIdx),
                                            (byte) cursor.getInt(positionIdx),
                                            (short) (bundles * cursor.getInt(quantityIdx)),
                                            petid);
                                    item.setOwner(cursor.getString(ownerIdx));
                                    item.setExpiration(cursor.getLong(expirationIdx));
                                    item.setGiftFrom(cursor.getString(giftFromIdx));
                                    item.setFlag((short) cursor.getInt(flagIdx));
                                    items.add(new Pair<>(item, mit));
                                }
                            }
                        }
                    }
                }
            }
        }
        return items;
    }

    private void saveItemsMerchant(List<Pair<Item, InventoryType>> items, List<Short> bundlesList, int id, SQLiteDatabase con) throws SQLiteException {
        Lock lock = locks[id % lockCount];
        lock.lock();
        try {
            String deleteQuery = "DELETE FROM inventorymerchant WHERE characterid = ?";
            con.execSQL(deleteQuery, new String[] { String.valueOf(id) });

            StringBuilder query = new StringBuilder();
            query.append("DELETE `inventoryitems`, `inventoryequipment` FROM `inventoryitems` LEFT JOIN `inventoryequipment` USING(`inventoryitemid`) WHERE `type` = ? AND `");
            query.append(account ? "accountid" : "characterid").append("` = ?");

            String columnName = account ? "accountid" : "characterid";
            String[] selectionArgs = { String.valueOf(value), String.valueOf(id) };
            String delQuery = "DELETE FROM inventoryitems " +
                    "WHERE inventoryitemid IN " +
                    "(SELECT inventoryitems.inventoryitemid " +
                    "FROM inventoryitems " +
                    "LEFT JOIN inventoryequipment ON inventoryitems.inventoryitemid = inventoryequipment.inventoryitemid " +
                    "WHERE inventoryitems.type = ? " +
                    "AND " + columnName + " = ?)";
            con.execSQL(delQuery, selectionArgs);

            int i = 0;
            for (Pair<Item, InventoryType> pair : items) {
                final Item item = pair.getLeft();
                final Short bundles = bundlesList.get(i);
                final InventoryType mit = pair.getRight();
                i++;

                int genKey = 0;
                // Item
                ContentValues values = new ContentValues();
                values.put("type", value);
                values.put("characterid", account ? null : id);
                values.put("accountid", account ? id : null);
                values.put("itemid", item.getItemId());
                values.put("inventorytype", mit.getType());
                values.put("position", item.getPosition());
                values.put("quantity", item.getQuantity());
                values.put("owner", item.getOwner());
                values.put("petid", item.getPetId());
                values.put("flag", item.getFlag());
                values.put("expiration", item.getExpiration());
                values.put("giftFrom", item.getGiftFrom());
                long rowId = con.insert("inventoryitems", null, values);

                if (rowId == -1) {
                    throw new RuntimeException("Inserting data into inventoryitems failed.");
                } else {
                    genKey = (int) rowId; // Assuming genKey is an int
                }

                ContentValues values1 = new ContentValues();
                values1.put("inventoryitemid", genKey);
                values1.put("characterid", id);
                values1.put("bundles", bundles);
                // Merchant
                rowId = con.insert("inventorymerchant", null, values1);
                if (rowId == -1) {
                    throw new RuntimeException("Inserting data into inventorymerchant failed.");
                }

                ContentValues values2 = new ContentValues();
                values2.put("inventoryitemid", genKey);

                Equip equip = (Equip) item;
                values2.put("upgradeslots", equip.getUpgradeSlots());
                values2.put("level", equip.getLevel());
                values2.put("str", equip.getStr());
                values2.put("dex", equip.getDex());
                values2.put("int", equip.getInt()); // Note: "int" is a reserved keyword in SQLite, so use backticks in your table schema
                values2.put("luk", equip.getLuk());
                values2.put("hp", equip.getHp());
                values2.put("mp", equip.getMp());
                values2.put("watk", equip.getWatk());
                values2.put("matk", equip.getMatk());
                values2.put("wdef", equip.getWdef());
                values2.put("mdef", equip.getMdef());
                values2.put("acc", equip.getAcc());
                values2.put("avoid", equip.getAvoid());
                values2.put("hands", equip.getHands());
                values2.put("speed", equip.getSpeed());
                values2.put("jump", equip.getJump());
                values2.put("vicious", equip.getVicious());
                values2.put("itemlevel", equip.getItemLevel());
                values2.put("itemexp", equip.getItemExp());
                values2.put("ringid", equip.getRingId());
                // Equipment
                if (mit.equals(InventoryType.EQUIP) || mit.equals(InventoryType.EQUIPPED)) {
                    rowId = con.insert("inventoryequipment", null, values);
                    if (rowId == -1) {
                        throw new RuntimeException("Inserting data into inventoryequipment failed.");
                    }
                }
            }
        } finally {
            lock.unlock();
        }
    }
}