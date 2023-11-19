package tools;

import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import database.MapleDBHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Frz (Big Daddy)
 * @author The Real Spookster - some modifications to this beautiful code
 * @author Ronan - some connection pool to this beautiful code
 */
public class DatabaseConnection {
    private static final Logger log = LoggerFactory.getLogger(DatabaseConnection.class);
    private static final SQLiteDatabase dbConnection = MapleDBHelper.getInstance().getWritableDatabase();
    public static synchronized SQLiteDatabase getConnection() throws SQLiteException {
        if (dbConnection == null) {
            throw new IllegalStateException("Unable to get connection - connection pool is uninitialized");
        }
        return dbConnection;
    }
    public static void initializeConnectionPool() {

    }
}
