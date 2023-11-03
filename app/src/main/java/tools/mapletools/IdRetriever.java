package tools.mapletools;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;

/**
 * @author RonanLana
 * <p>
 * This application acts two-way: first section sets up a table on the SQL Server with all the names used within MapleStory,
 * and the second queries all the names placed inside "fetch.txt", returning in the same line order the ids of the elements.
 * In case of multiple entries with the same name, multiple ids will be returned in the same line split by a simple space
 * in ascending order. An empty line means that no entry with the given name in a line has been found.
 * <p>
 * IMPORTANT: this will fail for fetching MAP ID (you shouldn't be using this program for these, just checking them up in the
 * handbook is enough anyway).
 * <p>
 * Set whether you are first installing the handbook on the SQL Server (TRUE) or just fetching whatever is on your "fetch_ids.txt"
 * file (FALSE) on the INSTALL_SQLTABLE property and build the project. With all done, run the Java executable.
 * <p>
 * Expected installing time: 30 minutes
 */
public class IdRetriever {
    private static final boolean INSTALL_SQLTABLE = true;
    private static final Path INPUT_FILE = ToolConstants.getInputFile("fetch_ids.txt");
    private static final Path OUTPUT_FILE = ToolConstants.getOutputFile("fetched_ids.txt");
    private static final SQLiteDatabase con = SimpleDatabaseConnection.getConnection();

    private static InputStreamReader fileReader = null;
    private static BufferedReader bufferedReader = null;

    private static void listFiles(String directoryName, ArrayList<File> files) {
        File directory = new File(directoryName);

        // get all the files from a directory
        File[] fList = directory.listFiles();
        for (File file : fList) {
            if (file.isFile()) {
                files.add(file);
            } else if (file.isDirectory()) {
                listFiles(file.getAbsolutePath(), files);
            }
        }
    }

    private static void parseMapleHandbookLine(String line) throws SQLiteException {
        String[] tokens = line.split(" - ", 3);

        if (tokens.length > 1) {
            ContentValues values = new ContentValues();

            try {
                values.put("id", Integer.parseInt(tokens[0]));
            } catch (NumberFormatException npe) {
                String num = tokens[0].substring(1);
                values.put("id", Integer.parseInt(num));
            }

            values.put("name", tokens[1]);
            values.put("description", tokens.length > 2 ? tokens[2] : "");
            con.insertOrThrow("handbook", null, values);
        }
    }

    private static void parseMapleHandbookFile(File fileObj) throws SQLiteException {
        if (shouldSkipParsingFile(fileObj.getName())) {
            return;
        }

        String line;

        try {
            fileReader = new InputStreamReader(new FileInputStream(fileObj), StandardCharsets.UTF_8);
            bufferedReader = new BufferedReader(fileReader);

            System.out.println("Parsing file '" + fileObj.getCanonicalPath() + "'.");

            while ((line = bufferedReader.readLine()) != null) {
                try {
                    parseMapleHandbookLine(line);
                } catch (SQLiteException e) {
                    System.err.println("Failed to parse line: " + line);
                    throw e;
                }
            }

            bufferedReader.close();
            fileReader.close();
        } catch (IOException ex) {
            System.out.println(ex.getMessage());
        }
    }

    // Quest.txt has different formatting: id is last token on the line, instead of the first
    private static boolean shouldSkipParsingFile(String fileName) {
        return "Quest.txt".equals(fileName);
    }

    private static void setupSqlTable() throws SQLiteException {
        con.execSQL("DROP TABLE IF EXISTS handbook;");

        String createTableQuery = "CREATE TABLE IF NOT EXISTS handbook ("
                + "_key INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "id INTEGER,"
                + "name TEXT,"
                + "description TEXT DEFAULT ''"
                + ");";
        con.execSQL(createTableQuery);
    }

    private static void parseMapleHandbook() throws SQLiteException {
        ArrayList<File> files = new ArrayList<>();

        listFiles(ToolConstants.HANDBOOK_PATH, files);
        if (files.isEmpty()) {
            return;
        }

        setupSqlTable();

        for (File f : files) {
            parseMapleHandbookFile(f);
        }
    }

    private static void fetchDataOnMapleHandbook() throws SQLiteException {
        try (BufferedReader br = Files.newBufferedReader(INPUT_FILE);
                PrintWriter printWriter = new PrintWriter(Files.newOutputStream(OUTPUT_FILE));) {
            bufferedReader = br;
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                if (line.isEmpty()) {
                    printWriter.println("");
                    continue;
                }

                try (Cursor cursor = con.rawQuery("SELECT id FROM handbook WHERE name LIKE ? ORDER BY id ASC;", new String[]{line})) {
                    StringBuilder str = new StringBuilder();
                    while (cursor.moveToNext()) {
                        int idIdx = cursor.getColumnIndex("id");
                        if (idIdx != -1) {
                            int id = cursor.getInt(idIdx);
                            str.append(id).append(" ");
                        }
                    }
                    printWriter.println(str);
                }

            }
        } catch (IOException ex) {
            System.out.println(ex.getMessage());
        }
    }

    public static void main(String[] args) {
        Instant instantStarted = Instant.now();
        try (con) {
            if (INSTALL_SQLTABLE) {
                parseMapleHandbook();
            } else {
                fetchDataOnMapleHandbook();
            }
        } catch (SQLiteException e) {
            System.out.println("Error: invalid SQL syntax");
            e.printStackTrace();
        }
        Instant instantStopped = Instant.now();
        Duration durationBetween = Duration.between(instantStarted, instantStopped);
        System.out.println("Get elapsed time in milliseconds: " + durationBetween.toMillis());
        System.out.println("Get elapsed time in seconds: " + (durationBetween.toMillis() / 1000));
    }
}

