-- Original ticket images are private entitlements, not public product media.
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
