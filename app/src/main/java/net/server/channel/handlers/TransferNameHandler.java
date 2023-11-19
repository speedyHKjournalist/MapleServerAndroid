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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.DatabaseConnection;
import tools.PacketCreator;

import java.util.Calendar;

import static java.util.concurrent.TimeUnit.DAYS;

/**
 * @author Ronan
 * @author Ubaware
 */
public final class TransferNameHandler extends AbstractPacketHandler {
    private static final Logger log = LoggerFactory.getLogger(TransferNameHandler.class);
    @Override
    public final void handlePacket(InPacket p, Client c) {
        p.readInt(); //cid
        int birthday = p.readInt();
        if (!CashOperationHandler.checkBirthday(c, birthday)) {
            c.sendPacket(PacketCreator.showCashShopMessage((byte) 0xC4));
            c.sendPacket(PacketCreator.enableActions());
            return;
        }
        if (!YamlConfig.config.server.ALLOW_CASHSHOP_NAME_CHANGE) {
            c.sendPacket(PacketCreator.sendNameTransferRules(4));
            return;
        }
        Character chr = c.getPlayer();
        if (chr.getLevel() < 10) {
            c.sendPacket(PacketCreator.sendNameTransferRules(4));
            return;
        } else if (c.getTempBanCalendar() != null && c.getTempBanCalendar().getTimeInMillis() + DAYS.toMillis(30) < Calendar.getInstance().getTimeInMillis()) {
            c.sendPacket(PacketCreator.sendNameTransferRules(2));
            return;
        }
        //sql queries
        SQLiteDatabase con = DatabaseConnection.getConnection();
        try (Cursor cursor = con.rawQuery("SELECT completionTime FROM namechanges WHERE characterid=?", new String[]{String.valueOf(chr.getId())})) { //double check, just in case
            while (cursor.moveToNext()) {
                int completionTimeIdx = cursor.getColumnIndex("completionTime");
                long completedTimestamp = cursor.getLong(completionTimeIdx);
                long currentTime = System.currentTimeMillis();

                if (completedTimestamp == 0) { // has a pending name request
                    // Send the appropriate response to the client (implement this method accordingly).
                    c.sendPacket(PacketCreator.sendNameTransferRules(1));
                    return;
                } else if (completedTimestamp + YamlConfig.config.server.NAME_CHANGE_COOLDOWN > currentTime) {
                    // Send the appropriate response to the client (implement this method accordingly).
                    c.sendPacket(PacketCreator.sendNameTransferRules(3));
                    return;
                }
            }
        } catch (SQLiteException e) {
            log.error("TransferNameHandler handlepacket error", e);
            return;
        }
        c.sendPacket(PacketCreator.sendNameTransferRules(0));
    }
}