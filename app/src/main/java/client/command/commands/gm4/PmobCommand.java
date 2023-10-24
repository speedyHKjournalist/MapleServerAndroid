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
import client.Character;
import client.Client;
import client.command.Command;
import net.server.channel.Channel;
import server.life.LifeFactory;
import server.life.Monster;
import server.maps.MapleMap;
import tools.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import android.graphics.Point;

public class PmobCommand extends Command {
    {
        setDescription("Spawn a permanent mob on your location.");
    }

    @Override
    public void execute(Client c, String[] params) {
        Character player = c.getPlayer();
        if (params.length < 1) {
            player.yellowMessage("Syntax: !pmob <mobid> [<mobtime>]");
            return;
        }

        // command suggestion thanks to HighKey21, none, bibiko94 (TAYAMO), asafgb
        int mapId = player.getMapId();
        int mobId = Integer.parseInt(params[0]);
        int mobTime = (params.length > 1) ? Integer.parseInt(params[1]) : -1;

        Point checkpos = player.getMap().getGroundBelow(player.getPosition());
        int xpos = checkpos.x;
        int ypos = checkpos.y;
        int fh = player.getMap().getFootholds().findBelow(checkpos).getId();

        Monster mob = LifeFactory.getMonster(mobId);
        if (mob != null && !mob.getName().equals("MISSINGNO")) {
            mob.setPosition(checkpos);
            mob.setCy(ypos);
            mob.setRx0(xpos + 50);
            mob.setRx1(xpos - 50);
            mob.setFh(fh);
            try (SQLiteDatabase con = DatabaseConnection.getConnection()) {
                ContentValues values = new ContentValues();
                values.put("life", mobId);
                values.put("f", 0);
                values.put("fh", fh);
                values.put("cy", ypos);
                values.put("rx0", xpos + 50);
                values.put("rx1", xpos - 50);
                values.put("type", "m");
                values.put("x", xpos);
                values.put("y", ypos);
                values.put("world", player.getWorld());
                values.put("map", mapId);
                values.put("mobtime", mobTime);
                values.put("hide", 0);
                con.insert("plife", null, values);

                for (Channel ch : player.getWorldServer().getChannels()) {
                    MapleMap map = ch.getMapFactory().getMap(mapId);
                    map.addMonsterSpawn(mob, mobTime, -1);
                    map.addAllMonsterSpawn(mob, mobTime, -1);
                }

                player.yellowMessage("Pmob created.");
            } catch (SQLiteException e) {
                e.printStackTrace();
                player.dropMessage(5, "Failed to store pmob in the database.");
            }
        } else {
            player.dropMessage(5, "You have entered an invalid mob id.");
        }
    }
}