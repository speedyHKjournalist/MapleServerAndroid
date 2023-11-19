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
import client.*;
import client.BuddyList.BuddyAddResult;
import client.Character;
import client.BuddyList.BuddyOperation;
import net.AbstractPacketHandler;
import net.packet.InPacket;
import net.server.world.World;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.DatabaseConnection;
import tools.PacketCreator;

import static client.BuddyList.BuddyOperation.ADDED;

public class BuddylistModifyHandler extends AbstractPacketHandler {
    private static final Logger log = LoggerFactory.getLogger(BuddylistModifyHandler.class);
    private static class CharacterIdNameBuddyCapacity extends CharacterNameAndId {
        private final int buddyCapacity;

        public CharacterIdNameBuddyCapacity(int id, String name, int buddyCapacity) {
            super(id, name);
            this.buddyCapacity = buddyCapacity;
        }

        public int getBuddyCapacity() {
            return buddyCapacity;
        }
    }

    private void nextPendingRequest(Client c) {
        CharacterNameAndId pendingBuddyRequest = c.getPlayer().getBuddylist().pollPendingRequest();
        if (pendingBuddyRequest != null) {
            c.sendPacket(PacketCreator.requestBuddylistAdd(pendingBuddyRequest.getId(), c.getPlayer().getId(), pendingBuddyRequest.getName()));
        }
    }

    private CharacterIdNameBuddyCapacity getCharacterIdAndNameFromDatabase(String name) throws SQLiteException {
        CharacterIdNameBuddyCapacity ret = null;
        String[] selectionArgs = { "%" + name + "%" };
        SQLiteDatabase con = DatabaseConnection.getConnection();
        try (Cursor cursor = con.rawQuery("SELECT id, name, buddyCapacity FROM characters WHERE name LIKE ?", selectionArgs)) {
            if (cursor.moveToFirst()) {
                int idIdx = cursor.getColumnIndex("id");
                int nameIdx = cursor.getColumnIndex("name");
                int buddyCapacityIdx = cursor.getColumnIndex("buddyCapacity");
                ret = new CharacterIdNameBuddyCapacity(cursor.getInt(idIdx), cursor.getString(nameIdx), cursor.getInt(buddyCapacityIdx));
            }
        }

        return ret;
    }

    @Override
    public void handlePacket(InPacket p, Client c) {
        int mode = p.readByte();
        Character player = c.getPlayer();
        BuddyList buddylist = player.getBuddylist();
        if (mode == 1) { // add
            String addName = p.readString();
            String group = p.readString();
            if (group.length() > 16 || addName.length() < 4 || addName.length() > 13) {
                return; //hax.
            }
            BuddylistEntry ble = buddylist.get(addName);
            if (ble != null && !ble.isVisible() && group.equals(ble.getGroup())) {
                c.sendPacket(PacketCreator.serverNotice(1, "You already have \"" + ble.getName() + "\" on your Buddylist"));
            } else if (buddylist.isFull() && ble == null) {
                c.sendPacket(PacketCreator.serverNotice(1, "Your buddylist is already full"));
            } else if (ble == null) {
                try {
                    World world = c.getWorldServer();
                    CharacterIdNameBuddyCapacity charWithId;
                    int channel;
                    Character otherChar = c.getChannelServer().getPlayerStorage().getCharacterByName(addName);
                    if (otherChar != null) {
                        channel = c.getChannel();
                        charWithId = new CharacterIdNameBuddyCapacity(otherChar.getId(), otherChar.getName(), otherChar.getBuddylist().getCapacity());
                    } else {
                        channel = world.find(addName);
                        charWithId = getCharacterIdAndNameFromDatabase(addName);
                    }
                    if (charWithId != null) {
                        BuddyAddResult buddyAddResult = null;
                        if (channel != -1) {
                            buddyAddResult = world.requestBuddyAdd(addName, c.getChannel(), player.getId(), player.getName());
                        } else {
                            SQLiteDatabase con = DatabaseConnection.getConnection();
                            try (Cursor cursor = con.rawQuery("SELECT COUNT(*) as buddyCount FROM buddies WHERE characterid = ? AND pending = 0",
                                    new String[]{ String.valueOf(charWithId.getId()) })) {
                                if (cursor.moveToFirst()) {
                                    int buddyCountIdx = cursor.getColumnIndex("buddyCount");
                                    if (cursor.getInt(buddyCountIdx) >= charWithId.getBuddyCapacity()) {
                                        buddyAddResult = BuddyAddResult.BUDDYLIST_FULL;
                                    }
                                }
                            }

                            try (Cursor cursor = con.rawQuery("SELECT pending FROM buddies WHERE characterid = ? AND buddyid = ?",
                                    new String[]{ String.valueOf(charWithId.getId()), String.valueOf(player.getId()) })) {
                                if (cursor.moveToFirst()) {
                                        buddyAddResult = BuddyAddResult.ALREADY_ON_LIST;
                                }
                            }
                        }
                        if (buddyAddResult == BuddyAddResult.BUDDYLIST_FULL) {
                            c.sendPacket(PacketCreator.serverNotice(1, "\"" + addName + "\"'s Buddylist is full"));
                        } else {
                            int displayChannel;
                            displayChannel = -1;
                            int otherCid = charWithId.getId();
                            if (buddyAddResult == BuddyAddResult.ALREADY_ON_LIST && channel != -1) {
                                displayChannel = channel;
                                notifyRemoteChannel(c, channel, otherCid, ADDED);
                            } else if (buddyAddResult != BuddyAddResult.ALREADY_ON_LIST && channel == -1) {
                                SQLiteDatabase con = DatabaseConnection.getConnection();
                                ContentValues values = new ContentValues();
                                values.put("characterid", charWithId.getId());
                                values.put("buddyid", player.getId());
                                values.put("pending", 1);
                                con.insert("buddies", null, values);
                            }
                            buddylist.put(new BuddylistEntry(charWithId.getName(), group, otherCid, displayChannel, true));
                            c.sendPacket(PacketCreator.updateBuddylist(buddylist.getBuddies()));
                        }
                    } else {
                        c.sendPacket(PacketCreator.serverNotice(1, "A character called \"" + addName + "\" does not exist"));
                    }
                } catch (SQLiteException e) {
                    log.error("BuddylistModifyHandler error", e);
                }
            } else {
                ble.changeGroup(group);
                c.sendPacket(PacketCreator.updateBuddylist(buddylist.getBuddies()));
            }
        } else if (mode == 2) { // accept buddy
            int otherCid = p.readInt();
            if (!buddylist.isFull()) {
                try {
                    int channel = c.getWorldServer().find(otherCid);//worldInterface.find(otherCid);
                    String otherName = null;
                    Character otherChar = c.getChannelServer().getPlayerStorage().getCharacterById(otherCid);
                    if (otherChar == null) {
                        String[] projection = { "name" };
                        String selection = "id = ?";
                        String[] selectionArgs = { String.valueOf(otherCid) };
                        SQLiteDatabase con = DatabaseConnection.getConnection();
                        try (Cursor cursor = con.query("characters", projection, selection, selectionArgs, null, null, null)) {
                            if (cursor.moveToNext()) {
                                int nameIdx = cursor.getColumnIndex("name");
                                otherName = cursor.getString(nameIdx);
                            }
                        }
                    } else {
                        otherName = otherChar.getName();
                    }
                    if (otherName != null) {
                        buddylist.put(new BuddylistEntry(otherName, "Default Group", otherCid, channel, true));
                        c.sendPacket(PacketCreator.updateBuddylist(buddylist.getBuddies()));
                        notifyRemoteChannel(c, channel, otherCid, ADDED);
                    }
                } catch (SQLiteException e) {
                    log.error("Buddylist query characters error", e);
                }
            }
            nextPendingRequest(c);
        } else if (mode == 3) { // delete
            int otherCid = p.readInt();
            player.deleteBuddy(otherCid);
        }
    }

    private void notifyRemoteChannel(Client c, int remoteChannel, int otherCid, BuddyOperation operation) {
        Character player = c.getPlayer();
        if (remoteChannel != -1) {
            c.getWorldServer().buddyChanged(otherCid, player.getId(), player.getName(), c.getChannel(), operation);
        }
    }
}
