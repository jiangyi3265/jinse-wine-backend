-- 金色酒业业务域初始化脚本。
-- 可在空 MySQL 8 数据库执行；不会删除已有业务表或覆盖已有业务数据。
CREATE DATABASE IF NOT EXISTS jinse_wine DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE jinse_wine;
SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS biz_product (
  product_id bigint NOT NULL AUTO_INCREMENT COMMENT '商品ID',
  product_code varchar(64) NOT NULL COMMENT '商品编码',
  product_name varchar(128) NOT NULL COMMENT '商品名称',
  category varchar(64) NOT NULL DEFAULT '' COMMENT '分类',
  spec varchar(128) NOT NULL DEFAULT '' COMMENT '规格',
  series varchar(128) NOT NULL DEFAULT '' COMMENT '系列',
  description text COMMENT '商品描述',
  image_url varchar(512) NOT NULL DEFAULT '' COMMENT '主图地址',
  price_cent int NOT NULL COMMENT '售价，单位分',
  original_price_cent int NOT NULL DEFAULT 0 COMMENT '划线价，单位分',
  stock int NOT NULL DEFAULT 0 COMMENT '可售库存',
  sales int NOT NULL DEFAULT 0 COMMENT '销量基数',
  sort_num int NOT NULL DEFAULT 0 COMMENT '排序',
  status char(1) NOT NULL DEFAULT '0' COMMENT '状态（0上架 1下架）',
  create_by varchar(64) NOT NULL DEFAULT '',
  create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_by varchar(64) NOT NULL DEFAULT '',
  update_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  remark varchar(500) DEFAULT NULL,
  PRIMARY KEY (product_id),
  UNIQUE KEY uk_biz_product_code (product_code),
  KEY idx_biz_product_status_sort (status, sort_num)
) ENGINE=InnoDB COMMENT='酒类商品';

CREATE TABLE IF NOT EXISTS biz_store (
  store_id bigint NOT NULL AUTO_INCREMENT COMMENT '门店ID',
  store_code varchar(64) NOT NULL COMMENT '门店编码',
  store_name varchar(128) NOT NULL COMMENT '门店名称',
  province varchar(64) NOT NULL DEFAULT '',
  city varchar(64) NOT NULL DEFAULT '',
  district varchar(64) NOT NULL DEFAULT '',
  address varchar(255) NOT NULL DEFAULT '',
  phone varchar(32) NOT NULL DEFAULT '',
  latitude decimal(10,7) DEFAULT NULL,
  longitude decimal(10,7) DEFAULT NULL,
  open_hours varchar(128) NOT NULL DEFAULT '',
  image_url varchar(512) NOT NULL DEFAULT '',
  service_qr varchar(512) NOT NULL DEFAULT '',
  group_qr varchar(512) NOT NULL DEFAULT '',
  dept_id bigint DEFAULT NULL COMMENT '后台数据权限部门ID（核销员角色）',
  sort_num int NOT NULL DEFAULT 0,
  status char(1) NOT NULL DEFAULT '0' COMMENT '状态（0营业 1停用）',
  create_by varchar(64) NOT NULL DEFAULT '',
  create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_by varchar(64) NOT NULL DEFAULT '',
  update_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  remark varchar(500) DEFAULT NULL,
  PRIMARY KEY (store_id),
  UNIQUE KEY uk_biz_store_code (store_code),
  KEY idx_biz_store_dept (dept_id),
  KEY idx_biz_store_status_sort (status, sort_num)
) ENGINE=InnoDB COMMENT='兑换门店';

CREATE TABLE IF NOT EXISTS biz_coupon_template (
  coupon_template_id bigint NOT NULL AUTO_INCREMENT COMMENT '券模板ID',
  template_code varchar(64) NOT NULL COMMENT '模板编码',
  template_name varchar(128) NOT NULL COMMENT '券名称',
  coupon_type varchar(16) NOT NULL COMMENT 'fixed/percent',
  amount_cent int NOT NULL DEFAULT 0 COMMENT '固定优惠，单位分',
  discount_rate int NOT NULL DEFAULT 100 COMMENT '折扣百分比，如95为9.5折',
  min_amount_cent int NOT NULL DEFAULT 0 COMMENT '最低消费，单位分',
  min_quantity int NOT NULL DEFAULT 1 COMMENT '最低商品件数',
  max_discount_cent int NOT NULL DEFAULT 0 COMMENT '最大优惠，0表示不限制',
  member_only tinyint(1) NOT NULL DEFAULT 0 COMMENT '仅会员可领',
  points_only tinyint(1) NOT NULL DEFAULT 0 COMMENT '仅积分兑换',
  channel varchar(64) NOT NULL DEFAULT '' COMMENT '发放渠道',
  stock int NOT NULL DEFAULT -1 COMMENT '库存，-1表示不限量',
  claim_limit int NOT NULL DEFAULT 1 COMMENT '每人领取上限',
  start_time datetime DEFAULT NULL,
  end_time datetime DEFAULT NULL,
  sort_num int NOT NULL DEFAULT 0,
  status char(1) NOT NULL DEFAULT '0' COMMENT '状态（0启用 1停用）',
  create_by varchar(64) NOT NULL DEFAULT '',
  create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_by varchar(64) NOT NULL DEFAULT '',
  update_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  remark varchar(500) DEFAULT NULL,
  PRIMARY KEY (coupon_template_id),
  UNIQUE KEY uk_biz_coupon_template_code (template_code),
  KEY idx_biz_coupon_template_status_time (status, start_time, end_time)
) ENGINE=InnoDB COMMENT='优惠券模板';

CREATE TABLE IF NOT EXISTS biz_exchange_item (
  exchange_item_id bigint NOT NULL AUTO_INCREMENT COMMENT '兑换项ID',
  exchange_code varchar(64) NOT NULL COMMENT '兑换编码',
  item_name varchar(128) NOT NULL COMMENT '兑换名称',
  item_type varchar(16) NOT NULL COMMENT 'coupon/goods',
  coupon_template_id bigint DEFAULT NULL COMMENT '券模板ID',
  spec varchar(128) NOT NULL DEFAULT '',
  image_url varchar(512) NOT NULL DEFAULT '',
  redeem_store_id bigint DEFAULT NULL COMMENT '指定兑换门店，NULL表示全部合作门店',
  points int NOT NULL COMMENT '所需积分',
  stock int NOT NULL DEFAULT 0,
  per_user_limit int NOT NULL DEFAULT 1,
  sort_num int NOT NULL DEFAULT 0,
  status char(1) NOT NULL DEFAULT '0' COMMENT '状态（0上架 1下架）',
  create_by varchar(64) NOT NULL DEFAULT '',
  create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_by varchar(64) NOT NULL DEFAULT '',
  update_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  remark varchar(500) DEFAULT NULL,
  PRIMARY KEY (exchange_item_id),
  UNIQUE KEY uk_biz_exchange_item_code (exchange_code),
  KEY idx_biz_exchange_item_status_sort (status, sort_num)
) ENGINE=InnoDB COMMENT='积分兑换目录';

CREATE TABLE IF NOT EXISTS biz_member (
  member_id bigint NOT NULL AUTO_INCREMENT COMMENT '会员ID',
  member_no varchar(32) DEFAULT NULL COMMENT '独立会员编号',
  wechat_openid varchar(128) DEFAULT NULL COMMENT '微信OpenID',
  union_id varchar(128) DEFAULT NULL COMMENT '微信UnionID',
  nickname varchar(128) NOT NULL DEFAULT '',
  avatar varchar(512) NOT NULL DEFAULT '',
  phone varchar(32) NOT NULL DEFAULT '',
  vip_level int NOT NULL DEFAULT 0,
  staff_store_id bigint DEFAULT NULL COMMENT '核销员所属门店',
  status char(1) NOT NULL DEFAULT '0' COMMENT '状态（0正常 1停用）',
  create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (member_id),
  UNIQUE KEY uk_biz_member_openid (wechat_openid),
  UNIQUE KEY uk_biz_member_no (member_no),
  KEY idx_biz_member_phone (phone)
) ENGINE=InnoDB COMMENT='小程序会员';

CREATE TABLE IF NOT EXISTS biz_member_address (
  address_id bigint NOT NULL AUTO_INCREMENT,
  member_id bigint NOT NULL,
  receiver_name varchar(64) NOT NULL,
  receiver_phone varchar(32) NOT NULL,
  province varchar(64) NOT NULL DEFAULT '',
  city varchar(64) NOT NULL DEFAULT '',
  district varchar(64) NOT NULL DEFAULT '',
  detail varchar(255) NOT NULL DEFAULT '',
  is_default tinyint(1) NOT NULL DEFAULT 0,
  create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (address_id),
  KEY idx_biz_member_address_member (member_id, is_default)
) ENGINE=InnoDB COMMENT='会员收货地址';

CREATE TABLE IF NOT EXISTS biz_member_coupon (
  member_coupon_id bigint NOT NULL AUTO_INCREMENT,
  member_id bigint NOT NULL,
  coupon_template_id bigint NOT NULL,
  coupon_code varchar(64) NOT NULL,
  status varchar(16) NOT NULL DEFAULT 'unused' COMMENT 'unused/locked/used/expired',
  rule_snapshot json DEFAULT NULL COMMENT '领券时规则快照',
  order_id bigint DEFAULT NULL,
  expires_at datetime DEFAULT NULL,
  create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (member_coupon_id),
  UNIQUE KEY uk_biz_member_coupon_code (coupon_code),
  KEY idx_biz_member_coupon_member_status (member_id, status)
) ENGINE=InnoDB COMMENT='会员优惠券';

CREATE TABLE IF NOT EXISTS biz_order (
  order_id bigint NOT NULL AUTO_INCREMENT,
  order_no varchar(64) NOT NULL,
  member_id bigint NOT NULL,
  address_snapshot json NOT NULL COMMENT '地址快照',
  subtotal_cent int NOT NULL,
  discount_cent int NOT NULL DEFAULT 0,
  shipping_cent int NOT NULL DEFAULT 0,
  total_cent int NOT NULL,
  coupon_id bigint DEFAULT NULL,
  request_id varchar(64) NOT NULL COMMENT '服务端幂等键',
  cancel_request_id varchar(64) DEFAULT NULL COMMENT '取消动作幂等键',
  receive_request_id varchar(64) DEFAULT NULL COMMENT '确认收货动作幂等键',
  status varchar(16) NOT NULL DEFAULT 'pending' COMMENT 'pending/paid/shipping/receiving/complete/cancelled/refunded',
  paid_at datetime DEFAULT NULL,
  wechat_transaction_id varchar(64) DEFAULT NULL,
  cancelled_at datetime DEFAULT NULL,
  create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (order_id),
  UNIQUE KEY uk_biz_order_no (order_no),
  UNIQUE KEY uk_biz_order_request (member_id, request_id),
  KEY idx_biz_order_member_status (member_id, status),
  UNIQUE KEY uk_biz_order_wechat_transaction (wechat_transaction_id)
) ENGINE=InnoDB COMMENT='商城订单';

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

CREATE TABLE IF NOT EXISTS biz_order_item (
  order_item_id bigint NOT NULL AUTO_INCREMENT,
  order_id bigint NOT NULL,
  product_id bigint NOT NULL,
  product_name varchar(128) NOT NULL,
  product_spec varchar(128) NOT NULL DEFAULT '',
  image_url varchar(512) NOT NULL DEFAULT '',
  unit_price_cent int NOT NULL,
  quantity int NOT NULL,
  total_cent int NOT NULL,
  PRIMARY KEY (order_item_id),
  KEY idx_biz_order_item_order (order_id)
) ENGINE=InnoDB COMMENT='订单商品快照';

CREATE TABLE IF NOT EXISTS biz_point_account (
  member_id bigint NOT NULL,
  balance int NOT NULL DEFAULT 0,
  update_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (member_id)
) ENGINE=InnoDB COMMENT='会员积分账户';

CREATE TABLE IF NOT EXISTS biz_point_record (
  point_record_id bigint NOT NULL AUTO_INCREMENT,
  member_id bigint NOT NULL,
  change_amount int NOT NULL,
  balance_after int NOT NULL,
  source_type varchar(32) NOT NULL,
  source_id varchar(64) NOT NULL DEFAULT '',
  title varchar(128) NOT NULL,
  expires_at datetime DEFAULT NULL,
  create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (point_record_id),
  KEY idx_biz_point_record_member_time (member_id, create_time)
) ENGINE=InnoDB COMMENT='积分流水';

CREATE TABLE IF NOT EXISTS biz_reward (
  reward_id bigint NOT NULL AUTO_INCREMENT,
  member_id bigint NOT NULL,
  reward_type varchar(16) NOT NULL COMMENT 'cash/points/coupon/goods',
  reward_name varchar(128) NOT NULL,
  spec varchar(255) NOT NULL DEFAULT '' COMMENT '奖品规格',
  amount_cent int NOT NULL DEFAULT 0,
  points int NOT NULL DEFAULT 0,
  exchange_code varchar(128) NOT NULL DEFAULT '',
  image_url varchar(512) NOT NULL DEFAULT '' COMMENT '奖品图片地址',
  status varchar(16) NOT NULL DEFAULT 'pending' COMMENT 'pending/processing/received/redeemed/failed/closed',
  expires_at datetime DEFAULT NULL,
  create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (reward_id),
  UNIQUE KEY uk_biz_reward_exchange_code (exchange_code),
  KEY idx_biz_reward_member_status (member_id, status)
) ENGINE=InnoDB COMMENT='奖品与红包权益';

CREATE TABLE IF NOT EXISTS biz_redemption (
  redemption_id bigint NOT NULL AUTO_INCREMENT,
  reward_id bigint NOT NULL,
  member_id bigint NOT NULL,
  store_id bigint NOT NULL,
  verifier_member_id bigint NOT NULL,
  status varchar(16) NOT NULL DEFAULT 'confirmed',
  request_id varchar(64) NOT NULL,
  create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (redemption_id),
  UNIQUE KEY uk_biz_redemption_reward (reward_id),
  UNIQUE KEY uk_biz_redemption_request (verifier_member_id, request_id),
  KEY idx_biz_redemption_store_time (store_id, create_time)
) ENGINE=InnoDB COMMENT='门店核销记录';

CREATE TABLE IF NOT EXISTS biz_brand_config (
  config_key varchar(64) NOT NULL,
  config_value varchar(1024) NOT NULL DEFAULT '',
  update_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (config_key)
) ENGINE=InnoDB COMMENT='小程序品牌配置';

INSERT IGNORE INTO biz_product (product_code, product_name, category, spec, series, description, image_url, price_cent, original_price_cent, stock, sales, sort_num, status, create_by, update_by, remark) VALUES
('classic', '典藏酱香', '酱香', '53度 500mL', '金色典藏系列', '53度酱香型 500mL', '/mini-assets/design/product-classic.jpg', 39800, 49800, 99, 1286, 10, '0', 'seed', 'seed', '前端演示初始数据'),
('mellow', '经典浓香', '浓香', '52度 500mL', '金色经典系列', '52度浓香型 500mL', '/mini-assets/design/product-mellow.jpg', 26800, 32800, 86, 965, 20, '0', 'seed', 'seed', '前端演示初始数据'),
('double', '双瓶礼盒', '礼盒', '53度 500mL × 2', '金色礼赠系列', '53度酱香型 双瓶礼盒', '/mini-assets/design/product-double.jpg', 59800, 69800, 50, 658, 30, '0', 'seed', 'seed', '前端演示初始数据'),
('gift', '珍藏礼盒', '礼盒', '53度 500mL', '金色礼赠系列', '53度酱香型 珍藏礼盒', '/mini-assets/design/product-gift.jpg', 89800, 99800, 30, 328, 40, '0', 'seed', 'seed', '前端演示初始数据');

INSERT IGNORE INTO biz_store (store_code, store_name, city, district, address, phone, latitude, longitude, open_hours, image_url, sort_num, status, create_by, update_by, remark) VALUES
('guigang', '金色酒业体验店', '贵港', '港北区', '贵港市港北区金港大道 888 号金色酒业体验店', '13800002688', 23.0965000, 109.6086000, '09:00 - 21:00', '/mini-assets/design/store.jpg', 10, '0', 'seed', 'seed', '本地演示数据，正式上线前请替换为实际门店资料'),
('partner', '金色酒业合作门店', '贵港', '港南区', '贵港市港南区江南大道 188 号金色酒业合作门店', '13800002689', 23.0668000, 109.5945000, '09:00 - 21:00', '/mini-assets/design/store.jpg', 20, '0', 'seed', 'seed', '本地演示数据，正式上线前请替换为实际门店资料');

INSERT IGNORE INTO biz_coupon_template (template_code, template_name, coupon_type, amount_cent, discount_rate, min_amount_cent, min_quantity, member_only, points_only, channel, stock, claim_limit, sort_num, status, create_by, update_by, remark) VALUES
('group95', '社群专享券', 'percent', 0, 95, 0, 2, 1, 0, '社群专享', -1, 1, 10, '0', 'seed', 'seed', '购买2件及以上可用'),
('full15', '满减优惠券', 'fixed', 1500, 100, 20000, 1, 0, 0, '活动领取', -1, 1, 20, '0', 'seed', 'seed', '满200元减15元'),
('festival90', '中秋全场券', 'percent', 0, 90, 0, 1, 0, 0, '节日活动', -1, 1, 30, '0', 'seed', 'seed', '全场商品可用'),
('voucher20', '20元代金券', 'fixed', 2000, 100, 0, 1, 0, 1, '积分兑换', -1, 1, 40, '0', 'seed', 'seed', '购物抵扣，不可提现');

INSERT IGNORE INTO biz_exchange_item (exchange_code, item_name, item_type, coupon_template_id, spec, image_url, points, stock, per_user_limit, sort_num, status, create_by, update_by) VALUES
('voucher20', '20元代金券', 'coupon', (SELECT coupon_template_id FROM biz_coupon_template WHERE template_code = 'voucher20'), '', '/mini-assets/design/voucher.jpg', 200, 99, 1, 10, '0', 'seed', 'seed'),
('cup', '品鉴杯', 'goods', NULL, '精美陶瓷品鉴杯', '/mini-assets/design/cup.jpg', 300, 30, 1, 20, '0', 'seed', 'seed'),
('cups', '双杯礼盒', 'goods', NULL, '金色酒业定制 · 双杯装', '/mini-assets/design/cups.jpg', 800, 20, 1, 30, '0', 'seed', 'seed');

INSERT IGNORE INTO biz_brand_config (config_key, config_value) VALUES
('finderUserName', ''), ('officialAccountQr', ''), ('groupQr', ''), ('customerServicePhone', '');

INSERT IGNORE INTO biz_brand_config (config_key, config_value) VALUES
('homeCity', '贵港'),
('homeHeroImage', '/mini-assets/design/hero-product.jpg'),
('homeHeroTitle', '金色典藏'),
('homeHeroSubtitle', '53度酱香 · 500mL'),
('homeCampaignTitle', '节日领券'),
('homeCampaignSubtitle', '会员专享优惠，数量有限'),
('homeCampaignImage', '/mini-assets/design/hero-product.jpg'),
('homeVideoTitle', '视频号精选'),
('homeVideoImage', '/mini-assets/design/detail-landscape.jpg'),
('homeFeaturedProductCode', 'classic'),
('homeQuickLinksJson', '[{"name":"扫码有礼","icon":"scan","route":"scan"},{"name":"兑换网点","icon":"shop","route":"stores"},{"name":"积分专区","icon":"coin","route":"points"},{"name":"VIP社群","icon":"users","route":"vip"}]'),
('rankingJson', '[]'),
('navigationJson', '{"shop":"商城","stores":"兑换网点","points":"积分专区","vip":"VIP社群"}'),
('rulesJson', '{}'),
('pageContentJson', '{}');

-- 小程序公开页面内容，后台可按稳定 key 配置标题、结构化规则、图片和跳转路由。
CREATE TABLE IF NOT EXISTS biz_mini_content (
  content_id bigint NOT NULL AUTO_INCREMENT COMMENT '内容ID',
  content_key varchar(128) NOT NULL COMMENT '稳定内容键',
  content_type varchar(64) NOT NULL COMMENT '内容类型',
  title varchar(128) NOT NULL DEFAULT '',
  subtitle varchar(255) NOT NULL DEFAULT '',
  content_json json NOT NULL,
  image_url varchar(512) NOT NULL DEFAULT '',
  link_route varchar(128) NOT NULL DEFAULT '',
  sort_num int NOT NULL DEFAULT 0,
  status char(1) NOT NULL DEFAULT '0',
  create_by varchar(64) NOT NULL DEFAULT '',
  create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_by varchar(64) NOT NULL DEFAULT '',
  update_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  remark varchar(500) DEFAULT NULL,
  PRIMARY KEY (content_id),
  UNIQUE KEY uk_biz_mini_content_key (content_key),
  KEY idx_biz_mini_content_type_status_sort (content_type, status, sort_num)
) ENGINE=InnoDB COMMENT='小程序公开页面内容';

INSERT IGNORE INTO biz_mini_content
  (content_key, content_type, title, subtitle, content_json, image_url, link_route, sort_num, status, create_by, update_by, remark)
VALUES
  ('home.hero', 'banner', '金色酒业', '品质酱香，礼遇知己', '{"alt":"首页主视觉"}', '/mini-assets/design/hero-product.jpg', 'shop', 10, '0', 'seed', 'seed', '首页主视觉'),
  ('home.detail', 'image', '品牌详情', '', '{"alt":"品牌详情横幅"}', '/mini-assets/design/detail-landscape.jpg', '', 20, '0', 'seed', 'seed', '详情横幅'),
  ('home.navigation', 'navigation', '', '', '[{"name":"扫码有礼","icon":"scan","route":"scan"},{"name":"兑换网点","icon":"shop","route":"stores"},{"name":"积分专区","icon":"coin","route":"points"},{"name":"VIP社群","icon":"users","route":"vip"}]', '', '', 10, '0', 'seed', 'seed', '首页快捷导航'),
  ('rules.shipping', 'rule', '配送说明', '', '[{"title":"配送范围","text":"具体配送范围和运费以结算页服务端报价为准。"}]', '', '', 10, '0', 'seed', 'seed', '规则页面内容'),
  ('rules.campaign', 'rule', '活动规则', '', '[{"title":"活动期限","text":"以后台配置的活动期限为准。"}]', '', '', 20, '0', 'seed', 'seed', '规则页面内容'),
  ('rules.points', 'rule', '积分规则', '', '[{"title":"积分获取","text":"积分到账和有效期以服务端记录为准。"}]', '', '', 30, '0', 'seed', 'seed', '规则页面内容'),
  ('rules.ranking', 'rule', '排行榜规则', '', '[{"title":"统计周期","text":"排行榜周期和奖励以后台活动配置为准。"}]', '', '', 40, '0', 'seed', 'seed', '规则页面内容'),
  ('rules.promotion', 'rule', '推广规则', '', '[{"title":"推广关系","text":"推广关系由服务端建立并确认。"}]', '', '', 50, '0', 'seed', 'seed', '规则页面内容'),
  ('rules.draw', 'rule', '抽奖规则', '', '[{"title":"活动码","text":"活动码、次数、概率和奖池库存由服务端校验。"}]', '', '', 60, '0', 'seed', 'seed', '规则页面内容'),
  ('rules.coupon', 'rule', '优惠券规则', '', '[{"title":"使用规则","text":"优惠券门槛、适用商品和有效期以服务端报价为准。"}]', '', '', 70, '0', 'seed', 'seed', '规则页面内容'),
  ('brand.avatar', 'image', '默认头像', '', '{"alt":"默认会员头像"}', '/mini-assets/design/avatar.jpg', '', 10, '0', 'seed', 'seed', '默认头像图片'),
  ('brand.store', 'image', '门店图片', '', '{"alt":"门店展示图"}', '/mini-assets/design/store.jpg', '', 20, '0', 'seed', 'seed', '门店默认图片'),
  ('brand.map', 'image', '门店地图', '', '{"alt":"门店地图"}', '/mini-assets/design/map.jpg', '', 30, '0', 'seed', 'seed', '门店地图图片');

INSERT IGNORE INTO biz_mini_content
  (content_key, content_type, title, subtitle, content_json, image_url, link_route, sort_num, status, create_by, update_by, remark)
VALUES
  ('brand.points', 'image', '积分页主图', '', '{"alt":"积分页主图"}', '/mini-assets/design/detail-scene-clean.jpg', '', 40, '0', 'seed', 'seed', '积分页图片'),
  ('brand.scan', 'image', '扫码页主图', '', '{"alt":"扫码页主图"}', '/mini-assets/design/detail-scene-clean.jpg', '', 50, '0', 'seed', 'seed', '扫码页图片'),
  ('brand.ranking', 'image', '排行榜主图', '', '{"alt":"排行榜主图"}', '/mini-assets/design/landscape.jpg', '', 60, '0', 'seed', 'seed', '排行榜图片'),
  ('brand.cup', 'image', '品鉴杯图片', '', '{"alt":"品鉴杯"}', '/mini-assets/design/cup.jpg', '', 70, '0', 'seed', 'seed', '扫码页奖品图片'),
  ('brand.productDetail', 'image', '商品详情图', '', '{"alt":"商品详情图"}', '/mini-assets/design/detail-landscape.jpg', '', 80, '0', 'seed', 'seed', '商品详情兜底图片'),
  ('brand.productStory', 'image', '商品故事图', '', '{"alt":"商品故事图"}', '/mini-assets/design/detail-scene-clean.jpg', '', 90, '0', 'seed', 'seed', '商品故事图片'),
  ('brand.poster', 'image', '推广海报主图', '', '{"alt":"推广海报主图"}', '/mini-assets/design/detail-scene-clean.jpg', '', 100, '0', 'seed', 'seed', '推广海报兜底图片'),
  ('brand.qr', 'image', '二维码兜底图', '', '{"alt":"二维码"}', '/mini-assets/design/qr-demo.png', '', 110, '0', 'seed', 'seed', '推广二维码兜底图');
