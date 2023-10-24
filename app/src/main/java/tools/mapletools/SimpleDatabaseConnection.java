package tools.mapletools;

import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.config.Configurator;
import tools.DatabaseConnection;

import java.sql.Connection;
import java.sql.SQLException;

final class SimpleDatabaseConnection {
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
        Configurator.setLevel(LogManager.getLogger(com.zaxxer.hikari.HikariDataSource.class).getName(), minimumVisibleLevel);
        Configurator.setRootLevel(minimumVisibleLevel);
    }
}
