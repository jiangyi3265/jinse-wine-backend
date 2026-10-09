-- Existing databases: add the audited physical-return ledger before deploying the new backend.
USE jinse_wine;
SET NAMES utf8mb4;

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
