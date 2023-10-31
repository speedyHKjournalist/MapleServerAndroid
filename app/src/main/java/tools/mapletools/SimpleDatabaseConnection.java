package tools.mapletools;

import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import tools.DatabaseConnection;

final class SimpleDatabaseConnection {
    private SimpleDatabaseConnection() {}

    static SQLiteDatabase getConnection() {
//        muffleLogging();
        DatabaseConnection.initializeConnectionPool();

        try {
            return DatabaseConnection.getConnection();
        } catch (SQLiteException e) {
            throw new IllegalStateException("Failed to get database connection", e);
        }
    }

//    private static void muffleLogging() {
//        LoggerFactory.getLogger(com.zaxxer.hikari.HikariDataSource.class);
//    }
}
