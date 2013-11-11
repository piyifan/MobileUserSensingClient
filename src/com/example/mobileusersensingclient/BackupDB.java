package com.example.mobileusersensingclient;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class BackupDB extends SQLiteOpenHelper {
	
	private final static String DATABASE_NAME = "BACKUP.db";
	private final static int DATABASE_VERSION = 1;
	private final static String TABLE_NAME = "backup_table";
	private final static int NUM_COLS = 4;
	private final static String COL_NAME[] = 
		{"back_location", "back_light", "back_acc", "back_wifi"};
	private final static String COL_TIME = "back_time";
	
	
	public BackupDB(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

	@Override
	public void onCreate(SQLiteDatabase db) {
		String sql = "CREATE TABLE " + TABLE_NAME + " (" + 
                COL_TIME + " INTEGER primary key autoincrement";
		for (int i = 0; i < SensorCollection.NUM_SENSORS; i++)
			sql = sql + ", " + COL_NAME[i] + " text";
		sql = sql + ");";
        db.execSQL(sql);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		String sql = "DROP TABLE IF EXISTS " + TABLE_NAME;
        db.execSQL(sql);
        onCreate(db);
	}
	
	public synchronized void insert(SensorInfoPack info) {
		SQLiteDatabase db = this.getWritableDatabase();
		ContentValues cv = new ContentValues();
		cv.put(COL_TIME, info.getRawTime());
		for (int i = 0; i < NUM_COLS; i++)
			cv.put(COL_NAME[i], info.getInfo(i));
		db.insert(TABLE_NAME, null, cv);
	}
	
	private synchronized void delete(long time) {
        SQLiteDatabase db = this.getWritableDatabase();
        String where = COL_TIME + " = ?";
        String[] whereValue = { String.valueOf(time) };
        db.delete(TABLE_NAME, where, whereValue);
    }
	
	public synchronized SensorInfoPack get() {
		SQLiteDatabase db = this.getWritableDatabase();
		Cursor cursor = db.query(TABLE_NAME,
				null, null, null, null, null, null, "1");
		if (cursor.getCount() != 1)
			return null;
		cursor.moveToFirst();
		
		String info[] = new String[NUM_COLS];
		for (int i = 0; i < NUM_COLS; i++) 
			info[i] = cursor.getString(
					cursor.getColumnIndex(COL_NAME[i]));
		long time = cursor.getLong(
					cursor.getColumnIndex(COL_TIME));
		delete(time);
		return new SensorInfoPack(info, time);
	}

}
