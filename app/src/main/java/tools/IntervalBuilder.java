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
package tools;

import android.graphics.RectF;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * @author Ronan
 */
public class IntervalBuilder {
    private final List<RectF> intervalLimits = new ArrayList<>();
    private final Lock intervalRlock;
    private final Lock intervalWlock;

    public IntervalBuilder() {
        ReadWriteLock readWriteLock = new ReentrantReadWriteLock(true);
        this.intervalRlock = readWriteLock.readLock();
        this.intervalWlock = readWriteLock.writeLock();
    }

    private void refitOverlappedIntervals(int st, int en, int newFrom, int newTo) {
        List<RectF> checkLimits = new ArrayList<>(intervalLimits.subList(st, en));

        float newLimitX1, newLimitX2;
        if (!checkLimits.isEmpty()) {
            RectF firstLimit = checkLimits.get(0);
            RectF lastLimit = checkLimits.get(checkLimits.size() - 1);

            newLimitX1 = Math.min(newFrom, firstLimit.left);
            newLimitX2 = Math.max(newTo, lastLimit.right);

            for (RectF limit : checkLimits) {
                intervalLimits.remove(st);
            }
        } else {
            newLimitX1 = newFrom;
            newLimitX2 = newTo;
        }

        intervalLimits.add(st, new RectF(newLimitX1, 0, newLimitX2, 0));
    }

    private int bsearchInterval(int point) {
        int st = 0, en = intervalLimits.size() - 1;

        int mid, idx;
        while (en >= st) {
            idx = (st + en) / 2;
            mid = (int) intervalLimits.get(idx).left;

            if (mid == point) {
                return idx;
            } else if (mid < point) {
                st = idx + 1;
            } else {
                en = idx - 1;
            }
        }

        return en;
    }

    public void addInterval(int from, int to) {
        intervalWlock.lock();
        try {
            int st = bsearchInterval(from);
            if (st < 0) {
                st = 0;
            } else if (intervalLimits.get(st).right < from) {
                st += 1;
            }

            int en = bsearchInterval(to);
            if (en < st) {
                en = st - 1;
            }

            refitOverlappedIntervals(st, en + 1, from, to);
        } finally {
            intervalWlock.unlock();
        }
    }

    public boolean inInterval(int point) {
        return inInterval(point, point);
    }

    public boolean inInterval(int from, int to) {
        intervalRlock.lock();
        try {
            int idx = bsearchInterval(from);
            return idx >= 0 && to <= intervalLimits.get(idx).right;
        } finally {
            intervalRlock.unlock();
        }
    }

    public void clear() {
        intervalWlock.lock();
        try {
            intervalLimits.clear();
        } finally {
            intervalWlock.unlock();
        }
    }

}
