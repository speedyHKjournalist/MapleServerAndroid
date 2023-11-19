/*
    This file is part of the HeavenMS MapleStory Server, commands OdinMS-based
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

/*
   @Author: Ronan
*/
package client.command.commands.gm4;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.graphics.Point;
import client.Character;
import client.Client;
import client.command.Command;
import net.server.channel.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import server.life.LifeFactory;
import server.life.NPC;
import server.maps.MapleMap;
import tools.DatabaseConnection;
import tools.PacketCreator;

public class PnpcCommand extends Command {
    private static final Logger log = LoggerFactory.getLogger(PnpcCommand.class);
    {
        setDescription("Spawn a permanent NPC on your location.");
    }

    @Override
    public void execute(Client c, String[] params) {
        Character player = c.getPlayer();
        if (params.length < 1) {
            player.yellowMessage("Syntax: !pnpc <npcid>");
            return;
        }

        // command suggestion thanks to HighKey21, none, bibiko94 (TAYAMO), asafgb
        int mapId = player.getMapId();
        int npcId = Integer.parseInt(params[0]);
        if (player.getMap().containsNPC(npcId)) {
            player.dropMessage(5, "This map already contains the specified NPC.");
            return;
        }

        NPC npc = LifeFactory.getNPC(npcId);

        Point checkpos = player.getMap().getGroundBelow(player.getPosition());
        int xpos = checkpos.x;
        int ypos = checkpos.y;
        int fh = player.getMap().getFootholds().findBelow(checkpos).getId();

        if (npc != null && !npc.getName().equals("MISSINGNO")) {
            SQLiteDatabase con = DatabaseConnection.getConnection();
            try {
                ContentValues values = new ContentValues();
                values.put("life", npcId);
                values.put("f", 0);
                values.put("fh", fh);
                values.put("cy", ypos);
                values.put("rx0", xpos + 50);
                values.put("rx1", xpos - 50);
                values.put("type", "n");
                values.put("x", xpos);
                values.put("y", ypos);
                values.put("world", player.getWorld());
                values.put("map", mapId);
                values.put("mobtime", -1);
                values.put("hide", 0);
                con.insert("plife", null, values);

                for (Channel ch : player.getWorldServer().getChannels()) {
                    npc = LifeFactory.getNPC(npcId);
                    npc.setPosition(checkpos);
                    npc.setCy(ypos);
                    npc.setRx0(xpos + 50);
                    npc.setRx1(xpos - 50);
                    npc.setFh(fh);

                    MapleMap map = ch.getMapFactory().getMap(mapId);
                    map.addMapObject(npc);
                    map.broadcastMessage(PacketCreator.spawnNPC(npc));
                }

                player.yellowMessage("Pnpc created.");
            } catch (SQLiteException e) {
                log.error("PnpcCommand error", e);
                player.dropMessage(5, "Failed to store pNPC in the database.");
            }
        } else {
            player.dropMessage(5, "You have entered an invalid NPC id.");
        }
    }
}