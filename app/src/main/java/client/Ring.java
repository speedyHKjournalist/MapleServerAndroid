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
package client;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import client.inventory.manipulator.CashIdGenerator;
import tools.DatabaseConnection;
import tools.Pair;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * @author Danny
 */
public class Ring implements Comparable<Ring> {
    private final int ringId;
    private final int ringId2;
    private final int partnerId;
    private final int itemId;
    private final String partnerName;
    private boolean equipped = false;

    public Ring(int id, int id2, int partnerId, int itemid, String partnername) {
        this.ringId = id;
        this.ringId2 = id2;
        this.partnerId = partnerId;
        this.itemId = itemid;
        this.partnerName = partnername;
    }

    public static Ring loadFromDb(int ringId) {
        Ring ret = null;
        SQLiteDatabase con = DatabaseConnection.getConnection();
        try (Cursor cursor = con.rawQuery("SELECT * FROM rings WHERE id = ?", new String[]{String.valueOf(ringId)})) {

            int partnerRingIdIdx = cursor.getColumnIndex("partnerRingId");
            int partnerChrIdIdx = cursor.getColumnIndex("partnerChrId");
            int itemidIdx = cursor.getColumnIndex("itemid");
            int partnerNameIdx = cursor.getColumnIndex("partnerName");

            if (partnerRingIdIdx != -1 &&
                    partnerChrIdIdx != -1 &&
                    itemidIdx != -1 &&
                    partnerNameIdx != -1) {
                if (cursor.moveToFirst()) {
                    ret = new Ring(ringId, cursor.getInt(partnerRingIdIdx), cursor.getInt(partnerChrIdIdx), cursor.getInt(itemidIdx), cursor.getString(partnerNameIdx));
                }
            }

            return ret;
        } catch (SQLiteException ex) {
            ex.printStackTrace();
            return null;
        }
    }

    public static void removeRing(final Ring ring) {
        if (ring == null) {
            return;
        }

        SQLiteDatabase con = DatabaseConnection.getConnection();
        try {
                con.beginTransaction();
                con.execSQL("DELETE FROM rings WHERE id=?", new Object[]{ring.getRingId()});
                con.execSQL("DELETE FROM rings WHERE id=?", new Object[]{ring.getPartnerRingId()});

                CashIdGenerator.freeCashId(ring.getRingId());
                CashIdGenerator.freeCashId(ring.getPartnerRingId());

                con.execSQL("UPDATE inventoryequipment SET ringid=-1 WHERE ringid=?", new Object[]{ring.getRingId()});
                con.execSQL("UPDATE inventoryequipment SET ringid=-1 WHERE ringid=?", new Object[]{ring.getPartnerRingId()});
                con.setTransactionSuccessful();
        } catch (SQLiteException ex) {
            ex.printStackTrace();
        } finally {
            con.endTransaction();
            con.close();
        }
    }

    public static Pair<Integer, Integer> createRing(int itemid, final Character partner1, final Character partner2) {
        if (partner1 == null) {
            return new Pair<>(-3, -3);
        } else if (partner2 == null) {
            return new Pair<>(-2, -2);
        }
        SQLiteDatabase con = DatabaseConnection.getConnection();
        try {
            con.beginTransaction();
            int[] ringID = new int[2];
            ringID[0] = CashIdGenerator.generateCashId();
            ringID[1] = CashIdGenerator.generateCashId();
            con.execSQL("INSERT INTO rings (id, itemid, partnerRingId, partnerChrId, partnername) VALUES (?, ?, ?, ?, ?)",
                    new Object[]{ringID[0], itemid, ringID[1], partner2.getId(), partner2.getName()});

            con.execSQL("INSERT INTO rings (id, itemid, partnerRingId, partnerChrId, partnername) VALUES (?, ?, ?, ?, ?)",
                    new Object[]{ringID[1], itemid, ringID[0], partner1.getId(), partner1.getName()});
            con.setTransactionSuccessful();
            con.endTransaction();
            con.close();
            return new Pair<>(ringID[0], ringID[1]);
        } catch (SQLiteException ex) {
            con.endTransaction();
            con.close();
            ex.printStackTrace();
            return new Pair<>(-1, -1);
        }
    }

    public int getRingId() {
        return ringId;
    }

    public int getPartnerRingId() {
        return ringId2;
    }

    public int getPartnerChrId() {
        return partnerId;
    }

    public int getItemId() {
        return itemId;
    }

    public String getPartnerName() {
        return partnerName;
    }

    public boolean equipped() {
        return equipped;
    }

    public void equip() {
        this.equipped = true;
    }

    public void unequip() {
        this.equipped = false;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof Ring ring) {
            return ring.getRingId() == getRingId();
        }
        return false;
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 53 * hash + this.ringId;
        return hash;
    }

    @Override
    public int compareTo(Ring other) {
        if (ringId < other.getRingId()) {
            return -1;
        } else if (ringId == other.getRingId()) {
            return 0;
        }
        return 1;
    }
}
