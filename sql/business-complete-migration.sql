-- 金色酒业完整业务扩展迁移（MySQL 8）
-- 在 business.example.sql、business-transaction-migration.sql、business-marketing-migration.sql 后执行。
-- 所有表均使用 IF NOT EXISTS，可重复执行。正式微信能力通过配置开启，未配置时服务端拒绝伪成功。
USE jinse_wine;
SET NAMES utf8mb4;

-- Complete the local demo store records so phone, navigation and map flows are testable.
UPDATE biz_store SET city = '贵港', district = '港北区',
  address = '贵港市港北区金港大道 888 号金色酒业体验店', phone = '13800002688',
  latitude = 23.0965000, longitude = 109.6086000,
  remark = '本地演示数据，正式上线前请替换为实际门店资料'
WHERE store_code = 'guigang'
  AND (COALESCE(remark, '') LIKE '本地演示%' OR COALESCE(address, '') = '' OR COALESCE(phone, '') = ''
       OR latitude IS NULL OR longitude IS NULL);
UPDATE biz_store SET city = '贵港', district = '港南区',
  address = '贵港市港南区江南大道 188 号金色酒业合作门店', phone = '13800002689',
  latitude = 23.0668000, longitude = 109.5945000,
  remark = '本地演示数据，正式上线前请替换为实际门店资料'
WHERE store_code = 'partner'
  AND (COALESCE(remark, '') LIKE '本地演示%' OR COALESCE(address, '') = '' OR COALESCE(phone, '') = ''
       OR latitude IS NULL OR longitude IS NULL);

-- Keep administrator-only scope and finance-only views out of operational roles on upgraded databases.
DELETE rm FROM sys_role_menu rm JOIN sys_menu m ON m.menu_id = rm.menu_id
JOIN sys_role r ON r.role_id = rm.role_id
WHERE r.role_key IN ('operator', 'verifier')
  AND m.perms IN ('business:store:scope', 'business:settlement:list', 'business:settlement:edit',
                  'business:refund:edit', 'business:report:export');

-- Map backend departments to stores so verifier-role SQL queries can be scoped.
SET @add_store_dept = (SELECT IF(COUNT(*) = 0,
  'ALTER TABLE biz_store ADD COLUMN dept_id bigint DEFAULT NULL AFTER group_qr',
  'SELECT 1') FROM information_schema.columns WHERE table_schema = DATABASE()
  AND table_name = 'biz_store' AND column_name = 'dept_id');
PREPARE stmt_store_dept FROM @add_store_dept; EXECUTE stmt_store_dept; DEALLOCATE PREPARE stmt_store_dept;
SET @add_store_dept_key = (SELECT IF(COUNT(*) = 0,
  'ALTER TABLE biz_store ADD KEY idx_biz_store_dept (dept_id)',
  'SELECT 1') FROM information_schema.statistics WHERE table_schema = DATABASE()
  AND table_name = 'biz_store' AND index_name = 'idx_biz_store_dept');
PREPARE stmt_store_dept_key FROM @add_store_dept_key; EXECUTE stmt_store_dept_key; DEALLOCATE PREPARE stmt_store_dept_key;

-- Map only untouched demo stores to the active default department. Formal store
-- records keep their existing mapping and must be assigned by an administrator.
UPDATE biz_store s
JOIN sys_dept d ON d.dept_name = '默认部门' AND d.status = '0'
SET s.dept_id = d.dept_id
WHERE s.dept_id IS NULL
  AND COALESCE(s.remark, '') LIKE '本地演示%';

SET @add_order_wechat_transaction = (SELECT IF(COUNT(*) = 0,
  'ALTER TABLE biz_order ADD COLUMN wechat_transaction_id varchar(64) DEFAULT NULL AFTER paid_at',
  'SELECT 1') FROM information_schema.columns WHERE table_schema = DATABASE()
  AND table_name = 'biz_order' AND column_name = 'wechat_transaction_id');
PREPARE stmt_order_wechat_transaction FROM @add_order_wechat_transaction; EXECUTE stmt_order_wechat_transaction; DEALLOCATE PREPARE stmt_order_wechat_transaction;
SET @add_order_wechat_transaction_key = (SELECT IF(COUNT(*) = 0,
  'ALTER TABLE biz_order ADD UNIQUE KEY uk_biz_order_wechat_transaction (wechat_transaction_id)',
  'SELECT 1') FROM information_schema.statistics WHERE table_schema = DATABASE()
  AND table_name = 'biz_order' AND index_name = 'uk_biz_order_wechat_transaction');
PREPARE stmt_order_wechat_transaction_key FROM @add_order_wechat_transaction_key; EXECUTE stmt_order_wechat_transaction_key; DEALLOCATE PREPARE stmt_order_wechat_transaction_key;

-- Mall pickup fulfillment and store-side redemption for commission settlement.
SET @add_order_fulfillment_mode = (SELECT IF(COUNT(*) = 0,
  'ALTER TABLE biz_order ADD COLUMN fulfillment_mode varchar(16) NOT NULL DEFAULT ''delivery'' AFTER address_snapshot',
  'SELECT 1') FROM information_schema.columns WHERE table_schema = DATABASE()
  AND table_name = 'biz_order' AND column_name = 'fulfillment_mode');
PREPARE stmt_order_fulfillment_mode FROM @add_order_fulfillment_mode; EXECUTE stmt_order_fulfillment_mode; DEALLOCATE PREPARE stmt_order_fulfillment_mode;
SET @add_order_pickup_store = (SELECT IF(COUNT(*) = 0,
  'ALTER TABLE biz_order ADD COLUMN pickup_store_id bigint DEFAULT NULL AFTER fulfillment_mode',
  'SELECT 1') FROM information_schema.columns WHERE table_schema = DATABASE()
  AND table_name = 'biz_order' AND column_name = 'pickup_store_id');
PREPARE stmt_order_pickup_store FROM @add_order_pickup_store; EXECUTE stmt_order_pickup_store; DEALLOCATE PREPARE stmt_order_pickup_store;
-- Delivery store assignment is independent of pickup fulfillment.
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

CREATE TABLE IF NOT EXISTS biz_order_pickup_redemption (
  pickup_redemption_id bigint NOT NULL AUTO_INCREMENT,
  order_id bigint NOT NULL,
  order_no varchar(64) NOT NULL,
  store_id bigint NOT NULL,
  verifier_member_id bigint NOT NULL,
  request_id varchar(64) NOT NULL,
  status varchar(16) NOT NULL DEFAULT 'confirmed',
  create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (pickup_redemption_id),
  UNIQUE KEY uk_biz_order_pickup_order (order_id),
  UNIQUE KEY uk_biz_order_pickup_request (verifier_member_id, request_id),
  KEY idx_biz_order_pickup_store_time (store_id, create_time)
) ENGINE=InnoDB COMMENT='商城门店自提核销';

CREATE TABLE IF NOT EXISTS biz_lottery_activity (
  activity_id bigint NOT NULL AUTO_INCREMENT,
  activity_code varchar(64) NOT NULL,
  activity_name varchar(128) NOT NULL,
  start_time datetime DEFAULT NULL,
  end_time datetime DEFAULT NULL,
  daily_limit int NOT NULL DEFAULT 1,
  total_limit int NOT NULL DEFAULT -1,
  used_count int NOT NULL DEFAULT 0,
  status char(1) NOT NULL DEFAULT '0',
  rule_json json DEFAULT NULL,
  create_by varchar(64) NOT NULL DEFAULT '',
  create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (activity_id), UNIQUE KEY uk_biz_lottery_activity_code (activity_code),
  KEY idx_biz_lottery_activity_status (status, start_time, end_time)
) ENGINE=InnoDB COMMENT='扫码抽奖活动';

CREATE TABLE IF NOT EXISTS biz_lottery_prize (
  prize_id bigint NOT NULL AUTO_INCREMENT,
  activity_id bigint NOT NULL,
  prize_type varchar(16) NOT NULL COMMENT 'cash/goods/points/coupon/none',
  prize_name varchar(128) NOT NULL,
  probability_bp int NOT NULL DEFAULT 0 COMMENT '万分比，全部奖项合计不超过10000',
  amount_min_cent int NOT NULL DEFAULT 0,
  amount_max_cent int NOT NULL DEFAULT 0,
  amount_rules_json text DEFAULT NULL COMMENT '固定红包金额和数量 JSON',
  points int NOT NULL DEFAULT 0,
  coupon_template_id bigint DEFAULT NULL,
  stock int NOT NULL DEFAULT -1,
  issued_count int NOT NULL DEFAULT 0,
  image_url varchar(512) NOT NULL DEFAULT '',
  redeem_store_id bigint DEFAULT NULL,
  status char(1) NOT NULL DEFAULT '0',
  create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (prize_id), UNIQUE KEY uk_biz_lottery_prize_name (activity_id, prize_name), KEY idx_biz_lottery_prize_activity (activity_id, status)
) ENGINE=InnoDB COMMENT='抽奖奖项及概率库存';

CREATE TABLE IF NOT EXISTS biz_lottery_code (
  code_id bigint NOT NULL AUTO_INCREMENT,
  activity_id bigint NOT NULL,
  draw_code varchar(128) NOT NULL,
  status varchar(16) NOT NULL DEFAULT 'unused' COMMENT 'unused/used/disabled',
  qr_url varchar(512) NOT NULL DEFAULT '',
  mini_path varchar(255) NOT NULL DEFAULT '',
  member_id bigint DEFAULT NULL,
  used_at datetime DEFAULT NULL,
  create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (code_id), UNIQUE KEY uk_biz_lottery_code (draw_code),
  KEY idx_biz_lottery_code_activity_status (activity_id, status)
) ENGINE=InnoDB COMMENT='抽奖二维码活动码';

CREATE TABLE IF NOT EXISTS biz_lottery_record (
  record_id bigint NOT NULL AUTO_INCREMENT,
  activity_id bigint NOT NULL,
  code_id bigint NOT NULL,
  member_id bigint NOT NULL,
  prize_id bigint DEFAULT NULL,
  result_type varchar(16) NOT NULL,
  result_value varchar(128) NOT NULL DEFAULT '',
  request_id varchar(64) NOT NULL,
  create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (record_id), UNIQUE KEY uk_biz_lottery_record_request (member_id, request_id),
  KEY idx_biz_lottery_record_code (code_id), KEY idx_biz_lottery_record_member (member_id, create_time)
) ENGINE=InnoDB COMMENT='抽奖流水';

CREATE TABLE IF NOT EXISTS biz_cart_item (
  cart_item_id bigint NOT NULL AUTO_INCREMENT,
  member_id bigint NOT NULL,
  product_id bigint NOT NULL,
  sku_code varchar(64) NOT NULL DEFAULT '',
  quantity int NOT NULL DEFAULT 1,
  create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (cart_item_id), UNIQUE KEY uk_biz_cart_member_sku (member_id, product_id, sku_code)
) ENGINE=InnoDB COMMENT='购物车';

CREATE TABLE IF NOT EXISTS biz_product_sku (
  sku_id bigint NOT NULL AUTO_INCREMENT,
  product_id bigint NOT NULL,
  sku_code varchar(64) NOT NULL,
  sku_name varchar(128) NOT NULL DEFAULT '',
  spec_json json DEFAULT NULL,
  price_cent int NOT NULL,
  stock int NOT NULL DEFAULT 0,
  status char(1) NOT NULL DEFAULT '0',
  PRIMARY KEY (sku_id), UNIQUE KEY uk_biz_product_sku_code (sku_code), KEY idx_biz_product_sku_product (product_id, status)
) ENGINE=InnoDB COMMENT='商品规格库存';

SET @add_sku_external_id = (SELECT IF(COUNT(*) = 0,
  'ALTER TABLE biz_product_sku ADD COLUMN external_sku_id varchar(128) NOT NULL DEFAULT '''' AFTER spec_json',
  'SELECT 1') FROM information_schema.columns WHERE table_schema = DATABASE()
  AND table_name = 'biz_product_sku' AND column_name = 'external_sku_id');
PREPARE stmt_sku_external_id FROM @add_sku_external_id; EXECUTE stmt_sku_external_id; DEALLOCATE PREPARE stmt_sku_external_id;
SET @add_sku_video_sync_status = (SELECT IF(COUNT(*) = 0,
  'ALTER TABLE biz_product_sku ADD COLUMN video_sync_status varchar(16) NOT NULL DEFAULT ''pending'' AFTER status',
  'SELECT 1') FROM information_schema.columns WHERE table_schema = DATABASE()
  AND table_name = 'biz_product_sku' AND column_name = 'video_sync_status');
PREPARE stmt_sku_video_sync_status FROM @add_sku_video_sync_status; EXECUTE stmt_sku_video_sync_status; DEALLOCATE PREPARE stmt_sku_video_sync_status;
SET @add_sku_video_last_error = (SELECT IF(COUNT(*) = 0,
  'ALTER TABLE biz_product_sku ADD COLUMN video_last_error varchar(255) NOT NULL DEFAULT '''' AFTER video_sync_status',
  'SELECT 1') FROM information_schema.columns WHERE table_schema = DATABASE()
  AND table_name = 'biz_product_sku' AND column_name = 'video_last_error');
PREPARE stmt_sku_video_last_error FROM @add_sku_video_last_error; EXECUTE stmt_sku_video_last_error; DEALLOCATE PREPARE stmt_sku_video_last_error;
SET @add_sku_video_synced_at = (SELECT IF(COUNT(*) = 0,
  'ALTER TABLE biz_product_sku ADD COLUMN video_synced_at datetime DEFAULT NULL AFTER video_last_error',
  'SELECT 1') FROM information_schema.columns WHERE table_schema = DATABASE()
  AND table_name = 'biz_product_sku' AND column_name = 'video_synced_at');
PREPARE stmt_sku_video_synced_at FROM @add_sku_video_synced_at; EXECUTE stmt_sku_video_synced_at; DEALLOCATE PREPARE stmt_sku_video_synced_at;
SET @add_sku_video_retry_at = (SELECT IF(COUNT(*) = 0,
  'ALTER TABLE biz_product_sku ADD COLUMN video_retry_at datetime DEFAULT NULL AFTER video_synced_at',
  'SELECT 1') FROM information_schema.columns WHERE table_schema = DATABASE()
  AND table_name = 'biz_product_sku' AND column_name = 'video_retry_at');
PREPARE stmt_sku_video_retry_at FROM @add_sku_video_retry_at; EXECUTE stmt_sku_video_retry_at; DEALLOCATE PREPARE stmt_sku_video_retry_at;
SET @add_sku_video_retry_count = (SELECT IF(COUNT(*) = 0,
  'ALTER TABLE biz_product_sku ADD COLUMN video_retry_count int NOT NULL DEFAULT 0 AFTER video_retry_at',
  'SELECT 1') FROM information_schema.columns WHERE table_schema = DATABASE()
  AND table_name = 'biz_product_sku' AND column_name = 'video_retry_count');
PREPARE stmt_sku_video_retry_count FROM @add_sku_video_retry_count; EXECUTE stmt_sku_video_retry_count; DEALLOCATE PREPARE stmt_sku_video_retry_count;

CREATE TABLE IF NOT EXISTS biz_shipping_template (
  template_id bigint NOT NULL AUTO_INCREMENT,
  template_name varchar(128) NOT NULL,
  free_threshold_cent int NOT NULL DEFAULT 0,
  base_fee_cent int NOT NULL DEFAULT 0,
  extra_fee_cent int NOT NULL DEFAULT 0,
  extra_quantity int NOT NULL DEFAULT 1,
  status char(1) NOT NULL DEFAULT '0',
  create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (template_id)
) ENGINE=InnoDB COMMENT='运费模板';

-- 首次安装提供一条可在后台调整的默认配送规则：满399元包邮，否则首件12元，续件5元。
INSERT INTO biz_shipping_template
  (template_name, free_threshold_cent, base_fee_cent, extra_fee_cent, extra_quantity, status)
SELECT '默认配送模板', 39900, 1200, 500, 1, '0'
WHERE NOT EXISTS (SELECT 1 FROM biz_shipping_template WHERE status = '0');

CREATE TABLE IF NOT EXISTS biz_order_delivery (
  delivery_id bigint NOT NULL AUTO_INCREMENT,
  order_id bigint NOT NULL,
  company varchar(64) NOT NULL DEFAULT '',
  company_code varchar(40) NOT NULL DEFAULT '',
  tracking_no varchar(128) NOT NULL DEFAULT '',
  status varchar(16) NOT NULL DEFAULT 'pending',
  last_trace json DEFAULT NULL,
  shipped_at datetime DEFAULT NULL,
  received_at datetime DEFAULT NULL,
  last_synced_at datetime DEFAULT NULL,
  next_query_at datetime DEFAULT NULL,
  sync_error varchar(255) DEFAULT NULL,
  waybill_token varchar(512) DEFAULT NULL,
  signed_at datetime DEFAULT NULL,
  PRIMARY KEY (delivery_id), UNIQUE KEY uk_biz_order_delivery_order (order_id)
) ENGINE=InnoDB COMMENT='订单物流';

CREATE TABLE IF NOT EXISTS biz_order_refund (
  refund_id bigint NOT NULL AUTO_INCREMENT,
  order_id bigint NOT NULL,
  member_id bigint NOT NULL,
  refund_no varchar(64) NOT NULL,
  request_id varchar(64) DEFAULT NULL,
  amount_cent int NOT NULL,
  reason varchar(255) NOT NULL DEFAULT '',
  status varchar(16) NOT NULL DEFAULT 'pending' COMMENT 'pending/approved/rejected/refunded',
  channel_status varchar(32) DEFAULT NULL,
  channel_refund_id varchar(64) DEFAULT NULL,
  manual_payment_reference varchar(128) DEFAULT NULL,
  manual_paid_at datetime DEFAULT NULL,
  manual_verified_by varchar(64) DEFAULT NULL,
  create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (refund_id), UNIQUE KEY uk_biz_order_refund_no (refund_no), UNIQUE KEY uk_biz_order_refund_request (member_id,request_id), KEY idx_biz_order_refund_order (order_id)
) ENGINE=InnoDB COMMENT='订单售后退款';

CREATE TABLE IF NOT EXISTS biz_order_return_receipt (
  receipt_id bigint NOT NULL AUTO_INCREMENT,
  refund_id bigint NOT NULL,
  order_id bigint NOT NULL,
  order_item_id bigint NOT NULL,
  quantity int NOT NULL,
  restock_quantity int NOT NULL,
  receipt_reference varchar(128) NOT NULL,
  request_id varchar(64) NOT NULL,
  received_by varchar(64) NOT NULL,
  received_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (receipt_id),
  UNIQUE KEY uk_biz_order_return_request_item (refund_id,request_id,order_item_id),
  KEY idx_biz_order_return_item (order_item_id),
  KEY idx_biz_order_return_time (received_at)
) ENGINE=InnoDB COMMENT='商城退货实收及可售入库流水';

SET @add_refund_channel_status = (SELECT IF(COUNT(*) = 0,
  'ALTER TABLE biz_order_refund ADD COLUMN channel_status varchar(32) DEFAULT NULL AFTER status',
  'SELECT 1') FROM information_schema.columns WHERE table_schema = DATABASE()
  AND table_name = 'biz_order_refund' AND column_name = 'channel_status');
PREPARE stmt_refund_channel_status FROM @add_refund_channel_status; EXECUTE stmt_refund_channel_status; DEALLOCATE PREPARE stmt_refund_channel_status;
SET @add_refund_channel_id = (SELECT IF(COUNT(*) = 0,
  'ALTER TABLE biz_order_refund ADD COLUMN channel_refund_id varchar(64) DEFAULT NULL AFTER channel_status',
  'SELECT 1') FROM information_schema.columns WHERE table_schema = DATABASE()
  AND table_name = 'biz_order_refund' AND column_name = 'channel_refund_id');
PREPARE stmt_refund_channel_id FROM @add_refund_channel_id; EXECUTE stmt_refund_channel_id; DEALLOCATE PREPARE stmt_refund_channel_id;

SET @add_refund_manual_reference = (SELECT IF(COUNT(*) = 0,
  'ALTER TABLE biz_order_refund ADD COLUMN manual_payment_reference varchar(128) DEFAULT NULL AFTER channel_refund_id',
  'SELECT 1') FROM information_schema.columns WHERE table_schema = DATABASE()
  AND table_name = 'biz_order_refund' AND column_name = 'manual_payment_reference');
PREPARE stmt_refund_manual_reference FROM @add_refund_manual_reference; EXECUTE stmt_refund_manual_reference; DEALLOCATE PREPARE stmt_refund_manual_reference;
SET @add_refund_manual_paid_at = (SELECT IF(COUNT(*) = 0,
  'ALTER TABLE biz_order_refund ADD COLUMN manual_paid_at datetime DEFAULT NULL AFTER manual_payment_reference',
  'SELECT 1') FROM information_schema.columns WHERE table_schema = DATABASE()
  AND table_name = 'biz_order_refund' AND column_name = 'manual_paid_at');
PREPARE stmt_refund_manual_paid_at FROM @add_refund_manual_paid_at; EXECUTE stmt_refund_manual_paid_at; DEALLOCATE PREPARE stmt_refund_manual_paid_at;
SET @add_refund_manual_verified_by = (SELECT IF(COUNT(*) = 0,
  'ALTER TABLE biz_order_refund ADD COLUMN manual_verified_by varchar(64) DEFAULT NULL AFTER manual_paid_at',
  'SELECT 1') FROM information_schema.columns WHERE table_schema = DATABASE()
  AND table_name = 'biz_order_refund' AND column_name = 'manual_verified_by');
PREPARE stmt_refund_manual_verified_by FROM @add_refund_manual_verified_by; EXECUTE stmt_refund_manual_verified_by; DEALLOCATE PREPARE stmt_refund_manual_verified_by;

SET @add_order_cancel_request = (SELECT IF(COUNT(*) = 0,
  'ALTER TABLE biz_order ADD COLUMN cancel_request_id varchar(64) DEFAULT NULL AFTER request_id',
  'SELECT 1') FROM information_schema.columns WHERE table_schema = DATABASE()
  AND table_name = 'biz_order' AND column_name = 'cancel_request_id');
PREPARE stmt_order_cancel_request FROM @add_order_cancel_request; EXECUTE stmt_order_cancel_request; DEALLOCATE PREPARE stmt_order_cancel_request;
SET @add_order_receive_request = (SELECT IF(COUNT(*) = 0,
  'ALTER TABLE biz_order ADD COLUMN receive_request_id varchar(64) DEFAULT NULL AFTER cancel_request_id',
  'SELECT 1') FROM information_schema.columns WHERE table_schema = DATABASE()
  AND table_name = 'biz_order' AND column_name = 'receive_request_id');
PREPARE stmt_order_receive_request FROM @add_order_receive_request; EXECUTE stmt_order_receive_request; DEALLOCATE PREPARE stmt_order_receive_request;

SET @add_refund_request = (SELECT IF(COUNT(*) = 0,
  'ALTER TABLE biz_order_refund ADD COLUMN request_id varchar(64) DEFAULT NULL AFTER refund_no',
  'SELECT 1') FROM information_schema.columns WHERE table_schema = DATABASE()
  AND table_name = 'biz_order_refund' AND column_name = 'request_id');
PREPARE stmt_refund_request FROM @add_refund_request; EXECUTE stmt_refund_request; DEALLOCATE PREPARE stmt_refund_request;
SET @add_refund_request_key = (SELECT IF(COUNT(*) = 0,
  'ALTER TABLE biz_order_refund ADD UNIQUE KEY uk_biz_order_refund_request (member_id,request_id)',
  'SELECT 1') FROM information_schema.statistics WHERE table_schema = DATABASE()
  AND table_name = 'biz_order_refund' AND index_name = 'uk_biz_order_refund_request');
PREPARE stmt_refund_request_key FROM @add_refund_request_key; EXECUTE stmt_refund_request_key; DEALLOCATE PREPARE stmt_refund_request_key;

CREATE TABLE IF NOT EXISTS biz_point_rule (
  rule_id bigint NOT NULL AUTO_INCREMENT,
  rule_code varchar(64) NOT NULL,
  rule_name varchar(128) NOT NULL,
  source_type varchar(32) NOT NULL,
  points int NOT NULL DEFAULT 0,
  daily_limit int NOT NULL DEFAULT -1,
  validity_days int NOT NULL DEFAULT 0,
  status char(1) NOT NULL DEFAULT '0',
  rule_json json DEFAULT NULL,
  update_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (rule_id), UNIQUE KEY uk_biz_point_rule_code (rule_code)
) ENGINE=InnoDB COMMENT='积分获取及消费规则';

-- 旧版已建积分流水但没有有效期列时补齐；历史流水保持永久有效（NULL）。
SET @add_point_expiry = (SELECT IF(COUNT(*) = 0,
  'ALTER TABLE biz_point_record ADD COLUMN expires_at datetime DEFAULT NULL AFTER title',
  'SELECT 1') FROM information_schema.columns WHERE table_schema = DATABASE()
  AND table_name = 'biz_point_record' AND column_name = 'expires_at');
PREPARE stmt_point_expiry FROM @add_point_expiry; EXECUTE stmt_point_expiry; DEALLOCATE PREPARE stmt_point_expiry;

CREATE TABLE IF NOT EXISTS biz_promotion_config (
  config_id bigint NOT NULL AUTO_INCREMENT,
  max_level int NOT NULL DEFAULT 3,
  customer_rate_bp int NOT NULL DEFAULT 1000,
  staff_rate_bp int NOT NULL DEFAULT 1500,
  level_rates_json json NOT NULL,
  settlement_days int NOT NULL DEFAULT 7,
  min_withdraw_cent int NOT NULL DEFAULT 1000,
  refund_policy varchar(32) NOT NULL DEFAULT 'reverse',
  status char(1) NOT NULL DEFAULT '0',
  update_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (config_id)
) ENGINE=InnoDB COMMENT='分销佣金规则';

-- 退款佣金闭环字段：保留冻结前状态和原始金额，支持部分退款按比例冲回。
SET @add_promotion_original = (SELECT IF(COUNT(*) = 0, 'ALTER TABLE biz_promotion_record ADD COLUMN original_amount_cent int DEFAULT NULL AFTER amount_cent', 'SELECT 1') FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'biz_promotion_record' AND column_name = 'original_amount_cent');
PREPARE stmt_promotion_original FROM @add_promotion_original; EXECUTE stmt_promotion_original; DEALLOCATE PREPARE stmt_promotion_original;
SET @add_promotion_adjustment = (SELECT IF(COUNT(*) = 0, 'ALTER TABLE biz_promotion_record ADD COLUMN refund_adjustment_cent int NOT NULL DEFAULT 0 AFTER original_amount_cent', 'SELECT 1') FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'biz_promotion_record' AND column_name = 'refund_adjustment_cent');
PREPARE stmt_promotion_adjustment FROM @add_promotion_adjustment; EXECUTE stmt_promotion_adjustment; DEALLOCATE PREPARE stmt_promotion_adjustment;
SET @add_promotion_hold = (SELECT IF(COUNT(*) = 0, 'ALTER TABLE biz_promotion_record ADD COLUMN refund_hold_status varchar(16) DEFAULT NULL AFTER status', 'SELECT 1') FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'biz_promotion_record' AND column_name = 'refund_hold_status');
PREPARE stmt_promotion_hold FROM @add_promotion_hold; EXECUTE stmt_promotion_hold; DEALLOCATE PREPARE stmt_promotion_hold;
SET @add_promotion_recovery = (SELECT IF(COUNT(*) = 0, 'ALTER TABLE biz_promotion_record ADD COLUMN refund_recovery_required tinyint(1) NOT NULL DEFAULT 0 AFTER refund_adjustment_cent', 'SELECT 1') FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'biz_promotion_record' AND column_name = 'refund_recovery_required');
PREPARE stmt_promotion_recovery FROM @add_promotion_recovery; EXECUTE stmt_promotion_recovery; DEALLOCATE PREPARE stmt_promotion_recovery;
UPDATE biz_promotion_record SET original_amount_cent=amount_cent WHERE original_amount_cent IS NULL;

CREATE TABLE IF NOT EXISTS biz_promotion_settlement (
  settlement_id bigint NOT NULL AUTO_INCREMENT,
  member_id bigint NOT NULL,
  amount_cent int NOT NULL,
  status varchar(16) NOT NULL DEFAULT 'pending' COMMENT 'pending/processing/paid/failed',
  request_id varchar(64) DEFAULT NULL,
  payment_reference varchar(128) DEFAULT NULL COMMENT '线下转账凭证号',
  failure_reason varchar(500) DEFAULT NULL COMMENT '提现退回原因',
  paid_at datetime DEFAULT NULL,
  create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (settlement_id), UNIQUE KEY uk_biz_promotion_settlement_request (member_id, request_id), KEY idx_biz_promotion_settlement_member (member_id, status)
) ENGINE=InnoDB COMMENT='佣金提现结算';

SET @add_settlement_payment_reference = (SELECT IF(COUNT(*) = 0,
  'ALTER TABLE biz_promotion_settlement ADD COLUMN payment_reference varchar(128) DEFAULT NULL COMMENT ''线下转账凭证号'' AFTER request_id',
  'SELECT 1') FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'biz_promotion_settlement' AND column_name = 'payment_reference');
PREPARE stmt_settlement_payment_reference FROM @add_settlement_payment_reference; EXECUTE stmt_settlement_payment_reference; DEALLOCATE PREPARE stmt_settlement_payment_reference;
SET @add_settlement_failure_reason = (SELECT IF(COUNT(*) = 0,
  'ALTER TABLE biz_promotion_settlement ADD COLUMN failure_reason varchar(500) DEFAULT NULL COMMENT ''提现退回原因'' AFTER payment_reference',
  'SELECT 1') FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'biz_promotion_settlement' AND column_name = 'failure_reason');
PREPARE stmt_settlement_failure_reason FROM @add_settlement_failure_reason; EXECUTE stmt_settlement_failure_reason; DEALLOCATE PREPARE stmt_settlement_failure_reason;

CREATE TABLE IF NOT EXISTS biz_promotion_settlement_item (
  settlement_id bigint NOT NULL,
  promotion_record_id bigint NOT NULL,
  PRIMARY KEY (settlement_id, promotion_record_id), KEY idx_biz_settlement_record (promotion_record_id)
) ENGINE=InnoDB COMMENT='提现单与佣金流水关联';

SET @drop_settlement_record_unique = (SELECT IF(COUNT(*) > 0,
  'ALTER TABLE biz_promotion_settlement_item DROP INDEX uk_biz_settlement_record',
  'SELECT 1') FROM information_schema.statistics WHERE table_schema = DATABASE()
  AND table_name = 'biz_promotion_settlement_item' AND index_name = 'uk_biz_settlement_record');
PREPARE stmt_drop_settlement_record_unique FROM @drop_settlement_record_unique; EXECUTE stmt_drop_settlement_record_unique; DEALLOCATE PREPARE stmt_drop_settlement_record_unique;
SET @add_settlement_record_index = (SELECT IF(COUNT(*) = 0,
  'ALTER TABLE biz_promotion_settlement_item ADD KEY idx_biz_settlement_record (promotion_record_id)',
  'SELECT 1') FROM information_schema.statistics WHERE table_schema = DATABASE()
  AND table_name = 'biz_promotion_settlement_item' AND index_name = 'idx_biz_settlement_record');
PREPARE stmt_add_settlement_record_index FROM @add_settlement_record_index; EXECUTE stmt_add_settlement_record_index; DEALLOCATE PREPARE stmt_add_settlement_record_index;

SET @add_settlement_request_key = (SELECT IF(COUNT(*) = 0,
  'ALTER TABLE biz_promotion_settlement ADD UNIQUE KEY uk_biz_promotion_settlement_request (member_id, request_id)',
  'SELECT 1') FROM information_schema.statistics WHERE table_schema = DATABASE()
  AND table_name = 'biz_promotion_settlement' AND index_name = 'uk_biz_promotion_settlement_request');
PREPARE stmt_settlement_request_key FROM @add_settlement_request_key; EXECUTE stmt_settlement_request_key; DEALLOCATE PREPARE stmt_settlement_request_key;

CREATE TABLE IF NOT EXISTS biz_ranking_config (
  config_id bigint NOT NULL AUTO_INCREMENT,
  period_type varchar(16) NOT NULL DEFAULT 'month',
  reward_json json NOT NULL,
  start_time datetime DEFAULT NULL,
  end_time datetime DEFAULT NULL,
  status char(1) NOT NULL DEFAULT '0',
  update_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (config_id)
) ENGINE=InnoDB COMMENT='销冠天梯配置';

CREATE TABLE IF NOT EXISTS biz_ranking_period (
  period_id bigint NOT NULL AUTO_INCREMENT,
  config_id bigint NOT NULL,
  period_start datetime NOT NULL,
  period_end datetime NOT NULL,
  period_type varchar(16) NOT NULL,
  reward_json json NOT NULL,
  result_json json NOT NULL,
  reward_count int NOT NULL DEFAULT 0,
  create_by varchar(64) NOT NULL DEFAULT '',
  create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (period_id), UNIQUE KEY uk_ranking_period (config_id, period_start)
) ENGINE=InnoDB COMMENT='排行榜已结算周期与排名奖励快照';

CREATE TABLE IF NOT EXISTS biz_ranking_reward (
  reward_id bigint NOT NULL AUTO_INCREMENT,
  config_id bigint NOT NULL,
  period_start datetime NOT NULL,
  period_end datetime NOT NULL,
  member_id bigint NOT NULL,
  rank_no int NOT NULL,
  points int NOT NULL DEFAULT 0,
  subsidy_cent int NOT NULL DEFAULT 0,
  status varchar(16) NOT NULL DEFAULT 'pending',
  issue_reference varchar(128) DEFAULT NULL,
  issued_at datetime DEFAULT NULL,
  PRIMARY KEY (reward_id), UNIQUE KEY uk_biz_ranking_reward_period_member (config_id, period_start, member_id)
) ENGINE=InnoDB COMMENT='排行榜奖励发放';

-- 补贴发放必须保留财务凭证号，便于对账和审计；旧库按需补齐。
SET @add_ranking_issue_reference = (SELECT IF(COUNT(*) = 0,
  'ALTER TABLE biz_ranking_reward ADD COLUMN issue_reference varchar(128) DEFAULT NULL AFTER status',
  'SELECT 1') FROM information_schema.columns WHERE table_schema = DATABASE()
  AND table_name = 'biz_ranking_reward' AND column_name = 'issue_reference');
PREPARE stmt_ranking_issue_reference FROM @add_ranking_issue_reference; EXECUTE stmt_ranking_issue_reference; DEALLOCATE PREPARE stmt_ranking_issue_reference;

CREATE TABLE IF NOT EXISTS biz_social_campaign (
  campaign_id bigint NOT NULL AUTO_INCREMENT,
  campaign_type varchar(16) NOT NULL COMMENT 'promotion/coupon/points',
  title varchar(128) NOT NULL,
  content text NOT NULL,
  coupon_template_id bigint DEFAULT NULL,
  target_json json DEFAULT NULL,
  scheduled_at datetime DEFAULT NULL,
  sent_count int NOT NULL DEFAULT 0,
  status varchar(16) NOT NULL DEFAULT 'draft',
  create_by varchar(64) NOT NULL DEFAULT '',
  create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (campaign_id)
) ENGINE=InnoDB COMMENT='VIP社群运营活动';

CREATE TABLE IF NOT EXISTS biz_social_delivery (
  delivery_id bigint NOT NULL AUTO_INCREMENT,
  campaign_id bigint NOT NULL,
  member_id bigint NOT NULL,
  channel varchar(32) NOT NULL DEFAULT 'subscribe',
  subscription_id bigint DEFAULT NULL,
  status varchar(16) NOT NULL DEFAULT 'pending',
  attempt_count int NOT NULL DEFAULT 0,
  next_retry_at datetime DEFAULT NULL,
  locked_until datetime DEFAULT NULL,
  sent_at datetime DEFAULT NULL,
  provider_message_id varchar(128) NOT NULL DEFAULT '',
  error_message varchar(255) NOT NULL DEFAULT '',
  PRIMARY KEY (delivery_id), UNIQUE KEY uk_biz_social_delivery (campaign_id, member_id, channel)
) ENGINE=InnoDB COMMENT='社群通知投递记录';

CREATE TABLE IF NOT EXISTS biz_social_group_delivery (
  delivery_id bigint NOT NULL AUTO_INCREMENT,
  campaign_id bigint NOT NULL,
  chat_id varchar(128) NOT NULL,
  status varchar(16) NOT NULL DEFAULT 'pending' COMMENT 'pending/sending/retry/sent/failed/unknown',
  attempt_count int NOT NULL DEFAULT 0,
  next_retry_at datetime DEFAULT NULL,
  locked_until datetime DEFAULT NULL,
  sent_at datetime DEFAULT NULL,
  error_message varchar(255) NOT NULL DEFAULT '',
  PRIMARY KEY (delivery_id),
  UNIQUE KEY uk_biz_social_group_delivery (campaign_id, chat_id),
  KEY idx_biz_social_group_delivery_status (status, next_retry_at, locked_until)
) ENGINE=InnoDB COMMENT='企业微信群通知投递记录';

CREATE TABLE IF NOT EXISTS biz_social_subscription (
  subscription_id bigint NOT NULL AUTO_INCREMENT,
  member_id bigint NOT NULL,
  template_id varchar(128) NOT NULL,
  request_id varchar(64) DEFAULT NULL,
  status varchar(16) NOT NULL DEFAULT 'available' COMMENT 'available/reserved/consumed/rejected',
  delivery_id bigint DEFAULT NULL,
  reserved_until datetime DEFAULT NULL,
  granted_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  consumed_at datetime DEFAULT NULL,
  PRIMARY KEY (subscription_id),
  UNIQUE KEY uk_biz_social_subscription_request (member_id, request_id),
  KEY idx_biz_social_subscription_member (member_id, template_id, status, granted_at)
) ENGINE=InnoDB COMMENT='会员主动授权的一次性订阅消息额度';

SET @add_social_subscription_request_id = (SELECT IF(COUNT(*) = 0,
  'ALTER TABLE biz_social_subscription ADD COLUMN request_id varchar(64) DEFAULT NULL AFTER template_id',
  'SELECT 1') FROM information_schema.columns WHERE table_schema = DATABASE()
  AND table_name = 'biz_social_subscription' AND column_name = 'request_id');
PREPARE stmt_social_subscription_request_id FROM @add_social_subscription_request_id; EXECUTE stmt_social_subscription_request_id; DEALLOCATE PREPARE stmt_social_subscription_request_id;
SET @add_social_subscription_request_key = (SELECT IF(COUNT(*) = 0,
  'ALTER TABLE biz_social_subscription ADD UNIQUE KEY uk_biz_social_subscription_request (member_id, request_id)',
  'SELECT 1') FROM information_schema.statistics WHERE table_schema = DATABASE()
  AND table_name = 'biz_social_subscription' AND index_name = 'uk_biz_social_subscription_request');
PREPARE stmt_social_subscription_request_key FROM @add_social_subscription_request_key; EXECUTE stmt_social_subscription_request_key; DEALLOCATE PREPARE stmt_social_subscription_request_key;

CREATE TABLE IF NOT EXISTS biz_channel_config (
  config_key varchar(64) NOT NULL,
  config_value text NOT NULL,
  update_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (config_key)
) ENGINE=InnoDB COMMENT='微信支付、视频号及群二维码配置';

CREATE TABLE IF NOT EXISTS biz_production_verification (
  verification_id bigint NOT NULL AUTO_INCREMENT,
  check_key varchar(32) NOT NULL,
  config_fingerprint char(64) NOT NULL,
  evidence varchar(1000) NOT NULL,
  verified_by varchar(64) NOT NULL,
  verified_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (verification_id),
  KEY idx_production_verification_check (check_key, verification_id)
) ENGINE=InnoDB COMMENT='正式平台上线验收证据';

CREATE TABLE IF NOT EXISTS biz_video_product (
  mapping_id bigint NOT NULL AUTO_INCREMENT,
  product_id bigint NOT NULL,
  channel varchar(16) NOT NULL DEFAULT 'video',
  external_id varchar(128) NOT NULL DEFAULT '',
  sync_status varchar(16) NOT NULL DEFAULT 'pending',
  last_error varchar(255) NOT NULL DEFAULT '',
  synced_at datetime DEFAULT NULL,
  publish_payload_json longtext DEFAULT NULL,
  publish_status varchar(32) NOT NULL DEFAULT 'draft',
  publish_error varchar(255) NOT NULL DEFAULT '',
  external_status varchar(32) NOT NULL DEFAULT '',
  published_at datetime DEFAULT NULL,
  PRIMARY KEY (mapping_id), UNIQUE KEY uk_biz_video_product (product_id, channel)
) ENGINE=InnoDB COMMENT='视频号/微信小店商品映射';

SET @add_video_publish_payload_json = (SELECT IF(COUNT(*) = 0,
  'ALTER TABLE biz_video_product ADD COLUMN publish_payload_json longtext DEFAULT NULL AFTER synced_at',
  'SELECT 1') FROM information_schema.columns WHERE table_schema = DATABASE()
  AND table_name = 'biz_video_product' AND column_name = 'publish_payload_json');
PREPARE stmt_video_publish_payload_json FROM @add_video_publish_payload_json; EXECUTE stmt_video_publish_payload_json; DEALLOCATE PREPARE stmt_video_publish_payload_json;
SET @add_video_publish_status = (SELECT IF(COUNT(*) = 0,
  'ALTER TABLE biz_video_product ADD COLUMN publish_status varchar(32) NOT NULL DEFAULT ''draft'' AFTER publish_payload_json',
  'SELECT 1') FROM information_schema.columns WHERE table_schema = DATABASE()
  AND table_name = 'biz_video_product' AND column_name = 'publish_status');
PREPARE stmt_video_publish_status FROM @add_video_publish_status; EXECUTE stmt_video_publish_status; DEALLOCATE PREPARE stmt_video_publish_status;
SET @add_video_publish_error = (SELECT IF(COUNT(*) = 0,
  'ALTER TABLE biz_video_product ADD COLUMN publish_error varchar(255) NOT NULL DEFAULT '''' AFTER publish_status',
  'SELECT 1') FROM information_schema.columns WHERE table_schema = DATABASE()
  AND table_name = 'biz_video_product' AND column_name = 'publish_error');
PREPARE stmt_video_publish_error FROM @add_video_publish_error; EXECUTE stmt_video_publish_error; DEALLOCATE PREPARE stmt_video_publish_error;
SET @add_video_external_status = (SELECT IF(COUNT(*) = 0,
  'ALTER TABLE biz_video_product ADD COLUMN external_status varchar(32) NOT NULL DEFAULT '''' AFTER publish_error',
  'SELECT 1') FROM information_schema.columns WHERE table_schema = DATABASE()
  AND table_name = 'biz_video_product' AND column_name = 'external_status');
PREPARE stmt_video_external_status FROM @add_video_external_status; EXECUTE stmt_video_external_status; DEALLOCATE PREPARE stmt_video_external_status;
SET @add_video_published_at = (SELECT IF(COUNT(*) = 0,
  'ALTER TABLE biz_video_product ADD COLUMN published_at datetime DEFAULT NULL AFTER external_status',
  'SELECT 1') FROM information_schema.columns WHERE table_schema = DATABASE()
  AND table_name = 'biz_video_product' AND column_name = 'published_at');
PREPARE stmt_video_published_at FROM @add_video_published_at; EXECUTE stmt_video_published_at; DEALLOCATE PREPARE stmt_video_published_at;

CREATE TABLE IF NOT EXISTS biz_video_order (
  order_id bigint NOT NULL AUTO_INCREMENT,
  external_order_id varchar(64) NOT NULL,
  external_status int NOT NULL DEFAULT 0,
  created_at datetime DEFAULT NULL,
  updated_at datetime DEFAULT NULL,
  order_price_cent bigint NOT NULL DEFAULT 0,
  freight_cent bigint NOT NULL DEFAULT 0,
  merchant_receive_cent bigint NOT NULL DEFAULT 0,
  aftersale_count int NOT NULL DEFAULT 0,
  items_json json DEFAULT NULL,
  last_synced_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (order_id),
  UNIQUE KEY uk_biz_video_order_external (external_order_id),
  KEY idx_biz_video_order_status_time (external_status, updated_at)
) ENGINE=InnoDB COMMENT='微信小店只读订单镜像，不存买家身份和收货信息';

CREATE TABLE IF NOT EXISTS biz_video_order_sync_state (
  sync_key varchar(32) NOT NULL,
  completed_through bigint NOT NULL DEFAULT 0,
  window_start bigint NOT NULL DEFAULT 0,
  window_end bigint NOT NULL DEFAULT 0,
  next_key varchar(512) NOT NULL DEFAULT '',
  updated_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (sync_key)
) ENGINE=InnoDB COMMENT='微信小店订单增量同步游标';
INSERT IGNORE INTO biz_video_order_sync_state(sync_key) VALUES ('orders');

CREATE TABLE IF NOT EXISTS biz_video_order_fulfillment (
  fulfillment_id bigint NOT NULL AUTO_INCREMENT,
  external_order_id varchar(64) NOT NULL,
  delivery_id varchar(64) NOT NULL,
  waybill_id varchar(128) NOT NULL,
  request_id varchar(64) NOT NULL,
  status varchar(20) NOT NULL DEFAULT 'submitting' COMMENT 'submitting/submitted/unknown/verified',
  last_error varchar(255) NOT NULL DEFAULT '',
  create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (fulfillment_id),
  UNIQUE KEY uk_biz_video_order_fulfillment_order (external_order_id),
  UNIQUE KEY uk_biz_video_order_fulfillment_request (request_id)
) ENGINE=InnoDB COMMENT='微信小店订单发货请求与幂等状态';

CREATE TABLE IF NOT EXISTS biz_video_aftersale (
  aftersale_id bigint NOT NULL AUTO_INCREMENT,
  external_aftersale_id varchar(64) NOT NULL,
  external_order_id varchar(64) NOT NULL DEFAULT '',
  external_status varchar(48) NOT NULL DEFAULT '',
  aftersale_type varchar(24) NOT NULL DEFAULT '',
  refund_amount_cent bigint NOT NULL DEFAULT 0,
  created_at datetime DEFAULT NULL,
  updated_at datetime DEFAULT NULL,
  last_synced_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (aftersale_id),
  UNIQUE KEY uk_biz_video_aftersale_external (external_aftersale_id),
  KEY idx_biz_video_aftersale_status_time (external_status, updated_at)
) ENGINE=InnoDB COMMENT='微信小店售后单脱敏镜像';

CREATE TABLE IF NOT EXISTS biz_video_aftersale_sync_state (
  sync_key varchar(32) NOT NULL,
  completed_through bigint NOT NULL DEFAULT 0,
  window_start bigint NOT NULL DEFAULT 0,
  window_end bigint NOT NULL DEFAULT 0,
  next_key varchar(512) NOT NULL DEFAULT '',
  updated_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (sync_key)
) ENGINE=InnoDB COMMENT='微信小店售后增量同步游标';
INSERT IGNORE INTO biz_video_aftersale_sync_state(sync_key) VALUES ('aftersales');

CREATE TABLE IF NOT EXISTS biz_video_refund_ledger (
  ledger_id bigint NOT NULL AUTO_INCREMENT,
  external_aftersale_id varchar(64) NOT NULL,
  external_order_id varchar(64) NOT NULL DEFAULT '',
  amount_cent bigint NOT NULL DEFAULT 0,
  external_status varchar(48) NOT NULL DEFAULT '',
  ledger_status varchar(24) NOT NULL DEFAULT 'pending' COMMENT 'pending/confirmed/failed/unknown',
  confirmed_at datetime DEFAULT NULL,
  last_error varchar(255) NOT NULL DEFAULT '',
  created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (ledger_id),
  UNIQUE KEY uk_biz_video_refund_ledger_aftersale (external_aftersale_id),
  KEY idx_biz_video_refund_ledger_status_time (ledger_status, updated_at)
) ENGINE=InnoDB COMMENT='微信小店退款对账台账，不并入本地商城订单';

CREATE TABLE IF NOT EXISTS biz_video_feed_mount (
  mount_id bigint NOT NULL AUTO_INCREMENT,
  product_id bigint NOT NULL,
  external_product_id varchar(128) NOT NULL DEFAULT '',
  feed_id varchar(128) NOT NULL,
  nonce_id varchar(128) NOT NULL DEFAULT '',
  mount_title varchar(128) NOT NULL DEFAULT '',
  status varchar(24) NOT NULL DEFAULT 'draft' COMMENT 'draft/submitting/submitted/verified/unknown/failed/disabled',
  last_error varchar(255) NOT NULL DEFAULT '',
  request_id varchar(64) NOT NULL DEFAULT '',
  submitted_at datetime DEFAULT NULL,
  verified_at datetime DEFAULT NULL,
  create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (mount_id),
  UNIQUE KEY uk_biz_video_feed_mount (product_id, feed_id, nonce_id),
  KEY idx_biz_video_feed_mount_status (status, update_time)
) ENGINE=InnoDB COMMENT='视频号短视频商品挂载状态';

CREATE TABLE IF NOT EXISTS biz_reward_transfer (
  transfer_id bigint NOT NULL AUTO_INCREMENT,
  reward_id bigint NOT NULL,
  member_id bigint NOT NULL,
  request_id varchar(64) NOT NULL,
  out_bill_no varchar(32) NOT NULL,
  transfer_bill_no varchar(64) DEFAULT NULL,
  amount_cent int NOT NULL,
  openid varchar(128) NOT NULL,
  status varchar(24) NOT NULL DEFAULT 'SUBMITTING' COMMENT 'SUBMITTING/ACCEPTED/PROCESSING/WAIT_USER_CONFIRM/SUCCESS/FAIL/CANCELLED/UNKNOWN',
  package_info varchar(2048) DEFAULT NULL,
  fail_reason varchar(255) NOT NULL DEFAULT '',
  create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (transfer_id),
  UNIQUE KEY uk_biz_reward_transfer_bill (out_bill_no),
  UNIQUE KEY uk_biz_reward_transfer_request (member_id, request_id),
  KEY idx_biz_reward_transfer_reward (reward_id, transfer_id),
  KEY idx_biz_reward_transfer_state (status, update_time)
) ENGINE=InnoDB COMMENT='微信现金奖商户转账尝试';

CREATE TABLE IF NOT EXISTS biz_report_export (
  export_id bigint NOT NULL AUTO_INCREMENT,
  report_type varchar(32) NOT NULL,
  filters_json json DEFAULT NULL,
  file_name varchar(255) NOT NULL,
  row_count int NOT NULL DEFAULT 0,
  create_by varchar(64) NOT NULL DEFAULT '',
  create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (export_id)
) ENGINE=InnoDB COMMENT='财务报表导出记录';

INSERT IGNORE INTO biz_shipping_template(template_id, template_name, free_threshold_cent, base_fee_cent, extra_fee_cent, extra_quantity, status)
VALUES (1, '默认运费', 39900, 1200, 500, 1, '0');

SET @add_order_item_sku = (SELECT IF(COUNT(*) = 0,
  'ALTER TABLE biz_order_item ADD COLUMN sku_code varchar(64) NOT NULL DEFAULT '''' AFTER product_id',
  'SELECT 1') FROM information_schema.columns WHERE table_schema = DATABASE()
  AND table_name = 'biz_order_item' AND column_name = 'sku_code');
PREPARE stmt_order_item_sku FROM @add_order_item_sku; EXECUTE stmt_order_item_sku; DEALLOCATE PREPARE stmt_order_item_sku;
INSERT IGNORE INTO biz_point_rule(rule_code, rule_name, source_type, points, daily_limit, validity_days, status, rule_json)
VALUES ('order_paid', '商城消费积分', 'order_paid', 1, -1, 0, '0', JSON_OBJECT('per_cent', 1)),
       ('invite_register', '邀请注册', 'invite_register', 100, 20, 0, '0', JSON_OBJECT()),
       ('daily_sign', '每日签到', 'daily_sign', 10, 1, 0, '0', JSON_OBJECT()),
       ('ranking', '销冠排行榜奖励', 'ranking', 0, -1, 0, '0', JSON_OBJECT());
INSERT IGNORE INTO biz_promotion_config(config_id, max_level, customer_rate_bp, staff_rate_bp, level_rates_json, settlement_days, min_withdraw_cent, refund_policy, status)
VALUES (1, 3, 1000, 1500, JSON_ARRAY(1000, 500, 200), 7, 1000, 'reverse', '0');
UPDATE biz_promotion_config SET min_withdraw_cent=1000 WHERE min_withdraw_cent IS NULL OR min_withdraw_cent<=0;
INSERT IGNORE INTO biz_ranking_config(config_id, period_type, reward_json, status)
VALUES (1, 'month', JSON_ARRAY(JSON_OBJECT('rank',1,'points',1000,'subsidyCent',50000), JSON_OBJECT('rank',2,'points',500,'subsidyCent',20000), JSON_OBJECT('rank',3,'points',300,'subsidyCent',10000)), '0');
INSERT IGNORE INTO biz_channel_config(config_key, config_value) VALUES
('wechat.pay.enabled','false'), ('wechat.transfer.enabled','false'), ('wechat.merchant.appId',''),
('wechat.video.finderUserName',''), ('wechat.video.shopId',''), ('wechat.video.syncEnabled','false'),
('wechat.work.groupQr',''), ('wechat.work.groupQrExpiresAt','');

-- 旧版推广关系表补充层级字段，兼容已上线数据库。
SET @add_relation_level = (SELECT IF(COUNT(*) = 0,
  'ALTER TABLE biz_promotion_relation ADD COLUMN level_no tinyint NOT NULL DEFAULT 1 AFTER inviter_member_id',
  'SELECT 1') FROM information_schema.columns WHERE table_schema = DATABASE()
  AND table_name = 'biz_promotion_relation' AND column_name = 'level_no');
PREPARE stmt_relation_level FROM @add_relation_level; EXECUTE stmt_relation_level; DEALLOCATE PREPARE stmt_relation_level;

SET @add_reward_store = (SELECT IF(COUNT(*) = 0,
  'ALTER TABLE biz_reward ADD COLUMN redeem_store_id bigint DEFAULT NULL AFTER image_url',
  'SELECT 1') FROM information_schema.columns WHERE table_schema = DATABASE()
  AND table_name = 'biz_reward' AND column_name = 'redeem_store_id');
PREPARE stmt_reward_store FROM @add_reward_store; EXECUTE stmt_reward_store; DEALLOCATE PREPARE stmt_reward_store;

SET @add_lottery_prize_store = (SELECT IF(COUNT(*) = 0,
  'ALTER TABLE biz_lottery_prize ADD COLUMN redeem_store_id bigint DEFAULT NULL AFTER image_url',
  'SELECT 1') FROM information_schema.columns WHERE table_schema = DATABASE()
  AND table_name = 'biz_lottery_prize' AND column_name = 'redeem_store_id');
PREPARE stmt_lottery_prize_store FROM @add_lottery_prize_store; EXECUTE stmt_lottery_prize_store; DEALLOCATE PREPARE stmt_lottery_prize_store;

SET @add_exchange_item_store = (SELECT IF(COUNT(*) = 0,
  'ALTER TABLE biz_exchange_item ADD COLUMN redeem_store_id bigint DEFAULT NULL AFTER image_url',
  'SELECT 1') FROM information_schema.columns WHERE table_schema = DATABASE()
  AND table_name = 'biz_exchange_item' AND column_name = 'redeem_store_id');
PREPARE stmt_exchange_item_store FROM @add_exchange_item_store; EXECUTE stmt_exchange_item_store; DEALLOCATE PREPARE stmt_exchange_item_store;

SET @add_lottery_qr_url = (SELECT IF(COUNT(*) = 0,
  'ALTER TABLE biz_lottery_code ADD COLUMN qr_url varchar(512) NOT NULL DEFAULT '''' AFTER status',
  'SELECT 1') FROM information_schema.columns WHERE table_schema = DATABASE()
  AND table_name = 'biz_lottery_code' AND column_name = 'qr_url');
PREPARE stmt_lottery_qr_url FROM @add_lottery_qr_url; EXECUTE stmt_lottery_qr_url; DEALLOCATE PREPARE stmt_lottery_qr_url;
SET @add_lottery_mini_path = (SELECT IF(COUNT(*) = 0,
  'ALTER TABLE biz_lottery_code ADD COLUMN mini_path varchar(255) NOT NULL DEFAULT '''' AFTER qr_url',
  'SELECT 1') FROM information_schema.columns WHERE table_schema = DATABASE()
  AND table_name = 'biz_lottery_code' AND column_name = 'mini_path');
PREPARE stmt_lottery_mini_path FROM @add_lottery_mini_path; EXECUTE stmt_lottery_mini_path; DEALLOCATE PREPARE stmt_lottery_mini_path;

INSERT IGNORE INTO biz_lottery_activity(activity_code, activity_name, start_time, end_time, daily_limit, total_limit, status, rule_json)
VALUES ('JS-DEMO-ACTIVITY', '金色酒业扫码有礼', NOW(), DATE_ADD(NOW(), INTERVAL 1 YEAR), 1, -1, '0', JSON_OBJECT('codeReusable', false));
INSERT IGNORE INTO biz_lottery_prize(activity_id, prize_type, prize_name, probability_bp, amount_min_cent, amount_max_cent, points, coupon_template_id, stock, image_url, status)
SELECT activity_id, 'cash', '现金红包', 500, 500, 2000, 0, NULL, -1, '', '0' FROM biz_lottery_activity a WHERE activity_code = 'JS-DEMO-ACTIVITY'
  AND NOT EXISTS (SELECT 1 FROM biz_lottery_prize p WHERE p.activity_id = a.activity_id AND p.prize_name = '现金红包');
INSERT IGNORE INTO biz_lottery_prize(activity_id, prize_type, prize_name, probability_bp, points, stock, image_url, status)
SELECT activity_id, 'points', '积分奖励', 5000, 50, -1, '', '0' FROM biz_lottery_activity a WHERE activity_code = 'JS-DEMO-ACTIVITY'
  AND NOT EXISTS (SELECT 1 FROM biz_lottery_prize p WHERE p.activity_id = a.activity_id AND p.prize_name = '积分奖励');
INSERT IGNORE INTO biz_lottery_prize(activity_id, prize_type, prize_name, probability_bp, coupon_template_id, stock, image_url, status)
SELECT a.activity_id, 'coupon', '20元代金券', 2500, c.coupon_template_id, -1, '', '0' FROM biz_lottery_activity a JOIN biz_coupon_template c ON c.template_code = 'voucher20' WHERE a.activity_code = 'JS-DEMO-ACTIVITY'
  AND NOT EXISTS (SELECT 1 FROM biz_lottery_prize p WHERE p.activity_id = a.activity_id AND p.prize_name = '20元代金券');
INSERT IGNORE INTO biz_lottery_prize(activity_id, prize_type, prize_name, probability_bp, stock, image_url, status)
SELECT activity_id, 'goods', '品鉴杯', 2000, -1, '/mini-assets/design/cup.jpg', '0' FROM biz_lottery_activity a WHERE activity_code = 'JS-DEMO-ACTIVITY'
  AND NOT EXISTS (SELECT 1 FROM biz_lottery_prize p WHERE p.activity_id = a.activity_id AND p.prize_name = '品鉴杯');
INSERT IGNORE INTO biz_lottery_code(activity_id, draw_code, status)
SELECT activity_id, 'JS-DEMO-QR-0001', 'unused' FROM biz_lottery_activity WHERE activity_code = 'JS-DEMO-ACTIVITY';

-- 微信 JSAPI 预支付请求幂等记录。与业务扩展服务的支付下单接口配套。
CREATE TABLE IF NOT EXISTS biz_order_pay_request (
  pay_request_id bigint NOT NULL AUTO_INCREMENT,
  member_id bigint NOT NULL,
  order_no varchar(64) NOT NULL,
  request_id varchar(64) NOT NULL,
  response_json text NOT NULL,
  create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (pay_request_id),
  UNIQUE KEY uk_biz_order_pay_request (member_id, request_id),
  KEY idx_biz_order_pay_order (member_id, order_no)
) ENGINE=InnoDB COMMENT='微信支付预下单幂等记录';

CREATE TABLE IF NOT EXISTS biz_order_pay_reconcile (
  order_no varchar(64) NOT NULL,
  member_id bigint NOT NULL,
  last_trade_state varchar(24) NOT NULL DEFAULT '',
  attempt_count int NOT NULL DEFAULT 0,
  last_query_at datetime DEFAULT NULL,
  next_query_at datetime DEFAULT NULL,
  last_error varchar(255) NOT NULL DEFAULT '',
  update_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (order_no),
  KEY idx_biz_order_pay_reconcile_due (next_query_at)
) ENGINE=InnoDB COMMENT='微信支付回调丢失补查台账';

-- Additive migration: historical codes have no outcome row and retain legacy draw rules.
CREATE TABLE IF NOT EXISTS biz_lottery_batch (
  batch_id varchar(64) NOT NULL,
  activity_id bigint NOT NULL,
  code_count int NOT NULL,
  win_count int NOT NULL DEFAULT 0,
  code_prefix varchar(2) NOT NULL,
  create_by varchar(64) NOT NULL DEFAULT '',
  create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (batch_id), KEY idx_lottery_batch_activity (activity_id, create_time)
) ENGINE=InnoDB COMMENT='生成时固定结果的抽奖批次';

CREATE TABLE IF NOT EXISTS biz_lottery_code_outcome (
  code_id bigint NOT NULL,
  batch_id varchar(64) NOT NULL,
  prize_id bigint DEFAULT NULL,
  coupon_template_id bigint DEFAULT NULL,
  redeem_store_id bigint DEFAULT NULL,
  result_type varchar(16) NOT NULL,
  result_snapshot json NOT NULL,
  prize_reserved tinyint NOT NULL DEFAULT 0,
  coupon_reserved tinyint NOT NULL DEFAULT 0,
  expires_at datetime DEFAULT NULL,
  status varchar(16) NOT NULL DEFAULT 'reserved' COMMENT 'reserved/claimed/expired',
  PRIMARY KEY (code_id), KEY idx_lottery_outcome_batch (batch_id),
  KEY idx_lottery_outcome_expiry (status, expires_at),
  KEY idx_lottery_outcome_prize (prize_id),
  KEY idx_lottery_outcome_coupon (coupon_template_id),
  KEY idx_lottery_outcome_store (redeem_store_id)
) ENGINE=InnoDB COMMENT='不可重抽的奖项快照及库存预留';

-- Run against jinse_wine before starting the new backend. Existing products are preserved.
SET NAMES utf8mb4;
CREATE TABLE IF NOT EXISTS biz_product_category (
  category_id bigint NOT NULL AUTO_INCREMENT,
  category_code varchar(64) NOT NULL,
  category_name varchar(64) NOT NULL,
  sort_num int NOT NULL DEFAULT 0,
  status char(1) NOT NULL DEFAULT '0',
  remark varchar(500) DEFAULT NULL,
  create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY(category_id),
  UNIQUE KEY uk_product_category_code(category_code),
  UNIQUE KEY uk_product_category_name(category_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='商品分类';

SET @category_sql = (SELECT IF(COUNT(*)=0, 'ALTER TABLE biz_product ADD COLUMN category_id bigint DEFAULT NULL AFTER category, ADD INDEX idx_product_category(category_id)', 'SELECT 1') FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='biz_product' AND column_name='category_id');
PREPARE category_stmt FROM @category_sql;
EXECUTE category_stmt;
DEALLOCATE PREPARE category_stmt;
SET @product_shipping_sql = (SELECT IF(COUNT(*)=0,
  'ALTER TABLE biz_product ADD COLUMN shipping_template_id bigint DEFAULT NULL AFTER product_id',
  'SELECT 1') FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='biz_product' AND column_name='shipping_template_id');
PREPARE product_shipping_stmt FROM @product_shipping_sql;
EXECUTE product_shipping_stmt;
DEALLOCATE PREPARE product_shipping_stmt;
SET @product_shipping_index_sql = (SELECT IF(COUNT(*)=0,
  'ALTER TABLE biz_product ADD INDEX idx_product_shipping_template(shipping_template_id)',
  'SELECT 1') FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name='biz_product' AND index_name='idx_product_shipping_template');
PREPARE product_shipping_index_stmt FROM @product_shipping_index_sql;
EXECUTE product_shipping_index_stmt;
DEALLOCATE PREPARE product_shipping_index_stmt;
INSERT INTO biz_product_category(category_code,category_name)
SELECT CONCAT('CAT_',REPLACE(UUID(),'-','')), legacy.name FROM
  (SELECT DISTINCT COALESCE(NULLIF(TRIM(category),''),'未分类') AS name FROM biz_product WHERE category_id IS NULL) legacy
WHERE NOT EXISTS(SELECT 1 FROM biz_product_category c WHERE c.category_name=legacy.name COLLATE utf8mb4_unicode_ci);
UPDATE biz_product p JOIN biz_product_category c ON c.category_name=COALESCE(NULLIF(TRIM(p.category),''),'未分类') COLLATE utf8mb4_unicode_ci
SET p.category_id=c.category_id,p.category=c.category_name WHERE p.category_id IS NULL;

CREATE TABLE IF NOT EXISTS biz_order_aftersale (
  aftersale_id bigint NOT NULL AUTO_INCREMENT,
  aftersale_no varchar(32) NOT NULL,
  order_id bigint NOT NULL,
  member_id bigint NOT NULL,
  order_item_id bigint NOT NULL,
  request_id varchar(64) NOT NULL,
  aftersale_type varchar(24) NOT NULL,
  quantity int NOT NULL,
  amount_cent int NOT NULL DEFAULT 0,
  status varchar(24) NOT NULL DEFAULT 'pending',
  reason varchar(200) NOT NULL,
  review_note varchar(500) DEFAULT NULL,
  return_address varchar(500) DEFAULT NULL,
  return_company varchar(64) DEFAULT NULL,
  return_tracking_no varchar(128) DEFAULT NULL,
  restock_quantity int NOT NULL DEFAULT 0,
  receipt_reference varchar(128) NOT NULL DEFAULT '',
  received_by varchar(64) DEFAULT NULL,
  received_at datetime DEFAULT NULL,
  replacement_company varchar(64) DEFAULT NULL,
  replacement_tracking_no varchar(128) DEFAULT NULL,
  replacement_shipped_at datetime DEFAULT NULL,
  refund_id bigint DEFAULT NULL,
  completed_at datetime DEFAULT NULL,
  create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (aftersale_id),
  UNIQUE KEY uk_aftersale_no (aftersale_no),
  UNIQUE KEY uk_aftersale_request (member_id,request_id),
  KEY idx_aftersale_order (order_id,status),
  KEY idx_aftersale_refund (refund_id)
) ENGINE=InnoDB COMMENT='商城退货换货';
CREATE TABLE IF NOT EXISTS biz_order_aftersale_event (
  event_id bigint NOT NULL AUTO_INCREMENT,
  aftersale_id bigint NOT NULL,
  status varchar(24) NOT NULL,
  actor varchar(64) NOT NULL,
  note varchar(500) NOT NULL DEFAULT '',
  create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (event_id), KEY idx_aftersale_event (aftersale_id,event_id)
) ENGINE=InnoDB COMMENT='售后处理记录';

SET @aftersale_receipt_sql = (SELECT IF(COUNT(*)=0, 'ALTER TABLE biz_order_aftersale ADD COLUMN receipt_reference varchar(128) NOT NULL DEFAULT '''' AFTER restock_quantity', 'SELECT 1') FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='biz_order_aftersale' AND column_name='receipt_reference');
PREPARE aftersale_receipt_stmt FROM @aftersale_receipt_sql;
EXECUTE aftersale_receipt_stmt;
DEALLOCATE PREPARE aftersale_receipt_stmt;

-- Existing databases also need the logistics fields used by the current jobs.
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

-- Public member numbers must exist before the current authentication code starts.
SET @member_number_column = (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='biz_member' AND column_name='member_no');
SET @member_number_ddl = IF(@member_number_column=0, 'ALTER TABLE biz_member ADD COLUMN member_no varchar(32) DEFAULT NULL AFTER member_id', 'SELECT 1');
PREPARE member_number_statement FROM @member_number_ddl;
EXECUTE member_number_statement;
DEALLOCATE PREPARE member_number_statement;
SET @member_number_index = (SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name='biz_member' AND index_name='uk_biz_member_no');
SET @member_number_ddl = IF(@member_number_index=0, 'ALTER TABLE biz_member ADD UNIQUE KEY uk_biz_member_no (member_no)', 'SELECT 1');
PREPARE member_number_statement FROM @member_number_ddl;
EXECUTE member_number_statement;
DEALLOCATE PREPARE member_number_statement;
UPDATE biz_member SET member_no=CONCAT('HY',HEX(RANDOM_BYTES(10))),update_time=update_time WHERE member_no IS NULL OR member_no='';

CREATE TABLE IF NOT EXISTS biz_external_ticket (
  ticket_id bigint NOT NULL AUTO_INCREMENT,
  prize_id bigint NOT NULL,
  provider_name varchar(128) NOT NULL,
  external_no varchar(128) NOT NULL,
  qr_image mediumblob NOT NULL,
  qr_mime varchar(32) NOT NULL,
  qr_sha256 char(64) NOT NULL,
  valid_until datetime NOT NULL,
  status varchar(16) NOT NULL DEFAULT 'available',
  code_id bigint DEFAULT NULL,
  reward_id bigint DEFAULT NULL,
  create_by varchar(64) NOT NULL DEFAULT '',
  create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  issued_at datetime DEFAULT NULL,
  PRIMARY KEY (ticket_id),
  UNIQUE KEY uk_external_ticket_identity (provider_name, external_no),
  UNIQUE KEY uk_external_ticket_image (qr_sha256),
  UNIQUE KEY uk_external_ticket_code (code_id),
  UNIQUE KEY uk_external_ticket_reward (reward_id),
  KEY idx_external_ticket_available (prize_id, status, valid_until)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin COMMENT='Third-party ticket inventory; redemption is not synchronized';

