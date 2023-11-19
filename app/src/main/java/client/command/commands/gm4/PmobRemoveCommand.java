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

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.graphics.Point;
import client.Character;
import client.Client;
import client.command.Command;
import net.server.channel.Channel;
import server.maps.MapleMap;
import tools.DatabaseConnection;
import tools.Pair;

import java.sql.PreparedStatement;
import java.util.LinkedList;
import java.util.List;

public class PmobRemoveCommand extends Command {
    {
        setDescription("Remove all permanent mobs of the same type on the map.");
    }

    @Override
    public void execute(Client c, String[] params) {
        Character player = c.getPlayer();

        int mapId = player.getMapId();
        int mobId = params.length > 0 ? Integer.parseInt(params[0]) : -1;

        Point pos = player.getPosition();
        int xpos = pos.x;
        int ypos = pos.y;

        List<Pair<Integer, Pair<Integer, Integer>>> toRemove = new LinkedList<>();
        SQLiteDatabase con = DatabaseConnection.getConnection();
        try {
            final PreparedStatement ps;
            String select;
            String[] selectionArgs;
            if (mobId > -1) {
                select = "SELECT * FROM plife WHERE world = ? AND map = ? AND type LIKE ? AND life = ?";
                selectionArgs = new String[]{
                        String.valueOf(player.getWorld()),
                        String.valueOf(mapId),
                        "m",
                        String.valueOf(mobId)
                };
            } else {
                select = "SELECT * FROM plife WHERE world = ? AND map = ? AND type LIKE ? AND x >= ? AND x <= ? AND y >= ? AND y <= ?";
                selectionArgs = new String[]{
                        String.valueOf(player.getWorld()),
                        String.valueOf(mapId),
                        "m",
                        String.valueOf(xpos - 50),
                        String.valueOf(xpos + 50),
                        String.valueOf(ypos - 50),
                        String.valueOf(ypos + 50)
                };
            }

            try (Cursor cursor = con.rawQuery(select, selectionArgs)) {
                if (cursor.moveToFirst()) {
                    do {
                        int lifeIdx = cursor.getColumnIndex("life");
                        int xIdx = cursor.getColumnIndex("x");
                        int yIdx = cursor.getColumnIndex("y");

                        int life = cursor.getInt(lifeIdx);
                        int x = cursor.getInt(xIdx);
                        int y = cursor.getInt(yIdx);

                        toRemove.add(new Pair<>(life, new Pair<>(x, y)));

                        // Delete the row
                        int _idIdx = cursor.getColumnIndex("_id");
                        long rowId = cursor.getLong(_idIdx);
                        con.delete("plife", "_id=?", new String[]{String.valueOf(rowId)});
                    } while (cursor.moveToNext());
                }
            }
        } catch (SQLiteException e) {
            player.dropMessage(5, "Failed to remove pmob from the database.");
        }

        if (!toRemove.isEmpty()) {
            for (Channel ch : player.getWorldServer().getChannels()) {
                MapleMap map = ch.getMapFactory().getMap(mapId);

                for (Pair<Integer, Pair<Integer, Integer>> r : toRemove) {
                    map.removeMonsterSpawn(r.getLeft(), r.getRight().getLeft(), r.getRight().getRight());
                    map.removeAllMonsterSpawn(r.getLeft(), r.getRight().getLeft(), r.getRight().getRight());
                }
            }
        }

        player.yellowMessage("Cleared " + toRemove.size() + " pmob placements.");
    }
}