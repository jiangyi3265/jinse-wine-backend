CREATE TABLE IF NOT EXISTS biz_promotion_transfer (
  settlement_id bigint NOT NULL PRIMARY KEY,
  member_id bigint NOT NULL,
  out_bill_no varchar(32) NOT NULL,
  transfer_bill_no varchar(64) DEFAULT NULL,
  amount_cent int NOT NULL,
  openid varchar(128) NOT NULL,
  status varchar(32) NOT NULL DEFAULT 'SUBMITTING',
  package_info varchar(2048) DEFAULT NULL,
  failure_reason varchar(500) DEFAULT NULL,
  create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_promotion_transfer_bill (out_bill_no),
  KEY idx_promotion_transfer_member (member_id,status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin COMMENT='佣金微信转账，终态仅来源于微信验签回调或查单';
