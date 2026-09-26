-- 小程序公开内容配置迁移，可重复执行。
-- 只保存可公开展示的页面内容，不在此表保存支付密钥、微信 secret 等敏感配置。
CREATE DATABASE IF NOT EXISTS jinse_wine DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE jinse_wine;

-- 品牌页扩展字段与管理端白名单保持一致；INSERT IGNORE 不覆盖运营已保存内容。
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

CREATE TABLE IF NOT EXISTS biz_mini_content (
  content_id bigint NOT NULL AUTO_INCREMENT COMMENT '内容ID',
  content_key varchar(128) NOT NULL COMMENT '稳定内容键，例如 home.hero',
  content_type varchar(64) NOT NULL COMMENT '内容类型，例如 banner/rule/navigation/image',
  title varchar(128) NOT NULL DEFAULT '' COMMENT '标题',
  subtitle varchar(255) NOT NULL DEFAULT '' COMMENT '副标题',
  content_json json NOT NULL COMMENT '结构化内容，数组或对象',
  image_url varchar(512) NOT NULL DEFAULT '' COMMENT '展示图片或视频封面地址',
  link_route varchar(128) NOT NULL DEFAULT '' COMMENT '小程序内部路由或业务链接',
  sort_num int NOT NULL DEFAULT 0 COMMENT '排序号',
  status char(1) NOT NULL DEFAULT '0' COMMENT '状态（0启用 1停用）',
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
  ('home.hero', 'banner', '金色酒业', '品质酱香，礼遇知己', '{"alt":"首页主视觉"}', '/mini-assets/design/hero-product.jpg', 'shop', 10, '0', 'seed', 'seed', '首页主视觉，可替换为 CDN 图片'),
  ('home.hero.2', 'banner', '窖藏时光', '匠心酿造，典藏好酒', '{"alt":"首页轮播图二"}', '/mini-assets/design/detail-scene.jpg', 'shop', 20, '0', 'seed', 'seed', '首页轮播图，可在后台替换图片'),
  ('home.hero.3', 'banner', '举杯共饮', '一杯好酒，礼遇知己', '{"alt":"首页轮播图三"}', '/mini-assets/design/landscape.jpg', 'shop', 30, '0', 'seed', 'seed', '首页轮播图，可在后台替换图片'),
  ('home.detail', 'image', '品牌详情', '', '{"alt":"品牌详情横幅"}', '/mini-assets/design/detail-landscape.jpg', '', 20, '0', 'seed', 'seed', '首页和商品详情页横幅'),
  ('home.navigation', 'navigation', '', '', '[{"name":"扫码有礼","icon":"scan","route":"scan"},{"name":"兑换网点","icon":"shop","route":"stores"},{"name":"积分专区","icon":"coin","route":"points"},{"name":"VIP社群","icon":"users","route":"vip"}]', '', '', 10, '0', 'seed', 'seed', '首页快捷导航，icon 使用前端图标名'),
  ('rules.shipping', 'rule', '配送说明', '', '[{"title":"配送范围","text":"具体配送范围和运费以结算页服务端报价为准。"},{"title":"收货验货","text":"请在签收时核对商品数量和包装。"}]', '', '', 10, '0', 'seed', 'seed', '规则页面内容'),
  ('rules.campaign', 'rule', '活动规则', '', '[{"title":"活动期限","text":"以后台配置的活动期限为准。"},{"title":"参与资格","text":"每个会员的领取和使用资格由服务端校验。"}]', '', '', 20, '0', 'seed', 'seed', '规则页面内容'),
  ('rules.points', 'rule', '积分规则', '', '[{"title":"积分获取","text":"积分到账和有效期以服务端记录为准。"},{"title":"积分兑换","text":"兑换成功后积分和库存由服务端原子扣减。"}]', '', '', 30, '0', 'seed', 'seed', '规则页面内容'),
  ('rules.ranking', 'rule', '排行榜规则', '', '[{"title":"统计周期","text":"排行榜周期和奖励以后台活动配置为准。"}]', '', '', 40, '0', 'seed', 'seed', '规则页面内容'),
  ('rules.promotion', 'rule', '推广规则', '', '[{"title":"推广关系","text":"推广关系由服务端建立并确认。"}]', '', '', 50, '0', 'seed', 'seed', '规则页面内容'),
  ('rules.draw', 'rule', '抽奖规则', '', '[{"title":"活动码","text":"活动码、次数、概率和奖池库存由服务端校验。"}]', '', '', 60, '0', 'seed', 'seed', '规则页面内容'),
  ('rules.coupon', 'rule', '优惠券规则', '', '[{"title":"使用规则","text":"优惠券门槛、适用商品和有效期以服务端报价为准。"}]', '', '', 70, '0', 'seed', 'seed', '规则页面内容'),
  ('brand.avatar', 'image', '默认头像', '', '{"alt":"默认会员头像"}', '/mini-assets/design/avatar.jpg', '', 10, '0', 'seed', 'seed', '默认头像图片'),
  ('brand.store', 'image', '门店图片', '', '{"alt":"门店展示图"}', '/mini-assets/design/store.jpg', '', 20, '0', 'seed', 'seed', '门店默认图片'),
  ('brand.map', 'image', '门店地图', '', '{"alt":"门店地图"}', '/mini-assets/design/map.jpg', '', 30, '0', 'seed', 'seed', '门店地图图片');

-- 设计稿中的页面图片也纳入内容中心，后台可直接替换而无需改包。
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

-- 将历史种子中的图片地址统一迁移到后端静态资源目录。
UPDATE biz_product SET image_url = REPLACE(image_url, '/static/design/', '/mini-assets/design/');
UPDATE biz_store SET image_url = REPLACE(image_url, '/static/design/', '/mini-assets/design/');
UPDATE biz_store SET service_qr = REPLACE(service_qr, '/static/design/', '/mini-assets/design/'),
                     group_qr = REPLACE(group_qr, '/static/design/', '/mini-assets/design/');
UPDATE biz_exchange_item SET image_url = REPLACE(image_url, '/static/design/', '/mini-assets/design/');
