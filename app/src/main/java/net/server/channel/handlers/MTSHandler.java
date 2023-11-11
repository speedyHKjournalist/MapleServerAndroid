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
import client.inventory.Equip;
import client.inventory.InventoryType;
import client.inventory.Item;
import client.inventory.manipulator.InventoryManipulator;
import constants.inventory.ItemConstants;
import net.AbstractPacketHandler;
import net.packet.InPacket;
import net.packet.Packet;
import net.server.Server;
import net.server.channel.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import server.ItemInformationProvider;
import server.MTSItemInfo;
import tools.DatabaseConnection;
import tools.PacketCreator;
import tools.Pair;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public final class MTSHandler extends AbstractPacketHandler {
    private static final Logger log = LoggerFactory.getLogger(MTSHandler.class);

    @Override
    public void handlePacket(InPacket p, Client c) {
        // TODO add karma-to-untradeable flag on sold items here

        if (!c.getPlayer().getCashShop().isOpened()) {
            return;
        }
        if (p.available() > 0) {
            byte op = p.readByte();
            switch (op) {
            case 2: { //put item up for sale
                byte itemtype = p.readByte();
                int itemid = p.readInt();
                p.readShort();
                p.skip(7);
                short stars = 1;
                if (itemtype == 1) {
                    p.skip(32);
                } else {
                    stars = p.readShort();
                }
                p.readString(); // another useless thing (owner)
                if (itemtype == 1) {
                    p.skip(32);
                } else {
                    p.readShort();
                }
                short slot;
                short quantity;
                if (itemtype != 1) {
                    if (itemid / 10000 == 207 || itemid / 10000 == 233) {
                        p.skip(8);
                    }
                    slot = (short) p.readInt();
                } else {
                    slot = (short) p.readInt();
                }
                if (itemtype != 1) {
                    if (itemid / 10000 == 207 || itemid / 10000 == 233) {
                        quantity = stars;
                        p.skip(4);
                    } else {
                        quantity = (short) p.readInt();
                    }
                } else {
                    quantity = (byte) p.readInt();
                }
                int price = p.readInt();
                if (itemtype == 1) {
                    quantity = 1;
                }
                if (quantity < 0 || price < 110 || c.getPlayer().getItemQuantity(itemid, false) < quantity) {
                    return;
                }
                InventoryType invType = ItemConstants.getInventoryType(itemid);
                Item i = c.getPlayer().getInventory(invType).getItem(slot).copy();
                if (i != null && c.getPlayer().getMeso() >= 5000) {
                    try (SQLiteDatabase con = DatabaseConnection.getConnection();
                         Cursor cursor = con.rawQuery("SELECT COUNT(*) FROM mts_items WHERE seller = ?",
                                 new String[]{String.valueOf(c.getPlayer().getId())})) {
                        if (cursor.moveToFirst()) {
                            int itemCount = cursor.getInt(0);
                            if (itemCount > 10) {
                                // They have more than 10 items up for sale already!
                                c.getPlayer().dropMessage(1, "You already have 10 items up for auction!");
                                c.sendPacket(getMTS(1, 0, 0));
                                c.sendPacket(PacketCreator.transferInventory(getTransfer(c.getPlayer().getId())));
                                c.sendPacket(PacketCreator.notYetSoldInv(getNotYetSold(c.getPlayer().getId())));
                                return;
                            }
                        }
                        LocalDate now = LocalDate.now();
                        LocalDate sellEnd = now.plusDays(7);
                        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
                        String date = sellEnd.format(formatter);

                        if (!i.getInventoryType().equals(InventoryType.EQUIP)) {
                            Item item = i;
                            ContentValues values = new ContentValues();
                            values.put("tab", 1);
                            values.put("type", invType.getType());
                            values.put("itemid", item.getItemId());
                            values.put("quantity", quantity);
                            values.put("expiration", item.getExpiration());
                            values.put("giftFrom", item.getGiftFrom());
                            values.put("seller", c.getPlayer().getId());
                            values.put("price", price);
                            values.put("owner", item.getOwner());
                            values.put("sellername", c.getPlayer().getName());
                            values.put("sell_ends", date);
                            con.insert("mts_items", null, values);
                        } else {
                            Equip equip = (Equip) i;
                            ContentValues values = new ContentValues();

                            values.put("tab", 1);
                            values.put("type", invType.getType());
                            values.put("itemid", equip.getItemId());
                            values.put("quantity", quantity);
                            values.put("expiration", equip.getExpiration());
                            values.put("giftFrom", equip.getGiftFrom());
                            values.put("seller", c.getPlayer().getId());
                            values.put("price", price);
                            values.put("upgradeslots", equip.getUpgradeSlots());
                            values.put("level", equip.getLevel());
                            values.put("str", equip.getStr());
                            values.put("dex", equip.getDex());
                            values.put("int", equip.getInt());
                            values.put("luk", equip.getLuk());
                            values.put("hp", equip.getHp());
                            values.put("mp", equip.getMp());
                            values.put("watk", equip.getWatk());
                            values.put("matk", equip.getMatk());
                            values.put("wdef", equip.getWdef());
                            values.put("mdef", equip.getMdef());
                            values.put("acc", equip.getAcc());
                            values.put("avoid", equip.getAvoid());
                            values.put("hands", equip.getHands());
                            values.put("speed", equip.getSpeed());
                            values.put("jump", equip.getJump());
                            values.put("locked", 0);
                            values.put("owner", equip.getOwner());
                            values.put("sellername", c.getPlayer().getName());
                            values.put("sell_ends", date);
                            values.put("vicious", equip.getVicious());
                            values.put("flag", equip.getFlag());
                            values.put("itemexp", equip.getItemExp());
                            values.put("itemlevel", equip.getItemLevel());
                            values.put("ringid", equip.getRingId());
                            con.insert("mts_items", null, values);
                        }
                        InventoryManipulator.removeFromSlot(c, invType, slot, quantity, false);
                    } catch (SQLiteException e) {
                        e.printStackTrace();
                    }
                    c.getPlayer().gainMeso(-5000, false);
                    c.sendPacket(PacketCreator.MTSConfirmSell());
                    c.sendPacket(getMTS(1, 0, 0));
                    c.enableCSActions();
                    c.sendPacket(PacketCreator.transferInventory(getTransfer(c.getPlayer().getId())));
                    c.sendPacket(PacketCreator.notYetSoldInv(getNotYetSold(c.getPlayer().getId())));
                }
                break;
            }
            case 3: //send offer for wanted item
                break;
            case 4: //list wanted item
                p.readInt();
                p.readInt();
                p.readInt();
                p.readShort();
                p.readString();
                break;
            case 5: { //change page
                int tab = p.readInt();
                int type = p.readInt();
                int page = p.readInt();
                c.getPlayer().changePage(page);
                if (tab == 4 && type == 0) {
                    c.sendPacket(getCart(c.getPlayer().getId()));
                } else if (tab == c.getPlayer().getCurrentTab() && type == c.getPlayer().getCurrentType() && c.getPlayer().getSearch() != null) {
                    c.sendPacket(getMTSSearch(tab, type, c.getPlayer().getCurrentCI(), c.getPlayer().getSearch(), page));
                } else {
                    c.getPlayer().setSearch(null);
                    c.sendPacket(getMTS(tab, type, page));
                }
                c.getPlayer().changeTab(tab);
                c.getPlayer().changeType(type);
                c.enableCSActions();
                c.sendPacket(PacketCreator.transferInventory(getTransfer(c.getPlayer().getId())));
                c.sendPacket(PacketCreator.notYetSoldInv(getNotYetSold(c.getPlayer().getId())));
                break;
            }
            case 6: { //search
                int tab = p.readInt();
                int type = p.readInt();
                p.readInt();
                int ci = p.readInt();
                String search = p.readString();
                c.getPlayer().setSearch(search);
                c.getPlayer().changeTab(tab);
                c.getPlayer().changeType(type);
                c.getPlayer().changeCI(ci);
                c.enableCSActions();
                c.sendPacket(PacketCreator.enableActions());
                c.sendPacket(getMTSSearch(tab, type, ci, search, c.getPlayer().getCurrentPage()));
                c.sendPacket(PacketCreator.showMTSCash(c.getPlayer()));
                c.sendPacket(PacketCreator.transferInventory(getTransfer(c.getPlayer().getId())));
                c.sendPacket(PacketCreator.notYetSoldInv(getNotYetSold(c.getPlayer().getId())));
                break;
            }
            case 7: { //cancel sale
                int id = p.readInt(); // id of the item
                try (SQLiteDatabase con = DatabaseConnection.getConnection()) {
                    ContentValues updateValues = new ContentValues();
                    updateValues.put("transfer", 1);
                    String updateWhere = "id = ? AND seller = ?";
                    String[] updateArgs = { String.valueOf(id), String.valueOf(c.getPlayer().getId()) };
                    con.update("mts_items", updateValues, updateWhere, updateArgs);

                    String deleteWhere = "itemid = ?";
                    String[] deleteArgs = { String.valueOf(id) };
                    con.delete("mts_cart", deleteWhere, deleteArgs);
                } catch (SQLiteException e) {
                    e.printStackTrace();
                }
                c.enableCSActions();
                c.sendPacket(getMTS(c.getPlayer().getCurrentTab(), c.getPlayer().getCurrentType(),
                        c.getPlayer().getCurrentPage()));
                c.sendPacket(PacketCreator.notYetSoldInv(getNotYetSold(c.getPlayer().getId())));
                c.sendPacket(PacketCreator.transferInventory(getTransfer(c.getPlayer().getId())));
                break;
            }
            case 8: { // transfer item from transfer inv.
                int id = p.readInt(); // id of the item
                try (SQLiteDatabase con = DatabaseConnection.getConnection()) {
                    try (Cursor cursor = con.rawQuery("SELECT * FROM mts_items WHERE seller = ? AND transfer = 1  AND id= ? ORDER BY id DESC", new String[]{ String.valueOf(c.getPlayer().getId()), String.valueOf(id) })) {
                        if (cursor.moveToNext()) {
                            Item i;
                            int typeIdx = cursor.getColumnIndex("type");
                            if (cursor.getInt(typeIdx) != 1) {
                                int itemidIdx = cursor.getColumnIndex("itemid");
                                int quantityIdx = cursor.getColumnIndex("quantity");
                                Item ii = new Item(cursor.getInt(itemidIdx), (short) 0, (short) cursor.getInt(quantityIdx));
                                int ownerIdx = cursor.getColumnIndex("owner");
                                ii.setOwner(cursor.getString(ownerIdx));
                                ii.setPosition(
                                        c.getPlayer().getInventory(ItemConstants.getInventoryType(cursor.getInt(itemidIdx)))
                                                .getNextFreeSlot());
                                i = ii.copy();
                            } else {
                                int itemidIdx = cursor.getColumnIndex("itemid");
                                int positionIdx = cursor.getColumnIndex("position");
                                int ownerIdx = cursor.getColumnIndex("owner");
                                int accIdx = cursor.getColumnIndex("acc");
                                int avoidIdx = cursor.getColumnIndex("avoid");
                                int dexIdx = cursor.getColumnIndex("dex");
                                int handsIdx = cursor.getColumnIndex("hands");
                                int hpIdx = cursor.getColumnIndex("hp");
                                int intIdx = cursor.getColumnIndex("int");
                                int jumpIdx = cursor.getColumnIndex("jump");
                                int lukIdx = cursor.getColumnIndex("luk");
                                int matkIdx = cursor.getColumnIndex("matk");
                                int mdefIdx = cursor.getColumnIndex("mdef");
                                int mpIdx = cursor.getColumnIndex("mp");
                                int speedIdx = cursor.getColumnIndex("speed");
                                int strIdx = cursor.getColumnIndex("str");
                                int watkIdx = cursor.getColumnIndex("watk");
                                int wdefIdx = cursor.getColumnIndex("wdef");
                                int upgradeslotsIdx = cursor.getColumnIndex("upgradeslots");
                                int levelIdx = cursor.getColumnIndex("level");
                                int itemlevelIdx = cursor.getColumnIndex("itemlevel");
                                int itemexpIdx = cursor.getColumnIndex("itemexp");
                                int ringidIdx = cursor.getColumnIndex("ringid");
                                int viciousIdx = cursor.getColumnIndex("vicious");
                                int flagIdx = cursor.getColumnIndex("flag");
                                int expirationIdx = cursor.getColumnIndex("expiration");
                                int giftFromIdx = cursor.getColumnIndex("giftFrom");

                                Equip equip = new Equip(cursor.getInt(itemidIdx), (byte) cursor.getInt(positionIdx), -1);
                                equip.setOwner(cursor.getString(ownerIdx));
                                equip.setQuantity((short) 1);
                                equip.setAcc((short) cursor.getInt(accIdx));
                                equip.setAvoid((short) cursor.getInt(avoidIdx));
                                equip.setDex((short) cursor.getInt(dexIdx));
                                equip.setHands((short) cursor.getInt(handsIdx));
                                equip.setHp((short) cursor.getInt(hpIdx));
                                equip.setInt((short) cursor.getInt(intIdx));
                                equip.setJump((short) cursor.getInt(jumpIdx));
                                equip.setLuk((short) cursor.getInt(lukIdx));
                                equip.setMatk((short) cursor.getInt(matkIdx));
                                equip.setMdef((short) cursor.getInt(mdefIdx));
                                equip.setMp((short) cursor.getInt(mpIdx));
                                equip.setSpeed((short) cursor.getInt(speedIdx));
                                equip.setStr((short) cursor.getInt(strIdx));
                                equip.setWatk((short) cursor.getInt(watkIdx));
                                equip.setWdef((short) cursor.getInt(wdefIdx));
                                equip.setUpgradeSlots((byte) cursor.getInt(upgradeslotsIdx));
                                equip.setLevel((byte) cursor.getInt(levelIdx));
                                equip.setItemLevel((byte)cursor.getInt(itemlevelIdx));
                                equip.setItemExp(cursor.getInt(itemexpIdx));
                                equip.setRingId(cursor.getInt(ringidIdx));
                                equip.setVicious((byte) cursor.getInt(viciousIdx));
                                equip.setFlag((short) cursor.getInt(flagIdx));
                                equip.setExpiration(cursor.getLong(expirationIdx));
                                equip.setGiftFrom(cursor.getString(giftFromIdx));
                                equip.setPosition(
                                        c.getPlayer().getInventory(ItemConstants.getInventoryType(cursor.getInt(itemidIdx)))
                                                .getNextFreeSlot());
                                i = equip.copy();
                            }
                            String deleteWhere = "id = ? AND seller = ? AND transfer = 1";
                            String[] deleteArgs = { String.valueOf(id), String.valueOf(c.getPlayer().getId()) };
                            con.delete("mts_items", deleteWhere, deleteArgs);

                            InventoryManipulator.addFromDrop(c, i, false);
                            c.enableCSActions();
                            c.sendPacket(getCart(c.getPlayer().getId()));
                            c.sendPacket(getMTS(c.getPlayer().getCurrentTab(), c.getPlayer().getCurrentType(),
                                    c.getPlayer().getCurrentPage()));
                            c.sendPacket(PacketCreator.MTSConfirmTransfer(i.getQuantity(), i.getPosition()));
                            c.sendPacket(PacketCreator.transferInventory(getTransfer(c.getPlayer().getId())));
                        }
                    }
                } catch (SQLiteException e) {
                    log.error("MTS Transfer error", e);
                }
                break;
            }
            case 9: { //add to cart
                int id = p.readInt(); // id of the item
                try (SQLiteDatabase con = DatabaseConnection.getConnection()) {
                    try (Cursor cursor = con.rawQuery("SELECT id FROM mts_items WHERE id = ? AND seller <> ?",
                            new String[]{ String.valueOf(id), String.valueOf(c.getPlayer().getId()) })) {// Dummy query, prevents adding to cart self owned items
                        if (cursor.moveToNext()) {
                            try (Cursor cartCursor = con.rawQuery("SELECT cid FROM mts_cart WHERE cid = ? AND itemid = ?",
                                    new String[]{ String.valueOf(c.getPlayer().getId()), String.valueOf(id) })) {
                                if (!cartCursor.moveToNext()) {
                                    ContentValues values = new ContentValues();
                                    values.put("cid", c.getPlayer().getId());
                                    values.put("itemid", id);
                                    con.insert("mts_cart", null, values);
                                }
                            }
                        }
                    }
                } catch (SQLiteException e) {
                    e.printStackTrace();
                }
                c.sendPacket(getMTS(c.getPlayer().getCurrentTab(), c.getPlayer().getCurrentType(), c.getPlayer().getCurrentPage()));
                c.enableCSActions();
                c.sendPacket(PacketCreator.enableActions());
                c.sendPacket(PacketCreator.transferInventory(getTransfer(c.getPlayer().getId())));
                c.sendPacket(PacketCreator.notYetSoldInv(getNotYetSold(c.getPlayer().getId())));
                break;
            }
            case 10: { //delete from cart
                int id = p.readInt(); // id of the item
                try (SQLiteDatabase con = DatabaseConnection.getConnection()) {
                    con.execSQL("DELETE FROM mts_cart WHERE itemid = ? AND cid = ?", new String[]{ String.valueOf(id), String.valueOf(c.getPlayer().getId()) });
                } catch (SQLiteException e) {
                    e.printStackTrace();
                }
                c.sendPacket(getCart(c.getPlayer().getId()));
                c.enableCSActions();
                c.sendPacket(PacketCreator.transferInventory(getTransfer(c.getPlayer().getId())));
                c.sendPacket(PacketCreator.notYetSoldInv(getNotYetSold(c.getPlayer().getId())));
                break;
            }
            case 12: //put item up for auction
                break;
            case 13: //cancel wanted cart thing
                break;
            case 14: //buy auction item now
                break;
            case 16: { //buy
                int id = p.readInt(); // id of the item
                try (SQLiteDatabase con = DatabaseConnection.getConnection();
                     Cursor cursor = con.rawQuery("SELECT * FROM mts_items WHERE id = ? ORDER BY id DESC", new String[]{ String.valueOf(id) })) {
                    if (cursor.moveToFirst()) {
                        int priceIdx = cursor.getColumnIndex("price");
                        int price = cursor.getInt(priceIdx) + 100 + (int) (cursor.getInt(priceIdx) * 0.1); // taxes
                        if (c.getPlayer().getCashShop().getCash(4) >= price) { // FIX
                            boolean alwaysnull = true;
                            int sellerIdx = cursor.getColumnIndex("seller");
                            for (Channel cserv : Server.getInstance().getAllChannels()) {
                                Character victim = cserv.getPlayerStorage().getCharacterById(cursor.getInt(sellerIdx));
                                if (victim != null) {
                                    victim.getCashShop().gainCash(4, cursor.getInt(priceIdx));
                                    alwaysnull = false;
                                }
                            }
                            if (alwaysnull) {
                                try (Cursor rs = con.rawQuery("SELECT accountid FROM characters WHERE id = ?", new String[]{String.valueOf(cursor.getInt(sellerIdx))})) {
                                    if (rs.moveToFirst()) {
                                        int accountidIdx = rs.getColumnIndex("accountid");
                                        con.execSQL("UPDATE accounts SET nxPrepaid = nxPrepaid + ? WHERE id = ?",
                                                new String[]{ String.valueOf(cursor.getInt(priceIdx)), String.valueOf(rs.getInt(accountidIdx)) });
                                        }
                                    }
                                }
                            }
                            con.execSQL("UPDATE mts_items SET seller = ?, transfer = 1 WHERE id = ?",
                                    new String[]{ String.valueOf(c.getPlayer().getId()), String.valueOf(id) });
                            con.execSQL("DELETE FROM mts_cart WHERE itemid = ?", new String[]{ String.valueOf(id) });

                            c.getPlayer().getCashShop().gainCash(4, -price);
                            c.enableCSActions();
                            c.sendPacket(getMTS(c.getPlayer().getCurrentTab(), c.getPlayer().getCurrentType(),c.getPlayer().getCurrentPage()));
                            c.sendPacket(PacketCreator.MTSConfirmBuy());
                            c.sendPacket(PacketCreator.showMTSCash(c.getPlayer()));
                            c.sendPacket(PacketCreator.transferInventory(getTransfer(c.getPlayer().getId())));
                            c.sendPacket(PacketCreator.notYetSoldInv(getNotYetSold(c.getPlayer().getId())));
                            c.sendPacket(PacketCreator.enableActions());
                        } else {
                            c.sendPacket(PacketCreator.MTSFailBuy());
                        }
                } catch (SQLiteException e) {
                    e.printStackTrace();
                    c.sendPacket(PacketCreator.MTSFailBuy());
                }
                break;
            }
            case 17: { //buy from cart
                int id = p.readInt(); // id of the item
                try (SQLiteDatabase con = DatabaseConnection.getConnection();
                        Cursor cursor = con.rawQuery("SELECT * FROM mts_items WHERE id = ? ORDER BY id DESC",
                                new String[]{ String.valueOf(id) })) {
                    if (cursor.moveToFirst()) {
                        int priceIdx = cursor.getColumnIndex("price");
                        int sellerIdx = cursor.getColumnIndex("seller");

                        int price = cursor.getInt(priceIdx) + 100 + (int) (cursor.getInt(priceIdx) * 0.1);
                        if (c.getPlayer().getCashShop().getCash(4) >= price) {
                            for (Channel cserv : Server.getInstance().getAllChannels()) {
                                Character victim = cserv.getPlayerStorage().getCharacterById(cursor.getInt(sellerIdx));
                                if (victim != null) {
                                    victim.getCashShop().gainCash(4, cursor.getInt(priceIdx));
                                } else {
                                    try (Cursor accountCursor = con.rawQuery("SELECT accountid FROM characters WHERE id = ?",
                                            new String[]{ String.valueOf(cursor.getInt(sellerIdx)) })) {
                                        if (accountCursor.moveToFirst()) {
                                            int accountidIdx = accountCursor.getColumnIndex("accountid");
                                            con.execSQL("UPDATE accounts SET nxPrepaid = nxPrepaid + ? WHERE id = ?",
                                                    new String[]{ String.valueOf(cursor.getInt(priceIdx)), String.valueOf(accountCursor.getInt(accountidIdx)) });
                                        }
                                    }
                                }
                            }
                            con.execSQL("UPDATE mts_items SET seller = ?, transfer = 1 WHERE id = ?",
                                    new String[]{ String.valueOf(c.getPlayer().getId()), String.valueOf(id) });
                            con.execSQL("DELETE FROM mts_cart WHERE itemid = ?", new String[]{ String.valueOf(id) });
                            c.getPlayer().getCashShop().gainCash(4, -price);
                            c.sendPacket(getCart(c.getPlayer().getId()));
                            c.enableCSActions();
                            c.sendPacket(PacketCreator.MTSConfirmBuy());
                            c.sendPacket(PacketCreator.showMTSCash(c.getPlayer()));
                            c.sendPacket(PacketCreator.transferInventory(getTransfer(c.getPlayer().getId())));
                            c.sendPacket(PacketCreator.notYetSoldInv(getNotYetSold(c.getPlayer().getId())));
                        } else {
                            c.sendPacket(PacketCreator.MTSFailBuy());
                        }
                    }
                } catch (SQLiteException e) {
                    e.printStackTrace();
                    c.sendPacket(PacketCreator.MTSFailBuy());
                }
                break;
            }
            default:
                log.warn("Unhandled OP (MTS): {}, packet: {}", op, p);
                break;
            }
        } else {
            c.sendPacket(PacketCreator.showMTSCash(c.getPlayer()));
        }
    }

    public List<MTSItemInfo> getNotYetSold(int cid) {
        List<MTSItemInfo> items = new ArrayList<>();
        try (SQLiteDatabase con = DatabaseConnection.getConnection();
             Cursor cursor = con.rawQuery("SELECT * FROM mts_items WHERE seller = ? AND transfer = 0 ORDER BY id DESC",
                     new String[]{ String.valueOf(cid) })) {
            while (cursor.moveToNext()) {
                int typeIdx = cursor.getColumnIndex("type");
                int itemidIdx = cursor.getColumnIndex("itemid");
                int quantityIdx = cursor.getColumnIndex("quantity");
                int ownerIdx = cursor.getColumnIndex("owner");

                if (cursor.getInt(typeIdx) != 1) {
                    Item i = new Item(cursor.getInt(itemidIdx), (byte) 0, (short) cursor.getInt(quantityIdx));
                    i.setOwner(cursor.getString(ownerIdx));
                    int priceIdx = cursor.getColumnIndex("price");
                    int idIdx = cursor.getColumnIndex("id");
                    int sellerIdx = cursor.getColumnIndex("seller");
                    int sellernameIdx = cursor.getColumnIndex("sellername");
                    int sell_endsIdx = cursor.getColumnIndex("sell_ends");

                    items.add(new MTSItemInfo(i, cursor.getInt(priceIdx), cursor.getInt(idIdx), cursor.getInt(sellerIdx), cursor.getString(sellernameIdx), cursor.getString(sell_endsIdx)));
                } else {
                    int positionIdx = cursor.getColumnIndex("position");
                    int accIdx = cursor.getColumnIndex("acc");
                    int avoidIdx = cursor.getColumnIndex("avoid");
                    int dexIdx = cursor.getColumnIndex("dex");
                    int handsIdx = cursor.getColumnIndex("hands");
                    int hpIdx = cursor.getColumnIndex("hp");
                    int intIdx = cursor.getColumnIndex("int");
                    int jumpIdx = cursor.getColumnIndex("jump");
                    int viciousIdx = cursor.getColumnIndex("vicious");
                    int lukIdx = cursor.getColumnIndex("luk");
                    int matkIdx = cursor.getColumnIndex("matk");
                    int mdefIdx = cursor.getColumnIndex("mdef");
                    int mpIdx = cursor.getColumnIndex("mp");
                    int speedIdx = cursor.getColumnIndex("speed");
                    int strIdx = cursor.getColumnIndex("str");
                    int watkIdx = cursor.getColumnIndex("watk");
                    int wdefIdx = cursor.getColumnIndex("wdef");
                    int upgradeslotsIdx = cursor.getColumnIndex("upgradeslots");
                    int levelIdx = cursor.getColumnIndex("level");
                    int flagIdx = cursor.getColumnIndex("flag");
                    int itemlevelIdx = cursor.getColumnIndex("itemlevel");
                    int itemexpIdx = cursor.getColumnIndex("itemexp");
                    int ringidIdx = cursor.getColumnIndex("ringid");
                    int expirationIdx = cursor.getColumnIndex("expiration");
                    int giftFromIdx = cursor.getColumnIndex("giftFrom");
                    int priceIdx = cursor.getColumnIndex("price");
                    int idIdx = cursor.getColumnIndex("id");
                    int sellerIdx = cursor.getColumnIndex("seller");
                    int sellernameIdx = cursor.getColumnIndex("sellername");
                    int sell_endsIdx = cursor.getColumnIndex("sell_ends");


                    Equip equip = new Equip(cursor.getInt(itemidIdx), (byte) cursor.getInt(positionIdx), -1);
                    equip.setOwner(cursor.getString(ownerIdx));
                    equip.setQuantity((short) 1);
                    equip.setAcc((short) cursor.getInt(accIdx));
                    equip.setAvoid((short) cursor.getInt(avoidIdx));
                    equip.setDex((short) cursor.getInt(dexIdx));
                    equip.setHands((short) cursor.getInt(handsIdx));
                    equip.setHp((short) cursor.getInt(hpIdx));
                    equip.setInt((short) cursor.getInt(intIdx));
                    equip.setJump((short) cursor.getInt(jumpIdx));
                    equip.setVicious((short) cursor.getInt(viciousIdx));
                    equip.setLuk((short) cursor.getInt(lukIdx));
                    equip.setMatk((short) cursor.getInt(matkIdx));
                    equip.setMdef((short) cursor.getInt(mdefIdx));
                    equip.setMp((short) cursor.getInt(mpIdx));
                    equip.setSpeed((short) cursor.getInt(speedIdx));
                    equip.setStr((short) cursor.getInt(strIdx));
                    equip.setWatk((short) cursor.getInt(watkIdx));
                    equip.setWdef((short) cursor.getInt(wdefIdx));
                    equip.setUpgradeSlots((byte) cursor.getInt(upgradeslotsIdx));
                    equip.setLevel((byte) cursor.getInt(levelIdx));
                    equip.setFlag((short) cursor.getInt(flagIdx));
                    equip.setItemLevel((byte) cursor.getInt(itemlevelIdx));
                    equip.setItemExp(cursor.getInt(itemexpIdx));
                    equip.setRingId(cursor.getInt(ringidIdx));
                    equip.setExpiration(cursor.getLong(expirationIdx));
                    equip.setGiftFrom(cursor.getString(giftFromIdx));
                    items.add(new MTSItemInfo(equip, cursor.getInt(priceIdx), cursor.getInt(idIdx), cursor.getInt(sellerIdx), cursor.getString(sellernameIdx), cursor.getString(sell_endsIdx)));
                }
            }
        } catch (SQLiteException e) {
            e.printStackTrace();
        }
        return items;
    }

    public Packet getCart(int cid) {
        List<MTSItemInfo> items = new ArrayList<>();
        int pages = 0;
        try (SQLiteDatabase con = DatabaseConnection.getConnection()) {
            try (Cursor cartCursor = con.rawQuery("SELECT * FROM mts_cart WHERE cid = ? ORDER BY id DESC", new String[]{ String.valueOf(cid) })) {
                while (cartCursor.moveToNext()) {
                    int itemidIdx = cartCursor.getColumnIndex("itemid");
                    try (Cursor itemCursor = con.rawQuery("SELECT * FROM mts_items WHERE id = ?",
                            new String[]{ String.valueOf(cartCursor.getInt(itemidIdx)) })) {
                        if (itemCursor.moveToFirst()) {
                            int typeIdx = cartCursor.getColumnIndex("type");
                            int quantityIdx = cartCursor.getColumnIndex("quantity");
                            int ownerIdx = cartCursor.getColumnIndex("owner");
                            int priceIdx = cartCursor.getColumnIndex("price");
                            int idIdx = cartCursor.getColumnIndex("id");
                            int sellerIdx = cartCursor.getColumnIndex("seller");
                            int sellernameIdx = cartCursor.getColumnIndex("sellername");
                            int sell_endsIdx = cartCursor.getColumnIndex("sell_ends");


                            if (itemCursor.getInt(typeIdx) != 1) {
                                Item i = new Item(itemCursor.getInt(itemidIdx), (short) 0, (short) itemCursor.getInt(quantityIdx));
                                i.setOwner(itemCursor.getString(ownerIdx));
                                items.add(new MTSItemInfo(i, itemCursor.getInt(priceIdx), itemCursor.getInt(idIdx),
                                        itemCursor.getInt(sellerIdx), itemCursor.getString(sellernameIdx), itemCursor.getString(sell_endsIdx)));
                            } else {
                                int positionIdx = itemCursor.getColumnIndex("position");
                                int accIdx = itemCursor.getColumnIndex("acc");
                                int avoidIdx = itemCursor.getColumnIndex("avoid");
                                int dexIdx = itemCursor.getColumnIndex("dex");
                                int handsIdx = itemCursor.getColumnIndex("hands");
                                int hpIdx = itemCursor.getColumnIndex("hp");
                                int intIdx = itemCursor.getColumnIndex("int");
                                int jumpIdx = itemCursor.getColumnIndex("jump");
                                int viciousIdx = itemCursor.getColumnIndex("vicious");
                                int lukIdx = itemCursor.getColumnIndex("luk");
                                int matkIdx = itemCursor.getColumnIndex("matk");
                                int mdefIdx = itemCursor.getColumnIndex("mdef");
                                int mpIdx = itemCursor.getColumnIndex("mp");
                                int speedIdx = itemCursor.getColumnIndex("speed");
                                int strIdx = itemCursor.getColumnIndex("str");
                                int watkIdx = itemCursor.getColumnIndex("watk");
                                int wdefIdx = itemCursor.getColumnIndex("wdef");
                                int upgradeslotsIdx = itemCursor.getColumnIndex("upgradeslots");
                                int levelIdx = itemCursor.getColumnIndex("level");
                                int flagIdx = itemCursor.getColumnIndex("flag");
                                int itemlevelIdx = itemCursor.getColumnIndex("itemlevel");
                                int itemexpIdx = itemCursor.getColumnIndex("itemexp");
                                int ringidIdx = itemCursor.getColumnIndex("ringid");
                                int expirationIdx = itemCursor.getColumnIndex("expiration");
                                int giftFromIdx = itemCursor.getColumnIndex("giftFrom");

                                Equip equip = new Equip(itemCursor.getInt(itemidIdx), (byte) itemCursor.getInt(positionIdx), -1);
                                equip.setOwner(itemCursor.getString(ownerIdx));
                                equip.setQuantity((short) 1);
                                equip.setAcc((short) itemCursor.getInt(accIdx));
                                equip.setAvoid((short) itemCursor.getInt(avoidIdx));
                                equip.setDex((short) itemCursor.getInt(dexIdx));
                                equip.setHands((short) itemCursor.getInt(handsIdx));
                                equip.setHp((short) itemCursor.getInt(hpIdx));
                                equip.setInt((short) itemCursor.getInt(intIdx));
                                equip.setJump((short) itemCursor.getInt(jumpIdx));
                                equip.setVicious((short) itemCursor.getInt(viciousIdx));
                                equip.setLuk((short) itemCursor.getInt(lukIdx));
                                equip.setMatk((short) itemCursor.getInt(matkIdx));
                                equip.setMdef((short) itemCursor.getInt(mdefIdx));
                                equip.setMp((short) itemCursor.getInt(mpIdx));
                                equip.setSpeed((short) itemCursor.getInt(speedIdx));
                                equip.setStr((short) itemCursor.getInt(strIdx));
                                equip.setWatk((short) itemCursor.getInt(watkIdx));
                                equip.setWdef((short) itemCursor.getInt(wdefIdx));
                                equip.setUpgradeSlots((byte) itemCursor.getInt(upgradeslotsIdx));
                                equip.setLevel((byte) itemCursor.getInt(levelIdx));
                                equip.setItemLevel((byte) itemCursor.getInt(itemlevelIdx));
                                equip.setItemExp(itemCursor.getInt(itemexpIdx));
                                equip.setRingId(itemCursor.getInt(ringidIdx));
                                equip.setFlag((short) itemCursor.getInt(flagIdx));
                                equip.setExpiration(itemCursor.getLong(expirationIdx));
                                equip.setGiftFrom(itemCursor.getString(giftFromIdx));
                                items.add(new MTSItemInfo(equip, itemCursor.getInt(priceIdx), itemCursor.getInt(idIdx),
                                        itemCursor.getInt(sellerIdx), itemCursor.getString(sellernameIdx), itemCursor.getString(sell_endsIdx)));
                            }
                        }
                    }
                }
            }
            try (Cursor countCursor = con.rawQuery("SELECT COUNT(*) FROM mts_cart WHERE cid = ?",
                    new String[]{ String.valueOf(cid) })) {
                if (countCursor.moveToFirst()) {
                    pages = countCursor.getInt(0) / 16;
                    if (countCursor.getInt(0) % 16 > 0) {
                        pages += 1;
                    }
                }
            }
        } catch (SQLiteException e) {
            e.printStackTrace();
        }
        return PacketCreator.sendMTS(items, 4, 0, 0, pages);
    }

    public List<MTSItemInfo> getTransfer(int cid) {
        List<MTSItemInfo> items = new ArrayList<>();
        try (SQLiteDatabase con = DatabaseConnection.getConnection();
             Cursor rs = con.rawQuery("SELECT * FROM mts_items WHERE transfer = 1 AND seller = ? ORDER BY id DESC", new String[]{ String.valueOf(cid) })) {
            while (rs.moveToNext()) {
                int typeColumn = rs.getColumnIndex("type");
                int itemIdColumn = rs.getColumnIndex("itemid");
                int positionColumn = rs.getColumnIndex("position");
                int quantityColumn = rs.getColumnIndex("quantity");
                int priceColumn = rs.getColumnIndex("price");
                int idColumn = rs.getColumnIndex("id");
                int sellerColumn = rs.getColumnIndex("seller");
                int sellerNameColumn = rs.getColumnIndex("sellername");
                int sellEndsColumn = rs.getColumnIndex("sell_ends");
                int ownerColumn = rs.getColumnIndex("owner");
                int accColumn = rs.getColumnIndex("acc");
                int avoidColumn = rs.getColumnIndex("avoid");
                int dexColumn = rs.getColumnIndex("dex");
                int handsColumn = rs.getColumnIndex("hands");
                int hpColumn = rs.getColumnIndex("hp");
                int intColumn = rs.getColumnIndex("int");
                int jumpColumn = rs.getColumnIndex("jump");
                int viciousColumn = rs.getColumnIndex("vicious");
                int lukColumn = rs.getColumnIndex("luk");
                int matkColumn = rs.getColumnIndex("matk");
                int mdefColumn = rs.getColumnIndex("mdef");
                int mpColumn = rs.getColumnIndex("mp");
                int speedColumn = rs.getColumnIndex("speed");
                int strColumn = rs.getColumnIndex("str");
                int watkColumn = rs.getColumnIndex("watk");
                int wdefColumn = rs.getColumnIndex("wdef");
                int upgradeSlotsColumn = rs.getColumnIndex("upgradeslots");
                int levelColumn = rs.getColumnIndex("level");
                int itemLevelColumn = rs.getColumnIndex("itemlevel");
                int itemExpColumn = rs.getColumnIndex("itemexp");
                int ringIdColumn = rs.getColumnIndex("ringid");
                int flagColumn = rs.getColumnIndex("flag");
                int expirationColumn = rs.getColumnIndex("expiration");
                int giftFromColumn = rs.getColumnIndex("giftFrom");

                if (rs.getInt(typeColumn) != 1) {
                    Item i = new Item(rs.getInt(itemIdColumn), (short) 0, (short) rs.getInt(quantityColumn));
                    i.setOwner(rs.getString(ownerColumn));
                    items.add(new MTSItemInfo(i, rs.getInt(priceColumn), rs.getInt(idColumn), rs.getInt(sellerColumn), rs.getString(sellerNameColumn), rs.getString(sellEndsColumn)));
                } else {
                    Equip equip = new Equip(rs.getInt(itemIdColumn), (byte) rs.getInt(positionColumn), -1);
                    equip.setOwner(rs.getString(ownerColumn));
                    equip.setQuantity((short) 1);
                    equip.setAcc((short) rs.getInt(accColumn));
                    equip.setAvoid((short) rs.getInt(avoidColumn));
                    equip.setDex((short) rs.getInt(dexColumn));
                    equip.setHands((short) rs.getInt(handsColumn));
                    equip.setHp((short) rs.getInt(hpColumn));
                    equip.setInt((short) rs.getInt(intColumn));
                    equip.setJump((short) rs.getInt(jumpColumn));
                    equip.setVicious((short) rs.getInt(viciousColumn));
                    equip.setLuk((short) rs.getInt(lukColumn));
                    equip.setMatk((short) rs.getInt(matkColumn));
                    equip.setMdef((short) rs.getInt(mdefColumn));
                    equip.setMp((short) rs.getInt(mpColumn));
                    equip.setSpeed((short) rs.getInt(speedColumn));
                    equip.setStr((short) rs.getInt(strColumn));
                    equip.setWatk((short) rs.getInt(watkColumn));
                    equip.setWdef((short) rs.getInt(wdefColumn));
                    equip.setUpgradeSlots((byte) rs.getInt(upgradeSlotsColumn));
                    equip.setLevel((byte) rs.getInt(levelColumn));
                    equip.setItemLevel((byte) rs.getInt(itemLevelColumn));
                    equip.setItemExp(rs.getInt(itemExpColumn));
                    equip.setRingId(rs.getInt(ringIdColumn));
                    equip.setFlag((short) rs.getInt(flagColumn));
                    equip.setExpiration(rs.getLong(expirationColumn));
                    equip.setGiftFrom(rs.getString(giftFromColumn));
                    items.add(new MTSItemInfo(equip, rs.getInt(priceColumn), rs.getInt(idColumn), rs.getInt(sellerColumn), rs.getString(sellerNameColumn), rs.getString(sellEndsColumn)));
                }
            }
        } catch (SQLiteException e) {
            e.printStackTrace();
        }
        return items;
    }

    private static Packet getMTS(int tab, int type, int page) {
        List<MTSItemInfo> items = new ArrayList<>();
        int pages = 0;
        try (SQLiteDatabase con = DatabaseConnection.getConnection()) {
            String sql;
            if (type != 0) {
                sql = "SELECT * FROM mts_items WHERE tab = ? AND type = ? AND transfer = 0 ORDER BY id DESC LIMIT ?, 16";
            } else {
                sql = "SELECT * FROM mts_items WHERE tab = ? AND transfer = 0 ORDER BY id DESC LIMIT ?, 16";
            }
            String[] selectionArgs;
            if (type != 0) {
                selectionArgs = new String[]{String.valueOf(tab), String.valueOf(type), String.valueOf(page * 16)};
            } else {
                selectionArgs = new String[]{String.valueOf(tab), String.valueOf(page * 16)};
            }

            try (Cursor cursor = con.rawQuery(sql, selectionArgs)) {
                while (cursor.moveToNext()) {
                    int typeColumn = cursor.getColumnIndex("type");
                    int itemIdColumn = cursor.getColumnIndex("itemid");
                    int quantityColumn = cursor.getColumnIndex("quantity");
                    int positionColumn = cursor.getColumnIndex("position");
                    int priceColumn = cursor.getColumnIndex("price");
                    int idColumn = cursor.getColumnIndex("id");
                    int sellerColumn = cursor.getColumnIndex("seller");
                    int sellerNameColumn = cursor.getColumnIndex("sellername");
                    int sellEndsColumn = cursor.getColumnIndex("sell_ends");
                    int ownerColumn = cursor.getColumnIndex("owner");
                    int accColumn = cursor.getColumnIndex("acc");
                    int avoidColumn = cursor.getColumnIndex("avoid");
                    int dexColumn = cursor.getColumnIndex("dex");
                    int handsColumn = cursor.getColumnIndex("hands");
                    int hpColumn = cursor.getColumnIndex("hp");
                    int intColumn = cursor.getColumnIndex("int");
                    int jumpColumn = cursor.getColumnIndex("jump");
                    int viciousColumn = cursor.getColumnIndex("vicious");
                    int lukColumn = cursor.getColumnIndex("luk");
                    int matkColumn = cursor.getColumnIndex("matk");
                    int mdefColumn = cursor.getColumnIndex("mdef");
                    int mpColumn = cursor.getColumnIndex("mp");
                    int speedColumn = cursor.getColumnIndex("speed");
                    int strColumn = cursor.getColumnIndex("str");
                    int watkColumn = cursor.getColumnIndex("watk");
                    int wdefColumn = cursor.getColumnIndex("wdef");
                    int upgradeSlotsColumn = cursor.getColumnIndex("upgradeslots");
                    int levelColumn = cursor.getColumnIndex("level");
                    int itemLevelColumn = cursor.getColumnIndex("itemlevel");
                    int itemExpColumn = cursor.getColumnIndex("itemexp");
                    int ringIdColumn = cursor.getColumnIndex("ringid");
                    int flagColumn = cursor.getColumnIndex("flag");
                    int expirationColumn = cursor.getColumnIndex("expiration");
                    int giftFromColumn = cursor.getColumnIndex("giftFrom");

                    if (cursor.getInt(typeColumn) != 1) {
                        Item i = new Item(cursor.getInt(itemIdColumn), (short) 0, (short) cursor.getInt(quantityColumn));
                        i.setOwner(cursor.getString(ownerColumn));
                        items.add(new MTSItemInfo(i, cursor.getInt(priceColumn), cursor.getInt(idColumn), cursor.getInt(sellerColumn),
                                cursor.getString(sellerNameColumn), cursor.getString(sellEndsColumn)));
                    } else {
                        Equip equip = new Equip(cursor.getInt(itemIdColumn), (byte) cursor.getInt(positionColumn), -1);
                        equip.setOwner(cursor.getString(ownerColumn));
                        equip.setQuantity((short) 1);
                        equip.setAcc((short) cursor.getInt(accColumn));
                        equip.setAvoid((short) cursor.getInt(avoidColumn));
                        equip.setDex((short) cursor.getInt(dexColumn));
                        equip.setHands((short) cursor.getInt(handsColumn));
                        equip.setHp((short) cursor.getInt(hpColumn));
                        equip.setInt((short) cursor.getInt(intColumn));
                        equip.setJump((short) cursor.getInt(jumpColumn));
                        equip.setVicious((short) cursor.getInt(viciousColumn));
                        equip.setLuk((short) cursor.getInt(lukColumn));
                        equip.setMatk((short) cursor.getInt(matkColumn));
                        equip.setMdef((short) cursor.getInt(mdefColumn));
                        equip.setMp((short) cursor.getInt(mpColumn));
                        equip.setSpeed((short) cursor.getInt(speedColumn));
                        equip.setStr((short) cursor.getInt(strColumn));
                        equip.setWatk((short) cursor.getInt(watkColumn));
                        equip.setWdef((short) cursor.getInt(wdefColumn));
                        equip.setUpgradeSlots((byte) cursor.getInt(upgradeSlotsColumn));
                        equip.setLevel((byte) cursor.getInt(levelColumn));
                        equip.setItemLevel((byte) cursor.getInt(itemLevelColumn));
                        equip.setItemExp(cursor.getInt(itemExpColumn));
                        equip.setRingId(cursor.getInt(ringIdColumn));
                        equip.setFlag((short) cursor.getInt(flagColumn));
                        equip.setExpiration(cursor.getLong(expirationColumn));
                        equip.setGiftFrom(cursor.getString(giftFromColumn));
                        items.add(new MTSItemInfo(equip, cursor.getInt(priceColumn), cursor.getInt(idColumn), cursor.getInt(sellerColumn), cursor.getString(sellerNameColumn), cursor.getString(sellEndsColumn)));
                    }
                }
            }

            String sqlmts = "SELECT COUNT(*) FROM mts_items WHERE tab = ?";
            if (type != 0) {
                sqlmts += " AND type = ?";
            }
            sqlmts += " AND transfer = 0";
            try (Cursor cursor = (type == 0) ? con.rawQuery(sqlmts, new String[]{String.valueOf(tab)}) :
                    con.rawQuery(sqlmts, new String[]{String.valueOf(tab), String.valueOf(type)})) {
                if (cursor.moveToNext()) {
                    pages = cursor.getInt(0) / 16;
                    if (cursor.getInt(0) % 16 > 0) {
                        pages++;
                    }
                }
            }
        } catch (SQLiteException e) {
            e.printStackTrace();
        }
        return PacketCreator.sendMTS(items, tab, type, page, pages); // resniff
    }

    public Packet getMTSSearch(int tab, int type, int cOi, String search, int page) {
        List<MTSItemInfo> items = new ArrayList<>();
        ItemInformationProvider ii = ItemInformationProvider.getInstance();
        String listaitems = "";
        if (cOi != 0) {
            List<String> retItems = new ArrayList<>();
            for (Pair<Integer, String> itemPair : ii.getAllItems()) {
                if (itemPair.getRight().toLowerCase().contains(search.toLowerCase())) {
                    retItems.add(" itemid=" + itemPair.getLeft() + " OR ");
                }
            }
            listaitems += " AND (";
            if (retItems != null && retItems.size() > 0) {
                for (String singleRetItem : retItems) {
                    listaitems += singleRetItem;
                }
                listaitems += " itemid=0 )";
            }
        } else {
            listaitems = " AND sellername LIKE CONCAT('%','" + search + "', '%')";
        }
        int pages = 0;
        String[] selectionArgs;
        try (SQLiteDatabase con = DatabaseConnection.getConnection()){
            String sql;
            if (type != 0) {
                sql = "SELECT * FROM mts_items WHERE tab = ? " + listaitems + " AND type = ? AND transfer = 0 ORDER BY id DESC LIMIT ?, 16";
                selectionArgs = new String[]{String.valueOf(tab), String.valueOf(type), String.valueOf(page * 16)};
            } else {
                sql = "SELECT * FROM mts_items WHERE tab = ? " + listaitems + " AND transfer = 0 ORDER BY id DESC LIMIT ?, 16";
                selectionArgs = new String[]{String.valueOf(tab), String.valueOf(page * 16)};
            }
            try (Cursor cursor = con.rawQuery(sql, selectionArgs)) {
                if (cursor != null) {
                    while (cursor.moveToNext()) {
                        int typeColumn = cursor.getColumnIndex("type");
                        int itemIdColumn = cursor.getColumnIndex("itemid");
                        int quantityColumn = cursor.getColumnIndex("quantity");
                        int positionColumn = cursor.getColumnIndex("position");
                        int priceColumn = cursor.getColumnIndex("price");
                        int idColumn = cursor.getColumnIndex("id");
                        int sellerColumn = cursor.getColumnIndex("seller");
                        int sellerNameColumn = cursor.getColumnIndex("sellername");
                        int sellEndsColumn = cursor.getColumnIndex("sell_ends");
                        int ownerColumn = cursor.getColumnIndex("owner");
                        int accColumn = cursor.getColumnIndex("acc");
                        int avoidColumn = cursor.getColumnIndex("avoid");
                        int dexColumn = cursor.getColumnIndex("dex");
                        int handsColumn = cursor.getColumnIndex("hands");
                        int hpColumn = cursor.getColumnIndex("hp");
                        int intColumn = cursor.getColumnIndex("int");
                        int jumpColumn = cursor.getColumnIndex("jump");
                        int viciousColumn = cursor.getColumnIndex("vicious");
                        int lukColumn = cursor.getColumnIndex("luk");
                        int matkColumn = cursor.getColumnIndex("matk");
                        int mdefColumn = cursor.getColumnIndex("mdef");
                        int mpColumn = cursor.getColumnIndex("mp");
                        int speedColumn = cursor.getColumnIndex("speed");
                        int strColumn = cursor.getColumnIndex("str");
                        int watkColumn = cursor.getColumnIndex("watk");
                        int wdefColumn = cursor.getColumnIndex("wdef");
                        int upgradeSlotsColumn = cursor.getColumnIndex("upgradeslots");
                        int levelColumn = cursor.getColumnIndex("level");
                        int itemLevelColumn = cursor.getColumnIndex("itemlevel");
                        int itemExpColumn = cursor.getColumnIndex("itemexp");
                        int ringIdColumn = cursor.getColumnIndex("ringid");
                        int flagColumn = cursor.getColumnIndex("flag");
                        int expirationColumn = cursor.getColumnIndex("expiration");
                        int giftFromColumn = cursor.getColumnIndex("giftFrom");

                        if (cursor.getInt(typeColumn) != 1) {
                            Item i = new Item(cursor.getInt(itemIdColumn), (short) 0, (short) cursor.getInt(quantityColumn));
                            i.setOwner(cursor.getString(ownerColumn));
                            items.add(new MTSItemInfo(i, cursor.getInt(priceColumn), cursor.getInt(idColumn), cursor.getInt(sellerColumn), cursor.getString(sellerColumn), cursor.getString(sellEndsColumn)));
                        } else {
                            Equip equip = new Equip(cursor.getInt(itemIdColumn), (byte) cursor.getInt(positionColumn), -1);
                            equip.setOwner(cursor.getString(ownerColumn));
                            equip.setQuantity((short) 1);
                            equip.setAcc((short) cursor.getInt(accColumn));
                            equip.setAvoid((short) cursor.getInt(avoidColumn));
                            equip.setDex((short) cursor.getInt(dexColumn));
                            equip.setHands((short) cursor.getInt(handsColumn));
                            equip.setHp((short) cursor.getInt(hpColumn));
                            equip.setInt((short) cursor.getInt(intColumn));
                            equip.setJump((short) cursor.getInt(jumpColumn));
                            equip.setVicious((short) cursor.getInt(viciousColumn));
                            equip.setLuk((short) cursor.getInt(lukColumn));
                            equip.setMatk((short) cursor.getInt(matkColumn));
                            equip.setMdef((short) cursor.getInt(mdefColumn));
                            equip.setMp((short) cursor.getInt(mpColumn));
                            equip.setSpeed((short) cursor.getInt(speedColumn));
                            equip.setStr((short) cursor.getInt(strColumn));
                            equip.setWatk((short) cursor.getInt(watkColumn));
                            equip.setWdef((short) cursor.getInt(wdefColumn));
                            equip.setUpgradeSlots((byte) cursor.getInt(upgradeSlotsColumn));
                            equip.setLevel((byte) cursor.getInt(levelColumn));
                            equip.setItemLevel((byte) cursor.getInt(itemLevelColumn));
                            equip.setItemExp(cursor.getInt(itemExpColumn));
                            equip.setRingId(cursor.getInt(ringIdColumn));
                            equip.setFlag((short) cursor.getInt(flagColumn));
                            equip.setExpiration(cursor.getLong(expirationColumn));
                            equip.setGiftFrom(cursor.getString(giftFromColumn));
                            items.add(new MTSItemInfo(equip, cursor.getInt(priceColumn), cursor.getInt(idColumn), cursor.getInt(sellerColumn), cursor.getString(sellerNameColumn), cursor.getString(sellEndsColumn)));
                        }
                    }
                }
            }
            if (type == 0) {
                String[] selectionArgsmts = new String[]{String.valueOf(tab)};
                try (Cursor cursor = con.rawQuery("SELECT COUNT(*) FROM mts_items WHERE tab = ? " + listaitems + " AND transfer = 0", selectionArgsmts)) {
                    if (cursor.moveToNext()) {
                        pages = cursor.getInt(0) / 16;
                        if (cursor.getInt(0) % 16 > 0) {
                            pages++;
                        }
                    }
                }
            }
        } catch (SQLiteException e) {
            e.printStackTrace();
        }
        return PacketCreator.sendMTS(items, tab, type, page, pages);
    }
}
