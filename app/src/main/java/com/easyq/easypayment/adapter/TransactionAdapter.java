package com.easyq.easypayment.adapter;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.easyq.easypayment.R;
import com.easyq.easypayment.db.TransactionDatabaseHelper;
import com.easyq.easypayment.models.MfsTransaction;
import com.easyq.easypayment.network.ApiClient;

import java.util.List;
import java.util.Locale;

public class TransactionAdapter extends RecyclerView.Adapter<TransactionAdapter.ViewHolder> {

    private final Context context;
    private final List<MfsTransaction> transactions;
    private final OnItemActionListener listener;

    public interface OnItemActionListener {
        void onTransactionUpdated();
    }

    public TransactionAdapter(Context context, List<MfsTransaction> transactions, OnItemActionListener listener) {
        this.context = context;
        this.transactions = transactions;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_transaction, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        MfsTransaction tx = transactions.get(position);

        holder.tvProvider.setText(tx.getProvider());
        holder.tvAmount.setText(String.format(Locale.US, "Tk %.2f", tx.getAmount()));
        holder.tvSender.setText("From: " + tx.getSenderNumber());
        holder.tvReceiver.setText("To: " + (tx.getReceiverAccount() != null && !tx.getReceiverAccount().isEmpty() ? tx.getReceiverAccount() : "SIM"));
        holder.tvTrxId.setText("TrxID: " + tx.getTrxId());
        holder.tvTime.setText(tx.getSmsTimeFormatted());

        // Package Name Display
        if (tx.getPackageName() != null && !tx.getPackageName().isEmpty()) {
            holder.tvPackageName.setVisibility(View.VISIBLE);
            holder.tvPackageName.setText("Package: " + tx.getPackageName());
        } else {
            holder.tvPackageName.setVisibility(View.GONE);
        }

        // Provider Color
        int providerColor = Color.parseColor("#1E3A8A");
        if ("bKash".equalsIgnoreCase(tx.getProvider())) {
            providerColor = Color.parseColor("#E2136E");
        } else if ("Nagad".equalsIgnoreCase(tx.getProvider())) {
            providerColor = Color.parseColor("#F7941D");
        } else if ("Rocket".equalsIgnoreCase(tx.getProvider())) {
            providerColor = Color.parseColor("#8C3494");
        } else if ("Upay".equalsIgnoreCase(tx.getProvider())) {
            providerColor = Color.parseColor("#FFC709");
        }
        holder.tvProvider.setTextColor(providerColor);

        // Activation Status Badge (USED vs UNUSED)
        boolean isUsed = "USED".equalsIgnoreCase(tx.getActivationStatus());
        if (isUsed) {
            holder.tvActivationStatus.setText("✔ USED");
            holder.tvActivationStatus.setBackgroundResource(R.drawable.bg_badge_success);
            holder.tvActivationStatus.setTextColor(Color.parseColor("#065F46"));
        } else {
            holder.tvActivationStatus.setText("⏳ UNUSED");
            holder.tvActivationStatus.setBackgroundResource(R.drawable.bg_badge_pending);
            holder.tvActivationStatus.setTextColor(Color.parseColor("#92400E"));
        }

        // API Sync Status Badge
        if (tx.getSyncStatus() == 1) {
            holder.tvStatus.setText("SYNCED (200)");
            holder.tvStatus.setBackgroundResource(R.drawable.bg_badge_success);
            holder.tvStatus.setTextColor(Color.parseColor("#065F46"));
        } else if (tx.getSyncStatus() == 2) {
            holder.tvStatus.setText("FAILED");
            holder.tvStatus.setBackgroundResource(R.drawable.bg_badge_failed);
            holder.tvStatus.setTextColor(Color.parseColor("#991B1B"));
        } else {
            holder.tvStatus.setText("PENDING");
            holder.tvStatus.setBackgroundResource(R.drawable.bg_badge_pending);
            holder.tvStatus.setTextColor(Color.parseColor("#92400E"));
        }

        holder.itemView.setOnClickListener(v -> showDetailsDialog(tx));
    }

    private void showDetailsDialog(MfsTransaction tx) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_transaction_detail, null);

        TextView tvDialogTitle = dialogView.findViewById(R.id.tvDialogTitle);
        TextView tvDetailAmount = dialogView.findViewById(R.id.tvDetailAmount);
        TextView tvDetailTrxId = dialogView.findViewById(R.id.tvDetailTrxId);
        TextView tvDetailSender = dialogView.findViewById(R.id.tvDetailSender);
        TextView tvDetailReceiver = dialogView.findViewById(R.id.tvDetailReceiver);
        TextView tvDetailPackage = dialogView.findViewById(R.id.tvDetailPackage);
        TextView tvDetailTime = dialogView.findViewById(R.id.tvDetailTime);
        TextView tvDetailRawSms = dialogView.findViewById(R.id.tvDetailRawSms);
        TextView tvDetailApiResponse = dialogView.findViewById(R.id.tvDetailApiResponse);
        Button btnActivateInApp = dialogView.findViewById(R.id.btnActivateInApp);
        Button btnResend = dialogView.findViewById(R.id.btnResend);
        Button btnClose = dialogView.findViewById(R.id.btnClose);

        tvDialogTitle.setText(tx.getProvider() + " - " + tx.getTransactionType());
        tvDetailAmount.setText(String.format(Locale.US, "Tk %.2f", tx.getAmount()));
        tvDetailTrxId.setText(tx.getTrxId());
        tvDetailSender.setText(tx.getSenderNumber());
        tvDetailReceiver.setText(tx.getReceiverAccount() != null ? tx.getReceiverAccount() : "N/A");
        
        boolean isUsed = "USED".equalsIgnoreCase(tx.getActivationStatus());
        if (isUsed) {
            tvDetailPackage.setText("ACTIVATED (Package: " + (tx.getPackageName() != null ? tx.getPackageName() : "Active") + ")");
            tvDetailPackage.setTextColor(Color.parseColor("#065F46"));
            btnActivateInApp.setVisibility(View.GONE);
        } else {
            tvDetailPackage.setText("UNUSED (Ready to activate package)");
            tvDetailPackage.setTextColor(Color.parseColor("#D97706"));
            btnActivateInApp.setVisibility(View.VISIBLE);
        }

        tvDetailTime.setText(tx.getSmsTimeFormatted());
        tvDetailRawSms.setText(tx.getRawMessage());
        tvDetailApiResponse.setText("HTTP " + tx.getHttpStatusCode() + "\n" + (tx.getApiResponseBody() != null ? tx.getApiResponseBody() : "No response"));

        AlertDialog dialog = builder.setView(dialogView).create();

        btnActivateInApp.setOnClickListener(v -> {
            showPackageSelectionDialog(tx, dialog);
        });

        btnResend.setOnClickListener(v -> {
            Toast.makeText(context, "Resending to Easy Q API...", Toast.LENGTH_SHORT).show();
            ApiClient client = new ApiClient(context);
            client.postTransaction(tx, new ApiClient.ApiCallback() {
                @Override
                public void onSuccess(int statusCode, String responseBody) {
                    v.post(() -> {
                        Toast.makeText(context, "Success! HTTP " + statusCode, Toast.LENGTH_LONG).show();
                        dialog.dismiss();
                        if (listener != null) listener.onTransactionUpdated();
                    });
                }

                @Override
                public void onFailure(int statusCode, String errorMessage) {
                    v.post(() -> {
                        Toast.makeText(context, "Failed: " + errorMessage, Toast.LENGTH_LONG).show();
                        if (listener != null) listener.onTransactionUpdated();
                    });
                }
            });
        });

        btnClose.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void showPackageSelectionDialog(MfsTransaction tx, AlertDialog parentDialog) {
        final String[] packages = {
                "Easy Q 30 Days VIP Pack (Tk 500)",
                "Easy Q Premium Monthly (Tk 1,000)",
                "Easy Q 3 Months Starter (Tk 1,200)",
                "Easy Q 1 Year Enterprise (Tk 5,000)",
                "Custom Package"
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Select Package to Activate");
        builder.setItems(packages, (dialog, which) -> {
            String selected = packages[which];
            if (which == 4) {
                // Custom input prompt
                showCustomPackageInputDialog(tx, parentDialog);
            } else {
                completeActivation(tx, selected, parentDialog);
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void showCustomPackageInputDialog(MfsTransaction tx, AlertDialog parentDialog) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Enter Custom Package Name");
        final EditText input = new EditText(context);
        input.setHint("e.g. Special Offer Pack");
        builder.setView(input);

        builder.setPositiveButton("Activate", (d, w) -> {
            String name = input.getText().toString().trim();
            if (name.isEmpty()) name = "Custom Package";
            completeActivation(tx, name, parentDialog);
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void completeActivation(MfsTransaction tx, String packageName, AlertDialog parentDialog) {
        TransactionDatabaseHelper db = TransactionDatabaseHelper.getInstance(context);
        boolean ok = db.markPackageActivated(tx.getId(), packageName);
        if (ok) {
            tx.setActivationStatus("USED");
            tx.setPackageName(packageName);
            Toast.makeText(context, "Package Activated: " + packageName, Toast.LENGTH_LONG).show();
            if (parentDialog != null) parentDialog.dismiss();
            if (listener != null) listener.onTransactionUpdated();
        } else {
            Toast.makeText(context, "Failed to update package status", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public int getItemCount() {
        return transactions.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvProvider, tvAmount, tvSender, tvReceiver, tvPackageName, tvTrxId, tvTime, tvActivationStatus, tvStatus;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvProvider = itemView.findViewById(R.id.tvProvider);
            tvAmount = itemView.findViewById(R.id.tvAmount);
            tvSender = itemView.findViewById(R.id.tvSender);
            tvReceiver = itemView.findViewById(R.id.tvReceiver);
            tvPackageName = itemView.findViewById(R.id.tvPackageName);
            tvTrxId = itemView.findViewById(R.id.tvTrxId);
            tvTime = itemView.findViewById(R.id.tvTime);
            tvActivationStatus = itemView.findViewById(R.id.tvActivationStatus);
            tvStatus = itemView.findViewById(R.id.tvStatus);
        }
    }
}
