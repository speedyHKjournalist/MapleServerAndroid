package server;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import config.YamlConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.DatabaseConnection;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;

import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.concurrent.TimeUnit.SECONDS;

public class ExpLogger {
    private static final LinkedBlockingQueue<ExpLogRecord> expLoggerQueue = new LinkedBlockingQueue<>();
    private static final short EXP_LOGGER_THREAD_SLEEP_DURATION_SECONDS = 60;
    private static final short EXP_LOGGER_THREAD_SHUTDOWN_WAIT_DURATION_MINUTES = 5;
    private static final Logger log = LoggerFactory.getLogger(ExpLogger.class);

    public record ExpLogRecord(int worldExpRate, int expCoupon, long gainedExp, int currentExp,Timestamp expGainTime, int charid) {}

    public static void putExpLogRecord(ExpLogRecord expLogRecord) {
        try {
            expLoggerQueue.put(expLogRecord);
        } catch (InterruptedException e) {
            log.error("putExpLogRecord error", e);
        }
    }

    static private ScheduledExecutorService schdExctr = Executors.newSingleThreadScheduledExecutor(new ThreadFactory() {
        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r);
            t.setPriority(Thread.MIN_PRIORITY);
            return t;
        }
    });

    private static Runnable saveExpLoggerToDBRunnable = new Runnable() {
        @Override
        public void run() {
            SQLiteDatabase con = DatabaseConnection.getConnection();
            try {
                List<ExpLogRecord> drainedExpLogs = new ArrayList<>();
                expLoggerQueue.drainTo(drainedExpLogs);

                con.beginTransaction();
                for (ExpLogRecord expLogRecord : drainedExpLogs) {
                    ContentValues values = new ContentValues();
                    values.put("world_exp_rate", expLogRecord.worldExpRate);
                    values.put("exp_coupon", expLogRecord.expCoupon);
                    values.put("gained_exp", expLogRecord.gainedExp);
                    values.put("current_exp", expLogRecord.currentExp);
                    values.put("exp_gain_time", expLogRecord.expGainTime.getTime()); // Assuming expGainTime is a Date object
                    values.put("charid", expLogRecord.charid);

                    con.insert("characterexplogs", null, values);
                }
                con.setTransactionSuccessful();
            } catch (SQLiteException sqle) {
                log.error("saveExpLoggerToDBRunnable error", sqle);
            } finally {
                con.endTransaction();
            }
        }
    };


    private static void startExpLogger() {
        schdExctr.scheduleWithFixedDelay(saveExpLoggerToDBRunnable, EXP_LOGGER_THREAD_SLEEP_DURATION_SECONDS, EXP_LOGGER_THREAD_SLEEP_DURATION_SECONDS, SECONDS);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            stopExpLogger();
        }));
    }

    private static boolean stopExpLogger() {
        schdExctr.shutdown();
        try {
            schdExctr.awaitTermination(EXP_LOGGER_THREAD_SHUTDOWN_WAIT_DURATION_MINUTES, MINUTES);
            Thread runThreadBeforeShutdown = new Thread(saveExpLoggerToDBRunnable);
            runThreadBeforeShutdown.setPriority(Thread.MIN_PRIORITY);
            runThreadBeforeShutdown.start();
            return true;
        } catch (InterruptedException e) {
            log.error("stopExpLogger error", e);
            return false;
        }
    }

    static {
        if (YamlConfig.config.server.USE_EXP_GAIN_LOG) {
            startExpLogger();
        }
    }
}