-- Assign delivery orders to a store after payment. Existing orders remain unassigned.
SET @shipping_store_sql = (SELECT IF(COUNT(*)=0, 'ALTER TABLE biz_order ADD COLUMN shipping_store_id bigint DEFAULT NULL', 'SELECT 1') FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='biz_order' AND column_name='shipping_store_id');
PREPARE shipping_store_stmt FROM @shipping_store_sql; EXECUTE shipping_store_stmt; DEALLOCATE PREPARE shipping_store_stmt;
SET @shipping_store_sql = (SELECT IF(COUNT(*)=0, 'ALTER TABLE biz_order ADD COLUMN shipping_store_name varchar(64) DEFAULT NULL', 'SELECT 1') FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='biz_order' AND column_name='shipping_store_name');
PREPARE shipping_store_stmt FROM @shipping_store_sql; EXECUTE shipping_store_stmt; DEALLOCATE PREPARE shipping_store_stmt;
SET @shipping_store_sql = (SELECT IF(COUNT(*)=0, 'ALTER TABLE biz_order ADD INDEX idx_order_shipping_store (shipping_store_id, status)', 'SELECT 1') FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name='biz_order' AND index_name='idx_order_shipping_store');
PREPARE shipping_store_stmt FROM @shipping_store_sql; EXECUTE shipping_store_stmt; DEALLOCATE PREPARE shipping_store_stmt;

CREATE TABLE IF NOT EXISTS biz_order_store_assignment (
  assignment_id bigint NOT NULL AUTO_INCREMENT,
  order_id bigint NOT NULL,
  previous_store_id bigint DEFAULT NULL,
  previous_store_name varchar(64) DEFAULT NULL,
  store_id bigint NOT NULL,
  store_name varchar(64) NOT NULL,
  operator_name varchar(64) NOT NULL,
  create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (assignment_id),
  KEY idx_order_store_assignment (order_id, assignment_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Delivery store assignment history';
