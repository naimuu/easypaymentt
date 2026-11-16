# Easy Payment (Easy Q Payment Webhook Gateway)

**Easy Payment** is a dedicated Android background gateway app built for **Easy Q Web Applications**. It automatically detects incoming mobile financial service (MFS) SMS from **bKash**, **Nagad**, **Rocket**, and **Upay**, extracts the transaction details, and sends them via real-time HTTP POST webhooks to your database API for **instant, automated package activation**.

---

## 🚀 Key Features

1. **Intelligent MFS SMS Parsing**:
   - **bKash**: Received Money, Cash In, Merchant Payment.
   - **Nagad**: Received Money, Cash In.
   - **Rocket (DBBL / 16216)**: Received Money, Cash In.
   - **Upay & Cellfin**: Generic MFS fallbacks.
   - Extracts: **TrxID (Unique Transaction ID)**, **Sender Mobile Number**, **Amount (Tk)**, **Date/Time**, **Reference**, and **New Balance**.

2. **Real-time API Webhook**:
   - Automatically posts extracted data as a JSON payload to your Easy Q server.
   - Configurable **API Webhook URL**, **Secret Bearer Token**, and **Device Identifier**.
   - Built-in connection tester with immediate HTTP response feedback.

3. **24/7 Foreground Service & Reliability**:
   - Android 14 compliant Foreground Service (`dataSync`) with persistent notification.
   - Auto-restarts on device boot (`BOOT_COMPLETED`).
   - Local SQLite database storage to prevent duplicate TrxID processing.
   - Automatic retry engine (every 3 minutes) if network drops.

4. **In-App Testing & Management**:
   - **SMS Simulator**: Test and verify parsing with real bKash, Nagad, and Rocket SMS samples before putting the phone in production.
   - **Transaction History**: View all parsed transactions, filter by provider, and manually resend any failed requests.
   - **Branding**: Uses the official **Easy Q** logo and clean Material 3 user interface.

---

## 📦 How to Build the APK

### Method 1: Build in Android Studio (Recommended)
1. Open **Android Studio**.
2. Select **File > Open** and choose `F:\Easy payment APK`.
3. Wait for Gradle sync to complete.
4. Go to **Build > Build Bundle(s) / APK(s) > Build APK(s)**.
5. Once built, Android Studio will display a popup: click **locate** to find `app-debug.apk`.
6. Transfer `app-debug.apk` to your Android phone and tap to install.

### Method 2: Free 1-Click Cloud Build with GitHub Actions (No local Android SDK needed)
This project includes a ready-to-run GitHub Actions workflow in `.github/workflows/build-apk.yml`.
1. Initialize git and push this project to a GitHub repository:
   ```bash
   git init
   git add .
   git commit -m "Initial commit for Easy Payment"
   git remote add origin https://github.com/YOUR_USERNAME/easy-payment-apk.git
   git push -u origin main
   ```
2. In your GitHub repository, click the **Actions** tab.
3. The **Build Easy Payment APK** workflow will run automatically.
4. When finished (typically 2-3 minutes), click on the completed run and download the **EasyPayment-Debug-APK** artifact zip file.
5. Unzip and install the APK on your phone!

### Method 3: Command Line (if JDK 17 & Android SDK are in your PATH)
```bash
.\gradlew assembleDebug
```
The APK will be generated at:
`app\build\outputs\apk\debug\app-debug.apk`

---

## 📱 Phone Configuration & Permissions

For 24/7 automated operation on merchant devices:
1. **Grant SMS Permissions**:
   - When launching the app, allow **SMS Receiving** and **SMS Reading** permissions.
2. **Disable Battery Optimization (Doze Mode)**:
   - Tap the warning banner inside the app ("Disable Battery Saver") or go to:
     `Settings > Apps > Easy Payment > Battery > Unrestricted / Don't Optimize`.
3. **Xiaomi / Redmi (MIUI / HyperOS)**:
   - Enable **Autostart** in App Info.
   - Set Battery Saver to **No Restrictions**.
4. **Samsung**:
   - Add Easy Payment to **Never sleeping apps** in Device Care.

---

## 🌐 Easy Q Web API Specification

When an SMS arrives, the app sends an HTTP `POST` request to your configured webhook URL:

### HTTP Headers:
```http
POST /api/payment-webhook HTTP/1.1
Host: your-domain.com
Content-Type: application/json
Authorization: Bearer YOUR_API_SECRET_KEY
X-Device-Id: SIM1_CASHPOINT
```

### JSON Request Payload:
```json
{
  "provider": "bKash",
  "transactionType": "RECEIVED_MONEY",
  "amount": 500.0,
  "currency": "BDT",
  "senderNumber": "01712345678",
  "trxId": "9K48ABCDEF",
  "reference": "EasyQ01",
  "balanceAfter": 2450.0,
  "smsTimestamp": 1763284200000,
  "smsTimeFormatted": "2026-11-16 12:30:00",
  "rawMessage": "You have received Tk 500.00 from 01712345678. Ref EasyQ01. Fee Tk 0.00. Balance Tk 2,450.00. TrxID 9K48ABCDEF at 16/11/2026 12:30",
  "senderAddress": "bKash",
  "deviceId": "Samsung_SM-A546E"
}
```

### Expected Server Response:
Return HTTP `200 OK` with JSON:
```json
{
  "status": "success",
  "message": "Payment verified and package activated",
  "trxId": "9K48ABCDEF"
}
```

---

## 🛠️ Ready-to-use Backend Examples

Look inside the `backend_example/` directory:
- [schema.sql](file:///f:/Easy%20payment%20APK/backend_example/schema.sql): Complete MySQL database table definition with indexes.
- [webhook.php](file:///f:/Easy%20payment%20APK/backend_example/webhook.php): PHP webhook listener that validates Bearer token, stores transaction in MySQL, and triggers package activation.
- [webhook_server.js](file:///f:/Easy%20payment%20APK/backend_example/webhook_server.js): Node.js Express server implementation.
