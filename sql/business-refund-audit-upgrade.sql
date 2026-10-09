-- Re-runnable upgrade for audited offline refunds on existing databases.
USE jinse_wine;
SET NAMES utf8mb4;

SET @sql = (SELECT IF(COUNT(*) = 0,
  'ALTER TABLE biz_order_refund ADD COLUMN manual_payment_reference varchar(128) DEFAULT NULL AFTER channel_refund_id',
  'SELECT 1') FROM information_schema.columns WHERE table_schema = DATABASE()
  AND table_name = 'biz_order_refund' AND column_name = 'manual_payment_reference');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(COUNT(*) = 0,
  'ALTER TABLE biz_order_refund ADD COLUMN manual_paid_at datetime DEFAULT NULL AFTER manual_payment_reference',
  'SELECT 1') FROM information_schema.columns WHERE table_schema = DATABASE()
  AND table_name = 'biz_order_refund' AND column_name = 'manual_paid_at');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(COUNT(*) = 0,
  'ALTER TABLE biz_order_refund ADD COLUMN manual_verified_by varchar(64) DEFAULT NULL AFTER manual_paid_at',
  'SELECT 1') FROM information_schema.columns WHERE table_schema = DATABASE()
  AND table_name = 'biz_order_refund' AND column_name = 'manual_verified_by');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
