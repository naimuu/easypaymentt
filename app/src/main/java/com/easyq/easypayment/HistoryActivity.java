package com.easyq.easypayment;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.easyq.easypayment.adapter.TransactionAdapter;
import com.easyq.easypayment.db.TransactionDatabaseHelper;
import com.easyq.easypayment.models.MfsTransaction;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.chip.ChipGroup;

import java.util.ArrayList;
import java.util.List;

public class HistoryActivity extends AppCompatActivity implements TransactionAdapter.OnItemActionListener {

    private RecyclerView rvHistory;
    private TextView tvHistoryEmpty, tvFilterSummary;
    private EditText etSearchHistory;
    private ChipGroup chipGroupStatus, chipGroupProvider;
    private TransactionAdapter adapter;
    private final List<MfsTransaction> displayedList = new ArrayList<>();

    private String selectedStatus = "UNUSED"; // Default: Show Pending/Unused list first!
    private String selectedProvider = "ALL";
    private String currentSearchQuery = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        MaterialToolbar toolbar = findViewById(R.id.toolbarHistory);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        rvHistory = findViewById(R.id.rvHistory);
        tvHistoryEmpty = findViewById(R.id.tvHistoryEmpty);
        tvFilterSummary = findViewById(R.id.tvFilterSummary);
        etSearchHistory = findViewById(R.id.etSearchHistory);
        chipGroupStatus = findViewById(R.id.chipGroupStatus);
        chipGroupProvider = findViewById(R.id.chipGroupProvider);

        rvHistory.setLayoutManager(new LinearLayoutManager(this));
        adapter = new TransactionAdapter(this, displayedList, this);
        rvHistory.setAdapter(adapter);

        // Status Tabs Listener (Pending/Unused vs Used/Activated)
        chipGroupStatus.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.contains(R.id.chipStatusPending)) {
                selectedStatus = "UNUSED";
            } else if (checkedIds.contains(R.id.chipStatusUsed)) {
                selectedStatus = "USED";
            } else {
                selectedStatus = "ALL";
            }
            loadAndFilterData();
        });

        // Provider Filter Listener
        chipGroupProvider.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.contains(R.id.chipBkash)) {
                selectedProvider = "bKash";
            } else if (checkedIds.contains(R.id.chipNagad)) {
                selectedProvider = "Nagad";
            } else if (checkedIds.contains(R.id.chipRocket)) {
                selectedProvider = "Rocket";
            } else {
                selectedProvider = "ALL";
            }
            loadAndFilterData();
        });

        // Live Search Input Listener
        etSearchHistory.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentSearchQuery = s.toString().trim();
                loadAndFilterData();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        loadAndFilterData();
    }

    private void loadAndFilterData() {
        TransactionDatabaseHelper db = TransactionDatabaseHelper.getInstance(this);
        List<MfsTransaction> rawList = db.getFilteredTransactions(selectedStatus, currentSearchQuery);

        displayedList.clear();
        for (MfsTransaction tx : rawList) {
            if ("ALL".equalsIgnoreCase(selectedProvider) || selectedProvider.equalsIgnoreCase(tx.getProvider())) {
                displayedList.add(tx);
            }
        }
        adapter.notifyDataSetChanged();

        // Update Summary Banner
        int count = displayedList.size();
        String statusLabel = "ALL".equalsIgnoreCase(selectedStatus) ? "All" : ("UNUSED".equalsIgnoreCase(selectedStatus) ? "Pending (Unused)" : "Used (Activated)");
        tvFilterSummary.setText("Showing " + count + " " + statusLabel + " transactions");

        if (displayedList.isEmpty()) {
            tvHistoryEmpty.setVisibility(View.VISIBLE);
            tvHistoryEmpty.setText("No " + statusLabel + " transactions found.");
        } else {
            tvHistoryEmpty.setVisibility(View.GONE);
        }
    }

    @Override
    public void onTransactionUpdated() {
        loadAndFilterData();
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
