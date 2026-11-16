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
    `package_name` VARCHAR(128) NULL COMMENT 'Name of activated package',
    `activation_status` ENUM('UNUSED', 'USED', 'REFUNDED') DEFAULT 'UNUSED',
    `activated_at` DATETIME NULL,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX `idx_trx_id` (`trx_id`),
    INDEX `idx_sender_number` (`sender_number`),
    INDEX `idx_activation_status` (`activation_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ====================================================================
-- USEFUL SQL QUERIES TO CHECK USED VS UNUSED TrxIDs
-- ====================================================================

-- 1. Check all UNUSED (অব্যবহৃত) transactions ready for package activation:
-- SELECT * FROM mfs_transactions WHERE activation_status = 'UNUSED' ORDER BY id DESC;

-- 2. Check all USED (ইতিমধ্যে ব্যবহৃত) transactions:
-- SELECT * FROM mfs_transactions WHERE activation_status = 'USED' ORDER BY id DESC;

-- 3. Check a specific TrxID submitted by a customer:
-- SELECT * FROM mfs_transactions WHERE trx_id = '9K48ABCDEF';

-- 4. Mark a TrxID as USED when activating a package:
-- UPDATE mfs_transactions 
-- SET activation_status = 'USED', package_id = 1, package_name = '30 Days VIP Package', activated_at = NOW() 
-- WHERE trx_id = '9K48ABCDEF' AND activation_status = 'UNUSED';
