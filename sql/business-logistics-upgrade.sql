-- Safe to repeat when upgrading an existing or partially migrated database.
SET @logistics_sql = (SELECT IF(COUNT(*)=0, 'ALTER TABLE biz_order_delivery ADD COLUMN company_code varchar(40) NOT NULL DEFAULT '''' AFTER company', 'SELECT 1') FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='biz_order_delivery' AND column_name='company_code');
PREPARE logistics_stmt FROM @logistics_sql;
EXECUTE logistics_stmt;
DEALLOCATE PREPARE logistics_stmt;
SET @logistics_sql = (SELECT IF(COUNT(*)=0, 'ALTER TABLE biz_order_delivery ADD COLUMN last_synced_at datetime DEFAULT NULL', 'SELECT 1') FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='biz_order_delivery' AND column_name='last_synced_at');
PREPARE logistics_stmt FROM @logistics_sql;
EXECUTE logistics_stmt;
DEALLOCATE PREPARE logistics_stmt;
SET @logistics_sql = (SELECT IF(COUNT(*)=0, 'ALTER TABLE biz_order_delivery ADD COLUMN next_query_at datetime DEFAULT NULL', 'SELECT 1') FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='biz_order_delivery' AND column_name='next_query_at');
PREPARE logistics_stmt FROM @logistics_sql;
EXECUTE logistics_stmt;
DEALLOCATE PREPARE logistics_stmt;
SET @logistics_sql = (SELECT IF(COUNT(*)=0, 'ALTER TABLE biz_order_delivery ADD COLUMN sync_error varchar(255) DEFAULT NULL', 'SELECT 1') FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='biz_order_delivery' AND column_name='sync_error');
PREPARE logistics_stmt FROM @logistics_sql;
EXECUTE logistics_stmt;
DEALLOCATE PREPARE logistics_stmt;
SET @logistics_sql = (SELECT IF(COUNT(*)=0, 'ALTER TABLE biz_order_delivery ADD COLUMN waybill_token varchar(512) DEFAULT NULL', 'SELECT 1') FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='biz_order_delivery' AND column_name='waybill_token');
PREPARE logistics_stmt FROM @logistics_sql;
EXECUTE logistics_stmt;
DEALLOCATE PREPARE logistics_stmt;
SET @logistics_sql = (SELECT IF(COUNT(*)=0, 'ALTER TABLE biz_order_delivery ADD COLUMN signed_at datetime DEFAULT NULL', 'SELECT 1') FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='biz_order_delivery' AND column_name='signed_at');
PREPARE logistics_stmt FROM @logistics_sql;
EXECUTE logistics_stmt;
DEALLOCATE PREPARE logistics_stmt;
UPDATE biz_order o JOIN biz_order_delivery d ON d.order_id=o.order_id
SET o.status='receiving'
WHERE o.fulfillment_mode='delivery' AND o.status IN ('paid','shipping') AND d.tracking_no<>'' AND d.shipped_at IS NOT NULL;
