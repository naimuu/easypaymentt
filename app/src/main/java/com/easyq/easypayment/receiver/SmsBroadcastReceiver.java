package com.easyq.easypayment.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.telephony.SmsMessage;
import android.util.Log;

import com.easyq.easypayment.db.TransactionDatabaseHelper;
import com.easyq.easypayment.models.MfsTransaction;
import com.easyq.easypayment.network.ApiClient;
import com.easyq.easypayment.parser.MfsSmsParser;

public class SmsBroadcastReceiver extends BroadcastReceiver {
    private static final String TAG = "EasyPaymentSmsReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (!"android.provider.Telephony.SMS_RECEIVED".equals(intent.getAction())) {
            return;
        }

        Bundle bundle = intent.getExtras();
        if (bundle == null) return;

        Object[] pdus = (Object[]) bundle.get("pdus");
        String format = bundle.getString("format");
        if (pdus == null || pdus.length == 0) return;

        StringBuilder fullBody = new StringBuilder();
        String senderAddress = "";
        long timestamp = System.currentTimeMillis();

        for (Object pdu : pdus) {
            SmsMessage smsMessage;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                smsMessage = SmsMessage.createFromPdu((byte[]) pdu, format);
            } else {
                smsMessage = SmsMessage.createFromPdu((byte[]) pdu);
            }

            if (smsMessage != null) {
                senderAddress = smsMessage.getDisplayOriginatingAddress();
                fullBody.append(smsMessage.getDisplayMessageBody());
                timestamp = smsMessage.getTimestampMillis();
            }
        }

        String completeMessage = fullBody.toString();
        Log.d(TAG, "SMS received from: " + senderAddress + " | Body: " + completeMessage);

        // Parse with MfsSmsParser
        MfsTransaction tx = MfsSmsParser.parse(senderAddress, completeMessage, timestamp);
        if (tx != null) {
            Log.i(TAG, "MFS Payment Detected! Provider: " + tx.getProvider()
                    + " | Type: " + tx.getTransactionType()
                    + " | Amount: " + tx.getAmount()
                    + " | Sender: " + tx.getSenderNumber()
                    + " | TrxID: " + tx.getTrxId());

            // Save to local database
            TransactionDatabaseHelper dbHelper = TransactionDatabaseHelper.getInstance(context);
            long rowId = dbHelper.insertTransaction(tx);

            if (rowId != -1) {
                // Keep CPU awake while sending HTTP request
                PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
                PowerManager.WakeLock wakeLock = null;
                if (pm != null) {
                    wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "EasyPayment:SmsUploadWakeLock");
                    wakeLock.acquire(15000); // Max 15 seconds
                }

                PowerManager.WakeLock finalWakeLock = wakeLock;
                ApiClient apiClient = new ApiClient(context);
                apiClient.postTransaction(tx, new ApiClient.ApiCallback() {
                    @Override
                    public void onSuccess(int statusCode, String responseBody) {
                        Log.i(TAG, "Transaction posted successfully to Easy Q API: HTTP " + statusCode);
                        releaseWakeLock(finalWakeLock);
                        // Notify UI
                        notifyUi(context);
                    }

                    @Override
                    public void onFailure(int statusCode, String errorMessage) {
                        Log.e(TAG, "Failed posting transaction: " + errorMessage);
                        releaseWakeLock(finalWakeLock);
                        // Notify UI
                        notifyUi(context);
                    }
                });
            } else {
                Log.w(TAG, "Duplicate transaction ignored: TrxID " + tx.getTrxId());
            }
        }
    }

    private void releaseWakeLock(PowerManager.WakeLock wakeLock) {
        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
        }
    }

    private void notifyUi(Context context) {
        Intent updateIntent = new Intent("com.easyq.easypayment.TRANSACTION_RECEIVED");
        context.sendBroadcast(updateIntent);
    }
}
