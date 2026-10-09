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
