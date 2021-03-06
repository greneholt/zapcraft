/*******************************************************************************
 * Copyright (C) 2012 Team ZapCraft, Colorado School of Mines
 * 
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 * 
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY
 * CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 ******************************************************************************/
package edu.mines.zapcraft.FuelBehavior;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class DbAdapter {
    private static final String TAG = DbAdapter.class.getSimpleName();
    private DatabaseHelper mDbHelper;
    private SQLiteDatabase mDb;

    private static final String DRIVES_TABLE_CREATE =
        "CREATE TABLE drives (" +
		"	_id INTEGER PRIMARY KEY AUTOINCREMENT," +
		"	start_time INTEGER" +
		")";

    private static final String LOGS_TABLE_CREATE =
		"CREATE TABLE logs (" +
		"	_id INTEGER PRIMARY KEY AUTOINCREMENT," +
		"	drive_id INTEGER REFERENCES drives(_id)," +
		"	time INTEGER NOT NULL," +
		"	latitude REAL NOT NULL," +
		"	longitude REAL NOT NULL," +
		"	altitude REAL," +
		"	gpsSpeed REAL NOT NULL," +
		"	course REAL NOT NULL," +
		"	rpm INTEGER NOT NULL," +
		"	throttle INTEGER NOT NULL," +
		"	mpg REAL NOT NULL," +
		"	obd2Speed INTEGER NOT NULL," +
		"	xAccel REAL NOT NULL," +
		"	yAccel REAL NOT NULL," +
		"	zAccel REAL NOT NULL" +
		")";

    private static final String DATABASE_NAME = "data";
    private static final int DATABASE_VERSION = 1;

    private final Context mCtx;

    private static class DatabaseHelper extends SQLiteOpenHelper {
        DatabaseHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL(DRIVES_TABLE_CREATE);
            db.execSQL(LOGS_TABLE_CREATE);
        }

        @Override
        public void onOpen(SQLiteDatabase db) {
        	super.onOpen(db);
        	// enable foreign key constraints
        	if (!db.isReadOnly()) {
        		db.execSQL("PRAGMA foreign_keys = ON;");
        	}
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            Log.w(TAG, "Upgrading database from version " + oldVersion + " to "
                    + newVersion + ", which will destroy all old data");
            db.execSQL("DROP TABLE IF EXISTS drives; DROP TABLE IF EXISTS logs;");
            onCreate(db);
        }
    }

    /**
     * Constructor - takes the context to allow the database to be
     * opened/created
     *
     * @param ctx the Context within which to work
     */
    public DbAdapter(Context ctx) {
        this.mCtx = ctx;
    }

    /**
     * Open the database. If it cannot be opened, try to create a new
     * instance of the database. If it cannot be created, throw an exception to
     * signal the failure
     *
     * @return this (self reference, allowing this to be chained in an
     *         initialization call)
     * @throws SQLException if the database could be neither opened or created
     */
    public DbAdapter open() throws SQLException {
        mDbHelper = new DatabaseHelper(mCtx);
        mDb = mDbHelper.getWritableDatabase();
        return this;
    }

    public void close() {
        mDbHelper.close();
    }

    public long createLog(ContentValues values) {
    	return mDb.insert("logs", null, values);

    }

    public long createDrive(ContentValues values) {
    	return mDb.insert("drives", null, values);
    }

    /**
     * Create a new note using the title and body provided. If the note is
     * successfully created return the new rowId for that note, otherwise return
     * a -1 to indicate failure.
     *
     * @param title the title of the note
     * @param body the body of the note
     * @return rowId or -1 if failed
     */
    /*public long createNote(String title, String body) {
        ContentValues initialValues = new ContentValues();
        initialValues.put(KEY_TITLE, title);
        initialValues.put(KEY_BODY, body);

        return mDb.insert(DATABASE_TABLE, null, initialValues);
    }*/

    public boolean deleteDrive(long driveId) {
    	mDb.delete("logs", "drive_id = " + driveId, null); // delete logs
        return mDb.delete("drives", "_id =" + driveId, null) > 0;
    }

    public boolean deleteLog(long logId) {
        return mDb.delete("logs", "_id =" + logId, null) > 0;
    }

    public Cursor fetchAllDrives() {
        return mDb.rawQuery("SELECT * FROM drives ORDER BY start_time DESC", null);
    }

    public Cursor fetchDriveLogs(long driveId) {
    	return mDb.rawQuery("SELECT * FROM logs WHERE drive_id = ORDER BY time ASC" + driveId, null);
    }

    public Cursor fetchDrive(long driveId) {
    	Cursor mCursor = mDb.rawQuery("SELECT * FROM drives WHERE _id = " + driveId, null);

        if (mCursor != null) {
            mCursor.moveToFirst();
        }

        return mCursor;
    }
}
