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

package net.server.channel.handlers;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import client.Character;
import client.Client;
import config.YamlConfig;
import net.AbstractPacketHandler;
import net.packet.InPacket;
import net.server.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.DatabaseConnection;
import tools.PacketCreator;

/**
 * @author Ronan
 * @author Ubaware
 */
public final class TransferWorldHandler extends AbstractPacketHandler {
    private static final Logger log = LoggerFactory.getLogger(TransferWorldHandler.class);
    @Override
    public final void handlePacket(InPacket p, Client c) {
        p.readInt(); //cid
        int birthday = p.readInt();
        if (!CashOperationHandler.checkBirthday(c, birthday)) {
            c.sendPacket(PacketCreator.showCashShopMessage((byte) 0xC4));
            c.sendPacket(PacketCreator.enableActions());
            return;
        }
        Character chr = c.getPlayer();
        if (!YamlConfig.config.server.ALLOW_CASHSHOP_WORLD_TRANSFER || Server.getInstance().getWorldsSize() <= 1) {
            c.sendPacket(PacketCreator.sendWorldTransferRules(9, c));
            return;
        }
        int worldTransferError = chr.checkWorldTransferEligibility();
        if (worldTransferError != 0) {
            c.sendPacket(PacketCreator.sendWorldTransferRules(worldTransferError, c));
            return;
        }
        SQLiteDatabase con = DatabaseConnection.getConnection();
        try (Cursor cursor = con.rawQuery("SELECT completionTime FROM worldtransfers WHERE characterid=?",
                     new String[]{ String.valueOf(chr.getId()) })) {
            while (cursor.moveToNext()) {
                int completionTimeIdx = cursor.getColumnIndex("completionTime");
                long completionTime = cursor.getLong(completionTimeIdx);
                if (completionTime == 0) { //has pending world transfer
                    c.sendPacket(PacketCreator.sendWorldTransferRules(6, c));
                    return;
                } else if (completionTime + YamlConfig.config.server.WORLD_TRANSFER_COOLDOWN > System.currentTimeMillis()) {
                    c.sendPacket(PacketCreator.sendWorldTransferRules(7, c));
                    return;
                }
            }
        } catch (SQLiteException e) {
            log.error("Transfer world handlePacket error", e);
            return;
        }
        c.sendPacket(PacketCreator.sendWorldTransferRules(0, c));
    }
}