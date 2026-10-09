-- Repair business seed text that was imported with the wrong client charset.
-- Safe to run repeatedly: only the stable seed rows are updated.
USE jinse_wine;

SET NAMES utf8mb4;

UPDATE biz_product SET
  product_name = CASE product_code
    WHEN 'classic' THEN '典藏酱香'
    WHEN 'mellow' THEN '经典浓香'
    WHEN 'double' THEN '双瓶礼盒'
    WHEN 'gift' THEN '珍藏礼盒'
  END,
  category = CASE product_code
    WHEN 'classic' THEN '酱香'
    WHEN 'mellow' THEN '浓香'
    WHEN 'double' THEN '礼盒'
    WHEN 'gift' THEN '礼盒'
  END,
  spec = CASE product_code
    WHEN 'classic' THEN '53度 500mL'
    WHEN 'mellow' THEN '52度 500mL'
    WHEN 'double' THEN '53度 500mL × 2'
    WHEN 'gift' THEN '53度 500mL'
  END,
  series = CASE product_code
    WHEN 'classic' THEN '金色典藏系列'
    WHEN 'mellow' THEN '金色经典系列'
    WHEN 'double' THEN '金色礼赠系列'
    WHEN 'gift' THEN '金色礼赠系列'
  END,
  description = CASE product_code
    WHEN 'classic' THEN '53度酱香型 500mL'
    WHEN 'mellow' THEN '52度浓香型 500mL'
    WHEN 'double' THEN '53度酱香型 双瓶礼盒'
    WHEN 'gift' THEN '53度酱香型 珍藏礼盒'
  END
WHERE product_code IN ('classic', 'mellow', 'double', 'gift');

UPDATE biz_store SET
  store_name = CASE store_code
    WHEN 'guigang' THEN '金色酒业体验店'
    WHEN 'partner' THEN '金色酒业合作门店'
  END,
  city = '贵港',
  district = CASE store_code
    WHEN 'guigang' THEN '港北区'
    WHEN 'partner' THEN '港南区'
  END,
  address = CASE store_code
    WHEN 'guigang' THEN '贵港市港北区 示例门店地址'
    WHEN 'partner' THEN '贵港市港南区 示例门店地址'
  END
WHERE store_code IN ('guigang', 'partner');

UPDATE biz_coupon_template SET
  template_name = CASE template_code
    WHEN 'group95' THEN '社群专享券'
    WHEN 'full15' THEN '满减优惠券'
    WHEN 'festival90' THEN '中秋全场券'
    WHEN 'voucher20' THEN '20元代金券'
  END,
  channel = CASE template_code
    WHEN 'group95' THEN '社群专享'
    WHEN 'full15' THEN '活动领取'
    WHEN 'festival90' THEN '节日活动'
    WHEN 'voucher20' THEN '积分兑换'
  END,
  remark = CASE template_code
    WHEN 'group95' THEN '购买2件及以上可用'
    WHEN 'full15' THEN '满200元减15元'
    WHEN 'festival90' THEN '全场商品可用'
    WHEN 'voucher20' THEN '购物抵扣，不可提现'
  END
WHERE template_code IN ('group95', 'full15', 'festival90', 'voucher20');

UPDATE biz_exchange_item SET
  item_name = CASE exchange_code
    WHEN 'voucher20' THEN '20元代金券'
    WHEN 'cup' THEN '品鉴杯'
    WHEN 'cups' THEN '双杯礼盒'
  END,
  spec = CASE exchange_code
    WHEN 'voucher20' THEN ''
    WHEN 'cup' THEN '精美陶瓷品鉴杯'
    WHEN 'cups' THEN '金色酒业定制 · 双杯装'
  END
WHERE exchange_code IN ('voucher20', 'cup', 'cups');

INSERT INTO biz_brand_config (config_key, config_value) VALUES
  ('homeCity', '贵港'),
  ('homeHeroTitle', '金色典藏'),
  ('homeHeroSubtitle', '53度酱香 · 500mL'),
  ('homeCampaignTitle', '节日领券'),
  ('homeCampaignSubtitle', '会员专享优惠，数量有限'),
  ('homeQuickLinksJson', '[{"name":"扫码有礼","icon":"scan","route":"scan"},{"name":"兑换网点","icon":"shop","route":"stores"},{"name":"积分专区","icon":"coin","route":"points"},{"name":"VIP社群","icon":"users","route":"vip"}]')
ON DUPLICATE KEY UPDATE config_value = VALUES(config_value);

UPDATE biz_brand_config SET config_value = '视频号精选' WHERE config_key = 'homeVideoTitle';
UPDATE biz_brand_config SET config_value = '{"shop":"商城","stores":"兑换网点","points":"积分专区","vip":"VIP社群"}' WHERE config_key = 'navigationJson';

-- The demo activity was imported twice by an older seed script.  The second
-- batch used the wrong client charset and created duplicate prize rows.
-- Keep the original rows (which may already be referenced by draw records)
-- and remove only the unreferenced duplicate seed IDs.
DELETE FROM biz_lottery_prize
WHERE prize_id IN (5, 6, 7, 8)
  AND activity_id = (SELECT activity_id FROM biz_lottery_activity WHERE activity_code = 'JS-DEMO-ACTIVITY');
