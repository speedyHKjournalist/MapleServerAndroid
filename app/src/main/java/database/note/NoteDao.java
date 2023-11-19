package database.note;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import database.DaoException;
import model.Note;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.DatabaseConnection;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class NoteDao {
    private static final Logger log = LoggerFactory.getLogger(NoteDao.class);

    public void save(Note note) {
        ContentValues values = new ContentValues();
        values.put("message", note.message());
        values.put("from", note.from());
        values.put("to", note.to());
        values.put("timestamp", note.timestamp());
        values.put("fame", note.fame());
        values.put("deleted", 0);
        SQLiteDatabase con = DatabaseConnection.getConnection();
        try {
            long newRowId = con.insert("notes", null, values);
            if (newRowId == -1) {
                log.error("Failed to save note");
                throw new SQLiteException("Failed to save note");
            }
        } catch (SQLiteException e) {
            log.error("Failed to save note: %s" + note.toString(), e);
            throw new DaoException("Failed to save note: %s" + note.toString(), e);
        }
    }

    public List<Note> findAllByTo(String to) {
        List<Note> notes = new ArrayList<>();
        SQLiteDatabase con = DatabaseConnection.getConnection();
        try (Cursor cursor = con.rawQuery("SELECT * FROM notes WHERE `deleted` = 0 AND `to` = ?", new String[]{to})) {
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    int idIdx = cursor.getColumnIndex("_id");
                    int toIdx = cursor.getColumnIndex("to");
                    int fromIdx = cursor.getColumnIndex("from");
                    int messageIdx = cursor.getColumnIndex("message");
                    int timestampIdx = cursor.getColumnIndex("timestamp");
                    int fameIdx = cursor.getColumnIndex("fame");

                    Note note = new Note(cursor.getInt(idIdx),
                            cursor.getString(messageIdx),
                            cursor.getString(fromIdx),
                            cursor.getString(toIdx),
                            Timestamp.valueOf(cursor.getString(timestampIdx)).getTime(),
                        cursor.getInt(fameIdx));

                    notes.add(note);
                }
            }
            return notes;
        } catch (SQLiteException e) {
            log.error("Failed to find notes sent to: %s" + to, e);
            throw new DaoException("Failed to find notes sent to: %s" + to, e);
        }
    }

    public Optional<Note> delete(int id) {
        SQLiteDatabase con = DatabaseConnection.getConnection();
        try {
            Optional<Note> note = findById(con, id);
            if (!note.isPresent()) {
                return Optional.empty();
            }
            deleteById(con, id);

            return note;
        } catch (SQLiteException e) {
            log.error("Failed to delete note with id: %d" + id, e);
            throw new DaoException("Failed to delete note with id: %d" + id, e);
        }
    }

    private Optional<Note> findById(SQLiteDatabase con, int id) {
        final Optional<Note> note;

        String[] columns = {"_id", "to", "from", "message", "timestamp", "fame", "deleted"};
        String selection = "id = ? AND deleted = ?";
        String[] selectionArgs = {String.valueOf(id), "0"};

        try (Cursor cursor = con.query("notes", columns, selection, selectionArgs, null, null, null)) {
            if (cursor.moveToFirst()) {
                int idIdx = cursor.getColumnIndex("_id");
                int toIdx = cursor.getColumnIndex("to");
                int fromIdx = cursor.getColumnIndex("from");
                int messageIdx = cursor.getColumnIndex("message");
                int timestampIdx = cursor.getColumnIndex("timestamp");
                int fameIdx = cursor.getColumnIndex("fame");

                note = Optional.of(new Note(
                        cursor.getInt(idIdx),
                        cursor.getString(messageIdx),
                        cursor.getString(fromIdx),
                        cursor.getString(toIdx),
                        Timestamp.valueOf(cursor.getString(timestampIdx)).getTime(),
                        cursor.getInt(fameIdx)));
                return note;
            }

        } catch (SQLiteException e) {
            log.error("Failed find note with id %s" + id, e);
            throw new DaoException("Failed find note with id %s" + id, e);
        }
        return Optional.empty();
    }

    private void deleteById(SQLiteDatabase con, int id) {
        String whereClause = "id = ?";
        String[] whereArgs = {String.valueOf(id)};
        ContentValues values = new ContentValues();
        values.put("deleted", 1);

        try {
            int affectedRows = con.update("notes", values, whereClause, whereArgs);
            if (affectedRows == 0) {
                throw new SQLiteException("Note with id " + id + " not found or could not be deleted");
            }
        } catch (SQLiteException e) {
            log.error("Failed to delete note with id %d" + id, e);
            throw new DaoException("Failed to delete note with id %d" + id, e);
        }
    }
}
