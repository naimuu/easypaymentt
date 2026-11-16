<?php
/**
 * Easy Q Payment Webhook & TrxID Verifier Endpoint (PHP Example)
 * 
 * 1. POST /webhook.php -> Receives new SMS from Easy Payment Android App and stores as 'UNUSED'
 * 2. GET /webhook.php?action=check&trx_id=XXXXX -> Checks if a TrxID is UNUSED, USED, or NOT FOUND
 * 3. POST /webhook.php?action=activate -> Activates a package and marks TrxID as 'USED'
 */

header('Content-Type: application/json; charset=UTF-8');

// Database credentials
$dbHost = 'localhost';
$dbName = 'easy_q_db';
$dbUser = 'root';
$dbPass = '';

try {
    $pdo = new PDO("mysql:host=$dbHost;dbname=$dbName;charset=utf8mb4", $dbUser, $dbPass, [
        PDO::ATTR_ERRMODE => PDO::ERRMODE_EXCEPTION,
        PDO::ATTR_DEFAULT_FETCH_MODE => PDO::FETCH_ASSOC
    ]);
} catch (Exception $e) {
    http_response_code(500);
    echo json_encode(["status" => "error", "message" => "Database connection failed"]);
    exit;
}

// ACTION 1: Check TrxID Status (USED vs UNUSED)
if (isset($_GET['action']) && $_GET['action'] === 'check') {
    $trxId = trim($_GET['trx_id'] ?? '');

    if (empty($trxId)) {
        http_response_code(400);
        echo json_encode(["status" => "error", "message" => "TrxID is required"]);
        exit;
    }

    $stmt = $pdo->prepare("SELECT trx_id, provider, amount, sender_number, activation_status, package_name, activated_at, created_at FROM mfs_transactions WHERE trx_id = ?");
    $stmt->execute([$trxId]);
    $tx = $stmt->fetch();

    if (!$tx) {
        // NOT FOUND -> Fake or not arrived yet
        http_response_code(404);
        echo json_encode([
            "status" => "not_found",
            "message" => "এই ট্রানজেকশন আইডি ডাটাবেজে পাওয়া যায়নি। টাকা পাঠানো নিশ্চিত করুন।",
            "is_valid" => false
        ]);
        exit;
    }

    if ($tx['activation_status'] === 'USED') {
        // ALREADY USED
        echo json_encode([
            "status" => "already_used",
            "message" => "সতর্কতা: এই TrxID ইতিমধ্যে ব্যবহার করা হয়েছে!",
            "is_usable" => false,
            "data" => $tx
        ]);
    } else {
        // UNUSED -> Can be used to activate package!
        echo json_encode([
            "status" => "unused",
            "message" => "সফল! এই TrxID টি অব্যবহৃত আছে এবং প্যাকেজ নেওয়ার জন্য উপযুক্ত।",
            "is_usable" => true,
            "data" => $tx
        ]);
    }
    exit;
}

// ACTION 2: Activate Package & Mark TrxID as USED
if (isset($_GET['action']) && $_GET['action'] === 'activate') {
    $rawInput = file_get_contents('php://input');
    $data = json_decode($rawInput, true);

    $trxId       = trim($data['trx_id'] ?? '');
    $packageId   = intval($data['package_id'] ?? 1);
    $packageName = trim($data['package_name'] ?? 'Easy Q Package');

    $checkStmt = $pdo->prepare("SELECT id, activation_status, amount FROM mfs_transactions WHERE trx_id = ?");
    $checkStmt->execute([$trxId]);
    $row = $checkStmt->fetch();

    if (!$row) {
        http_response_code(404);
        echo json_encode(["status" => "error", "message" => "Invalid TrxID"]);
        exit;
    }

    if ($row['activation_status'] === 'USED') {
        http_response_code(409); // Conflict
        echo json_encode(["status" => "error", "message" => "এই TrxID পূর্বে ব্যবহার করা হয়ে গেছে"]);
        exit;
    }

    // Mark as USED
    $updateStmt = $pdo->prepare("UPDATE mfs_transactions SET activation_status = 'USED', package_id = ?, package_name = ?, activated_at = NOW() WHERE id = ?");
    $updateStmt->execute([$packageId, $packageName, $row['id']]);

    echo json_encode([
        "status" => "success",
        "message" => "প্যাকেজ সফলভাবে অ্যাক্টিভেট হয়েছে এবং TrxID USED হিসেবে চিহ্নিত করা হয়েছে!",
        "trx_id" => $trxId,
        "package" => $packageName
    ]);
    exit;
}

// ACTION 3: Receive incoming SMS from Android App (Default Webhook)
$SECRET_API_KEY = "YOUR_SUPER_SECRET_KEY_HERE";
$headers = getallheaders();
$authHeader = $headers['Authorization'] ?? ($headers['authorization'] ?? '');

if (!empty($SECRET_API_KEY) && $SECRET_API_KEY !== "YOUR_SUPER_SECRET_KEY_HERE") {
    if ($authHeader !== "Bearer " . $SECRET_API_KEY) {
        http_response_code(401);
        echo json_encode(["status" => "error", "message" => "Unauthorized access"]);
        exit;
    }
}

$rawInput = file_get_contents('php://input');
$data = json_decode($rawInput, true);

if (!$data) {
    http_response_code(400);
    echo json_encode(["status" => "error", "message" => "Invalid JSON payload"]);
    exit;
}

if (isset($data['event']) && $data['event'] === 'ping') {
    http_response_code(200);
    echo json_encode(["status" => "success", "message" => "API is healthy & reachable!"]);
    exit;
}

$provider        = $data['provider'] ?? 'Unknown';
$transactionType = $data['transactionType'] ?? 'RECEIVED_MONEY';
$amount          = floatval($data['amount'] ?? 0);
$senderNumber    = $data['senderNumber'] ?? '';
$trxId           = $data['trxId'] ?? '';
$reference       = $data['reference'] ?? '';
$balanceAfter    = floatval($data['balanceAfter'] ?? 0);
$smsTimestamp    = intval($data['smsTimestamp'] ?? (time() * 1000));
$rawMessage      = $data['rawMessage'] ?? '';
$deviceId        = $data['deviceId'] ?? 'Mobile';

if (empty($trxId) || $amount <= 0) {
    http_response_code(422);
    echo json_encode(["status" => "error", "message" => "Missing required trxId or amount"]);
    exit;
}

// Check Duplicate
$checkStmt = $pdo->prepare("SELECT id, activation_status FROM mfs_transactions WHERE trx_id = ?");
$checkStmt->execute([$trxId]);
$existing = $checkStmt->fetch();

if ($existing) {
    http_response_code(200);
    echo json_encode([
        "status" => "success",
        "message" => "TrxID already exists in database",
        "trxId" => $trxId,
        "duplicate" => true,
        "activation_status" => $existing['activation_status']
    ]);
    exit;
}

// Insert as 'UNUSED' by default
$smsDate = date('Y-m-d H:i:s', $smsTimestamp / 1000);
$insertStmt = $pdo->prepare("
    INSERT INTO mfs_transactions 
    (provider, transaction_type, amount, sender_number, trx_id, reference, balance_after, sms_timestamp, sms_time_formatted, raw_message, device_id, activation_status)
    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'UNUSED')
");
$insertStmt->execute([
    $provider, $transactionType, $amount, $senderNumber, $trxId, $reference, $balanceAfter, $smsTimestamp, $smsDate, $rawMessage, $deviceId
]);

echo json_encode([
    "status" => "success",
    "message" => "পেমেন্ট সফলভাবে সংরক্ষিত হয়েছে এবং UNUSED হিসেবে চিহ্নিত হয়েছে।",
    "trxId" => $trxId,
    "amount" => $amount,
    "activation_status" => "UNUSED"
]);
