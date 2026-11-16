package com.easyq.easypayment.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import com.easyq.easypayment.network.ApiClient;
import com.easyq.easypayment.service.SmsListenerService;

public class BootReceiver extends BroadcastReceiver {
    private static final String TAG = "EasyPaymentBootReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (Intent.ACTION_BOOT_COMPLETED.equals(action) || "android.intent.action.QUICKBOOT_POWERON".equals(action)) {
            Log.i(TAG, "Device booted, checking if SMS listener service was enabled...");
            boolean enabled = ApiClient.getPrefs(context).getBoolean(ApiClient.KEY_SERVICE_ENABLED, true);
            if (enabled) {
                Intent serviceIntent = new Intent(context, SmsListenerService.class);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(serviceIntent);
                } else {
                    context.startService(serviceIntent);
                }
                Log.i(TAG, "SmsListenerService started after boot.");
            }
        }
    }
}
