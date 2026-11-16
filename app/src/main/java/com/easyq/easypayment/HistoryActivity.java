package com.easyq.easypayment;

import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
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
    private TextView tvHistoryEmpty;
    private ChipGroup chipGroupProvider;
    private TransactionAdapter adapter;
    private final List<MfsTransaction> currentList = new ArrayList<>();
    private List<MfsTransaction> rawList = new ArrayList<>();
    private String selectedProvider = "ALL";

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
        chipGroupProvider = findViewById(R.id.chipGroupProvider);

        rvHistory.setLayoutManager(new LinearLayoutManager(this));
        adapter = new TransactionAdapter(this, currentList, this);
        rvHistory.setAdapter(adapter);

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
            filterList();
        });

        loadTransactions();
    }

    private void loadTransactions() {
        rawList = TransactionDatabaseHelper.getInstance(this).getAllTransactions(0);
        filterList();
    }

    private void filterList() {
        currentList.clear();
        for (MfsTransaction tx : rawList) {
            if ("ALL".equalsIgnoreCase(selectedProvider) || selectedProvider.equalsIgnoreCase(tx.getProvider())) {
                currentList.add(tx);
            }
        }
        adapter.notifyDataSetChanged();

        if (currentList.isEmpty()) {
            tvHistoryEmpty.setVisibility(View.VISIBLE);
        } else {
            tvHistoryEmpty.setVisibility(View.GONE);
        }
    }

    @Override
    public void onTransactionUpdated() {
        loadTransactions();
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
