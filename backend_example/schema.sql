-- Easy Q Payment & Package Activation Database Schema
-- Run this in your MySQL / MariaDB database

CREATE TABLE IF NOT EXISTS `mfs_transactions` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `provider` VARCHAR(32) NOT NULL COMMENT 'bKash, Nagad, Rocket, Upay',
    `transaction_type` VARCHAR(32) NOT NULL COMMENT 'RECEIVED_MONEY, CASH_IN, PAYMENT',
    `amount` DECIMAL(10,2) NOT NULL,
    `currency` VARCHAR(10) DEFAULT 'BDT',
    `sender_number` VARCHAR(32) NOT NULL COMMENT 'Customer Mobile Number',
    `trx_id` VARCHAR(64) NOT NULL UNIQUE COMMENT 'Unique Transaction ID from MFS',
    `reference` VARCHAR(128) DEFAULT NULL COMMENT 'Reference entered by customer',
    `balance_after` DECIMAL(10,2) DEFAULT 0.00,
    `sms_timestamp` BIGINT NOT NULL,
    `sms_time_formatted` DATETIME NULL,
    `raw_message` TEXT NULL,
    `device_id` VARCHAR(128) NULL,
    `package_id` INT NULL COMMENT 'ID of activated package if matched',
    `activation_status` ENUM('PENDING', 'ACTIVATED', 'MISMATCH', 'REFUNDED') DEFAULT 'PENDING',
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX `idx_trx_id` (`trx_id`),
    INDEX `idx_sender_number` (`sender_number`),
    INDEX `idx_activation_status` (`activation_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Optional: Packages table for automated activation
CREATE TABLE IF NOT EXISTS `easy_packages` (
    `id` INT AUTO_INCREMENT PRIMARY KEY,
    `package_name` VARCHAR(100) NOT NULL,
    `price` DECIMAL(10,2) NOT NULL,
    `duration_days` INT NOT NULL,
    `is_active` TINYINT(1) DEFAULT 1
);
