package com.aseemsethi.esp32_iot;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by tutlane on 06-01-2018.
 * Modified by Aseem Sethi for Sensor details on 25-08-2019
 */

public class DBHandler extends SQLiteOpenHelper {
    private static final int DB_VERSION = 5;
    private static final String DB_NAME = "sensorsdb";
    private static final String TABLE_Sensors = "sensordetails";
    private static final String KEY_P = "keyP";
    private static final String KEY_ID = "id";
    private static final String KEY_NAME = "name";
    private static final String KEY_STATUS = "status";
    private static final String KEY_TIME = "time";
    final String TAG = "ESP32IOT Database";

    public DBHandler(Context context){
        super(context,DB_NAME, null, DB_VERSION);
        Log.d(TAG, "DB Handler");
    }
    @Override
    public void onCreate(SQLiteDatabase db){
        String CREATE_TABLE = "CREATE TABLE " + TABLE_Sensors + "("
                + KEY_P + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + KEY_ID + " INTEGER ," + KEY_NAME + " TEXT,"
                + KEY_STATUS + " TEXT,"
                + KEY_TIME + " TEXT,"
                + " unique " + " (" + KEY_ID + ") "  + ")";
        db.execSQL(CREATE_TABLE);
        Log.d(TAG, "OnCreate: DB Create: " + CREATE_TABLE);
    }
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion){
        // Drop older table if exist
        Log.d(TAG, "Dropping table...");
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_Sensors);
        // Create tables again
        onCreate(db);
    }
    // **** CRUD (Create, Read, Update, Delete) Operations ***** //

    // Adding new Sensor Details
    void insertSensorDetails(Integer id, String name, String status, String time){
        //Get the Data Repository in write mode
        SQLiteDatabase db = this.getWritableDatabase();
        //Create a new map of values, where column names are the keys
        ContentValues cValues = new ContentValues();
        cValues.put(KEY_ID, id);
        cValues.put(KEY_NAME, name);
        cValues.put(KEY_STATUS, status);
        cValues.put(KEY_TIME, time);
        Log.d(TAG, "InsertSensorDetails: " + id);
        // Insert the new row, returning the primary key value of the new row
        long newRowId = db.insert(TABLE_Sensors,null, cValues);
        db.close();
    }
    // Get Sensor Details
    public ArrayList<HashMap<String, String>> GetSensors(){
        SQLiteDatabase db = this.getWritableDatabase();
        ArrayList<HashMap<String, String>> sensorList = new ArrayList<>();
        String query = "SELECT id, name, status, time FROM "+ TABLE_Sensors;
        Cursor cursor = db.rawQuery(query,null);
        while (cursor.moveToNext()){
            HashMap<String,String> user = new HashMap<>();
            //Log.d(TAG, "GetSensors: " +
            //        cursor.getString(cursor.getColumnIndex(KEY_NAME)) +
            //        cursor.getString(cursor.getColumnIndex(KEY_STATUS)));
            user.put("id",cursor.getString(cursor.getColumnIndex(KEY_ID)));
            user.put("name",cursor.getString(cursor.getColumnIndex(KEY_NAME)));
            user.put("status",cursor.getString(cursor.getColumnIndex(KEY_STATUS)));
            user.put("time",cursor.getString(cursor.getColumnIndex(KEY_TIME)));
            sensorList.add(user);
        }
        return  sensorList;
    }
    // Get Sensor Details based on sensorID
    // public ArrayList<HashMap<String, String>> GetSensorBySensorId(int sensorID){
    public HashMap<String, String> GetSensorBySensorId(int sensorID){
            SQLiteDatabase db = this.getWritableDatabase();
        //ArrayList<HashMap<String, String>> userList = new ArrayList<>();
        String query = "SELECT name, status FROM "+ TABLE_Sensors;
        Cursor cursor = db.query(TABLE_Sensors, new String[]{KEY_NAME, KEY_STATUS, KEY_TIME},
                KEY_ID+ "=?",new String[]{String.valueOf(sensorID)},
                null, null, null, null);
        if (cursor.moveToNext()){
            HashMap<String,String> user = new HashMap<>();
            user.put("name",cursor.getString(cursor.getColumnIndex(KEY_NAME)));
            user.put("status",cursor.getString(cursor.getColumnIndex(KEY_STATUS)));
            user.put("time",cursor.getString(cursor.getColumnIndex(KEY_TIME)));
            //userList.add(user);
            return user;
        }
        return  null;
    }
    // Delete Sensor Details
    public void DeleteSensor(int sensorID){
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_Sensors, KEY_ID+" = ?",
                new String[]{String.valueOf(sensorID)});
        db.close();
    }
    // Update Sensor Details
    public int UpdateSensorDetails(String status, int id){
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cVals = new ContentValues();
        cVals.put(KEY_STATUS, status);
        int count = db.update(TABLE_Sensors, cVals,
                KEY_ID+" = ?",new String[]{String.valueOf(id)});
        return  count;
    }

    // Update Sensor Details
    public int UpdateSensorTime(String time, int id){
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cVals = new ContentValues();
        cVals.put(KEY_TIME, time);
        int count = db.update(TABLE_Sensors, cVals,
                KEY_ID+" = ?",new String[]{String.valueOf(id)});
        return  count;
    }
}
