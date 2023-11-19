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
package client.inventory;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.graphics.Point;
import client.Character;
import client.inventory.manipulator.CashIdGenerator;
import constants.game.ExpTable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import server.ItemInformationProvider;
import server.movement.AbsoluteLifeMovement;
import server.movement.LifeMovement;
import server.movement.LifeMovementFragment;
import tools.DatabaseConnection;
import tools.PacketCreator;
import tools.Pair;

import java.util.List;

/**
 * @author Matze
 */
public class Pet extends Item {
    private String name;
    private int uniqueid;
    private int tameness = 0;
    private byte level = 1;
    private int fullness = 100;
    private int Fh;
    private Point pos;
    private int stance;
    private boolean summoned;
    private int petAttribute = 0;
    private static final Logger log = LoggerFactory.getLogger(Pet.class);

    public enum PetAttribute {
        OWNER_SPEED(0x01);

        private final int i;

        PetAttribute(int i) {
            this.i = i;
        }

        public int getValue() {
            return i;
        }
    }

    private Pet(int id, short position, int uniqueid) {
        super(id, position, (short) 1);
        this.uniqueid = uniqueid;
        this.pos = new Point(0, 0);
    }

    public static Pet loadFromDb(int itemid, short position, int petid) {
        Pet ret = new Pet(itemid, position, petid);
        SQLiteDatabase con = DatabaseConnection.getConnection();
        try (Cursor cursor = con.rawQuery("SELECT name, level, closeness, fullness, summoned, flag FROM pets WHERE petid = ?",
                new String[]{String.valueOf(petid)})) { // Get the pet details...
            if (cursor.moveToFirst()) {
                int nameIdx = cursor.getColumnIndex("name");
                int closenessIdx = cursor.getColumnIndex("closeness");
                int levelIdx = cursor.getColumnIndex("level");
                int fullnessIdx = cursor.getColumnIndex("fullness");
                int summonedIdx = cursor.getColumnIndex("summoned");
                int flagIdx = cursor.getColumnIndex("flag");

                if (nameIdx != -1 && closenessIdx >= 0 && levelIdx >= 0 && fullnessIdx >= 0 && summonedIdx >= 0 && flagIdx >= 0) {
                    ret.setName(cursor.getString(nameIdx));
                    ret.setTameness(Math.min(cursor.getInt(closenessIdx), 30000));
                    ret.setLevel((byte) Math.min((byte) cursor.getInt(levelIdx), 30));
                    ret.setFullness(Math.min(cursor.getInt(fullnessIdx), 100));
                    ret.setSummoned(cursor.getInt(summonedIdx) == 1);
                    ret.setPetAttribute(cursor.getInt(flagIdx));
                }
            }
            return ret;
        } catch (SQLiteException e) {
            log.error("loadFromDb error", e);
            return null;
        }
    }

    public static void deleteFromDb(Character owner, int petid) {
        SQLiteDatabase con = DatabaseConnection.getConnection();
        String whereClause = "`petid` = ?";
        String[] whereArgs = {String.valueOf(petid)};
        try {
            con.delete("pets", whereClause, whereArgs);
            // thanks Vcoc for detecting petignores remaining after deletion
            owner.resetExcluded(petid);
            CashIdGenerator.freeCashId(petid);
        } catch (SQLiteException ex) {
            log.error("deleteFromDb error", ex);
        }
    }

    public void saveToDb() {
        SQLiteDatabase con = DatabaseConnection.getConnection();
        try {
             con.execSQL("UPDATE pets SET name = ?, level = ?, closeness = ?, fullness = ?, summoned = ?, flag = ? WHERE petid = ?", new String[]{
                     getName(),
                     String.valueOf(getLevel()),
                     String.valueOf(getTameness()),
                     String.valueOf(getFullness()),
                     isSummoned() ? "1" : "0",
                     String.valueOf(getPetAttribute()),
                     String.valueOf(getUniqueId())
             });
        } catch (SQLiteException e) {
            log.error("saveToDb error", e);
        }
    }

    public static int createPet(int itemid) {
        SQLiteDatabase con = DatabaseConnection.getConnection();
        try {
            int ret = CashIdGenerator.generateCashId();
             con.execSQL("INSERT INTO pets (petid, name, level, closeness, fullness, summoned, flag) VALUES (?, ?, 1, 0, 100, 0, 0)", new String[]{
                     String.valueOf(ret),
                     ItemInformationProvider.getInstance().getName(itemid)
             });
            return ret;
        } catch (SQLiteException e) {
            log.error("createPet error", e);
            return -1;
        }
    }

    public static int createPet(int itemid, byte level, int tameness, int fullness) {
        SQLiteDatabase con = DatabaseConnection.getConnection();
        try {
            int ret = CashIdGenerator.generateCashId();
            con.execSQL("INSERT INTO pets (petid, name, level, closeness, fullness, summoned, flag) VALUES (?, ?, ?, ?, ?, 0, 0)", new String[]{
                    String.valueOf(ret),
                    ItemInformationProvider.getInstance().getName(itemid),
                    String.valueOf(level),
                    String.valueOf(tameness),
                    String.valueOf(fullness)
            });
            return ret;
        } catch (SQLiteException e) {
            log.error("createPet error", e);
            return -1;
        }
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getUniqueId() {
        return uniqueid;
    }

    public void setUniqueId(int id) {
        this.uniqueid = id;
    }

    public int getTameness() {
        return tameness;
    }

    public void setTameness(int tameness) {
        this.tameness = tameness;
    }

    public byte getLevel() {
        return level;
    }

    public void gainTamenessFullness(Character owner, int incTameness, int incFullness, int type) {
        gainTamenessFullness(owner, incTameness, incFullness, type, false);
    }

    public void gainTamenessFullness(Character owner, int incTameness, int incFullness, int type, boolean forceEnjoy) {
        byte slot = owner.getPetIndex(this);
        boolean enjoyed;

        //will NOT increase pet's tameness if tried to feed pet with 100% fullness
        // unless forceEnjoy == true (cash shop)
        if (fullness < 100 || incFullness == 0 || forceEnjoy) {   //incFullness == 0: command given
            int newFullness = fullness + incFullness;
            if (newFullness > 100) {
                newFullness = 100;
            }
            fullness = newFullness;

            if (incTameness > 0 && tameness < 30000) {
                int newTameness = tameness + incTameness;
                if (newTameness > 30000) {
                    newTameness = 30000;
                }

                tameness = newTameness;
                while (newTameness >= ExpTable.getTamenessNeededForLevel(level)) {
                    level += 1;
                    owner.sendPacket(PacketCreator.showOwnPetLevelUp(slot));
                    owner.getMap().broadcastMessage(PacketCreator.showPetLevelUp(owner, slot));
                }
            }

            enjoyed = true;
        } else {
            int newTameness = tameness - 1;
            if (newTameness < 0) {
                newTameness = 0;
            }

            tameness = newTameness;
            if (level > 1 && newTameness < ExpTable.getTamenessNeededForLevel(level - 1)) {
                level -= 1;
            }

            enjoyed = false;
        }

        owner.getMap().broadcastMessage(PacketCreator.petFoodResponse(owner.getId(), slot, enjoyed, false));
        saveToDb();

        Item petz = owner.getInventory(InventoryType.CASH).getItem(getPosition());
        if (petz != null) {
            owner.forceUpdateItem(petz);
        }
    }

    public void setLevel(byte level) {
        this.level = level;
    }

    public int getFullness() {
        return fullness;
    }

    public void setFullness(int fullness) {
        this.fullness = fullness;
    }

    public int getFh() {
        return Fh;
    }

    public void setFh(int Fh) {
        this.Fh = Fh;
    }

    public Point getPos() {
        return pos;
    }

    public void setPos(Point pos) {
        this.pos = pos;
    }

    public int getStance() {
        return stance;
    }

    public void setStance(int stance) {
        this.stance = stance;
    }

    public boolean isSummoned() {
        return summoned;
    }

    public void setSummoned(boolean yes) {
        this.summoned = yes;
    }

    public int getPetAttribute() {
        return this.petAttribute;
    }

    private void setPetAttribute(int flag) {
        this.petAttribute = flag;
    }

    public void addPetAttribute(Character owner, PetAttribute flag) {
        this.petAttribute |= flag.getValue();
        saveToDb();

        Item petz = owner.getInventory(InventoryType.CASH).getItem(getPosition());
        if (petz != null) {
            owner.forceUpdateItem(petz);
        }
    }

    public void removePetAttribute(Character owner, PetAttribute flag) {
        this.petAttribute &= 0xFFFFFFFF ^ flag.getValue();
        saveToDb();

        Item petz = owner.getInventory(InventoryType.CASH).getItem(getPosition());
        if (petz != null) {
            owner.forceUpdateItem(petz);
        }
    }

    public Pair<Integer, Boolean> canConsume(int itemId) {
        return ItemInformationProvider.getInstance().canPetConsume(this.getItemId(), itemId);
    }

    public void updatePosition(List<LifeMovementFragment> movement) {
        for (LifeMovementFragment move : movement) {
            if (move instanceof LifeMovement) {
                if (move instanceof AbsoluteLifeMovement) {
                    this.setPos(move.getPosition());
                }
                this.setStance(((LifeMovement) move).getNewstate());
            }
        }
    }
}