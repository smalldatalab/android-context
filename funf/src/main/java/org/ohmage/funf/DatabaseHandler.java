package org.ohmage.funf;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.joda.time.DateTime;

import java.util.ArrayList;
import java.util.List;

public class DatabaseHandler extends SQLiteOpenHelper {

    // All Static variables
    // Database Version
    private static final int DATABASE_VERSION = 1;

    // Database Name
    private static final String DATABASE_NAME = "DSUDataManager";

    // Contacts table name
    private static final String TABLE_MOBILE_SENSOR = "DSUData";

    // Contacts Table Columns names
    private static final String KEY_ID = "id";
    private static final String KEY_CONFIG = "config";
    private static final String KEY_TIMESTAMP = "timestamp";
    private static final String KEY_DEVICE = "device_info";
    private static final String KEY_DATA = "data";

    public DatabaseHandler(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    // Creating Tables
    @Override
    public void onCreate(SQLiteDatabase db) {
        //create mobile sensor table
        String CREATE_MOBILE_SENSOR_TABLE ="CREATE TABLE " + TABLE_MOBILE_SENSOR + " ("
                + KEY_ID + " INTEGER PRIMARY KEY," + KEY_DATA + " TEXT,"
                + KEY_CONFIG + " TEXT," + KEY_TIMESTAMP + " TEXT," + KEY_DEVICE + " TEXT," + ")";

        db.execSQL(CREATE_MOBILE_SENSOR_TABLE);
    }

    // Upgrading database
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Drop older table if existed
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_MOBILE_SENSOR);

        // Create tables again
        onCreate(db);
    }

    /**
     * All CRUD(Create, Read, Update, Delete) Operations
     */

    // Adding new Entry
    public void add(ProbeObject obj) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(KEY_DATA, obj.getData());
        values.put(KEY_CONFIG, obj.getConfig());
        values.put(KEY_TIMESTAMP, obj.getTimestamp());
        values.put(KEY_DEVICE, obj.getBuild());

        // Inserting Row
        db.insert(TABLE_MOBILE_SENSOR, null, values);
        db.close(); // Closing database connection
    }

    // Retrieves all of the Entries
    public List<ProbeObject> getAll() {
        List<ProbeObject> probeList = new ArrayList<ProbeObject>();

        // Select All Query
        String selectQuery = "SELECT  * FROM " + TABLE_MOBILE_SENSOR;

        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);

        // looping through all rows and adding to list
        if (cursor.moveToFirst()) {
            do {
                JsonParser parser = new JsonParser();

                ProbeObject obj = new ProbeObject(
                    Integer.parseInt(cursor.getString(0)),
                    (JsonObject) (parser.parse(cursor.getString(1))),
                    (JsonObject) (parser.parse(cursor.getString(2))),
                    cursor.getString(3),
                    cursor.getString(4)
                );
                // Adding contact to list
                probeList.add(obj);
            } while (cursor.moveToNext());
        }

        // return contact list
        return probeList;
    }

    // Deleting single probe
    public void delete(ProbeObject obj) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_MOBILE_SENSOR, KEY_ID + " = ?",
                new String[] { String.valueOf(obj.getID()) });
        db.close();
    }
}
