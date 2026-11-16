package com.easyq.easypayment;

import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.easyq.easypayment.models.MfsTransaction;
import com.easyq.easypayment.network.ApiClient;
import com.easyq.easypayment.parser.MfsSmsParser;
import com.google.android.material.appbar.MaterialToolbar;

import java.util.Locale;

public class TestParserActivity extends AppCompatActivity {

    private EditText etTestSender, etTestBody;
    private Button btnParseSms, btnSendParsedToApi;
    private Button btnSampleBkash, btnSampleNagad, btnSampleRocket;
    private CardView cardResult;
    private TextView tvExtractedDetails, tvApiTestFeedback;

    private MfsTransaction parsedTx = null;

    private static final String SAMPLE_BKASH =
            "You have received Tk 500.00 from 01712345678. Ref EasyQ01. Fee Tk 0.00. Balance Tk 2,450.00. TrxID 9K48ABCDEF at 16/11/2026 12:30";

    private static final String SAMPLE_NAGAD =
            "You have received Tk 1,200.00 from 01898765432. TxnID: 71AB98ZZ at 16/11/2026 12:35. Balance: Tk 3,500.00";

    private static final String SAMPLE_ROCKET =
            "Cash In Tk. 750.00 from 019112233445 successful. Fee Tk. 0.00. Balance Tk. 1,800.00. TxnId: 8877665544 at 16/11/2026 12:40";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test_parser);

        MaterialToolbar toolbar = findViewById(R.id.toolbarTest);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        etTestSender = findViewById(R.id.etTestSender);
        etTestBody = findViewById(R.id.etTestBody);
        btnParseSms = findViewById(R.id.btnParseSms);
        btnSendParsedToApi = findViewById(R.id.btnSendParsedToApi);
        btnSampleBkash = findViewById(R.id.btnSampleBkash);
        btnSampleNagad = findViewById(R.id.btnSampleNagad);
        btnSampleRocket = findViewById(R.id.btnSampleRocket);
        cardResult = findViewById(R.id.cardResult);
        tvExtractedDetails = findViewById(R.id.tvExtractedDetails);
        tvApiTestFeedback = findViewById(R.id.tvApiTestFeedback);

        // Preload sample bKash SMS by default
        etTestSender.setText("bKash");
        etTestBody.setText(SAMPLE_BKASH);

        btnSampleBkash.setOnClickListener(v -> {
            etTestSender.setText("bKash");
            etTestBody.setText(SAMPLE_BKASH);
        });

        btnSampleNagad.setOnClickListener(v -> {
            etTestSender.setText("NAGAD");
            etTestBody.setText(SAMPLE_NAGAD);
        });

        btnSampleRocket.setOnClickListener(v -> {
            etTestSender.setText("16216");
            etTestBody.setText(SAMPLE_ROCKET);
        });

        btnParseSms.setOnClickListener(v -> {
            String sender = etTestSender.getText().toString().trim();
            String body = etTestBody.getText().toString().trim();

            if (body.isEmpty()) {
                Toast.makeText(this, "Please enter an SMS body", Toast.LENGTH_SHORT).show();
                return;
            }

            parsedTx = MfsSmsParser.parse(sender, body, System.currentTimeMillis());

            if (parsedTx != null) {
                cardResult.setVisibility(View.VISIBLE);
                tvApiTestFeedback.setVisibility(View.GONE);

                String details = String.format(Locale.US,
                        "Provider: %s\n" +
                        "Type: %s\n" +
                        "Amount: Tk %.2f\n" +
                        "Sender Number: %s\n" +
                        "TrxID: %s\n" +
                        "Reference: %s\n" +
                        "Balance After: Tk %.2f\n" +
                        "Timestamp: %s",
                        parsedTx.getProvider(),
                        parsedTx.getTransactionType(),
                        parsedTx.getAmount(),
                        parsedTx.getSenderNumber(),
                        parsedTx.getTrxId(),
                        parsedTx.getReference() != null ? parsedTx.getReference() : "N/A",
                        parsedTx.getBalanceAfter(),
                        parsedTx.getSmsTimeFormatted()
                );
                tvExtractedDetails.setText(details);
            } else {
                cardResult.setVisibility(View.VISIBLE);
                tvExtractedDetails.setText("❌ Not recognized as an incoming MFS payment SMS.\nMake sure it has 'received' or 'cash in', an amount, and a TrxID/TxnID.");
                btnSendParsedToApi.setVisibility(View.GONE);
            }
        });

        btnSendParsedToApi.setOnClickListener(v -> {
            if (parsedTx == null) return;

            tvApiTestFeedback.setVisibility(View.VISIBLE);
            tvApiTestFeedback.setText("Sending to Easy Q API webhook...");

            ApiClient client = new ApiClient(this);
            client.postTransaction(parsedTx, new ApiClient.ApiCallback() {
                @Override
                public void onSuccess(int statusCode, String responseBody) {
                    runOnUiThread(() -> {
                        tvApiTestFeedback.setText("✅ SUCCESS HTTP " + statusCode + "\nResponse: " + responseBody);
                    });
                }

                @Override
                public void onFailure(int statusCode, String errorMessage) {
                    runOnUiThread(() -> {
                        tvApiTestFeedback.setText("❌ FAILED (HTTP " + statusCode + ")\nError: " + errorMessage);
                    });
                }
            });
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
