package com.easyq.easypayment.models;

import com.google.gson.annotations.SerializedName;

public class MfsTransaction {
    private long id; // Local database ID

    @SerializedName("provider")
    private String provider; // bKash, Nagad, Rocket, Upay, Other

    @SerializedName("transactionType")
    private String transactionType; // RECEIVED_MONEY, CASH_IN, PAYMENT

    @SerializedName("amount")
    private double amount;

    @SerializedName("currency")
    private String currency = "BDT";

    @SerializedName("senderNumber")
    private String senderNumber; // Mobile number of sender

    @SerializedName("receiverAccount")
    private String receiverAccount; // Merchant number / receiver SIM receiving the money

    @SerializedName("trxId")
    private String trxId; // Unique Transaction ID (e.g. 9K48ABCDEF)

    @SerializedName("reference")
    private String reference;

    @SerializedName("balanceAfter")
    private double balanceAfter;

    @SerializedName("smsTimestamp")
    private long smsTimestamp; // Milliseconds

    @SerializedName("smsTimeFormatted")
    private String smsTimeFormatted;

    @SerializedName("rawMessage")
    private String rawMessage;

    @SerializedName("senderAddress")
    private String senderAddress; // e.g. "bKash", "16216", "NAGAD"

    @SerializedName("deviceId")
    private String deviceId;

    @SerializedName("packageName")
    private String packageName;

    @SerializedName("activationStatus")
    private String activationStatus = "UNUSED"; // "UNUSED" or "USED"

    @SerializedName("activatedAt")
    private String activatedAt;

    // Local Sync State: 0 = Pending, 1 = Synced (HTTP 200), 2 = Failed
    private int syncStatus = 0;
    private int httpStatusCode = 0;
    private String apiResponseBody;
    private long lastAttemptTimestamp;

    public MfsTransaction() {
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }

    public String getTransactionType() { return transactionType; }
    public void setTransactionType(String transactionType) { this.transactionType = transactionType; }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public String getSenderNumber() { return senderNumber; }
    public void setSenderNumber(String senderNumber) { this.senderNumber = senderNumber; }

    public String getReceiverAccount() { return receiverAccount; }
    public void setReceiverAccount(String receiverAccount) { this.receiverAccount = receiverAccount; }

    public String getTrxId() { return trxId; }
    public void setTrxId(String trxId) { this.trxId = trxId; }

    public String getReference() { return reference; }
    public void setReference(String reference) { this.reference = reference; }

    public double getBalanceAfter() { return balanceAfter; }
    public void setBalanceAfter(double balanceAfter) { this.balanceAfter = balanceAfter; }

    public long getSmsTimestamp() { return smsTimestamp; }
    public void setSmsTimestamp(long smsTimestamp) { this.smsTimestamp = smsTimestamp; }

    public String getSmsTimeFormatted() { return smsTimeFormatted; }
    public void setSmsTimeFormatted(String smsTimeFormatted) { this.smsTimeFormatted = smsTimeFormatted; }

    public String getRawMessage() { return rawMessage; }
    public void setRawMessage(String rawMessage) { this.rawMessage = rawMessage; }

    public String getSenderAddress() { return senderAddress; }
    public void setSenderAddress(String senderAddress) { this.senderAddress = senderAddress; }

    public String getDeviceId() { return deviceId; }
    public void setDeviceId(String deviceId) { this.deviceId = deviceId; }

    public String getPackageName() { return packageName; }
    public void setPackageName(String packageName) { this.packageName = packageName; }

    public String getActivationStatus() { return activationStatus != null ? activationStatus : "UNUSED"; }
    public void setActivationStatus(String activationStatus) { this.activationStatus = activationStatus; }

    public String getActivatedAt() { return activatedAt; }
    public void setActivatedAt(String activatedAt) { this.activatedAt = activatedAt; }

    public int getSyncStatus() { return syncStatus; }
    public void setSyncStatus(int syncStatus) { this.syncStatus = syncStatus; }

    public int getHttpStatusCode() { return httpStatusCode; }
    public void setHttpStatusCode(int httpStatusCode) { this.httpStatusCode = httpStatusCode; }

    public String getApiResponseBody() { return apiResponseBody; }
    public void setApiResponseBody(String apiResponseBody) { this.apiResponseBody = apiResponseBody; }

    public long getLastAttemptTimestamp() { return lastAttemptTimestamp; }
    public void setLastAttemptTimestamp(long lastAttemptTimestamp) { this.lastAttemptTimestamp = lastAttemptTimestamp; }
}
