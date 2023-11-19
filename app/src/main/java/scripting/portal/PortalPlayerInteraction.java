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
package scripting.portal;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import client.Client;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scripting.AbstractPlayerInteraction;
import scripting.map.MapScriptManager;
import server.maps.Portal;
import tools.DatabaseConnection;
import tools.PacketCreator;

public class PortalPlayerInteraction extends AbstractPlayerInteraction {
    private final Portal portal;
    private static final Logger log = LoggerFactory.getLogger(PortalPlayerInteraction.class);

    public PortalPlayerInteraction(Client c, Portal portal) {
        super(c);
        this.portal = portal;
    }

    public Portal getPortal() {
        return portal;
    }

    public void runMapScript() {
        MapScriptManager msm = MapScriptManager.getInstance();
        msm.runMapScript(c, "onUserEnter/" + portal.getScriptName(), false);
    }

    public boolean hasLevel30Character() {
        SQLiteDatabase con = DatabaseConnection.getConnection();
        try (Cursor ps = con.rawQuery("SELECT `level` FROM `characters` WHERE accountid = ?", new String[]{String.valueOf(getPlayer().getAccountID())})) {

            if (ps != null) {
                while (ps.moveToNext()) {
                    int levelIdx = ps.getColumnIndex("level");
                    if (ps.getInt(levelIdx) >= 30) {
                        return true;
                    }
                }
            }
        } catch (SQLiteException sqle) {
            log.error("hasLevel30Character error", sqle);
        }

        return getPlayer().getLevel() >= 30;
    }

    public void blockPortal() {
        c.getPlayer().blockPortal(getPortal().getScriptName());
    }

    public void unblockPortal() {
        c.getPlayer().unblockPortal(getPortal().getScriptName());
    }

    public void playPortalSound() {
        c.sendPacket(PacketCreator.playPortalSound());
    }
}