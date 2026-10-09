-- Existing installations: add persistent reconciliation for lost WeChat payment notifications.
USE jinse_wine;
SET NAMES utf8mb4;

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
