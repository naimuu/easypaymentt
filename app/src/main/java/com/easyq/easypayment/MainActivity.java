package com.easyq.easypayment;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.easyq.easypayment.adapter.TransactionAdapter;
import com.easyq.easypayment.db.TransactionDatabaseHelper;
import com.easyq.easypayment.models.MfsTransaction;
import com.easyq.easypayment.network.ApiClient;
import com.easyq.easypayment.service.SmsListenerService;
import com.google.android.material.switchmaterial.SwitchMaterial;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements TransactionAdapter.OnItemActionListener {

    private View viewStatusDot;
    private TextView tvServiceTitle, tvServiceSubtitle;
    private SwitchMaterial switchService;
    private TextView tvTodayCount, tvTodayAmount;
    private LinearLayout layoutPermissionWarning;
    private Button btnFixPermissions, btnQuickSimulator, btnQuickHistory;
    private ImageButton btnSettings;
    private TextView tvViewAll, tvEmptyState;
    private RecyclerView rvRecentTransactions;

    private TransactionAdapter adapter;
    private final List<MfsTransaction> recentList = new ArrayList<>();

    private final ActivityResultLauncher<String[]> permissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                boolean allGranted = true;
                for (Boolean granted : result.values()) {
                    if (!granted) {
                        allGranted = false;
                        break;
                    }
                }
                if (allGranted) {
                    Toast.makeText(this, "SMS permissions granted!", Toast.LENGTH_SHORT).show();
                    checkPermissionsAndBattery();
                    startListenerService();
                } else {
                    Toast.makeText(this, "SMS permission is required to detect bKash/Nagad payments!", Toast.LENGTH_LONG).show();
                }
            });

    private final BroadcastReceiver updateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            refreshDashboardData();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews();
        setupListeners();
        checkPermissionsAndBattery();

        // If enabled in settings, ensure service is running
        boolean isEnabled = ApiClient.getPrefs(this).getBoolean(ApiClient.KEY_SERVICE_ENABLED, true);
        if (isEnabled && hasSmsPermissions()) {
            startListenerService();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshDashboardData();
        checkPermissionsAndBattery();

        IntentFilter filter = new IntentFilter();
        filter.addAction("com.easyq.easypayment.TRANSACTION_RECEIVED");
        filter.addAction("com.easyq.easypayment.SERVICE_STATE_CHANGED");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(updateReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(updateReceiver, filter);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        try {
            unregisterReceiver(updateReceiver);
        } catch (Exception ignored) {
        }
    }

    private void initViews() {
        viewStatusDot = findViewById(R.id.viewStatusDot);
        tvServiceTitle = findViewById(R.id.tvServiceTitle);
        tvServiceSubtitle = findViewById(R.id.tvServiceSubtitle);
        switchService = findViewById(R.id.switchService);
        tvTodayCount = findViewById(R.id.tvTodayCount);
        tvTodayAmount = findViewById(R.id.tvTodayAmount);
        layoutPermissionWarning = findViewById(R.id.layoutPermissionWarning);
        btnFixPermissions = findViewById(R.id.btnFixPermissions);
        btnQuickSimulator = findViewById(R.id.btnQuickSimulator);
        btnQuickHistory = findViewById(R.id.btnQuickHistory);
        btnSettings = findViewById(R.id.btnSettings);
        tvViewAll = findViewById(R.id.tvViewAll);
        tvEmptyState = findViewById(R.id.tvEmptyState);
        rvRecentTransactions = findViewById(R.id.rvRecentTransactions);

        rvRecentTransactions.setLayoutManager(new LinearLayoutManager(this));
        adapter = new TransactionAdapter(this, recentList, this);
        rvRecentTransactions.setAdapter(adapter);
    }

    private void setupListeners() {
        btnSettings.setOnClickListener(v -> startActivity(new Intent(this, SettingsActivity.class)));
        btnQuickSimulator.setOnClickListener(v -> startActivity(new Intent(this, TestParserActivity.class)));
        btnQuickHistory.setOnClickListener(v -> startActivity(new Intent(this, HistoryActivity.class)));
        tvViewAll.setOnClickListener(v -> startActivity(new Intent(this, HistoryActivity.class)));

        switchService.setOnCheckedChangeListener((buttonView, isChecked) -> {
            ApiClient.getPrefs(this).edit().putBoolean(ApiClient.KEY_SERVICE_ENABLED, isChecked).apply();
            if (isChecked) {
                if (hasSmsPermissions()) {
                    startListenerService();
                } else {
                    requestRequiredPermissions();
                }
            } else {
                stopListenerService();
            }
            updateServiceUi(isChecked);
        });

        btnFixPermissions.setOnClickListener(v -> {
            if (!hasSmsPermissions()) {
                requestRequiredPermissions();
            } else if (!isBatteryOptimizationIgnored()) {
                requestIgnoreBatteryOptimization();
            }
        });
    }

    private boolean hasSmsPermissions() {
        boolean sms = ContextCompat.checkSelfPermission(this, Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED;
        boolean readSms = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED;
        return sms && readSms;
    }

    private void requestRequiredPermissions() {
        List<String> perms = new ArrayList<>();
        perms.add(Manifest.permission.RECEIVE_SMS);
        perms.add(Manifest.permission.READ_SMS);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            perms.add(Manifest.permission.POST_NOTIFICATIONS);
        }
        permissionLauncher.launch(perms.toArray(new String[0]));
    }

    private boolean isBatteryOptimizationIgnored() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
            return pm != null && pm.isIgnoringBatteryOptimizations(getPackageName());
        }
        return true;
    }

    @SuppressLint("BatteryLife")
    private void requestIgnoreBatteryOptimization() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                intent.setData(Uri.parse("package:" + getPackageName()));
                startActivity(intent);
            } catch (Exception e) {
                Intent fallback = new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);
                startActivity(fallback);
            }
        }
    }

    private void checkPermissionsAndBattery() {
        boolean hasPerms = hasSmsPermissions();
        boolean batteryIgnored = isBatteryOptimizationIgnored();

        if (!hasPerms || !batteryIgnored) {
            layoutPermissionWarning.setVisibility(View.VISIBLE);
            if (!hasPerms) {
                btnFixPermissions.setText("Grant SMS Permissions");
            } else {
                btnFixPermissions.setText("Disable Battery Saver");
            }
        } else {
            layoutPermissionWarning.setVisibility(View.GONE);
        }
    }

    private void startListenerService() {
        Intent serviceIntent = new Intent(this, SmsListenerService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }
        updateServiceUi(true);
    }

    private void stopListenerService() {
        Intent serviceIntent = new Intent(this, SmsListenerService.class);
        stopService(serviceIntent);
        updateServiceUi(false);
    }

    private void updateServiceUi(boolean isRunning) {
        if (isRunning) {
            viewStatusDot.setBackgroundResource(R.drawable.bg_badge_success);
            tvServiceTitle.setText(R.string.service_status_running);
            tvServiceSubtitle.setText("Foreground listener active & ready");
            switchService.setChecked(true);
        } else {
            viewStatusDot.setBackgroundResource(R.drawable.bg_badge_failed);
            tvServiceTitle.setText(R.string.service_status_stopped);
            tvServiceSubtitle.setText("Tap switch to resume SMS monitoring");
            switchService.setChecked(false);
        }
    }

    private void refreshDashboardData() {
        TransactionDatabaseHelper db = TransactionDatabaseHelper.getInstance(this);
        TransactionDatabaseHelper.TodayStats stats = db.getTodayStats();

        tvTodayCount.setText(String.valueOf(stats.countToday));
        tvTodayAmount.setText(String.format(Locale.US, "Tk %.2f", stats.totalAmountToday));

        recentList.clear();
        recentList.addAll(db.getAllTransactions(10));
        adapter.notifyDataSetChanged();

        if (recentList.isEmpty()) {
            tvEmptyState.setVisibility(View.VISIBLE);
            rvRecentTransactions.setVisibility(View.GONE);
        } else {
            tvEmptyState.setVisibility(View.GONE);
            rvRecentTransactions.setVisibility(View.VISIBLE);
        }

        updateServiceUi(SmsListenerService.isServiceRunning());
    }

    @Override
    public void onTransactionUpdated() {
        refreshDashboardData();
    }
}
