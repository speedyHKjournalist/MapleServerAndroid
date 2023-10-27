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

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import tools.DatabaseConnection;
import tools.PacketCreator;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public final class MonsterBook {
    private int specialCard = 0;
    private int normalCard = 0;
    private int bookLevel = 1;
    private final Map<Integer, Integer> cards = new LinkedHashMap<>();
    private final Lock lock = new ReentrantLock();

    public Set<Entry<Integer, Integer>> getCardSet() {
        lock.lock();
        try {
            return new HashSet<>(cards.entrySet());
        } finally {
            lock.unlock();
        }
    }

    public void addCard(final Client c, final int cardid) {
        c.getPlayer().getMap().broadcastMessage(c.getPlayer(), PacketCreator.showForeignCardEffect(c.getPlayer().getId()), false);

        Integer qty;
        lock.lock();
        try {
            qty = cards.get(cardid);

            if (qty != null) {
                if (qty < 5) {
                    cards.put(cardid, qty + 1);
                }
            } else {
                cards.put(cardid, 1);
                qty = 0;

                if (cardid / 1000 >= 2388) {
                    specialCard++;
                } else {
                    normalCard++;
                }
            }
        } finally {
            lock.unlock();
        }

        if (qty < 5) {
            if (qty == 0) {     // leveling system only accounts unique cards
                calculateLevel();
            }

            c.sendPacket(PacketCreator.addCard(false, cardid, qty + 1));
            c.sendPacket(PacketCreator.showGainCard());
        } else {
            c.sendPacket(PacketCreator.addCard(true, cardid, 5));
        }
    }

    private void calculateLevel() {
        lock.lock();
        try {
            int collectionExp = (normalCard + specialCard);

            int level = 0, expToNextlevel = 1;
            do {
                level++;
                expToNextlevel += level * 10;
            } while (collectionExp >= expToNextlevel);

            bookLevel = level;  // thanks IxianMace for noticing book level differing between book UI and character info UI
        } finally {
            lock.unlock();
        }
    }

    public int getBookLevel() {
        lock.lock();
        try {
            return bookLevel;
        } finally {
            lock.unlock();
        }
    }

    public Map<Integer, Integer> getCards() {
        lock.lock();
        try {
            return Collections.unmodifiableMap(cards);
        } finally {
            lock.unlock();
        }
    }

    public int getTotalCards() {
        lock.lock();
        try {
            return specialCard + normalCard;
        } finally {
            lock.unlock();
        }
    }

    public int getNormalCard() {
        lock.lock();
        try {
            return normalCard;
        } finally {
            lock.unlock();
        }
    }

    public int getSpecialCard() {
        lock.lock();
        try {
            return specialCard;
        } finally {
            lock.unlock();
        }
    }

    public void loadCards(final int charid, SQLiteDatabase con) throws SQLiteException {
        lock.lock();
        try (Cursor ps = con.rawQuery("SELECT cardid, level FROM monsterbook WHERE charid = ? ORDER BY cardid ASC",
                     new String[]{String.valueOf(charid)})) {
            int cardid;
            int level;

            while (ps.moveToNext()) {
                int cardidIdx = ps.getColumnIndex("cardid");
                int levelIdx = ps.getColumnIndex("level");
                if (cardidIdx != -1 && levelIdx != -1) {
                    cardid = ps.getInt(cardidIdx);
                    level = ps.getInt(levelIdx);
                    if (cardid / 1000 >= 2388) {
                        specialCard++;
                    } else {
                        normalCard++;
                    }
                    cards.put(cardid, level);
                }
            }
        } finally {
            lock.unlock();
        }

        calculateLevel();
    }

    public void saveCards(SQLiteDatabase con, int chrId) throws SQLiteException {
        for (Entry<Integer, Integer> cardAndLevel : cards.entrySet()) {
            final int card = cardAndLevel.getKey();
            final int level = cardAndLevel.getValue();

            ContentValues values = new ContentValues();
            values.put("charid", chrId);
            values.put("cardid", card);
            values.put("level", level);

            con.insertWithOnConflict("monsterbook", null, values, SQLiteDatabase.CONFLICT_REPLACE);
        }
    }

    public static int[] getCardTierSize() {
        try (SQLiteDatabase con = DatabaseConnection.getConnection();
             Cursor rs = con.rawQuery("SELECT COUNT(*) FROM monstercarddata GROUP BY floor(cardid / 1000);", null)) {
            int rowCount = rs.getCount();
            int[] tierSizes = new int[rowCount];
            if (rs.moveToFirst()) {
                int index = 0;
                do {
                    tierSizes[index] = rs.getInt(0);
                    index++;
                } while (rs.moveToNext());
            }

            return tierSizes;
        } catch (SQLiteException e) {
            e.printStackTrace();
            return new int[0];
        }
    }
}
