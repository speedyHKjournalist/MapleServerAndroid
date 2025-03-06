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

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteStatement;
import client.Character;
import client.Client;
import client.inventory.Equip;
import client.inventory.Item;
import config.YamlConfig;
import net.AbstractPacketHandler;
import net.packet.InPacket;
import net.server.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import server.MTSItemInfo;
import server.maps.FieldLimit;
import server.maps.MiniDungeonInfo;
import tools.DatabaseConnection;
import tools.PacketCreator;

import java.util.ArrayList;
import java.util.List;


public final class EnterMTSHandler extends AbstractPacketHandler {
    private static final Logger log = LoggerFactory.getLogger(EnterMTSHandler.class);

    @Override
    public void handlePacket(InPacket p, Client c) {
        Character chr = c.getPlayer();

        if (!YamlConfig.config.server.USE_MTS) {
            c.sendPacket(PacketCreator.enableActions());
            return;
        }

        if (chr.getEventInstance() != null) {
            c.sendPacket(PacketCreator.serverNotice(5, "Entering Cash Shop or MTS are disabled when registered on an event."));
            c.sendPacket(PacketCreator.enableActions());
            return;
        }

        if (MiniDungeonInfo.isDungeonMap(chr.getMapId())) {
            c.sendPacket(PacketCreator.serverNotice(5, "Changing channels or entering Cash Shop or MTS are disabled when inside a Mini-Dungeon."));
            c.sendPacket(PacketCreator.enableActions());
            return;
        }

        if (FieldLimit.CANNOTMIGRATE.check(chr.getMap().getFieldLimit())) {
            chr.dropMessage(1, "You can't do it here in this map.");
            c.sendPacket(PacketCreator.enableActions());
            return;
        }

        if (!chr.isAlive()) {
            c.sendPacket(PacketCreator.enableActions());
            return;
        }
        if (chr.getLevel() < 10) {
            c.sendPacket(PacketCreator.blockedMessage2(5));
            c.sendPacket(PacketCreator.enableActions());
            return;
        }

        chr.closePlayerInteractions();
        chr.closePartySearchInteractions();

        chr.unregisterChairBuff();
        Server.getInstance().getPlayerBuffStorage().addBuffsToStorage(chr.getId(), chr.getAllBuffs());
        Server.getInstance().getPlayerBuffStorage().addDiseasesToStorage(chr.getId(), chr.getAllDiseases());
        chr.setAwayFromChannelWorld();
        chr.notifyMapTransferToPartner(-1);
        chr.removeIncomingInvites();
        chr.cancelAllBuffs(true);
        chr.cancelAllDebuffs();
        chr.cancelBuffExpireTask();
        chr.cancelDiseaseExpireTask();
        chr.cancelSkillCooldownTask();
        chr.cancelExpirationTask();

        chr.forfeitExpirableQuests();
        chr.cancelQuestExpirationTask();

        chr.saveCharToDB();

        c.getChannelServer().removePlayer(chr);
        chr.getMap().removePlayer(c.getPlayer());
        try {
            c.sendPacket(PacketCreator.openCashShop(c, true));
        } catch (Exception ex) {
            log.error("EnterMTSHandler sendPacket error", ex);
        }
        chr.getCashShop().open(true);// xD
        c.enableCSActions();
        c.sendPacket(PacketCreator.MTSWantedListingOver(0, 0));
        c.sendPacket(PacketCreator.showMTSCash(c.getPlayer()));
        List<MTSItemInfo> items = new ArrayList<>();
        int pages = 0;
        SQLiteDatabase con = DatabaseConnection.getConnection();
        try (Cursor rs = con.rawQuery("SELECT * FROM mts_items WHERE tab = 1 AND transfer = 0 ORDER BY id DESC LIMIT 16, 16",
                null)) {
            if (rs != null) {
                while (rs.moveToNext()) {
                    int typeColumn = rs.getColumnIndex("type");
                    int itemIdColumn = rs.getColumnIndex("itemid");
                    int quantityColumn = rs.getColumnIndex("quantity");
                    int positionColumn = rs.getColumnIndex("position");
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
                        items.add(new MTSItemInfo(i, rs.getInt(priceColumn) + 100 + (int) (rs.getInt(priceColumn) * 0.1), rs.getInt(idColumn), rs.getInt(sellerColumn), rs.getString(sellerNameColumn), rs.getString(sellEndsColumn)));
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
                        equip.setFlag((short) rs.getInt(flagColumn));
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
                        equip.setExpiration(rs.getLong(expirationColumn));
                        equip.setGiftFrom(rs.getString(giftFromColumn));

                        items.add(new MTSItemInfo(equip, rs.getInt(priceColumn) + 100 + (int) (rs.getInt(priceColumn) * 0.1),
                                rs.getInt(idColumn), rs.getInt(sellerColumn), rs.getString(sellerNameColumn), rs.getString(sellEndsColumn)));
                    }
                }
            }
        } catch (SQLiteException e) {
            log.error("select from mts_items error", e);
        }

        try (SQLiteStatement statement = con.compileStatement("SELECT COUNT(*) FROM mts_items")) {
            long count = statement.simpleQueryForLong();
            pages = (int) Math.ceil(count / 16);
        }
        c.sendPacket(PacketCreator.sendMTS(items, 1, 0, 0, pages));
        c.sendPacket(PacketCreator.transferInventory(getTransfer(chr.getId())));
        c.sendPacket(PacketCreator.notYetSoldInv(getNotYetSold(chr.getId())));
    }

    private List<MTSItemInfo> getNotYetSold(int cid) {
        List<MTSItemInfo> items = new ArrayList<>();
        SQLiteDatabase con = DatabaseConnection.getConnection();
        try (Cursor rs = con.rawQuery("SELECT * FROM mts_items WHERE seller = ? AND transfer = 0 ORDER BY id DESC",
                new String[]{String.valueOf(cid)})) {

            if (rs != null) {
                while (rs.moveToNext()) {
                    int typeColumn = rs.getColumnIndex("type");
                    int itemIdColumn = rs.getColumnIndex("itemid");
                    int quantityColumn = rs.getColumnIndex("quantity");
                    int positionColumn = rs.getColumnIndex("position");
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
            }
        } catch (SQLiteException e) {
            log.error("getNotYetSold error", e);
        }
        return items;
    }

    private List<MTSItemInfo> getTransfer(int cid) {
        List<MTSItemInfo> items = new ArrayList<>();
        SQLiteDatabase con = DatabaseConnection.getConnection();
        try (Cursor rs = con.rawQuery("SELECT * FROM mts_items WHERE transfer = 1 AND seller = ? ORDER BY id DESC", new String[]{String.valueOf(cid)})) {
            if (rs != null) {
                while (rs.moveToNext()) {
                    int typeColumn = rs.getColumnIndex("type");
                    int itemIdColumn = rs.getColumnIndex("itemid");
                    int quantityColumn = rs.getColumnIndex("quantity");
                    int positionColumn = rs.getColumnIndex("position");
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
            }
        } catch (SQLiteException e) {
            log.error("getTransfer error", e);
        }
        return items;
    }
}