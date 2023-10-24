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
package net.server.channel.handlers;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import client.Character;
import client.Client;
import client.inventory.Item;
import client.inventory.manipulator.InventoryManipulator;
import net.AbstractPacketHandler;
import net.packet.InPacket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import server.CashShop;
import server.ItemInformationProvider;
import tools.DatabaseConnection;
import tools.PacketCreator;
import tools.Pair;
import java.util.*;
import java.util.Map.Entry;

/**
 * @author Penguins (Acrylic)
 * @author Ronan (HeavenMS)
 */
public final class CouponCodeHandler extends AbstractPacketHandler {
    private static final Logger log = LoggerFactory.getLogger(CouponCodeHandler.class);

    private static List<Pair<Integer, Pair<Integer, Integer>>> getNXCodeItems(Character chr, SQLiteDatabase con, int codeid) throws SQLiteException {
        Map<Integer, Integer> couponItems = new HashMap<>();
        Map<Integer, Integer> couponPoints = new HashMap<>(5);

        try (Cursor cursor = con.rawQuery("SELECT * FROM nxcode_items WHERE codeid = ?", new String[]{String.valueOf(codeid)})) {
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    int typeIdx = cursor.getColumnIndex("type");
                    int quantityIdx = cursor.getColumnIndex("quantity");
                    if (typeIdx != -1 && quantityIdx != -1) {
                        int type = cursor.getInt(typeIdx);
                        int quantity = cursor.getInt(quantityIdx);

                        if (type < 5) {
                            Integer i = couponPoints.get(type);
                            if (i != null) {
                                couponPoints.put(type, i + quantity);
                            } else {
                                couponPoints.put(type, quantity);
                            }
                        } else {
                            int itemIdx = cursor.getColumnIndex("item");
                            if (itemIdx != -1) {
                                int item = cursor.getInt(itemIdx);

                                Integer i = couponItems.get(item);
                                if (i != null) {
                                    couponItems.put(item, i + quantity);
                                } else {
                                    couponItems.put(item, quantity);
                                }
                            }
                        }
                    }
                }
            }
        }

        List<Pair<Integer, Pair<Integer, Integer>>> ret = new LinkedList<>();
        if (!couponItems.isEmpty()) {
            for (Entry<Integer, Integer> e : couponItems.entrySet()) {
                int item = e.getKey(), qty = e.getValue();

                if (ItemInformationProvider.getInstance().getName(item) == null) {
                    item = 4000000;
                    qty = 1;

                    log.warn("Error trying to redeem itemid {} from coupon codeid {}", item, codeid);
                }

                if (!chr.canHold(item, qty)) {
                    return null;
                }

                ret.add(new Pair<>(5, new Pair<>(item, qty)));
            }
        }

        if (!couponPoints.isEmpty()) {
            for (Entry<Integer, Integer> e : couponPoints.entrySet()) {
                ret.add(new Pair<>(e.getKey(), new Pair<>(777, e.getValue())));
            }
        }

        return ret;
    }

    private static Pair<Integer, List<Pair<Integer, Pair<Integer, Integer>>>> getNXCodeResult(Character chr, String code) {
        Client c = chr.getClient();
        List<Pair<Integer, Pair<Integer, Integer>>> ret = new LinkedList<>();
        try {
            if (!c.attemptCsCoupon()) {
                return new Pair<>(-5, null);
            }

            try (SQLiteDatabase con = DatabaseConnection.getConnection()) {
                try (Cursor cursor = con.rawQuery("SELECT * FROM nxcode WHERE code = ?", new String[]{code})) {
                    if (cursor.moveToNext()) {
                        int idIdx = cursor.getColumnIndex("id");
                        int retrieverIdx = cursor.getColumnIndex("retriever");
                        int expirationIdx = cursor.getColumnIndex("expiration");

                        int codeId = cursor.getInt(idIdx);
                        String retriever = cursor.getString(retrieverIdx);
                        long expiration = cursor.getLong(expirationIdx);

                        if (retriever != null) {
                            return new Pair<>(-2, null);
                        }

                        if (expiration < System.currentTimeMillis()) {
                            return new Pair<>(-3, null);
                        }

                        ret = getNXCodeItems(chr, con, codeId);
                        if (ret == null) {
                            return new Pair<>(-4, null);
                        }
                    } else {
                        return new Pair<>(-1, null);
                    }
                }

                ContentValues values = new ContentValues();
                values.put("retriever", chr.getName());
                con.update("nxcode", values, "code = ?", new String[]{code});
            }
        } catch (SQLiteException ex) {
            ex.printStackTrace();
        }

        c.resetCsCoupon();
        return new Pair<>(0, ret);
    }

    private static int parseCouponResult(int res) {
        switch (res) {
            case -1:
                return 0xB0;

            case -2:
                return 0xB3;

            case -3:
                return 0xB2;

            case -4:
                return 0xBB;

            default:
                return 0xB1;
        }
    }

    @Override
    public final void handlePacket(InPacket p, Client c) {
        p.skip(2);
        String code = p.readString();

        if (c.tryacquireClient()) {
            try {
                Pair<Integer, List<Pair<Integer, Pair<Integer, Integer>>>> codeRes = getNXCodeResult(c.getPlayer(), code.toUpperCase());
                int type = codeRes.getLeft();
                if (type < 0) {
                    c.sendPacket(PacketCreator.showCashShopMessage((byte) parseCouponResult(type)));
                } else {
                    List<Item> cashItems = new LinkedList<>();
                    List<Pair<Integer, Integer>> items = new LinkedList<>();
                    int nxCredit = 0;
                    int maplePoints = 0;
                    int nxPrepaid = 0;
                    int mesos = 0;

                    for (Pair<Integer, Pair<Integer, Integer>> pair : codeRes.getRight()) {
                        type = pair.getLeft();
                        int quantity = pair.getRight().getRight();

                        CashShop cs = c.getPlayer().getCashShop();
                        switch (type) {
                            case 0:
                                c.getPlayer().gainMeso(quantity, false); //mesos
                                mesos += quantity;
                                break;
                            case 4:
                                cs.gainCash(1, quantity);    //nxCredit
                                nxCredit += quantity;
                                break;
                            case 1:
                                cs.gainCash(2, quantity);    //maplePoint
                                maplePoints += quantity;
                                break;
                            case 2:
                                cs.gainCash(4, quantity);    //nxPrepaid
                                nxPrepaid += quantity;
                                break;
                            case 3:
                                cs.gainCash(1, quantity);
                                nxCredit += quantity;
                                cs.gainCash(4, (quantity / 5000));
                                nxPrepaid += quantity / 5000;
                                break;

                            default:
                                int item = pair.getRight().getLeft();

                                short qty;
                                if (quantity > Short.MAX_VALUE) {
                                    qty = Short.MAX_VALUE;
                                } else if (quantity < Short.MIN_VALUE) {
                                    qty = Short.MIN_VALUE;
                                } else {
                                    qty = (short) quantity;
                                }

                                if (ItemInformationProvider.getInstance().isCash(item)) {
                                    Item it = CashShop.generateCouponItem(item, qty);

                                    cs.addToInventory(it);
                                    cashItems.add(it);
                                } else {
                                    InventoryManipulator.addById(c, item, qty, "", -1);
                                    items.add(new Pair<>((int) qty, item));
                                }
                                break;
                        }
                    }
                    if (cashItems.size() > 255) {
                        List<Item> oldList = cashItems;
                        cashItems = Arrays.asList(new Item[255]);
                        int index = 0;
                        for (Item item : oldList) {
                            cashItems.set(index, item);
                            index++;
                        }
                    }
                    if (nxCredit != 0 || nxPrepaid != 0) { //coupon packet can only show maple points (afaik)
                        c.sendPacket(PacketCreator.showBoughtQuestItem(0));
                    } else {
                        c.sendPacket(PacketCreator.showCouponRedeemedItems(c.getAccID(), maplePoints, mesos, cashItems, items));
                    }
                    c.enableCSActions();
                }
            } finally {
                c.releaseClient();
            }
        }
    }
}
