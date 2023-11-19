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
package server;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import client.inventory.*;
import config.YamlConfig;
import constants.id.ItemId;
import constants.inventory.ItemConstants;
import net.server.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import provider.Data;
import provider.DataProvider;
import provider.DataProviderFactory;
import provider.DataTool;
import provider.wz.WZFiles;
import tools.DatabaseConnection;
import tools.Pair;

import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static java.util.concurrent.TimeUnit.DAYS;
import static java.util.concurrent.TimeUnit.HOURS;

/*
 * @author Flav
 */
public class CashShop {
    public static class CashItem {

        private final int sn;
        private final int itemId;
        private final int price;
        private final long period;
        private final short count;
        private final boolean onSale;

        private CashItem(int sn, int itemId, int price, long period, short count, boolean onSale) {
            this.sn = sn;
            this.itemId = itemId;
            this.price = price;
            this.period = (period == 0 ? 90 : period);
            this.count = count;
            this.onSale = onSale;
        }

        public int getSN() {
            return sn;
        }

        public int getItemId() {
            return itemId;
        }

        public int getPrice() {
            return price;
        }

        public short getCount() {
            return count;
        }

        public boolean isOnSale() {
            return onSale;
        }

        public Item toItem() {
            Item item;

            int petid = -1;
            if (ItemConstants.isPet(itemId)) {
                petid = Pet.createPet(itemId);
            }

            if (ItemConstants.getInventoryType(itemId).equals(InventoryType.EQUIP)) {
                item = ItemInformationProvider.getInstance().getEquipById(itemId);
            } else {
                item = new Item(itemId, (byte) 0, count, petid);
            }

            if (ItemConstants.EXPIRING_ITEMS) {
                if (period == 1) {
                    switch (itemId) {
                    case ItemId.DROP_COUPON_2X_4H, ItemId.EXP_COUPON_2X_4H: // 4 Hour 2X coupons, the period is 1, but we don't want them to last a day.
                        item.setExpiration(Server.getInstance().getCurrentTime() + HOURS.toMillis(4));
                            /*
                            } else if(itemId == 5211047 || itemId == 5360014) { // 3 Hour 2X coupons, unused as of now
                                    item.setExpiration(Server.getInstance().getCurrentTime() + HOURS.toMillis(3));
                            */
                        break;
                    case ItemId.EXP_COUPON_3X_2H:
                        item.setExpiration(Server.getInstance().getCurrentTime() + HOURS.toMillis(2));
                        break;
                    default:
                        item.setExpiration(Server.getInstance().getCurrentTime() + DAYS.toMillis(1));
                        break;
                    }
                } else {
                    item.setExpiration(Server.getInstance().getCurrentTime() + DAYS.toMillis(period));
                }
            }

            item.setSN(sn);
            return item;
        }
    }

    public static class SpecialCashItem {
        private final int sn;
        private final int modifier;
        private final byte info; //?

        public SpecialCashItem(int sn, int modifier, byte info) {
            this.sn = sn;
            this.modifier = modifier;
            this.info = info;
        }

        public int getSN() {
            return sn;
        }

        public int getModifier() {
            return modifier;
        }

        public byte getInfo() {
            return info;
        }
    }

    public static class CashItemFactory {
        private static volatile Map<Integer, CashItem> items = new HashMap<>();
        private static volatile List<Integer> randomitemsns = new ArrayList<>();
        private static volatile Map<Integer, List<Integer>> packages = new HashMap<>();
        private static volatile List<SpecialCashItem> specialcashitems = new ArrayList<>();
        private static final Logger log = LoggerFactory.getLogger(CashItemFactory.class);

        public static void loadAllCashItems() {
            DataProvider etc = DataProviderFactory.getDataProvider(WZFiles.ETC);

            Map<Integer, CashItem> loadedItems = new HashMap<>();
            List<Integer> onSaleItems = new ArrayList<>();
            for (Data item : etc.getData("Commodity.img").getChildren()) {
                int sn = DataTool.getIntConvert("SN", item);
                int itemId = DataTool.getIntConvert("ItemId", item);
                int price = DataTool.getIntConvert("Price", item, 0);
                long period = DataTool.getIntConvert("Period", item, 1);
                short count = (short) DataTool.getIntConvert("Count", item, 1);
                boolean onSale = DataTool.getIntConvert("OnSale", item, 0) == 1;
                loadedItems.put(sn, new CashItem(sn, itemId, price, period, count, onSale));

                if (onSale) {
                    onSaleItems.add(sn);
                }
            }
            CashItemFactory.items = loadedItems;
            CashItemFactory.randomitemsns = onSaleItems;

            Map<Integer, List<Integer>> loadedPackages = new HashMap<>();
            for (Data cashPackage : etc.getData("CashPackage.img").getChildren()) {
                List<Integer> cPackage = new ArrayList<>();

                for (Data item : cashPackage.getChildByPath("SN").getChildren()) {
                    cPackage.add(Integer.parseInt(item.getData().toString()));
                }

                loadedPackages.put(Integer.parseInt(cashPackage.getName()), cPackage);
            }
            CashItemFactory.packages = loadedPackages;

            List<SpecialCashItem> loadedSpecialItems = new ArrayList<>();
            String query = "SELECT * FROM specialcashitems";
            SQLiteDatabase con = DatabaseConnection.getConnection();
            try (Cursor cursor = con.rawQuery(query, null)) {
                if (cursor != null) {
                    if (cursor.moveToFirst()) {
                        do {
                            int snIdx = cursor.getColumnIndex("sn");
                            int modifierIdx = cursor.getColumnIndex("modifier");
                            int infoIdx = cursor.getColumnIndex("info");
                            if (snIdx != -1 && modifierIdx != -1 && infoIdx != -1) {
                                int sn = cursor.getInt(snIdx);
                                int modifier = cursor.getInt(modifierIdx);
                                byte info = (byte) cursor.getInt(infoIdx);

                                loadedSpecialItems.add(new SpecialCashItem(sn, modifier, info));
                            }
                        } while (cursor.moveToNext());
                    }
                }
            } catch (SQLiteException ex) {
                log.error("loadAllCashItems error", ex);
            }
            CashItemFactory.specialcashitems = loadedSpecialItems;
        }

        public static CashItem getRandomCashItem() {
            if (randomitemsns.isEmpty()) {
                return null;
            }

            int rnd = (int) (Math.random() * randomitemsns.size());
            return items.get(randomitemsns.get(rnd));
        }

        public static CashItem getItem(int sn) {
            return items.get(sn);
        }

        public static List<Item> getPackage(int itemId) {
            List<Item> cashPackage = new ArrayList<>();

            for (int sn : packages.get(itemId)) {
                cashPackage.add(getItem(sn).toItem());
            }

            return cashPackage;
        }

        public static boolean isPackage(int itemId) {
            return packages.containsKey(itemId);
        }

        public static List<SpecialCashItem> getSpecialCashItems() {
            return specialcashitems;
        }

        public static void reloadSpecialCashItems() {//Yay?
            List<SpecialCashItem> loadedSpecialItems = new ArrayList<>();
            String[] columns = { "sn", "modifier", "info" };
            SQLiteDatabase con = DatabaseConnection.getConnection();
            try (Cursor cursor = con.query("specialcashitems", columns, null, null, null, null, null)) {
                while (cursor.moveToNext()) {
                    int snIdx = cursor.getColumnIndex("sn");
                    int modifierIdx = cursor.getColumnIndex("modifier");
                    int infoIdx = cursor.getColumnIndex("info");
                    if (snIdx != -1 && modifierIdx != -1 && infoIdx != -1) {
                        loadedSpecialItems.add(new SpecialCashItem(cursor.getInt(snIdx), cursor.getInt(modifierIdx), (byte) cursor.getInt(infoIdx)));
                    }
                }
            } catch (SQLiteException ex) {
                log.error("reloadSpecialCashItems error", ex);
            }
            CashItemFactory.specialcashitems = loadedSpecialItems;
        }
    }

    private final int accountId;
    private final int characterId;
    private int nxCredit;
    private int maplePoint;
    private int nxPrepaid;
    private boolean opened;
    private ItemFactory factory;
    private final List<Item> inventory = new ArrayList<>();
    private final List<Integer> wishList = new ArrayList<>();
    private int notes = 0;
    private final Lock lock = new ReentrantLock();
    private static final Logger log = LoggerFactory.getLogger(CashShop.class);

    public CashShop(int accountId, int characterId, int jobType) throws SQLiteException {
        this.accountId = accountId;
        this.characterId = characterId;

        if (!YamlConfig.config.server.USE_JOINT_CASHSHOP_INVENTORY) {
            switch (jobType) {
            case 0:
                factory = ItemFactory.CASH_EXPLORER;
                break;
            case 1:
                factory = ItemFactory.CASH_CYGNUS;
                break;
            case 2:
                factory = ItemFactory.CASH_ARAN;
                break;
            }
        } else {
            factory = ItemFactory.CASH_OVERALL;
        }

        SQLiteDatabase con = DatabaseConnection.getConnection();
        try (Cursor ps = con.rawQuery("SELECT nxCredit, maplePoint, nxPrepaid FROM accounts WHERE id = ?", new String[] { String.valueOf(accountId) })) {
            if (ps.moveToNext()) {
                int nxCreditIdx = ps.getColumnIndex("nxCredit");
                int maplePointIdx =ps.getColumnIndex("maplePoint");
                int nxPrepaidIdx = ps.getColumnIndex("nxPrepaid");
                if (nxCreditIdx != -1 && maplePointIdx != -1 && nxPrepaidIdx != -1) {
                    this.nxCredit = ps.getInt(nxCreditIdx);
                    this.maplePoint = ps.getInt(maplePointIdx);
                    this.nxPrepaid = ps.getInt(nxPrepaidIdx);
                }
            }
        }

        for (Pair<Item, InventoryType> item : factory.loadItems(accountId, false)) {
            inventory.add(item.getLeft());
        }

        try (Cursor cursor = con.rawQuery("SELECT sn FROM wishlists WHERE charid = ?", new String[] { String.valueOf(characterId) })) {
            while (cursor.moveToNext()) {
                int snIdx = cursor.getColumnIndex("sn");
                if (snIdx != -1) {
                    wishList.add(cursor.getInt(snIdx));
                }
            }
        }
    }

    public int getCash(int type) {
        switch (type) {
            case 1:
                return nxCredit;
            case 2:
                return maplePoint;
            case 4:
                return nxPrepaid;
        }

        return 0;
    }

    public void gainCash(int type, int cash) {
        switch (type) {
            case 1:
                nxCredit += cash;
                break;
            case 2:
                maplePoint += cash;
                break;
            case 4:
                nxPrepaid += cash;
                break;
        }
    }

    public void gainCash(int type, CashItem buyItem, int world) {
        gainCash(type, -buyItem.getPrice());
        if (!YamlConfig.config.server.USE_ENFORCE_ITEM_SUGGESTION) {
            Server.getInstance().getWorld(world).addCashItemBought(buyItem.getSN());
        }
    }

    public boolean isOpened() {
        return opened;
    }

    public void open(boolean b) {
        opened = b;
    }

    public List<Item> getInventory() {
        lock.lock();
        try {
            return Collections.unmodifiableList(inventory);
        } finally {
            lock.unlock();
        }
    }

    public Item findByCashId(int cashId) {
        boolean isRing;
        Equip equip = null;
        for (Item item : getInventory()) {
            if (item.getInventoryType().equals(InventoryType.EQUIP)) {
                equip = (Equip) item;
                isRing = equip.getRingId() > -1;
            } else {
                isRing = false;
            }

            if ((item.getPetId() > -1 ? item.getPetId() : isRing ? equip.getRingId() : item.getCashId()) == cashId) {
                return item;
            }
        }

        return null;
    }

    public void addToInventory(Item item) {
        lock.lock();
        try {
            inventory.add(item);
        } finally {
            lock.unlock();
        }
    }

    public void removeFromInventory(Item item) {
        lock.lock();
        try {
            inventory.remove(item);
        } finally {
            lock.unlock();
        }
    }

    public List<Integer> getWishList() {
        return wishList;
    }

    public void clearWishList() {
        wishList.clear();
    }

    public void addToWishList(int sn) {
        wishList.add(sn);
    }

    public void gift(int recipient, String from, String message, int sn) {
        gift(recipient, from, message, sn, -1);
    }

    public void gift(int recipient, String from, String message, int sn, int ringid) {
        SQLiteDatabase con = DatabaseConnection.getConnection();
        try {
            ContentValues values = new ContentValues();
            values.put("recipient", recipient);
            values.put("from", from);
            values.put("message", message);
            values.put("sn", sn);
            values.put("ringid", ringid);
            con.insert("gifts", null, values);
        } catch (SQLiteException sqle) {
            log.error("insert into gift error", sqle);
        }
    }

    public List<Pair<Item, String>> loadGifts() {
        List<Pair<Item, String>> gifts = new ArrayList<>();
        SQLiteDatabase con = DatabaseConnection.getConnection();
        try {
            String[] selectionArgs = { String.valueOf(characterId) };

            try (Cursor cursor = con.rawQuery("SELECT * FROM `gifts` WHERE `to` = ?", selectionArgs)) {
                while (cursor.moveToNext()) {
                    notes++;
                    int snIdx = cursor.getColumnIndex("sn");
                    int fromIdx = cursor.getColumnIndex("from");
                    int messageIdx = cursor.getColumnIndex("message");
                    int ringidIdx = cursor.getColumnIndex("ringid");
                    if (snIdx != -1 &&
                            fromIdx != -1 &&
                            messageIdx != -1 &&
                            ringidIdx != -1) {
                        int sn = cursor.getInt(snIdx);
                        String from = cursor.getString(fromIdx);
                        String message = cursor.getString(messageIdx);
                        int ringid = cursor.getInt(ringidIdx);

                        CashItem cItem = CashItemFactory.getItem(sn);
                        Item item = cItem.toItem();
                        Equip equip = null;
                        item.setGiftFrom(from);
                        if (item.getInventoryType().equals(InventoryType.EQUIP)) {
                            equip = (Equip) item;
                            equip.setRingId(ringid);
                            gifts.add(new Pair<>(equip, message));
                        } else {
                            gifts.add(new Pair<>(item, message));
                        }

                        if (CashItemFactory.isPackage(cItem.getItemId())) { //Packages never contains a ring
                            for (Item packageItem : CashItemFactory.getPackage(cItem.getItemId())) {
                                packageItem.setGiftFrom(from);
                                addToInventory(packageItem);
                            }
                        } else {
                            addToInventory(equip == null ? item : equip);
                        }
                    }
                }
            }

            String table = "gifts";
            String whereClause = "`to` = ?";
            String[] whereArgs = { String.valueOf(characterId) };
            con.delete(table, whereClause, whereArgs);
        } catch (SQLiteException sqle) {
            log.error("loadGifts error", sqle);
        }

        return gifts;
    }

    public int getAvailableNotes() {
        return notes;
    }

    public void decreaseNotes() {
        notes--;
    }

    public void save(SQLiteDatabase con) throws SQLiteException {
        ContentValues values = new ContentValues();
        values.put("nxCredit", nxCredit);
        values.put("maplePoint", maplePoint);
        values.put("nxPrepaid", nxPrepaid);

        String whereClause = "id = ?";
        String[] whereArgs = { String.valueOf(accountId) };
        con.update("accounts", values, whereClause, whereArgs);

        List<Pair<Item, InventoryType>> itemsWithType = new ArrayList<>();

        List<Item> inv = getInventory();
        for (Item item : inv) {
            itemsWithType.add(new Pair<>(item, item.getInventoryType()));
        }

        factory.saveItems(itemsWithType, accountId, con);

        String wishlistsWhereClause = "charid = ?";
        String[] wishlistsWhereArgs = { String.valueOf(characterId) };
        con.delete("wishlists", wishlistsWhereClause, wishlistsWhereArgs);

        for (int sn : wishList) {
            ContentValues wishlistsValues = new ContentValues();
            wishlistsValues.put("charid", characterId);
            wishlistsValues.put("sn", sn);
            con.insert("wishlists", null, wishlistsValues);
        }
    }

    private Item getCashShopItemByItemid(int itemid) {
        lock.lock();
        try {
            for (Item it : inventory) {
                if (it.getItemId() == itemid) {
                    return it;
                }
            }
        } finally {
            lock.unlock();
        }

        return null;
    }

    public synchronized Pair<Item, Item> openCashShopSurprise() {
        Item css = getCashShopItemByItemid(ItemId.CASH_SHOP_SURPRISE);

        if (css != null) {
            CashItem cItem = CashItemFactory.getRandomCashItem();

            if (cItem != null) {
                if (css.getQuantity() > 1) {
                    /* if(NOT ENOUGH SPACE) { looks like we're not dealing with cash inventory limit whatsoever, k then
                        return null;
                    } */

                    css.setQuantity((short) (css.getQuantity() - 1));
                } else {
                    removeFromInventory(css);
                }

                Item item = cItem.toItem();
                addToInventory(item);

                return new Pair<>(item, css);
            } else {
                return null;
            }
        } else {
            return null;
        }
    }

    public static Item generateCouponItem(int itemId, short quantity) {
        CashItem it = new CashItem(77777777, itemId, 7777, ItemConstants.isPet(itemId) ? 30 : 0, quantity, true);
        return it.toItem();
    }
}
