package com.easyq.easypayment;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.easyq.easypayment.network.ApiClient;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.switchmaterial.SwitchMaterial;

public class SettingsActivity extends AppCompatActivity {

    private EditText etApiUrl, etApiKey, etDeviceId, etReceiverAccount;
    private SwitchMaterial switchAutoRetry;
    private Button btnTestConnection, btnSaveSettings;
    private TextView tvTestResult;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        MaterialToolbar toolbar = findViewById(R.id.toolbarSettings);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        etApiUrl = findViewById(R.id.etApiUrl);
        etApiKey = findViewById(R.id.etApiKey);
        etDeviceId = findViewById(R.id.etDeviceId);
        etReceiverAccount = findViewById(R.id.etReceiverAccount);
        switchAutoRetry = findViewById(R.id.switchAutoRetry);
        btnTestConnection = findViewById(R.id.btnTestConnection);
        btnSaveSettings = findViewById(R.id.btnSaveSettings);
        tvTestResult = findViewById(R.id.tvTestResult);

        // Load existing values
        SharedPreferences prefs = ApiClient.getPrefs(this);
        etApiUrl.setText(ApiClient.getApiUrl(this));
        etApiKey.setText(ApiClient.getApiKey(this));
        etDeviceId.setText(ApiClient.getDeviceId(this));
        etReceiverAccount.setText(ApiClient.getReceiverAccount(this));
        switchAutoRetry.setChecked(prefs.getBoolean(ApiClient.KEY_AUTO_RETRY, true));

        btnTestConnection.setOnClickListener(v -> {
            String url = etApiUrl.getText().toString().trim();
            String key = etApiKey.getText().toString().trim();

            if (url.isEmpty() || !url.startsWith("http")) {
                Toast.makeText(this, "Please enter a valid HTTP/HTTPS URL", Toast.LENGTH_SHORT).show();
                return;
            }

            tvTestResult.setVisibility(View.VISIBLE);
            tvTestResult.setText("Testing connection to " + url + "...");

            ApiClient client = new ApiClient(this);
            client.testConnection(url, key, new ApiClient.ApiCallback() {
                @Override
                public void onSuccess(int statusCode, String responseBody) {
                    runOnUiThread(() -> {
                        tvTestResult.setText("✅ Connection OK! (HTTP " + statusCode + ")\n" + responseBody);
                    });
                }

                @Override
                public void onFailure(int statusCode, String errorMessage) {
                    runOnUiThread(() -> {
                        tvTestResult.setText("❌ Connection Failed! (HTTP " + statusCode + ")\n" + errorMessage);
                    });
                }
            });
        });

        btnSaveSettings.setOnClickListener(v -> {
            String url = etApiUrl.getText().toString().trim();
            String key = etApiKey.getText().toString().trim();
            String deviceId = etDeviceId.getText().toString().trim();
            String receiverAcc = etReceiverAccount.getText().toString().trim();
            boolean autoRetry = switchAutoRetry.isChecked();

            if (url.isEmpty() || !url.startsWith("http")) {
                Toast.makeText(this, "Please enter a valid HTTP/HTTPS URL", Toast.LENGTH_SHORT).show();
                return;
            }

            prefs.edit()
                    .putString(ApiClient.KEY_API_URL, url)
                    .putString(ApiClient.KEY_API_KEY, key)
                    .putString(ApiClient.KEY_DEVICE_ID, deviceId)
                    .putString(ApiClient.KEY_RECEIVER_ACCOUNT, receiverAcc)
                    .putBoolean(ApiClient.KEY_AUTO_RETRY, autoRetry)
                    .apply();

            Toast.makeText(this, "Settings saved successfully!", Toast.LENGTH_SHORT).show();
            finish();
        });
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
