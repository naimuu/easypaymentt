<?php
/**
 * Easy Q Payment Webhook Endpoint (PHP Example)
 * 
 * Receives incoming SMS transactions from "Easy Payment" Android App
 * Automatically verifies TrxID, logs to MySQL database, and activates packages.
 */

header('Content-Type: application/json; charset=UTF-8');

// 1. Authenticate Request
$SECRET_API_KEY = "YOUR_SUPER_SECRET_KEY_HERE"; // Set this same key in Easy Payment app settings
$headers = getallheaders();
$authHeader = isset($headers['Authorization']) ? $headers['Authorization'] : (isset($headers['authorization']) ? $headers['authorization'] : '');

if (!empty($SECRET_API_KEY) && $SECRET_API_KEY !== "YOUR_SUPER_SECRET_KEY_HERE") {
    $expected = "Bearer " . $SECRET_API_KEY;
    if ($authHeader !== $expected) {
        http_response_code(401);
        echo json_encode(["status" => "error", "message" => "Unauthorized access: invalid token"]);
        exit;
    }
}

// 2. Read JSON Payload
$rawInput = file_get_contents('php://input');
$data = json_decode($rawInput, true);

if (!$data) {
    http_response_code(400);
    echo json_encode(["status" => "error", "message" => "Invalid JSON payload"]);
    exit;
}

// Ping test from app
if (isset($data['event']) && $data['event'] === 'ping') {
    http_response_code(200);
    echo json_encode(["status" => "success", "message" => "Easy Q Database API is healthy & reachable!"]);
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

// 3. Connect to Database (Replace with your actual DB credentials)
$dbHost = 'localhost';
$dbName = 'easy_q_db';
$dbUser = 'root';
$dbPass = '';

try {
    $pdo = new PDO("mysql:host=$dbHost;dbname=$dbName;charset=utf8mb4", $dbUser, $dbPass, [
        PDO::ATTR_ERRMODE => PDO::ERRMODE_EXCEPTION,
        PDO::ATTR_DEFAULT_FETCH_MODE => PDO::FETCH_ASSOC
    ]);

    // Check if TrxID already exists
    $checkStmt = $pdo->prepare("SELECT id, activation_status FROM mfs_transactions WHERE trx_id = ?");
    $checkStmt->execute([$trxId]);
    $existing = $checkStmt->fetch();

    if ($existing) {
        http_response_code(200);
        echo json_encode([
            "status" => "success",
            "message" => "TrxID already recorded",
            "trxId" => $trxId,
            "duplicate" => true
        ]);
        exit;
    }

    // Insert new transaction
    $smsDate = date('Y-m-d H:i:s', $smsTimestamp / 1000);
    $insertStmt = $pdo->prepare("
        INSERT INTO mfs_transactions 
        (provider, transaction_type, amount, sender_number, trx_id, reference, balance_after, sms_timestamp, sms_time_formatted, raw_message, device_id, activation_status)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'PENDING')
    ");
    $insertStmt->execute([
        $provider, $transactionType, $amount, $senderNumber, $trxId, $reference, $balanceAfter, $smsTimestamp, $smsDate, $rawMessage, $deviceId
    ]);
    $transactionRecordId = $pdo->lastInsertId();

    // 4. AUTOMATED PACKAGE ACTIVATION LOGIC
    // E.g. Find pending user order by senderNumber or reference or matching amount:
    /*
    $orderStmt = $pdo->prepare("
        SELECT id, user_id, package_id 
        FROM user_package_orders 
        WHERE status = 'PENDING' 
          AND (sender_number = ? OR order_id = ?) 
          AND amount = ? 
        LIMIT 1
    ");
    $orderStmt->execute([$senderNumber, $reference, $amount]);
    $order = $orderStmt->fetch();

    if ($order) {
        // Activate package for user!
        $pdo->prepare("UPDATE user_package_orders SET status = 'ACTIVE', trx_id = ? WHERE id = ?")
            ->execute([$trxId, $order['id']]);

        $pdo->prepare("UPDATE mfs_transactions SET activation_status = 'ACTIVATED', package_id = ? WHERE id = ?")
            ->execute([$order['package_id'], $transactionRecordId]);
    }
    */

    http_response_code(200);
    echo json_encode([
        "status" => "success",
        "message" => "Payment successfully recorded in Easy Q database",
        "transactionId" => $transactionRecordId,
        "trxId" => $trxId,
        "amount" => $amount,
        "sender" => $senderNumber
    ]);

} catch (Exception $e) {
    http_response_code(500);
    echo json_encode(["status" => "error", "message" => "Database error: " . $e->getMessage()]);
}
