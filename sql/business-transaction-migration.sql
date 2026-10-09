-- 已执行 business.example.sql 的数据库，再执行本迁移一次。
-- 会话只保存令牌 SHA-256 摘要，数据库泄露时不会暴露可直接使用的令牌。
CREATE DATABASE IF NOT EXISTS jinse_wine DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE jinse_wine;
SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS biz_member_session (
  token_hash char(64) NOT NULL,
  member_id bigint NOT NULL,
  expires_at datetime NOT NULL,
  create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (token_hash),
  KEY idx_biz_member_session_member (member_id),
  KEY idx_biz_member_session_expiry (expires_at)
) ENGINE=InnoDB COMMENT='小程序会员会话';

-- 先占用请求号，再进行领券；并发重复请求只返回同一张券。
CREATE TABLE IF NOT EXISTS biz_coupon_claim_request (
  member_id bigint NOT NULL,
  request_id varchar(64) NOT NULL,
  template_code varchar(64) NOT NULL,
  member_coupon_id bigint DEFAULT NULL,
  create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (member_id, request_id)
) ENGINE=InnoDB COMMENT='领券幂等记录';

-- 同一个请求号的订单请求串行化，并拒绝换参数复用请求号。
CREATE TABLE IF NOT EXISTS biz_order_idempotency (
  member_id bigint NOT NULL,
  request_id varchar(64) NOT NULL,
  payload_hash char(64) NOT NULL,
  order_id bigint DEFAULT NULL,
  create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (member_id, request_id)
) ENGINE=InnoDB COMMENT='订单幂等记录';
