package com.easyq.easypayment.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.easyq.easypayment.models.MfsTransaction;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class TransactionDatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "easy_payment.db";
    private static final int DATABASE_VERSION = 2;

    public static final String TABLE_TRANSACTIONS = "transactions";
    public static final String COL_ID = "id";
    public static final String COL_PROVIDER = "provider";
    public static final String COL_TYPE = "transaction_type";
    public static final String COL_AMOUNT = "amount";
    public static final String COL_CURRENCY = "currency";
    public static final String COL_SENDER_NUMBER = "sender_number";
    public static final String COL_RECEIVER_ACCOUNT = "receiver_account";
    public static final String COL_TRX_ID = "trx_id";
    public static final String COL_REFERENCE = "reference";
    public static final String COL_BALANCE_AFTER = "balance_after";
    public static final String COL_SMS_TIMESTAMP = "sms_timestamp";
    public static final String COL_SMS_TIME_FORMATTED = "sms_time_formatted";
    public static final String COL_RAW_MESSAGE = "raw_message";
    public static final String COL_SENDER_ADDRESS = "sender_address";
    public static final String COL_DEVICE_ID = "device_id";
    public static final String COL_SYNC_STATUS = "sync_status"; // 0=pending, 1=synced, 2=failed
    public static final String COL_HTTP_STATUS = "http_status";
    public static final String COL_API_RESPONSE = "api_response";
    public static final String COL_LAST_ATTEMPT = "last_attempt";
    public static final String COL_PACKAGE_NAME = "package_name";
    public static final String COL_ACTIVATION_STATUS = "activation_status"; // "UNUSED" vs "USED"
    public static final String COL_ACTIVATED_AT = "activated_at";

    private static TransactionDatabaseHelper instance;

    public static synchronized TransactionDatabaseHelper getInstance(Context context) {
        if (instance == null) {
            instance = new TransactionDatabaseHelper(context.getApplicationContext());
        }
        return instance;
    }

    private TransactionDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String createTable = "CREATE TABLE " + TABLE_TRANSACTIONS + " ("
                + COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COL_PROVIDER + " TEXT, "
                + COL_TYPE + " TEXT, "
                + COL_AMOUNT + " REAL, "
                + COL_CURRENCY + " TEXT, "
                + COL_SENDER_NUMBER + " TEXT, "
                + COL_RECEIVER_ACCOUNT + " TEXT, "
                + COL_TRX_ID + " TEXT UNIQUE, "
                + COL_REFERENCE + " TEXT, "
                + COL_BALANCE_AFTER + " REAL, "
                + COL_SMS_TIMESTAMP + " INTEGER, "
                + COL_SMS_TIME_FORMATTED + " TEXT, "
                + COL_RAW_MESSAGE + " TEXT, "
                + COL_SENDER_ADDRESS + " TEXT, "
                + COL_DEVICE_ID + " TEXT, "
                + COL_SYNC_STATUS + " INTEGER DEFAULT 0, "
                + COL_HTTP_STATUS + " INTEGER DEFAULT 0, "
                + COL_API_RESPONSE + " TEXT, "
                + COL_LAST_ATTEMPT + " INTEGER DEFAULT 0, "
                + COL_PACKAGE_NAME + " TEXT, "
                + COL_ACTIVATION_STATUS + " TEXT DEFAULT 'UNUSED', "
                + COL_ACTIVATED_AT + " TEXT"
                + ");";
        db.execSQL(createTable);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            try {
                db.execSQL("ALTER TABLE " + TABLE_TRANSACTIONS + " ADD COLUMN " + COL_RECEIVER_ACCOUNT + " TEXT;");
                db.execSQL("ALTER TABLE " + TABLE_TRANSACTIONS + " ADD COLUMN " + COL_PACKAGE_NAME + " TEXT;");
                db.execSQL("ALTER TABLE " + TABLE_TRANSACTIONS + " ADD COLUMN " + COL_ACTIVATION_STATUS + " TEXT DEFAULT 'UNUSED';");
                db.execSQL("ALTER TABLE " + TABLE_TRANSACTIONS + " ADD COLUMN " + COL_ACTIVATED_AT + " TEXT;");
            } catch (Exception e) {
                // If migration fails, recreate
                db.execSQL("DROP TABLE IF EXISTS " + TABLE_TRANSACTIONS);
                onCreate(db);
            }
        }
    }

    public boolean exists(String trxId) {
        if (trxId == null) return false;
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT 1 FROM " + TABLE_TRANSACTIONS + " WHERE " + COL_TRX_ID + " = ?", new String[]{trxId});
        boolean exists = cursor.moveToFirst();
        cursor.close();
        return exists;
    }

    public synchronized long insertTransaction(MfsTransaction tx) {
        if (exists(tx.getTrxId())) {
            return -1;
        }
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COL_PROVIDER, tx.getProvider());
        cv.put(COL_TYPE, tx.getTransactionType());
        cv.put(COL_AMOUNT, tx.getAmount());
        cv.put(COL_CURRENCY, tx.getCurrency());
        cv.put(COL_SENDER_NUMBER, tx.getSenderNumber());
        cv.put(COL_RECEIVER_ACCOUNT, tx.getReceiverAccount());
        cv.put(COL_TRX_ID, tx.getTrxId());
        cv.put(COL_REFERENCE, tx.getReference());
        cv.put(COL_BALANCE_AFTER, tx.getBalanceAfter());
        cv.put(COL_SMS_TIMESTAMP, tx.getSmsTimestamp());
        cv.put(COL_SMS_TIME_FORMATTED, tx.getSmsTimeFormatted());
        cv.put(COL_RAW_MESSAGE, tx.getRawMessage());
        cv.put(COL_SENDER_ADDRESS, tx.getSenderAddress());
        cv.put(COL_DEVICE_ID, tx.getDeviceId());
        cv.put(COL_SYNC_STATUS, tx.getSyncStatus());
        cv.put(COL_HTTP_STATUS, tx.getHttpStatusCode());
        cv.put(COL_API_RESPONSE, tx.getApiResponseBody());
        cv.put(COL_LAST_ATTEMPT, tx.getLastAttemptTimestamp());
        cv.put(COL_PACKAGE_NAME, tx.getPackageName());
        cv.put(COL_ACTIVATION_STATUS, tx.getActivationStatus() != null ? tx.getActivationStatus() : "UNUSED");
        cv.put(COL_ACTIVATED_AT, tx.getActivatedAt());

        long id = db.insert(TABLE_TRANSACTIONS, null, cv);
        tx.setId(id);
        return id;
    }

    public synchronized void updateSyncStatus(long id, int status, int httpCode, String response) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COL_SYNC_STATUS, status);
        cv.put(COL_HTTP_STATUS, httpCode);
        cv.put(COL_API_RESPONSE, response);
        cv.put(COL_LAST_ATTEMPT, System.currentTimeMillis());
        db.update(TABLE_TRANSACTIONS, cv, COL_ID + " = ?", new String[]{String.valueOf(id)});
    }

    /**
     * Activates a package for a given transaction and marks it as USED.
     */
    public synchronized boolean markPackageActivated(long id, String packageName) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COL_ACTIVATION_STATUS, "USED");
        cv.put(COL_PACKAGE_NAME, packageName);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
        cv.put(COL_ACTIVATED_AT, sdf.format(new Date()));
        int rows = db.update(TABLE_TRANSACTIONS, cv, COL_ID + " = ?", new String[]{String.valueOf(id)});
        return rows > 0;
    }

    /**
     * Returns transactions filtered by status ("UNUSED", "USED", or "ALL") and search query.
     */
    public List<MfsTransaction> getFilteredTransactions(String statusFilter, String query) {
        List<MfsTransaction> list = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();

        StringBuilder sql = new StringBuilder("SELECT * FROM " + TABLE_TRANSACTIONS + " WHERE 1=1");
        List<String> args = new ArrayList<>();

        if ("UNUSED".equalsIgnoreCase(statusFilter)) {
            sql.append(" AND " + COL_ACTIVATION_STATUS + " = 'UNUSED'");
        } else if ("USED".equalsIgnoreCase(statusFilter)) {
            sql.append(" AND " + COL_ACTIVATION_STATUS + " = 'USED'");
        }

        if (query != null && !query.trim().isEmpty()) {
            sql.append(" AND (" + COL_TRX_ID + " LIKE ? OR " + COL_SENDER_NUMBER + " LIKE ? OR " + COL_PACKAGE_NAME + " LIKE ?)");
            String q = "%" + query.trim() + "%";
            args.add(q);
            args.add(q);
            args.add(q);
        }

        sql.append(" ORDER BY " + COL_ID + " DESC");

        Cursor c = db.rawQuery(sql.toString(), args.toArray(new String[0]));
        if (c.moveToFirst()) {
            do {
                list.add(cursorToModel(c));
            } while (c.moveToNext());
        }
        c.close();
        return list;
    }

    public List<MfsTransaction> getAllTransactions(int limit) {
        return getFilteredTransactions("ALL", null);
    }

    public int getCountByStatus(String status) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_TRANSACTIONS + " WHERE " + COL_ACTIVATION_STATUS + " = ?", new String[]{status});
        int count = 0;
        if (c.moveToFirst()) {
            count = c.getInt(0);
        }
        c.close();
        return count;
    }

    public List<MfsTransaction> getPendingOrFailedTransactions() {
        List<MfsTransaction> list = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.rawQuery("SELECT * FROM " + TABLE_TRANSACTIONS + " WHERE " + COL_SYNC_STATUS + " != 1 ORDER BY " + COL_ID + " ASC LIMIT 20", null);
        if (c.moveToFirst()) {
            do {
                list.add(cursorToModel(c));
            } while (c.moveToNext());
        }
        c.close();
        return list;
    }

    public TodayStats getTodayStats() {
        TodayStats stats = new TodayStats();
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        long startOfDay = cal.getTimeInMillis();

        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.rawQuery("SELECT COUNT(*), SUM(" + COL_AMOUNT + ") FROM " + TABLE_TRANSACTIONS + " WHERE " + COL_SMS_TIMESTAMP + " >= ?", new String[]{String.valueOf(startOfDay)});
        if (c.moveToFirst()) {
            stats.countToday = c.getInt(0);
            stats.totalAmountToday = c.getDouble(1);
        }
        c.close();

        Cursor c2 = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_TRANSACTIONS + " WHERE " + COL_ACTIVATION_STATUS + " = 'UNUSED'", null);
        if (c2.moveToFirst()) {
            stats.pendingCount = c2.getInt(0);
        }
        c2.close();

        Cursor c3 = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_TRANSACTIONS + " WHERE " + COL_ACTIVATION_STATUS + " = 'USED'", null);
        if (c3.moveToFirst()) {
            stats.usedCount = c3.getInt(0);
        }
        c3.close();

        return stats;
    }

    public static class TodayStats {
        public int countToday = 0;
        public double totalAmountToday = 0.0;
        public int pendingCount = 0;
        public int usedCount = 0;
    }

    private MfsTransaction cursorToModel(Cursor c) {
        MfsTransaction tx = new MfsTransaction();
        tx.setId(c.getLong(c.getColumnIndexOrThrow(COL_ID)));
        tx.setProvider(c.getString(c.getColumnIndexOrThrow(COL_PROVIDER)));
        tx.setTransactionType(c.getString(c.getColumnIndexOrThrow(COL_TYPE)));
        tx.setAmount(c.getDouble(c.getColumnIndexOrThrow(COL_AMOUNT)));
        tx.setCurrency(c.getString(c.getColumnIndexOrThrow(COL_CURRENCY)));
        tx.setSenderNumber(c.getString(c.getColumnIndexOrThrow(COL_SENDER_NUMBER)));
        tx.setReceiverAccount(c.getString(c.getColumnIndexOrThrow(COL_RECEIVER_ACCOUNT)));
        tx.setTrxId(c.getString(c.getColumnIndexOrThrow(COL_TRX_ID)));
        tx.setReference(c.getString(c.getColumnIndexOrThrow(COL_REFERENCE)));
        tx.setBalanceAfter(c.getDouble(c.getColumnIndexOrThrow(COL_BALANCE_AFTER)));
        tx.setSmsTimestamp(c.getLong(c.getColumnIndexOrThrow(COL_SMS_TIMESTAMP)));
        tx.setSmsTimeFormatted(c.getString(c.getColumnIndexOrThrow(COL_SMS_TIME_FORMATTED)));
        tx.setRawMessage(c.getString(c.getColumnIndexOrThrow(COL_RAW_MESSAGE)));
        tx.setSenderAddress(c.getString(c.getColumnIndexOrThrow(COL_SENDER_ADDRESS)));
        tx.setDeviceId(c.getString(c.getColumnIndexOrThrow(COL_DEVICE_ID)));
        tx.setSyncStatus(c.getInt(c.getColumnIndexOrThrow(COL_SYNC_STATUS)));
        tx.setHttpStatusCode(c.getInt(c.getColumnIndexOrThrow(COL_HTTP_STATUS)));
        tx.setApiResponseBody(c.getString(c.getColumnIndexOrThrow(COL_API_RESPONSE)));
        tx.setLastAttemptTimestamp(c.getLong(c.getColumnIndexOrThrow(COL_LAST_ATTEMPT)));
        tx.setPackageName(c.getString(c.getColumnIndexOrThrow(COL_PACKAGE_NAME)));
        tx.setActivationStatus(c.getString(c.getColumnIndexOrThrow(COL_ACTIVATION_STATUS)));
        tx.setActivatedAt(c.getString(c.getColumnIndexOrThrow(COL_ACTIVATED_AT)));
        return tx;
    }
}
