package org.ohmage.funf;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import edu.mit.media.funf.json.IJsonObject;
import android.util.Log;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.joda.time.DateTime;

public class DatabaseHandler extends SQLiteOpenHelper {

    // All Static variables
    // Database Version
    private static final int DATABASE_VERSION = 1;

    // Database Name
    private static final String DATABASE_NAME = "DSUDataManager";

    // Contacts table name
    private static final String TABLE_MOBILE_SENSOR = "DSUData";

    // Contacts Table Columns names
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
                + KEY_CONFIG + " TEXT," + KEY_TIMESTAMP + " TEXT,"
                + KEY_DEVICE + " TEXT," + KEY_DATA + " TEXT," + ")";

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
    long addContact(JsonObject data, JsonObject config, DateTime timestamp, String device) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(KEY_CONFIG, config.toString());
        values.put(KEY_TIMESTAMP, timestamp.toString());
        values.put(KEY_DEVICE, device);
        values.put(KEY_DATA, data.toString());

        // Inserting Row
        long newRowId = db.insert(TABLE_MOBILE_SENSOR, null, values);
        db.close(); // Closing database connection
        return newRowId;
    }

    // Getting single Entry
    JsonObject getContact(long id) {
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.query(TABLE_MOBILE_SENSOR, new String[]{KEY_CONFIG,
                        KEY_TIMESTAMP, KEY_DEVICE, KEY_DATA}, KEY_CONFIG + "=?",
                new String[]{String.valueOf(id)}, null, null, null, null);
        if (cursor != null) {
            cursor.moveToFirst();
        }

        JsonParser parser = new JsonParser();

        JsonObject data = (JsonObject) (parser.parse(cursor.getString(3)));
        JsonObject config = (JsonObject) (parser.parse(cursor.getString(0)));
        data.add("probeConfig", config);
        data.addProperty("timestamp", cursor.getString(1));
        data.addProperty("device_info", cursor.getString(2));
        // return json data
        return data;
    }

    // Deleting single contact
    public void deleteContact(JsonObject config) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_MOBILE_SENSOR, KEY_CONFIG + " = ?",
                new String[] { String.valueOf(config.toString()) });
        db.close();
    }
}
