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
package server.life;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteStatement;
import android.graphics.Point;
import client.Character;
import client.Client;
import client.inventory.InventoryType;
import client.inventory.Item;
import config.YamlConfig;
import constants.game.GameConstants;
import constants.id.NpcId;
import net.server.Server;
import net.server.channel.Channel;
import net.server.world.World;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import server.life.positioner.PlayerNPCPodium;
import server.life.positioner.PlayerNPCPositioner;
import server.maps.AbstractMapObject;
import server.maps.MapObject;
import server.maps.MapObjectType;
import server.maps.MapleMap;
import tools.DatabaseConnection;
import tools.PacketCreator;
import tools.Pair;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author XoticStory
 * @author Ronan
 */
// TODO: remove dependency on custom Npc.wz. All NPCs with id 9901910 and above are custom additions for player npcs.
// In summary: NPCs 9901910-9906599 and 9977777 are custom additions to HeavenMS that should be removed.
public class PlayerNPC extends AbstractMapObject {
    private static final Logger log = LoggerFactory.getLogger(PlayerNPC.class);
    private static final Map<Byte, List<Integer>> availablePlayerNpcScriptIds = new HashMap<>();
    private static final AtomicInteger runningOverallRank = new AtomicInteger();
    private static final List<AtomicInteger> runningWorldRank = new ArrayList<>();
    private static final Map<Pair<Integer, Integer>, AtomicInteger> runningWorldJobRank = new HashMap<>();

    private Map<Short, Integer> equips = new HashMap<>();
    private int scriptId, face, hair, gender, job;
    private byte skin;
    private String name = "";
    private int dir, FH, RX0, RX1, CY;
    private int worldRank, overallRank, worldJobRank, overallJobRank;

    public PlayerNPC(String name, int scriptId, int face, int hair, int gender, byte skin, Map<Short, Integer> equips, int dir, int FH, int RX0, int RX1, int CX, int CY, int oid) {
        this.equips = equips;
        this.scriptId = scriptId;
        this.face = face;
        this.hair = hair;
        this.gender = gender;
        this.skin = skin;
        this.name = name;
        this.dir = dir;
        this.FH = FH;
        this.RX0 = RX0;
        this.RX1 = RX1;
        this.CY = CY;
        this.job = 7777;    // supposed to be developer

        setPosition(new Point(CX, CY));
        setObjectId(oid);
    }

    public PlayerNPC(Cursor cursor) {
        int objectId = -1;
        try {
            int cyIdx = cursor.getColumnIndex("cy");
            int nameIdx = cursor.getColumnIndex("name");
            int hairIdx = cursor.getColumnIndex("hair");
            int faceIdx = cursor.getColumnIndex("face");
            int skinIdx = cursor.getColumnIndex("skin");
            int genderIdx = cursor.getColumnIndex("gender");
            int dirIdx = cursor.getColumnIndex("dir");
            int fhIdx = cursor.getColumnIndex("fh");
            int rx0Idx = cursor.getColumnIndex("rx0");
            int rx1Idx = cursor.getColumnIndex("rx1");
            int scriptidIdx = cursor.getColumnIndex("scriptid");
            int worldrankIdx = cursor.getColumnIndex("worldrank");
            int overallrankIdx = cursor.getColumnIndex("overallrank");
            int worldjobrankIdx = cursor.getColumnIndex("worldjobrank");
            int jobIdx = cursor.getColumnIndex("job");
            if (cyIdx != -1 &&
                    nameIdx != -1 &&
                    hairIdx != -1 &&
                    faceIdx != -1 &&
                    skinIdx != -1 &&
                    genderIdx != -1 &&
                    dirIdx != -1 &&
                    fhIdx != -1 &&
                    rx0Idx != -1 &&
                    rx1Idx != -1 &&
                    scriptidIdx != -1 &&
                    worldrankIdx != -1 &&
                    overallrankIdx != -1 &&
                    worldjobrankIdx != -1 &&
                    jobIdx != -1) {
                CY = cursor.getInt(cyIdx);
                name = cursor.getString(nameIdx);
                hair = cursor.getInt(hairIdx);
                face = cursor.getInt(faceIdx);
                skin = cursor.getBlob(skinIdx)[0];
                gender = cursor.getInt(genderIdx);
                dir = cursor.getInt(dirIdx);
                FH = cursor.getInt(fhIdx);
                RX0 = cursor.getInt(rx0Idx);
                RX1 = cursor.getInt(rx1Idx);
                scriptId = cursor.getInt(scriptidIdx);

                worldRank = cursor.getInt(worldrankIdx);
                overallRank = cursor.getInt(overallrankIdx);
                worldJobRank = cursor.getInt(worldjobrankIdx);
                overallJobRank = GameConstants.getOverallJobRankByScriptId(scriptId);
                job = cursor.getInt(jobIdx);

                int xIdx = cursor.getColumnIndex("x");
                int idIdx = cursor.getColumnIndex("id");
                if (xIdx != -1) {
                    setPosition(new Point(cursor.getInt(xIdx), CY));
                }
                if (idIdx != -1) {
                    objectId = cursor.getInt(idIdx);
                    setObjectId(objectId);
                }
            }

            if (objectId != -1) {
                String query = "SELECT equippos, equipid FROM playernpcs_equip WHERE npcid = ?";
                String[] selectionArgs = {String.valueOf(objectId)};
                SQLiteDatabase con = DatabaseConnection.getConnection();
                try (Cursor cursor1 = con.rawQuery(query, selectionArgs)) {
                    if (cursor1 != null) {
                        while (cursor.moveToNext()) {
                            int equipposIdx = cursor.getColumnIndex("equippos");
                            int equipidIdx = cursor.getColumnIndex("equipid");
                            if (equipposIdx != -1 && equipidIdx != -1) {
                                equips.put(cursor1.getShort(equipposIdx), cursor1.getInt(equipidIdx));
                            }
                        }
                    }
                }
            }
        } catch (SQLiteException e) {
            log.error("select PlayerNPC error", e);
        }
    }

    public static void loadRunningRankData(SQLiteDatabase con, int worlds) throws SQLiteException {
        getRunningOverallRanks(con);
        getRunningWorldRanks(con, worlds);
        getRunningWorldJobRanks(con);
    }

    public Map<Short, Integer> getEquips() {
        return equips;
    }

    public int getScriptId() {
        return scriptId;
    }

    public int getJob() {
        return job;
    }

    public int getDirection() {
        return dir;
    }

    public int getFH() {
        return FH;
    }

    public int getRX0() {
        return RX0;
    }

    public int getRX1() {
        return RX1;
    }

    public int getCY() {
        return CY;
    }

    public byte getSkin() {
        return skin;
    }

    public String getName() {
        return name;
    }

    public int getFace() {
        return face;
    }

    public int getHair() {
        return hair;
    }

    public int getGender() {
        return gender;
    }

    public int getWorldRank() {
        return worldRank;
    }

    public int getOverallRank() {
        return overallRank;
    }

    public int getWorldJobRank() {
        return worldJobRank;
    }

    public int getOverallJobRank() {
        return overallJobRank;
    }

    @Override
    public MapObjectType getType() {
        return MapObjectType.PLAYER_NPC;
    }

    @Override
    public void sendSpawnData(Client client) {
        client.sendPacket(PacketCreator.spawnPlayerNPC(this));
        client.sendPacket(PacketCreator.getPlayerNPC(this));
    }

    @Override
    public void sendDestroyData(Client client) {
        client.sendPacket(PacketCreator.removeNPCController(this.getObjectId()));
        client.sendPacket(PacketCreator.removePlayerNPC(this.getObjectId()));
    }

    private static void getRunningOverallRanks(SQLiteDatabase con) throws SQLiteException {
        String query = "SELECT max(overallrank) FROM playernpcs"; // Replace with your query
        try (Cursor cursor = con.rawQuery(query, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int maxOverallRank = cursor.getInt(0);
                runningOverallRank.set(maxOverallRank + 1);
            } else {
                runningOverallRank.set(1);
            }
        }
    }

    private static void getRunningWorldRanks(SQLiteDatabase con, int worlds) throws SQLiteException {
        // Initialize the list of runningWorldRank with AtomicInteger instances
        for (int i = 0; i < worlds; i++) {
            runningWorldRank.add(new AtomicInteger(1));
        }

        String query = "SELECT world, max(worldrank) FROM playernpcs GROUP BY world ORDER BY world"; // Replace with your query

        try (Cursor cursor = con.rawQuery(query, null)) {
            while (cursor != null && cursor.moveToNext()) {
                int wid = cursor.getInt(0);
                if (wid < worlds) {
                    int maxWorldRank = cursor.getInt(1);
                    runningWorldRank.get(wid).set(maxWorldRank + 1);
                }
            }
        }
    }

    private static void getRunningWorldJobRanks(SQLiteDatabase con) throws SQLiteException {
        String query = "SELECT world, job, max(worldjobrank) FROM playernpcs GROUP BY world, job ORDER BY world, job";
        try (Cursor cursor = con.rawQuery(query, null)) {
            while (cursor != null && cursor.moveToNext()) {
                int world = cursor.getInt(0);
                int job = cursor.getInt(1);
                int maxWorldJobRank = cursor.getInt(2);
                Pair<Integer, Integer> key = new Pair<>(world, job);
                runningWorldJobRank.put(key, new AtomicInteger(maxWorldJobRank + 1));
            }
        }
    }

    private static int getAndIncrementRunningWorldJobRanks(int world, int job) {
        AtomicInteger wjr = runningWorldJobRank.get(new Pair<>(world, job));
        if (wjr == null) {
            wjr = new AtomicInteger(1);
            runningWorldJobRank.put(new Pair<>(world, job), wjr);
        }

        return wjr.getAndIncrement();
    }

    public static boolean canSpawnPlayerNpc(String name, int mapid) {
        boolean ret = true;

        String[] selectionArgs = {name, String.valueOf(mapid)};
        SQLiteDatabase con = DatabaseConnection.getConnection();
        try (Cursor cursor = con.rawQuery("SELECT name FROM playernpcs WHERE name LIKE ? AND map = ?", selectionArgs)) {
            if (cursor.moveToFirst()) {
                ret = false;
            }
        } catch (SQLiteException e) {
            log.error("canSpawnPlayerNpc error", e);
        }

        return ret;
    }

    public void updatePlayerNPCPosition(MapleMap map, Point newPos) {
        setPosition(newPos);
        RX0 = newPos.x + 50;
        RX1 = newPos.x - 50;
        CY = newPos.y;
        FH = map.getFootholds().findBelow(newPos).getId();
        SQLiteDatabase con = DatabaseConnection.getConnection();
        try {
             con.execSQL("UPDATE playernpcs SET x = ?, cy = ?, fh = ?, rx0 = ?, rx1 = ? WHERE id = ?", new String[]{
                     String.valueOf(newPos.x),
                     String.valueOf(CY),
                     String.valueOf(FH),
                     String.valueOf(RX0),
                     String.valueOf(RX1),
                     String.valueOf(getObjectId())
             });
        } catch (SQLiteException e) {
            log.error("updatePlayerNPCPosition", e);
        }
    }

    private static void fetchAvailableScriptIdsFromDb(byte branch, List<Integer> list) {
        try {
            int branchLen = (branch < 26) ? 100 : 400;
            int branchSid = NpcId.PLAYER_NPC_BASE + (branch * 100);
            int nextBranchSid = branchSid + branchLen;

            List<Integer> availables = new ArrayList<>(20);
            String query = "SELECT scriptid FROM playernpcs WHERE scriptid >= ? AND scriptid < ? ORDER BY scriptid";
            String[] selectionArgs = {String.valueOf(branchSid), String.valueOf(nextBranchSid)};
            SQLiteDatabase con = DatabaseConnection.getConnection();
            try (Cursor cursor = con.rawQuery(query, selectionArgs)) {
                Set<Integer> usedScriptIds = new HashSet<>();
                while (cursor.moveToNext()) {
                    usedScriptIds.add(cursor.getInt(0));
                }
                int j = 0;
                for (int i = branchSid; i < nextBranchSid; i++) {
                    if (!usedScriptIds.contains(i)) {
                        if (PlayerNPCFactory.isExistentScriptid(i)) {  // thanks Ark, Zein, geno, Ariel, JrCl0wn for noticing client crashes due to use of missing scriptids
                            availables.add(i);
                            j++;

                            if (j == 20) {
                                break;
                            }
                        } else {
                            break;  // after this point no more scriptids expected...
                        }
                    }
                }
            }

            for (int i = availables.size() - 1; i >= 0; i--) {
                list.add(availables.get(i));
            }
        } catch (SQLiteException sqle) {
            log.error("fetchAvailableScriptIdsFromDb error", sqle);
        }
    }

    private static int getNextScriptId(byte branch) {
        List<Integer> availablesBranch = availablePlayerNpcScriptIds.get(branch);

        if (availablesBranch == null) {
            availablesBranch = new ArrayList<>(20);
            availablePlayerNpcScriptIds.put(branch, availablesBranch);
        }

        if (availablesBranch.isEmpty()) {
            fetchAvailableScriptIdsFromDb(branch, availablesBranch);

            if (availablesBranch.isEmpty()) {
                return -1;
            }
        }

        return availablesBranch.remove(availablesBranch.size() - 1);
    }

    private static PlayerNPC createPlayerNPCInternal(MapleMap map, Point pos, Character chr) {
        int mapId = map.getId();

        if (!canSpawnPlayerNpc(chr.getName(), mapId)) {
            return null;
        }

        byte branch = GameConstants.getHallOfFameBranch(chr.getJob(), mapId);

        int scriptId = getNextScriptId(branch);
        if (scriptId == -1) {
            return null;
        }

        if (pos == null) {
            if (GameConstants.isPodiumHallOfFameMap(map.getId())) {
                pos = PlayerNPCPodium.getNextPlayerNpcPosition(map);
            } else {
                pos = PlayerNPCPositioner.getNextPlayerNpcPosition(map);
            }

            if (pos == null) {
                return null;
            }
        }

        if (YamlConfig.config.server.USE_DEBUG) {
            log.debug("GOT SID {}, POS {}", scriptId, pos);
        }

        int worldId = chr.getWorld();
        int jobId = (chr.getJob().getId() / 100) * 100;

        PlayerNPC ret = null;
        SQLiteDatabase con = DatabaseConnection.getConnection();
        try {
            boolean createNew = false;
            String[] selectionArgs = {String.valueOf(scriptId)};
            try (Cursor cursor = con.rawQuery("SELECT * FROM playernpcs WHERE scriptid = ?", selectionArgs)) {
                if (!cursor.moveToFirst()) {
                    createNew = true;
                }
            }

            if (createNew) {   // creates new playernpc if scriptid doesn't exist
                final int npcId;
                ContentValues values = new ContentValues();
                values.put("name", chr.getName());
                values.put("hair", chr.getHair());
                values.put("face", chr.getFace());
                values.put("skin", chr.getSkinColor().getId());
                values.put("gender", chr.getGender());
                values.put("x", pos.x);
                values.put("cy", pos.y);
                values.put("world", worldId);
                values.put("map", mapId);
                values.put("scriptid", scriptId);
                values.put("dir", 1);  // default direction
                values.put("fh", map.getFootholds().findBelow(pos).getId());
                values.put("rx0", pos.x + 50);
                values.put("rx1", pos.x - 50);
                values.put("worldrank", runningWorldRank.get(worldId).getAndIncrement());
                values.put("overallrank", runningOverallRank.getAndIncrement());
                values.put("worldjobrank", getAndIncrementRunningWorldJobRanks(worldId, jobId));
                values.put("job", jobId);

                long rowId = con.insert("playernpcs", null, values);
                npcId = (int) rowId;

                try {
                    con.beginTransaction();
                    SQLiteStatement statement = con.compileStatement("INSERT INTO playernpcs_equip (npcid, equipid, equippos) VALUES (?, ?, ?)");

                    for (Item equip : chr.getInventory(InventoryType.EQUIPPED)) {
                        int position = Math.abs(equip.getPosition());
                        if ((position < 12 && position > 0) || (position > 100 && position < 112)) {
                            statement.clearBindings();
                            statement.bindLong(1, npcId);
                            statement.bindLong(2, equip.getItemId());
                            statement.bindLong(3, equip.getPosition());
                            statement.executeInsert();
                        }
                    }
                    con.setTransactionSuccessful();
                }  catch (SQLiteException e) {
                    log.error("insert into playernpcs_equip error", e); // Handle any potential SQL exceptions
                } finally {
                    con.endTransaction();
                }

                try (Cursor cursor = con.rawQuery("SELECT * FROM playernpcs WHERE id = ?", new String[]{String.valueOf(npcId)})) {
                    if (cursor.moveToFirst()) {
                        ret = new PlayerNPC(cursor);
                    }
                }
            }
            return ret;
        } catch (SQLiteException e) {
            log.error("createPlayerNPCInternal error", e);
            return null;
        }
    }

    private static List<Integer> removePlayerNPCInternal(MapleMap map, Character chr) {
        Set<Integer> updateMapids = new HashSet<>();

        List<Integer> mapids = new LinkedList<>();
        mapids.add(chr.getWorld());

        String selectQuery = "SELECT id, map FROM playernpcs WHERE name LIKE ?";
        String deleteQuery1 = "DELETE FROM playernpcs WHERE id = ?";
        String deleteQuery2 = "DELETE FROM playernpcs_equip WHERE npcid = ?";
        SQLiteDatabase con = DatabaseConnection.getConnection();
        try {
            try (Cursor cursor = con.rawQuery(selectQuery, new String[]{chr.getName()})) {
                if (cursor != null) {
                    while (cursor.moveToNext()) {
                        int mapIdx = cursor.getColumnIndex("map");
                        int idIdx = cursor.getColumnIndex("id");
                        if (mapIdx != -1 && idIdx != -1) {
                            updateMapids.add(cursor.getInt(mapIdx));
                            int npcId = cursor.getInt(idIdx);

                            // Delete rows from playernpcs table
                            con.execSQL(deleteQuery1, new String[]{String.valueOf(npcId)});

                            // Delete rows from playernpcs_equip table
                            con.execSQL(deleteQuery2, new String[]{String.valueOf(npcId)});
                        }
                    }
                }
            }
        } catch (SQLiteException e) {
            log.error("removePlayerNPCInternal error", e);
        }

        mapids.addAll(updateMapids);

        return mapids;
    }

    private static synchronized Pair<PlayerNPC, List<Integer>> processPlayerNPCInternal(MapleMap map, Point pos, Character chr, boolean create) {
        if (create) {
            return new Pair<>(createPlayerNPCInternal(map, pos, chr), null);
        } else {
            return new Pair<>(null, removePlayerNPCInternal(map, chr));
        }
    }

    public static boolean spawnPlayerNPC(int mapid, Character chr) {
        return spawnPlayerNPC(mapid, null, chr);
    }

    public static boolean spawnPlayerNPC(int mapid, Point pos, Character chr) {
        if (chr == null) {
            return false;
        }

        PlayerNPC pn = processPlayerNPCInternal(chr.getClient().getChannelServer().getMapFactory().getMap(mapid), pos, chr, true).getLeft();
        if (pn != null) {
            for (Channel channel : Server.getInstance().getChannelsFromWorld(chr.getWorld())) {
                MapleMap m = channel.getMapFactory().getMap(mapid);

                m.addPlayerNPCMapObject(pn);
                m.broadcastMessage(PacketCreator.spawnPlayerNPC(pn));
                m.broadcastMessage(PacketCreator.getPlayerNPC(pn));
            }

            return true;
        } else {
            return false;
        }
    }

    private static PlayerNPC getPlayerNPCFromWorldMap(String name, int world, int map) {
        World wserv = Server.getInstance().getWorld(world);
        for (MapObject pnpcObj : wserv.getChannel(1).getMapFactory().getMap(map).getMapObjectsInRange(new Point(0, 0), Double.POSITIVE_INFINITY, Arrays.asList(MapObjectType.PLAYER_NPC))) {
            PlayerNPC pn = (PlayerNPC) pnpcObj;

            if (name.contentEquals(pn.getName()) && pn.getScriptId() < NpcId.CUSTOM_DEV) {
                return pn;
            }
        }

        return null;
    }

    public static void removePlayerNPC(Character chr) {
        if (chr == null) {
            return;
        }

        List<Integer> updateMapids = processPlayerNPCInternal(null, null, chr, false).getRight();
        int worldid = updateMapids.remove(0);

        for (Integer mapid : updateMapids) {
            PlayerNPC pn = getPlayerNPCFromWorldMap(chr.getName(), worldid, mapid);

            if (pn != null) {
                for (Channel channel : Server.getInstance().getChannelsFromWorld(worldid)) {
                    MapleMap m = channel.getMapFactory().getMap(mapid);
                    m.removeMapObject(pn);

                    m.broadcastMessage(PacketCreator.removeNPCController(pn.getObjectId()));
                    m.broadcastMessage(PacketCreator.removePlayerNPC(pn.getObjectId()));
                }
            }
        }
    }

    public static void multicastSpawnPlayerNPC(int mapid, int world) {
        World wserv = Server.getInstance().getWorld(world);
        if (wserv == null) {
            return;
        }

        Client c = Client.createMock(Server.getInstance().getContext());
        c.setWorld(world);
        c.setChannel(1);

        for (Character mc : wserv.loadAndGetAllCharactersView()) {
            mc.setClient(c);
            spawnPlayerNPC(mapid, mc);
        }
    }

    public static void removeAllPlayerNPC() {
        SQLiteDatabase con = DatabaseConnection.getConnection();
        try (Cursor cursor = con.rawQuery("SELECT DISTINCT world, map FROM playernpcs", null)) {
            int wsize = Server.getInstance().getWorldsSize();

            while (cursor.moveToNext()) {
                int worldIdx = cursor.getColumnIndex("world");
                int mapIdx = cursor.getColumnIndex("map");
                if (worldIdx != -1 && mapIdx != -1) {
                    int world = cursor.getInt(worldIdx);
                    int map = cursor.getInt(mapIdx);

                    if (world >= wsize) {
                        continue;
                    }

                    for (Channel channel : Server.getInstance().getChannelsFromWorld(world)) {
                        MapleMap m = channel.getMapFactory().getMap(map);

                        for (MapObject pnpcObj : m.getMapObjectsInRange(new Point(0, 0), Double.POSITIVE_INFINITY, Arrays.asList(MapObjectType.PLAYER_NPC))) {
                            PlayerNPC pn = (PlayerNPC) pnpcObj;
                            m.removeMapObject(pnpcObj);
                            m.broadcastMessage(PacketCreator.removeNPCController(pn.getObjectId()));
                            m.broadcastMessage(PacketCreator.removePlayerNPC(pn.getObjectId()));
                        }
                    }
                }
            }

            con.execSQL("DELETE FROM playernpcs");
            con.execSQL("DELETE FROM playernpcs_equip");
            con.execSQL("DELETE FROM playernpcs_field");

            for (World w : Server.getInstance().getWorlds()) {
                w.resetPlayerNpcMapData();
            }
        } catch (SQLiteException e) {
            log.error("removeAllPlayerNPC error", e);
        }
    }
}
