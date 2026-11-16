package com.easyq.easypayment.network;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;

import com.easyq.easypayment.db.TransactionDatabaseHelper;
import com.easyq.easypayment.models.MfsTransaction;
import com.google.gson.Gson;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ApiClient {
    private static final String TAG = "EasyPaymentApiClient";
    private static final String PREF_NAME = "easy_payment_prefs";

    public static final String KEY_API_URL = "api_url";
    public static final String KEY_API_KEY = "api_key";
    public static final String KEY_DEVICE_ID = "device_id";
    public static final String KEY_SERVICE_ENABLED = "service_enabled";
    public static final String KEY_AUTO_RETRY = "auto_retry";

    public static final String DEFAULT_API_URL = "https://your-domain.com/api/payment-webhook";

    private final OkHttpClient httpClient;
    private final Gson gson;
    private final Context context;

    public interface ApiCallback {
        void onSuccess(int statusCode, String responseBody);
        void onFailure(int statusCode, String errorMessage);
    }

    public ApiClient(Context context) {
        this.context = context.getApplicationContext();
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(25, TimeUnit.SECONDS)
                .readTimeout(25, TimeUnit.SECONDS)
                .writeTimeout(25, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true)
                .build();
        this.gson = new Gson();
    }

    public static SharedPreferences getPrefs(Context context) {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public static String getApiUrl(Context context) {
        return getPrefs(context).getString(KEY_API_URL, DEFAULT_API_URL);
    }

    public static String getApiKey(Context context) {
        return getPrefs(context).getString(KEY_API_KEY, "");
    }

    public static String getDeviceId(Context context) {
        String id = getPrefs(context).getString(KEY_DEVICE_ID, "");
        if (id == null || id.isEmpty()) {
            id = Build.MANUFACTURER + "_" + Build.MODEL;
            getPrefs(context).edit().putString(KEY_DEVICE_ID, id).apply();
        }
        return id;
    }

    /**
     * Posts a parsed MFS transaction to the Easy Q Web API.
     */
    public void postTransaction(MfsTransaction tx, ApiCallback callback) {
        String apiUrl = getApiUrl(context);
        String apiKey = getApiKey(context);
        String deviceId = getDeviceId(context);

        tx.setDeviceId(deviceId);

        String jsonPayload = gson.toJson(tx);
        RequestBody body = RequestBody.create(jsonPayload, MediaType.parse("application/json; charset=utf-8"));

        Request.Builder requestBuilder = new Request.Builder()
                .url(apiUrl)
                .post(body)
                .addHeader("Content-Type", "application/json")
                .addHeader("User-Agent", "EasyPayment-Android/" + deviceId)
                .addHeader("X-Device-Id", deviceId);

        if (apiKey != null && !apiKey.trim().isEmpty()) {
            requestBuilder.addHeader("Authorization", "Bearer " + apiKey.trim());
            requestBuilder.addHeader("X-Api-Key", apiKey.trim());
        }

        Request request = requestBuilder.build();

        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "Network request failed: " + e.getMessage());
                // Update local DB status = 2 (Failed)
                if (tx.getId() > 0) {
                    TransactionDatabaseHelper.getInstance(context).updateSyncStatus(
                            tx.getId(), 2, 0, e.getMessage()
                    );
                }
                if (callback != null) {
                    callback.onFailure(0, e.getMessage());
                }
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                int code = response.code();
                String respBody = response.body() != null ? response.body().string() : "";

                if (response.isSuccessful()) {
                    Log.i(TAG, "Sync success HTTP " + code + ": " + respBody);
                    if (tx.getId() > 0) {
                        TransactionDatabaseHelper.getInstance(context).updateSyncStatus(
                                tx.getId(), 1, code, respBody
                        );
                    }
                    if (callback != null) {
                        callback.onSuccess(code, respBody);
                    }
                } else {
                    Log.w(TAG, "Sync rejected HTTP " + code + ": " + respBody);
                    if (tx.getId() > 0) {
                        TransactionDatabaseHelper.getInstance(context).updateSyncStatus(
                                tx.getId(), 2, code, respBody
                        );
                    }
                    if (callback != null) {
                        callback.onFailure(code, respBody);
                    }
                }
            }
        });
    }

    /**
     * Tests the API connection with a ping request.
     */
    public void testConnection(String targetUrl, String secretKey, ApiCallback callback) {
        String testPayload = "{\"event\":\"ping\",\"client\":\"EasyPaymentApp\",\"timestamp\":" + System.currentTimeMillis() + "}";
        RequestBody body = RequestBody.create(testPayload, MediaType.parse("application/json; charset=utf-8"));

        Request.Builder builder = new Request.Builder()
                .url(targetUrl)
                .post(body)
                .addHeader("Content-Type", "application/json")
                .addHeader("X-Device-Id", getDeviceId(context));

        if (secretKey != null && !secretKey.trim().isEmpty()) {
            builder.addHeader("Authorization", "Bearer " + secretKey.trim());
            builder.addHeader("X-Api-Key", secretKey.trim());
        }

        httpClient.newCall(builder.build()).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                if (callback != null) {
                    callback.onFailure(0, e.getMessage());
                }
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                int code = response.code();
                String resp = response.body() != null ? response.body().string() : "";
                if (response.isSuccessful()) {
                    if (callback != null) callback.onSuccess(code, resp);
                } else {
                    if (callback != null) callback.onFailure(code, resp);
                }
            }
        });
    }
}
