package database;

import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import net.server.Server;

import java.io.*;

public class MapleDBHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "cosmic";
    private static String DATABASE_PATH = "";
    private static final int DATABASE_VERSION = 1;
    private static MapleDBHelper sInstance;
    private MapleDBHelper() {
        super(Server.getInstance().getContext(), DATABASE_NAME, null, DATABASE_VERSION);
        DATABASE_PATH = Server.getInstance().getContext().getDatabasePath(DATABASE_NAME).getPath();
    }

    public void createDataBase() throws IOException {
        //If the database does not exist, copy it from the assets.
        boolean mDataBaseExist = checkDataBase();
        if(!mDataBaseExist)
        {
            try {
                //Copy the database from assests
                copyDataBase();
                mDataBaseExist = true;
            } catch (IOException mIOException) {
                mIOException.printStackTrace();
                throw new Error("ErrorCopyingDataBase");
            }
        }
    }

    private boolean checkDataBase() {
        File dbFile = Server.getInstance().getContext().getDatabasePath(DATABASE_NAME);
        return dbFile.exists();
    }

    private void copyDataBase() throws IOException {
        InputStream mInput = Server.getInstance().getContext().getAssets().open("sql/cosmic");
        String outFileName = DATABASE_PATH;
        OutputStream mOutput = new FileOutputStream(outFileName);
        byte[] mBuffer = new byte[1024];
        int mLength;
        while ((mLength = mInput.read(mBuffer))>0)
        {
            mOutput.write(mBuffer, 0, mLength);
        }
        mOutput.flush();
        mOutput.close();
        mInput.close();
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
    }
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    }

    public static synchronized MapleDBHelper getInstance() {
        // Use the application context, which will ensure that you
        // don't accidentally leak an Activity's context.
        // See this article for more information: http://bit.ly/6LRzfx
        if (sInstance == null) {
            try {
                sInstance = new MapleDBHelper();
                sInstance.createDataBase();
            } catch (IOException mIOException) {
                mIOException.printStackTrace();
            }
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
