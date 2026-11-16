package com.easyq.easypayment.adapter;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.easyq.easypayment.R;
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
        holder.tvSender.setText(tx.getSenderNumber());
        holder.tvTrxId.setText("TrxID: " + tx.getTrxId());
        holder.tvTime.setText(tx.getSmsTimeFormatted());

        // Provider Badge Color
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

        // Sync Status Badge
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

        // Tap on card to view full details dialog
        holder.itemView.setOnClickListener(v -> showDetailsDialog(tx));
    }

    private void showDetailsDialog(MfsTransaction tx) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_transaction_detail, null);

        TextView tvDialogTitle = dialogView.findViewById(R.id.tvDialogTitle);
        TextView tvDetailAmount = dialogView.findViewById(R.id.tvDetailAmount);
        TextView tvDetailTrxId = dialogView.findViewById(R.id.tvDetailTrxId);
        TextView tvDetailSender = dialogView.findViewById(R.id.tvDetailSender);
        TextView tvDetailTime = dialogView.findViewById(R.id.tvDetailTime);
        TextView tvDetailRawSms = dialogView.findViewById(R.id.tvDetailRawSms);
        TextView tvDetailApiResponse = dialogView.findViewById(R.id.tvDetailApiResponse);
        Button btnResend = dialogView.findViewById(R.id.btnResend);
        Button btnClose = dialogView.findViewById(R.id.btnClose);

        tvDialogTitle.setText(tx.getProvider() + " - " + tx.getTransactionType());
        tvDetailAmount.setText(String.format(Locale.US, "Tk %.2f", tx.getAmount()));
        tvDetailTrxId.setText(tx.getTrxId());
        tvDetailSender.setText(tx.getSenderNumber());
        tvDetailTime.setText(tx.getSmsTimeFormatted());
        tvDetailRawSms.setText(tx.getRawMessage());
        tvDetailApiResponse.setText("HTTP " + tx.getHttpStatusCode() + "\n" + (tx.getApiResponseBody() != null ? tx.getApiResponseBody() : "No response"));

        AlertDialog dialog = builder.setView(dialogView).create();

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

    @Override
    public int getItemCount() {
        return transactions.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvProvider, tvAmount, tvSender, tvTrxId, tvTime, tvStatus;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvProvider = itemView.findViewById(R.id.tvProvider);
            tvAmount = itemView.findViewById(R.id.tvAmount);
            tvSender = itemView.findViewById(R.id.tvSender);
            tvTrxId = itemView.findViewById(R.id.tvTrxId);
            tvTime = itemView.findViewById(R.id.tvTime);
            tvStatus = itemView.findViewById(R.id.tvStatus);
        }
    }
}
