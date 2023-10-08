package database;

import android.content.Context;
import android.content.res.AssetManager;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.io.*;

public class MapleDBHelper extends SQLiteOpenHelper {
    public Context context;
    private static final String DATABASE_NAME = "cosmic";
    private static final int DATABASE_VERSION = 1;
    private static MapleDBHelper sInstance;
    private MapleDBHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        this.context = context;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        File databaseFile = context.getDatabasePath(DATABASE_NAME);
        if (databaseFile.exists()) {
            // Delete the existing database file
            SQLiteDatabase.deleteDatabase(databaseFile);
        }

        try {
            AssetManager assetManager = context.getAssets();
            InputStream db_database = assetManager.open("sql/1_db_database.sql");
            InputStream db_drops = assetManager.open("sql/2_db_drops.sql");
            InputStream db_shopupdate = assetManager.open("sql/3_db_shopupdate.sql");
            InputStream db_admin = assetManager.open("sql/4_db-admin.sql");
            db.execSQL(convertInputStreamToString(db_database));
            db.execSQL(convertInputStreamToString(db_drops));
            db.execSQL(convertInputStreamToString(db_shopupdate));
            db.execSQL(convertInputStreamToString(db_admin));

            db_database.close();
            db_drops.close();
            db_shopupdate.close();
            db_admin.close();
        } catch (FileNotFoundException e) {
            throw new RuntimeException("Could not read database file : " + e.getMessage());
        } catch (IOException e) {
            throw new RuntimeException("Could not successfully parse database file " + e.getMessage());
        }
    }
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    }

    public static synchronized MapleDBHelper getInstance(Context context) {
        // Use the application context, which will ensure that you
        // don't accidentally leak an Activity's context.
        // See this article for more information: http://bit.ly/6LRzfx
        if (sInstance == null) {
            sInstance = new MapleDBHelper(context.getApplicationContext());
        }
        return sInstance;
    }

    public static String convertInputStreamToString(InputStream inputStream) throws IOException {
        StringBuilder stringBuilder = new StringBuilder();
        String line;

        try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream))) {
            while ((line = bufferedReader.readLine()) != null) {
                stringBuilder.append(line);
            }
        }

        return stringBuilder.toString();
    }
}
