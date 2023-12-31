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
   @Author: Arthur L - Refactored command content into modules
*/
package client.command.commands.gm3;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import client.Character;
import client.Client;
import client.command.Command;
import tools.DatabaseConnection;

public class UnBanCommand extends Command {
    {
        setDescription("Unban a player.");
    }

    @Override
    public void execute(Client c, String[] params) {
        Character player = c.getPlayer();
        if (params.length < 1) {
            player.yellowMessage("Syntax: !unban <playername>");
            return;
        }
        SQLiteDatabase con = DatabaseConnection.getConnection();
        try {
            int aid = Character.getAccountIdByName(params[0]);
            ContentValues values = new ContentValues();
            values.put("banned", -1);

            con.update("accounts", values, "id = ?", new String[]{String.valueOf(aid)});
            con.delete("ipbans", "aid = ?", new String[]{String.valueOf(aid)});
            con.delete("macbans", "aid = ?", new String[]{String.valueOf(aid)});
        } catch (Exception e) {
            player.message("Failed to unban " + params[0]);
            return;
        }
        player.message("Unbanned " + params[0]);
    }
}
