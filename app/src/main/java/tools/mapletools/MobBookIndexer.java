package tools.mapletools;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import provider.wz.WZFiles;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * @author RonanLana
 * <p>
 * This application simply gets from the MonsterBook.img.xml all mobid's and
 * puts them on a SQL table with the correspondent mob cardid.
 */
public class MobBookIndexer {
    private static final Path INPUT_FILE = WZFiles.STRING.getFile().resolve("MonsterBook.img.xml");
    private static final SQLiteDatabase con = SimpleDatabaseConnection.getConnection();

    private static BufferedReader bufferedReader = null;
    private static byte status = 0;
    private static int mobId = -1;

    private static String getName(String token) {
        int i, j;
        char[] dest;
        String d;

        i = token.lastIndexOf("name");
        i = token.indexOf("\"", i) + 1; //lower bound of the string
        j = token.indexOf("\"", i);     //upper bound

        if (j - i < 7) {
            dest = new char[6];
        } else {
            dest = new char[7];
        }
        token.getChars(i, j, dest, 0);

        d = new String(dest);
        return (d);
    }

    private static void forwardCursor(int st) {
        String line = null;

        try {
            while (status >= st && (line = bufferedReader.readLine()) != null) {
                simpleToken(line);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void simpleToken(String token) {
        if (token.contains("/imgdir")) {
            status -= 1;
        } else if (token.contains("imgdir")) {
            status += 1;
        }
    }

    private static boolean isCard(int itemId) {
        return itemId / 10000 == 238;
    }

    private static void loadPairFromMob() {
        System.out.println("Loading mob id " + mobId);

        try {
            PreparedStatement ps, ps2;
            ResultSet rs;

            Cursor cursor1 = con.rawQuery("SELECT itemid FROM drop_data WHERE (dropperid = ? AND itemid > 0) GROUP BY itemid;",
                    new String[]{String.valueOf(mobId)});

            if (cursor1 != null) {
                try {
                    while (cursor1.moveToNext()) {
                        int itemidIdx = cursor1.getColumnIndex("itemid");
                        if (itemidIdx != -1) {
                            int itemId = cursor1.getInt(itemidIdx);
                            if (isCard(itemId)) {
                                ContentValues values = new ContentValues();
                                values.put("cardid", itemId);
                                values.put("mobid", mobId);
                                con.insert("monstercardwz", null, values);
                            }
                        }
                    }
                } finally {
                    cursor1.close();
                }
            }
        } catch (SQLiteException e) {
            e.printStackTrace();
        }
    }

    private static void translateToken(String token) {
        String d;
        int temp;

        if (token.contains("/imgdir")) {
            status -= 1;
        } else if (token.contains("imgdir")) {
            if (status == 1) {           //getting MobId
                d = getName(token);
                mobId = Integer.parseInt(d);
            } else if (status == 2) {
                d = getName(token);

                if (d.contains("reward")) {
                    temp = status;

                    loadPairFromMob();
                    forwardCursor(temp);
                }
            }

            status += 1;
        }

    }

    private static void indexFromDropData() {

		try (con; BufferedReader br = Files.newBufferedReader(INPUT_FILE);) {
			bufferedReader = br;
			String line = null;

            con.execSQL("DROP TABLE IF EXISTS monstercardwz;");

            con.execSQL("CREATE TABLE monstercardwz (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "cardid INTEGER NOT NULL DEFAULT -1," +
                    "mobid INTEGER NOT NULL DEFAULT -1" +
                    ");");

			while ((line = bufferedReader.readLine()) != null) {
				translateToken(line);
			}
		} catch (FileNotFoundException ex) {
			System.out.println("Unable to open file '" + INPUT_FILE + "'");
		} catch (IOException ex) {
			System.out.println("Error reading file '" + INPUT_FILE + "'");
		} catch (SQLiteException e) {
			System.out.println("Warning: Could not establish connection to database to change card chance rate.");
			System.out.println(e.getMessage());
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
    }

    public static void main(String[] args) {
        indexFromDropData();
    }
}

