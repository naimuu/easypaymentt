package com.easyq.easypayment.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.easyq.easypayment.MainActivity;
import com.easyq.easypayment.R;
import com.easyq.easypayment.db.TransactionDatabaseHelper;
import com.easyq.easypayment.models.MfsTransaction;
import com.easyq.easypayment.network.ApiClient;

import java.util.List;

public class SmsListenerService extends Service {
    private static final String TAG = "SmsListenerService";
    public static final String CHANNEL_ID = "easy_payment_service_channel";
    public static final int NOTIFICATION_ID = 1001;

    private static boolean isRunning = false;
    private Handler retryHandler;
    private Runnable retryRunnable;

    public static boolean isServiceRunning() {
        return isRunning;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        retryHandler = new Handler(Looper.getMainLooper());
        setupPeriodicRetry();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        isRunning = true;
        Notification notification = buildNotification("Easy Payment Active", "Listening 24/7 for bKash, Nagad, Rocket SMS");
        startForeground(NOTIFICATION_ID, notification);

        // Notify UI that service state changed
        sendBroadcast(new Intent("com.easyq.easypayment.SERVICE_STATE_CHANGED"));

        return START_STICKY;
    }

    private void setupPeriodicRetry() {
        // Every 3 minutes, automatically retry any failed transactions
        retryRunnable = new Runnable() {
            @Override
            public void run() {
                boolean autoRetry = ApiClient.getPrefs(SmsListenerService.this)
                        .getBoolean(ApiClient.KEY_AUTO_RETRY, true);
                if (autoRetry) {
                    retryFailedTransactions();
                }
                retryHandler.postDelayed(this, 3 * 60 * 1000); // 3 minutes
            }
        };
        retryHandler.postDelayed(retryRunnable, 60 * 1000);
    }

    private void retryFailedTransactions() {
        List<MfsTransaction> pendingList = TransactionDatabaseHelper.getInstance(this).getPendingOrFailedTransactions();
        if (pendingList != null && !pendingList.isEmpty()) {
            Log.d(TAG, "Retrying " + pendingList.size() + " pending/failed transactions...");
            ApiClient client = new ApiClient(this);
            for (MfsTransaction tx : pendingList) {
                client.postTransaction(tx, null);
            }
        }
    }

    private Notification buildNotification(String title, String text) {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                0,
                notificationIntent,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
        );

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(title)
                .setContentText(text)
                .setSmallIcon(R.drawable.ic_easy_q_logo)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "Easy Payment Background Service",
                    NotificationManager.IMPORTANCE_LOW
            );
            serviceChannel.setDescription("Keeps the SMS payment listener active in background");
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(serviceChannel);
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        isRunning = false;
        if (retryHandler != null && retryRunnable != null) {
            retryHandler.removeCallbacks(retryRunnable);
        }
        sendBroadcast(new Intent("com.easyq.easypayment.SERVICE_STATE_CHANGED"));
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
