-- 金色酒业营销域迁移；在 business.example.sql 和 business-transaction-migration.sql 后执行。
USE jinse_wine;
SET NAMES utf8mb4;

-- 历史业务库补齐奖品图片字段；动态 SQL 使迁移可重复执行。
SET @add_reward_image = (SELECT IF(COUNT(*) = 0,
  'ALTER TABLE biz_reward ADD COLUMN image_url varchar(512) NOT NULL DEFAULT '''' AFTER exchange_code',
  'SELECT 1') FROM information_schema.columns WHERE table_schema = DATABASE()
  AND table_name = 'biz_reward' AND column_name = 'image_url');
PREPARE stmt_reward_image FROM @add_reward_image;
EXECUTE stmt_reward_image;
DEALLOCATE PREPARE stmt_reward_image;

SET @add_reward_spec = (SELECT IF(COUNT(*) = 0,
  'ALTER TABLE biz_reward ADD COLUMN spec varchar(255) NOT NULL DEFAULT '''' AFTER reward_name',
  'SELECT 1') FROM information_schema.columns WHERE table_schema = DATABASE()
  AND table_name = 'biz_reward' AND column_name = 'spec');
PREPARE stmt_reward_spec FROM @add_reward_spec;
EXECUTE stmt_reward_spec;
DEALLOCATE PREPARE stmt_reward_spec;

-- 抽奖和积分兑换使用服务端幂等键，防止重复扫码或重复点击造成重复发奖。
CREATE TABLE IF NOT EXISTS biz_mini_idempotency (
  member_id bigint NOT NULL,
  request_id varchar(64) NOT NULL,
  action_type varchar(32) NOT NULL,
  payload_hash char(64) NOT NULL,
  reward_id bigint DEFAULT NULL,
  member_coupon_id bigint DEFAULT NULL,
  result_type varchar(16) DEFAULT NULL COMMENT 'draw结果类型',
  result_value varchar(64) DEFAULT NULL COMMENT 'draw结果展示值',
  create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (member_id, request_id),
  KEY idx_biz_mini_idempotency_action (member_id, action_type)
) ENGINE=InnoDB COMMENT='小程序营销幂等记录';

-- 已执行过旧版迁移时补齐抽奖结果字段；使用信息_schema 保证脚本可重复执行。
SET @add_result_type = (SELECT IF(COUNT(*) = 0,
  'ALTER TABLE biz_mini_idempotency ADD COLUMN result_type varchar(16) DEFAULT NULL',
  'SELECT 1') FROM information_schema.columns WHERE table_schema = DATABASE()
  AND table_name = 'biz_mini_idempotency' AND column_name = 'result_type');
PREPARE stmt_result_type FROM @add_result_type;
EXECUTE stmt_result_type;
DEALLOCATE PREPARE stmt_result_type;
SET @add_result_value = (SELECT IF(COUNT(*) = 0,
  'ALTER TABLE biz_mini_idempotency ADD COLUMN result_value varchar(64) DEFAULT NULL',
  'SELECT 1') FROM information_schema.columns WHERE table_schema = DATABASE()
  AND table_name = 'biz_mini_idempotency' AND column_name = 'result_value');
PREPARE stmt_result_value FROM @add_result_value;
EXECUTE stmt_result_value;
DEALLOCATE PREPARE stmt_result_value;

-- 推广关系与佣金记录由订单确认收货、退款冲回和结算任务共同维护；费率与结算周期由运营中心配置。
CREATE TABLE IF NOT EXISTS biz_promotion_relation (
  relation_id bigint NOT NULL AUTO_INCREMENT,
  member_id bigint NOT NULL,
  inviter_member_id bigint DEFAULT NULL,
  invite_code varchar(64) NOT NULL,
  status char(1) NOT NULL DEFAULT '0' COMMENT '0有效 1停用',
  create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (relation_id),
  UNIQUE KEY uk_biz_promotion_member (member_id),
  UNIQUE KEY uk_biz_promotion_code (invite_code),
  KEY idx_biz_promotion_inviter (inviter_member_id)
) ENGINE=InnoDB COMMENT='会员推广关系';

CREATE TABLE IF NOT EXISTS biz_promotion_record (
  promotion_record_id bigint NOT NULL AUTO_INCREMENT,
  member_id bigint NOT NULL,
  order_id bigint DEFAULT NULL,
  title varchar(128) NOT NULL,
  amount_cent int NOT NULL DEFAULT 0,
  status varchar(16) NOT NULL DEFAULT 'pending' COMMENT 'pending/settled/processing/paid/closed',
  original_amount_cent int DEFAULT NULL,
  refund_adjustment_cent int NOT NULL DEFAULT 0,
  refund_hold_status varchar(16) DEFAULT NULL,
  refund_recovery_required tinyint(1) NOT NULL DEFAULT 0,
  create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (promotion_record_id),
  KEY idx_biz_promotion_record_member (member_id, create_time)
) ENGINE=InnoDB COMMENT='推广佣金记录';
