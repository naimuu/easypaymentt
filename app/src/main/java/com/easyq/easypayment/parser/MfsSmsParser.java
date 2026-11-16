package com.easyq.easypayment.parser;

import com.easyq.easypayment.models.MfsTransaction;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MfsSmsParser {

    private static final SimpleDateFormat DATE_FORMAT =
            new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);

    /**
     * Parses an incoming SMS message to determine if it is a valid MFS cash in / received payment.
     * Returns an MfsTransaction object if matched, or null if irrelevant.
     */
    public static MfsTransaction parse(String senderAddress, String messageBody, long timestamp) {
        if (messageBody == null || messageBody.trim().isEmpty()) {
            return null;
        }

        String lowerBody = messageBody.toLowerCase(Locale.ROOT);
        String lowerSender = senderAddress != null ? senderAddress.toLowerCase(Locale.ROOT) : "";

        // Check if the message contains payment receipt or cash in keywords
        boolean isCashIn = lowerBody.contains("cash in");
        boolean isReceived = lowerBody.contains("received") || lowerBody.contains("you have received") || lowerBody.contains("payment received");
        
        if (!isCashIn && !isReceived) {
            // Not a received money / cash in SMS
            return null;
        }

        // Must contain transaction ID keyword (TrxID, TxnID, TxnId, Transaction ID)
        if (!lowerBody.contains("trxid") && !lowerBody.contains("txnid") && !lowerBody.contains("transid")) {
            return null;
        }

        MfsTransaction tx = new MfsTransaction();
        tx.setRawMessage(messageBody);
        tx.setSenderAddress(senderAddress);
        tx.setSmsTimestamp(timestamp > 0 ? timestamp : System.currentTimeMillis());
        tx.setSmsTimeFormatted(DATE_FORMAT.format(new Date(tx.getSmsTimestamp())));

        // Determine Provider
        String provider = detectProvider(lowerSender, lowerBody);
        tx.setProvider(provider);

        // Determine Transaction Type
        if (isCashIn) {
            tx.setTransactionType("CASH_IN");
        } else if (lowerBody.contains("payment")) {
            tx.setTransactionType("PAYMENT");
        } else {
            tx.setTransactionType("RECEIVED_MONEY");
        }

        // 1. Extract Transaction ID (TrxID / TxnID / TxnId)
        String trxId = extractTrxId(messageBody);
        if (trxId == null || trxId.isEmpty()) {
            return null; // A valid transaction must have a unique TrxID
        }
        tx.setTrxId(trxId);

        // 2. Extract Amount
        double amount = extractAmount(messageBody);
        if (amount <= 0) {
            return null; // Must have positive amount
        }
        tx.setAmount(amount);

        // 3. Extract Sender / Counterpart Mobile Number
        String senderNumber = extractSenderNumber(messageBody);
        tx.setSenderNumber(senderNumber != null ? senderNumber : "UNKNOWN");

        // 4. Extract Reference (if available)
        String ref = extractReference(messageBody);
        tx.setReference(ref);

        // 5. Extract Balance After (if available)
        double balance = extractBalance(messageBody);
        tx.setBalanceAfter(balance);

        return tx;
    }

    private static String detectProvider(String sender, String body) {
        if (sender.contains("bkash") || body.contains("bkash")) {
            return "bKash";
        } else if (sender.contains("nagad") || body.contains("nagad")) {
            return "Nagad";
        } else if (sender.contains("16216") || sender.contains("rocket") || body.contains("rocket") || body.contains("dbbl")) {
            return "Rocket";
        } else if (sender.contains("upay") || body.contains("upay")) {
            return "Upay";
        } else if (sender.contains("cellfin") || body.contains("cellfin")) {
            return "Cellfin";
        }
        return "MFS";
    }

    private static String extractTrxId(String body) {
        // Examples: TrxID 9K48ABCDEF, TxnID: 71AB12CD, TxnId: 1234567890, TrxID: BP901XYZ
        Pattern pattern = Pattern.compile("(?:TrxID|trxid|TxnID|TxnId|Transaction ID)[:\\s]+([A-Za-z0-9]+)", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(body);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return null;
    }

    private static double extractAmount(String body) {
        // Examples: "Tk 500.00", "Tk. 1,000.00", "Tk 250", "BDT 1500"
        Pattern pattern = Pattern.compile("(?:Tk|Tk\\.|BDT)\\s*([0-9,]+(?:\\.[0-9]{1,2})?)", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(body);
        if (matcher.find()) {
            try {
                String amtStr = matcher.group(1).replace(",", "").trim();
                return Double.parseDouble(amtStr);
            } catch (Exception ignored) {
            }
        }
        return 0.0;
    }

    private static String extractSenderNumber(String body) {
        // Matches Bangladesh phone numbers: 013, 014, 015, 016, 017, 018, 019 followed by 8 digits (or Rocket 12 digit format)
        Pattern pattern = Pattern.compile("from\\s+(?:A/C:?\\s*)?(\\+?8801[3-9][0-9]{8,9}|01[3-9][0-9]{8,9})", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(body);
        if (matcher.find()) {
            String rawNum = matcher.group(1).trim();
            // Clean +88 prefix
            if (rawNum.startsWith("+88")) {
                rawNum = rawNum.substring(3);
            } else if (rawNum.startsWith("88") && rawNum.length() > 11) {
                rawNum = rawNum.substring(2);
            }
            return rawNum;
        }

        // Secondary fallback search for any Bangladeshi mobile number
        Pattern fallback = Pattern.compile("(?:01[3-9][0-9]{8})");
        Matcher fbMatcher = fallback.matcher(body);
        if (fbMatcher.find()) {
            return fbMatcher.group(0);
        }

        return null;
    }

    private static String extractReference(String body) {
        Pattern pattern = Pattern.compile("Ref\\s+([A-Za-z0-9_-]+)", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(body);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return "";
    }

    private static double extractBalance(String body) {
        Pattern pattern = Pattern.compile("Balance(?:\\s*:\\s*|\\s+Tk\\.?\\s*)([0-9,]+(?:\\.[0-9]{1,2})?)", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(body);
        if (matcher.find()) {
            try {
                String balStr = matcher.group(1).replace(",", "").trim();
                return Double.parseDouble(balStr);
            } catch (Exception ignored) {
            }
        }
        return 0.0;
    }
}
