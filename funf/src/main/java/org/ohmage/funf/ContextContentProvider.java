package org.ohmage.funf;

/**
 * Created by changun on 3/13/15.
 */


        import android.content.ContentProvider;
        import android.content.ContentValues;
        import android.content.Context;
        import android.database.Cursor;
        import android.database.sqlite.SQLiteDatabase;
        import android.database.sqlite.SQLiteQueryBuilder;
        import android.net.Uri;

        import com.google.gson.JsonObject;
        import com.google.gson.JsonParser;

        import java.util.ArrayList;
        import java.util.List;

public class ContextContentProvider extends ContentProvider {

    // database
    private DatabaseHandler database;
    public static final String AUTHORITY = "io.smalldata.android.context.provider";
    public static final String BASE_PATH = "datapoints";
    public static final Uri CONTENT_URI = Uri.parse("content://"+AUTHORITY+"/"+BASE_PATH);

    @Override
    public boolean onCreate() {
        database = new DatabaseHandler(getContext());
        return false;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection,
                        String[] selectionArgs, String sortOrder) {

        // Select All Query
        SQLiteDatabase db = database.getWritableDatabase();
        SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();
        queryBuilder.setTables(DatabaseHandler.TABLE_MOBILE_SENSOR);


        return queryBuilder.query(db, projection, selection,
                selectionArgs, null, null, sortOrder);

    }

    @Override
    public String getType(Uri uri) {
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        SQLiteDatabase sqlDB = database.getWritableDatabase();
        // Inserting Row
        sqlDB.insert(DatabaseHandler.TABLE_MOBILE_SENSOR, null, values);
        return Uri.parse(BASE_PATH + "/" + values.get(DatabaseHandler.KEY_ID));

    }
    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        SQLiteDatabase sqlDB = database.getWritableDatabase();
        return sqlDB.delete(DatabaseHandler.TABLE_MOBILE_SENSOR, selection,
                selectionArgs);
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection,
                      String[] selectionArgs) {
      throw new UnsupportedOperationException();
    }

    public static Uri insert(Context cxt, ProbeObject obj){
        ContentValues values = new ContentValues();
        values.put(DatabaseHandler.KEY_DATA, obj.getData());
        values.put(DatabaseHandler.KEY_CONFIG, obj.getConfig());
        values.put(DatabaseHandler.KEY_TIMESTAMP, obj.getTimestamp());
        values.put(DatabaseHandler.KEY_DEVICE, obj.getBuild());
        return cxt.getContentResolver().insert(CONTENT_URI, values);

    }

    public static List<ProbeObject> getAll(Context cxt) {
        List<ProbeObject> probeList = new ArrayList<ProbeObject>();

        // Select All Query
        String [] projection = {
                DatabaseHandler.KEY_ID,
                DatabaseHandler.KEY_DATA,
                DatabaseHandler.KEY_CONFIG,
                DatabaseHandler.KEY_TIMESTAMP,
                DatabaseHandler.KEY_DEVICE

        };
        String[] mSelectionArgs = {};
        String mSortOrder = DatabaseHandler.KEY_ID;
        Cursor cursor = cxt.getContentResolver().query(CONTENT_URI, projection, null, mSelectionArgs, mSortOrder);

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
    static public int delete(Context cxt, ProbeObject obj) {
        String mSelectionClause =  DatabaseHandler.KEY_ID + " = ?";
        String[] mSelectionArgs = {String.valueOf(obj.getID())};
        return cxt.getContentResolver().delete(CONTENT_URI, mSelectionClause, mSelectionArgs);
    }

}