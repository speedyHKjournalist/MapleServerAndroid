package net.server.task;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import client.Family;
import net.server.world.World;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.DatabaseConnection;

import java.util.Calendar;

public class FamilyDailyResetTask implements Runnable {
    private static final Logger log = LoggerFactory.getLogger(FamilyDailyResetTask.class);
    private final World world;
    private final Context context;

    public FamilyDailyResetTask(World world, Context context) {
        this.world = world;
        this.context = context;
    }

    @Override
    public void run() {
        resetEntitlementUsage(world, context);
        for (Family family : world.getFamilies()) {
            family.resetDailyReps();
        }
    }

    public static void resetEntitlementUsage(World world, Context context) {
        Calendar resetTime = Calendar.getInstance();
        resetTime.add(Calendar.MINUTE, 1); // to make sure that we're in the "next day", since this is called at midnight
        resetTime.set(Calendar.HOUR_OF_DAY, 0);
        resetTime.set(Calendar.MINUTE, 0);
        resetTime.set(Calendar.SECOND, 0);
        resetTime.set(Calendar.MILLISECOND, 0);
        SQLiteDatabase con = DatabaseConnection.getConnection();
        try {
            String whereClause = "lastresettime <= ?";
            String[] whereArgs = {String.valueOf(resetTime.getTimeInMillis())};
            ContentValues values = new ContentValues();
            values.put("todaysrep", 0);
            values.put("reptosenior", 0);
            con.update("family_character", values, whereClause, whereArgs);

            whereClause = "timestamp <= ?";
            con.delete("family_entitlement", whereClause, whereArgs);
        } catch (SQLiteException e) {
            log.error("Could not get connection to DB", e);
        }
    }
}
