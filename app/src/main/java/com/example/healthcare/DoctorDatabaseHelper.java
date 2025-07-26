package com.example.healthcare;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class DoctorDatabaseHelper extends SQLiteOpenHelper {
    private static final String TAG = "DoctorDatabaseHelper";
    private static final String DATABASE_NAME = "DoctorDB";
    private static final int DATABASE_VERSION = 1;
    private static final String TABLE_DOCTOR = "doctor_profile";
    private static final String COLUMN_ID = "id";
    private static final String COLUMN_DOCTOR_ID = "doctor_id";
    private static final String COLUMN_NAME = "name";

    public DoctorDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        Log.d(TAG, "DatabaseHelper initialized");
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String createTable = "CREATE TABLE " + TABLE_DOCTOR + "("
                + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + COLUMN_DOCTOR_ID + " TEXT UNIQUE,"
                + COLUMN_NAME + " TEXT"
                + ")";
        db.execSQL(createTable);
        Log.d(TAG, "Database table created");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_DOCTOR);
        onCreate(db);
        Log.d(TAG, "Database upgraded from " + oldVersion + " to " + newVersion);
    }

    public void saveDoctorName(String doctorId, String name) {
        Log.d(TAG, "Attempting to save doctor name: " + name + " for ID: " + doctorId);
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_DOCTOR_ID, doctorId);
        values.put(COLUMN_NAME, name);

        // Check if doctor exists
        Cursor cursor = db.query(TABLE_DOCTOR, new String[]{COLUMN_DOCTOR_ID},
                COLUMN_DOCTOR_ID + "=?", new String[]{doctorId}, null, null, null);

        if (cursor != null && cursor.getCount() > 0) {
            // Update existing record
            int rowsAffected = db.update(TABLE_DOCTOR, values, COLUMN_DOCTOR_ID + "=?", new String[]{doctorId});
            Log.d(TAG, "Updated existing record. Rows affected: " + rowsAffected);
        } else {
            // Insert new record
            long newRowId = db.insert(TABLE_DOCTOR, null, values);
            Log.d(TAG, "Inserted new record. Row ID: " + newRowId);
        }

        if (cursor != null) {
            cursor.close();
        }
        db.close();
    }

    public String getDoctorName(String doctorId) {
        Log.d(TAG, "Attempting to get doctor name for ID: " + doctorId);
        SQLiteDatabase db = this.getReadableDatabase();
        String name = null;

        Cursor cursor = db.query(TABLE_DOCTOR, new String[]{COLUMN_NAME},
                COLUMN_DOCTOR_ID + "=?", new String[]{doctorId}, null, null, null);

        if (cursor != null && cursor.moveToFirst()) {
            name = cursor.getString(cursor.getColumnIndex(COLUMN_NAME));
            Log.d(TAG, "Found doctor name: " + name);
            cursor.close();
        } else {
            Log.d(TAG, "No doctor name found in database");
        }
        db.close();
        return name;
    }
} 