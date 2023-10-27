package tools.mapletools;

import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import org.apache.log4j.LogManager;
import org.apache.log4j.Level;
import de.mindpipe.android.logging.log4j.LogConfigurator;
import tools.DatabaseConnection;

import java.sql.Connection;
import java.sql.SQLException;

final class SimpleDatabaseConnection {
    private static LogConfigurator config = null;
    private SimpleDatabaseConnection() {}

    static SQLiteDatabase getConnection() {
        muffleLogging();
        DatabaseConnection.initializeConnectionPool();

        try {
            return DatabaseConnection.getConnection();
        } catch (SQLiteException e) {
            throw new IllegalStateException("Failed to get database connection", e);
        }
    }

    private static void muffleLogging() {
        final Level minimumVisibleLevel = Level.WARN;
        if (config == null) {
            config = new LogConfigurator();
            config.setLevel(LogManager.getLogger(com.zaxxer.hikari.HikariDataSource.class).getName(), minimumVisibleLevel);
            config.setRootLevel(minimumVisibleLevel);
        }
    }
}
