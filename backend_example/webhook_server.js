/**
 * Easy Q Payment Webhook Server (Node.js / Express Example)
 * 
 * Install dependencies: npm install express
 * Run: node webhook_server.js
 */

const express = require('express');
const app = express();

app.use(express.json());

const SECRET_API_KEY = process.env.API_SECRET_KEY || "YOUR_SUPER_SECRET_KEY_HERE";
const PORT = process.env.PORT || 3000;

// Middleware for authentication
function authMiddleware(req, res, next) {
    if (SECRET_API_KEY && SECRET_API_KEY !== "YOUR_SUPER_SECRET_KEY_HERE") {
        const auth = req.headers['authorization'];
        if (auth !== `Bearer ${SECRET_API_KEY}`) {
            return res.status(401).json({ status: "error", message: "Unauthorized: Invalid API Token" });
        }
    }
    next();
}

app.post('/api/payment-webhook', authMiddleware, async (req, res) => {
    const data = req.body;

    // Handle Ping test from App
    if (data.event === 'ping') {
        return res.status(200).json({ status: "success", message: "Easy Q API Server is online!" });
    }

    const {
        provider,
        transactionType,
        amount,
        senderNumber,
        trxId,
        reference,
        balanceAfter,
        smsTimestamp,
        smsTimeFormatted,
        rawMessage,
        deviceId
    } = data;

    console.log(`\n========================================`);
    console.log(`🔔 NEW PAYMENT RECEIVED via ${provider}`);
    console.log(`Amount: Tk ${amount}`);
    console.log(`Sender: ${senderNumber}`);
    console.log(`TrxID:  ${trxId}`);
    console.log(`Time:   ${smsTimeFormatted}`);
    console.log(`Device: ${deviceId}`);
    console.log(`Raw:    ${rawMessage}`);
    console.log(`========================================\n`);

    if (!trxId || !amount) {
        return res.status(422).json({ status: "error", message: "Missing trxId or amount" });
    }

    // TODO: Insert into your database (e.g., MySQL, MongoDB, PostgreSQL)
    // TODO: Activate the package for customer with mobile `senderNumber` or matching `trxId`!

    return res.status(200).json({
        status: "success",
        message: "Payment verified and queued for package activation",
        trxId: trxId,
        amount: amount,
        sender: senderNumber
    });
});

app.listen(PORT, () => {
    console.log(`Easy Q Payment Webhook Server listening on port ${PORT}`);
    console.log(`Webhook URL: http://localhost:${PORT}/api/payment-webhook`);
});
