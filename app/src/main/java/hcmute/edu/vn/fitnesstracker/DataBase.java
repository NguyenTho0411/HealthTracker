package hcmute.edu.vn.fitnesstracker;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import java.util.ArrayList;

public class DataBase extends SQLiteOpenHelper {
    private static final String DB_NAME = "health_care_db";
    private static final int DB_VERSION = 2;

    public DataBase(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE users (username TEXT, email TEXT, password TEXT)");
        db.execSQL("CREATE TABLE cart (username TEXT, product TEXT, price FLOAT, otype TEXT)");
        db.execSQL("CREATE TABLE orders (username TEXT, fullname TEXT, address TEXT, contact TEXT, pincode INTEGER, date TEXT, time TEXT, amount FLOAT, otype TEXT)");
        db.execSQL("CREATE TABLE medical_records (username TEXT, record_type TEXT, public_id TEXT, secure_url TEXT, upload_date TEXT)");
        db.execSQL("CREATE TABLE event_media (event_id TEXT, media_type TEXT, public_id TEXT, secure_url TEXT, upload_date TEXT)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            db.execSQL("CREATE TABLE medical_records (username TEXT, record_type TEXT, public_id TEXT, secure_url TEXT, upload_date TEXT)");
            db.execSQL("CREATE TABLE event_media (event_id TEXT, media_type TEXT, public_id TEXT, secure_url TEXT, upload_date TEXT)");
        }
    }

    public void register(String username, String email, String password) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("username", username);
        values.put("email", email);
        values.put("password", password);
        db.insert("users", null, values);
        db.close();
    }

    public int login(String username, String password) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM users WHERE username=? AND password=?", new String[]{username, password});
        int result = cursor.moveToFirst() ? 1 : 0;
        cursor.close();
        db.close();
        return result;
    }

    public void addtoCart(String username, String product, Float price, String otype) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("username", username);
        values.put("product", product);
        values.put("price", price);
        values.put("otype", otype);
        db.insert("cart", null, values);
        db.close();
    }

    public int checkCart(String username, String product) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM cart WHERE username=? AND product=?", new String[]{username, product});
        int result = cursor.moveToFirst() ? 1 : 0;
        cursor.close();
        db.close();
        return result;
    }

    public void removeCart(String username, String otype) {
        SQLiteDatabase db = getWritableDatabase();
        db.delete("cart", "username=? AND otype=?", new String[]{username, otype});
        db.close();
    }

    public ArrayList<String> getCart(String username, String otype) {
        ArrayList<String> arr = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM cart WHERE username=? AND otype=?", new String[]{username, otype});
        if (cursor.moveToFirst()) {
            do {
                arr.add(cursor.getString(1) + "$" + cursor.getString(2));
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return arr;
    }

    public void addOrder(String username, String fullname, String address, String contact, int pincode, String date, String time, float price, String otype) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("username", username);
        values.put("fullname", fullname);
        values.put("address", address);
        values.put("contact", contact);
        values.put("pincode", pincode);
        values.put("date", date);
        values.put("time", time);
        values.put("amount", price);
        values.put("otype", otype);
        db.insert("orders", null, values);
        db.close();
    }

    public ArrayList<String> getOrderData(String username) {
        ArrayList<String> arr = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM orders WHERE username=?", new String[]{username});
        if (cursor.moveToFirst()) {
            do {
                arr.add(cursor.getString(1) + "$" + cursor.getString(2) + "$" + cursor.getString(3) + "$" +
                        cursor.getString(4) + "$" + cursor.getString(5) + "$" + cursor.getString(6) + "$" +
                        cursor.getString(7) + "$" + cursor.getString(8));
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return arr;
    }

    public int checkAppointmentExists(String username, String fullname, String address, String contact, String date, String time) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM orders WHERE username=? AND fullname=? AND address=? AND contact=? AND date=? AND time=?",
                new String[]{username, fullname, address, contact, date, time});
        int result = cursor.moveToFirst() ? 1 : 0;
        cursor.close();
        db.close();
        return result;
    }

    public void removeOrder(String fullname, String otype, String address) {
        SQLiteDatabase db = getWritableDatabase();
        db.delete("orders", "fullname=? AND otype=? AND address=?", new String[]{fullname, otype, address});
        db.close();
    }

    public void addMedicalRecord(String username, String recordType, String publicId, String secureUrl, String uploadDate) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("username", username);
        values.put("record_type", recordType);
        values.put("public_id", publicId);
        values.put("secure_url", secureUrl);
        values.put("upload_date", uploadDate);
        db.insert("medical_records", null, values);
        db.close();
    }

    public ArrayList<String> getMedicalRecords(String username) {
        ArrayList<String> records = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM medical_records WHERE username=?", new String[]{username});
        if (cursor.moveToFirst()) {
            do {
                records.add(cursor.getString(1) + "$" + cursor.getString(3) + "$" + cursor.getString(4));
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return records;
    }

    public void addEventMedia(String eventId, String mediaType, String publicId, String secureUrl, String uploadDate) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("event_id", eventId);
        values.put("media_type", mediaType);
        values.put("public_id", publicId);
        values.put("secure_url", secureUrl);
        values.put("upload_date", uploadDate);
        db.insert("event_media", null, values);
        db.close();
    }

    public ArrayList<String> getEventMedia(String eventId) {
        ArrayList<String> media = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM event_media WHERE event_id=?", new String[]{eventId});
        if (cursor.moveToFirst()) {
            do {
                media.add(cursor.getString(1) + "$" + cursor.getString(3) + "$" + cursor.getString(4));
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return media;
    }
}