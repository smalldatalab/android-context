package org.ohmage.funf;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.ArrayList;
import java.util.List;

public class DatabaseHandler extends SQLiteOpenHelper {

    // All Static variables
    // Database Version
    private static final int DATABASE_VERSION = 1;

    // Database Name
    private static final String DATABASE_NAME = "DSUDataManager";

    // Contacts table name
    public static final String TABLE_MOBILE_SENSOR = "DSUData";

    // Contacts Table Columns names
    public static final String KEY_ID = "id";
    public static final String KEY_CONFIG = "config";
    public static final String KEY_TIMESTAMP = "timestamp";
    public static final String KEY_DEVICE = "device_info";
    public static final String KEY_DATA = "data";

    public DatabaseHandler(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    // Creating Tables
    @Override
    public void onCreate(SQLiteDatabase db) {
        //create mobile sensor table
        String CREATE_MOBILE_SENSOR_TABLE ="CREATE TABLE " + TABLE_MOBILE_SENSOR + " ("
                + KEY_ID + " INTEGER PRIMARY KEY," + KEY_DATA + " TEXT,"
                + KEY_CONFIG + " TEXT," + KEY_TIMESTAMP + " TEXT," + KEY_DEVICE + " TEXT" + ")";

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

}
