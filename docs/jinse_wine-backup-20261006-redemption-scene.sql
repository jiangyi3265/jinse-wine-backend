-- MySQL dump 10.13  Distrib 8.0.46, for Win64 (x86_64)
--
-- Host: 127.0.0.1    Database: jinse_wine
-- ------------------------------------------------------
-- Server version	8.0.46

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `biz_brand_config`
--

DROP TABLE IF EXISTS `biz_brand_config`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `biz_brand_config` (
  `config_key` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL,
  `config_value` varchar(1024) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`config_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='灏忕▼搴忓搧鐗岄厤缃';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `biz_brand_config`
--

LOCK TABLES `biz_brand_config` WRITE;
/*!40000 ALTER TABLE `biz_brand_config` DISABLE KEYS */;
INSERT INTO `biz_brand_config` VALUES ('customerServicePhone','','2026-10-02 23:45:52'),('finderUserName','','2026-10-02 23:45:52'),('groupQr','','2026-10-02 23:45:52'),('homeCampaignImage','/mini-assets/design/hero-product.jpg','2026-10-02 23:45:52'),('homeCampaignSubtitle','会员专享优惠，数量有限','2026-10-03 19:09:09'),('homeCampaignTitle','节日领券','2026-10-03 19:09:09'),('homeCity','贵港','2026-10-03 19:09:09'),('homeFeaturedProductCode','classic','2026-10-02 23:45:52'),('homeHeroImage','/mini-assets/design/hero-product.jpg','2026-10-02 23:45:52'),('homeHeroSubtitle','53度酱香 · 500mL','2026-10-03 19:09:09'),('homeHeroTitle','金色典藏','2026-10-03 19:09:09'),('homeQuickLinksJson','[{\"name\":\"扫码有礼\",\"icon\":\"scan\",\"route\":\"scan\"},{\"name\":\"兑换网点\",\"icon\":\"shop\",\"route\":\"stores\"},{\"name\":\"积分专区\",\"icon\":\"coin\",\"route\":\"points\"},{\"name\":\"VIP社群\",\"icon\":\"users\",\"route\":\"vip\"}]','2026-10-03 19:09:09'),('homeVideoImage','/mini-assets/design/detail-landscape.jpg','2026-10-02 23:45:52'),('homeVideoTitle','视频号精选','2026-10-03 19:09:56'),('navigationJson','{\"shop\":\"商城\",\"stores\":\"兑换网点\",\"points\":\"积分专区\",\"vip\":\"VIP社群\"}','2026-10-03 19:09:56'),('officialAccountQr','','2026-10-02 23:45:52'),('pageContentJson','{}','2026-10-02 23:45:52'),('rankingJson','[]','2026-10-02 23:45:52'),('rulesJson','{}','2026-10-02 23:45:52');
/*!40000 ALTER TABLE `biz_brand_config` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `biz_cart_item`
--

DROP TABLE IF EXISTS `biz_cart_item`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `biz_cart_item` (
  `cart_item_id` bigint NOT NULL AUTO_INCREMENT,
  `member_id` bigint NOT NULL,
  `product_id` bigint NOT NULL,
  `sku_code` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '',
  `quantity` int NOT NULL DEFAULT '1',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`cart_item_id`),
  UNIQUE KEY `uk_biz_cart_member_sku` (`member_id`,`product_id`,`sku_code`)
) ENGINE=InnoDB AUTO_INCREMENT=13 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='购物车';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `biz_cart_item`
--

LOCK TABLES `biz_cart_item` WRITE;
/*!40000 ALTER TABLE `biz_cart_item` DISABLE KEYS */;
/*!40000 ALTER TABLE `biz_cart_item` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `biz_channel_config`
--

DROP TABLE IF EXISTS `biz_channel_config`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `biz_channel_config` (
  `config_key` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL,
  `config_value` text COLLATE utf8mb4_unicode_ci NOT NULL,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`config_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='微信支付、视频号及群二维码配置';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `biz_channel_config`
--

LOCK TABLES `biz_channel_config` WRITE;
/*!40000 ALTER TABLE `biz_channel_config` DISABLE KEYS */;
INSERT INTO `biz_channel_config` VALUES ('wechat.merchant.appId','','2026-10-02 23:51:19'),('wechat.pay.enabled','false','2026-10-02 23:51:19'),('wechat.transfer.enabled','false','2026-10-02 23:51:19'),('wechat.video.finderUserName','','2026-10-02 23:51:19'),('wechat.video.shopId','','2026-10-02 23:51:19'),('wechat.video.syncEnabled','false','2026-10-02 23:51:19'),('wechat.work.groupQr','','2026-10-04 15:50:41'),('wechat.work.groupQrExpiresAt','','2026-10-04 15:50:41');
/*!40000 ALTER TABLE `biz_channel_config` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `biz_coupon_claim_request`
--

DROP TABLE IF EXISTS `biz_coupon_claim_request`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `biz_coupon_claim_request` (
  `member_id` bigint NOT NULL,
  `request_id` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL,
  `template_code` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL,
  `member_coupon_id` bigint DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`member_id`,`request_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='领券幂等记录';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `biz_coupon_claim_request`
--

LOCK TABLES `biz_coupon_claim_request` WRITE;
/*!40000 ALTER TABLE `biz_coupon_claim_request` DISABLE KEYS */;
INSERT INTO `biz_coupon_claim_request` VALUES (1,'claim-1791037218707-7dxk3','full15',1,'2026-10-03 22:20:18');
/*!40000 ALTER TABLE `biz_coupon_claim_request` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `biz_coupon_template`
--

DROP TABLE IF EXISTS `biz_coupon_template`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `biz_coupon_template` (
  `coupon_template_id` bigint NOT NULL AUTO_INCREMENT COMMENT '鍒告ā鏉縄D',
  `template_code` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '妯℃澘缂栫爜',
  `template_name` varchar(128) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '鍒稿悕绉',
  `coupon_type` varchar(16) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'fixed/percent',
  `amount_cent` int NOT NULL DEFAULT '0' COMMENT '鍥哄畾浼樻儬锛屽崟浣嶅垎',
  `discount_rate` int NOT NULL DEFAULT '100' COMMENT '鎶樻墸鐧惧垎姣旓紝濡?5涓?.5鎶',
  `min_amount_cent` int NOT NULL DEFAULT '0' COMMENT '鏈?綆娑堣垂锛屽崟浣嶅垎',
  `min_quantity` int NOT NULL DEFAULT '1' COMMENT '鏈?綆鍟嗗搧浠舵暟',
  `max_discount_cent` int NOT NULL DEFAULT '0' COMMENT '鏈?ぇ浼樻儬锛?琛ㄧず涓嶉檺鍒',
  `member_only` tinyint(1) NOT NULL DEFAULT '0' COMMENT '浠呬細鍛樺彲棰',
  `points_only` tinyint(1) NOT NULL DEFAULT '0' COMMENT '浠呯Н鍒嗗厬鎹',
  `channel` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '' COMMENT '鍙戞斁娓犻亾',
  `stock` int NOT NULL DEFAULT '-1' COMMENT '搴撳瓨锛?1琛ㄧず涓嶉檺閲',
  `claim_limit` int NOT NULL DEFAULT '1' COMMENT '姣忎汉棰嗗彇涓婇檺',
  `start_time` datetime DEFAULT NULL,
  `end_time` datetime DEFAULT NULL,
  `sort_num` int NOT NULL DEFAULT '0',
  `status` char(1) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '0' COMMENT '鐘舵?锛?鍚?敤 1鍋滅敤锛',
  `create_by` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_by` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `remark` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`coupon_template_id`),
  UNIQUE KEY `uk_biz_coupon_template_code` (`template_code`),
  KEY `idx_biz_coupon_template_status_time` (`status`,`start_time`,`end_time`)
) ENGINE=InnoDB AUTO_INCREMENT=18 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='浼樻儬鍒告ā鏉';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `biz_coupon_template`
--

LOCK TABLES `biz_coupon_template` WRITE;
/*!40000 ALTER TABLE `biz_coupon_template` DISABLE KEYS */;
INSERT INTO `biz_coupon_template` VALUES (1,'group95','社群专享券','percent',0,95,0,2,0,1,0,'社群专享',-1,1,NULL,NULL,10,'0','seed','2026-10-02 23:45:52','seed','2026-10-03 19:09:09','购买2件及以上可用'),(2,'full15','满减优惠券','fixed',1500,100,20000,1,0,0,0,'活动领取',-1,1,NULL,NULL,20,'0','seed','2026-10-02 23:45:52','seed','2026-10-03 19:09:09','满200元减15元'),(3,'festival90','中秋全场券','percent',0,90,0,1,0,0,0,'节日活动',-1,1,NULL,NULL,30,'0','seed','2026-10-02 23:45:52','seed','2026-10-03 19:09:09','全场商品可用'),(4,'voucher20','20元代金券','fixed',2000,100,0,1,0,0,1,'积分兑换',-1,1,NULL,NULL,40,'0','seed','2026-10-02 23:45:52','seed','2026-10-03 19:09:09','购物抵扣，不可提现');
/*!40000 ALTER TABLE `biz_coupon_template` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `biz_exchange_item`
--

DROP TABLE IF EXISTS `biz_exchange_item`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `biz_exchange_item` (
  `exchange_item_id` bigint NOT NULL AUTO_INCREMENT COMMENT '鍏戞崲椤笽D',
  `exchange_code` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '鍏戞崲缂栫爜',
  `item_name` varchar(128) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '鍏戞崲鍚嶇О',
  `item_type` varchar(16) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'coupon/goods',
  `coupon_template_id` bigint DEFAULT NULL COMMENT '鍒告ā鏉縄D',
  `spec` varchar(128) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '',
  `image_url` varchar(512) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '',
  `redeem_store_id` bigint DEFAULT NULL,
  `points` int NOT NULL COMMENT '鎵?渶绉?垎',
  `stock` int NOT NULL DEFAULT '0',
  `per_user_limit` int NOT NULL DEFAULT '1',
  `sort_num` int NOT NULL DEFAULT '0',
  `status` char(1) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '0' COMMENT '鐘舵?锛?涓婃灦 1涓嬫灦锛',
  `create_by` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_by` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `remark` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`exchange_item_id`),
  UNIQUE KEY `uk_biz_exchange_item_code` (`exchange_code`),
  KEY `idx_biz_exchange_item_status_sort` (`status`,`sort_num`)
) ENGINE=InnoDB AUTO_INCREMENT=13 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='绉?垎鍏戞崲鐩?綍';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `biz_exchange_item`
--

LOCK TABLES `biz_exchange_item` WRITE;
/*!40000 ALTER TABLE `biz_exchange_item` DISABLE KEYS */;
INSERT INTO `biz_exchange_item` VALUES (1,'voucher20','20元代金券','coupon',4,'','/mini-assets/design/voucher.jpg',NULL,200,99,1,10,'0','seed','2026-10-02 23:45:52','seed','2026-10-03 19:09:09',NULL),(2,'cup','品鉴杯','goods',NULL,'精美陶瓷品鉴杯','/mini-assets/design/cup.jpg',NULL,300,30,1,20,'0','seed','2026-10-02 23:45:52','seed','2026-10-03 19:09:09',NULL),(3,'cups','双杯礼盒','goods',NULL,'金色酒业定制 · 双杯装','/mini-assets/design/cups.jpg',NULL,800,20,1,30,'0','seed','2026-10-02 23:45:52','seed','2026-10-03 19:09:09',NULL);
/*!40000 ALTER TABLE `biz_exchange_item` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `biz_lottery_activity`
--

DROP TABLE IF EXISTS `biz_lottery_activity`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `biz_lottery_activity` (
  `activity_id` bigint NOT NULL AUTO_INCREMENT,
  `activity_code` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL,
  `activity_name` varchar(128) COLLATE utf8mb4_unicode_ci NOT NULL,
  `start_time` datetime DEFAULT NULL,
  `end_time` datetime DEFAULT NULL,
  `daily_limit` int NOT NULL DEFAULT '1',
  `total_limit` int NOT NULL DEFAULT '-1',
  `used_count` int NOT NULL DEFAULT '0',
  `status` char(1) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '0',
  `rule_json` json DEFAULT NULL,
  `create_by` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`activity_id`),
  UNIQUE KEY `uk_biz_lottery_activity_code` (`activity_code`),
  KEY `idx_biz_lottery_activity_status` (`status`,`start_time`,`end_time`)
) ENGINE=InnoDB AUTO_INCREMENT=23 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='扫码抽奖活动';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `biz_lottery_activity`
--

LOCK TABLES `biz_lottery_activity` WRITE;
/*!40000 ALTER TABLE `biz_lottery_activity` DISABLE KEYS */;
INSERT INTO `biz_lottery_activity` VALUES (1,'JS-DEMO-ACTIVITY','金色酒业扫码有礼','2026-10-02 23:51:19','2027-10-02 23:51:19',1,-1,1,'0','{\"codeReusable\": false}','','2026-10-02 23:51:19','2026-10-03 22:23:08');
/*!40000 ALTER TABLE `biz_lottery_activity` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `biz_lottery_code`
--

DROP TABLE IF EXISTS `biz_lottery_code`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `biz_lottery_code` (
  `code_id` bigint NOT NULL AUTO_INCREMENT,
  `activity_id` bigint NOT NULL,
  `draw_code` varchar(128) COLLATE utf8mb4_unicode_ci NOT NULL,
  `status` varchar(16) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'unused' COMMENT 'unused/used/disabled',
  `qr_url` varchar(512) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '',
  `mini_path` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '',
  `member_id` bigint DEFAULT NULL,
  `used_at` datetime DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`code_id`),
  UNIQUE KEY `uk_biz_lottery_code` (`draw_code`),
  KEY `idx_biz_lottery_code_activity_status` (`activity_id`,`status`)
) ENGINE=InnoDB AUTO_INCREMENT=23 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='抽奖二维码活动码';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `biz_lottery_code`
--

LOCK TABLES `biz_lottery_code` WRITE;
/*!40000 ALTER TABLE `biz_lottery_code` DISABLE KEYS */;
INSERT INTO `biz_lottery_code` VALUES (1,1,'JS-DEMO-QR-0001','used','','',1,'2026-10-03 22:23:08','2026-10-02 23:51:19');
/*!40000 ALTER TABLE `biz_lottery_code` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `biz_lottery_prize`
--

DROP TABLE IF EXISTS `biz_lottery_prize`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `biz_lottery_prize` (
  `prize_id` bigint NOT NULL AUTO_INCREMENT,
  `activity_id` bigint NOT NULL,
  `prize_type` varchar(16) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'cash/goods/points/coupon/none',
  `prize_name` varchar(128) COLLATE utf8mb4_unicode_ci NOT NULL,
  `probability_bp` int NOT NULL DEFAULT '0' COMMENT '万分比，全部奖项合计不超过10000',
  `amount_min_cent` int NOT NULL DEFAULT '0',
  `amount_max_cent` int NOT NULL DEFAULT '0',
  `points` int NOT NULL DEFAULT '0',
  `coupon_template_id` bigint DEFAULT NULL,
  `stock` int NOT NULL DEFAULT '-1',
  `issued_count` int NOT NULL DEFAULT '0',
  `image_url` varchar(512) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '',
  `redeem_store_id` bigint DEFAULT NULL,
  `status` char(1) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '0',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`prize_id`),
  UNIQUE KEY `uk_biz_lottery_prize_name` (`activity_id`,`prize_name`),
  KEY `idx_biz_lottery_prize_activity` (`activity_id`,`status`)
) ENGINE=InnoDB AUTO_INCREMENT=34 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='抽奖奖项及概率库存';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `biz_lottery_prize`
--

LOCK TABLES `biz_lottery_prize` WRITE;
/*!40000 ALTER TABLE `biz_lottery_prize` DISABLE KEYS */;
INSERT INTO `biz_lottery_prize` VALUES (1,1,'cash','现金红包',500,500,2000,0,NULL,-1,0,'',NULL,'0','2026-10-02 23:51:19','2026-10-02 23:51:19'),(2,1,'points','积分奖励',5000,0,0,50,NULL,-1,0,'',NULL,'0','2026-10-02 23:51:19','2026-10-02 23:51:19'),(3,1,'coupon','20元代金券',2500,0,0,0,4,-1,0,'',NULL,'0','2026-10-02 23:51:19','2026-10-02 23:51:19'),(4,1,'goods','品鉴杯',2000,0,0,0,NULL,-1,0,'/mini-assets/design/cup.jpg',NULL,'0','2026-10-02 23:51:19','2026-10-02 23:51:19');
/*!40000 ALTER TABLE `biz_lottery_prize` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `biz_lottery_record`
--

DROP TABLE IF EXISTS `biz_lottery_record`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `biz_lottery_record` (
  `record_id` bigint NOT NULL AUTO_INCREMENT,
  `activity_id` bigint NOT NULL,
  `code_id` bigint NOT NULL,
  `member_id` bigint NOT NULL,
  `prize_id` bigint DEFAULT NULL,
  `result_type` varchar(16) COLLATE utf8mb4_unicode_ci NOT NULL,
  `result_value` varchar(128) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '',
  `request_id` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`record_id`),
  UNIQUE KEY `uk_biz_lottery_record_request` (`member_id`,`request_id`),
  KEY `idx_biz_lottery_record_member` (`member_id`,`create_time`),
  KEY `idx_biz_lottery_record_code` (`code_id`)
) ENGINE=InnoDB AUTO_INCREMENT=21 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='抽奖流水';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `biz_lottery_record`
--

LOCK TABLES `biz_lottery_record` WRITE;
/*!40000 ALTER TABLE `biz_lottery_record` DISABLE KEYS */;
INSERT INTO `biz_lottery_record` VALUES (1,1,1,1,1,'goods','品鉴杯','draw-local-0001','2026-10-03 22:23:08');
/*!40000 ALTER TABLE `biz_lottery_record` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `biz_member`
--

DROP TABLE IF EXISTS `biz_member`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `biz_member` (
  `member_id` bigint NOT NULL AUTO_INCREMENT COMMENT '浼氬憳ID',
  `wechat_openid` varchar(128) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '寰?俊OpenID',
  `union_id` varchar(128) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '寰?俊UnionID',
  `nickname` varchar(128) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '',
  `avatar` varchar(512) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '',
  `phone` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '',
  `vip_level` int NOT NULL DEFAULT '0',
  `staff_store_id` bigint DEFAULT NULL COMMENT '鏍搁攢鍛樻墍灞為棬搴',
  `status` char(1) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '0' COMMENT '鐘舵?锛?姝ｅ父 1鍋滅敤锛',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`member_id`),
  UNIQUE KEY `uk_biz_member_openid` (`wechat_openid`),
  KEY `idx_biz_member_phone` (`phone`)
) ENGINE=InnoDB AUTO_INCREMENT=402 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='灏忕▼搴忎細鍛';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `biz_member`
--

LOCK TABLES `biz_member` WRITE;
/*!40000 ALTER TABLE `biz_member` DISABLE KEYS */;
INSERT INTO `biz_member` VALUES (1,'local-h5-member',NULL,'微信用户','','',0,NULL,'0','2026-10-03 21:54:53','2026-10-04 11:20:47'),(79,'local-h5-staff',NULL,'演示核销员','','',0,1,'0','2026-10-04 11:20:47','2026-10-04 13:26:00');
/*!40000 ALTER TABLE `biz_member` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `biz_member_address`
--

DROP TABLE IF EXISTS `biz_member_address`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `biz_member_address` (
  `address_id` bigint NOT NULL AUTO_INCREMENT,
  `member_id` bigint NOT NULL,
  `receiver_name` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL,
  `receiver_phone` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL,
  `province` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '',
  `city` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '',
  `district` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '',
  `detail` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '',
  `is_default` tinyint(1) NOT NULL DEFAULT '0',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`address_id`),
  KEY `idx_biz_member_address_member` (`member_id`,`is_default`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='浼氬憳鏀惰揣鍦板潃';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `biz_member_address`
--

LOCK TABLES `biz_member_address` WRITE;
/*!40000 ALTER TABLE `biz_member_address` DISABLE KEYS */;
INSERT INTO `biz_member_address` VALUES (1,1,'张先生','13800002688','广西壮族自治区','贵港市','港北区','金港大道 888 号 2 栋 101 室',1,'2026-10-03 22:08:39','2026-10-05 02:45:24');
/*!40000 ALTER TABLE `biz_member_address` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `biz_member_coupon`
--

DROP TABLE IF EXISTS `biz_member_coupon`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `biz_member_coupon` (
  `member_coupon_id` bigint NOT NULL AUTO_INCREMENT,
  `member_id` bigint NOT NULL,
  `coupon_template_id` bigint NOT NULL,
  `coupon_code` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL,
  `status` varchar(16) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'unused' COMMENT 'unused/locked/used/expired',
  `rule_snapshot` json DEFAULT NULL COMMENT '棰嗗埜鏃惰?鍒欏揩鐓',
  `order_id` bigint DEFAULT NULL,
  `expires_at` datetime DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`member_coupon_id`),
  UNIQUE KEY `uk_biz_member_coupon_code` (`coupon_code`),
  KEY `idx_biz_member_coupon_member_status` (`member_id`,`status`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='浼氬憳浼樻儬鍒';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `biz_member_coupon`
--

LOCK TABLES `biz_member_coupon` WRITE;
/*!40000 ALTER TABLE `biz_member_coupon` DISABLE KEYS */;
INSERT INTO `biz_member_coupon` VALUES (1,1,2,'CPdbcdf0fd76e44879a29a782b0a4b9d80','unused','{\"stock\": -1, \"end_time\": null, \"start_time\": null, \"amount_cent\": 1500, \"claim_limit\": 1, \"coupon_type\": \"fixed\", \"member_only\": false, \"points_only\": false, \"min_quantity\": 1, \"discount_rate\": 100, \"template_code\": \"full15\", \"template_name\": \"满减优惠券\", \"min_amount_cent\": 20000, \"max_discount_cent\": 0, \"coupon_template_id\": 2}',NULL,'2026-11-02 22:20:19','2026-10-03 22:20:18','2026-10-04 23:48:47');
/*!40000 ALTER TABLE `biz_member_coupon` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `biz_member_session`
--

DROP TABLE IF EXISTS `biz_member_session`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `biz_member_session` (
  `token_hash` char(64) COLLATE utf8mb4_unicode_ci NOT NULL,
  `member_id` bigint NOT NULL,
  `expires_at` datetime NOT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`token_hash`),
  KEY `idx_biz_member_session_member` (`member_id`),
  KEY `idx_biz_member_session_expiry` (`expires_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='小程序会员会话';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `biz_member_session`
--

LOCK TABLES `biz_member_session` WRITE;
/*!40000 ALTER TABLE `biz_member_session` DISABLE KEYS */;
INSERT INTO `biz_member_session` VALUES ('013c85859ead568a309ef3497dd547a75fb212b6aeffe68f5a0e5d2aea206356',1,'2026-11-04 16:25:41','2026-10-05 16:25:41'),('0165c8f674476def86bbdaaef3775ccf7e811a3bc3dd12e45986757d1e535069',1,'2026-11-05 03:49:27','2026-10-06 03:49:26'),('01d0e80864bdc0929a687897ad99446c91fbb0f7b79ed96fef65c4d9acbbdcd9',1,'2026-11-04 07:29:47','2026-10-05 07:29:47'),('029cbef16d6166565fe4e75e5d3ff0b02affc9583d470f197e412af68b336e70',1,'2026-11-03 10:23:11','2026-10-04 10:23:10'),('02e4c8aa7652b4a94b00c997a3bc5fa9f913f37eaba83a2a9feb37305c0e623b',79,'2026-11-03 13:24:34','2026-10-04 13:24:34'),('02f6a5dc3335e8ac5b7cd35887ff0f03c42d6c55d4253fc9ba34d1beb3ab408a',1,'2026-11-04 08:28:18','2026-10-05 08:28:18'),('034195c969d084ed9801cb9ef3b73c24052249c0ddd58e29e7cdd7c021916fff',1,'2026-11-02 23:44:54','2026-10-03 23:44:54'),('0383d1c4d96660603e8b05fe8007c2be8942302ae91989a552c976c137fc29a5',1,'2026-11-04 23:35:17','2026-10-05 23:35:17'),('03b5bc04c64f18b7a847400761a2a6b12f654479b4e119ebae6b0af9383d9e2b',1,'2026-11-04 14:49:26','2026-10-05 14:49:25'),('05765c16715cb3bbc31031bbc60d7b9f27426d39bea173e74121384ff94f46e8',1,'2026-11-04 09:41:46','2026-10-05 09:41:46'),('0644a96a885b33f2d9b3ca76b1801e8b1864658eba6924c0dc0bc1197e2d1828',1,'2026-11-03 11:44:15','2026-10-04 11:44:15'),('0676d3e559b7e63f5f6968af7d422ae7d6aefebe1c69947c9a2bb8d98531169f',1,'2026-11-04 00:19:08','2026-10-05 00:19:07'),('069125e18a793943bef4e3e6f2acf767cc546820ea7bb510f9d6544399433e98',1,'2026-11-04 12:00:54','2026-10-05 12:00:53'),('087fad8173dbac3530245320083d6b00648db2a59a74ab60e3f29ca15d5dfa25',1,'2026-11-03 08:27:57','2026-10-04 08:27:57'),('0a03d6ce85f55e553b3bb6964d276895d89dd02e74d5dc231d896c4b691d6b8b',1,'2026-11-04 17:38:21','2026-10-05 17:38:21'),('0a344de5f36a67f348164e5c3dc34ea71206c3246c72694e27fc423725ed86d3',1,'2026-11-04 06:16:55','2026-10-05 06:16:55'),('0b40d8af192d2f0ef92d9cdb8bf250eb6374aeaf5079a72b256a4e4f22496cd1',1,'2026-11-05 05:17:00','2026-10-06 05:17:00'),('0b8a86a8ffd9bf7cb6b32dc0ce761358f63aacb469fdf21ee759f5b281083d2f',1,'2026-11-04 06:17:14','2026-10-05 06:17:13'),('0dd42fea1cff1cc87533b493d5e83c409c932021fd9d9c1208b10f8860993efd',1,'2026-11-05 00:25:28','2026-10-06 00:25:28'),('0ebee6215b0a7e84cea1fac640f50e5231ffa1732b80a79ff08b667c8160bc33',1,'2026-11-02 23:04:17','2026-10-03 23:04:16'),('0ece42880ab0c3f087256f5def2c71ea42068ee76143a8f2d02111d01b74e9d7',1,'2026-11-04 23:07:59','2026-10-05 23:07:59'),('0fb4b7fe92aed3033f21043923bc9d7baa25b9fa7214224003957d023a2c3066',1,'2026-11-04 09:46:30','2026-10-05 09:46:30'),('0fe4e8d70cc2ddadc09dbf4ab67173436017a9e1f970b95b887fc0f6589f774a',1,'2026-11-03 16:01:05','2026-10-04 16:01:04'),('11137726a673484d100f8a6124dfd99b8a4e5da60ab1ff3e868d287cb112db95',1,'2026-11-03 08:11:56','2026-10-04 08:11:55'),('11999fc06b512086b27c35a94987a556e5348fc5e80ed4cf6fa8a47a3f5d5c86',1,'2026-11-04 06:31:33','2026-10-05 06:31:32'),('120c4d5133b3838b0b672fe765f883c71d57a2305d1c96581829471fe83f8ed4',1,'2026-11-04 03:44:46','2026-10-05 03:44:45'),('136f0de849758f3d4397714b2bec6896272785aa46a20ac9cb9d9adf863b3fad',1,'2026-11-04 16:52:01','2026-10-05 16:52:01'),('1383ff1f3ddf2ac1e10b08c9de1d086cf29eaf0bcd9dd8167b4f03df8671d538',1,'2026-11-03 07:55:46','2026-10-04 07:55:46'),('157335a6fd6623aae1a3fad552cd120d2254a1095808f2efece58efc414da152',1,'2026-11-03 08:01:30','2026-10-04 08:01:29'),('15ae198e64c9449a419ea54c8165348f2fea5926c55aa8eaf225e6d46e2fd688',1,'2026-11-04 17:37:34','2026-10-05 17:37:33'),('174d553310fd19b74fad5fcb0a22f6876cd92569f933cace12117f41cc072397',1,'2026-11-04 00:05:09','2026-10-05 00:05:08'),('174fac96c2b12c1ea060b45b451a2dab77002c02448731b178dd7418f69d83fe',1,'2026-11-04 22:24:07','2026-10-05 22:24:06'),('17a7cc447fee86a4ec1dc4c7764f4c13a6516f8e9fa59e3c5be2acee7e3e5e26',1,'2026-11-03 08:34:06','2026-10-04 08:34:05'),('1a6f64fb4c7f6cb0ba87f8cc67edc67e32a5ab3534f75096c47af6ef067d9566',1,'2026-11-05 01:22:27','2026-10-06 01:22:26'),('1ba64f126af53e0ac9c523b878348fa543002856f62d489b1a343b4819b778bb',1,'2026-11-03 08:26:50','2026-10-04 08:26:50'),('1c18c7a93a93e47a5cb87817e8998247ecdef88e07e01f52d62a3462374f2af4',1,'2026-11-03 11:47:39','2026-10-04 11:47:39'),('1c7645c21624fed4dd64580a30ec8fc5a7abf694be4f39f42ee3ddc62c2eb4a7',1,'2026-11-03 20:57:57','2026-10-04 20:57:57'),('1ca1ff06d058b92bd64e3e32bc8c6a312f6a9464a332e185aec34477c5c8b4bb',79,'2026-11-03 15:35:00','2026-10-04 15:35:00'),('1cab4e2b0e06dc9aa6d46f27d75d8dde19ec1c7635f53cc5ceab1a2ea9981495',1,'2026-11-04 06:19:17','2026-10-05 06:19:16'),('1fde4f5485bf1e63fe00d11ff1ef5273dc5401bcb1e42f7fc266a36619f19171',1,'2026-11-04 22:35:15','2026-10-05 22:35:14'),('21642b452ca5c4ed65324b2f2b77373cc24f8cdd422bf5f6321b362675718339',1,'2026-11-03 16:00:56','2026-10-04 16:00:56'),('21866f8a45321a380c488822716cc5e17464d50f0eda0f6c0b2755616a596fe4',79,'2026-11-03 11:46:56','2026-10-04 11:46:55'),('219e32e1ed23e40282a8bdbbc49622ab1daaf31c06c33126d46128acd97f3cbe',1,'2026-11-03 13:01:36','2026-10-04 13:01:36'),('21b13f7ff1e785d153f0be4cd45640af8fdf2ade5e613fa5f7f3d4871c8d735d',1,'2026-11-04 08:55:03','2026-10-05 08:55:03'),('21e593fa7f99aa9766ddab139c35a5a04627cc42f401ce504129ab32ed27d269',1,'2026-11-02 22:23:08','2026-10-03 22:23:08'),('21ef1961660d79722bf656d8bde3d52f0a29438fb3135fd37963edf1e0b03e70',1,'2026-11-03 16:07:40','2026-10-04 16:07:39'),('21fa7dfcca9af8291a03e7cd0739367a7fc66ff05ea41b9745f5e6b92b0cdb07',1,'2026-11-03 07:50:19','2026-10-04 07:50:19'),('22071870e940e9f80e6d91401182ed2bd618e49b55eea208d601e00a6c4ce0c9',1,'2026-11-04 16:51:29','2026-10-05 16:51:28'),('23350a9c5c0fa77c27005d2b34bf5e2d5fee6729039625c3ede6ea74b57597b8',1,'2026-11-03 13:22:05','2026-10-04 13:22:05'),('23bb9f6119a2c81f83f9ed7531f45aff47ec1f5f4f0867ab8bc5b36b85bd0ba5',1,'2026-11-03 08:06:35','2026-10-04 08:06:34'),('23f162de257798e55492c228a7749e83c5641222fffff3fa488fc67316f09a6f',1,'2026-11-03 17:23:57','2026-10-04 17:23:56'),('242ce4d50f8b948ac9df5c06931ee405b23f65183e4164ac86096f32c6ee62e1',1,'2026-11-03 08:27:57','2026-10-04 08:27:57'),('244d1b2aa0b1c01cbe780ebe06727ea3fdba506893d577aa79d81bafa2e0d91b',1,'2026-11-04 05:02:24','2026-10-05 05:02:24'),('24f7403b1f9efef8b041f603db6a4140afb7ca11243cb70538818075afd34707',1,'2026-11-03 08:33:06','2026-10-04 08:33:06'),('251a850df388f9e56c3ec08e38a33be1e7afdf3755867e8a1b3c5f98c8bc64b2',1,'2026-11-04 11:30:18','2026-10-05 11:30:18'),('25aeb2cbf7905e0ad1dd39d1458af55524325f3e2fba9f27ba89e18e8e3fb926',1,'2026-11-03 16:00:45','2026-10-04 16:00:45'),('25ba10df01563f705912f3665bf8dd17d753c0728c473c295e70819e5c303f6c',1,'2026-11-05 03:02:50','2026-10-06 03:02:49'),('25cb3dea989d4c8c084d1ab2a748ebb86c7c2f1c1c676e7494a5a30f40464ece',79,'2026-11-03 11:36:31','2026-10-04 11:36:31'),('27e50d93ebc07e18fe7353ace81ba7f6fd6a338f3ff86c8027e675da55457cc2',1,'2026-11-04 17:41:15','2026-10-05 17:41:14'),('280b4a97c2fa81e78c78b39b8242b774f4b9de15b94bbcf8bb459d5ae903e9c3',1,'2026-11-04 01:51:42','2026-10-05 01:51:42'),('28508a7ec58993356f72adce23126861828e473d4169cbeafad9a5303aab8a03',1,'2026-11-03 19:36:52','2026-10-04 19:36:51'),('288eb36b46384fd414e3d5d77f31a7a5c41eab236c41d92e6299eba4c878ec63',1,'2026-11-04 17:10:03','2026-10-05 17:10:03'),('28a755810a0f8aa43b2b689132ea52f91bd4c7bf3432333cb7a5eae111bd3c13',1,'2026-11-03 08:41:18','2026-10-04 08:41:18'),('28aec34aaa9210037f5166c3f652e05ea9347552ea01b74469d82a109d1d3fa1',1,'2026-11-04 05:47:26','2026-10-05 05:47:26'),('2c44e9d100cbf4ea3728c113f74b018069bf498cb60967363550b6c53a85139a',79,'2026-11-03 11:23:07','2026-10-04 11:23:07'),('2c599fd90c4fd9a39ec457c5871523bcf5f95048b14a9eff6c012ceb20953c94',1,'2026-11-04 23:07:20','2026-10-05 23:07:19'),('2c744ec0d65a2deb899557b72934172c0f49ef093f0ac72ba52182a7989ee314',1,'2026-11-05 00:33:02','2026-10-06 00:33:02'),('2e17b538756bbeefb810bcab9edac725fb92ae666443080e37f7c267e0713bca',1,'2026-11-05 01:11:07','2026-10-06 01:11:06'),('2e7cb5f90a95fd60169ca2109a3f7cf56784d9bed87cc63d9b8dbe2c6cd6d8a2',1,'2026-11-05 03:00:19','2026-10-06 03:00:18'),('2eb8808fb7c94d96a72e5f9dc6f680b780bccd616bd6b1f21a52d156e4424256',1,'2026-11-02 22:02:18','2026-10-03 22:02:18'),('2edfae93327bf70b7cb9c2bd478db3eca1dcc7e79f4dff473a8e25905d45c51a',1,'2026-11-04 08:27:15','2026-10-05 08:27:15'),('2fa4b70799a8bd4a56fd1545f9083a03eb12d40c81e6a56cde936d960030440d',79,'2026-11-03 11:28:21','2026-10-04 11:28:20'),('2fbab2428e129e13bdbc0a6f9985b6dcf24105d9946fd09b2dccbb7604a9b74f',1,'2026-11-03 13:24:34','2026-10-04 13:24:34'),('322d3130913242643ca63b43acef81496de0856b775108edabadfa1f20e2f766',1,'2026-11-03 18:07:09','2026-10-04 18:07:09'),('324ec0e57bd189e31cd0be39f162e180a20ca8a996e04302bc500cd6cff07fa8',1,'2026-11-04 10:07:57','2026-10-05 10:07:56'),('328ccd61ddc277ac9be1e1bcfda2f2634c4e081dc8306886669189bebde49882',1,'2026-11-03 08:34:06','2026-10-04 08:34:05'),('355e83ba2fa228f964c6beeb8efa4276c2c18e1eb9c3c01b8bfb000c44ee8ddd',1,'2026-11-03 09:29:37','2026-10-04 09:29:37'),('368daa46934d0ea3fa74986dd39acc102f7b6ca82ba2099cbcdd287cde6a0b19',1,'2026-11-04 00:16:04','2026-10-05 00:16:04'),('3976c09fb56a5d6c22b5c3b11a025d1a9585549abb4e2c1dab46ff5f87868380',1,'2026-11-03 22:45:06','2026-10-04 22:45:05'),('397954d4f0e1b43c61d839823c636c636ca1b98bd5ca321be88714eaa10c9845',1,'2026-11-04 03:45:41','2026-10-05 03:45:41'),('3b460ab78d94ecc72a601760b3c97efdb20269cc0bbacf72129620f9cf160f2b',1,'2026-11-03 08:49:25','2026-10-04 08:49:25'),('3c0469cb95ba82832883d0c83ef5120ca6791d2739b8e7050a6c4335cf6e6463',1,'2026-11-04 14:18:12','2026-10-05 14:18:11'),('3c0ac5664826c701c44a3be442eba24febbd568c410bbf2d8f2af0df47e27a6c',1,'2026-11-04 21:42:56','2026-10-05 21:42:56'),('3c41834981cfe474eec3fc0c05668daa9f40b48d9ebc418990cc80c7344bb9f9',1,'2026-11-03 15:17:37','2026-10-04 15:17:37'),('3c813d009d7b8a5fc75c9808c724dc158e6fb377b4dc05d3dcfdec41607867d2',1,'2026-11-04 22:34:47','2026-10-05 22:34:47'),('3cdd9250d52470492cda29f84ff7a5f8b846ed7bac9ff08c04a2237464a17b87',1,'2026-11-03 10:21:24','2026-10-04 10:21:23'),('3e79ed629f5ffbc402705dab247cc75057103f24b7403523ca95d380b15fae4b',79,'2026-11-04 15:07:56','2026-10-05 15:07:55'),('3e99ddb72a81fbba36d03bbd2e6c66b367d37aef74602291f667a615545bbfe4',1,'2026-11-05 02:58:46','2026-10-06 02:58:46'),('40ea3c943c4ec7106e54bfaf92ee2eb3ad7aed1bf8449c3dd0ae7f17564952ce',1,'2026-11-04 15:07:38','2026-10-05 15:07:38'),('41f126b852f6bda4e0b5484aa9d8e0629a444aa0d2eafcfb86b0e3773f80cbfd',1,'2026-11-04 16:50:38','2026-10-05 16:50:38'),('41f95514e920892d08433acc8d2627b0527ab320466e2dfaacbbf31d7b410246',79,'2026-11-03 18:16:47','2026-10-04 18:16:47'),('43d83cad742c77d86ab594fc38a767edcba8301a506875a89a2795f0ea9ac874',79,'2026-11-03 21:14:58','2026-10-04 21:14:58'),('44138b4f8d739271999f4a93fb07667ece9ed8e2abb869cf83c4c6fe4cf6c5a2',1,'2026-11-04 17:59:23','2026-10-05 17:59:22'),('44b924aae1b45c1db753a1e8f7f578a6bd00da6e9386d5929ac78567db9227f3',1,'2026-11-03 10:59:34','2026-10-04 10:59:33'),('45246e0893503906042e8b5bc0232f28f06847121a1610ae9841b3acde48c019',1,'2026-11-04 11:04:57','2026-10-05 11:04:56'),('4572b9ef2440fd93ef5e23707b3a25d011b57675deb979cb4685bc5dedb9e5de',1,'2026-11-03 19:06:19','2026-10-04 19:06:18'),('45f224d5f72b8869c8a9a0144b18e16fd5242be3b31ab60e34edc0c3ef5e840d',1,'2026-11-04 21:18:05','2026-10-05 21:18:05'),('463ee67ab87cf073a68cfdc955daf3e0b67c683ff5352aefde9897f6e57ef756',1,'2026-11-05 03:22:19','2026-10-06 03:22:18'),('4704cf669b38f5bd8b97790dad705db0b3ef9161635633eae2e93a2d3e8bd961',1,'2026-11-04 14:24:17','2026-10-05 14:24:16'),('47689916079b71b88a9155ad26304ded299ecf2efc6ce6a39d94bdb953d240f9',1,'2026-11-02 22:22:45','2026-10-03 22:22:44'),('485bb40c04901945f0a3d935a102157793732a5b36e3c0eb1b87ab079635bc35',1,'2026-11-03 09:00:07','2026-10-04 09:00:06'),('48c67d5506308bd12e1ef51e9c585f1c0ca25ca5548afc3a82c00a3c6039a936',1,'2026-11-05 01:21:54','2026-10-06 01:21:53'),('495e3f99e44545505e248cc6f45f656a0eabe08479e556765cd9a7ef6eef1537',1,'2026-11-03 18:06:54','2026-10-04 18:06:54'),('4970189b63f18a25e2207a59b17ab9b6b2b7d730541ed8fceac0ea4c23f20905',1,'2026-11-04 14:39:37','2026-10-05 14:39:36'),('4974db4563668047d1246457fdaa6b2024020ad58528b4bed9054e44b521f06e',1,'2026-11-03 08:32:57','2026-10-04 08:32:57'),('4c1cad2ac4496b22674ff51636a031a850228a3c2876f48b102ba32aae156576',1,'2026-11-05 00:56:07','2026-10-06 00:56:07'),('4d5f03c492ec3a26cb547a1f3e828c2e9286fabb57e63dcf017a000494cb4f55',1,'2026-11-04 07:37:32','2026-10-05 07:37:31'),('4d608f3032c7b124336a0266c9e5c1e00f3567e576a5953eebe70fd96c8cb169',1,'2026-11-04 07:50:45','2026-10-05 07:50:44'),('4dcac9e16dc06a6772af83e2dff17d84f0edeee65f03a4d564d1ec2606c86513',1,'2026-11-04 13:33:33','2026-10-05 13:33:33'),('4e4545ae31f822bf9aae7c099b9dcab99c2b5c5c8f5c3be233c23c3168920c70',1,'2026-11-03 08:04:20','2026-10-04 08:04:19'),('5137d8491ad5e438f4186813dad8f231a9a24acef811d9a2b7e542da3cdef4a4',79,'2026-11-04 12:08:58','2026-10-05 12:08:58'),('520e560bc99636d607509a6e95aa1d0a869e28eac784fdc08a8de90fba241948',1,'2026-11-05 01:01:58','2026-10-06 01:01:58'),('528837db8418380d2e0f1d7c33a54275e1a867697ef0213c3d221173700f7384',1,'2026-11-03 08:27:59','2026-10-04 08:27:58'),('531a16555c5acf7014153b3a7cfc7f60497022c6d1cf9ad5860179f3234dbccb',1,'2026-11-04 15:14:20','2026-10-05 15:14:19'),('534d656f687f3410a8756c836b183577086284d507f4a47e4eb9bc41eaf236c1',1,'2026-11-03 08:01:05','2026-10-04 08:01:05'),('555e23870ffd790498704de1e93d18fc08f5181efa544e856f0100d4f5c41cb1',1,'2026-11-03 07:48:13','2026-10-04 07:48:13'),('56dd89f68d38c5d6dfc0c8cc7c93d2eee1e20672040b12bbf55571a186e8653d',1,'2026-11-03 09:29:21','2026-10-04 09:29:20'),('58394c3c0f136492c4453c3809de65d3f1504a51a6cd644e48b11e0e1fb3a432',1,'2026-11-03 10:51:29','2026-10-04 10:51:29'),('588aaa38b252b5d3a1226e1cec248996da5fcb9649db757587db0b48bb093431',1,'2026-11-05 04:22:02','2026-10-06 04:22:01'),('58fc18f0285c0bc2494ceb762ab3960af6547c2b96272fe3815ae7ad9039dfab',1,'2026-11-04 23:55:22','2026-10-05 23:55:21'),('5a16cd90e3d825dbe208d732029494279f93e354332f5808d6ec105314c882e2',79,'2026-11-03 13:22:05','2026-10-04 13:22:05'),('5a4e6a21a7c767fca892b4e45a840c06dd683022a4452d7fefdef701a9fb5bb9',1,'2026-11-03 08:27:57','2026-10-04 08:27:57'),('5aa330a3530c2141519b62cbb9b1585402fc1c70d4310e9f7cc90117dd6d0d48',1,'2026-11-03 08:27:57','2026-10-04 08:27:57'),('5ab9028a816b28965d369109e93ac5bf1745ecf7905ad55ece6ac553959f77f8',1,'2026-11-04 09:29:59','2026-10-05 09:29:59'),('5b1e0ce8ab1f85534f9f5e5fffc076d469f0c83a577dec15534765971630d304',1,'2026-11-04 17:55:21','2026-10-05 17:55:21'),('5b83f90f5e01cd1fc2d38eefbf4caba95ca82a230b41888946ba2493390fb5d7',79,'2026-11-03 13:24:34','2026-10-04 13:24:34'),('5bd37ebbc905ddf0fb5623e550708bac47b43fdb86c58d088b56e33355b686d1',79,'2026-11-04 09:41:46','2026-10-05 09:41:46'),('5cf4d4e1aa81a86fa848f1cd10a501e572ce1755c06d0d8c92681afcbaa133cb',1,'2026-11-04 09:26:02','2026-10-05 09:26:01'),('5da29b14633d7eca540faea4d39f7a10a209be0962ab5d650505244ee9fc34b3',1,'2026-11-04 15:07:56','2026-10-05 15:07:55'),('5df49210d857d987ffdfc93d24d47c683718ddb32b8ddf74dd558e3babcc755d',1,'2026-11-03 18:00:49','2026-10-04 18:00:48'),('5e1d9b5d795e5200d3522ed8183b836533bffcacc76d4bd1492272da19dcb673',1,'2026-11-03 08:34:14','2026-10-04 08:34:13'),('5e47ccc9cd1e40550e01a3db4e22514b1eb73f3a9cc1837b9d7f99c19b0b210d',1,'2026-11-03 08:27:57','2026-10-04 08:27:56'),('5e8babfb8d7d1b07b23f837ad1663a2fdfaa8a3f8cc1c1c1a95c686f34db1203',79,'2026-11-03 11:47:39','2026-10-04 11:47:39'),('604a82bcd651b1818395e33146382f6d461976daccf7b9700eec9f24548b140a',1,'2026-11-04 01:52:28','2026-10-05 01:52:28'),('608520b4591552a7e17ae209725b4173e34fb1347a4f251c43cdb7e8d5cb32fa',1,'2026-11-05 01:19:34','2026-10-06 01:19:34'),('613128bcf378c678be84164970df3910a0b73c60041202769d46413173977dae',1,'2026-11-03 08:34:06','2026-10-04 08:34:05'),('62222efdd9395880f71b9311fb69bb68fc227351e355af6a7688421431dce4cd',1,'2026-11-04 00:18:52','2026-10-05 00:18:51'),('626d82c2c65ba6ad619fe5f950f95783c22f8a39ebef7d1edf1a025a5f77129f',1,'2026-11-03 09:01:38','2026-10-04 09:01:37'),('633dd88957136729702dc40f58bec511e612ecec7e7de72f6fe00de172a2ed05',1,'2026-11-03 08:54:50','2026-10-04 08:54:50'),('6404379af307c42a4edbfd3901a62d3c28345ccf566ce729ddce4dd461cf8ba2',79,'2026-11-03 18:17:27','2026-10-04 18:17:26'),('644056fb129d622c20fee3ce58a52a66a7aedf3e51654ecdb81ab811f5d0dc30',79,'2026-11-03 15:47:38','2026-10-04 15:47:37'),('660ae5798915a7ddf2cc14cea3e880fc158b074b59562ca5e65eee50c2958ec2',1,'2026-11-03 08:08:01','2026-10-04 08:08:01'),('66515ce67084666943fc96329144f9cfc5afbd9d3df05320f35a959c325d7c1b',1,'2026-11-05 05:06:03','2026-10-06 05:06:02'),('6659a2c8a3fe0fc5d8f4f955a32e00c9ef864319aa046ddd62a74879c2601551',1,'2026-11-04 10:56:32','2026-10-05 10:56:31'),('68dd5db1e491cffd8f8ebdeb2356e3c2a50f77cf81205139b279f0ba028356e3',1,'2026-11-02 21:56:20','2026-10-03 21:56:20'),('6a278ddd6f44b8b08496709707db1ca02dbe756e217b9b2f8be4be8c89de0ea0',1,'2026-11-03 13:01:17','2026-10-04 13:01:16'),('6a2d1a0e7516c309537313d1adc8c962cd74c132ec7f6235186bbab828d95eeb',1,'2026-11-04 05:00:58','2026-10-05 05:00:57'),('6c977483618bc920fa9fd91aadf7b2b7814d5afd6ba4e3d175ddc189f60e48e3',1,'2026-11-03 10:17:35','2026-10-04 10:17:35'),('6cd2cab772a87a04b3b1612adbe968142abb32600f7086160c62c4efd15470bd',1,'2026-11-05 05:10:11','2026-10-06 05:10:11'),('7063a2ca5791f9e7c219559438626b2c7b9ef0a1c9ef5b43e9b14bc2a4d7febd',1,'2026-11-04 17:49:59','2026-10-05 17:49:59'),('71114e4fcbc5b7a3b88ef4ab9e9297a3a664d4cb751a00fad189901681bd8eee',1,'2026-11-04 04:40:40','2026-10-05 04:40:39'),('72030c9a2c5461ee8e3e69d3852160e8db3998e9a3e324adad215ec64880fa57',1,'2026-11-02 21:54:53','2026-10-03 21:54:53'),('727f71522c3ddf3836e64f822805ac6955595a65d9a2efc4412995280c7f5e86',1,'2026-11-05 00:02:42','2026-10-06 00:02:41'),('75485cd8915b7a8f898936a9d8bda2b587e2c7ca9e32f3011a95a530792f9684',1,'2026-11-05 02:38:59','2026-10-06 02:38:59'),('7696b59fb00e8fd68cc39238e2a9669cf967b9d24337d4acb6485f07235cdb25',1,'2026-11-04 11:30:03','2026-10-05 11:30:03'),('76c9e7bc2d8f9e229256745678f3f11d8904179c64470a62dd1408f29e254220',1,'2026-11-03 08:23:10','2026-10-04 08:23:09'),('77abf546f4bb3b935dfc9ef9323d7aba1ac980c8ad1f30ae0b2642b7c8d5baf5',1,'2026-11-03 18:22:03','2026-10-04 18:22:02'),('77bceb6ea6d35ac94bf8abed6112f594490a0c15dbd54268adf753af42ae2fa6',1,'2026-11-03 11:36:31','2026-10-04 11:36:31'),('7a6ff3556cac1f98df3abb8995593e5f5fcf9ed1d1049aea2b4245f4411a8432',1,'2026-11-03 08:33:06','2026-10-04 08:33:06'),('7aad4462acb8f9956adbe50df468e29501f28e0150b2b3e69da50b024a0aceac',1,'2026-11-05 01:01:02','2026-10-06 01:01:01'),('7af7e6171df40a942dbcf02c3c4f6fd935558e04ad8097af4475371f633cabfb',1,'2026-11-03 08:27:59','2026-10-04 08:27:58'),('7b57bfc8289f5ceb9e8bc6e31b03d175a8a87b8ad7ad0172c0d114d1c7eeaebb',1,'2026-11-03 11:06:47','2026-10-04 11:06:46'),('7dc5880ce07baa08cb46298c1590217d40c3d990b4b3e99ec06d9ec1a98cb06f',1,'2026-11-03 12:36:00','2026-10-04 12:35:59'),('7df57e5dd03b37f1acb09170fd3451c3bc86c0896e1e2343150a9d84798bbebf',1,'2026-11-04 07:14:12','2026-10-05 07:14:12'),('7e1decf2193b1bd4dbed49cf567bd3af132526989d366171a5581bb4e8f1de3c',1,'2026-11-03 10:19:35','2026-10-04 10:19:34'),('7e3ff7817d60f6a17bf70e8cc4763a77529db05df9d54af5c91338316cd9f9f3',1,'2026-11-05 02:28:09','2026-10-06 02:28:08'),('8000173fd9ab62b9f483181189f1acae3015a568104f5f96681ef77cd0b6061b',1,'2026-11-04 17:01:24','2026-10-05 17:01:24'),('803243b12046747868bd8ef58cedf6f7034bd0f0a6bc13d88cd712c068a0621a',79,'2026-11-03 15:15:16','2026-10-04 15:15:15'),('814557eb19d85e5895ac90397bb8bba820802748277e1354d327ff59fb275b4e',79,'2026-11-03 11:44:15','2026-10-04 11:44:15'),('81abd0ed1edaaa40a9696324d56a0d726bfcd61cf54294541a12d78e665b92ae',1,'2026-11-03 13:09:00','2026-10-04 13:08:59'),('82ae90191dc1b186407c23a215946c9b4902b10717f87de16654962f105e7bb5',1,'2026-11-04 05:48:12','2026-10-05 05:48:12'),('83e32476cc6b83ff54d7f2db528abcc886627424bac1ebab05d239da90f8db8b',1,'2026-11-03 19:12:33','2026-10-04 19:12:32'),('84d0b8f5e4270a9a878c682939e7d62004e6ab1355f042ccab31414285c5c3cc',1,'2026-11-05 02:49:29','2026-10-06 02:49:28'),('856ff7eb3681aa0191329220cc00a917d197d525a610d958c227da340dfd1383',1,'2026-11-05 05:41:57','2026-10-06 05:41:56'),('865c1ab6ffe5bd3c88e7b137e377b1fb93260b4f7ca62032eb94414e4d763fb9',1,'2026-11-04 06:19:00','2026-10-05 06:18:59'),('86a4ab5888f1031c26ae84b19524d4ce059c2deff2c4f7e4f00efee518c7a6ca',79,'2026-11-03 13:26:00','2026-10-04 13:26:00'),('88f7fe04ccafb322f6bdbab25a6229220ca663312a7ae4250347700fc78ae75f',1,'2026-11-04 16:52:43','2026-10-05 16:52:42'),('8916ee480d4eee134c2f2d617a8490a1adb51b7da2db32617809b00406baf892',1,'2026-11-05 01:45:25','2026-10-06 01:45:25'),('8924ac6f7b4cc59aff48e10c624dd0b27ff6085e07ad48b446ea195f99053b07',1,'2026-11-03 08:27:57','2026-10-04 08:27:56'),('898431852a7711622ee727eaeeb4f09853ee9db5449371787a5e074ec5b38cf7',1,'2026-11-03 07:48:23','2026-10-04 07:48:22'),('89d6c3a45782e66fcdbcaa71528b3697f26e6df3666e02a741d1ee9c4d1640bd',1,'2026-11-03 16:07:28','2026-10-04 16:07:27'),('8ae0e57c56987b9c1337c9d77501a69ee7a925d89b43e64f616373a4d6fcbc20',1,'2026-11-04 17:42:19','2026-10-05 17:42:19'),('8be1b4d15a90b0e00575081326fccb8e0bd5cd0c9d49d31523a0b7e5309e7b66',1,'2026-11-02 22:23:31','2026-10-03 22:23:31'),('8d3b8f9480314b3c1fe3f94b2919d86698b7a6f3ccc3b1af2a347371ac7fc28d',79,'2026-11-04 14:39:37','2026-10-05 14:39:36'),('8dbe8e48878a0fc874e3a3682cd8455777a509864cf35871ef716c3075342466',1,'2026-11-05 04:04:41','2026-10-06 04:04:40'),('8eb0d78887d8898d4b9220ebf0532a6382a17f6a0b7e3faccbb520ad8e12dd9f',1,'2026-11-03 13:30:22','2026-10-04 13:30:22'),('90129356f797d9e8cb28ca556ee46573b253b69781a4402f57f9962b9467ac5c',1,'2026-11-03 22:40:51','2026-10-04 22:40:51'),('91a00cf7cb3b1004a09c1996a4be0b5cf3eeff274955d62fad4f070d3e848430',1,'2026-11-03 08:34:06','2026-10-04 08:34:05'),('927c7dd1a5336b7c2c09e02729ac647092bac5ff94c00951a8532cc2affa81b6',1,'2026-11-03 08:55:00','2026-10-04 08:54:59'),('92e42323951b860dd53f82844fdbdf563f7dbac597e3342a7826bb278d1beb1a',1,'2026-11-03 08:33:40','2026-10-04 08:33:39'),('93423696bd024a9eeadd2b8c3fddf3620c85632b2a55e1e2036c0fbc88fc8628',1,'2026-11-03 13:11:11','2026-10-04 13:11:11'),('93867ab014d1fde352e28f3fb0a95fbef102c1776d7136019259d75af96dab04',1,'2026-11-03 11:08:58','2026-10-04 11:08:58'),('942f3bb7554944bebc8bd217ce9566e4a226158c172dfe5dc48b47abdece8468',1,'2026-11-03 08:34:06','2026-10-04 08:34:05'),('946cbb9fd8a0d3c980ca97d95cd8f64ed42ee6eed165afb39c5b1f1723a5c256',1,'2026-11-03 17:59:01','2026-10-04 17:59:00'),('94d9ad182badc9a0f02afb77691e103a11dba3e2ee8dfcd531fbe8688775cfc0',1,'2026-11-04 05:01:09','2026-10-05 05:01:08'),('9557a9d02c65097ce4a137015361c2b62e485f95a1664a386286e9ac1a78c983',1,'2026-11-03 11:23:07','2026-10-04 11:23:07'),('959458616e504168bd192c6329a1382793e3c1c5e92172e32d83a223e6daf0cc',1,'2026-11-04 05:47:37','2026-10-05 05:47:37'),('96e01ea70629220591e55a0c5ec0c072584c52e3183e1f63918fa0438d0f4534',1,'2026-11-05 04:11:02','2026-10-06 04:11:01'),('97f8830a3020f335f9dd17be852feff291f5b6bf64a82d53fc4c7dea22b05378',1,'2026-11-03 15:34:47','2026-10-04 15:34:47'),('984ec6132285ca510362754acd8e6e974786c36fe5dc3493aaffc57db828d05b',1,'2026-11-05 02:17:09','2026-10-06 02:17:08'),('98fe26cdff7eb78f00b7dd1047e06096440cdfcc708f69b8938a9b03ee637a7e',79,'2026-11-04 08:55:03','2026-10-05 08:55:03'),('99d286c5bdae0aea1d72219c29b04ebc07894677f582958b5acbb00dcd2d0588',1,'2026-11-04 04:40:18','2026-10-05 04:40:17'),('9a3bccaa1fe1f08a847301d605cdb4a77555a11b4ff27ad41ab0e18df10cb95c',1,'2026-11-04 06:17:56','2026-10-05 06:17:55'),('9b527cb80656eeb2d1e7d842fc7da97532a507ff9993ae94017d8c2f849ecaf7',1,'2026-11-03 18:29:52','2026-10-04 18:29:52'),('9b9c5f57756abce87d2de0dd0c1d283a7e13aba11bc723f046d68a79afffd692',1,'2026-11-04 17:50:30','2026-10-05 17:50:30'),('9c8f4b11920c9eac301f234361e94f58e19e008b409d73f8e5a5c6d817f1c1fe',1,'2026-11-03 10:21:02','2026-10-04 10:21:01'),('9eef167f57bf96d5f582245ae9d904f9a3a4bd83db596f16e01c0ebbf1e9c577',79,'2026-11-04 06:22:54','2026-10-05 06:22:53'),('9f928ba85637e57311141902c7337191f3888f66b00a7f076fbdedcb4885fd50',1,'2026-11-04 17:26:46','2026-10-05 17:26:45'),('a10f2d13f3d0debc0220700e5e5a733ce64d0bb7344e77be1f10b36d8d24f341',1,'2026-11-03 12:54:38','2026-10-04 12:54:38'),('a1a8093cd7ff0dfe5fdb5a10f66f7ce571ebbab6f0eeed293beba0c6790dd698',1,'2026-11-04 05:48:03','2026-10-05 05:48:02'),('a1ab2d7b4e2f018739d926c3130b74b72f2163113a412bbf8ae336018006a131',1,'2026-11-04 17:58:35','2026-10-05 17:58:35'),('a1fbb8ea1c4a33711d24faf0a830b0a9e216658cd3df4de5f5190c6c151da96b',1,'2026-11-03 11:28:20','2026-10-04 11:28:20'),('a21473fcc52685fa85fe98fe9dccea6c08b9f47c3206e6eb329ae97d056103dc',1,'2026-11-03 18:51:38','2026-10-04 18:51:38'),('a3d9de13681d54c24f4c7d212c3cbed3a091c3ed361c1061bf80277441ee656f',1,'2026-11-04 05:05:18','2026-10-05 05:05:18'),('a3fd538378c6d1b8ea541816871684266102304283b7c9c3a78e38fc3812edd6',1,'2026-11-05 00:50:40','2026-10-06 00:50:39'),('a61fdb2d75328219aebc94d7c9a596719c422bcca858b3fa405679dbb74a01b2',1,'2026-11-04 18:00:13','2026-10-05 18:00:13'),('a6b502ce0cfe3563597e3ef4df1f7dc6de0d4f7114b348f73bf0989b070d993e',1,'2026-11-05 04:35:58','2026-10-06 04:35:57'),('a85e8ffe10f1d52823192c78443d1e9e3a6dedf9e837bea0ac15afb80c1d0b53',1,'2026-11-04 16:45:50','2026-10-05 16:45:50'),('a87be8f22a60efadd056ff2e7b19eb7c1dc912c6fb6bf313e0846e4b1f61d914',1,'2026-11-03 16:07:02','2026-10-04 16:07:02'),('a8b16d493e80649cae967118f1675f9b3e6103e3ef05ecbb44c95c69c376a020',1,'2026-11-05 03:17:54','2026-10-06 03:17:53'),('a8d9ef6877e0b9565942c145e3915386fca9509bd80ccb594f11957c18ccf056',1,'2026-11-04 06:05:54','2026-10-05 06:05:54'),('aad70414867cddde192836787556323932ed82b7a7e77ec18efa66a9d6e1ee29',79,'2026-11-03 11:33:22','2026-10-04 11:33:21'),('ab1c5be3a7e1a75958cf5c723422382febae446407870b4e844d42439961be33',79,'2026-11-04 10:09:07','2026-10-05 10:09:06'),('abe33578228a1cd6e13cb3c803913ad353ab3014d5118adfbaa94dc2ffec89e5',79,'2026-11-04 08:28:19','2026-10-05 08:28:18'),('ac244b7e7b687f6a522656a90c15232efbac19289180fa5a5890ed530fa99e87',1,'2026-11-04 22:27:13','2026-10-05 22:27:13'),('ac353fb29a084bd49f944b2e90465a0b8e7e19d052ce6a4fb51e12188bfe62a5',1,'2026-11-04 09:26:17','2026-10-05 09:26:17'),('ad7cc20eb165c417f51c2222f7b32f3b0fe0908aa7661199cf58fe545a9d76de',1,'2026-11-04 21:58:23','2026-10-05 21:58:23'),('ade831f443f7572811aed2008dff76e353dd5ed853046ddc18224d050a4be48c',1,'2026-11-05 03:01:32','2026-10-06 03:01:31'),('ae9ddc8c6b7380a2654b8120fa6cafafb02d816d62bbdc8465a23c4d53e4f02c',1,'2026-11-05 00:10:53','2026-10-06 00:10:52'),('b33a8fb95bb27a7ceb0ceec74ad461434903adc86d3e00992e936a1fc86c385a',1,'2026-11-03 13:59:10','2026-10-04 13:59:10'),('b42eb169e57e4b8af5e74f3e68be1b2be390d7772edbea61e1f7a75a14167412',1,'2026-11-03 22:42:10','2026-10-04 22:42:09'),('b4950c9fd71b23180d372e2542f1e343bb49404651c3efe584ebb57e3c8d6898',1,'2026-11-05 00:28:58','2026-10-06 00:28:57'),('b5776049306af888bc7f9bd5dadc75e7b8fc86f48b189d1a36791948f7f9455f',1,'2026-11-03 19:48:08','2026-10-04 19:48:07'),('b5d92120b7df82870bc018c72b11ad1eaa20c6b6c15867b2309f8dc33ccb87c4',1,'2026-11-03 15:47:38','2026-10-04 15:47:37'),('b9ac6315e93648092a04056da430edf165403a31b24aad40035814bbe519e585',1,'2026-11-04 17:54:12','2026-10-05 17:54:12'),('bae5f803e5b22ede73b719ee7fd0560c7f7bf388ed4c4985a727e4376b9cc913',1,'2026-11-03 13:02:33','2026-10-04 13:02:32'),('bc78eed091c13dd774057dca9bb1aa686303d64c9ef2f21581f3b27fdbe1fe76',1,'2026-11-05 01:36:04','2026-10-06 01:36:03'),('bcedcd723c9065e00f1a8015b10d4869312556c34bfa785470234c1240e187d0',1,'2026-11-03 10:22:43','2026-10-04 10:22:43'),('bd006a6bcf44a6d0127ee21d46b5d80569ce803b86f1d91a029fd9e9c9af9741',1,'2026-11-04 03:45:24','2026-10-05 03:45:24'),('bddd954c8a5a60628b6d51e879fb5794fb617b1227c8ea36a4b3cd839ea8be53',1,'2026-11-04 06:10:18','2026-10-05 06:10:17'),('bf72aa275dc7019925f8373ea1837b62ebeb9c199314322efe7b18861c592763',1,'2026-11-05 05:54:25','2026-10-06 05:54:24'),('c0bf840ab0dac09b7884739592a7e6411dc6115c1d1ab9b65325345181eb096e',1,'2026-11-03 11:51:14','2026-10-04 11:51:14'),('c1e0538897b626c1b8b0c9a2c7c58408a8a9bee0ae9e2ccca41e7404a5b0b5be',1,'2026-11-03 13:16:16','2026-10-04 13:16:15'),('c28161b305a54f1866d5505113241e457af4a56aca7ce74dfcecc8f5083b543d',1,'2026-11-04 09:47:01','2026-10-05 09:47:00'),('c31713051863f139d6db38e0072b29aa69729237f3ed73556fb903ec0f1a4b53',1,'2026-11-03 11:47:16','2026-10-04 11:47:16'),('c373580b6b08a941b35e3a9e67f0a11e1b2d65c6ce2fdebedf334bda3272ea1d',1,'2026-11-03 08:31:37','2026-10-04 08:31:36'),('c422c20452125b43939af36047674e6f9b6b33276c712c5a1bda6895da0928be',1,'2026-11-04 17:53:39','2026-10-05 17:53:38'),('c6a0b550a518ef434102178ba4604755b6a38dcff6c29985dcd7b9dea57ad1fa',1,'2026-11-04 06:22:17','2026-10-05 06:22:16'),('c6a6956e2d3ae33404c4ffe47de3e47dfaf8e3ecf4148d7465c5a111556e69cb',1,'2026-11-04 22:51:46','2026-10-05 22:51:46'),('c6b55d6eeab318da8d663d0d3d7e439d4f24ed4386f38190f9df89a4f80c9d38',1,'2026-11-03 12:35:37','2026-10-04 12:35:36'),('c739199ce3bbfa56a44696f0bcc3920d65fcb02c0ac388be8ec3f722381353a1',1,'2026-11-03 18:01:10','2026-10-04 18:01:10'),('c77ce56189a3ecc3b830b6ab2f93eeec0a906a4aac53e94908ebfe23c8066bf6',1,'2026-11-03 11:46:55','2026-10-04 11:46:55'),('c78382af0b642e3c144c59596267e9a32907490d1137464c6c4fc1330e7ab480',1,'2026-11-05 05:03:08','2026-10-06 05:03:07'),('c7e033a1d9d60c7145e0b22becf080a7cf06bc54262507000277346385187df7',1,'2026-11-04 15:22:40','2026-10-05 15:22:40'),('c905b6c5e442af67f5a06603746a5f70fca30ad960f3ace2f63d0ba2037fc4f0',1,'2026-11-03 17:16:45','2026-10-04 17:16:44'),('c95d917e2d7f36c4d628734e83c7ae7ef16e3ecbf009ccc41de15e7ea4b74635',1,'2026-11-04 11:59:13','2026-10-05 11:59:12'),('c9932861861fc956f3b60f0f0cd3be47a7e146fdb72b7613842fcc0b46c1933c',1,'2026-11-04 08:51:50','2026-10-05 08:51:50'),('cbde181819d69d4fb6ec09eb07696e1fd25bfa8bedc9821158fb55c2b8adef54',1,'2026-11-04 22:05:57','2026-10-05 22:05:56'),('cc377222c15f75e8d4e541f48cc80c0c012ac92418a81cc5605b0347bf7b7ca5',1,'2026-11-03 08:32:57','2026-10-04 08:32:57'),('cc5944a2f81d0e9a67b23eba0cb100b412de9acce5e1041a2348f2dcceb5173e',1,'2026-11-04 07:54:38','2026-10-05 07:54:37'),('cc783089400d218be7d5d8293b1d70a9402493f7e3c61feae68d012a4f7c5848',1,'2026-11-05 05:35:21','2026-10-06 05:35:21'),('cc94801fb8f3d447909ad6155532a3a326e2fc1e46011c887f781da767de32cf',1,'2026-11-04 16:06:50','2026-10-05 16:06:49'),('cdb9c1d399ad9c2a7863f6c0bb4f3009305513dbf59a91af368e26366f23ed18',1,'2026-11-03 08:27:58','2026-10-04 08:27:57'),('ce5774142047904844a831ac8f62f72a41f0f87e526945e63764ce14948694ca',1,'2026-11-05 05:47:02','2026-10-06 05:47:01'),('ce63b592ed7f3f58d7803e8b67375dec40ba8702be3c411ed2681b75f2692105',1,'2026-11-03 08:32:57','2026-10-04 08:32:57'),('cea764d23db45b1b3694463f9375034469af93e8bf98346fda284f5943e7e90d',1,'2026-11-04 12:08:58','2026-10-05 12:08:57'),('ced60bf28e8836c073904bc81f458edc03f6a62e86868f976c51e26b089c09fb',1,'2026-11-05 04:43:16','2026-10-06 04:43:16'),('cfdc2c59c7741584ef970154242cad6703a60b55df228ac9eee0371c909fb7f6',1,'2026-11-03 18:23:16','2026-10-04 18:23:15'),('d0628c688213513f0c954baa2e2ca2d113b341c476dce681f75ce4af30f58542',1,'2026-11-04 09:49:00','2026-10-05 09:49:00'),('d23595af282ba529e3162044c63c95459ff503ea6806d1077c7ba968307093b6',1,'2026-11-03 08:34:06','2026-10-04 08:34:05'),('d34683f3257a059367dd1ca1664bae0295d89d5c0cbb2f7ea6544385256125f3',1,'2026-11-04 02:18:06','2026-10-05 02:18:05'),('d34c3d22461894552451e3c3e3c00ecf7d8b7bd17c7a5ab9701d628cd8a15274',1,'2026-11-03 10:22:25','2026-10-04 10:22:24'),('d3c925f9edf26d7d9eaaf83a54cd5575cfe97586ca65cafc9d3b6a1ec4676731',1,'2026-11-03 16:01:18','2026-10-04 16:01:18'),('d3da16af345deab2e15cd6e431878a937392435d58a907fff657b9e2161e30a3',1,'2026-11-04 23:51:02','2026-10-05 23:51:02'),('d4724e90be3a90884122326d17685e8c0ac149e9328ccbf20b79bc361f2160a0',1,'2026-11-04 13:02:26','2026-10-05 13:02:26'),('d5176ab2273c21dddab0c85e35ab814a2f76447f1c089f659eec5c3c50ab3914',1,'2026-11-05 01:24:54','2026-10-06 01:24:54'),('d543035719a3fbccbe51105da420ec6d05b11fdeff87eef8962ee4f7d6b036d0',1,'2026-11-03 21:14:58','2026-10-04 21:14:58'),('d6090316f72d9855653d01e2962942327dc933d86b58599b4adccf829fc45cb3',1,'2026-11-05 06:09:25','2026-10-06 06:09:25'),('d6f6e0b81810a972a5df779ebe64fa508490b31ac86dc2654de9fa41f0cd4c67',1,'2026-11-03 08:27:57','2026-10-04 08:27:56'),('d70c7ec9dcbb4982e20e636f0919566cccf9be0a10517e46b55171ca2729b72a',1,'2026-11-04 07:38:35','2026-10-05 07:38:35'),('d7fdd523a814d9221a63850c1ef608a4a83091700c48b09846cee2a488854f04',1,'2026-11-03 08:32:57','2026-10-04 08:32:57'),('da52881e45cc2a54ce5fc78032f0cc23086fec93bd6a156ea24ffbbb74877c29',1,'2026-11-03 08:49:11','2026-10-04 08:49:10'),('daa582250f5ebc3527f82569520eb63d8abf76c75e8b2cf45ce2767e9fac338f',79,'2026-11-03 20:57:57','2026-10-04 20:57:57'),('dab4cd6bbd9b8b9d341accff403d76d41674f74b9de1260b9fd2606ec4d1123b',79,'2026-11-04 02:18:06','2026-10-05 02:18:05'),('dca69ac9763c7b521f55bdca3d81fe147b0b909ac71ab28884df992464760020',1,'2026-11-03 11:51:45','2026-10-04 11:51:44'),('dd02f0f157d6c2306663ad95aaebdc12506a677c7075f26694042298990b16ef',1,'2026-11-04 05:52:52','2026-10-05 05:52:52'),('dd497b5418edba932ce83888c9ad53e430e95e53f776884e3ee4e4c4660a27bf',1,'2026-11-03 08:01:40','2026-10-04 08:01:40'),('dd88c87e07eb439be59e7771921da7e59f9827e616c8dae9ead2295b0151a634',1,'2026-11-05 01:40:11','2026-10-06 01:40:11'),('de29522af30e81a16f046fd0af6eda742331f00814e8810f24e96b76405b1236',1,'2026-11-05 00:46:45','2026-10-06 00:46:45'),('df6b9ce6f873e5e4290c9a7ab90ca359986850f639ac3fec1869cca8df165978',1,'2026-11-04 10:09:07','2026-10-05 10:09:06'),('e0f397081d6bd9224f2ad988979edb89848341a28f56964ae235c946bb96b827',1,'2026-11-05 05:02:52','2026-10-06 05:02:52'),('e1107feca237920e419cb302c3fa1cee185131e0627e950bbaba97a90ddf4a15',1,'2026-11-03 08:26:50','2026-10-04 08:26:50'),('e12d5321a16315a3569fbca2db4302899ef735b1756465b07a449b48dc4bb1fa',1,'2026-11-03 13:59:39','2026-10-04 13:59:38'),('e15dffd54452e75df5f29e80d54afa609f5bae4e3e463b92cc2bb54948fe80cd',1,'2026-11-04 01:48:42','2026-10-05 01:48:42'),('e1e041ccd2a332ddf36407b565b39517bdd738fec9731aa6f0b458f280ce4adf',1,'2026-11-05 02:47:51','2026-10-06 02:47:51'),('e285412b92a4f88a5e8d5d49727d86f7171c723a0869857728f67cbc3395bce9',1,'2026-11-05 01:23:54','2026-10-06 01:23:53'),('e35b8b80877fe22837f7f0d6759783e95fe2707688d76c1d769eeb8bbd61c35c',1,'2026-11-05 01:15:17','2026-10-06 01:15:16'),('e3ea02869ca61560a641dfc5964c110a0e0bfd06f7c7a0c40de6ff842dd333ff',79,'2026-11-03 13:59:39','2026-10-04 13:59:38'),('e6b8dc8ed440bb13c4bb8c247dff74ed47fccebe3c5834abefc5699285970683',1,'2026-11-03 11:33:22','2026-10-04 11:33:21'),('e6de76eaf118eb83f8fc15027821fe2b6638e043715536a899d06703d978e105',1,'2026-11-04 17:58:02','2026-10-05 17:58:02'),('e9da0cdac6c24cff06b3ec452db531d30d1acad79666b43135858181a4670cce',1,'2026-11-03 18:43:58','2026-10-04 18:43:57'),('ea6696429be1918ec54125aac3f78385431bff66a7cea471d5fc60c837dea998',1,'2026-11-03 10:18:06','2026-10-04 10:18:06'),('eb40c77c1b6e616bff8c49359604ad8137c919f31d6c3400e235470baebcaf7a',1,'2026-11-03 08:27:57','2026-10-04 08:27:56'),('eb756eed4b61e92f8aa2b609d8d90fae0bbf65fe832e53dd461e50df21be26a7',1,'2026-11-03 08:41:01','2026-10-04 08:41:01'),('ebfb9ec86efe15a879417d42876bd513b41b586f364a2bc16faa2c5cefdf5dcd',1,'2026-11-03 07:48:00','2026-10-04 07:48:00'),('ed0a7e90f804eb4b914e660291f5c262ae1f4b352417e6a5b5dc70c3c4035071',1,'2026-11-03 20:58:13','2026-10-04 20:58:13'),('edc277b2a64931587e683fdfd88de9d5a02be6408bcf11abdedfbcf38c91c884',1,'2026-11-05 00:17:00','2026-10-06 00:17:00'),('eeb75f8036451a7d92ba81cbb57addff34556ba268ec4b51a928b03e5ae16123',1,'2026-11-04 09:25:51','2026-10-05 09:25:51'),('f0e3960c33ed2b95f13f242c9247931e40304960fb2874f3a1be916cf34c586c',1,'2026-11-03 17:58:34','2026-10-04 17:58:34'),('f21586ad3a5a628b7e27dbe96a223fbd97a398e241634db7bd08ced038d1c691',1,'2026-11-03 18:06:20','2026-10-04 18:06:20'),('f2c8a27320f7e5edbc2a49add9e9711e7976dd2cd185d19fe27fc7e18ef27181',1,'2026-11-03 08:27:58','2026-10-04 08:27:57'),('f3aed9bf9c7d3c077814821cf1b9c9b24b4379511c8d20184117f7c75dc343a9',1,'2026-11-03 07:48:33','2026-10-04 07:48:33'),('f52789d8edcf48ea072779d7e228f011daed7a91b402888aa4a3a01895131f85',1,'2026-11-04 23:23:18','2026-10-05 23:23:18'),('f5ca3145eedcb9c56c99923f4521c7349978c1b397dff66268ea311249907a70',1,'2026-11-03 08:48:57','2026-10-04 08:48:57'),('f78f59327dfde849cf27bba7f279daa9c4c3c159d74ddf81d369317d4d08f0fa',1,'2026-11-03 08:31:36','2026-10-04 08:31:36'),('f7e8bf4756dc6c3da308dda3b56d771e995266429e8d2fc40f65f5c997d3ad7e',1,'2026-11-04 08:26:05','2026-10-05 08:26:04'),('f85a86a98cf658a89eec2f9a11e79d1d930cacb5fc80895b1c4a832a2adbb77c',1,'2026-11-03 11:06:28','2026-10-04 11:06:27'),('f8e24aaf9ea382ce688174182ade6d3f4481067f4ee0ac8b9254fa8244c9186c',1,'2026-11-04 21:21:15','2026-10-05 21:21:15'),('f9ae49f416a5417ed921fb5eb5978180fbbc28cd67dbde286364c0e5da57fc9f',1,'2026-11-03 08:34:06','2026-10-04 08:34:06'),('f9de342f698d294c9e4175183253da1f8cecfc868c23b3b8e02eeeaa4dec1e35',1,'2026-11-04 06:18:52','2026-10-05 06:18:51'),('fbfde2e157938db03ebea79273b98da715e82a52f7bcd7e4f8b2ed16820b95b9',79,'2026-11-04 15:08:08','2026-10-05 15:08:08'),('fc61a6cd5613d4517849394529a0390777fd3ef3caf350e8c3faa55063ebc571',1,'2026-11-05 03:02:52','2026-10-06 03:02:52'),('fc6506c80083e725f3c565c1847d7f34aad62f0e421aea943eba9c1a7c327edb',1,'2026-11-04 15:32:14','2026-10-05 15:32:13'),('fe0b42d49702ebf5f0774ca957177dda922a9c9bdc47757607e97ad2902c28e6',79,'2026-11-03 15:34:34','2026-10-04 15:34:34'),('fe16ba7cda67bf41db94e4d87d4d4f522fb3627f6db44bd8c854cd195a3b5192',1,'2026-11-03 08:05:09','2026-10-04 08:05:09');
/*!40000 ALTER TABLE `biz_member_session` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `biz_mini_content`
--

DROP TABLE IF EXISTS `biz_mini_content`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `biz_mini_content` (
  `content_id` bigint NOT NULL AUTO_INCREMENT COMMENT '鍐呭?ID',
  `content_key` varchar(128) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '绋冲畾鍐呭?閿',
  `content_type` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '鍐呭?绫诲瀷',
  `title` varchar(128) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '',
  `subtitle` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '',
  `content_json` json NOT NULL,
  `image_url` varchar(512) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '',
  `link_route` varchar(128) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '',
  `sort_num` int NOT NULL DEFAULT '0',
  `status` char(1) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '0',
  `create_by` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_by` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `remark` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`content_id`),
  UNIQUE KEY `uk_biz_mini_content_key` (`content_key`),
  KEY `idx_biz_mini_content_type_status_sort` (`content_type`,`status`,`sort_num`)
) ENGINE=InnoDB AUTO_INCREMENT=135 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='灏忕▼搴忓叕寮?〉闈㈠唴瀹';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `biz_mini_content`
--

LOCK TABLES `biz_mini_content` WRITE;
/*!40000 ALTER TABLE `biz_mini_content` DISABLE KEYS */;
INSERT INTO `biz_mini_content` VALUES (1,'home.hero','banner','金色酒业','品质酱香，礼遇知己','{\"alt\": \"首页主视觉\"}','/mini-assets/design/hero-product.jpg','shop',10,'0','seed','2026-10-02 23:47:37','seed','2026-10-02 23:47:37','首页主视觉'),(2,'home.detail','image','品牌详情','','{\"alt\": \"品牌详情横幅\"}','/mini-assets/design/detail-landscape.jpg','',20,'0','seed','2026-10-02 23:47:37','seed','2026-10-02 23:47:37','详情横幅'),(3,'home.navigation','navigation','','','[{\"icon\": \"scan\", \"name\": \"扫码有礼\", \"route\": \"scan\"}, {\"icon\": \"shop\", \"name\": \"兑换网点\", \"route\": \"stores\"}, {\"icon\": \"coin\", \"name\": \"积分专区\", \"route\": \"points\"}, {\"icon\": \"users\", \"name\": \"VIP社群\", \"route\": \"vip\"}]','','',10,'0','seed','2026-10-02 23:47:37','seed','2026-10-02 23:47:37','首页快捷导航'),(4,'rules.shipping','rule','配送说明','','[{\"text\": \"具体配送范围和运费以结算页服务端报价为准。\", \"title\": \"配送范围\"}, {\"text\": \"请在签收时核对商品数量和包装，物流信息可在订单详情查看。\", \"title\": \"收货验货\"}]','','',10,'0','seed','2026-10-02 23:47:37','seed','2026-10-05 02:57:31','规则页面内容'),(5,'rules.campaign','rule','活动规则','','[{\"text\": \"活动时间、参与次数和奖品库存以后台当前配置为准。\", \"title\": \"活动期限\"}, {\"text\": \"每个会员的领取和使用资格由服务端校验，重复请求不会重复发放。\", \"title\": \"参与资格\"}]','','',20,'0','seed','2026-10-02 23:47:37','seed','2026-10-05 02:57:31','规则页面内容'),(6,'rules.points','rule','积分规则','','[{\"text\": \"签到、消费和活动奖励按服务端规则入账，积分有效期以明细显示为准。\", \"title\": \"积分获取\"}, {\"text\": \"兑换成功后积分和库存由服务端原子扣减，实物奖品按页面提示到指定门店领取。\", \"title\": \"积分兑换\"}]','','',30,'0','seed','2026-10-02 23:47:37','seed','2026-10-05 02:57:31','规则页面内容'),(7,'rules.ranking','rule','排行榜规则','','[{\"text\": \"排行榜按后台配置的周期统计有效佣金业绩，退款和无效订单不计入。\", \"title\": \"统计周期\"}, {\"text\": \"排名奖励和补贴以本期规则及后台发放记录为准，重复结算不会重复发奖。\", \"title\": \"奖励发放\"}]','','',40,'0','seed','2026-10-02 23:47:37','seed','2026-10-05 02:57:31','规则页面内容'),(8,'rules.promotion','rule','推广规则','','[{\"text\": \"推广关系由服务端建立并确认，已绑定的上级不能随意更换。\", \"title\": \"推广关系\"}, {\"text\": \"订单满足约定的支付、收货或核销条件后进入结算，退款会按规则冲回或追偿。\", \"title\": \"佣金结算\"}]','','',50,'0','seed','2026-10-02 23:47:37','seed','2026-10-05 02:57:31','规则页面内容'),(9,'rules.draw','rule','抽奖规则','','[{\"text\": \"活动码、个人次数、活动次数、中奖概率和奖池库存由服务端校验。\", \"title\": \"活动码\"}, {\"text\": \"现金奖励以微信转账最终回执为准，实物奖励须凭兑换码到符合条件的合作门店核销。\", \"title\": \"奖品领取\"}]','','',60,'0','seed','2026-10-02 23:47:37','seed','2026-10-05 02:57:31','规则页面内容'),(10,'rules.coupon','rule','优惠券规则','','[{\"text\": \"优惠券门槛、适用商品、最低件数和有效期以服务端报价为准。\", \"title\": \"使用规则\"}, {\"text\": \"未付款订单取消后按规则释放优惠券，退款和部分退款按订单实际处理结果更新。\", \"title\": \"订单售后\"}]','','',70,'0','seed','2026-10-02 23:47:37','seed','2026-10-05 02:57:31','规则页面内容'),(11,'brand.avatar','image','默认头像','','{\"alt\": \"默认会员头像\"}','/mini-assets/design/avatar.jpg','',10,'0','seed','2026-10-02 23:47:37','seed','2026-10-02 23:47:37','默认头像图片'),(12,'brand.store','image','门店图片','','{\"alt\": \"门店展示图\"}','/mini-assets/design/store.jpg','',20,'0','seed','2026-10-02 23:47:37','seed','2026-10-02 23:47:37','门店默认图片'),(13,'brand.map','image','门店地图','','{\"alt\": \"门店地图\"}','/mini-assets/design/map.jpg','',30,'0','seed','2026-10-02 23:47:37','seed','2026-10-02 23:47:37','门店地图图片'),(14,'brand.points','image','积分页主图','','{\"alt\": \"积分页主图\"}','/mini-assets/design/detail-scene-clean.jpg','',40,'0','seed','2026-10-02 23:47:37','seed','2026-10-02 23:47:37','积分页图片'),(15,'brand.scan','image','扫码页主图','','{\"alt\": \"扫码页主图\"}','/mini-assets/design/detail-scene-clean.jpg','',50,'0','seed','2026-10-02 23:47:37','seed','2026-10-02 23:47:37','扫码页图片'),(16,'brand.ranking','image','排行榜主图','','{\"alt\": \"排行榜主图\"}','/mini-assets/design/landscape.jpg','',60,'0','seed','2026-10-02 23:47:37','seed','2026-10-02 23:47:37','排行榜图片'),(17,'brand.cup','image','品鉴杯图片','','{\"alt\": \"品鉴杯\"}','/mini-assets/design/cup.jpg','',70,'0','seed','2026-10-02 23:47:37','seed','2026-10-02 23:47:37','扫码页奖品图片'),(18,'brand.productDetail','image','商品详情图','','{\"alt\": \"商品详情图\"}','/mini-assets/design/detail-landscape.jpg','',80,'0','seed','2026-10-02 23:47:37','seed','2026-10-02 23:47:37','商品详情兜底图片'),(19,'brand.productStory','image','商品故事图','','{\"alt\": \"商品故事图\"}','/mini-assets/design/detail-scene-clean.jpg','',90,'0','seed','2026-10-02 23:47:37','seed','2026-10-02 23:47:37','商品故事图片'),(20,'brand.poster','image','推广海报主图','','{\"alt\": \"推广海报主图\"}','/mini-assets/design/detail-scene-clean.jpg','',100,'0','seed','2026-10-02 23:47:37','seed','2026-10-02 23:47:37','推广海报兜底图片'),(21,'brand.qr','image','二维码兜底图','','{\"alt\": \"二维码\"}','/mini-assets/design/qr-demo.png','',110,'0','seed','2026-10-02 23:47:37','seed','2026-10-02 23:47:37','推广二维码兜底图'),(22,'home.hero.2','banner','窖藏时光','匠心酿造，典藏好酒','{\"alt\": \"首页轮播图二\"}','/mini-assets/design/detail-scene.jpg','shop',20,'0','seed','2026-10-02 23:50:12','seed','2026-10-02 23:50:12','首页轮播图，可在后台替换图片'),(23,'home.hero.3','banner','举杯共饮','一杯好酒，礼遇知己','{\"alt\": \"首页轮播图三\"}','/mini-assets/design/landscape.jpg','shop',30,'0','seed','2026-10-02 23:50:12','seed','2026-10-02 23:50:12','首页轮播图，可在后台替换图片');
/*!40000 ALTER TABLE `biz_mini_content` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `biz_mini_idempotency`
--

DROP TABLE IF EXISTS `biz_mini_idempotency`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `biz_mini_idempotency` (
  `member_id` bigint NOT NULL,
  `request_id` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL,
  `action_type` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL,
  `payload_hash` char(64) COLLATE utf8mb4_unicode_ci NOT NULL,
  `reward_id` bigint DEFAULT NULL,
  `member_coupon_id` bigint DEFAULT NULL,
  `result_type` varchar(16) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'draw结果类型',
  `result_value` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'draw结果展示值',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`member_id`,`request_id`),
  KEY `idx_biz_mini_idempotency_action` (`member_id`,`action_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='小程序营销幂等记录';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `biz_mini_idempotency`
--

LOCK TABLES `biz_mini_idempotency` WRITE;
/*!40000 ALTER TABLE `biz_mini_idempotency` DISABLE KEYS */;
INSERT INTO `biz_mini_idempotency` VALUES (1,'draw-local-0001','draw','973464f6e6790812ba181d54d18a5d2a243cf43e97dc3d37c452d55c1a00a5d1',1,NULL,'goods',NULL,'2026-10-03 22:23:08');
/*!40000 ALTER TABLE `biz_mini_idempotency` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `biz_order`
--

DROP TABLE IF EXISTS `biz_order`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `biz_order` (
  `order_id` bigint NOT NULL AUTO_INCREMENT,
  `order_no` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL,
  `member_id` bigint NOT NULL,
  `address_snapshot` json NOT NULL COMMENT '鍦板潃蹇?収',
  `fulfillment_mode` varchar(16) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'delivery',
  `pickup_store_id` bigint DEFAULT NULL,
  `subtotal_cent` int NOT NULL,
  `discount_cent` int NOT NULL DEFAULT '0',
  `shipping_cent` int NOT NULL DEFAULT '0',
  `total_cent` int NOT NULL,
  `coupon_id` bigint DEFAULT NULL,
  `request_id` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '鏈嶅姟绔?箓绛夐敭',
  `cancel_request_id` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `receive_request_id` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `status` varchar(16) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'pending' COMMENT 'pending/paid/shipping/receiving/complete/cancelled/refunded',
  `paid_at` datetime DEFAULT NULL,
  `wechat_transaction_id` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `cancelled_at` datetime DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`order_id`),
  UNIQUE KEY `uk_biz_order_no` (`order_no`),
  UNIQUE KEY `uk_biz_order_request` (`member_id`,`request_id`),
  UNIQUE KEY `uk_biz_order_wechat_transaction` (`wechat_transaction_id`),
  KEY `idx_biz_order_member_status` (`member_id`,`status`)
) ENGINE=InnoDB AUTO_INCREMENT=34 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='鍟嗗煄璁㈠崟';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `biz_order`
--

LOCK TABLES `biz_order` WRITE;
/*!40000 ALTER TABLE `biz_order` DISABLE KEYS */;
INSERT INTO `biz_order` VALUES (1,'JSCA3A4FB6C3644D5AB5AA',1,'{\"city\": \"\", \"detail\": \"港北区金色酒业体验店示例地址\", \"district\": \"\", \"province\": \"广西壮族自治区 贵港市 港北区\", \"address_id\": 1, \"receiver_name\": \"张先生\", \"receiver_phone\": \"13800002688\"}','delivery',NULL,39800,0,1200,41000,NULL,'cart-order-1791036696384-8tqfa',NULL,NULL,'cancelled',NULL,NULL,'2026-10-03 22:16:59','2026-10-03 22:11:36','2026-10-03 22:16:59'),(2,'JS21911553E42C4CD3A7D2',1,'{\"city\": \"\", \"detail\": \"港北区金色酒业体验店示例地址\", \"district\": \"\", \"province\": \"广西壮族自治区 贵港市 港北区\", \"address_id\": 1, \"receiver_name\": \"张先生\", \"receiver_phone\": \"13800002688\"}','delivery',NULL,39800,1500,1200,39500,1,'cart-flow-20261004001',NULL,NULL,'cancelled',NULL,NULL,'2026-10-04 08:49:25','2026-10-04 08:49:25','2026-10-04 08:49:25'),(3,'JSE1A88B7C8C7648EC99FF',1,'{\"city\": \"\", \"detail\": \"港北区金色酒业体验店示例地址\", \"district\": \"\", \"province\": \"广西壮族自治区 贵港市 港北区\", \"address_id\": 1, \"receiver_name\": \"张先生\", \"receiver_phone\": \"13800002688\"}','delivery',NULL,39800,0,1200,41000,NULL,'flow-cart-20261004',NULL,NULL,'cancelled','2026-10-04 12:10:04',NULL,'2026-10-04 11:51:44','2026-10-04 11:51:44','2026-10-04 12:15:39'),(13,'JS9077C2DBB97440D9BA36',1,'{\"id\": \"1\", \"name\": \"张先生\", \"phone\": \"13800002688\", \"detail\": \"港北区金色酒业体验店示例地址\", \"region\": \"广西壮族自治区 贵港市 港北区\", \"isDefault\": true}','delivery',NULL,59800,1500,0,58300,1,'checkout-1791127044180-6rfst',NULL,NULL,'cancelled',NULL,NULL,'2026-10-04 23:48:47','2026-10-04 23:17:34','2026-10-04 23:48:47');
/*!40000 ALTER TABLE `biz_order` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `biz_order_delivery`
--

DROP TABLE IF EXISTS `biz_order_delivery`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `biz_order_delivery` (
  `delivery_id` bigint NOT NULL AUTO_INCREMENT,
  `order_id` bigint NOT NULL,
  `company` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '',
  `tracking_no` varchar(128) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '',
  `status` varchar(16) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'pending',
  `last_trace` json DEFAULT NULL,
  `shipped_at` datetime DEFAULT NULL,
  `received_at` datetime DEFAULT NULL,
  PRIMARY KEY (`delivery_id`),
  UNIQUE KEY `uk_biz_order_delivery_order` (`order_id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='订单物流';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `biz_order_delivery`
--

LOCK TABLES `biz_order_delivery` WRITE;
/*!40000 ALTER TABLE `biz_order_delivery` DISABLE KEYS */;
/*!40000 ALTER TABLE `biz_order_delivery` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `biz_order_idempotency`
--

DROP TABLE IF EXISTS `biz_order_idempotency`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `biz_order_idempotency` (
  `member_id` bigint NOT NULL,
  `request_id` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL,
  `payload_hash` char(64) COLLATE utf8mb4_unicode_ci NOT NULL,
  `order_id` bigint DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`member_id`,`request_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='订单幂等记录';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `biz_order_idempotency`
--

LOCK TABLES `biz_order_idempotency` WRITE;
/*!40000 ALTER TABLE `biz_order_idempotency` DISABLE KEYS */;
INSERT INTO `biz_order_idempotency` VALUES (1,'cart-flow-20261004001','98bfda277596a14b73f2f48726d841ed4a5b62d2d7f3f5e386ef497acb17cc88',2,'2026-10-04 08:49:25'),(1,'cart-order-1791036696384-8tqfa','646f69de1c20827b1a1b0456201908760c1dbe9633e0ec33b8bd63cc7af0980c',1,'2026-10-03 22:11:36'),(1,'checkout-1791127044180-6rfst','7d4a287f4d57eae02aad088a0489b9ba9c8d6bae7eb4b4245fd41ea9fdf1745e',13,'2026-10-04 23:17:34'),(1,'flow-cart-20261004','86d84bca6e11c840f68c18f3f53655573a4d9c5a7f40eeb4153aa9dc420543f3',3,'2026-10-04 11:51:44');
/*!40000 ALTER TABLE `biz_order_idempotency` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `biz_order_item`
--

DROP TABLE IF EXISTS `biz_order_item`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `biz_order_item` (
  `order_item_id` bigint NOT NULL AUTO_INCREMENT,
  `order_id` bigint NOT NULL,
  `product_id` bigint NOT NULL,
  `sku_code` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '',
  `product_name` varchar(128) COLLATE utf8mb4_unicode_ci NOT NULL,
  `product_spec` varchar(128) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '',
  `image_url` varchar(512) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '',
  `unit_price_cent` int NOT NULL,
  `quantity` int NOT NULL,
  `total_cent` int NOT NULL,
  PRIMARY KEY (`order_item_id`),
  KEY `idx_biz_order_item_order` (`order_id`)
) ENGINE=InnoDB AUTO_INCREMENT=21 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='璁㈠崟鍟嗗搧蹇?収';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `biz_order_item`
--

LOCK TABLES `biz_order_item` WRITE;
/*!40000 ALTER TABLE `biz_order_item` DISABLE KEYS */;
INSERT INTO `biz_order_item` VALUES (1,1,1,'','典藏酱香','53度 500mL','/mini-assets/design/product-classic.jpg',39800,1,39800),(2,2,1,'','典藏酱香','53度 500mL','/mini-assets/design/product-classic.jpg',39800,1,39800),(3,3,1,'','典藏酱香','53度 500mL','/mini-assets/design/product-classic.jpg',39800,1,39800),(10,13,3,'','双瓶礼盒','53度 500mL × 2','/mini-assets/design/product-double.jpg',59800,1,59800);
/*!40000 ALTER TABLE `biz_order_item` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `biz_order_pay_request`
--

DROP TABLE IF EXISTS `biz_order_pay_request`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `biz_order_pay_request` (
  `pay_request_id` bigint NOT NULL AUTO_INCREMENT,
  `member_id` bigint NOT NULL,
  `order_no` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL,
  `request_id` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL,
  `response_json` text COLLATE utf8mb4_unicode_ci NOT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`pay_request_id`),
  UNIQUE KEY `uk_biz_order_pay_request` (`member_id`,`request_id`),
  KEY `idx_biz_order_pay_order` (`member_id`,`order_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='寰?俊鏀?粯棰勪笅鍗曞箓绛夎?褰';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `biz_order_pay_request`
--

LOCK TABLES `biz_order_pay_request` WRITE;
/*!40000 ALTER TABLE `biz_order_pay_request` DISABLE KEYS */;
/*!40000 ALTER TABLE `biz_order_pay_request` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `biz_order_pickup_redemption`
--

DROP TABLE IF EXISTS `biz_order_pickup_redemption`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `biz_order_pickup_redemption` (
  `pickup_redemption_id` bigint NOT NULL AUTO_INCREMENT,
  `order_id` bigint NOT NULL,
  `order_no` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL,
  `store_id` bigint NOT NULL,
  `verifier_member_id` bigint NOT NULL,
  `request_id` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL,
  `status` varchar(16) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'confirmed',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`pickup_redemption_id`),
  UNIQUE KEY `uk_biz_order_pickup_order` (`order_id`),
  UNIQUE KEY `uk_biz_order_pickup_request` (`verifier_member_id`,`request_id`),
  KEY `idx_biz_order_pickup_store_time` (`store_id`,`create_time`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='商城门店自提核销';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `biz_order_pickup_redemption`
--

LOCK TABLES `biz_order_pickup_redemption` WRITE;
/*!40000 ALTER TABLE `biz_order_pickup_redemption` DISABLE KEYS */;
/*!40000 ALTER TABLE `biz_order_pickup_redemption` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `biz_order_refund`
--

DROP TABLE IF EXISTS `biz_order_refund`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `biz_order_refund` (
  `refund_id` bigint NOT NULL AUTO_INCREMENT,
  `order_id` bigint NOT NULL,
  `member_id` bigint NOT NULL,
  `refund_no` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL,
  `request_id` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `amount_cent` int NOT NULL,
  `reason` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '',
  `status` varchar(16) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'pending' COMMENT 'pending/approved/rejected/refunded',
  `channel_status` varchar(32) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `channel_refund_id` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `manual_payment_reference` varchar(128) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `manual_paid_at` datetime DEFAULT NULL,
  `manual_verified_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`refund_id`),
  UNIQUE KEY `uk_biz_order_refund_no` (`refund_no`),
  UNIQUE KEY `uk_biz_order_refund_request` (`member_id`,`request_id`),
  KEY `idx_biz_order_refund_order` (`order_id`)
) ENGINE=InnoDB AUTO_INCREMENT=14 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='订单售后退款';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `biz_order_refund`
--

LOCK TABLES `biz_order_refund` WRITE;
/*!40000 ALTER TABLE `biz_order_refund` DISABLE KEYS */;
/*!40000 ALTER TABLE `biz_order_refund` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `biz_point_account`
--

DROP TABLE IF EXISTS `biz_point_account`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `biz_point_account` (
  `member_id` bigint NOT NULL,
  `balance` int NOT NULL DEFAULT '0',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`member_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='浼氬憳绉?垎璐︽埛';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `biz_point_account`
--

LOCK TABLES `biz_point_account` WRITE;
/*!40000 ALTER TABLE `biz_point_account` DISABLE KEYS */;
INSERT INTO `biz_point_account` VALUES (1,10,'2026-10-05 09:54:14');
/*!40000 ALTER TABLE `biz_point_account` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `biz_point_record`
--

DROP TABLE IF EXISTS `biz_point_record`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `biz_point_record` (
  `point_record_id` bigint NOT NULL AUTO_INCREMENT,
  `member_id` bigint NOT NULL,
  `change_amount` int NOT NULL,
  `balance_after` int NOT NULL,
  `source_type` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL,
  `source_id` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '',
  `title` varchar(128) COLLATE utf8mb4_unicode_ci NOT NULL,
  `expires_at` datetime DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`point_record_id`),
  KEY `idx_biz_point_record_member_time` (`member_id`,`create_time`)
) ENGINE=InnoDB AUTO_INCREMENT=172 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='绉?垎娴佹按';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `biz_point_record`
--

LOCK TABLES `biz_point_record` WRITE;
/*!40000 ALTER TABLE `biz_point_record` DISABLE KEYS */;
INSERT INTO `biz_point_record` VALUES (1,1,10,10,'daily_sign','2026-10-03','每日签到',NULL,'2026-10-03 22:19:03');
/*!40000 ALTER TABLE `biz_point_record` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `biz_point_rule`
--

DROP TABLE IF EXISTS `biz_point_rule`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `biz_point_rule` (
  `rule_id` bigint NOT NULL AUTO_INCREMENT,
  `rule_code` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL,
  `rule_name` varchar(128) COLLATE utf8mb4_unicode_ci NOT NULL,
  `source_type` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL,
  `points` int NOT NULL DEFAULT '0',
  `daily_limit` int NOT NULL DEFAULT '-1',
  `validity_days` int NOT NULL DEFAULT '0',
  `status` char(1) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '0',
  `rule_json` json DEFAULT NULL,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`rule_id`),
  UNIQUE KEY `uk_biz_point_rule_code` (`rule_code`)
) ENGINE=InnoDB AUTO_INCREMENT=26 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='积分获取及消费规则';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `biz_point_rule`
--

LOCK TABLES `biz_point_rule` WRITE;
/*!40000 ALTER TABLE `biz_point_rule` DISABLE KEYS */;
INSERT INTO `biz_point_rule` VALUES (1,'order_paid','商城消费积分','order_paid',1,-1,0,'0','{\"per_cent\": 1}','2026-10-02 23:51:19'),(2,'invite_register','邀请注册','invite_register',100,20,0,'0','{}','2026-10-02 23:51:19'),(3,'daily_sign','每日签到','daily_sign',10,1,0,'0','{}','2026-10-02 23:51:19'),(25,'ranking','销冠排行榜奖励','ranking',0,-1,0,'0','{}','2026-10-05 22:20:39');
/*!40000 ALTER TABLE `biz_point_rule` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `biz_product`
--

DROP TABLE IF EXISTS `biz_product`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `biz_product` (
  `product_id` bigint NOT NULL AUTO_INCREMENT COMMENT '鍟嗗搧ID',
  `product_code` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '鍟嗗搧缂栫爜',
  `product_name` varchar(128) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '鍟嗗搧鍚嶇О',
  `category` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '' COMMENT '鍒嗙被',
  `spec` varchar(128) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '' COMMENT '瑙勬牸',
  `series` varchar(128) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '' COMMENT '绯诲垪',
  `description` text COLLATE utf8mb4_unicode_ci COMMENT '鍟嗗搧鎻忚堪',
  `image_url` varchar(512) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '' COMMENT '涓诲浘鍦板潃',
  `price_cent` int NOT NULL COMMENT '鍞?环锛屽崟浣嶅垎',
  `original_price_cent` int NOT NULL DEFAULT '0' COMMENT '鍒掔嚎浠凤紝鍗曚綅鍒',
  `stock` int NOT NULL DEFAULT '0' COMMENT '鍙?敭搴撳瓨',
  `sales` int NOT NULL DEFAULT '0' COMMENT '閿?噺鍩烘暟',
  `sort_num` int NOT NULL DEFAULT '0' COMMENT '鎺掑簭',
  `status` char(1) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '0' COMMENT '鐘舵?锛?涓婃灦 1涓嬫灦锛',
  `create_by` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_by` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `remark` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`product_id`),
  UNIQUE KEY `uk_biz_product_code` (`product_code`),
  KEY `idx_biz_product_status_sort` (`status`,`sort_num`)
) ENGINE=InnoDB AUTO_INCREMENT=17 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='閰掔被鍟嗗搧';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `biz_product`
--

LOCK TABLES `biz_product` WRITE;
/*!40000 ALTER TABLE `biz_product` DISABLE KEYS */;
INSERT INTO `biz_product` VALUES (1,'classic','典藏酱香','酱香','53度 500mL','金色典藏系列','53度酱香型 500mL','/mini-assets/design/product-classic.jpg',39800,49800,99,1286,10,'0','seed','2026-10-02 23:45:52','seed','2026-10-05 09:42:13','鍓嶇?婕旂ず鍒濆?鏁版嵁'),(2,'mellow','经典浓香','浓香','52度 500mL','金色经典系列','52度浓香型 500mL','/mini-assets/design/product-mellow.jpg',26800,32800,86,965,20,'0','seed','2026-10-02 23:45:52','seed','2026-10-03 19:09:09','鍓嶇?婕旂ず鍒濆?鏁版嵁'),(3,'double','双瓶礼盒','礼盒','53度 500mL × 2','金色礼赠系列','53度酱香型 双瓶礼盒','/mini-assets/design/product-double.jpg',59800,69800,50,658,30,'0','seed','2026-10-02 23:45:52','seed','2026-10-04 23:48:47','鍓嶇?婕旂ず鍒濆?鏁版嵁'),(4,'gift','珍藏礼盒','礼盒','53度 500mL','金色礼赠系列','53度酱香型 珍藏礼盒','/mini-assets/design/product-gift.jpg',89800,99800,30,328,40,'0','seed','2026-10-02 23:45:52','seed','2026-10-03 19:09:09','鍓嶇?婕旂ず鍒濆?鏁版嵁');
/*!40000 ALTER TABLE `biz_product` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `biz_product_sku`
--

DROP TABLE IF EXISTS `biz_product_sku`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `biz_product_sku` (
  `sku_id` bigint NOT NULL AUTO_INCREMENT,
  `product_id` bigint NOT NULL,
  `sku_code` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL,
  `sku_name` varchar(128) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '',
  `spec_json` json DEFAULT NULL,
  `external_sku_id` varchar(128) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '',
  `price_cent` int NOT NULL,
  `stock` int NOT NULL DEFAULT '0',
  `status` char(1) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '0',
  `video_sync_status` varchar(16) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'pending',
  `video_last_error` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '',
  `video_synced_at` datetime DEFAULT NULL,
  `video_retry_at` datetime DEFAULT NULL,
  `video_retry_count` int NOT NULL DEFAULT '0',
  PRIMARY KEY (`sku_id`),
  UNIQUE KEY `uk_biz_product_sku_code` (`sku_code`),
  KEY `idx_biz_product_sku_product` (`product_id`,`status`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='商品规格库存';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `biz_product_sku`
--

LOCK TABLES `biz_product_sku` WRITE;
/*!40000 ALTER TABLE `biz_product_sku` DISABLE KEYS */;
/*!40000 ALTER TABLE `biz_product_sku` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `biz_production_verification`
--

DROP TABLE IF EXISTS `biz_production_verification`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `biz_production_verification` (
  `verification_id` bigint NOT NULL AUTO_INCREMENT,
  `check_key` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL,
  `config_fingerprint` char(64) COLLATE utf8mb4_unicode_ci NOT NULL,
  `evidence` varchar(1000) COLLATE utf8mb4_unicode_ci NOT NULL,
  `verified_by` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL,
  `verified_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`verification_id`),
  KEY `idx_production_verification_check` (`check_key`,`verification_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='正式平台上线验收证据';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `biz_production_verification`
--

LOCK TABLES `biz_production_verification` WRITE;
/*!40000 ALTER TABLE `biz_production_verification` DISABLE KEYS */;
/*!40000 ALTER TABLE `biz_production_verification` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `biz_promotion_config`
--

DROP TABLE IF EXISTS `biz_promotion_config`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `biz_promotion_config` (
  `config_id` bigint NOT NULL AUTO_INCREMENT,
  `max_level` int NOT NULL DEFAULT '3',
  `customer_rate_bp` int NOT NULL DEFAULT '1000',
  `staff_rate_bp` int NOT NULL DEFAULT '1500',
  `level_rates_json` json NOT NULL,
  `settlement_days` int NOT NULL DEFAULT '7',
  `min_withdraw_cent` int NOT NULL DEFAULT '1000',
  `refund_policy` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'reverse',
  `status` char(1) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '0',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`config_id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='分销佣金规则';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `biz_promotion_config`
--

LOCK TABLES `biz_promotion_config` WRITE;
/*!40000 ALTER TABLE `biz_promotion_config` DISABLE KEYS */;
INSERT INTO `biz_promotion_config` VALUES (1,3,1000,1500,'[1000, 500, 200]',7,1000,'reverse','0','2026-10-02 23:51:19');
/*!40000 ALTER TABLE `biz_promotion_config` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `biz_promotion_record`
--

DROP TABLE IF EXISTS `biz_promotion_record`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `biz_promotion_record` (
  `promotion_record_id` bigint NOT NULL AUTO_INCREMENT,
  `member_id` bigint NOT NULL,
  `order_id` bigint DEFAULT NULL,
  `title` varchar(128) COLLATE utf8mb4_unicode_ci NOT NULL,
  `amount_cent` int NOT NULL DEFAULT '0',
  `status` varchar(16) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'pending' COMMENT 'pending/settled/processing/paid/closed',
  `original_amount_cent` int DEFAULT NULL,
  `refund_adjustment_cent` int NOT NULL DEFAULT '0',
  `refund_hold_status` varchar(16) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `refund_recovery_required` tinyint(1) NOT NULL DEFAULT '0',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`promotion_record_id`),
  KEY `idx_biz_promotion_record_member` (`member_id`,`create_time`)
) ENGINE=InnoDB AUTO_INCREMENT=30 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='推广佣金记录';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `biz_promotion_record`
--

LOCK TABLES `biz_promotion_record` WRITE;
/*!40000 ALTER TABLE `biz_promotion_record` DISABLE KEYS */;
/*!40000 ALTER TABLE `biz_promotion_record` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `biz_promotion_relation`
--

DROP TABLE IF EXISTS `biz_promotion_relation`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `biz_promotion_relation` (
  `relation_id` bigint NOT NULL AUTO_INCREMENT,
  `member_id` bigint NOT NULL,
  `inviter_member_id` bigint DEFAULT NULL,
  `level_no` tinyint NOT NULL DEFAULT '1',
  `invite_code` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL,
  `status` char(1) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '0' COMMENT '0有效 1停用',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`relation_id`),
  UNIQUE KEY `uk_biz_promotion_member` (`member_id`),
  UNIQUE KEY `uk_biz_promotion_code` (`invite_code`),
  KEY `idx_biz_promotion_inviter` (`inviter_member_id`)
) ENGINE=InnoDB AUTO_INCREMENT=948 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='会员推广关系';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `biz_promotion_relation`
--

LOCK TABLES `biz_promotion_relation` WRITE;
/*!40000 ALTER TABLE `biz_promotion_relation` DISABLE KEYS */;
INSERT INTO `biz_promotion_relation` VALUES (1,1,NULL,1,'JS1PROMO','0','2026-10-03 21:54:53'),(95,79,NULL,1,'JS79PROMO','0','2026-10-04 11:23:07');
/*!40000 ALTER TABLE `biz_promotion_relation` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `biz_promotion_settlement`
--

DROP TABLE IF EXISTS `biz_promotion_settlement`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `biz_promotion_settlement` (
  `settlement_id` bigint NOT NULL AUTO_INCREMENT,
  `member_id` bigint NOT NULL,
  `amount_cent` int NOT NULL,
  `status` varchar(16) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'pending' COMMENT 'pending/processing/paid/failed',
  `request_id` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `payment_reference` varchar(128) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '线下转账凭证号',
  `failure_reason` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '提现退回原因',
  `paid_at` datetime DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`settlement_id`),
  UNIQUE KEY `uk_biz_promotion_settlement_request` (`member_id`,`request_id`),
  KEY `idx_biz_promotion_settlement_member` (`member_id`,`status`)
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='佣金提现结算';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `biz_promotion_settlement`
--

LOCK TABLES `biz_promotion_settlement` WRITE;
/*!40000 ALTER TABLE `biz_promotion_settlement` DISABLE KEYS */;
/*!40000 ALTER TABLE `biz_promotion_settlement` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `biz_promotion_settlement_item`
--

DROP TABLE IF EXISTS `biz_promotion_settlement_item`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `biz_promotion_settlement_item` (
  `settlement_id` bigint NOT NULL,
  `promotion_record_id` bigint NOT NULL,
  PRIMARY KEY (`settlement_id`,`promotion_record_id`),
  KEY `idx_biz_settlement_record` (`promotion_record_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='提现单与佣金流水关联';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `biz_promotion_settlement_item`
--

LOCK TABLES `biz_promotion_settlement_item` WRITE;
/*!40000 ALTER TABLE `biz_promotion_settlement_item` DISABLE KEYS */;
/*!40000 ALTER TABLE `biz_promotion_settlement_item` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `biz_ranking_config`
--

DROP TABLE IF EXISTS `biz_ranking_config`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `biz_ranking_config` (
  `config_id` bigint NOT NULL AUTO_INCREMENT,
  `period_type` varchar(16) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'month',
  `reward_json` json NOT NULL,
  `start_time` datetime DEFAULT NULL,
  `end_time` datetime DEFAULT NULL,
  `status` char(1) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '0',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`config_id`)
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='销冠天梯配置';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `biz_ranking_config`
--

LOCK TABLES `biz_ranking_config` WRITE;
/*!40000 ALTER TABLE `biz_ranking_config` DISABLE KEYS */;
INSERT INTO `biz_ranking_config` VALUES (1,'month','[{\"rank\": 1, \"points\": 1000, \"subsidyCent\": 50000}, {\"rank\": 2, \"points\": 500, \"subsidyCent\": 20000}, {\"rank\": 3, \"points\": 300, \"subsidyCent\": 10000}]',NULL,NULL,'0','2026-10-02 23:51:19');
/*!40000 ALTER TABLE `biz_ranking_config` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `biz_ranking_period`
--

DROP TABLE IF EXISTS `biz_ranking_period`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `biz_ranking_period` (
  `period_id` bigint NOT NULL AUTO_INCREMENT,
  `config_id` bigint NOT NULL,
  `period_start` datetime NOT NULL,
  `period_end` datetime NOT NULL,
  `period_type` varchar(16) COLLATE utf8mb4_unicode_ci NOT NULL,
  `reward_json` json NOT NULL,
  `result_json` json NOT NULL,
  `reward_count` int NOT NULL DEFAULT '0',
  `create_by` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`period_id`),
  UNIQUE KEY `uk_ranking_period` (`config_id`,`period_start`)
) ENGINE=InnoDB AUTO_INCREMENT=10 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='排行榜已结算周期与排名奖励快照';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `biz_ranking_period`
--

LOCK TABLES `biz_ranking_period` WRITE;
/*!40000 ALTER TABLE `biz_ranking_period` DISABLE KEYS */;
/*!40000 ALTER TABLE `biz_ranking_period` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `biz_ranking_reward`
--

DROP TABLE IF EXISTS `biz_ranking_reward`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `biz_ranking_reward` (
  `reward_id` bigint NOT NULL AUTO_INCREMENT,
  `config_id` bigint NOT NULL,
  `period_start` datetime NOT NULL,
  `period_end` datetime NOT NULL,
  `member_id` bigint NOT NULL,
  `rank_no` int NOT NULL,
  `points` int NOT NULL DEFAULT '0',
  `subsidy_cent` int NOT NULL DEFAULT '0',
  `status` varchar(16) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'pending',
  `issue_reference` varchar(128) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `issued_at` datetime DEFAULT NULL,
  PRIMARY KEY (`reward_id`),
  UNIQUE KEY `uk_biz_ranking_reward_period_member` (`config_id`,`period_start`,`member_id`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='排行榜奖励发放';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `biz_ranking_reward`
--

LOCK TABLES `biz_ranking_reward` WRITE;
/*!40000 ALTER TABLE `biz_ranking_reward` DISABLE KEYS */;
/*!40000 ALTER TABLE `biz_ranking_reward` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `biz_redemption`
--

DROP TABLE IF EXISTS `biz_redemption`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `biz_redemption` (
  `redemption_id` bigint NOT NULL AUTO_INCREMENT,
  `reward_id` bigint NOT NULL,
  `member_id` bigint NOT NULL,
  `store_id` bigint NOT NULL,
  `verifier_member_id` bigint NOT NULL,
  `status` varchar(16) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'confirmed',
  `request_id` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`redemption_id`),
  UNIQUE KEY `uk_biz_redemption_reward` (`reward_id`),
  UNIQUE KEY `uk_biz_redemption_request` (`verifier_member_id`,`request_id`),
  KEY `idx_biz_redemption_store_time` (`store_id`,`create_time`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='闂ㄥ簵鏍搁攢璁板綍';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `biz_redemption`
--

LOCK TABLES `biz_redemption` WRITE;
/*!40000 ALTER TABLE `biz_redemption` DISABLE KEYS */;
INSERT INTO `biz_redemption` VALUES (1,1,1,1,1,'confirmed','verify-test-20261003-01','2026-10-03 23:04:16');
/*!40000 ALTER TABLE `biz_redemption` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `biz_report_export`
--

DROP TABLE IF EXISTS `biz_report_export`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `biz_report_export` (
  `export_id` bigint NOT NULL AUTO_INCREMENT,
  `report_type` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL,
  `filters_json` json DEFAULT NULL,
  `file_name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `row_count` int NOT NULL DEFAULT '0',
  `create_by` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`export_id`)
) ENGINE=InnoDB AUTO_INCREMENT=125 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='财务报表导出记录';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `biz_report_export`
--

LOCK TABLES `biz_report_export` WRITE;
/*!40000 ALTER TABLE `biz_report_export` DISABLE KEYS */;
INSERT INTO `biz_report_export` VALUES (1,'videoRefunds','{\"toDate\": \"2026-12-31\", \"fromDate\": \"2026-01-01\"}','videoRefunds-report-2026-01-01_2026-12-31.csv',0,'admin','2026-10-03 20:14:52'),(2,'orders','{\"toDate\": \"\", \"fromDate\": \"\"}','orders-report-all_all.csv',1,'admin','2026-10-04 07:42:28'),(3,'commissions','{\"toDate\": \"\", \"fromDate\": \"\"}','commissions-report-all_all.csv',0,'admin','2026-10-04 07:42:28'),(4,'settlements','{\"toDate\": \"\", \"fromDate\": \"\"}','settlements-report-all_all.csv',0,'admin','2026-10-04 07:42:28'),(5,'lottery','{\"toDate\": \"\", \"fromDate\": \"\"}','lottery-report-all_all.csv',1,'admin','2026-10-04 07:42:28'),(6,'videoRefunds','{\"toDate\": \"\", \"fromDate\": \"\"}','videoRefunds-report-all_all.csv',0,'admin','2026-10-04 07:42:28'),(7,'orders','{\"toDate\": \"\", \"fromDate\": \"\"}','orders-report-all_all.csv',1,'admin','2026-10-04 07:44:48'),(8,'commissions','{\"toDate\": \"\", \"fromDate\": \"\"}','commissions-report-all_all.csv',0,'admin','2026-10-04 07:44:48'),(9,'settlements','{\"toDate\": \"\", \"fromDate\": \"\"}','settlements-report-all_all.csv',0,'admin','2026-10-04 07:44:48'),(10,'lottery','{\"toDate\": \"\", \"fromDate\": \"\"}','lottery-report-all_all.csv',1,'admin','2026-10-04 07:44:48'),(11,'redemptions','{\"toDate\": \"\", \"fromDate\": \"\"}','redemptions-report-all_all.csv',1,'admin','2026-10-04 07:44:48'),(12,'videoRefunds','{\"toDate\": \"\", \"fromDate\": \"\"}','videoRefunds-report-all_all.csv',0,'admin','2026-10-04 07:44:48'),(13,'redemptions','{\"toDate\": \"2026-10-04\", \"fromDate\": \"2026-10-01\"}','redemptions-report-2026-10-01_2026-10-04.csv',1,'admin','2026-10-04 07:44:58'),(14,'orders','{\"toDate\": \"2026-12-31\", \"fromDate\": \"2026-01-01\"}','orders-report-2026-01-01_2026-12-31.csv',1,'admin','2026-10-04 08:08:52'),(15,'orders','{\"toDate\": \"\", \"fromDate\": \"\"}','orders-report-all_all.csv',1,'admin','2026-10-04 08:23:23'),(16,'commissions','{\"toDate\": \"\", \"fromDate\": \"\"}','commissions-report-all_all.csv',0,'admin','2026-10-04 08:23:23'),(17,'settlements','{\"toDate\": \"\", \"fromDate\": \"\"}','settlements-report-all_all.csv',0,'admin','2026-10-04 08:23:23'),(18,'lottery','{\"toDate\": \"\", \"fromDate\": \"\"}','lottery-report-all_all.csv',1,'admin','2026-10-04 08:23:23'),(19,'redemptions','{\"toDate\": \"\", \"fromDate\": \"\"}','redemptions-report-all_all.csv',1,'admin','2026-10-04 08:23:23'),(20,'videoRefunds','{\"toDate\": \"\", \"fromDate\": \"\"}','videoRefunds-report-all_all.csv',0,'admin','2026-10-04 08:23:23'),(21,'orders','{\"toDate\": \"\", \"fromDate\": \"\"}','orders-report-all_all.csv',2,'admin','2026-10-04 11:44:15'),(22,'orders','{\"toDate\": \"2026-12-31\", \"fromDate\": \"2026-01-01\"}','orders-report-2026-01-01_2026-12-31.csv',2,'admin','2026-10-04 11:45:03'),(23,'commissions','{\"toDate\": \"2026-12-31\", \"fromDate\": \"2026-01-01\"}','commissions-report-2026-01-01_2026-12-31.csv',0,'admin','2026-10-04 11:45:03'),(24,'settlements','{\"toDate\": \"2026-12-31\", \"fromDate\": \"2026-01-01\"}','settlements-report-2026-01-01_2026-12-31.csv',0,'admin','2026-10-04 11:45:03'),(25,'lottery','{\"toDate\": \"2026-12-31\", \"fromDate\": \"2026-01-01\"}','lottery-report-2026-01-01_2026-12-31.csv',1,'admin','2026-10-04 11:45:03'),(26,'redemptions','{\"toDate\": \"2026-12-31\", \"fromDate\": \"2026-01-01\"}','redemptions-report-2026-01-01_2026-12-31.csv',1,'admin','2026-10-04 11:45:03'),(27,'videoRefunds','{\"toDate\": \"2026-12-31\", \"fromDate\": \"2026-01-01\"}','videoRefunds-report-2026-01-01_2026-12-31.csv',0,'admin','2026-10-04 11:45:03'),(28,'orders','{\"toDate\": \"2026-12-31\", \"fromDate\": \"2026-01-01\"}','orders-report-2026-01-01_2026-12-31.csv',3,'admin','2026-10-04 21:03:36'),(29,'commissions','{\"toDate\": \"2026-12-31\", \"fromDate\": \"2026-01-01\"}','commissions-report-2026-01-01_2026-12-31.csv',0,'admin','2026-10-04 21:03:36'),(30,'settlements','{\"toDate\": \"2026-12-31\", \"fromDate\": \"2026-01-01\"}','settlements-report-2026-01-01_2026-12-31.csv',0,'admin','2026-10-04 21:03:36'),(31,'lottery','{\"toDate\": \"2026-12-31\", \"fromDate\": \"2026-01-01\"}','lottery-report-2026-01-01_2026-12-31.csv',1,'admin','2026-10-04 21:03:36'),(32,'redemptions','{\"toDate\": \"2026-12-31\", \"fromDate\": \"2026-01-01\"}','redemptions-report-2026-01-01_2026-12-31.csv',1,'admin','2026-10-04 21:03:36'),(33,'videoRefunds','{\"toDate\": \"2026-12-31\", \"fromDate\": \"2026-01-01\"}','videoRefunds-report-2026-01-01_2026-12-31.csv',0,'admin','2026-10-04 21:03:36'),(34,'rankingRewards','{\"toDate\": \"\", \"fromDate\": \"\"}','rankingRewards-report-all_all.csv',0,'admin','2026-10-04 23:48:17'),(35,'rankingRewards','{\"toDate\": \"\", \"fromDate\": \"\"}','rankingRewards-report-all_all.csv',0,'admin','2026-10-04 23:48:28'),(36,'rankingRewards','{\"toDate\": \"\", \"fromDate\": \"\"}','rankingRewards-report-all_all.csv',0,'admin','2026-10-04 23:49:56'),(37,'orders','{\"toDate\": \"2026-12-31\", \"fromDate\": \"2026-01-01\"}','orders-report-2026-01-01_2026-12-31.csv',4,'admin','2026-10-05 08:52:29'),(38,'orders','{\"toDate\": \"2026-10-05\", \"fromDate\": \"2026-10-01\"}','orders-report-2026-10-01_2026-10-05.csv',4,'admin','2026-10-05 09:01:11'),(39,'orders','{\"toDate\": \"2026-12-31\", \"fromDate\": \"2026-01-01\"}','orders-report-2026-01-01_2026-12-31.csv',4,'admin','2026-10-05 10:08:30'),(40,'commissions','{\"toDate\": \"2026-12-31\", \"fromDate\": \"2026-01-01\"}','commissions-report-2026-01-01_2026-12-31.csv',0,'admin','2026-10-05 10:08:30'),(41,'settlements','{\"toDate\": \"2026-12-31\", \"fromDate\": \"2026-01-01\"}','settlements-report-2026-01-01_2026-12-31.csv',0,'admin','2026-10-05 10:08:30'),(42,'lottery','{\"toDate\": \"2026-12-31\", \"fromDate\": \"2026-01-01\"}','lottery-report-2026-01-01_2026-12-31.csv',1,'admin','2026-10-05 10:08:30'),(43,'redemptions','{\"toDate\": \"2026-12-31\", \"fromDate\": \"2026-01-01\"}','redemptions-report-2026-01-01_2026-12-31.csv',1,'admin','2026-10-05 10:08:30'),(44,'orders','{\"toDate\": \"\", \"fromDate\": \"\"}','orders-report-all_all.csv',4,'admin','2026-10-05 10:39:52'),(45,'commissions','{\"toDate\": \"\", \"fromDate\": \"\"}','commissions-report-all_all.csv',0,'admin','2026-10-05 10:39:52'),(46,'lottery','{\"toDate\": \"\", \"fromDate\": \"\"}','lottery-report-all_all.csv',1,'admin','2026-10-05 10:39:52'),(47,'redemptions','{\"toDate\": \"\", \"fromDate\": \"\"}','redemptions-report-all_all.csv',1,'admin','2026-10-05 10:39:52'),(48,'videoRefunds','{\"toDate\": \"\", \"fromDate\": \"\"}','videoRefunds-report-all_all.csv',0,'admin','2026-10-05 10:39:52'),(49,'orders','{\"toDate\": \"2026-10-05\", \"fromDate\": \"2026-10-01\"}','orders-report-2026-10-01_2026-10-05.csv',4,'admin','2026-10-05 10:50:41'),(50,'orders','{\"toDate\": \"2026-10-05\", \"fromDate\": \"2026-10-01\"}','orders-report-2026-10-01_2026-10-05.csv',4,'admin','2026-10-05 10:56:15'),(51,'orders','{\"toDate\": \"2026-10-05\", \"fromDate\": \"2026-10-01\"}','orders-report-2026-10-01_2026-10-05.csv',4,'admin','2026-10-05 11:30:03'),(52,'orders','{\"toDate\": \"2026-10-05\", \"fromDate\": \"2026-10-01\"}','orders-report-2026-10-01_2026-10-05.csv',4,'admin','2026-10-05 11:59:13'),(53,'orders','{\"toDate\": \"2026-10-05\", \"fromDate\": \"2026-10-01\"}','orders-report-2026-10-01_2026-10-05.csv',4,'admin','2026-10-05 14:24:17'),(54,'orders','{\"toDate\": \"2026-10-05\", \"fromDate\": \"2026-10-01\"}','orders-report-2026-10-01_2026-10-05.csv',4,'admin','2026-10-05 14:24:42'),(55,'settlements','{\"toDate\": \"\", \"fromDate\": \"\"}','settlements-report-all_all.csv',0,'admin','2026-10-05 17:08:05'),(56,'settlements','{\"toDate\": \"\", \"fromDate\": \"\"}','settlements-report-all_all.csv',0,'admin','2026-10-05 17:08:17'),(57,'settlements','{\"toDate\": \"\", \"fromDate\": \"\"}','settlements-report-all_all.csv',1,'admin','2026-10-05 17:08:58'),(58,'settlements','{\"toDate\": \"\", \"fromDate\": \"\"}','settlements-report-all_all.csv',0,'finance','2026-10-05 17:22:18'),(59,'settlements','{\"toDate\": \"\", \"fromDate\": \"\"}','settlements-report-all_all.csv',0,'finance','2026-10-05 17:23:08'),(60,'orders','{\"toDate\": \"\", \"fromDate\": \"\"}','orders-report-all_all.csv',4,'admin','2026-10-05 23:55:21'),(61,'orders','{\"toDate\": \"\", \"fromDate\": \"\"}','orders-report-all_all.csv',4,'admin','2026-10-06 00:02:41'),(62,'orders','{\"toDate\": \"\", \"fromDate\": \"\"}','orders-report-all_all.csv',4,'admin','2026-10-06 00:10:52'),(63,'orders','{\"toDate\": \"\", \"fromDate\": \"\"}','orders-report-all_all.csv',4,'admin','2026-10-06 00:17:00'),(64,'orders','{\"toDate\": \"\", \"fromDate\": \"\"}','orders-report-all_all.csv',4,'admin','2026-10-06 00:25:28'),(65,'orders','{\"toDate\": \"\", \"fromDate\": \"\"}','orders-report-all_all.csv',4,'admin','2026-10-06 00:28:57'),(66,'orders','{\"toDate\": \"\", \"fromDate\": \"\"}','orders-report-all_all.csv',4,'admin','2026-10-06 00:33:02'),(67,'orders','{\"toDate\": \"\", \"fromDate\": \"\"}','orders-report-all_all.csv',4,'admin','2026-10-06 00:46:45'),(68,'orders','{\"toDate\": \"\", \"fromDate\": \"\"}','orders-report-all_all.csv',4,'admin','2026-10-06 00:50:39'),(69,'orders','{\"toDate\": \"\", \"fromDate\": \"\"}','orders-report-all_all.csv',4,'admin','2026-10-06 00:56:07'),(70,'orders','{\"toDate\": \"\", \"fromDate\": \"\"}','orders-report-all_all.csv',4,'admin','2026-10-06 01:01:01'),(71,'orders','{\"toDate\": \"\", \"fromDate\": \"\"}','orders-report-all_all.csv',4,'admin','2026-10-06 01:01:58'),(72,'orders','{\"toDate\": \"\", \"fromDate\": \"\"}','orders-report-all_all.csv',4,'admin','2026-10-06 01:11:06'),(73,'orders','{\"toDate\": \"\", \"fromDate\": \"\"}','orders-report-all_all.csv',4,'admin','2026-10-06 01:15:16'),(74,'orders','{\"toDate\": \"\", \"fromDate\": \"\"}','orders-report-all_all.csv',4,'admin','2026-10-06 01:19:34'),(75,'orders','{\"toDate\": \"\", \"fromDate\": \"\"}','orders-report-all_all.csv',4,'admin','2026-10-06 01:21:53'),(76,'orders','{\"toDate\": \"\", \"fromDate\": \"\"}','orders-report-all_all.csv',4,'admin','2026-10-06 01:22:26'),(77,'orders','{\"toDate\": \"\", \"fromDate\": \"\"}','orders-report-all_all.csv',4,'admin','2026-10-06 01:23:53'),(78,'orders','{\"toDate\": \"\", \"fromDate\": \"\"}','orders-report-all_all.csv',4,'admin','2026-10-06 01:24:54'),(79,'orders','{\"toDate\": \"\", \"fromDate\": \"\"}','orders-report-all_all.csv',4,'admin','2026-10-06 01:36:03'),(80,'orders','{\"toDate\": \"\", \"fromDate\": \"\"}','orders-report-all_all.csv',4,'admin','2026-10-06 01:40:11'),(81,'orders','{\"toDate\": \"\", \"fromDate\": \"\"}','orders-report-all_all.csv',4,'admin','2026-10-06 01:45:25'),(82,'orders','{\"toDate\": \"\", \"fromDate\": \"\"}','orders-report-all_all.csv',4,'admin','2026-10-06 02:17:08'),(83,'orders','{\"toDate\": \"\", \"fromDate\": \"\"}','orders-report-all_all.csv',4,'admin','2026-10-06 02:28:08'),(84,'orders','{\"toDate\": \"\", \"fromDate\": \"\"}','orders-report-all_all.csv',4,'admin','2026-10-06 02:38:59'),(85,'orders','{\"toDate\": \"\", \"fromDate\": \"\"}','orders-report-all_all.csv',4,'admin','2026-10-06 02:47:51'),(86,'orders','{\"toDate\": \"\", \"fromDate\": \"\"}','orders-report-all_all.csv',4,'admin','2026-10-06 02:49:28'),(87,'refunds','{\"toDate\": \"\", \"fromDate\": \"\"}','refunds-report-all_all.csv',0,'admin','2026-10-06 02:56:56'),(88,'refunds','{\"toDate\": \"\", \"fromDate\": \"\"}','refunds-report-all_all.csv',1,'admin','2026-10-06 02:58:07'),(89,'orders','{\"toDate\": \"\", \"fromDate\": \"\"}','orders-report-all_all.csv',4,'admin','2026-10-06 02:58:45'),(90,'refunds','{\"toDate\": \"\", \"fromDate\": \"\"}','refunds-report-all_all.csv',1,'admin','2026-10-06 03:14:07'),(91,'refunds','{\"toDate\": \"\", \"fromDate\": \"\"}','refunds-report-all_all.csv',1,'admin','2026-10-06 03:17:06'),(92,'refunds','{\"toDate\": \"\", \"fromDate\": \"\"}','refunds-report-all_all.csv',1,'admin','2026-10-06 03:17:45'),(93,'orders','{\"toDate\": \"\", \"fromDate\": \"\"}','orders-report-all_all.csv',4,'admin','2026-10-06 03:17:53'),(94,'refunds','{\"toDate\": \"\", \"fromDate\": \"\"}','refunds-report-all_all.csv',0,'admin','2026-10-06 03:17:53'),(95,'orders','{\"toDate\": \"\", \"fromDate\": \"\"}','orders-report-all_all.csv',4,'admin','2026-10-06 03:22:18'),(96,'refunds','{\"toDate\": \"\", \"fromDate\": \"\"}','refunds-report-all_all.csv',0,'admin','2026-10-06 03:22:18'),(97,'orders','{\"toDate\": \"\", \"fromDate\": \"\"}','orders-report-all_all.csv',4,'admin','2026-10-06 03:49:26'),(98,'refunds','{\"toDate\": \"\", \"fromDate\": \"\"}','refunds-report-all_all.csv',0,'admin','2026-10-06 03:49:26'),(99,'orders','{\"toDate\": \"\", \"fromDate\": \"\"}','orders-report-all_all.csv',4,'admin','2026-10-06 04:04:40'),(100,'refunds','{\"toDate\": \"\", \"fromDate\": \"\"}','refunds-report-all_all.csv',0,'admin','2026-10-06 04:04:40'),(101,'orders','{\"toDate\": \"\", \"fromDate\": \"\"}','orders-report-all_all.csv',4,'admin','2026-10-06 04:11:01'),(102,'refunds','{\"toDate\": \"\", \"fromDate\": \"\"}','refunds-report-all_all.csv',0,'admin','2026-10-06 04:11:01'),(103,'orders','{\"toDate\": \"\", \"fromDate\": \"\"}','orders-report-all_all.csv',4,'admin','2026-10-06 04:22:01'),(104,'refunds','{\"toDate\": \"\", \"fromDate\": \"\"}','refunds-report-all_all.csv',0,'admin','2026-10-06 04:22:01'),(105,'orders','{\"toDate\": \"\", \"fromDate\": \"\"}','orders-report-all_all.csv',4,'admin','2026-10-06 04:35:57'),(106,'refunds','{\"toDate\": \"\", \"fromDate\": \"\"}','refunds-report-all_all.csv',0,'admin','2026-10-06 04:35:57'),(107,'orders','{\"toDate\": \"\", \"fromDate\": \"\"}','orders-report-all_all.csv',4,'admin','2026-10-06 04:43:16'),(108,'refunds','{\"toDate\": \"\", \"fromDate\": \"\"}','refunds-report-all_all.csv',0,'admin','2026-10-06 04:43:16'),(109,'orders','{\"toDate\": \"\", \"fromDate\": \"\"}','orders-report-all_all.csv',4,'admin','2026-10-06 05:06:02'),(110,'refunds','{\"toDate\": \"\", \"fromDate\": \"\"}','refunds-report-all_all.csv',0,'admin','2026-10-06 05:06:02'),(111,'orders','{\"toDate\": \"\", \"fromDate\": \"\"}','orders-report-all_all.csv',4,'admin','2026-10-06 05:10:11'),(112,'refunds','{\"toDate\": \"\", \"fromDate\": \"\"}','refunds-report-all_all.csv',0,'admin','2026-10-06 05:10:11'),(113,'orders','{\"toDate\": \"\", \"fromDate\": \"\"}','orders-report-all_all.csv',4,'admin','2026-10-06 05:17:00'),(114,'refunds','{\"toDate\": \"\", \"fromDate\": \"\"}','refunds-report-all_all.csv',0,'admin','2026-10-06 05:17:00'),(115,'orders','{\"toDate\": \"\", \"fromDate\": \"\"}','orders-report-all_all.csv',4,'admin','2026-10-06 05:35:21'),(116,'refunds','{\"toDate\": \"\", \"fromDate\": \"\"}','refunds-report-all_all.csv',0,'admin','2026-10-06 05:35:21'),(117,'orders','{\"toDate\": \"\", \"fromDate\": \"\"}','orders-report-all_all.csv',4,'admin','2026-10-06 05:41:56'),(118,'refunds','{\"toDate\": \"\", \"fromDate\": \"\"}','refunds-report-all_all.csv',0,'admin','2026-10-06 05:41:56'),(119,'orders','{\"toDate\": \"\", \"fromDate\": \"\"}','orders-report-all_all.csv',4,'admin','2026-10-06 05:47:01'),(120,'refunds','{\"toDate\": \"\", \"fromDate\": \"\"}','refunds-report-all_all.csv',0,'admin','2026-10-06 05:47:01'),(121,'orders','{\"toDate\": \"\", \"fromDate\": \"\"}','orders-report-all_all.csv',4,'admin','2026-10-06 05:54:24'),(122,'refunds','{\"toDate\": \"\", \"fromDate\": \"\"}','refunds-report-all_all.csv',0,'admin','2026-10-06 05:54:24'),(123,'orders','{\"toDate\": \"\", \"fromDate\": \"\"}','orders-report-all_all.csv',4,'admin','2026-10-06 06:09:25'),(124,'refunds','{\"toDate\": \"\", \"fromDate\": \"\"}','refunds-report-all_all.csv',0,'admin','2026-10-06 06:09:25');
/*!40000 ALTER TABLE `biz_report_export` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `biz_reward`
--

DROP TABLE IF EXISTS `biz_reward`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `biz_reward` (
  `reward_id` bigint NOT NULL AUTO_INCREMENT,
  `member_id` bigint NOT NULL,
  `reward_type` varchar(16) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'cash/points/coupon/goods',
  `reward_name` varchar(128) COLLATE utf8mb4_unicode_ci NOT NULL,
  `spec` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '' COMMENT '濂栧搧瑙勬牸',
  `amount_cent` int NOT NULL DEFAULT '0',
  `points` int NOT NULL DEFAULT '0',
  `exchange_code` varchar(128) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '',
  `image_url` varchar(512) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '' COMMENT '濂栧搧鍥剧墖鍦板潃',
  `redeem_store_id` bigint DEFAULT NULL,
  `status` varchar(16) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'pending' COMMENT 'pending/processing/received/redeemed/failed/closed',
  `expires_at` datetime DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`reward_id`),
  UNIQUE KEY `uk_biz_reward_exchange_code` (`exchange_code`),
  KEY `idx_biz_reward_member_status` (`member_id`,`status`)
) ENGINE=InnoDB AUTO_INCREMENT=23 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='濂栧搧涓庣孩鍖呮潈鐩';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `biz_reward`
--

LOCK TABLES `biz_reward` WRITE;
/*!40000 ALTER TABLE `biz_reward` DISABLE KEYS */;
INSERT INTO `biz_reward` VALUES (1,1,'goods','品鉴杯','',0,0,'DR-b2d045c758e5428c','/mini-assets/design/cup.jpg',NULL,'redeemed',NULL,'2026-10-03 22:23:08','2026-10-03 23:04:16');
/*!40000 ALTER TABLE `biz_reward` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `biz_reward_transfer`
--

DROP TABLE IF EXISTS `biz_reward_transfer`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `biz_reward_transfer` (
  `transfer_id` bigint NOT NULL AUTO_INCREMENT,
  `reward_id` bigint NOT NULL,
  `member_id` bigint NOT NULL,
  `request_id` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL,
  `out_bill_no` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL,
  `transfer_bill_no` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `amount_cent` int NOT NULL,
  `openid` varchar(128) COLLATE utf8mb4_unicode_ci NOT NULL,
  `status` varchar(24) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'SUBMITTING' COMMENT 'SUBMITTING/ACCEPTED/PROCESSING/WAIT_USER_CONFIRM/SUCCESS/FAIL/CANCELLED/UNKNOWN',
  `package_info` varchar(2048) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `fail_reason` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`transfer_id`),
  UNIQUE KEY `uk_biz_reward_transfer_bill` (`out_bill_no`),
  UNIQUE KEY `uk_biz_reward_transfer_request` (`member_id`,`request_id`),
  KEY `idx_biz_reward_transfer_reward` (`reward_id`,`transfer_id`),
  KEY `idx_biz_reward_transfer_state` (`status`,`update_time`)
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='微信现金奖商户转账尝试';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `biz_reward_transfer`
--

LOCK TABLES `biz_reward_transfer` WRITE;
/*!40000 ALTER TABLE `biz_reward_transfer` DISABLE KEYS */;
/*!40000 ALTER TABLE `biz_reward_transfer` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `biz_shipping_template`
--

DROP TABLE IF EXISTS `biz_shipping_template`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `biz_shipping_template` (
  `template_id` bigint NOT NULL AUTO_INCREMENT,
  `template_name` varchar(128) COLLATE utf8mb4_unicode_ci NOT NULL,
  `free_threshold_cent` int NOT NULL DEFAULT '0',
  `base_fee_cent` int NOT NULL DEFAULT '0',
  `extra_fee_cent` int NOT NULL DEFAULT '0',
  `extra_quantity` int NOT NULL DEFAULT '1',
  `status` char(1) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '0',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`template_id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='运费模板';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `biz_shipping_template`
--

LOCK TABLES `biz_shipping_template` WRITE;
/*!40000 ALTER TABLE `biz_shipping_template` DISABLE KEYS */;
INSERT INTO `biz_shipping_template` VALUES (1,'默认运费',39900,1200,500,1,'0','2026-10-02 23:51:19','2026-10-02 23:51:19');
/*!40000 ALTER TABLE `biz_shipping_template` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `biz_social_campaign`
--

DROP TABLE IF EXISTS `biz_social_campaign`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `biz_social_campaign` (
  `campaign_id` bigint NOT NULL AUTO_INCREMENT,
  `campaign_type` varchar(16) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'vip/group/coupon',
  `title` varchar(128) COLLATE utf8mb4_unicode_ci NOT NULL,
  `content` text COLLATE utf8mb4_unicode_ci NOT NULL,
  `coupon_template_id` bigint DEFAULT NULL,
  `target_json` json DEFAULT NULL,
  `scheduled_at` datetime DEFAULT NULL,
  `sent_count` int NOT NULL DEFAULT '0',
  `status` varchar(16) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'draft',
  `create_by` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`campaign_id`)
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='VIP社群运营活动';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `biz_social_campaign`
--

LOCK TABLES `biz_social_campaign` WRITE;
/*!40000 ALTER TABLE `biz_social_campaign` DISABLE KEYS */;
/*!40000 ALTER TABLE `biz_social_campaign` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `biz_social_delivery`
--

DROP TABLE IF EXISTS `biz_social_delivery`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `biz_social_delivery` (
  `delivery_id` bigint NOT NULL AUTO_INCREMENT,
  `campaign_id` bigint NOT NULL,
  `member_id` bigint NOT NULL,
  `channel` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'subscribe',
  `subscription_id` bigint DEFAULT NULL,
  `status` varchar(16) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'pending',
  `attempt_count` int NOT NULL DEFAULT '0',
  `next_retry_at` datetime DEFAULT NULL,
  `locked_until` datetime DEFAULT NULL,
  `sent_at` datetime DEFAULT NULL,
  `provider_message_id` varchar(128) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '',
  `error_message` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '',
  PRIMARY KEY (`delivery_id`),
  UNIQUE KEY `uk_biz_social_delivery` (`campaign_id`,`member_id`,`channel`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='社群通知投递记录';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `biz_social_delivery`
--

LOCK TABLES `biz_social_delivery` WRITE;
/*!40000 ALTER TABLE `biz_social_delivery` DISABLE KEYS */;
/*!40000 ALTER TABLE `biz_social_delivery` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `biz_social_group_delivery`
--

DROP TABLE IF EXISTS `biz_social_group_delivery`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `biz_social_group_delivery` (
  `delivery_id` bigint NOT NULL AUTO_INCREMENT,
  `campaign_id` bigint NOT NULL,
  `chat_id` varchar(128) COLLATE utf8mb4_unicode_ci NOT NULL,
  `status` varchar(16) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'pending' COMMENT 'pending/sending/retry/sent/failed/unknown',
  `attempt_count` int NOT NULL DEFAULT '0',
  `next_retry_at` datetime DEFAULT NULL,
  `locked_until` datetime DEFAULT NULL,
  `sent_at` datetime DEFAULT NULL,
  `error_message` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '',
  PRIMARY KEY (`delivery_id`),
  UNIQUE KEY `uk_biz_social_group_delivery` (`campaign_id`,`chat_id`),
  KEY `idx_biz_social_group_delivery_status` (`status`,`next_retry_at`,`locked_until`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='企业微信群通知投递记录';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `biz_social_group_delivery`
--

LOCK TABLES `biz_social_group_delivery` WRITE;
/*!40000 ALTER TABLE `biz_social_group_delivery` DISABLE KEYS */;
/*!40000 ALTER TABLE `biz_social_group_delivery` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `biz_social_subscription`
--

DROP TABLE IF EXISTS `biz_social_subscription`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `biz_social_subscription` (
  `subscription_id` bigint NOT NULL AUTO_INCREMENT,
  `member_id` bigint NOT NULL,
  `template_id` varchar(128) COLLATE utf8mb4_unicode_ci NOT NULL,
  `request_id` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `status` varchar(16) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'available' COMMENT 'available/reserved/consumed/rejected',
  `delivery_id` bigint DEFAULT NULL,
  `reserved_until` datetime DEFAULT NULL,
  `granted_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `consumed_at` datetime DEFAULT NULL,
  PRIMARY KEY (`subscription_id`),
  UNIQUE KEY `uk_biz_social_subscription_request` (`member_id`,`request_id`),
  KEY `idx_biz_social_subscription_member` (`member_id`,`template_id`,`status`,`granted_at`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='会员主动授权的一次性订阅消息额度';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `biz_social_subscription`
--

LOCK TABLES `biz_social_subscription` WRITE;
/*!40000 ALTER TABLE `biz_social_subscription` DISABLE KEYS */;
/*!40000 ALTER TABLE `biz_social_subscription` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `biz_store`
--

DROP TABLE IF EXISTS `biz_store`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `biz_store` (
  `store_id` bigint NOT NULL AUTO_INCREMENT COMMENT '闂ㄥ簵ID',
  `store_code` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '闂ㄥ簵缂栫爜',
  `store_name` varchar(128) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '闂ㄥ簵鍚嶇О',
  `city` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '',
  `district` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '',
  `address` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '',
  `phone` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '',
  `latitude` decimal(10,7) DEFAULT NULL,
  `longitude` decimal(10,7) DEFAULT NULL,
  `open_hours` varchar(128) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '',
  `image_url` varchar(512) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '',
  `service_qr` varchar(512) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '',
  `group_qr` varchar(512) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '',
  `dept_id` bigint DEFAULT NULL,
  `sort_num` int NOT NULL DEFAULT '0',
  `status` char(1) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '0' COMMENT '鐘舵?锛?钀ヤ笟 1鍋滅敤锛',
  `create_by` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_by` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `remark` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`store_id`),
  UNIQUE KEY `uk_biz_store_code` (`store_code`),
  KEY `idx_biz_store_status_sort` (`status`,`sort_num`),
  KEY `idx_biz_store_dept` (`dept_id`)
) ENGINE=InnoDB AUTO_INCREMENT=9 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='鍏戞崲闂ㄥ簵';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `biz_store`
--

LOCK TABLES `biz_store` WRITE;
/*!40000 ALTER TABLE `biz_store` DISABLE KEYS */;
INSERT INTO `biz_store` VALUES (1,'guigang','金色酒业体验店','贵港','港北区','贵港市港北区金港大道 888 号金色酒业体验店','13800002688',23.0965000,109.6086000,'09:00 - 21:00','/mini-assets/design/store.jpg','','',101,10,'0','seed','2026-10-02 23:45:52','seed','2026-10-05 09:10:35','本地演示数据，正式上线前请替换为实际门店资料'),(2,'partner','金色酒业合作门店','贵港','港南区','贵港市港南区江南大道 188 号金色酒业合作门店','13800002689',23.0668000,109.5945000,'09:00 - 21:00','/mini-assets/design/store.jpg','','',101,20,'0','seed','2026-10-02 23:45:52','seed','2026-10-05 09:10:35','本地演示数据，正式上线前请替换为实际门店资料');
/*!40000 ALTER TABLE `biz_store` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `biz_video_aftersale`
--

DROP TABLE IF EXISTS `biz_video_aftersale`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `biz_video_aftersale` (
  `aftersale_id` bigint NOT NULL AUTO_INCREMENT,
  `external_aftersale_id` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL,
  `external_order_id` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '',
  `external_status` varchar(48) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '',
  `aftersale_type` varchar(24) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '',
  `refund_amount_cent` bigint NOT NULL DEFAULT '0',
  `created_at` datetime DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL,
  `last_synced_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`aftersale_id`),
  UNIQUE KEY `uk_biz_video_aftersale_external` (`external_aftersale_id`),
  KEY `idx_biz_video_aftersale_status_time` (`external_status`,`updated_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='微信小店售后单脱敏镜像';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `biz_video_aftersale`
--

LOCK TABLES `biz_video_aftersale` WRITE;
/*!40000 ALTER TABLE `biz_video_aftersale` DISABLE KEYS */;
/*!40000 ALTER TABLE `biz_video_aftersale` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `biz_video_aftersale_sync_state`
--

DROP TABLE IF EXISTS `biz_video_aftersale_sync_state`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `biz_video_aftersale_sync_state` (
  `sync_key` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL,
  `completed_through` bigint NOT NULL DEFAULT '0',
  `window_start` bigint NOT NULL DEFAULT '0',
  `window_end` bigint NOT NULL DEFAULT '0',
  `next_key` varchar(512) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`sync_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='微信小店售后增量同步游标';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `biz_video_aftersale_sync_state`
--

LOCK TABLES `biz_video_aftersale_sync_state` WRITE;
/*!40000 ALTER TABLE `biz_video_aftersale_sync_state` DISABLE KEYS */;
INSERT INTO `biz_video_aftersale_sync_state` VALUES ('aftersales',0,0,0,'','2026-10-02 23:51:19');
/*!40000 ALTER TABLE `biz_video_aftersale_sync_state` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `biz_video_feed_mount`
--

DROP TABLE IF EXISTS `biz_video_feed_mount`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `biz_video_feed_mount` (
  `mount_id` bigint NOT NULL AUTO_INCREMENT,
  `product_id` bigint NOT NULL,
  `external_product_id` varchar(128) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '',
  `feed_id` varchar(128) COLLATE utf8mb4_unicode_ci NOT NULL,
  `nonce_id` varchar(128) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '',
  `mount_title` varchar(128) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '',
  `status` varchar(24) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'draft' COMMENT 'draft/submitting/submitted/verified/unknown/failed/disabled',
  `last_error` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '',
  `request_id` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '',
  `submitted_at` datetime DEFAULT NULL,
  `verified_at` datetime DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`mount_id`),
  UNIQUE KEY `uk_biz_video_feed_mount` (`product_id`,`feed_id`,`nonce_id`),
  KEY `idx_biz_video_feed_mount_status` (`status`,`update_time`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='瑙嗛?鍙风煭瑙嗛?鍟嗗搧鎸傝浇鐘舵?';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `biz_video_feed_mount`
--

LOCK TABLES `biz_video_feed_mount` WRITE;
/*!40000 ALTER TABLE `biz_video_feed_mount` DISABLE KEYS */;
/*!40000 ALTER TABLE `biz_video_feed_mount` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `biz_video_order`
--

DROP TABLE IF EXISTS `biz_video_order`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `biz_video_order` (
  `order_id` bigint NOT NULL AUTO_INCREMENT,
  `external_order_id` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL,
  `external_status` int NOT NULL DEFAULT '0',
  `created_at` datetime DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL,
  `order_price_cent` bigint NOT NULL DEFAULT '0',
  `freight_cent` bigint NOT NULL DEFAULT '0',
  `merchant_receive_cent` bigint NOT NULL DEFAULT '0',
  `aftersale_count` int NOT NULL DEFAULT '0',
  `items_json` json DEFAULT NULL,
  `last_synced_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`order_id`),
  UNIQUE KEY `uk_biz_video_order_external` (`external_order_id`),
  KEY `idx_biz_video_order_status_time` (`external_status`,`updated_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='微信小店只读订单镜像，不存买家身份和收货信息';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `biz_video_order`
--

LOCK TABLES `biz_video_order` WRITE;
/*!40000 ALTER TABLE `biz_video_order` DISABLE KEYS */;
/*!40000 ALTER TABLE `biz_video_order` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `biz_video_order_fulfillment`
--

DROP TABLE IF EXISTS `biz_video_order_fulfillment`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `biz_video_order_fulfillment` (
  `fulfillment_id` bigint NOT NULL AUTO_INCREMENT,
  `external_order_id` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL,
  `delivery_id` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL,
  `waybill_id` varchar(128) COLLATE utf8mb4_unicode_ci NOT NULL,
  `request_id` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL,
  `status` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'submitting' COMMENT 'submitting/submitted/unknown/verified',
  `last_error` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`fulfillment_id`),
  UNIQUE KEY `uk_biz_video_order_fulfillment_order` (`external_order_id`),
  UNIQUE KEY `uk_biz_video_order_fulfillment_request` (`request_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='微信小店订单发货请求与幂等状态';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `biz_video_order_fulfillment`
--

LOCK TABLES `biz_video_order_fulfillment` WRITE;
/*!40000 ALTER TABLE `biz_video_order_fulfillment` DISABLE KEYS */;
/*!40000 ALTER TABLE `biz_video_order_fulfillment` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `biz_video_order_sync_state`
--

DROP TABLE IF EXISTS `biz_video_order_sync_state`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `biz_video_order_sync_state` (
  `sync_key` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL,
  `completed_through` bigint NOT NULL DEFAULT '0',
  `window_start` bigint NOT NULL DEFAULT '0',
  `window_end` bigint NOT NULL DEFAULT '0',
  `next_key` varchar(512) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`sync_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='微信小店订单增量同步游标';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `biz_video_order_sync_state`
--

LOCK TABLES `biz_video_order_sync_state` WRITE;
/*!40000 ALTER TABLE `biz_video_order_sync_state` DISABLE KEYS */;
INSERT INTO `biz_video_order_sync_state` VALUES ('orders',0,0,0,'','2026-10-02 23:51:19');
/*!40000 ALTER TABLE `biz_video_order_sync_state` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `biz_video_product`
--

DROP TABLE IF EXISTS `biz_video_product`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `biz_video_product` (
  `mapping_id` bigint NOT NULL AUTO_INCREMENT,
  `product_id` bigint NOT NULL,
  `channel` varchar(16) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'video',
  `external_id` varchar(128) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '',
  `sync_status` varchar(16) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'pending',
  `last_error` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '',
  `synced_at` datetime DEFAULT NULL,
  `publish_payload_json` longtext COLLATE utf8mb4_unicode_ci,
  `publish_status` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'draft',
  `publish_error` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '',
  `external_status` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '',
  `published_at` datetime DEFAULT NULL,
  PRIMARY KEY (`mapping_id`),
  UNIQUE KEY `uk_biz_video_product` (`product_id`,`channel`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='视频号/微信小店商品映射';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `biz_video_product`
--

LOCK TABLES `biz_video_product` WRITE;
/*!40000 ALTER TABLE `biz_video_product` DISABLE KEYS */;
/*!40000 ALTER TABLE `biz_video_product` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `biz_video_refund_ledger`
--

DROP TABLE IF EXISTS `biz_video_refund_ledger`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `biz_video_refund_ledger` (
  `ledger_id` bigint NOT NULL AUTO_INCREMENT,
  `external_aftersale_id` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL,
  `external_order_id` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '',
  `amount_cent` bigint NOT NULL DEFAULT '0',
  `external_status` varchar(48) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '',
  `ledger_status` varchar(24) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'pending' COMMENT 'pending/confirmed/failed/unknown',
  `confirmed_at` datetime DEFAULT NULL,
  `last_error` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`ledger_id`),
  UNIQUE KEY `uk_biz_video_refund_ledger_aftersale` (`external_aftersale_id`),
  KEY `idx_biz_video_refund_ledger_status_time` (`ledger_status`,`updated_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='寰?俊灏忓簵閫??瀵硅处鍙拌处锛屼笉骞跺叆鏈?湴鍟嗗煄璁㈠崟';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `biz_video_refund_ledger`
--

LOCK TABLES `biz_video_refund_ledger` WRITE;
/*!40000 ALTER TABLE `biz_video_refund_ledger` DISABLE KEYS */;
/*!40000 ALTER TABLE `biz_video_refund_ledger` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sys_config`
--

DROP TABLE IF EXISTS `sys_config`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_config` (
  `config_id` int NOT NULL AUTO_INCREMENT COMMENT '参数主键',
  `config_name` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '参数名称',
  `config_key` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '参数键名',
  `config_value` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '参数键值',
  `config_type` char(1) COLLATE utf8mb4_unicode_ci DEFAULT 'N' COMMENT '系统内置（Y是 N否）',
  `create_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`config_id`)
) ENGINE=InnoDB AUTO_INCREMENT=100 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='参数配置表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_config`
--

LOCK TABLES `sys_config` WRITE;
/*!40000 ALTER TABLE `sys_config` DISABLE KEYS */;
INSERT INTO `sys_config` VALUES (1,'主框架页-默认皮肤样式名称','sys.index.skinName','skin-blue','Y','admin','2026-10-02 23:48:30','',NULL,''),(2,'用户管理-账号初始密码','sys.user.initPassword','','Y','admin','2026-10-02 23:48:30','',NULL,''),(3,'主框架页-侧边栏主题','sys.index.sideTheme','theme-dark','Y','admin','2026-10-02 23:48:30','',NULL,''),(5,'账号自助-是否开启用户注册功能','sys.account.registerUser','false','Y','admin','2026-10-02 23:48:30','',NULL,'');
/*!40000 ALTER TABLE `sys_config` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sys_dept`
--

DROP TABLE IF EXISTS `sys_dept`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_dept` (
  `dept_id` bigint NOT NULL AUTO_INCREMENT COMMENT '部门id',
  `parent_id` bigint DEFAULT '0' COMMENT '父部门id',
  `ancestors` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '祖级列表',
  `dept_name` varchar(30) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '部门名称',
  `order_num` int DEFAULT '0' COMMENT '显示顺序',
  `leader` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '负责人',
  `phone` varchar(11) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '联系电话',
  `email` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '邮箱',
  `status` char(1) COLLATE utf8mb4_unicode_ci DEFAULT '0' COMMENT '部门状态（0正常 1停用）',
  `del_flag` char(1) COLLATE utf8mb4_unicode_ci DEFAULT '0' COMMENT '删除标志（0代表存在 2代表删除）',
  `create_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`dept_id`)
) ENGINE=InnoDB AUTO_INCREMENT=200 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='部门表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_dept`
--

LOCK TABLES `sys_dept` WRITE;
/*!40000 ALTER TABLE `sys_dept` DISABLE KEYS */;
INSERT INTO `sys_dept` VALUES (100,0,'0','总公司',0,'admin','','','0','0','admin','2026-10-02 23:48:30','',NULL),(101,100,'0,100','默认部门',1,'admin','','','0','0','admin','2026-10-02 23:48:30','',NULL);
/*!40000 ALTER TABLE `sys_dept` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sys_dict_data`
--

DROP TABLE IF EXISTS `sys_dict_data`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_dict_data` (
  `dict_code` bigint NOT NULL AUTO_INCREMENT COMMENT '字典编码',
  `dict_sort` int DEFAULT '0' COMMENT '字典排序',
  `dict_label` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '字典标签',
  `dict_value` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '字典键值',
  `dict_type` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '字典类型',
  `css_class` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '样式属性（其他样式扩展）',
  `list_class` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '表格回显样式',
  `is_default` char(1) COLLATE utf8mb4_unicode_ci DEFAULT 'N' COMMENT '是否默认（Y是 N否）',
  `status` char(1) COLLATE utf8mb4_unicode_ci DEFAULT '0' COMMENT '状态（0正常 1停用）',
  `create_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`dict_code`)
) ENGINE=InnoDB AUTO_INCREMENT=100 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='字典数据表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_dict_data`
--

LOCK TABLES `sys_dict_data` WRITE;
/*!40000 ALTER TABLE `sys_dict_data` DISABLE KEYS */;
INSERT INTO `sys_dict_data` VALUES (1,1,'男','0','sys_user_sex','','','Y','0','admin','2026-10-02 23:48:30','',NULL,''),(2,2,'女','1','sys_user_sex','','','N','0','admin','2026-10-02 23:48:30','',NULL,''),(3,3,'未知','2','sys_user_sex','','','N','0','admin','2026-10-02 23:48:30','',NULL,''),(4,1,'显示','0','sys_show_hide','','primary','Y','0','admin','2026-10-02 23:48:30','',NULL,''),(5,2,'隐藏','1','sys_show_hide','','danger','N','0','admin','2026-10-02 23:48:30','',NULL,''),(6,1,'正常','0','sys_normal_disable','','primary','Y','0','admin','2026-10-02 23:48:30','',NULL,''),(7,2,'停用','1','sys_normal_disable','','danger','N','0','admin','2026-10-02 23:48:30','',NULL,''),(12,1,'是','Y','sys_yes_no','','primary','Y','0','admin','2026-10-02 23:48:30','',NULL,''),(13,2,'否','N','sys_yes_no','','danger','N','0','admin','2026-10-02 23:48:30','',NULL,''),(28,1,'成功','0','sys_common_status','','primary','N','0','admin','2026-10-02 23:48:30','',NULL,''),(29,2,'失败','1','sys_common_status','','danger','N','0','admin','2026-10-02 23:48:30','',NULL,'');
/*!40000 ALTER TABLE `sys_dict_data` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sys_dict_type`
--

DROP TABLE IF EXISTS `sys_dict_type`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_dict_type` (
  `dict_id` bigint NOT NULL AUTO_INCREMENT COMMENT '字典主键',
  `dict_name` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '字典名称',
  `dict_type` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '字典类型',
  `status` char(1) COLLATE utf8mb4_unicode_ci DEFAULT '0' COMMENT '状态（0正常 1停用）',
  `create_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`dict_id`),
  UNIQUE KEY `dict_type` (`dict_type`)
) ENGINE=InnoDB AUTO_INCREMENT=100 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='字典类型表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_dict_type`
--

LOCK TABLES `sys_dict_type` WRITE;
/*!40000 ALTER TABLE `sys_dict_type` DISABLE KEYS */;
INSERT INTO `sys_dict_type` VALUES (1,'用户性别','sys_user_sex','0','admin','2026-10-02 23:48:30','',NULL,''),(2,'菜单状态','sys_show_hide','0','admin','2026-10-02 23:48:30','',NULL,''),(3,'系统开关','sys_normal_disable','0','admin','2026-10-02 23:48:30','',NULL,''),(6,'系统是否','sys_yes_no','0','admin','2026-10-02 23:48:30','',NULL,''),(10,'系统状态','sys_common_status','0','admin','2026-10-02 23:48:30','',NULL,'');
/*!40000 ALTER TABLE `sys_dict_type` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sys_logininfor`
--

DROP TABLE IF EXISTS `sys_logininfor`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_logininfor` (
  `info_id` bigint NOT NULL AUTO_INCREMENT COMMENT '访问ID',
  `user_name` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '用户账号',
  `ipaddr` varchar(128) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '登录IP地址',
  `login_location` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '登录地点',
  `browser` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '浏览器类型',
  `os` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '操作系统',
  `status` char(1) COLLATE utf8mb4_unicode_ci DEFAULT '0' COMMENT '登录状态（0成功 1失败）',
  `msg` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '提示消息',
  `login_time` datetime DEFAULT NULL COMMENT '访问时间',
  PRIMARY KEY (`info_id`),
  KEY `idx_sys_logininfor_s` (`status`),
  KEY `idx_sys_logininfor_lt` (`login_time`)
) ENGINE=InnoDB AUTO_INCREMENT=550 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统访问记录';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_logininfor`
--

LOCK TABLES `sys_logininfor` WRITE;
/*!40000 ALTER TABLE `sys_logininfor` DISABLE KEYS */;
INSERT INTO `sys_logininfor` VALUES (100,'18042618531','127.0.0.1','内网IP','Edge 154','Windows >=10','1','用户不存在/密码错误','2026-10-03 18:48:05'),(101,'admin','127.0.0.1','内网IP','Edge 154','Windows >=10','1','用户不存在/密码错误','2026-10-03 18:48:06'),(102,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-03 18:55:23'),(103,'admin','127.0.0.1','内网IP','Edge 154','Windows >=10','0','登录成功','2026-10-03 18:57:49'),(104,'admin','127.0.0.1','内网IP','Chrome 154','Windows10','0','登录成功','2026-10-03 19:12:53'),(105,'admin','127.0.0.1','内网IP','Chrome 154','Windows10','0','退出成功','2026-10-03 19:16:44'),(106,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-03 19:24:55'),(107,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-03 19:25:13'),(108,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-03 19:26:56'),(109,'admin','127.0.0.1','内网IP','Chrome 154','Windows10','0','登录成功','2026-10-03 19:34:43'),(110,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-03 20:01:07'),(111,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-03 20:01:21'),(112,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-03 20:01:34'),(113,'admin','127.0.0.1','内网IP','Chrome 154','Windows10','0','登录成功','2026-10-03 20:01:59'),(114,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-03 20:09:41'),(115,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-03 20:14:52'),(116,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-03 20:31:30'),(117,'admin','127.0.0.1','内网IP','Chrome 154','Windows10','0','登录成功','2026-10-03 20:37:51'),(118,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-03 21:07:08'),(119,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-03 21:07:25'),(120,'admin','127.0.0.1','内网IP','Chrome 154','Windows10','0','登录成功','2026-10-03 21:13:06'),(121,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-03 21:24:38'),(122,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-03 22:27:50'),(123,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-03 22:28:14'),(124,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-03 22:56:25'),(125,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-03 22:56:58'),(126,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-03 23:00:47'),(127,'admin','127.0.0.1','内网IP','Chrome 154','Windows10','0','登录成功','2026-10-03 23:11:21'),(128,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-03 23:12:44'),(129,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-03 23:13:08'),(130,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-03 23:18:54'),(131,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-03 23:19:49'),(132,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-03 23:21:27'),(133,'qa_finance_20261003232127','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','1','用户不存在/密码错误','2026-10-03 23:21:27'),(134,'qa_operator_20261003232127','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','1','用户不存在/密码错误','2026-10-03 23:21:27'),(135,'qa_verifier_20261003232127','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','1','用户不存在/密码错误','2026-10-03 23:21:27'),(136,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-03 23:22:08'),(137,'qa_role_probe_20261003232207','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','1','用户不存在/密码错误','2026-10-03 23:22:08'),(138,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-03 23:23:09'),(139,'qa_role_probe_20261003232309','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','1','用户不存在/密码错误','2026-10-03 23:23:09'),(140,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-03 23:26:49'),(141,'qa_role_probe_20261003232649','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','1','用户不存在/密码错误','2026-10-03 23:26:49'),(142,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-03 23:28:20'),(143,'qa_role_probe_20261003232820','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','1','用户不存在/密码错误','2026-10-03 23:28:20'),(144,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-03 23:37:09'),(145,'qa_finance_20261003233709','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','1','用户不存在/密码错误','2026-10-03 23:37:09'),(146,'qa_operator_20261003233709','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','1','用户不存在/密码错误','2026-10-03 23:37:10'),(147,'qa_verifier_20261003233709','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','1','用户不存在/密码错误','2026-10-03 23:37:10'),(148,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-03 23:37:56'),(149,'qa_single_20261003233756','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','1','用户不存在/密码错误','2026-10-03 23:37:57'),(150,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-03 23:55:58'),(151,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-04 00:02:01'),(152,'admin','127.0.0.1','内网IP','Chrome 154','Windows10','0','登录成功','2026-10-04 00:02:54'),(153,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-04 00:06:55'),(154,'qa_debug_20261004000655','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','1','用户不存在/密码错误','2026-10-04 00:06:55'),(155,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-04 00:08:20'),(156,'qafn000820','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-04 00:08:21'),(157,'qaop000820','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-04 00:08:21'),(158,'qavr000820','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-04 00:08:21'),(159,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-04 00:09:29'),(160,'qaop000929','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-04 00:09:29'),(161,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-04 00:11:07'),(162,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-04 00:16:51'),(163,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-04 00:25:59'),(164,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-04 00:28:35'),(165,'admin','127.0.0.1','内网IP','Chrome 154','Windows10','0','登录成功','2026-10-04 00:36:36'),(166,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-04 00:48:57'),(167,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-04 01:07:04'),(168,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-04 01:07:27'),(169,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-04 01:25:25'),(170,'admin','127.0.0.1','内网IP','Chrome 154','Windows10','0','登录成功','2026-10-04 01:30:59'),(171,'admin','127.0.0.1','内网IP','Chrome 154','Windows10','0','登录成功','2026-10-04 07:32:48'),(172,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-04 07:36:44'),(173,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-04 07:42:28'),(174,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-04 07:42:36'),(175,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-04 07:44:47'),(176,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-04 07:44:58'),(177,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-04 07:46:05'),(178,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-04 07:47:17'),(179,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-04 07:52:23'),(180,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-04 07:52:33'),(181,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-04 07:59:14'),(182,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-04 08:01:05'),(183,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-04 08:01:29'),(184,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-04 08:08:51'),(185,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-04 08:09:02'),(186,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-04 08:10:23'),(187,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-04 08:11:55'),(188,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-04 08:12:51'),(189,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-04 08:13:52'),(190,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-04 08:23:23'),(191,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-04 08:37:10'),(192,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-04 08:52:17'),(193,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-04 08:52:24'),(194,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-04 09:00:08'),(195,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-04 09:01:37'),(196,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-04 09:14:59'),(197,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-04 09:17:14'),(198,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-04 09:17:23'),(199,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-04 09:17:38'),(200,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-04 09:18:03'),(201,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-04 09:21:07'),(202,'admin','127.0.0.1','内网IP','Chrome 154','Windows10','0','登录成功','2026-10-04 09:22:57'),(203,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-04 09:29:59'),(204,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-04 09:32:27'),(205,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-04 09:40:15'),(206,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-04 09:43:44'),(207,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-04 09:51:40'),(208,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-04 09:56:43'),(209,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-04 10:07:17'),(210,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-04 10:07:27'),(211,'admin','127.0.0.1','内网IP','Chrome 154','Windows10','0','登录成功','2026-10-04 10:12:04'),(212,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-04 10:17:35'),(213,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-04 10:21:04'),(214,'admin','127.0.0.1','内网IP','Chrome 154','Windows10','0','登录成功','2026-10-04 10:23:33'),(215,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-04 10:32:39'),(216,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-04 10:33:35'),(217,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-04 10:49:25'),(218,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-04 10:50:15'),(219,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-04 10:50:51'),(220,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-04 10:51:29'),(221,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-04 10:59:33'),(222,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-04 11:03:42'),(223,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-04 11:04:10'),(224,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-04 11:06:27'),(225,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-04 11:07:32'),(226,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-04 11:09:22'),(227,'admin','127.0.0.1','内网IP','Chrome 154','Windows10','0','登录成功','2026-10-04 11:12:16'),(228,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-04 11:23:26'),(229,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-04 11:28:24'),(230,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-04 11:33:21'),(231,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-04 11:36:31'),(232,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-04 11:39:08'),(233,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-04 11:44:15'),(234,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-04 11:45:03'),(235,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-04 11:48:54'),(236,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-04 12:05:13'),(237,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-04 12:05:43'),(238,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-04 12:10:04'),(239,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-04 12:12:07'),(240,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-04 12:15:39'),(241,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-04 12:20:15'),(242,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-04 12:34:05'),(243,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-04 12:37:28'),(244,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-04 12:39:58'),(245,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-04 12:46:04'),(246,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-04 12:48:26'),(247,'admin','127.0.0.1','内网IP','Chrome 154','Windows10','0','登录成功','2026-10-04 12:51:54'),(248,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-04 13:12:18'),(249,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-04 13:28:59'),(250,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-04 13:30:22'),(251,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-04 13:33:36'),(252,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-04 13:35:03'),(253,'admin','127.0.0.1','内网IP','Chrome 154','Windows10','0','登录成功','2026-10-04 13:42:37'),(254,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-04 13:46:16'),(255,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-04 13:47:18'),(256,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-04 13:51:55'),(257,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-04 13:53:39'),(258,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-04 14:07:21'),(259,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-04 14:32:19'),(260,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-04 14:32:52'),(261,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-04 14:46:47'),(262,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-04 14:47:10'),(263,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-04 14:57:08'),(264,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-04 14:57:29'),(265,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-04 14:57:54'),(266,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-04 14:58:39'),(267,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-04 15:15:19'),(268,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-04 15:18:24'),(269,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-04 15:25:12'),(270,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-04 15:25:33'),(271,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-04 15:36:55'),(272,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-04 15:43:20'),(273,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-04 15:43:53'),(274,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-04 15:45:34'),(275,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-04 15:46:25'),(276,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-04 15:47:37'),(277,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-04 15:51:42'),(278,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-04 15:53:58'),(279,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-04 15:54:09'),(280,'admin','127.0.0.1','内网IP','Chrome 154','Windows10','0','登录成功','2026-10-04 15:56:20'),(281,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-04 16:07:04'),(282,'admin','127.0.0.1','内网IP','Chrome 154','Windows10','0','登录成功','2026-10-04 16:11:32'),(283,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-04 16:18:00'),(284,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-04 16:25:24'),(285,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-04 16:32:55'),(286,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-04 17:12:25'),(287,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-04 17:30:44'),(288,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-04 17:36:12'),(289,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-04 17:36:33'),(290,'admin','127.0.0.1','内网IP','Chrome 154','Windows10','0','登录成功','2026-10-04 17:37:10'),(291,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-04 17:46:40'),(292,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-04 17:53:31'),(293,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-04 17:54:16'),(294,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-04 17:54:53'),(295,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-04 17:55:40'),(296,'admin','127.0.0.1','内网IP','Edge 154.0.0.0','Windows >=10','0','登录成功','2026-10-04 18:00:12'),(297,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-04 18:18:59'),(298,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-04 18:22:02'),(299,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-04 18:24:22'),(300,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-04 18:26:41'),(301,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-04 18:27:40'),(302,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-04 19:43:36'),(303,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-04 19:43:56'),(304,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-04 19:46:53'),(305,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-04 19:48:07'),(306,'admin','127.0.0.1','内网IP','Chrome 154','Windows10','0','登录成功','2026-10-04 19:53:07'),(307,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-04 20:06:49'),(308,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-04 20:14:38'),(309,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-04 20:35:06'),(310,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-04 20:41:27'),(311,'admin','127.0.0.1','内网IP','Chrome 154','Windows10','0','登录成功','2026-10-04 20:46:25'),(312,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-04 21:03:36'),(313,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-04 21:09:22'),(314,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-04 21:14:57'),(315,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-04 21:19:09'),(316,'qa_scope_guard_20261004211909','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','1','用户不存在/密码错误','2026-10-04 21:19:09'),(317,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-04 22:40:51'),(318,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-04 22:42:09'),(319,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-04 22:45:05'),(320,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-04 22:51:26'),(321,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-04 22:59:42'),(322,'admin','127.0.0.1','内网IP','Chrome 154','Windows10','0','登录成功','2026-10-04 23:05:07'),(323,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-04 23:12:14'),(324,'admin','127.0.0.1','内网IP','Edge 154','Windows >=10','0','登录成功','2026-10-04 23:15:26'),(325,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-04 23:42:30'),(326,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-04 23:43:03'),(327,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-04 23:43:19'),(328,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-04 23:48:17'),(329,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-04 23:48:28'),(330,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-04 23:49:56'),(331,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-05 00:01:34'),(332,'admin','127.0.0.1','内网IP','Chrome 154','Windows10','0','登录成功','2026-10-05 00:28:13'),(333,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-05 01:30:18'),(334,'admin','127.0.0.1','内网IP','Chrome 154','Windows10','0','登录成功','2026-10-05 02:36:24'),(335,'admin','127.0.0.1','内网IP','Chrome 154','Windows10','0','登录成功','2026-10-05 03:13:08'),(336,'admin','127.0.0.1','内网IP','Chrome 154','Windows10','0','登录成功','2026-10-05 05:12:08'),(337,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-05 05:42:36'),(338,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-05 05:53:02'),(339,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-05 05:53:10'),(340,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-05 06:05:54'),(341,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-05 06:15:15'),(342,'admin','127.0.0.1','内网IP','Curl 8.21.0','','0','登录成功','2026-10-05 06:22:00'),(343,'admin','127.0.0.1','内网IP','Curl 8.21.0','','0','登录成功','2026-10-05 06:32:26'),(344,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-05 06:38:35'),(345,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-05 06:38:42'),(346,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-05 06:38:47'),(347,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-05 06:44:17'),(348,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-05 06:44:35'),(349,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-05 06:46:00'),(350,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-05 06:47:14'),(351,'admin','127.0.0.1','内网IP','Chrome 154','Windows10','0','登录成功','2026-10-05 06:51:16'),(352,'admin','127.0.0.1','内网IP','Edge 154','Windows >=10','0','登录成功','2026-10-05 07:09:31'),(353,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-05 07:27:30'),(354,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-05 07:37:34'),(355,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-05 07:38:35'),(356,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-05 08:04:03'),(357,'finance','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','1','用户不存在/密码错误','2026-10-05 08:04:03'),(358,'operator','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','1','用户不存在/密码错误','2026-10-05 08:04:03'),(359,'verifier','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','1','用户不存在/密码错误','2026-10-05 08:04:03'),(360,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-05 08:10:09'),(361,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-05 08:25:46'),(362,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-05 08:25:54'),(363,'admin','127.0.0.1','内网IP','Chrome 154','Windows10','0','退出成功','2026-10-05 08:31:36'),(364,'admin','127.0.0.1','内网IP','Chrome 154','Windows10','0','登录成功','2026-10-05 08:33:07'),(365,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-05 08:42:23'),(366,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-05 08:42:32'),(367,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-05 08:51:50'),(368,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-05 08:52:02'),(369,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-05 08:52:29'),(370,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-05 09:00:34'),(371,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-05 09:00:45'),(372,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-05 09:01:11'),(373,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-05 09:01:52'),(374,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-05 09:11:15'),(375,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-05 09:11:30'),(376,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-05 09:17:50'),(377,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-05 09:18:01'),(378,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-05 09:19:41'),(379,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-05 09:22:21'),(380,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-05 09:32:39'),(381,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-05 09:42:58'),(382,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-05 09:55:54'),(383,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-05 09:56:04'),(384,'admin','127.0.0.1','内网IP','Chrome 154','Windows10','0','登录成功','2026-10-05 09:58:54'),(385,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-05 10:08:15'),(386,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-05 10:08:30'),(387,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-05 10:27:30'),(388,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-05 10:39:51'),(389,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-05 10:50:41'),(390,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-05 10:56:15'),(391,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-05 11:00:43'),(392,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-05 11:23:33'),(393,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-05 11:30:02'),(394,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-05 11:34:59'),(395,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-05 11:35:22'),(396,'admin','127.0.0.1','内网IP','Chrome 154','Windows10','0','登录成功','2026-10-05 11:37:40'),(397,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-05 11:44:54'),(398,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-05 11:46:02'),(399,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-05 11:46:14'),(400,'admin','127.0.0.1','内网IP','Chrome 154','Windows10','0','登录成功','2026-10-05 11:46:51'),(401,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-05 11:51:31'),(402,'admin','127.0.0.1','内网IP','Chrome 154','Windows10','0','退出成功','2026-10-05 11:52:11'),(403,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-05 11:55:04'),(404,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-05 11:55:04'),(405,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-05 11:59:13'),(406,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-05 12:11:30'),(407,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-05 12:12:12'),(408,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-05 12:15:56'),(409,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-05 12:16:31'),(410,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-05 12:22:14'),(411,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-05 12:24:24'),(412,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-05 12:27:04'),(413,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-05 12:27:52'),(414,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-05 12:29:57'),(415,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-05 12:32:05'),(416,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-05 12:32:41'),(417,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-05 12:36:12'),(418,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-05 12:36:31'),(419,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-05 12:36:32'),(420,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-05 12:44:16'),(421,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-05 12:44:44'),(422,'admin','127.0.0.1','内网IP','Chrome 154','Windows10','0','登录成功','2026-10-05 12:48:20'),(423,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-05 13:04:01'),(424,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-05 13:18:16'),(425,'admin','127.0.0.1','内网IP','Chrome 154','Windows10','0','登录成功','2026-10-05 13:30:26'),(426,'admin','127.0.0.1','内网IP','Chrome 154','Windows10','0','登录成功','2026-10-05 13:55:38'),(427,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-05 14:24:16'),(428,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-05 14:24:42'),(429,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-05 14:31:59'),(430,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-05 15:07:38'),(431,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-05 15:14:19'),(432,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-05 15:22:40'),(433,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-05 15:32:13'),(434,'admin','127.0.0.1','内网IP','Chrome 154','Windows10','0','登录成功','2026-10-05 15:39:33'),(435,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-05 16:06:49'),(436,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-05 16:25:41'),(437,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-05 16:45:50'),(438,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-05 16:52:42'),(439,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-05 17:01:24'),(440,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-05 17:07:15'),(441,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-05 17:08:05'),(442,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-05 17:08:17'),(443,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-05 17:08:38'),(444,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-05 17:08:58'),(445,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-05 17:10:03'),(446,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-05 17:21:38'),(447,'finance','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-05 17:22:17'),(448,'operator','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-05 17:22:17'),(449,'verifier','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-05 17:22:18'),(450,'finance','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-05 17:22:43'),(451,'operator','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-05 17:22:43'),(452,'verifier','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-05 17:22:44'),(453,'finance','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-05 17:23:08'),(454,'operator','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-05 17:23:08'),(455,'verifier','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-05 17:23:08'),(456,'verifier','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-05 17:24:42'),(457,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-05 17:26:45'),(458,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-05 17:37:33'),(459,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-05 17:38:21'),(460,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-05 17:41:14'),(461,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-05 17:41:14'),(462,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-05 17:41:30'),(463,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-05 17:42:19'),(464,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-05 17:43:36'),(465,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-05 17:43:45'),(466,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-05 17:54:11'),(467,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-05 17:55:21'),(468,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-05 18:00:12'),(469,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-05 21:18:05'),(470,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-05 21:21:15'),(471,'admin','127.0.0.1','内网IP','Chrome 154','Windows10','0','登录成功','2026-10-05 21:38:10'),(472,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-05 21:42:56'),(473,'admin','127.0.0.1','内网IP','WindowsPowerShell 5.1.26100.9444','Windows 10.0','0','登录成功','2026-10-05 21:58:23'),(474,'admin','127.0.0.1','内网IP','WindowsPowerShell 5.1.26100.9444','Windows 10.0','0','登录成功','2026-10-05 22:05:56'),(475,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-05 22:08:38'),(476,'admin','127.0.0.1','内网IP','WindowsPowerShell 5.1.26100.9444','Windows 10.0','0','登录成功','2026-10-05 22:24:06'),(477,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-05 22:26:40'),(478,'admin','127.0.0.1','内网IP','WindowsPowerShell 5.1.26100.9444','Windows 10.0','0','登录成功','2026-10-05 22:34:47'),(479,'admin','127.0.0.1','内网IP','WindowsPowerShell 5.1.26100.9444','Windows 10.0','0','登录成功','2026-10-05 22:51:46'),(480,'admin','127.0.0.1','内网IP','Edge 154.0.0.0','Windows >=10','0','登录成功','2026-10-05 23:06:07'),(481,'admin','127.0.0.1','内网IP','Edge 154.0.0.0','Windows >=10','0','登录成功','2026-10-05 23:12:37'),(482,'admin','127.0.0.1','内网IP','Edge 154.0.0.0','Windows >=10','0','登录成功','2026-10-05 23:14:30'),(483,'admin','127.0.0.1','内网IP','Edge 154.0.0.0','Windows >=10','0','登录成功','2026-10-05 23:16:31'),(484,'admin','127.0.0.1','内网IP','Edge 154.0.0.0','Windows >=10','0','登录成功','2026-10-05 23:19:12'),(485,'admin','127.0.0.1','内网IP','WindowsPowerShell 5.1.26100.9444','Windows 10.0','0','登录成功','2026-10-05 23:23:18'),(486,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-05 23:35:17'),(487,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-05 23:51:02'),(488,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-05 23:55:21'),(489,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-06 00:02:41'),(490,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-06 00:10:52'),(491,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-06 00:16:59'),(492,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-06 00:25:27'),(493,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-06 00:28:56'),(494,'admin','127.0.0.1','内网IP','WindowsPowerShell 5.1.26100.9444','Windows 10.0','0','登录成功','2026-10-06 00:33:01'),(495,'admin','127.0.0.1','内网IP','WindowsPowerShell 5.1.26100.9444','Windows 10.0','0','登录成功','2026-10-06 00:46:44'),(496,'admin','127.0.0.1','内网IP','WindowsPowerShell 5.1.26100.9444','Windows 10.0','0','登录成功','2026-10-06 00:50:39'),(497,'admin','127.0.0.1','内网IP','WindowsPowerShell 5.1.26100.9444','Windows 10.0','0','登录成功','2026-10-06 00:56:06'),(498,'admin','127.0.0.1','内网IP','WindowsPowerShell 5.1.26100.9444','Windows 10.0','0','登录成功','2026-10-06 01:01:01'),(499,'admin','127.0.0.1','内网IP','WindowsPowerShell 5.1.26100.9444','Windows 10.0','0','登录成功','2026-10-06 01:01:57'),(500,'admin','127.0.0.1','内网IP','WindowsPowerShell 5.1.26100.9444','Windows 10.0','0','登录成功','2026-10-06 01:11:06'),(501,'admin','127.0.0.1','内网IP','WindowsPowerShell 5.1.26100.9444','Windows 10.0','0','登录成功','2026-10-06 01:15:16'),(502,'admin','127.0.0.1','内网IP','WindowsPowerShell 5.1.26100.9444','Windows 10.0','0','登录成功','2026-10-06 01:19:33'),(503,'admin','127.0.0.1','内网IP','WindowsPowerShell 5.1.26100.9444','Windows 10.0','0','登录成功','2026-10-06 01:21:53'),(504,'admin','127.0.0.1','内网IP','WindowsPowerShell 5.1.26100.9444','Windows 10.0','0','登录成功','2026-10-06 01:22:26'),(505,'admin','127.0.0.1','内网IP','WindowsPowerShell 5.1.26100.9444','Windows 10.0','0','登录成功','2026-10-06 01:23:53'),(506,'admin','127.0.0.1','内网IP','WindowsPowerShell 5.1.26100.9444','Windows 10.0','0','登录成功','2026-10-06 01:24:53'),(507,'admin','127.0.0.1','内网IP','Chrome 154','Windows10','0','登录成功','2026-10-06 01:26:26'),(508,'admin','127.0.0.1','内网IP','WindowsPowerShell 5.1.26100.9444','Windows 10.0','0','登录成功','2026-10-06 01:36:03'),(509,'admin','127.0.0.1','内网IP','WindowsPowerShell 5.1.26100.9444','Windows 10.0','0','登录成功','2026-10-06 01:40:10'),(510,'admin','127.0.0.1','内网IP','WindowsPowerShell 5.1.26100.9444','Windows 10.0','0','登录成功','2026-10-06 01:45:25'),(511,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-06 02:17:08'),(512,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-06 02:27:32'),(513,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-06 02:27:42'),(514,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-06 02:28:08'),(515,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-06 02:38:58'),(516,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-06 02:38:59'),(517,'admin','127.0.0.1','内网IP','Chrome 154','Windows10','0','登录成功','2026-10-06 02:42:36'),(518,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-06 02:47:48'),(519,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-06 02:47:50'),(520,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-06 02:49:28'),(521,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-06 02:56:56'),(522,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-06 02:58:07'),(523,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-06 02:58:45'),(524,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-06 03:14:07'),(525,'admin','127.0.0.1','内网IP','WindowsPowerShell 5.1.26100.9444','Windows 10.0','0','登录成功','2026-10-06 03:17:06'),(526,'admin','127.0.0.1','内网IP','WindowsPowerShell 5.1.26100.9444','Windows 10.0','0','登录成功','2026-10-06 03:17:44'),(527,'admin','127.0.0.1','内网IP','WindowsPowerShell 5.1.26100.9444','Windows 10.0','0','登录成功','2026-10-06 03:17:53'),(528,'admin','127.0.0.1','内网IP','WindowsPowerShell 5.1.26100.9444','Windows 10.0','0','登录成功','2026-10-06 03:22:18'),(529,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-06 03:49:11'),(530,'admin','127.0.0.1','内网IP','WindowsPowerShell 5.1.26100.9444','Windows 10.0','0','登录成功','2026-10-06 03:49:26'),(531,'admin','127.0.0.1','内网IP','Edge 154.0.0.0','Windows >=10','0','登录成功','2026-10-06 03:50:31'),(532,'admin','127.0.0.1','内网IP','WindowsPowerShell 5.1.26100.9444','Windows 10.0','0','登录成功','2026-10-06 04:04:40'),(533,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-06 04:11:01'),(534,'admin','127.0.0.1','内网IP','WindowsPowerShell 5.1.26100.9444','Windows 10.0','0','登录成功','2026-10-06 04:11:01'),(535,'admin','127.0.0.1','内网IP','WindowsPowerShell 5.1.26100.9444','Windows 10.0','0','登录成功','2026-10-06 04:22:01'),(536,'admin','127.0.0.1','内网IP','WindowsPowerShell 5.1.26100.9444','Windows 10.0','0','登录成功','2026-10-06 04:35:57'),(537,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-06 04:35:57'),(538,'admin','127.0.0.1','内网IP','Chrome 154','Windows10','0','登录成功','2026-10-06 04:38:21'),(539,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-06 04:43:15'),(540,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-06 04:43:42'),(541,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-06 05:06:02'),(542,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-06 05:10:10'),(543,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-06 05:16:59'),(544,'admin','127.0.0.1','内网IP','Chrome 154','Windows10','0','登录成功','2026-10-06 05:29:53'),(545,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-06 05:35:20'),(546,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-06 05:41:55'),(547,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-06 05:47:01'),(548,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-06 05:54:24'),(549,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-06 06:09:24');
/*!40000 ALTER TABLE `sys_logininfor` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sys_menu`
--

DROP TABLE IF EXISTS `sys_menu`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_menu` (
  `menu_id` bigint NOT NULL AUTO_INCREMENT COMMENT '菜单ID',
  `menu_name` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '菜单名称',
  `parent_id` bigint DEFAULT '0' COMMENT '父菜单ID',
  `order_num` int DEFAULT '0' COMMENT '显示顺序',
  `path` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '路由地址',
  `component` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '组件路径',
  `query` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '路由参数',
  `route_name` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '路由名称',
  `is_frame` int DEFAULT '1' COMMENT '是否为外链（0是 1否）',
  `is_cache` int DEFAULT '0' COMMENT '是否缓存（0缓存 1不缓存）',
  `menu_type` char(1) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '菜单类型（M目录 C菜单 F按钮）',
  `visible` char(1) COLLATE utf8mb4_unicode_ci DEFAULT '0' COMMENT '菜单状态（0显示 1隐藏）',
  `status` char(1) COLLATE utf8mb4_unicode_ci DEFAULT '0' COMMENT '菜单状态（0正常 1停用）',
  `perms` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '权限标识',
  `icon` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT '#' COMMENT '菜单图标',
  `create_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '备注',
  PRIMARY KEY (`menu_id`)
) ENGINE=InnoDB AUTO_INCREMENT=3131 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='菜单权限表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_menu`
--

LOCK TABLES `sys_menu` WRITE;
/*!40000 ALTER TABLE `sys_menu` DISABLE KEYS */;
INSERT INTO `sys_menu` VALUES (3000,'业务管理',0,10,'business','Layout','','Business',1,0,'M','0','0','','shopping','admin','2026-10-02 23:51:58','',NULL,'金色酒业业务管理'),(3010,'商品管理',3000,1,'product','business/product/index','','BusinessProduct',1,0,'C','0','0','business:product:list','product','admin','2026-10-02 23:51:58','',NULL,''),(3011,'商品新增',3010,1,'','','','',1,0,'F','0','0','business:product:add','#','admin','2026-10-02 23:51:58','',NULL,''),(3012,'商品修改',3010,2,'','','','',1,0,'F','0','0','business:product:edit','#','admin','2026-10-02 23:51:58','',NULL,''),(3013,'商品删除',3010,3,'','','','',1,0,'F','0','0','business:product:remove','#','admin','2026-10-02 23:51:58','',NULL,''),(3014,'商品查询',3010,4,'','','','',1,0,'F','0','0','business:product:query','#','admin','2026-10-02 23:51:58','',NULL,''),(3020,'门店管理',3000,2,'store','business/store/index','','BusinessStore',1,0,'C','0','0','business:store:list','tree-table','admin','2026-10-02 23:51:58','',NULL,''),(3021,'门店新增',3020,1,'','','','',1,0,'F','0','0','business:store:add','#','admin','2026-10-02 23:51:58','',NULL,''),(3022,'门店修改',3020,2,'','','','',1,0,'F','0','0','business:store:edit','#','admin','2026-10-02 23:51:58','',NULL,''),(3023,'门店删除',3020,3,'','','','',1,0,'F','0','0','business:store:remove','#','admin','2026-10-02 23:51:58','',NULL,''),(3024,'门店查询',3020,4,'','','','',1,0,'F','0','0','business:store:query','#','admin','2026-10-02 23:51:58','',NULL,''),(3025,'门店数据范围',3020,5,'','','','',1,0,'F','0','0','business:store:scope','#','admin','2026-10-04 20:31:26','',NULL,''),(3030,'优惠券模板',3000,3,'coupon','business/coupon/index','','BusinessCoupon',1,0,'C','0','0','business:couponTemplate:list','discount','admin','2026-10-02 23:51:58','',NULL,''),(3031,'券模板新增',3030,1,'','','','',1,0,'F','0','0','business:couponTemplate:add','#','admin','2026-10-02 23:51:58','',NULL,''),(3032,'券模板修改',3030,2,'','','','',1,0,'F','0','0','business:couponTemplate:edit','#','admin','2026-10-02 23:51:58','',NULL,''),(3033,'券模板删除',3030,3,'','','','',1,0,'F','0','0','business:couponTemplate:remove','#','admin','2026-10-02 23:51:58','',NULL,''),(3034,'券模板查询',3030,4,'','','','',1,0,'F','0','0','business:couponTemplate:query','#','admin','2026-10-02 23:51:58','',NULL,''),(3040,'积分兑换',3000,4,'exchange','business/exchange/index','','BusinessExchange',1,0,'C','0','0','business:exchangeItem:list','money','admin','2026-10-02 23:51:58','',NULL,''),(3041,'兑换项新增',3040,1,'','','','',1,0,'F','0','0','business:exchangeItem:add','#','admin','2026-10-02 23:51:58','',NULL,''),(3042,'兑换项修改',3040,2,'','','','',1,0,'F','0','0','business:exchangeItem:edit','#','admin','2026-10-02 23:51:58','',NULL,''),(3043,'兑换项删除',3040,3,'','','','',1,0,'F','0','0','business:exchangeItem:remove','#','admin','2026-10-02 23:51:58','',NULL,''),(3044,'兑换项查询',3040,4,'','','','',1,0,'F','0','0','business:exchangeItem:query','#','admin','2026-10-02 23:51:58','',NULL,''),(3050,'订单管理',3000,5,'order','business/order/index','','BusinessOrder',1,0,'C','0','0','business:order:list','shopping','admin','2026-10-02 23:51:58','',NULL,''),(3051,'订单查询',3050,1,'','','','',1,0,'F','0','0','business:order:query','#','admin','2026-10-02 23:51:58','',NULL,''),(3052,'订单处理',3050,2,'','','','',1,0,'F','0','0','business:order:edit','#','admin','2026-10-02 23:51:58','',NULL,''),(3060,'会员管理',3000,6,'member','business/member/index','','BusinessMember',1,0,'C','0','0','business:member:list','user','admin','2026-10-02 23:51:58','',NULL,''),(3061,'会员查询',3060,1,'','','','',1,0,'F','0','0','business:member:query','#','admin','2026-10-02 23:51:58','',NULL,''),(3062,'会员处理',3060,2,'','','','',1,0,'F','0','0','business:member:edit','#','admin','2026-10-02 23:51:58','',NULL,''),(3070,'奖品权益',3000,7,'reward','business/reward/index','','BusinessReward',1,0,'C','0','0','business:reward:list','gift','admin','2026-10-02 23:51:58','',NULL,''),(3071,'奖品查询',3070,1,'','','','',1,0,'F','0','0','business:reward:query','#','admin','2026-10-02 23:51:58','',NULL,''),(3072,'奖品处理',3070,2,'','','','',1,0,'F','0','0','business:reward:edit','#','admin','2026-10-02 23:51:58','',NULL,''),(3080,'核销记录',3000,8,'redemption','business/redemption/index','','BusinessRedemption',1,0,'C','0','0','business:redemption:list','documentation','admin','2026-10-02 23:51:58','',NULL,''),(3081,'核销查询',3080,1,'','','','',1,0,'F','0','0','business:redemption:query','#','admin','2026-10-02 23:51:58','',NULL,''),(3090,'品牌配置',3000,9,'brand','business/brand/index','','BusinessBrand',1,0,'C','0','0','business:brand:query','edit','admin','2026-10-02 23:51:58','',NULL,''),(3091,'品牌查询',3090,1,'','','','',1,0,'F','0','0','business:brand:query','#','admin','2026-10-02 23:51:58','',NULL,''),(3092,'品牌修改',3090,2,'','','','',1,0,'F','0','0','business:brand:edit','#','admin','2026-10-02 23:51:58','',NULL,''),(3100,'小程序内容',3000,10,'content','business/content/index','','BusinessContent',1,0,'C','0','0','business:pageContent:list','documentation','admin','2026-10-02 23:51:58','',NULL,'首页、活动、规则和导航内容'),(3101,'内容查询',3100,1,'','','','',1,0,'F','0','0','business:pageContent:query','#','admin','2026-10-02 23:51:58','',NULL,''),(3102,'内容新增',3100,2,'','','','',1,0,'F','0','0','business:pageContent:add','#','admin','2026-10-02 23:51:58','',NULL,''),(3103,'内容修改',3100,3,'','','','',1,0,'F','0','0','business:pageContent:edit','#','admin','2026-10-02 23:51:58','',NULL,''),(3104,'内容删除',3100,4,'','','','',1,0,'F','0','0','business:pageContent:remove','#','admin','2026-10-02 23:51:58','',NULL,''),(3110,'运营中心',3000,11,'operations','business/operations/index','','BusinessOperations',1,0,'C','0','0','','data-analysis','admin','2026-10-02 23:51:58','',NULL,'抽奖、积分、分销、渠道、报表'),(3111,'运营中心查询',3110,1,'','','','',1,0,'F','0','0','business:dashboard:query','#','admin','2026-10-02 23:51:58','',NULL,''),(3112,'抽奖码生成',3110,2,'','','','',1,0,'F','0','0','business:lottery:generate','#','admin','2026-10-02 23:51:58','',NULL,''),(3113,'抽奖记录查询',3110,3,'','','','',1,0,'F','0','0','business:lottery:list','#','admin','2026-10-02 23:51:58','',NULL,''),(3114,'积分规则管理',3110,4,'','','','',1,0,'F','0','0','business:pointRule:edit','#','admin','2026-10-02 23:51:58','',NULL,''),(3115,'分销规则管理',3110,5,'','','','',1,0,'F','0','0','business:promotion:edit','#','admin','2026-10-02 23:51:58','',NULL,''),(3116,'排行榜奖励发放',3110,6,'','','','',1,0,'F','0','0','business:ranking:issue','#','admin','2026-10-02 23:51:58','',NULL,''),(3117,'社群通知发送',3110,7,'','','','',1,0,'F','0','0','business:social:send','#','admin','2026-10-02 23:51:58','',NULL,''),(3118,'微信渠道配置',3110,8,'','','','',1,0,'F','0','0','business:channel:edit','#','admin','2026-10-02 23:51:58','',NULL,''),(3119,'财务报表导出',3110,9,'','','','',1,0,'F','0','0','business:report:export','#','admin','2026-10-02 23:51:58','',NULL,''),(3120,'积分规则查询',3110,10,'','','','',1,0,'F','0','0','business:pointRule:list','#','admin','2026-10-02 23:51:58','',NULL,''),(3121,'分销规则查询',3110,11,'','','','',1,0,'F','0','0','business:promotion:query','#','admin','2026-10-02 23:51:58','',NULL,''),(3122,'排行榜查询',3110,12,'','','','',1,0,'F','0','0','business:ranking:query','#','admin','2026-10-02 23:51:58','',NULL,''),(3123,'社群活动新增',3110,13,'','','','',1,0,'F','0','0','business:social:add','#','admin','2026-10-02 23:51:58','',NULL,''),(3124,'渠道配置查询',3110,14,'','','','',1,0,'F','0','0','business:channel:query','#','admin','2026-10-02 23:51:58','',NULL,''),(3125,'退款查询',3110,15,'','','','',1,0,'F','0','0','business:refund:list','#','admin','2026-10-02 23:51:58','',NULL,''),(3126,'退款处理',3110,16,'','','','',1,0,'F','0','0','business:refund:edit','#','admin','2026-10-02 23:51:58','',NULL,''),(3127,'社群活动查询',3110,17,'','','','',1,0,'F','0','0','business:social:list','#','admin','2026-10-02 23:51:58','',NULL,''),(3128,'抽奖奖项管理',3110,18,'','','','',1,0,'F','0','0','business:lottery:edit','#','admin','2026-10-02 23:51:58','',NULL,''),(3129,'佣金提现查询',3110,19,'','','','',1,0,'F','0','0','business:settlement:list','#','admin','2026-10-02 23:51:58','',NULL,''),(3130,'佣金提现处理',3110,20,'','','','',1,0,'F','0','0','business:settlement:edit','#','admin','2026-10-02 23:51:58','',NULL,'');
/*!40000 ALTER TABLE `sys_menu` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sys_notice`
--

DROP TABLE IF EXISTS `sys_notice`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_notice` (
  `notice_id` int NOT NULL AUTO_INCREMENT COMMENT '公告ID',
  `notice_title` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '公告标题',
  `notice_type` char(1) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '公告类型（1通知 2公告）',
  `notice_content` longblob COMMENT '公告内容',
  `status` char(1) COLLATE utf8mb4_unicode_ci DEFAULT '0' COMMENT '公告状态（0正常 1关闭）',
  `create_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`notice_id`)
) ENGINE=InnoDB AUTO_INCREMENT=10 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='通知公告表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_notice`
--

LOCK TABLES `sys_notice` WRITE;
/*!40000 ALTER TABLE `sys_notice` DISABLE KEYS */;
/*!40000 ALTER TABLE `sys_notice` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sys_oper_log`
--

DROP TABLE IF EXISTS `sys_oper_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_oper_log` (
  `oper_id` bigint NOT NULL AUTO_INCREMENT COMMENT '日志主键',
  `title` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '模块标题',
  `business_type` int DEFAULT '0' COMMENT '业务类型（0其它 1新增 2修改 3删除）',
  `method` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '方法名称',
  `request_method` varchar(10) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '请求方式',
  `operator_type` int DEFAULT '0' COMMENT '操作类别（0其它 1后台用户 2手机端用户）',
  `oper_name` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '操作人员',
  `dept_name` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '部门名称',
  `oper_url` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '请求URL',
  `oper_ip` varchar(128) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '主机地址',
  `oper_location` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '操作地点',
  `oper_param` varchar(2000) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '请求参数',
  `json_result` varchar(2000) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '返回参数',
  `status` int DEFAULT '0' COMMENT '操作状态（0正常 1异常）',
  `error_msg` varchar(2000) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '错误消息',
  `oper_time` datetime DEFAULT NULL COMMENT '操作时间',
  `cost_time` bigint DEFAULT '0' COMMENT '消耗时间',
  PRIMARY KEY (`oper_id`),
  KEY `idx_sys_oper_log_bt` (`business_type`),
  KEY `idx_sys_oper_log_s` (`status`),
  KEY `idx_sys_oper_log_ot` (`oper_time`)
) ENGINE=InnoDB AUTO_INCREMENT=381 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='操作日志记录';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_oper_log`
--

LOCK TABLES `sys_oper_log` WRITE;
/*!40000 ALTER TABLE `sys_oper_log` DISABLE KEYS */;
INSERT INTO `sys_oper_log` VALUES (100,'短视频商品挂载配置',1,'com.ruoyi.web.controller.business.BusinessExtensionController.saveVideoFeedMount()','POST',1,'admin','默认部门','/business/extension/video/mounts','127.0.0.1','内网IP','{\"externalProductId\":\"wx-prod-demo\",\"feedId\":\"feed-demo\",\"mountTitle\":\"典藏酱香短视频\",\"productId\":1,\"nonceId\":\"nonce-demo\"} ','{\"msg\":\"操作成功\",\"code\":200,\"data\":{\"mountId\":1,\"productId\":1,\"externalProductId\":\"wx-prod-demo\",\"feedId\":\"feed-demo\",\"nonceId\":\"nonce-demo\",\"mountTitle\":\"典藏酱香短视频\",\"status\":\"draft\",\"lastError\":\"\",\"requestId\":\"\"}}',0,NULL,'2026-10-03 20:09:41',22),(101,'短视频商品挂载提交',2,'com.ruoyi.web.controller.business.BusinessExtensionController.submitVideoFeedMount()','POST',1,'admin','默认部门','/business/extension/video/mounts/1/submit','127.0.0.1','内网IP','1 ','{\"msg\":\"操作成功\",\"code\":200,\"data\":{\"mountId\":1,\"requestId\":\"713ccbde1b964e5c9b0e324380050d71\",\"status\":\"submitting\",\"feedId\":\"feed-demo\",\"nonceId\":\"nonce-demo\",\"externalProductId\":\"wx-prod-demo\",\"message\":\"已生成短视频挂载清单，请在视频号助手提交商品并回到此处确认平台结果\",\"requiresPlatformConfirmation\":true}}',0,NULL,'2026-10-03 20:09:41',9),(102,'短视频商品挂载结果确认',2,'com.ruoyi.web.controller.business.BusinessExtensionController.verifyVideoFeedMount()','PUT',1,'admin','默认部门','/business/extension/video/mounts/1/verify','127.0.0.1','内网IP','1 {\"platformConfirmed\":true,\"status\":\"verified\"} ','{\"msg\":\"操作成功\",\"code\":200,\"data\":{\"mountId\":1,\"productId\":1,\"externalProductId\":\"wx-prod-demo\",\"feedId\":\"feed-demo\",\"nonceId\":\"nonce-demo\",\"mountTitle\":\"典藏酱香短视频\",\"status\":\"verified\",\"lastError\":\"\",\"requestId\":\"713ccbde1b964e5c9b0e324380050d71\",\"submittedAt\":\"2026-10-03 20:09:41\",\"verifiedAt\":\"2026-10-03 20:09:41\"}}',0,NULL,'2026-10-03 20:09:41',11),(103,'业务财务报表',5,'com.ruoyi.web.controller.business.BusinessExtensionController.export()','GET',1,'admin','默认部门','/business/extension/reports/videoRefunds/export','127.0.0.1','内网IP','{\"fromDate\":\"2026-01-01\",\"toDate\":\"2026-12-31\"}',NULL,0,NULL,'2026-10-03 20:14:52',12),(104,'业务状态',2,'com.ruoyi.web.controller.business.BusinessAdminController.status()','PUT',1,'admin','默认部门','/business/member/1/status','127.0.0.1','内网IP','\"member\" \"1\" {\"staffStoreId\":1,\"status\":\"0\"} ','{\"msg\":\"操作成功\",\"code\":200}',0,NULL,'2026-10-03 23:00:47',10),(105,'抽奖奖品',2,'com.ruoyi.web.controller.business.BusinessExtensionController.disableLotteryPrize()','POST',1,'admin','默认部门','/business/extension/lottery/prizes/9/disable','127.0.0.1','内网IP','9 ','{\"msg\":\"操作成功\",\"code\":200}',0,NULL,'2026-10-03 23:13:08',6),(106,'抽奖奖品',2,'com.ruoyi.web.controller.business.BusinessExtensionController.disableLotteryPrize()','POST',1,'admin','默认部门','/business/extension/lottery/prizes/11/disable','127.0.0.1','内网IP','11 ','{\"msg\":\"操作成功\",\"code\":200}',0,NULL,'2026-10-03 23:13:08',4),(107,'抽奖奖品',2,'com.ruoyi.web.controller.business.BusinessExtensionController.disableLotteryPrize()','POST',1,'admin','默认部门','/business/extension/lottery/prizes/10/disable','127.0.0.1','内网IP','10 ','{\"msg\":\"操作成功\",\"code\":200}',0,NULL,'2026-10-03 23:13:08',5),(108,'抽奖奖品',2,'com.ruoyi.web.controller.business.BusinessExtensionController.disableLotteryPrize()','POST',1,'admin','默认部门','/business/extension/lottery/prizes/12/disable','127.0.0.1','内网IP','12 ','{\"msg\":\"操作成功\",\"code\":200}',0,NULL,'2026-10-03 23:13:08',4),(109,'用户管理',1,'com.ruoyi.web.controller.system.SysUserController.add()','POST',1,'admin','默认部门','/system/user','127.0.0.1','内网IP','{\"admin\":false,\"createBy\":\"admin\",\"deptId\":101,\"nickName\":\"qa_finance_20261003232127\",\"params\":{},\"postIds\":[],\"remark\":\"temporary permission verification\",\"roleIds\":[10],\"sex\":\"1\",\"status\":\"0\",\"userId\":100,\"userName\":\"qa_finance_20261003232127\"} ','{\"msg\":\"操作成功\",\"code\":200}',0,NULL,'2026-10-03 23:21:27',87),(110,'用户管理',1,'com.ruoyi.web.controller.system.SysUserController.add()','POST',1,'admin','默认部门','/system/user','127.0.0.1','内网IP','{\"admin\":false,\"createBy\":\"admin\",\"deptId\":101,\"nickName\":\"qa_operator_20261003232127\",\"params\":{},\"postIds\":[],\"remark\":\"temporary permission verification\",\"roleIds\":[20],\"sex\":\"1\",\"status\":\"0\",\"userId\":101,\"userName\":\"qa_operator_20261003232127\"} ','{\"msg\":\"操作成功\",\"code\":200}',0,NULL,'2026-10-03 23:21:27',68),(111,'用户管理',1,'com.ruoyi.web.controller.system.SysUserController.add()','POST',1,'admin','默认部门','/system/user','127.0.0.1','内网IP','{\"admin\":false,\"createBy\":\"admin\",\"deptId\":101,\"nickName\":\"qa_verifier_20261003232127\",\"params\":{},\"postIds\":[],\"remark\":\"temporary permission verification\",\"roleIds\":[30],\"sex\":\"1\",\"status\":\"0\",\"userId\":102,\"userName\":\"qa_verifier_20261003232127\"} ','{\"msg\":\"操作成功\",\"code\":200}',0,NULL,'2026-10-03 23:21:27',67),(112,'用户管理',3,'com.ruoyi.web.controller.system.SysUserController.remove()','DELETE',1,'admin','默认部门','/system/user/100','127.0.0.1','内网IP','[100] ','{\"msg\":\"操作成功\",\"code\":200}',0,NULL,'2026-10-03 23:21:27',12),(113,'用户管理',3,'com.ruoyi.web.controller.system.SysUserController.remove()','DELETE',1,'admin','默认部门','/system/user/101','127.0.0.1','内网IP','[101] ','{\"msg\":\"操作成功\",\"code\":200}',0,NULL,'2026-10-03 23:21:27',10),(114,'用户管理',3,'com.ruoyi.web.controller.system.SysUserController.remove()','DELETE',1,'admin','默认部门','/system/user/102','127.0.0.1','内网IP','[102] ','{\"msg\":\"操作成功\",\"code\":200}',0,NULL,'2026-10-03 23:21:27',9),(115,'用户管理',1,'com.ruoyi.web.controller.system.SysUserController.add()','POST',1,'admin','默认部门','/system/user','127.0.0.1','内网IP','{\"admin\":false,\"createBy\":\"admin\",\"deptId\":101,\"nickName\":\"qa_role_probe_20261003232207\",\"params\":{},\"postIds\":[],\"remark\":\"probe\",\"roleIds\":[30],\"sex\":\"1\",\"status\":\"0\",\"userId\":103,\"userName\":\"qa_role_probe_20261003232207\"} ','{\"msg\":\"操作成功\",\"code\":200}',0,NULL,'2026-10-03 23:22:08',68),(116,'用户管理',3,'com.ruoyi.web.controller.system.SysUserController.remove()','DELETE',1,'admin','默认部门','/system/user/103','127.0.0.1','内网IP','[103] ','{\"msg\":\"操作成功\",\"code\":200}',0,NULL,'2026-10-03 23:22:08',10),(117,'用户管理',1,'com.ruoyi.web.controller.system.SysUserController.add()','POST',1,'admin','默认部门','/system/user','127.0.0.1','内网IP','{\"admin\":false,\"createBy\":\"admin\",\"deptId\":101,\"nickName\":\"qa_role_probe_20261003232309\",\"params\":{},\"postIds\":[],\"remark\":\"probe\",\"roleIds\":[30],\"sex\":\"1\",\"status\":\"0\",\"userId\":104,\"userName\":\"qa_role_probe_20261003232309\"} ','{\"msg\":\"操作成功\",\"code\":200}',0,NULL,'2026-10-03 23:23:09',68),(118,'用户管理',3,'com.ruoyi.web.controller.system.SysUserController.remove()','DELETE',1,'admin','默认部门','/system/user/104','127.0.0.1','内网IP','[104] ','{\"msg\":\"操作成功\",\"code\":200}',0,NULL,'2026-10-03 23:23:09',13),(119,'用户管理',1,'com.ruoyi.web.controller.system.SysUserController.add()','POST',1,'admin','默认部门','/system/user','127.0.0.1','内网IP','{\"admin\":false,\"createBy\":\"admin\",\"deptId\":101,\"nickName\":\"qa_role_probe_20261003232649\",\"params\":{},\"postIds\":[],\"remark\":\"probe\",\"sex\":\"1\",\"status\":\"0\",\"userId\":105,\"userName\":\"qa_role_probe_20261003232649\"} ','{\"msg\":\"操作成功\",\"code\":200}',0,NULL,'2026-10-03 23:26:49',70),(120,'用户管理',4,'com.ruoyi.web.controller.system.SysUserController.insertAuthRole()','PUT',1,'admin','默认部门','/system/user/authRole','127.0.0.1','内网IP','{\"roleIds\":\"30\",\"userId\":\"105\"}','{\"msg\":\"操作成功\",\"code\":200}',0,NULL,'2026-10-03 23:26:49',9),(121,'用户管理',3,'com.ruoyi.web.controller.system.SysUserController.remove()','DELETE',1,'admin','默认部门','/system/user/105','127.0.0.1','内网IP','[105] ','{\"msg\":\"操作成功\",\"code\":200}',0,NULL,'2026-10-03 23:26:49',14),(122,'用户管理',1,'com.ruoyi.web.controller.system.SysUserController.add()','POST',1,'admin','默认部门','/system/user','127.0.0.1','内网IP','{\"admin\":false,\"createBy\":\"admin\",\"deptId\":101,\"nickName\":\"qa_role_probe_20261003232820\",\"params\":{},\"postIds\":[],\"remark\":\"probe\",\"roleIds\":[30],\"sex\":\"1\",\"status\":\"0\",\"userId\":106,\"userName\":\"qa_role_probe_20261003232820\"} ','{\"msg\":\"操作成功\",\"code\":200}',0,NULL,'2026-10-03 23:28:20',67),(123,'用户管理',2,'com.ruoyi.web.controller.system.SysUserController.resetPwd()','PUT',1,'admin','默认部门','/system/user/resetPwd','127.0.0.1','内网IP','{\"admin\":false,\"params\":{},\"updateBy\":\"admin\",\"userId\":106} ','{\"msg\":\"操作成功\",\"code\":200}',0,NULL,'2026-10-03 23:28:20',59),(124,'用户管理',4,'com.ruoyi.web.controller.system.SysUserController.insertAuthRole()','PUT',1,'admin','默认部门','/system/user/authRole','127.0.0.1','内网IP','{\"roleIds\":\"30\",\"userId\":\"106\"}','{\"msg\":\"操作成功\",\"code\":200}',0,NULL,'2026-10-03 23:28:20',8),(125,'用户管理',3,'com.ruoyi.web.controller.system.SysUserController.remove()','DELETE',1,'admin','默认部门','/system/user/106','127.0.0.1','内网IP','[106] ','{\"msg\":\"操作成功\",\"code\":200}',0,NULL,'2026-10-03 23:28:20',11),(126,'用户管理',1,'com.ruoyi.web.controller.system.SysUserController.add()','POST',1,'admin','默认部门','/system/user','127.0.0.1','内网IP','{\"admin\":false,\"createBy\":\"admin\",\"deptId\":101,\"nickName\":\"qa_finance_20261003233709\",\"params\":{},\"postIds\":[],\"remark\":\"temporary permission verification\",\"roleIds\":[],\"sex\":\"1\",\"status\":\"0\",\"userId\":107,\"userName\":\"qa_finance_20261003233709\"} ','{\"msg\":\"操作成功\",\"code\":200}',0,NULL,'2026-10-03 23:37:09',66),(127,'用户管理',4,'com.ruoyi.web.controller.system.SysUserController.insertAuthRole()','PUT',1,'admin','默认部门','/system/user/authRole','127.0.0.1','内网IP','{\"roleIds\":\"10\",\"userId\":\"107\"}','{\"msg\":\"操作成功\",\"code\":200}',0,NULL,'2026-10-03 23:37:09',7),(128,'用户管理',1,'com.ruoyi.web.controller.system.SysUserController.add()','POST',1,'admin','默认部门','/system/user','127.0.0.1','内网IP','{\"admin\":false,\"createBy\":\"admin\",\"deptId\":101,\"nickName\":\"qa_operator_20261003233709\",\"params\":{},\"postIds\":[],\"remark\":\"temporary permission verification\",\"roleIds\":[],\"sex\":\"1\",\"status\":\"0\",\"userId\":108,\"userName\":\"qa_operator_20261003233709\"} ','{\"msg\":\"操作成功\",\"code\":200}',0,NULL,'2026-10-03 23:37:09',64),(129,'用户管理',4,'com.ruoyi.web.controller.system.SysUserController.insertAuthRole()','PUT',1,'admin','默认部门','/system/user/authRole','127.0.0.1','内网IP','{\"roleIds\":\"20\",\"userId\":\"108\"}','{\"msg\":\"操作成功\",\"code\":200}',0,NULL,'2026-10-03 23:37:10',8),(130,'用户管理',1,'com.ruoyi.web.controller.system.SysUserController.add()','POST',1,'admin','默认部门','/system/user','127.0.0.1','内网IP','{\"admin\":false,\"createBy\":\"admin\",\"deptId\":101,\"nickName\":\"qa_verifier_20261003233709\",\"params\":{},\"postIds\":[],\"remark\":\"temporary permission verification\",\"roleIds\":[],\"sex\":\"1\",\"status\":\"0\",\"userId\":109,\"userName\":\"qa_verifier_20261003233709\"} ','{\"msg\":\"操作成功\",\"code\":200}',0,NULL,'2026-10-03 23:37:10',64),(131,'用户管理',4,'com.ruoyi.web.controller.system.SysUserController.insertAuthRole()','PUT',1,'admin','默认部门','/system/user/authRole','127.0.0.1','内网IP','{\"roleIds\":\"30\",\"userId\":\"109\"}','{\"msg\":\"操作成功\",\"code\":200}',0,NULL,'2026-10-03 23:37:10',9),(132,'用户管理',1,'com.ruoyi.web.controller.system.SysUserController.add()','POST',1,'admin','默认部门','/system/user','127.0.0.1','内网IP','{\"admin\":false,\"createBy\":\"admin\",\"deptId\":101,\"nickName\":\"qa_single_20261003233756\",\"params\":{},\"postIds\":[],\"remark\":\"probe\",\"roleIds\":[],\"sex\":\"1\",\"status\":\"0\",\"userId\":110,\"userName\":\"qa_single_20261003233756\"} ','{\"msg\":\"操作成功\",\"code\":200}',0,NULL,'2026-10-03 23:37:56',63),(133,'用户管理',1,'com.ruoyi.web.controller.system.SysUserController.add()','POST',1,'admin','默认部门','/system/user','127.0.0.1','内网IP','{\"admin\":false,\"createBy\":\"admin\",\"deptId\":101,\"nickName\":\"QA Debug\",\"params\":{},\"postIds\":[],\"remark\":\"temporary\",\"roleIds\":[30],\"sex\":\"1\",\"status\":\"0\",\"userId\":111,\"userName\":\"qa_debug_20261004000655\"} ','{\"msg\":\"操作成功\",\"code\":200}',0,NULL,'2026-10-04 00:06:55',108),(134,'用户管理',4,'com.ruoyi.web.controller.system.SysUserController.insertAuthRole()','PUT',1,'admin','默认部门','/system/user/authRole','127.0.0.1','内网IP','{\"roleIds\":\"30\",\"userId\":\"111\"}','{\"msg\":\"操作成功\",\"code\":200}',0,NULL,'2026-10-04 00:06:55',11),(135,'用户管理',3,'com.ruoyi.web.controller.system.SysUserController.remove()','DELETE',1,'admin','默认部门','/system/user/111','127.0.0.1','内网IP','[111] ','{\"msg\":\"操作成功\",\"code\":200}',0,NULL,'2026-10-04 00:06:55',16),(136,'用户管理',1,'com.ruoyi.web.controller.system.SysUserController.add()','POST',1,'admin','默认部门','/system/user','127.0.0.1','内网IP','{\"admin\":false,\"createBy\":\"admin\",\"deptId\":101,\"nickName\":\"qafn000820\",\"params\":{},\"postIds\":[],\"remark\":\"temporary permission verification\",\"roleIds\":[],\"sex\":\"1\",\"status\":\"0\",\"userId\":112,\"userName\":\"qafn000820\"} ','{\"msg\":\"操作成功\",\"code\":200}',0,NULL,'2026-10-04 00:08:21',68),(137,'用户管理',4,'com.ruoyi.web.controller.system.SysUserController.insertAuthRole()','PUT',1,'admin','默认部门','/system/user/authRole','127.0.0.1','内网IP','{\"roleIds\":\"10\",\"userId\":\"112\"}','{\"msg\":\"操作成功\",\"code\":200}',0,NULL,'2026-10-04 00:08:21',11),(138,'用户管理',1,'com.ruoyi.web.controller.system.SysUserController.add()','POST',1,'admin','默认部门','/system/user','127.0.0.1','内网IP','{\"admin\":false,\"createBy\":\"admin\",\"deptId\":101,\"nickName\":\"qaop000820\",\"params\":{},\"postIds\":[],\"remark\":\"temporary permission verification\",\"roleIds\":[],\"sex\":\"1\",\"status\":\"0\",\"userId\":113,\"userName\":\"qaop000820\"} ','{\"msg\":\"操作成功\",\"code\":200}',0,NULL,'2026-10-04 00:08:21',71),(139,'用户管理',4,'com.ruoyi.web.controller.system.SysUserController.insertAuthRole()','PUT',1,'admin','默认部门','/system/user/authRole','127.0.0.1','内网IP','{\"roleIds\":\"20\",\"userId\":\"113\"}','{\"msg\":\"操作成功\",\"code\":200}',0,NULL,'2026-10-04 00:08:21',9),(140,'用户管理',1,'com.ruoyi.web.controller.system.SysUserController.add()','POST',1,'admin','默认部门','/system/user','127.0.0.1','内网IP','{\"admin\":false,\"createBy\":\"admin\",\"deptId\":101,\"nickName\":\"qavr000820\",\"params\":{},\"postIds\":[],\"remark\":\"temporary permission verification\",\"roleIds\":[],\"sex\":\"1\",\"status\":\"0\",\"userId\":114,\"userName\":\"qavr000820\"} ','{\"msg\":\"操作成功\",\"code\":200}',0,NULL,'2026-10-04 00:08:21',67),(141,'用户管理',4,'com.ruoyi.web.controller.system.SysUserController.insertAuthRole()','PUT',1,'admin','默认部门','/system/user/authRole','127.0.0.1','内网IP','{\"roleIds\":\"30\",\"userId\":\"114\"}','{\"msg\":\"操作成功\",\"code\":200}',0,NULL,'2026-10-04 00:08:21',9),(142,'用户管理',1,'com.ruoyi.web.controller.system.SysUserController.add()','POST',1,'admin','默认部门','/system/user','127.0.0.1','内网IP','{\"admin\":false,\"createBy\":\"admin\",\"deptId\":101,\"nickName\":\"qaop000929\",\"params\":{},\"postIds\":[],\"remark\":\"temporary permission verification\",\"roleIds\":[],\"sex\":\"1\",\"status\":\"0\",\"userId\":115,\"userName\":\"qaop000929\"} ','{\"msg\":\"操作成功\",\"code\":200}',0,NULL,'2026-10-04 00:09:29',67),(143,'用户管理',4,'com.ruoyi.web.controller.system.SysUserController.insertAuthRole()','PUT',1,'admin','默认部门','/system/user/authRole','127.0.0.1','内网IP','{\"roleIds\":\"20\",\"userId\":\"115\"}','{\"msg\":\"操作成功\",\"code\":200}',0,NULL,'2026-10-04 00:09:29',10),(144,'业务财务报表',5,'com.ruoyi.web.controller.business.BusinessExtensionController.export()','GET',1,'admin','默认部门','/business/extension/reports/orders/export','127.0.0.1','内网IP','{}',NULL,0,NULL,'2026-10-04 07:42:28',17),(145,'业务财务报表',5,'com.ruoyi.web.controller.business.BusinessExtensionController.export()','GET',1,'admin','默认部门','/business/extension/reports/commissions/export','127.0.0.1','内网IP','{}',NULL,0,NULL,'2026-10-04 07:42:28',4),(146,'业务财务报表',5,'com.ruoyi.web.controller.business.BusinessExtensionController.export()','GET',1,'admin','默认部门','/business/extension/reports/lottery/export','127.0.0.1','内网IP','{}',NULL,0,NULL,'2026-10-04 07:42:28',4),(147,'业务财务报表',5,'com.ruoyi.web.controller.business.BusinessExtensionController.export()','GET',1,'admin','默认部门','/business/extension/reports/settlements/export','127.0.0.1','内网IP','{}',NULL,0,NULL,'2026-10-04 07:42:28',4),(148,'业务财务报表',5,'com.ruoyi.web.controller.business.BusinessExtensionController.export()','GET',1,'admin','默认部门','/business/extension/reports/redemptions/export','127.0.0.1','内网IP','{}',NULL,1,'PreparedStatementCallback; uncategorized SQLException for SQL [SELECT * FROM (SELECT \'抽奖奖品\' AS redemption_type, r.redemption_id AS id, CAST(r.reward_id AS CHAR) AS business_ref, r.member_id, r.store_id, r.verifier_member_id, r.status, r.request_id, r.create_time AS created_at FROM biz_redemption r UNION ALL SELECT \'商城自提\' AS redemption_type, p.pickup_redemption_id AS id, p.order_no AS business_ref, o.member_id, p.store_id, p.verifier_member_id, p.status, p.request_id, p.create_time AS created_at FROM biz_order_pickup_redemption p JOIN biz_order o ON o.order_id=p.order_id) redemption WHERE 1=1 ORDER BY id DESC]; SQL state [HY000]; error code [1271]; Illegal mix of collations for operation \'UNION\'; nested exception is java.sql.SQLException: Illegal mix of collations for operation \'UNION\'','2026-10-04 07:42:28',100),(149,'业务财务报表',5,'com.ruoyi.web.controller.business.BusinessExtensionController.export()','GET',1,'admin','默认部门','/business/extension/reports/videoRefunds/export','127.0.0.1','内网IP','{}',NULL,0,NULL,'2026-10-04 07:42:28',4),(150,'业务财务报表',5,'com.ruoyi.web.controller.business.BusinessExtensionController.export()','GET',1,'admin','默认部门','/business/extension/reports/redemptions/export','127.0.0.1','内网IP','{}',NULL,1,'PreparedStatementCallback; uncategorized SQLException for SQL [SELECT * FROM (SELECT \'抽奖奖品\' AS redemption_type, r.redemption_id AS id, CAST(r.reward_id AS CHAR) AS business_ref, r.member_id, r.store_id, r.verifier_member_id, r.status, r.request_id, r.create_time AS created_at FROM biz_redemption r UNION ALL SELECT \'商城自提\' AS redemption_type, p.pickup_redemption_id AS id, p.order_no AS business_ref, o.member_id, p.store_id, p.verifier_member_id, p.status, p.request_id, p.create_time AS created_at FROM biz_order_pickup_redemption p JOIN biz_order o ON o.order_id=p.order_id) redemption WHERE 1=1 ORDER BY id DESC]; SQL state [HY000]; error code [1271]; Illegal mix of collations for operation \'UNION\'; nested exception is java.sql.SQLException: Illegal mix of collations for operation \'UNION\'','2026-10-04 07:42:36',3),(151,'业务财务报表',5,'com.ruoyi.web.controller.business.BusinessExtensionController.export()','GET',1,'admin','默认部门','/business/extension/reports/orders/export','127.0.0.1','内网IP','{}',NULL,0,NULL,'2026-10-04 07:44:48',43),(152,'业务财务报表',5,'com.ruoyi.web.controller.business.BusinessExtensionController.export()','GET',1,'admin','默认部门','/business/extension/reports/commissions/export','127.0.0.1','内网IP','{}',NULL,0,NULL,'2026-10-04 07:44:48',8),(153,'业务财务报表',5,'com.ruoyi.web.controller.business.BusinessExtensionController.export()','GET',1,'admin','默认部门','/business/extension/reports/settlements/export','127.0.0.1','内网IP','{}',NULL,0,NULL,'2026-10-04 07:44:48',6),(154,'业务财务报表',5,'com.ruoyi.web.controller.business.BusinessExtensionController.export()','GET',1,'admin','默认部门','/business/extension/reports/lottery/export','127.0.0.1','内网IP','{}',NULL,0,NULL,'2026-10-04 07:44:48',6),(155,'业务财务报表',5,'com.ruoyi.web.controller.business.BusinessExtensionController.export()','GET',1,'admin','默认部门','/business/extension/reports/redemptions/export','127.0.0.1','内网IP','{}',NULL,0,NULL,'2026-10-04 07:44:48',10),(156,'业务财务报表',5,'com.ruoyi.web.controller.business.BusinessExtensionController.export()','GET',1,'admin','默认部门','/business/extension/reports/videoRefunds/export','127.0.0.1','内网IP','{}',NULL,0,NULL,'2026-10-04 07:44:48',6),(157,'业务财务报表',5,'com.ruoyi.web.controller.business.BusinessExtensionController.export()','GET',1,'admin','默认部门','/business/extension/reports/redemptions/export','127.0.0.1','内网IP','{\"fromDate\":\"2026-10-01\",\"toDate\":\"2026-10-04\"}',NULL,0,NULL,'2026-10-04 07:44:58',10),(158,'业务财务报表',5,'com.ruoyi.web.controller.business.BusinessExtensionController.export()','GET',1,'admin','默认部门','/business/extension/reports/orders/export','127.0.0.1','内网IP','{\"fromDate\":\"2026-01-01\",\"toDate\":\"2026-12-31\"}',NULL,0,NULL,'2026-10-04 08:08:52',14),(159,'业务财务报表',5,'com.ruoyi.web.controller.business.BusinessExtensionController.export()','GET',1,'admin','默认部门','/business/extension/reports/orders/export','127.0.0.1','内网IP','{}',NULL,0,NULL,'2026-10-04 08:23:23',15),(160,'业务财务报表',5,'com.ruoyi.web.controller.business.BusinessExtensionController.export()','GET',1,'admin','默认部门','/business/extension/reports/commissions/export','127.0.0.1','内网IP','{}',NULL,0,NULL,'2026-10-04 08:23:23',5),(161,'业务财务报表',5,'com.ruoyi.web.controller.business.BusinessExtensionController.export()','GET',1,'admin','默认部门','/business/extension/reports/settlements/export','127.0.0.1','内网IP','{}',NULL,0,NULL,'2026-10-04 08:23:23',4),(162,'业务财务报表',5,'com.ruoyi.web.controller.business.BusinessExtensionController.export()','GET',1,'admin','默认部门','/business/extension/reports/lottery/export','127.0.0.1','内网IP','{}',NULL,0,NULL,'2026-10-04 08:23:23',4),(163,'业务财务报表',5,'com.ruoyi.web.controller.business.BusinessExtensionController.export()','GET',1,'admin','默认部门','/business/extension/reports/redemptions/export','127.0.0.1','内网IP','{}',NULL,0,NULL,'2026-10-04 08:23:23',9),(164,'业务财务报表',5,'com.ruoyi.web.controller.business.BusinessExtensionController.export()','GET',1,'admin','默认部门','/business/extension/reports/videoRefunds/export','127.0.0.1','内网IP','{}',NULL,0,NULL,'2026-10-04 08:23:23',5),(165,'VIP社群活动',1,'com.ruoyi.web.controller.business.BusinessExtensionController.createSocialCampaign()','POST',1,'admin','默认部门','/business/extension/social/campaigns','127.0.0.1','内网IP','{\"campaignType\":\"promotion\",\"audience\":\"all\",\"content\":\"group channel verification\",\"title\":\"__verify_group_channel__\",\"channels\":\"group\"} ','{\"msg\":\"操作成功\",\"code\":200,\"data\":{\"campaignId\":1}}',0,NULL,'2026-10-04 09:17:38',14),(166,'VIP社群消息投递',0,'com.ruoyi.web.controller.business.BusinessExtensionController.sendSocialCampaign()','POST',1,'admin','默认部门','/business/extension/social/campaigns/1/send','127.0.0.1','内网IP','1 ','{\"msg\":\"该活动需要企业微信群，请先配置 CorpID、Secret 和目标群 ID\",\"code\":500}',0,NULL,'2026-10-04 09:17:38',4),(167,'VIP社群消息投递',0,'com.ruoyi.web.controller.business.BusinessExtensionController.sendSocialCampaign()','POST',1,'admin','默认部门','/business/extension/social/campaigns/1/send','127.0.0.1','内网IP','1 ','{\"msg\":\"该活动需要企业微信群，请先配置 CorpID、Secret 和目标群 ID\",\"code\":500}',0,NULL,'2026-10-04 09:18:04',4),(168,'VIP社群活动',1,'com.ruoyi.web.controller.business.BusinessExtensionController.createSocialCampaign()','POST',1,'admin','默认部门','/business/extension/social/campaigns','127.0.0.1','内网IP','{\"title\":\"__verify_group_channel__\",\"campaignType\":\"promotion\",\"content\":\"group channel verification\",\"audience\":\"all\",\"channels\":\"group\"} ','{\"msg\":\"操作成功\",\"code\":200,\"data\":{\"campaignId\":2}}',0,NULL,'2026-10-04 09:21:07',14),(169,'VIP社群消息投递',0,'com.ruoyi.web.controller.business.BusinessExtensionController.sendSocialCampaign()','POST',1,'admin','默认部门','/business/extension/social/campaigns/2/send','127.0.0.1','内网IP','2 ','{\"msg\":\"该活动需要企业微信群，请先配置 CorpID、Secret 和目标群 ID\",\"code\":503}',0,NULL,'2026-10-04 09:21:07',6),(170,'商品规格库存',2,'com.ruoyi.web.controller.business.BusinessExtensionController.saveProductSkus()','PUT',1,'admin','默认部门','/business/extension/products/1/skus','127.0.0.1','内网IP','1 {\"skus\":[{\"stock\":1,\"status\":\"0\",\"priceCent\":1,\"skuName\":\"临时规格\",\"specJson\":{\"size\":\"test\"},\"skuCode\":\"QA-REMOVE-20261004\"}]} ','{\"msg\":\"操作成功\",\"code\":200,\"data\":[{\"skuId\":1,\"productId\":1,\"skuCode\":\"QA-REMOVE-20261004\",\"skuName\":\"临时规格\",\"priceCent\":1,\"stock\":1,\"specJson\":\"{\\\"size\\\": \\\"test\\\"}\",\"status\":\"0\",\"externalSkuId\":\"\",\"videoSyncStatus\":\"pending\",\"videoLastError\":\"\"}]}',0,NULL,'2026-10-04 10:50:15',52),(171,'商品规格库存',2,'com.ruoyi.web.controller.business.BusinessExtensionController.saveProductSkus()','PUT',1,'admin','默认部门','/business/extension/products/1/skus','127.0.0.1','内网IP','1 {\"skus\":[]} ','{\"msg\":\"操作成功\",\"code\":200,\"data\":[]}',0,NULL,'2026-10-04 10:50:15',16),(172,'商品规格库存',2,'com.ruoyi.web.controller.business.BusinessExtensionController.saveProductSkus()','PUT',1,'admin','默认部门','/business/extension/products/1/skus','127.0.0.1','内网IP','1 {\"skus\":[{\"status\":\"0\",\"stock\":1,\"specJson\":{},\"skuName\":\"引用规格\",\"skuCode\":\"QA-REF-20261004\",\"priceCent\":1}]} ','{\"msg\":\"操作成功\",\"code\":200,\"data\":[{\"skuId\":2,\"productId\":1,\"skuCode\":\"QA-REF-20261004\",\"skuName\":\"引用规格\",\"priceCent\":1,\"stock\":1,\"specJson\":\"{}\",\"status\":\"0\",\"externalSkuId\":\"\",\"videoSyncStatus\":\"pending\",\"videoLastError\":\"\"}]}',0,NULL,'2026-10-04 10:50:51',14),(173,'商品规格库存',2,'com.ruoyi.web.controller.business.BusinessExtensionController.saveProductSkus()','PUT',1,'admin','默认部门','/business/extension/products/1/skus','127.0.0.1','内网IP','1 {\"skus\":[]} ','{\"msg\":\"操作成功\",\"code\":200,\"data\":[{\"skuId\":2,\"productId\":1,\"skuCode\":\"QA-REF-20261004\",\"skuName\":\"引用规格\",\"priceCent\":1,\"stock\":0,\"specJson\":\"{}\",\"status\":\"1\",\"externalSkuId\":\"\",\"videoSyncStatus\":\"pending\",\"videoLastError\":\"\"}]}',0,NULL,'2026-10-04 10:50:52',14),(174,'商品规格库存',2,'com.ruoyi.web.controller.business.BusinessExtensionController.saveProductSkus()','PUT',1,'admin','默认部门','/business/extension/products/1/skus','127.0.0.1','内网IP','1 {\"skus\":[]} ','{\"msg\":\"操作成功\",\"code\":200,\"data\":[]}',0,NULL,'2026-10-04 10:50:52',12),(175,'业务财务报表',5,'com.ruoyi.web.controller.business.BusinessExtensionController.export()','GET',1,'admin','默认部门','/business/extension/reports/orders/export','127.0.0.1','内网IP','{}',NULL,0,NULL,'2026-10-04 11:44:15',14),(176,'业务财务报表',5,'com.ruoyi.web.controller.business.BusinessExtensionController.export()','GET',1,'admin','默认部门','/business/extension/reports/commission/export','127.0.0.1','内网IP','{}',NULL,0,NULL,'2026-10-04 11:44:15',0),(177,'业务财务报表',5,'com.ruoyi.web.controller.business.BusinessExtensionController.export()','GET',1,'admin','默认部门','/business/extension/reports/redemption/export','127.0.0.1','内网IP','{}',NULL,0,NULL,'2026-10-04 11:44:15',0),(178,'业务财务报表',5,'com.ruoyi.web.controller.business.BusinessExtensionController.export()','GET',1,'admin','默认部门','/business/extension/reports/orders/export','127.0.0.1','内网IP','{\"fromDate\":\"2026-01-01\",\"toDate\":\"2026-12-31\"}',NULL,0,NULL,'2026-10-04 11:45:03',8),(179,'业务财务报表',5,'com.ruoyi.web.controller.business.BusinessExtensionController.export()','GET',1,'admin','默认部门','/business/extension/reports/commissions/export','127.0.0.1','内网IP','{\"fromDate\":\"2026-01-01\",\"toDate\":\"2026-12-31\"}',NULL,0,NULL,'2026-10-04 11:45:03',5),(180,'业务财务报表',5,'com.ruoyi.web.controller.business.BusinessExtensionController.export()','GET',1,'admin','默认部门','/business/extension/reports/settlements/export','127.0.0.1','内网IP','{\"fromDate\":\"2026-01-01\",\"toDate\":\"2026-12-31\"}',NULL,0,NULL,'2026-10-04 11:45:03',5),(181,'业务财务报表',5,'com.ruoyi.web.controller.business.BusinessExtensionController.export()','GET',1,'admin','默认部门','/business/extension/reports/lottery/export','127.0.0.1','内网IP','{\"fromDate\":\"2026-01-01\",\"toDate\":\"2026-12-31\"}',NULL,0,NULL,'2026-10-04 11:45:03',5),(182,'业务财务报表',5,'com.ruoyi.web.controller.business.BusinessExtensionController.export()','GET',1,'admin','默认部门','/business/extension/reports/redemptions/export','127.0.0.1','内网IP','{\"fromDate\":\"2026-01-01\",\"toDate\":\"2026-12-31\"}',NULL,0,NULL,'2026-10-04 11:45:03',10),(183,'业务财务报表',5,'com.ruoyi.web.controller.business.BusinessExtensionController.export()','GET',1,'admin','默认部门','/business/extension/reports/videoRefunds/export','127.0.0.1','内网IP','{\"fromDate\":\"2026-01-01\",\"toDate\":\"2026-12-31\"}',NULL,0,NULL,'2026-10-04 11:45:03',5),(184,'订单物流',2,'com.ruoyi.web.controller.business.BusinessAdminController.updateOrderDelivery()','PUT',1,'admin','默认部门','/business/order/1/delivery','127.0.0.1','内网IP','\"1\" {\"trackingNo\":\"SFTEST\",\"company\":\"顺丰\"} ','{\"msg\":\"当前订单状态不支持发货\",\"code\":500}',0,NULL,'2026-10-04 12:05:43',10),(185,'订单物流',2,'com.ruoyi.web.controller.business.BusinessAdminController.updateOrderDelivery()','PUT',1,'admin','默认部门','/business/order/3/delivery','127.0.0.1','内网IP','\"3\" {\"trackingNo\":\"TEST-20261004\",\"lastTrace\":\"Local regression trace\",\"company\":\"LocalTestCarrier\"} ','{\"msg\":\"操作成功\",\"code\":200,\"data\":{\"company\":\"LocalTestCarrier\",\"trackingNo\":\"TEST-20261004\",\"status\":\"shipped\",\"lastTrace\":\"[{\\\"time\\\": \\\"2026-10-04T12:10:04.387306700+08:00\\\", \\\"description\\\": \\\"Local regression trace\\\"}]\",\"shippedAt\":\"2026-10-04 12:10:04\"}}',0,NULL,'2026-10-04 12:10:04',15),(186,'业务状态',2,'com.ruoyi.web.controller.business.BusinessAdminController.status()','PUT',1,'admin','默认部门','/business/order/3/status','127.0.0.1','内网IP','\"order\" \"3\" {\"status\":\"shipping\"} ','{\"msg\":\"请先填写快递公司和物流单号，再推进配送状态\",\"code\":500}',0,NULL,'2026-10-04 12:15:39',17),(187,'退款售后审核',2,'com.ruoyi.web.controller.business.BusinessExtensionController.refundStatus()','PUT',1,'admin','默认部门','/business/extension/refunds/1/status','127.0.0.1','内网IP','1 {\"status\":\"approved\"} ','{\"msg\":\"操作成功\",\"code\":200}',0,NULL,'2026-10-04 13:30:22',10),(188,'退款售后审核',2,'com.ruoyi.web.controller.business.BusinessExtensionController.refundStatus()','PUT',1,'admin','默认部门','/business/extension/refunds/1/status','127.0.0.1','内网IP','1 {\"status\":\"refunded\"} ','{\"msg\":\"操作成功\",\"code\":200}',0,NULL,'2026-10-04 13:30:22',24),(189,'业务状态',2,'com.ruoyi.web.controller.business.BusinessAdminController.status()','PUT',1,'admin','默认部门','/business/reward/3/status','127.0.0.1','内网IP','\"reward\" \"3\" {\"status\":\"received\"} ','{\"msg\":\"现金红包只能由微信转账查单或回调更新到账状态\",\"code\":500}',0,NULL,'2026-10-04 15:43:53',5),(190,'业务状态',2,'com.ruoyi.web.controller.business.BusinessAdminController.status()','PUT',1,'admin','默认部门','/business/reward/4/status','127.0.0.1','内网IP','\"reward\" \"4\" {\"status\":\"received\"} ','{\"msg\":\"现金红包只能由微信转账查单或回调更新到账状态\",\"code\":409}',0,NULL,'2026-10-04 15:45:34',8),(191,'退款售后审核',2,'com.ruoyi.web.controller.business.BusinessExtensionController.refundStatus()','PUT',1,'admin','默认部门','/business/extension/refunds/2/status','127.0.0.1','内网IP','2 {\"status\":\"approved\"} ','{\"msg\":\"操作成功\",\"code\":200}',0,NULL,'2026-10-04 17:54:16',21),(192,'退款售后审核',2,'com.ruoyi.web.controller.business.BusinessExtensionController.refundStatus()','PUT',1,'admin','默认部门','/business/extension/refunds/3/status','127.0.0.1','内网IP','3 {\"status\":\"approved\"} ','{\"msg\":\"操作成功\",\"code\":200}',0,NULL,'2026-10-04 17:54:16',12),(193,'退款售后审核',2,'com.ruoyi.web.controller.business.BusinessExtensionController.refundStatus()','PUT',1,'admin','默认部门','/business/extension/refunds/2/status','127.0.0.1','内网IP','2 {\"status\":\"refunded\"} ','{\"msg\":\"操作成功\",\"code\":200}',0,NULL,'2026-10-04 17:54:16',37),(194,'退款售后审核',2,'com.ruoyi.web.controller.business.BusinessExtensionController.refundStatus()','PUT',1,'admin','默认部门','/business/extension/refunds/3/status','127.0.0.1','内网IP','3 {\"status\":\"rejected\"} ','{\"msg\":\"退款状态不能变更\",\"code\":500}',0,NULL,'2026-10-04 17:54:16',3),(195,'退款售后审核',2,'com.ruoyi.web.controller.business.BusinessExtensionController.refundStatus()','PUT',1,'admin','默认部门','/business/extension/refunds/4/status','127.0.0.1','内网IP','4 {\"status\":\"approved\"} ','{\"msg\":\"操作成功\",\"code\":200}',0,NULL,'2026-10-04 17:54:53',10),(196,'退款售后审核',2,'com.ruoyi.web.controller.business.BusinessExtensionController.refundStatus()','PUT',1,'admin','默认部门','/business/extension/refunds/4/status','127.0.0.1','内网IP','4 {\"status\":\"refunded\"} ','{\"msg\":\"操作成功\",\"code\":200}',0,NULL,'2026-10-04 17:54:53',29),(197,'退款售后审核',2,'com.ruoyi.web.controller.business.BusinessExtensionController.refundStatus()','PUT',1,'admin','默认部门','/business/extension/refunds/5/status','127.0.0.1','内网IP','5 {\"status\":\"rejected\"} ','{\"msg\":\"操作成功\",\"code\":200}',0,NULL,'2026-10-04 17:54:53',9),(198,'退款售后审核',2,'com.ruoyi.web.controller.business.BusinessExtensionController.refundStatus()','PUT',1,'admin','默认部门','/business/extension/refunds/6/status','127.0.0.1','内网IP','6 {\"status\":\"approved\"} ','{\"msg\":\"操作成功\",\"code\":200}',0,NULL,'2026-10-04 18:19:00',24),(199,'退款售后审核',2,'com.ruoyi.web.controller.business.BusinessExtensionController.refundStatus()','PUT',1,'admin','默认部门','/business/extension/refunds/6/status','127.0.0.1','内网IP','6 {\"status\":\"refunded\"} ','{\"msg\":\"操作成功\",\"code\":200}',0,NULL,'2026-10-04 18:19:00',42),(200,'佣金提现审核',2,'com.ruoyi.web.controller.business.BusinessExtensionController.updatePromotionSettlement()','PUT',1,'admin','默认部门','/business/extension/promotion/settlements/1/status','127.0.0.1','内网IP','1 {\"status\":\"processing\"} ','{\"msg\":\"操作成功\",\"code\":200}',0,NULL,'2026-10-04 18:19:00',4),(201,'佣金提现审核',2,'com.ruoyi.web.controller.business.BusinessExtensionController.updatePromotionSettlement()','PUT',1,'admin','默认部门','/business/extension/promotion/settlements/1/status','127.0.0.1','内网IP','1 {\"status\":\"paid\"} ','{\"msg\":\"操作成功\",\"code\":200}',0,NULL,'2026-10-04 18:19:00',12),(202,'佣金提现审核',2,'com.ruoyi.web.controller.business.BusinessExtensionController.updatePromotionSettlement()','PUT',1,'admin','默认部门','/business/extension/promotion/settlements/2/status','127.0.0.1','内网IP','2 {\"status\":\"failed\"} ','{\"msg\":\"操作成功\",\"code\":200}',0,NULL,'2026-10-04 18:26:41',13),(203,'分销规则',2,'com.ruoyi.web.controller.business.BusinessExtensionController.updatePromotionConfig()','PUT',1,'admin','默认部门','/business/extension/promotion/config','127.0.0.1','内网IP','{\"settlementDays\":7,\"customerRateBp\":1000,\"maxLevel\":3,\"staffRateBp\":1500,\"status\":\"0\",\"levelRatesJson\":\"[1000,500,200]\",\"configId\":1,\"refundPolicy\":\"reverse\",\"minWithdrawCent\":0} ','{\"msg\":\"最低提现金额必须大于0分\",\"code\":500}',0,NULL,'2026-10-04 19:46:53',20),(204,'业务财务报表',5,'com.ruoyi.web.controller.business.BusinessExtensionController.export()','GET',1,'admin','默认部门','/business/extension/reports/orders/export','127.0.0.1','内网IP','{\"fromDate\":\"2026-01-01\",\"toDate\":\"2026-12-31\"}',NULL,0,NULL,'2026-10-04 21:03:36',21),(205,'业务财务报表',5,'com.ruoyi.web.controller.business.BusinessExtensionController.export()','GET',1,'admin','默认部门','/business/extension/reports/commissions/export','127.0.0.1','内网IP','{\"fromDate\":\"2026-01-01\",\"toDate\":\"2026-12-31\"}',NULL,0,NULL,'2026-10-04 21:03:36',6),(206,'业务财务报表',5,'com.ruoyi.web.controller.business.BusinessExtensionController.export()','GET',1,'admin','默认部门','/business/extension/reports/settlements/export','127.0.0.1','内网IP','{\"fromDate\":\"2026-01-01\",\"toDate\":\"2026-12-31\"}',NULL,0,NULL,'2026-10-04 21:03:36',6),(207,'业务财务报表',5,'com.ruoyi.web.controller.business.BusinessExtensionController.export()','GET',1,'admin','默认部门','/business/extension/reports/lottery/export','127.0.0.1','内网IP','{\"fromDate\":\"2026-01-01\",\"toDate\":\"2026-12-31\"}',NULL,0,NULL,'2026-10-04 21:03:36',9),(208,'业务财务报表',5,'com.ruoyi.web.controller.business.BusinessExtensionController.export()','GET',1,'admin','默认部门','/business/extension/reports/redemptions/export','127.0.0.1','内网IP','{\"fromDate\":\"2026-01-01\",\"toDate\":\"2026-12-31\"}',NULL,0,NULL,'2026-10-04 21:03:36',13),(209,'业务财务报表',5,'com.ruoyi.web.controller.business.BusinessExtensionController.export()','GET',1,'admin','默认部门','/business/extension/reports/videoRefunds/export','127.0.0.1','内网IP','{\"fromDate\":\"2026-01-01\",\"toDate\":\"2026-12-31\"}',NULL,0,NULL,'2026-10-04 21:03:36',8),(210,'用户管理',1,'com.ruoyi.web.controller.system.SysUserController.add()','POST',1,'admin','默认部门','/system/user','127.0.0.1','内网IP','{\"admin\":false,\"createBy\":\"admin\",\"deptId\":101,\"nickName\":\"权限边界临时账号\",\"params\":{},\"remark\":\"temporary scope guard\",\"roleIds\":[20,30],\"sex\":\"1\",\"status\":\"0\",\"userId\":116,\"userName\":\"qa_scope_guard_20261004211909\"} ','{\"msg\":\"操作成功\",\"code\":200}',0,NULL,'2026-10-04 21:19:09',162),(211,'用户管理',3,'com.ruoyi.web.controller.system.SysUserController.remove()','DELETE',1,'admin','默认部门','/system/user/0','127.0.0.1','内网IP','[0] ','{\"msg\":\"操作失败\",\"code\":500}',0,NULL,'2026-10-04 21:19:09',17),(212,'抽奖码生成',1,'com.ruoyi.web.controller.business.BusinessExtensionController.generateCodes()','POST',1,'admin','默认部门','/business/extension/lottery/codes','127.0.0.1','内网IP','{\"activityCode\":\"JS-DEMO-ACTIVITY\",\"count\":100} ',NULL,1,'微信官方推广码生成失败：invalid page rid: 6ac26db3-7dc2ebea-450f6af2','2026-10-04 23:16:06',1302),(213,'积分规则',2,'com.ruoyi.web.controller.business.BusinessExtensionController.updatePointRule()','PUT',1,'admin','默认部门','/business/extension/point-rules','127.0.0.1','内网IP','{\"ruleId\":1,\"dailyLimit\":-1,\"ruleName\":\"\",\"status\":\"0\",\"ruleJson\":\"{}\",\"points\":1,\"validityDays\":0} ','{\"msg\":\"积分规则名称不能为空且最多128字\",\"code\":500}',0,NULL,'2026-10-04 23:43:19',3),(214,'销冠排行规则',2,'com.ruoyi.web.controller.business.BusinessExtensionController.updateRankingConfig()','PUT',1,'admin','默认部门','/business/extension/ranking/config','127.0.0.1','内网IP','{\"configId\":1,\"periodType\":\"year\",\"rewardJson\":\"[]\",\"status\":\"0\"} ','{\"msg\":\"排行榜周期无效\",\"code\":500}',0,NULL,'2026-10-04 23:43:19',0),(215,'业务财务报表',5,'com.ruoyi.web.controller.business.BusinessExtensionController.export()','GET',1,'admin','默认部门','/business/extension/reports/rankingRewards/export','127.0.0.1','内网IP','{}',NULL,0,NULL,'2026-10-04 23:48:17',18),(216,'业务财务报表',5,'com.ruoyi.web.controller.business.BusinessExtensionController.export()','GET',1,'admin','默认部门','/business/extension/reports/rankingRewards/export','127.0.0.1','内网IP','{}',NULL,0,NULL,'2026-10-04 23:48:28',6),(217,'业务财务报表',5,'com.ruoyi.web.controller.business.BusinessExtensionController.export()','GET',1,'admin','默认部门','/business/extension/reports/rankingRewards/export','127.0.0.1','内网IP','{}',NULL,0,NULL,'2026-10-04 23:49:56',7),(218,'抽奖活动配置',2,'com.ruoyi.web.controller.business.BusinessExtensionController.updateLotteryActivity()','PUT',1,'admin','默认部门','/business/extension/lottery/activity','127.0.0.1','内网IP','{\"ruleJson\":\"{}\",\"status\":\"0\",\"dailyLimit\":1,\"totalLimit\":-1,\"endTime\":\"\",\"activityName\":\"金色酒业扫码有礼\",\"startTime\":\"not-a-date\",\"activityCode\":\"JS-DEMO-ACTIVITY\"} ','{\"msg\":\"活动开始时间格式无效，应为 YYYY-MM-DD HH:mm:ss\",\"code\":500}',0,NULL,'2026-10-05 00:01:34',6),(219,'VIP社群活动',1,'com.ruoyi.web.controller.business.BusinessExtensionController.createSocialCampaign()','POST',1,'admin','默认部门','/business/extension/social/campaigns','127.0.0.1','内网IP','{\"audience\":\"vip\",\"campaignType\":\"promotion\",\"title\":\"QA_TEMP_SOCIAL\",\"channels\":\"group\",\"content\":\"QA temporary campaign\"} ','{\"msg\":\"操作成功\",\"code\":200,\"data\":{\"campaignId\":3}}',0,NULL,'2026-10-05 06:44:17',26),(220,'VIP社群活动',1,'com.ruoyi.web.controller.business.BusinessExtensionController.createSocialCampaign()','POST',1,'admin','默认部门','/business/extension/social/campaigns','127.0.0.1','内网IP','{\"title\":\"validation-check\",\"scheduledAt\":\"not-a-time\",\"audience\":\"all\",\"content\":\"validation-check\",\"campaignType\":\"promotion\",\"channels\":\"subscribe\"} ','{\"msg\":\"计划发送时间格式无效，应为 YYYY-MM-DD HH:mm:ss\",\"code\":500}',0,NULL,'2026-10-05 07:27:30',8),(221,'业务财务报表',5,'com.ruoyi.web.controller.business.BusinessExtensionController.export()','GET',1,'admin','默认部门','/business/extension/reports/order/export','127.0.0.1','内网IP','{}',NULL,0,NULL,'2026-10-05 08:51:50',5),(222,'业务财务报表',5,'com.ruoyi.web.controller.business.BusinessExtensionController.export()','GET',1,'admin','默认部门','/business/extension/reports/order/export','127.0.0.1','内网IP','{\"fromDate\":\"2026-01-01\",\"toDate\":\"2026-12-31\"}',NULL,0,NULL,'2026-10-05 08:52:02',1),(223,'业务财务报表',5,'com.ruoyi.web.controller.business.BusinessExtensionController.export()','GET',1,'admin','默认部门','/business/extension/reports/orders/export','127.0.0.1','内网IP','{\"fromDate\":\"2026-01-01\",\"toDate\":\"2026-12-31\"}',NULL,0,NULL,'2026-10-05 08:52:29',12),(224,'业务财务报表',5,'com.ruoyi.web.controller.business.BusinessExtensionController.export()','GET',1,'admin','默认部门','/business/extension/reports/orders/export','127.0.0.1','内网IP','{\"fromDate\":\"2026-10-01\",\"toDate\":\"2026-10-05\"}',NULL,0,NULL,'2026-10-05 09:01:11',7),(225,'业务财务报表',5,'com.ruoyi.web.controller.business.BusinessExtensionController.export()','GET',1,'admin','默认部门','/business/extension/reports/orders/export','127.0.0.1','内网IP','{\"fromDate\":\"2026-01-01\",\"toDate\":\"2026-12-31\"}',NULL,0,NULL,'2026-10-05 10:08:30',19),(226,'业务财务报表',5,'com.ruoyi.web.controller.business.BusinessExtensionController.export()','GET',1,'admin','默认部门','/business/extension/reports/commissions/export','127.0.0.1','内网IP','{\"fromDate\":\"2026-01-01\",\"toDate\":\"2026-12-31\"}',NULL,0,NULL,'2026-10-05 10:08:30',6),(227,'业务财务报表',5,'com.ruoyi.web.controller.business.BusinessExtensionController.export()','GET',1,'admin','默认部门','/business/extension/reports/settlements/export','127.0.0.1','内网IP','{\"fromDate\":\"2026-01-01\",\"toDate\":\"2026-12-31\"}',NULL,0,NULL,'2026-10-05 10:08:30',7),(228,'业务财务报表',5,'com.ruoyi.web.controller.business.BusinessExtensionController.export()','GET',1,'admin','默认部门','/business/extension/reports/lottery/export','127.0.0.1','内网IP','{\"fromDate\":\"2026-01-01\",\"toDate\":\"2026-12-31\"}',NULL,0,NULL,'2026-10-05 10:08:30',5),(229,'业务财务报表',5,'com.ruoyi.web.controller.business.BusinessExtensionController.export()','GET',1,'admin','默认部门','/business/extension/reports/redemptions/export','127.0.0.1','内网IP','{\"fromDate\":\"2026-01-01\",\"toDate\":\"2026-12-31\"}',NULL,0,NULL,'2026-10-05 10:08:30',15),(230,'业务财务报表',5,'com.ruoyi.web.controller.business.BusinessExtensionController.export()','GET',1,'admin','默认部门','/business/extension/reports/orders/export','127.0.0.1','内网IP','{}',NULL,0,NULL,'2026-10-05 10:39:52',19),(231,'业务财务报表',5,'com.ruoyi.web.controller.business.BusinessExtensionController.export()','GET',1,'admin','默认部门','/business/extension/reports/commissions/export','127.0.0.1','内网IP','{}',NULL,0,NULL,'2026-10-05 10:39:52',8),(232,'业务财务报表',5,'com.ruoyi.web.controller.business.BusinessExtensionController.export()','GET',1,'admin','默认部门','/business/extension/reports/lottery/export','127.0.0.1','内网IP','{}',NULL,0,NULL,'2026-10-05 10:39:52',5),(233,'业务财务报表',5,'com.ruoyi.web.controller.business.BusinessExtensionController.export()','GET',1,'admin','默认部门','/business/extension/reports/redemptions/export','127.0.0.1','内网IP','{}',NULL,0,NULL,'2026-10-05 10:39:52',14),(234,'业务财务报表',5,'com.ruoyi.web.controller.business.BusinessExtensionController.export()','GET',1,'admin','默认部门','/business/extension/reports/videoRefunds/export','127.0.0.1','内网IP','{}',NULL,0,NULL,'2026-10-05 10:39:52',5),(235,'业务财务报表',5,'com.ruoyi.web.controller.business.BusinessExtensionController.export()','GET',1,'admin','默认部门','/business/extension/reports/orders/export','127.0.0.1','内网IP','{\"fromDate\":\"2026-10-01\",\"toDate\":\"2026-10-05\"}',NULL,0,NULL,'2026-10-05 10:50:41',10),(236,'业务财务报表',5,'com.ruoyi.web.controller.business.BusinessExtensionController.export()','GET',1,'admin','默认部门','/business/extension/reports/orders/export','127.0.0.1','内网IP','{\"fromDate\":\"2026-10-01\",\"toDate\":\"2026-10-05\"}',NULL,0,NULL,'2026-10-05 10:56:15',9),(237,'业务财务报表',5,'com.ruoyi.web.controller.business.BusinessExtensionController.export()','GET',1,'admin','默认部门','/business/extension/reports/orders/export','127.0.0.1','内网IP','{\"fromDate\":\"2026-10-01\",\"toDate\":\"2026-10-05\"}',NULL,0,NULL,'2026-10-05 11:30:03',23),(238,'业务财务报表',5,'com.ruoyi.web.controller.business.BusinessExtensionController.export()','GET',1,'admin','默认部门','/business/extension/reports/orders/export','127.0.0.1','内网IP','{\"fromDate\":\"2026-10-01\",\"toDate\":\"2026-10-05\"}',NULL,0,NULL,'2026-10-05 11:59:13',28),(239,'企业微信群二维码',2,'com.ruoyi.web.controller.business.BusinessExtensionController.groupQr()','POST',1,'admin','默认部门','/business/extension/group-qr','127.0.0.1','内网IP','{\"chatIds\":\"\"} ','{\"msg\":\"请填写1至5个已创建的企业微信群 ID\",\"code\":500}',0,NULL,'2026-10-05 12:15:56',1),(240,'企业微信群二维码',2,'com.ruoyi.web.controller.business.BusinessExtensionController.groupQr()','POST',1,'admin','默认部门','/business/extension/group-qr','127.0.0.1','内网IP','{\"chatIds\":\"wxyz123\"} ','{\"msg\":\"企业微信 CorpID/Secret 未在服务端配置\",\"code\":503}',0,NULL,'2026-10-05 12:22:14',14),(241,'微信小店授权校验',0,'com.ruoyi.web.controller.business.BusinessExtensionController.checkVideoShop()','POST',1,'admin','默认部门','/business/extension/video/check','127.0.0.1','内网IP','','{\"msg\":\"微信小店授权校验失败：api unauthorized rid: 6ac325f6-21c26366-587b8d32\",\"code\":500}',0,NULL,'2026-10-05 12:22:15',1216),(242,'微信小店授权校验',0,'com.ruoyi.web.controller.business.BusinessExtensionController.checkVideoShop()','POST',1,'admin','默认部门','/business/extension/video/check','127.0.0.1','内网IP','','{\"msg\":\"微信小店授权校验失败：api unauthorized rid: 6ac32677-05321b66-371a96ce\",\"code\":422}',0,NULL,'2026-10-05 12:24:25',998),(243,'销冠排行规则',2,'com.ruoyi.web.controller.business.BusinessExtensionController.updateRankingConfig()','PUT',1,'admin','默认部门','/business/extension/ranking/config','127.0.0.1','内网IP','{\"configId\":1,\"status\":\"0\",\"periodType\":\"week\",\"rewardJson\":\"[]\",\"startTime\":\"bad\"} ','{\"msg\":\"排行榜开始时间格式无效，应为 YYYY-MM-DD HH:mm:ss\",\"code\":400}',0,NULL,'2026-10-05 12:27:05',23),(244,'微信渠道配置',2,'com.ruoyi.web.controller.business.BusinessExtensionController.updateChannel()','PUT',1,'admin','默认部门','/business/extension/channel','127.0.0.1','内网IP','{\"wechat.mini.socialTemplateId\":\"local-test-template\"} ','{\"msg\":\"操作成功\",\"code\":200}',0,NULL,'2026-10-05 12:36:12',18),(245,'VIP社群活动',1,'com.ruoyi.web.controller.business.BusinessExtensionController.createSocialCampaign()','POST',1,'admin','默认部门','/business/extension/social/campaigns','127.0.0.1','内网IP','{\"campaignType\":\"promotion\",\"title\":\"临时闭环测试\",\"content\":\"无目标受众状态校验\",\"audience\":\"vip\",\"channels\":\"subscribe\"} ','{\"msg\":\"操作成功\",\"code\":200,\"data\":{\"campaignId\":4}}',0,NULL,'2026-10-05 12:36:12',15),(246,'VIP社群消息投递',0,'com.ruoyi.web.controller.business.BusinessExtensionController.sendSocialCampaign()','POST',1,'admin','默认部门','/business/extension/social/campaigns/4/send','127.0.0.1','内网IP','4 ','{\"msg\":\"没有找到符合受众范围的订阅会员或可用企业微信群\",\"code\":409,\"data\":{\"queued\":0,\"subscribeQueued\":0,\"groupQueued\":0,\"message\":\"没有找到符合受众范围的订阅会员或可用企业微信群\"}}',0,NULL,'2026-10-05 12:36:12',25),(247,'微信渠道配置',2,'com.ruoyi.web.controller.business.BusinessExtensionController.updateChannel()','PUT',1,'admin','默认部门','/business/extension/channel','127.0.0.1','内网IP','{\"wechat.mini.socialTemplateId\":\"\"} ','{\"msg\":\"操作成功\",\"code\":200}',0,NULL,'2026-10-05 12:36:13',5),(248,'VIP社群活动',1,'com.ruoyi.web.controller.business.BusinessExtensionController.createSocialCampaign()','POST',1,'admin','默认部门','/business/extension/social/campaigns','127.0.0.1','内网IP','{\"channels\":\"subscribe\",\"content\":\"temporary regression\",\"title\":\"codex-temp-campaign\",\"campaignType\":\"promotion\",\"audience\":\"vip\"} ','{\"msg\":\"操作成功\",\"code\":200,\"data\":{\"campaignId\":5}}',0,NULL,'2026-10-05 12:44:44',5),(249,'VIP社群消息投递',0,'com.ruoyi.web.controller.business.BusinessExtensionController.sendSocialCampaign()','POST',1,'admin','默认部门','/business/extension/social/campaigns/5/send','127.0.0.1','内网IP','5 ','{\"msg\":\"没有找到符合受众范围的订阅会员或可用企业微信群\",\"code\":409,\"data\":{\"queued\":0,\"subscribeQueued\":0,\"groupQueued\":0,\"message\":\"没有找到符合受众范围的订阅会员或可用企业微信群\"}}',0,NULL,'2026-10-05 12:44:44',13),(250,'业务财务报表',5,'com.ruoyi.web.controller.business.BusinessExtensionController.export()','GET',1,'admin','默认部门','/business/extension/reports/orders/export','127.0.0.1','内网IP','{\"fromDate\":\"2026-10-01\",\"toDate\":\"2026-10-05\"}',NULL,0,NULL,'2026-10-05 14:24:17',9),(251,'业务财务报表',5,'com.ruoyi.web.controller.business.BusinessExtensionController.export()','GET',1,'admin','默认部门','/business/extension/reports/orders/export','127.0.0.1','内网IP','{\"fromDate\":\"2026-10-01\",\"toDate\":\"2026-10-05\"}',NULL,0,NULL,'2026-10-05 14:24:42',6),(252,'佣金提现审核',2,'com.ruoyi.web.controller.business.BusinessExtensionController.updatePromotionSettlement()','PUT',1,'admin','默认部门','/business/extension/promotion/settlements/4/status','127.0.0.1','内网IP','4 {\"status\":\"paid\",\"paymentReference\":\" \"} ','{\"msg\":\"确认付款必须填写有效的转账凭证号（最多128字）\",\"code\":500}',0,NULL,'2026-10-05 17:07:15',8),(253,'佣金提现审核',2,'com.ruoyi.web.controller.business.BusinessExtensionController.updatePromotionSettlement()','PUT',1,'admin','默认部门','/business/extension/promotion/settlements/3/status','127.0.0.1','内网IP','3 {\"status\":\"failed\",\"failureReason\":\"  \"} ','{\"msg\":\"退回提现必须填写原因（最多500字）\",\"code\":500}',0,NULL,'2026-10-05 17:07:15',3),(254,'佣金提现审核',2,'com.ruoyi.web.controller.business.BusinessExtensionController.updatePromotionSettlement()','PUT',1,'admin','默认部门','/business/extension/promotion/settlements/3/status','127.0.0.1','内网IP','3 {\"status\":\"failed\",\"failureReason\":\"codex回归测试退回\"} ','{\"msg\":\"操作成功\",\"code\":200}',0,NULL,'2026-10-05 17:07:15',12),(255,'佣金提现审核',2,'com.ruoyi.web.controller.business.BusinessExtensionController.updatePromotionSettlement()','PUT',1,'admin','默认部门','/business/extension/promotion/settlements/4/status','127.0.0.1','内网IP','4 {\"status\":\"paid\",\"paymentReference\":\"TEST-f65417065a9b41f987ae5decfeb7cdc6\"} ','{\"msg\":\"操作成功\",\"code\":200}',0,NULL,'2026-10-05 17:07:15',15),(256,'业务财务报表',5,'com.ruoyi.web.controller.business.BusinessExtensionController.export()','GET',1,'admin','默认部门','/business/extension/reports/settlements/export','127.0.0.1','内网IP','{}',NULL,0,NULL,'2026-10-05 17:08:05',12),(257,'业务财务报表',5,'com.ruoyi.web.controller.business.BusinessExtensionController.export()','GET',1,'admin','默认部门','/business/extension/reports/settlements/export','127.0.0.1','内网IP','{}',NULL,0,NULL,'2026-10-05 17:08:17',7),(258,'业务财务报表',5,'com.ruoyi.web.controller.business.BusinessExtensionController.export()','GET',1,'admin','默认部门','/business/extension/reports/settlements/export','127.0.0.1','内网IP','{}',NULL,0,NULL,'2026-10-05 17:08:58',9),(259,'用户管理',1,'com.ruoyi.web.controller.system.SysUserController.add()','POST',1,'admin','默认部门','/system/user','127.0.0.1','内网IP','{\"admin\":false,\"createBy\":\"admin\",\"deptId\":101,\"nickName\":\"财务人员\",\"params\":{},\"remark\":\"本地业务权限验收账号\",\"roleIds\":[10],\"sex\":\"1\",\"status\":\"0\",\"userId\":117,\"userName\":\"finance\"} ','{\"msg\":\"操作成功\",\"code\":200}',0,NULL,'2026-10-05 17:21:38',132),(260,'用户管理',1,'com.ruoyi.web.controller.system.SysUserController.add()','POST',1,'admin','默认部门','/system/user','127.0.0.1','内网IP','{\"admin\":false,\"createBy\":\"admin\",\"deptId\":101,\"nickName\":\"运营人员\",\"params\":{},\"remark\":\"本地业务权限验收账号\",\"roleIds\":[20],\"sex\":\"1\",\"status\":\"0\",\"userId\":118,\"userName\":\"operator\"} ','{\"msg\":\"操作成功\",\"code\":200}',0,NULL,'2026-10-05 17:21:39',113),(261,'用户管理',1,'com.ruoyi.web.controller.system.SysUserController.add()','POST',1,'admin','默认部门','/system/user','127.0.0.1','内网IP','{\"admin\":false,\"createBy\":\"admin\",\"deptId\":101,\"nickName\":\"核销员\",\"params\":{},\"remark\":\"本地业务权限验收账号\",\"roleIds\":[30],\"sex\":\"1\",\"status\":\"0\",\"userId\":119,\"userName\":\"verifier\"} ','{\"msg\":\"操作成功\",\"code\":200}',0,NULL,'2026-10-05 17:21:39',120),(262,'业务财务报表',5,'com.ruoyi.web.controller.business.BusinessExtensionController.export()','GET',1,'finance','默认部门','/business/extension/reports/settlements/export','127.0.0.1','内网IP','{}',NULL,0,NULL,'2026-10-05 17:22:18',6),(263,'业务财务报表',5,'com.ruoyi.web.controller.business.BusinessExtensionController.export()','GET',1,'finance','默认部门','/business/extension/reports/settlements/export','127.0.0.1','内网IP','{}',NULL,0,NULL,'2026-10-05 17:23:08',5),(264,'业务状态',2,'com.ruoyi.web.controller.business.BusinessAdminController.status()','PUT',1,'admin','默认部门','/business/reward/10/status','127.0.0.1','内网IP','\"reward\" \"10\" {\"status\":\"pending\"} ','{\"msg\":\"操作成功\",\"code\":200}',0,NULL,'2026-10-05 17:37:33',28),(265,'业务状态',2,'com.ruoyi.web.controller.business.BusinessAdminController.status()','PUT',1,'admin','默认部门','/business/reward/10/status','127.0.0.1','内网IP','\"reward\" \"10\" {\"status\":\"received\"} ','{\"msg\":\"奖品状态不能从 pending 变更为 received\",\"code\":500}',0,NULL,'2026-10-05 17:37:33',6),(266,'业务状态',2,'com.ruoyi.web.controller.business.BusinessAdminController.status()','PUT',1,'admin','默认部门','/business/reward/11/status','127.0.0.1','内网IP','\"reward\" \"11\" {\"status\":\"pending\"} ','{\"msg\":\"该奖品已有核销流水，不能恢复为待核销\",\"code\":409}',0,NULL,'2026-10-05 17:37:33',9),(267,'业务状态',2,'com.ruoyi.web.controller.business.BusinessAdminController.status()','PUT',1,'admin','默认部门','/business/reward/12/status','127.0.0.1','内网IP','\"reward\" \"12\" {\"status\":\"closed\"} ','{\"msg\":\"红包存在未决或已成功的微信转账，不能关闭\",\"code\":409}',0,NULL,'2026-10-05 17:37:34',9),(268,'业务状态',2,'com.ruoyi.web.controller.business.BusinessAdminController.status()','PUT',1,'admin','默认部门','/business/reward/15/status','127.0.0.1','内网IP','\"reward\" \"15\" {\"status\":\"pending\"} ','{\"msg\":\"奖品已过期，不能恢复为待核销\",\"code\":409}',0,NULL,'2026-10-05 17:41:30',19),(269,'业务财务报表',5,'com.ruoyi.web.controller.business.BusinessExtensionController.export()','GET',1,'admin','默认部门','/business/extension/reports/orders/export','127.0.0.1','内网IP','{}',NULL,0,NULL,'2026-10-05 23:55:21',12),(270,'业务财务报表',5,'com.ruoyi.web.controller.business.BusinessExtensionController.export()','GET',1,'admin','默认部门','/business/extension/reports/orders/export','127.0.0.1','内网IP','{}',NULL,0,NULL,'2026-10-06 00:02:41',6),(271,'业务财务报表',5,'com.ruoyi.web.controller.business.BusinessExtensionController.export()','GET',1,'admin','默认部门','/business/extension/reports/orders/export','127.0.0.1','内网IP','{}',NULL,0,NULL,'2026-10-06 00:10:52',5),(272,'业务财务报表',5,'com.ruoyi.web.controller.business.BusinessExtensionController.export()','GET',1,'admin','默认部门','/business/extension/reports/orders/export','127.0.0.1','内网IP','{}',NULL,0,NULL,'2026-10-06 00:17:00',22),(273,'业务财务报表',5,'com.ruoyi.web.controller.business.BusinessExtensionController.export()','GET',1,'admin','默认部门','/business/extension/reports/orders/export','127.0.0.1','内网IP','{}',NULL,0,NULL,'2026-10-06 00:25:28',27),(274,'业务财务报表',5,'com.ruoyi.web.controller.business.BusinessExtensionController.export()','GET',1,'admin','默认部门','/business/extension/reports/orders/export','127.0.0.1','内网IP','{}',NULL,0,NULL,'2026-10-06 00:28:57',25),(275,'业务财务报表',5,'com.ruoyi.web.controller.business.BusinessExtensionController.export()','GET',1,'admin','默认部门','/business/extension/reports/orders/export','127.0.0.1','内网IP','{}',NULL,0,NULL,'2026-10-06 00:33:02',8),(276,'业务财务报表',5,'com.ruoyi.web.controller.business.BusinessExtensionController.export()','GET',1,'admin','默认部门','/business/extension/reports/orders/export','127.0.0.1','内网IP','{}',NULL,0,NULL,'2026-10-06 00:46:45',11),(277,'业务财务报表',5,'com.ruoyi.web.controller.business.BusinessExtensionController.export()','GET',1,'admin','默认部门','/business/extension/reports/orders/export','127.0.0.1','内网IP','{}',NULL,0,NULL,'2026-10-06 00:50:39',9),(278,'业务财务报表',5,'com.ruoyi.web.controller.business.BusinessExtensionController.export()','GET',1,'admin','默认部门','/business/extension/reports/orders/export','127.0.0.1','内网IP','{}',NULL,0,NULL,'2026-10-06 00:56:07',8),(279,'业务财务报表',5,'com.ruoyi.web.controller.business.BusinessExtensionController.export()','GET',1,'admin','默认部门','/business/extension/reports/orders/export','127.0.0.1','内网IP','{}',NULL,0,NULL,'2026-10-06 01:01:01',8),(280,'业务财务报表',5,'com.ruoyi.web.controller.business.BusinessExtensionController.export()','GET',1,'admin','默认部门','/business/extension/reports/orders/export','127.0.0.1','内网IP','{}',NULL,0,NULL,'2026-10-06 01:01:58',9),(281,'业务财务报表',5,'com.ruoyi.web.controller.business.BusinessExtensionController.export()','GET',1,'admin','默认部门','/business/extension/reports/orders/export','127.0.0.1','内网IP','{}',NULL,0,NULL,'2026-10-06 01:11:06',7),(282,'业务财务报表',5,'com.ruoyi.web.controller.business.BusinessExtensionController.export()','GET',1,'admin','默认部门','/business/extension/reports/orders/export','127.0.0.1','内网IP','{}',NULL,0,NULL,'2026-10-06 01:15:16',10),(283,'业务财务报表',5,'com.ruoyi.web.controller.business.BusinessExtensionController.export()','GET',1,'admin','默认部门','/business/extension/reports/orders/export','127.0.0.1','内网IP','{}',NULL,0,NULL,'2026-10-06 01:19:34',10),(284,'业务财务报表',5,'com.ruoyi.web.controller.business.BusinessExtensionController.export()','GET',1,'admin','默认部门','/business/extension/reports/orders/export','127.0.0.1','内网IP','{}',NULL,0,NULL,'2026-10-06 01:21:53',7),(285,'业务财务报表',5,'com.ruoyi.web.controller.business.BusinessExtensionController.export()','GET',1,'admin','默认部门','/business/extension/reports/orders/export','127.0.0.1','内网IP','{}',NULL,0,NULL,'2026-10-06 01:22:26',7),(286,'业务财务报表',5,'com.ruoyi.web.controller.business.BusinessExtensionController.export()','GET',1,'admin','默认部门','/business/extension/reports/orders/export','127.0.0.1','内网IP','{}',NULL,0,NULL,'2026-10-06 01:23:53',6),(287,'业务财务报表',5,'com.ruoyi.web.controller.business.BusinessExtensionController.export()','GET',1,'admin','默认部门','/business/extension/reports/orders/export','127.0.0.1','内网IP','{}',NULL,0,NULL,'2026-10-06 01:24:54',8),(288,'业务财务报表',5,'com.ruoyi.web.controller.business.BusinessExtensionController.export()','GET',1,'admin','默认部门','/business/extension/reports/orders/export','127.0.0.1','内网IP','{}',NULL,0,NULL,'2026-10-06 01:36:03',8),(289,'业务财务报表',5,'com.ruoyi.web.controller.business.BusinessExtensionController.export()','GET',1,'admin','默认部门','/business/extension/reports/orders/export','127.0.0.1','内网IP','{}',NULL,0,NULL,'2026-10-06 01:40:11',8),(290,'业务财务报表',5,'com.ruoyi.web.controller.business.BusinessExtensionController.export()','GET',1,'admin','默认部门','/business/extension/reports/orders/export','127.0.0.1','内网IP','{}',NULL,0,NULL,'2026-10-06 01:45:25',5),(291,'业务财务报表',5,'com.ruoyi.web.controller.business.BusinessExtensionController.export()','GET',1,'admin','默认部门','/business/extension/reports/orders/export','127.0.0.1','内网IP','{}',NULL,0,NULL,'2026-10-06 02:17:08',22),(292,'销冠奖励计算',1,'com.ruoyi.web.controller.business.BusinessExtensionController.issueRanking()','POST',1,'admin','默认部门','/business/extension/ranking/issue','127.0.0.1','内网IP','{\"configId\":3} ','{\"msg\":\"操作成功\",\"code\":200,\"data\":{\"count\":1,\"alreadyIssued\":false,\"periodStart\":\"1901-01-01 00:00:00\",\"periodEnd\":\"1901-02-01 00:00:00\"}}',0,NULL,'2026-10-06 02:27:43',71),(293,'销冠奖励计算',1,'com.ruoyi.web.controller.business.BusinessExtensionController.issueRanking()','POST',1,'admin','默认部门','/business/extension/ranking/issue','127.0.0.1','内网IP','{\"configId\":3} ','{\"msg\":\"操作成功\",\"code\":200,\"data\":{\"periodStart\":\"1901-01-01 00:00:00\",\"periodEnd\":\"1901-02-01 00:00:00\",\"count\":1,\"alreadyIssued\":true}}',0,NULL,'2026-10-06 02:27:43',78),(294,'销冠奖励计算',1,'com.ruoyi.web.controller.business.BusinessExtensionController.issueRanking()','POST',1,'admin','默认部门','/business/extension/ranking/issue','127.0.0.1','内网IP','{\"configId\":3} ','{\"msg\":\"操作成功\",\"code\":200,\"data\":{\"periodStart\":\"1901-01-01 00:00:00\",\"periodEnd\":\"1901-02-01 00:00:00\",\"count\":1,\"alreadyIssued\":true}}',0,NULL,'2026-10-06 02:27:43',4),(295,'销冠奖励计算',1,'com.ruoyi.web.controller.business.BusinessExtensionController.issueRanking()','POST',1,'admin','默认部门','/business/extension/ranking/issue','127.0.0.1','内网IP','{\"configId\":3} ','{\"msg\":\"所选时间与已发奖周期重叠，请核对历史记录，不能重复结算\",\"code\":409}',0,NULL,'2026-10-06 02:27:43',7),(296,'销冠奖励计算',1,'com.ruoyi.web.controller.business.BusinessExtensionController.issueRanking()','POST',1,'admin','默认部门','/business/extension/ranking/issue','127.0.0.1','内网IP','{\"configId\":3} ','{\"msg\":\"操作成功\",\"code\":200,\"data\":{\"count\":0,\"alreadyIssued\":false,\"periodStart\":\"1901-03-01 00:00:00\",\"periodEnd\":\"1901-04-01 00:00:00\"}}',0,NULL,'2026-10-06 02:27:43',13),(297,'销冠奖励计算',1,'com.ruoyi.web.controller.business.BusinessExtensionController.issueRanking()','POST',1,'admin','默认部门','/business/extension/ranking/issue','127.0.0.1','内网IP','{\"configId\":3} ','{\"msg\":\"操作成功\",\"code\":200,\"data\":{\"periodStart\":\"1901-03-01 00:00:00\",\"periodEnd\":\"1901-04-01 00:00:00\",\"count\":0,\"alreadyIssued\":true}}',0,NULL,'2026-10-06 02:27:43',4),(298,'销冠奖励计算',1,'com.ruoyi.web.controller.business.BusinessExtensionController.issueRanking()','POST',1,'admin','默认部门','/business/extension/ranking/issue','127.0.0.1','内网IP','{\"configId\":3} ','{\"msg\":\"统计周期尚未结束，不能生成最终奖励\",\"code\":409}',0,NULL,'2026-10-06 02:27:43',3),(299,'销冠奖励计算',1,'com.ruoyi.web.controller.business.BusinessExtensionController.issueRanking()','POST',1,'admin','默认部门','/business/extension/ranking/issue','127.0.0.1','内网IP','{\"configId\":3} ','{\"msg\":\"请选择待结算周期内的日期，格式为 YYYY-MM-DD\",\"code\":400}',0,NULL,'2026-10-06 02:27:43',4),(300,'销冠奖励计算',1,'com.ruoyi.web.controller.business.BusinessExtensionController.issueRanking()','POST',1,'admin','默认部门','/business/extension/ranking/issue','127.0.0.1','内网IP','{\"periodDate\":\"1901-04-12\",\"configId\":3} ','{\"msg\":\"操作成功\",\"code\":200,\"data\":{\"count\":0,\"alreadyIssued\":false,\"periodStart\":\"1901-04-01 00:00:00\",\"periodEnd\":\"1901-05-01 00:00:00\"}}',0,NULL,'2026-10-06 02:27:43',13),(301,'销冠排行规则',2,'com.ruoyi.web.controller.business.BusinessExtensionController.updateRankingConfig()','PUT',1,'admin','默认部门','/business/extension/ranking/config','127.0.0.1','内网IP','{\"configId\":3,\"rewardJson\":\"[{\\\"rank\\\":1,\\\"points\\\":0.5,\\\"subsidyCent\\\":0}]\",\"periodType\":\"month\",\"status\":\"0\"} ','{\"msg\":\"名次、积分和补贴分值必须为有效整数\",\"code\":500}',0,NULL,'2026-10-06 02:27:43',0),(302,'销冠排行规则',2,'com.ruoyi.web.controller.business.BusinessExtensionController.updateRankingConfig()','PUT',1,'admin','默认部门','/business/extension/ranking/config','127.0.0.1','内网IP','{\"configId\":3,\"rewardJson\":\"[{\\\"rank\\\":1,\\\"points\\\":1,\\\"subsidyCent\\\":0},{\\\"rank\\\":1,\\\"points\\\":2,\\\"subsidyCent\\\":0}]\",\"periodType\":\"month\",\"status\":\"0\"} ','{\"msg\":\"奖励名次必须在 1 至 100 之间且不能重复\",\"code\":500}',0,NULL,'2026-10-06 02:27:43',0),(303,'业务财务报表',5,'com.ruoyi.web.controller.business.BusinessExtensionController.export()','GET',1,'admin','默认部门','/business/extension/reports/orders/export','127.0.0.1','内网IP','{}',NULL,0,NULL,'2026-10-06 02:28:08',10),(304,'业务财务报表',5,'com.ruoyi.web.controller.business.BusinessExtensionController.export()','GET',1,'admin','默认部门','/business/extension/reports/orders/export','127.0.0.1','内网IP','{}',NULL,0,NULL,'2026-10-06 02:38:59',7),(305,'销冠奖励计算',1,'com.ruoyi.web.controller.business.BusinessExtensionController.issueRanking()','POST',1,'admin','默认部门','/business/extension/ranking/issue','127.0.0.1','内网IP','{\"configId\":4} ','{\"msg\":\"操作成功\",\"code\":200,\"data\":{\"count\":1,\"alreadyIssued\":false,\"periodStart\":\"1901-01-01 00:00:00\",\"periodEnd\":\"1901-02-01 00:00:00\"}}',0,NULL,'2026-10-06 02:38:59',20),(306,'销冠奖励计算',1,'com.ruoyi.web.controller.business.BusinessExtensionController.issueRanking()','POST',1,'admin','默认部门','/business/extension/ranking/issue','127.0.0.1','内网IP','{\"configId\":4} ','{\"msg\":\"操作成功\",\"code\":200,\"data\":{\"periodStart\":\"1901-01-01 00:00:00\",\"periodEnd\":\"1901-02-01 00:00:00\",\"count\":1,\"alreadyIssued\":true}}',0,NULL,'2026-10-06 02:38:59',24),(307,'销冠奖励计算',1,'com.ruoyi.web.controller.business.BusinessExtensionController.issueRanking()','POST',1,'admin','默认部门','/business/extension/ranking/issue','127.0.0.1','内网IP','{\"configId\":4} ','{\"msg\":\"操作成功\",\"code\":200,\"data\":{\"periodStart\":\"1901-01-01 00:00:00\",\"periodEnd\":\"1901-02-01 00:00:00\",\"count\":1,\"alreadyIssued\":true}}',0,NULL,'2026-10-06 02:38:59',5),(308,'销冠奖励计算',1,'com.ruoyi.web.controller.business.BusinessExtensionController.issueRanking()','POST',1,'admin','默认部门','/business/extension/ranking/issue','127.0.0.1','内网IP','{\"configId\":4} ','{\"msg\":\"所选时间与已发奖周期重叠，请核对历史记录，不能重复结算\",\"code\":409}',0,NULL,'2026-10-06 02:39:00',4),(309,'销冠奖励计算',1,'com.ruoyi.web.controller.business.BusinessExtensionController.issueRanking()','POST',1,'admin','默认部门','/business/extension/ranking/issue','127.0.0.1','内网IP','{\"configId\":4} ','{\"msg\":\"操作成功\",\"code\":200,\"data\":{\"count\":0,\"alreadyIssued\":false,\"periodStart\":\"1901-03-01 00:00:00\",\"periodEnd\":\"1901-04-01 00:00:00\"}}',0,NULL,'2026-10-06 02:39:00',7),(310,'销冠奖励计算',1,'com.ruoyi.web.controller.business.BusinessExtensionController.issueRanking()','POST',1,'admin','默认部门','/business/extension/ranking/issue','127.0.0.1','内网IP','{\"configId\":4} ','{\"msg\":\"操作成功\",\"code\":200,\"data\":{\"periodStart\":\"1901-03-01 00:00:00\",\"periodEnd\":\"1901-04-01 00:00:00\",\"count\":0,\"alreadyIssued\":true}}',0,NULL,'2026-10-06 02:39:00',3),(311,'销冠奖励计算',1,'com.ruoyi.web.controller.business.BusinessExtensionController.issueRanking()','POST',1,'admin','默认部门','/business/extension/ranking/issue','127.0.0.1','内网IP','{\"configId\":4} ','{\"msg\":\"统计周期尚未结束，不能生成最终奖励\",\"code\":409}',0,NULL,'2026-10-06 02:39:00',2),(312,'销冠奖励计算',1,'com.ruoyi.web.controller.business.BusinessExtensionController.issueRanking()','POST',1,'admin','默认部门','/business/extension/ranking/issue','127.0.0.1','内网IP','{\"configId\":4} ','{\"msg\":\"请选择待结算周期内的日期，格式为 YYYY-MM-DD\",\"code\":400}',0,NULL,'2026-10-06 02:39:00',2),(313,'销冠奖励计算',1,'com.ruoyi.web.controller.business.BusinessExtensionController.issueRanking()','POST',1,'admin','默认部门','/business/extension/ranking/issue','127.0.0.1','内网IP','{\"configId\":4,\"periodDate\":\"1901-04-12\"} ','{\"msg\":\"操作成功\",\"code\":200,\"data\":{\"count\":0,\"alreadyIssued\":false,\"periodStart\":\"1901-04-01 00:00:00\",\"periodEnd\":\"1901-05-01 00:00:00\"}}',0,NULL,'2026-10-06 02:39:00',7),(314,'销冠排行规则',2,'com.ruoyi.web.controller.business.BusinessExtensionController.updateRankingConfig()','PUT',1,'admin','默认部门','/business/extension/ranking/config','127.0.0.1','内网IP','{\"periodType\":\"month\",\"status\":\"0\",\"rewardJson\":\"[{\\\"rank\\\":1,\\\"points\\\":0.5,\\\"subsidyCent\\\":0}]\",\"configId\":4} ','{\"msg\":\"名次、积分和补贴分值必须为有效整数\",\"code\":500}',0,NULL,'2026-10-06 02:39:00',0),(315,'销冠排行规则',2,'com.ruoyi.web.controller.business.BusinessExtensionController.updateRankingConfig()','PUT',1,'admin','默认部门','/business/extension/ranking/config','127.0.0.1','内网IP','{\"periodType\":\"month\",\"status\":\"0\",\"rewardJson\":\"[{\\\"rank\\\":1,\\\"points\\\":1,\\\"subsidyCent\\\":0},{\\\"rank\\\":1,\\\"points\\\":2,\\\"subsidyCent\\\":0}]\",\"configId\":4} ','{\"msg\":\"奖励名次必须在 1 至 100 之间且不能重复\",\"code\":500}',0,NULL,'2026-10-06 02:39:00',0),(316,'销冠奖励计算',1,'com.ruoyi.web.controller.business.BusinessExtensionController.issueRanking()','POST',1,'admin','默认部门','/business/extension/ranking/issue','127.0.0.1','内网IP','{\"configId\":5} ','{\"msg\":\"操作成功\",\"code\":200,\"data\":{\"count\":1,\"alreadyIssued\":false,\"periodStart\":\"1901-01-01 00:00:00\",\"periodEnd\":\"1901-02-01 00:00:00\"}}',0,NULL,'2026-10-06 02:47:48',25),(317,'销冠奖励计算',1,'com.ruoyi.web.controller.business.BusinessExtensionController.issueRanking()','POST',1,'admin','默认部门','/business/extension/ranking/issue','127.0.0.1','内网IP','{\"configId\":5} ','{\"msg\":\"操作成功\",\"code\":200,\"data\":{\"periodStart\":\"1901-01-01 00:00:00\",\"periodEnd\":\"1901-02-01 00:00:00\",\"count\":1,\"alreadyIssued\":true}}',0,NULL,'2026-10-06 02:47:48',27),(318,'销冠奖励计算',1,'com.ruoyi.web.controller.business.BusinessExtensionController.issueRanking()','POST',1,'admin','默认部门','/business/extension/ranking/issue','127.0.0.1','内网IP','{\"configId\":5} ','{\"msg\":\"操作成功\",\"code\":200,\"data\":{\"periodStart\":\"1901-01-01 00:00:00\",\"periodEnd\":\"1901-02-01 00:00:00\",\"count\":1,\"alreadyIssued\":true}}',0,NULL,'2026-10-06 02:47:48',3),(319,'销冠奖励计算',1,'com.ruoyi.web.controller.business.BusinessExtensionController.issueRanking()','POST',1,'admin','默认部门','/business/extension/ranking/issue','127.0.0.1','内网IP','{\"configId\":5} ','{\"msg\":\"所选时间与已发奖周期重叠，请核对历史记录，不能重复结算\",\"code\":409}',0,NULL,'2026-10-06 02:47:48',4),(320,'销冠奖励计算',1,'com.ruoyi.web.controller.business.BusinessExtensionController.issueRanking()','POST',1,'admin','默认部门','/business/extension/ranking/issue','127.0.0.1','内网IP','{\"configId\":5} ','{\"msg\":\"操作成功\",\"code\":200,\"data\":{\"count\":0,\"alreadyIssued\":false,\"periodStart\":\"1901-03-01 00:00:00\",\"periodEnd\":\"1901-04-01 00:00:00\"}}',0,NULL,'2026-10-06 02:47:49',9),(321,'销冠奖励计算',1,'com.ruoyi.web.controller.business.BusinessExtensionController.issueRanking()','POST',1,'admin','默认部门','/business/extension/ranking/issue','127.0.0.1','内网IP','{\"configId\":5} ','{\"msg\":\"操作成功\",\"code\":200,\"data\":{\"periodStart\":\"1901-03-01 00:00:00\",\"periodEnd\":\"1901-04-01 00:00:00\",\"count\":0,\"alreadyIssued\":true}}',0,NULL,'2026-10-06 02:47:49',3),(322,'销冠奖励计算',1,'com.ruoyi.web.controller.business.BusinessExtensionController.issueRanking()','POST',1,'admin','默认部门','/business/extension/ranking/issue','127.0.0.1','内网IP','{\"configId\":5} ','{\"msg\":\"统计周期尚未结束，不能生成最终奖励\",\"code\":409}',0,NULL,'2026-10-06 02:47:49',2),(323,'销冠奖励计算',1,'com.ruoyi.web.controller.business.BusinessExtensionController.issueRanking()','POST',1,'admin','默认部门','/business/extension/ranking/issue','127.0.0.1','内网IP','{\"configId\":5} ','{\"msg\":\"请选择待结算周期内的日期，格式为 YYYY-MM-DD\",\"code\":400}',0,NULL,'2026-10-06 02:47:49',2),(324,'销冠奖励计算',1,'com.ruoyi.web.controller.business.BusinessExtensionController.issueRanking()','POST',1,'admin','默认部门','/business/extension/ranking/issue','127.0.0.1','内网IP','{\"configId\":5,\"periodDate\":\"1901-04-12\"} ','{\"msg\":\"操作成功\",\"code\":200,\"data\":{\"count\":0,\"alreadyIssued\":false,\"periodStart\":\"1901-04-01 00:00:00\",\"periodEnd\":\"1901-05-01 00:00:00\"}}',0,NULL,'2026-10-06 02:47:49',9),(325,'销冠排行规则',2,'com.ruoyi.web.controller.business.BusinessExtensionController.updateRankingConfig()','PUT',1,'admin','默认部门','/business/extension/ranking/config','127.0.0.1','内网IP','{\"status\":\"0\",\"rewardJson\":\"[{\\\"rank\\\":1,\\\"points\\\":1,\\\"subsidyCent\\\":0},{\\\"rank\\\":1,\\\"points\\\":2,\\\"subsidyCent\\\":0}]\",\"periodType\":\"month\",\"configId\":5} ','{\"msg\":\"奖励名次必须在 1 至 100 之间且不能重复\",\"code\":500}',0,NULL,'2026-10-06 02:47:49',0),(326,'销冠排行规则',2,'com.ruoyi.web.controller.business.BusinessExtensionController.updateRankingConfig()','PUT',1,'admin','默认部门','/business/extension/ranking/config','127.0.0.1','内网IP','{\"status\":\"0\",\"rewardJson\":\"[{\\\"rank\\\":1,\\\"points\\\":0.5,\\\"subsidyCent\\\":0}]\",\"periodType\":\"month\",\"configId\":5} ','{\"msg\":\"名次、积分和补贴分值必须为有效整数\",\"code\":500}',0,NULL,'2026-10-06 02:47:49',0),(327,'业务财务报表',5,'com.ruoyi.web.controller.business.BusinessExtensionController.export()','GET',1,'admin','默认部门','/business/extension/reports/orders/export','127.0.0.1','内网IP','{}',NULL,0,NULL,'2026-10-06 02:47:51',9),(328,'业务财务报表',5,'com.ruoyi.web.controller.business.BusinessExtensionController.export()','GET',1,'admin','默认部门','/business/extension/reports/orders/export','127.0.0.1','内网IP','{}',NULL,0,NULL,'2026-10-06 02:49:28',9),(329,'业务财务报表',5,'com.ruoyi.web.controller.business.BusinessExtensionController.export()','GET',1,'admin','默认部门','/business/extension/reports/refunds/export','127.0.0.1','内网IP','{}',NULL,0,NULL,'2026-10-06 02:56:56',16),(330,'业务财务报表',5,'com.ruoyi.web.controller.business.BusinessExtensionController.export()','GET',1,'admin','默认部门','/business/extension/reports/refunds/export','127.0.0.1','内网IP','{}',NULL,0,NULL,'2026-10-06 02:58:07',7),(331,'业务财务报表',5,'com.ruoyi.web.controller.business.BusinessExtensionController.export()','GET',1,'admin','默认部门','/business/extension/reports/orders/export','127.0.0.1','内网IP','{}',NULL,0,NULL,'2026-10-06 02:58:46',13),(332,'退款售后审核',2,'com.ruoyi.web.controller.business.BusinessExtensionController.refundStatus()','PUT',1,'admin','默认部门','/business/extension/refunds/11/status','127.0.0.1','内网IP','11 {\"paymentReference\":\"BANK-39bc9f82d2d342dfa98045fe65e3b028\",\"status\":\"refunded\"} ','{\"msg\":\"微信退款状态尚未明确关闭，请先查单核对，不能线下确认\",\"code\":409}',0,NULL,'2026-10-06 03:14:07',13),(333,'退款售后审核',2,'com.ruoyi.web.controller.business.BusinessExtensionController.refundStatus()','PUT',1,'admin','默认部门','/business/extension/refunds/11/status','127.0.0.1','内网IP','11 {\"status\":\"refunded\"} ','{\"msg\":\"请填写不超过128字的实际付款凭证号\",\"code\":400}',0,NULL,'2026-10-06 03:14:07',4),(334,'退款售后审核',2,'com.ruoyi.web.controller.business.BusinessExtensionController.refundStatus()','PUT',1,'admin','默认部门','/business/extension/refunds/11/status','127.0.0.1','内网IP','11 {\"paymentReference\":\"BANK-39bc9f82d2d342dfa98045fe65e3b028\",\"status\":\"refunded\"} ','{\"msg\":\"操作成功\",\"code\":200}',0,NULL,'2026-10-06 03:14:07',48),(335,'业务财务报表',5,'com.ruoyi.web.controller.business.BusinessExtensionController.export()','GET',1,'admin','默认部门','/business/extension/reports/refunds/export','127.0.0.1','内网IP','{}',NULL,0,NULL,'2026-10-06 03:14:07',9),(336,'退款售后审核',2,'com.ruoyi.web.controller.business.BusinessExtensionController.refundStatus()','PUT',1,'admin','默认部门','/business/extension/refunds/12/status','127.0.0.1','内网IP','12 {\"status\":\"refunded\",\"paymentReference\":\"BANK-d4ad14bfc441431084358e026628157a\"} ','{\"msg\":\"微信退款状态尚未明确关闭，请先查单核对，不能线下确认\",\"code\":409}',0,NULL,'2026-10-06 03:17:06',3),(337,'退款售后审核',2,'com.ruoyi.web.controller.business.BusinessExtensionController.refundStatus()','PUT',1,'admin','默认部门','/business/extension/refunds/12/status','127.0.0.1','内网IP','12 {\"status\":\"refunded\"} ','{\"msg\":\"请填写不超过128字的实际付款凭证号\",\"code\":400}',0,NULL,'2026-10-06 03:17:06',4),(338,'退款售后审核',2,'com.ruoyi.web.controller.business.BusinessExtensionController.refundStatus()','PUT',1,'admin','默认部门','/business/extension/refunds/12/status','127.0.0.1','内网IP','12 {\"status\":\"refunded\",\"paymentReference\":\"BANK-d4ad14bfc441431084358e026628157a\"} ','{\"msg\":\"操作成功\",\"code\":200}',0,NULL,'2026-10-06 03:17:06',28),(339,'业务财务报表',5,'com.ruoyi.web.controller.business.BusinessExtensionController.export()','GET',1,'admin','默认部门','/business/extension/reports/refunds/export','127.0.0.1','内网IP','{}',NULL,0,NULL,'2026-10-06 03:17:06',6),(340,'退款售后审核',2,'com.ruoyi.web.controller.business.BusinessExtensionController.refundStatus()','PUT',1,'admin','默认部门','/business/extension/refunds/13/status','127.0.0.1','内网IP','13 {\"status\":\"refunded\",\"paymentReference\":\"BANK-2a65c98f690d4103a2c99456d8e0372c\"} ','{\"msg\":\"微信退款状态尚未明确关闭，请先查单核对，不能线下确认\",\"code\":409}',0,NULL,'2026-10-06 03:17:44',3),(341,'退款售后审核',2,'com.ruoyi.web.controller.business.BusinessExtensionController.refundStatus()','PUT',1,'admin','默认部门','/business/extension/refunds/13/status','127.0.0.1','内网IP','13 {\"status\":\"refunded\"} ','{\"msg\":\"请填写不超过128字的实际付款凭证号\",\"code\":400}',0,NULL,'2026-10-06 03:17:44',4),(342,'退款售后审核',2,'com.ruoyi.web.controller.business.BusinessExtensionController.refundStatus()','PUT',1,'admin','默认部门','/business/extension/refunds/13/status','127.0.0.1','内网IP','13 {\"status\":\"refunded\",\"paymentReference\":\"BANK-2a65c98f690d4103a2c99456d8e0372c\"} ','{\"msg\":\"操作成功\",\"code\":200}',0,NULL,'2026-10-06 03:17:45',30),(343,'业务财务报表',5,'com.ruoyi.web.controller.business.BusinessExtensionController.export()','GET',1,'admin','默认部门','/business/extension/reports/refunds/export','127.0.0.1','内网IP','{}',NULL,0,NULL,'2026-10-06 03:17:45',6),(344,'业务财务报表',5,'com.ruoyi.web.controller.business.BusinessExtensionController.export()','GET',1,'admin','默认部门','/business/extension/reports/orders/export','127.0.0.1','内网IP','{}',NULL,0,NULL,'2026-10-06 03:17:53',8),(345,'业务财务报表',5,'com.ruoyi.web.controller.business.BusinessExtensionController.export()','GET',1,'admin','默认部门','/business/extension/reports/refunds/export','127.0.0.1','内网IP','{}',NULL,0,NULL,'2026-10-06 03:17:53',9),(346,'业务财务报表',5,'com.ruoyi.web.controller.business.BusinessExtensionController.export()','GET',1,'admin','默认部门','/business/extension/reports/orders/export','127.0.0.1','内网IP','{}',NULL,0,NULL,'2026-10-06 03:22:18',7),(347,'业务财务报表',5,'com.ruoyi.web.controller.business.BusinessExtensionController.export()','GET',1,'admin','默认部门','/business/extension/reports/refunds/export','127.0.0.1','内网IP','{}',NULL,0,NULL,'2026-10-06 03:22:18',4),(348,'微信渠道配置',2,'com.ruoyi.web.controller.business.BusinessExtensionController.updateChannel()','PUT',1,'admin','默认部门','/business/extension/channel','127.0.0.1','内网IP','{\"wechat.work.groupQr\":\"https://invalid.example/should-not-save\"} ','{\"msg\":\"企业微信群二维码及群 ID 只能通过生成/轮换接口更新\",\"code\":400}',0,NULL,'2026-10-06 03:49:12',3),(349,'微信渠道配置',2,'com.ruoyi.web.controller.business.BusinessExtensionController.updateChannel()','PUT',1,'admin','默认部门','/business/extension/channel','127.0.0.1','内网IP','{\"wechat.work.groupChatIds\":\"should-not-save\"} ','{\"msg\":\"企业微信群二维码及群 ID 只能通过生成/轮换接口更新\",\"code\":400}',0,NULL,'2026-10-06 03:49:12',0),(350,'业务财务报表',5,'com.ruoyi.web.controller.business.BusinessExtensionController.export()','GET',1,'admin','默认部门','/business/extension/reports/orders/export','127.0.0.1','内网IP','{}',NULL,0,NULL,'2026-10-06 03:49:26',14),(351,'业务财务报表',5,'com.ruoyi.web.controller.business.BusinessExtensionController.export()','GET',1,'admin','默认部门','/business/extension/reports/refunds/export','127.0.0.1','内网IP','{}',NULL,0,NULL,'2026-10-06 03:49:26',6),(352,'业务财务报表',5,'com.ruoyi.web.controller.business.BusinessExtensionController.export()','GET',1,'admin','默认部门','/business/extension/reports/orders/export','127.0.0.1','内网IP','{}',NULL,0,NULL,'2026-10-06 04:04:40',17),(353,'业务财务报表',5,'com.ruoyi.web.controller.business.BusinessExtensionController.export()','GET',1,'admin','默认部门','/business/extension/reports/refunds/export','127.0.0.1','内网IP','{}',NULL,0,NULL,'2026-10-06 04:04:40',7),(354,'企业微信群旧码人工核验',2,'com.ruoyi.web.controller.business.BusinessExtensionController.reconcileGroupQr()','POST',1,'admin','默认部门','/business/extension/group-qr/reconcile','127.0.0.1','内网IP','{\"evidence\":\"verified test evidence only\",\"configId\":\"not-pending\"} ','{\"msg\":\"该旧二维码不在待核验清单中\",\"code\":404}',0,NULL,'2026-10-06 04:11:01',8),(355,'业务财务报表',5,'com.ruoyi.web.controller.business.BusinessExtensionController.export()','GET',1,'admin','默认部门','/business/extension/reports/orders/export','127.0.0.1','内网IP','{}',NULL,0,NULL,'2026-10-06 04:11:01',16),(356,'业务财务报表',5,'com.ruoyi.web.controller.business.BusinessExtensionController.export()','GET',1,'admin','默认部门','/business/extension/reports/refunds/export','127.0.0.1','内网IP','{}',NULL,0,NULL,'2026-10-06 04:11:01',5),(357,'业务财务报表',5,'com.ruoyi.web.controller.business.BusinessExtensionController.export()','GET',1,'admin','默认部门','/business/extension/reports/orders/export','127.0.0.1','内网IP','{}',NULL,0,NULL,'2026-10-06 04:22:01',8),(358,'业务财务报表',5,'com.ruoyi.web.controller.business.BusinessExtensionController.export()','GET',1,'admin','默认部门','/business/extension/reports/refunds/export','127.0.0.1','内网IP','{}',NULL,0,NULL,'2026-10-06 04:22:01',5),(359,'正式平台上线验收',2,'com.ruoyi.web.controller.business.BusinessExtensionController.verifyReadiness()','POST',1,'admin','默认部门','/business/extension/readiness/pay/verify','127.0.0.1','内网IP','\"pay\" {\"evidence\":\"local-only test evidence without platform receipt\"} ','{\"msg\":\"该渠道配置尚不完整，不能登记平台验收\",\"code\":409}',0,NULL,'2026-10-06 04:35:57',7),(360,'业务财务报表',5,'com.ruoyi.web.controller.business.BusinessExtensionController.export()','GET',1,'admin','默认部门','/business/extension/reports/orders/export','127.0.0.1','内网IP','{}',NULL,0,NULL,'2026-10-06 04:35:57',13),(361,'业务财务报表',5,'com.ruoyi.web.controller.business.BusinessExtensionController.export()','GET',1,'admin','默认部门','/business/extension/reports/refunds/export','127.0.0.1','内网IP','{}',NULL,0,NULL,'2026-10-06 04:35:57',5),(362,'业务财务报表',5,'com.ruoyi.web.controller.business.BusinessExtensionController.export()','GET',1,'admin','默认部门','/business/extension/reports/orders/export','127.0.0.1','内网IP','{}',NULL,0,NULL,'2026-10-06 04:43:16',23),(363,'业务财务报表',5,'com.ruoyi.web.controller.business.BusinessExtensionController.export()','GET',1,'admin','默认部门','/business/extension/reports/refunds/export','127.0.0.1','内网IP','{}',NULL,0,NULL,'2026-10-06 04:43:16',6),(364,'正式平台上线验收',2,'com.ruoyi.web.controller.business.BusinessExtensionController.verifyReadiness()','POST',1,'admin','默认部门','/business/extension/readiness/pay/verify','127.0.0.1','内网IP','\"pay\" {\"evidence\":\"Production platform receipt placeholder\"} ','{\"msg\":\"该渠道配置尚不完整，不能登记平台验收\",\"code\":409}',0,NULL,'2026-10-06 04:43:42',0),(365,'业务财务报表',5,'com.ruoyi.web.controller.business.BusinessExtensionController.export()','GET',1,'admin','默认部门','/business/extension/reports/orders/export','127.0.0.1','内网IP','{}',NULL,0,NULL,'2026-10-06 05:06:02',15),(366,'业务财务报表',5,'com.ruoyi.web.controller.business.BusinessExtensionController.export()','GET',1,'admin','默认部门','/business/extension/reports/refunds/export','127.0.0.1','内网IP','{}',NULL,0,NULL,'2026-10-06 05:06:02',9),(367,'业务财务报表',5,'com.ruoyi.web.controller.business.BusinessExtensionController.export()','GET',1,'admin','默认部门','/business/extension/reports/orders/export','127.0.0.1','内网IP','{}',NULL,0,NULL,'2026-10-06 05:10:11',23),(368,'业务财务报表',5,'com.ruoyi.web.controller.business.BusinessExtensionController.export()','GET',1,'admin','默认部门','/business/extension/reports/refunds/export','127.0.0.1','内网IP','{}',NULL,0,NULL,'2026-10-06 05:10:11',6),(369,'业务财务报表',5,'com.ruoyi.web.controller.business.BusinessExtensionController.export()','GET',1,'admin','默认部门','/business/extension/reports/orders/export','127.0.0.1','内网IP','{}',NULL,0,NULL,'2026-10-06 05:17:00',10),(370,'业务财务报表',5,'com.ruoyi.web.controller.business.BusinessExtensionController.export()','GET',1,'admin','默认部门','/business/extension/reports/refunds/export','127.0.0.1','内网IP','{}',NULL,0,NULL,'2026-10-06 05:17:00',4),(371,'业务财务报表',5,'com.ruoyi.web.controller.business.BusinessExtensionController.export()','GET',1,'admin','默认部门','/business/extension/reports/orders/export','127.0.0.1','内网IP','{}',NULL,0,NULL,'2026-10-06 05:35:21',17),(372,'业务财务报表',5,'com.ruoyi.web.controller.business.BusinessExtensionController.export()','GET',1,'admin','默认部门','/business/extension/reports/refunds/export','127.0.0.1','内网IP','{}',NULL,0,NULL,'2026-10-06 05:35:21',6),(373,'业务财务报表',5,'com.ruoyi.web.controller.business.BusinessExtensionController.export()','GET',1,'admin','默认部门','/business/extension/reports/orders/export','127.0.0.1','内网IP','{}',NULL,0,NULL,'2026-10-06 05:41:56',17),(374,'业务财务报表',5,'com.ruoyi.web.controller.business.BusinessExtensionController.export()','GET',1,'admin','默认部门','/business/extension/reports/refunds/export','127.0.0.1','内网IP','{}',NULL,0,NULL,'2026-10-06 05:41:56',6),(375,'业务财务报表',5,'com.ruoyi.web.controller.business.BusinessExtensionController.export()','GET',1,'admin','默认部门','/business/extension/reports/orders/export','127.0.0.1','内网IP','{}',NULL,0,NULL,'2026-10-06 05:47:01',18),(376,'业务财务报表',5,'com.ruoyi.web.controller.business.BusinessExtensionController.export()','GET',1,'admin','默认部门','/business/extension/reports/refunds/export','127.0.0.1','内网IP','{}',NULL,0,NULL,'2026-10-06 05:47:01',6),(377,'业务财务报表',5,'com.ruoyi.web.controller.business.BusinessExtensionController.export()','GET',1,'admin','默认部门','/business/extension/reports/orders/export','127.0.0.1','内网IP','{}',NULL,0,NULL,'2026-10-06 05:54:24',17),(378,'业务财务报表',5,'com.ruoyi.web.controller.business.BusinessExtensionController.export()','GET',1,'admin','默认部门','/business/extension/reports/refunds/export','127.0.0.1','内网IP','{}',NULL,0,NULL,'2026-10-06 05:54:24',5),(379,'业务财务报表',5,'com.ruoyi.web.controller.business.BusinessExtensionController.export()','GET',1,'admin','默认部门','/business/extension/reports/orders/export','127.0.0.1','内网IP','{}',NULL,0,NULL,'2026-10-06 06:09:25',16),(380,'业务财务报表',5,'com.ruoyi.web.controller.business.BusinessExtensionController.export()','GET',1,'admin','默认部门','/business/extension/reports/refunds/export','127.0.0.1','内网IP','{}',NULL,0,NULL,'2026-10-06 06:09:25',7);
/*!40000 ALTER TABLE `sys_oper_log` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sys_post`
--

DROP TABLE IF EXISTS `sys_post`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_post` (
  `post_id` bigint NOT NULL AUTO_INCREMENT COMMENT '岗位ID',
  `post_code` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '岗位编码',
  `post_name` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '岗位名称',
  `post_sort` int NOT NULL COMMENT '显示顺序',
  `status` char(1) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '状态（0正常 1停用）',
  `create_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`post_id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='岗位信息表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_post`
--

LOCK TABLES `sys_post` WRITE;
/*!40000 ALTER TABLE `sys_post` DISABLE KEYS */;
INSERT INTO `sys_post` VALUES (1,'admin','管理员',1,'0','admin','2026-10-02 23:48:30','',NULL,'');
/*!40000 ALTER TABLE `sys_post` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sys_role`
--

DROP TABLE IF EXISTS `sys_role`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_role` (
  `role_id` bigint NOT NULL AUTO_INCREMENT COMMENT '角色ID',
  `role_name` varchar(30) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '角色名称',
  `role_key` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '角色权限字符串',
  `role_sort` int NOT NULL COMMENT '显示顺序',
  `data_scope` char(1) COLLATE utf8mb4_unicode_ci DEFAULT '1' COMMENT '数据范围（1：全部数据权限 2：自定数据权限 3：本部门数据权限 4：本部门及以下数据权限）',
  `menu_check_strictly` tinyint(1) DEFAULT '1' COMMENT '菜单树选择项是否关联显示',
  `dept_check_strictly` tinyint(1) DEFAULT '1' COMMENT '部门树选择项是否关联显示',
  `status` char(1) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '角色状态（0正常 1停用）',
  `del_flag` char(1) COLLATE utf8mb4_unicode_ci DEFAULT '0' COMMENT '删除标志（0代表存在 2代表删除）',
  `create_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`role_id`)
) ENGINE=InnoDB AUTO_INCREMENT=100 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色信息表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_role`
--

LOCK TABLES `sys_role` WRITE;
/*!40000 ALTER TABLE `sys_role` DISABLE KEYS */;
INSERT INTO `sys_role` VALUES (1,'超级管理员','admin',1,'1',1,1,'0','0','admin','2026-10-02 23:48:30','',NULL,'超级管理员'),(10,'财务人员','finance',10,'1',1,1,'0','0','admin','2026-10-03 19:24:23','admin','2026-10-04 20:40:32','订单、退款、佣金和财务报表'),(20,'运营人员','operator',20,'1',1,1,'0','0','admin','2026-10-03 19:24:23','admin','2026-10-04 20:40:32','商品、活动、积分、分销和渠道运营'),(30,'核销员','verifier',30,'4',1,1,'0','0','admin','2026-10-03 19:24:23','admin','2026-10-04 20:40:32','门店核销及相关查询');
/*!40000 ALTER TABLE `sys_role` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sys_role_dept`
--

DROP TABLE IF EXISTS `sys_role_dept`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_role_dept` (
  `role_id` bigint NOT NULL COMMENT '角色ID',
  `dept_id` bigint NOT NULL COMMENT '部门ID',
  PRIMARY KEY (`role_id`,`dept_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色和部门关联表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_role_dept`
--

LOCK TABLES `sys_role_dept` WRITE;
/*!40000 ALTER TABLE `sys_role_dept` DISABLE KEYS */;
/*!40000 ALTER TABLE `sys_role_dept` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sys_role_menu`
--

DROP TABLE IF EXISTS `sys_role_menu`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_role_menu` (
  `role_id` bigint NOT NULL COMMENT '角色ID',
  `menu_id` bigint NOT NULL COMMENT '菜单ID',
  PRIMARY KEY (`role_id`,`menu_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色和菜单关联表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_role_menu`
--

LOCK TABLES `sys_role_menu` WRITE;
/*!40000 ALTER TABLE `sys_role_menu` DISABLE KEYS */;
INSERT INTO `sys_role_menu` VALUES (1,3000),(1,3010),(1,3011),(1,3012),(1,3013),(1,3014),(1,3020),(1,3021),(1,3022),(1,3023),(1,3024),(1,3025),(1,3030),(1,3031),(1,3032),(1,3033),(1,3034),(1,3040),(1,3041),(1,3042),(1,3043),(1,3044),(1,3050),(1,3051),(1,3052),(1,3060),(1,3061),(1,3062),(1,3070),(1,3071),(1,3072),(1,3080),(1,3081),(1,3090),(1,3091),(1,3092),(1,3100),(1,3101),(1,3102),(1,3103),(1,3104),(1,3110),(1,3111),(1,3112),(1,3113),(1,3114),(1,3115),(1,3116),(1,3117),(1,3118),(1,3119),(1,3120),(1,3121),(1,3122),(1,3123),(1,3124),(1,3125),(1,3126),(1,3127),(1,3128),(1,3129),(1,3130),(10,3000),(10,3050),(10,3051),(10,3060),(10,3061),(10,3070),(10,3071),(10,3080),(10,3081),(10,3110),(10,3111),(10,3113),(10,3119),(10,3121),(10,3125),(10,3126),(10,3129),(10,3130),(20,3000),(20,3010),(20,3011),(20,3012),(20,3013),(20,3014),(20,3020),(20,3021),(20,3022),(20,3023),(20,3024),(20,3030),(20,3031),(20,3032),(20,3033),(20,3034),(20,3040),(20,3041),(20,3042),(20,3043),(20,3044),(20,3050),(20,3051),(20,3052),(20,3060),(20,3061),(20,3062),(20,3070),(20,3071),(20,3072),(20,3080),(20,3081),(20,3090),(20,3091),(20,3092),(20,3100),(20,3101),(20,3102),(20,3103),(20,3104),(20,3110),(20,3111),(20,3112),(20,3113),(20,3114),(20,3115),(20,3116),(20,3117),(20,3118),(20,3120),(20,3121),(20,3122),(20,3123),(20,3124),(20,3125),(20,3127),(20,3128),(30,3000),(30,3050),(30,3051),(30,3060),(30,3061),(30,3070),(30,3071),(30,3080),(30,3081);
/*!40000 ALTER TABLE `sys_role_menu` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sys_user`
--

DROP TABLE IF EXISTS `sys_user`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_user` (
  `user_id` bigint NOT NULL AUTO_INCREMENT COMMENT '用户ID',
  `dept_id` bigint DEFAULT NULL COMMENT '部门ID',
  `user_name` varchar(30) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '用户账号',
  `nick_name` varchar(30) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '用户昵称',
  `user_type` varchar(2) COLLATE utf8mb4_unicode_ci DEFAULT '00' COMMENT '用户类型（00系统用户）',
  `email` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '用户邮箱',
  `phonenumber` varchar(11) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '手机号码',
  `sex` char(1) COLLATE utf8mb4_unicode_ci DEFAULT '0' COMMENT '用户性别（0男 1女 2未知）',
  `avatar` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '头像地址',
  `password` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '密码',
  `status` char(1) COLLATE utf8mb4_unicode_ci DEFAULT '0' COMMENT '账号状态（0正常 1停用）',
  `del_flag` char(1) COLLATE utf8mb4_unicode_ci DEFAULT '0' COMMENT '删除标志（0代表存在 2代表删除）',
  `login_ip` varchar(128) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '最后登录IP',
  `login_date` datetime DEFAULT NULL COMMENT '最后登录时间',
  `pwd_update_date` datetime DEFAULT NULL COMMENT '密码最后更新时间',
  `create_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`user_id`)
) ENGINE=InnoDB AUTO_INCREMENT=120 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户信息表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_user`
--

LOCK TABLES `sys_user` WRITE;
/*!40000 ALTER TABLE `sys_user` DISABLE KEYS */;
INSERT INTO `sys_user` VALUES (1,101,'admin','管理员','00','','','1','','$2a$10$faroG3ddhhsQwAf.kDHDgOvnuYyMRdjDa2c3vD7vZTy0Rg7pRN0qS','0','0','127.0.0.1','2026-10-06 06:09:23','2026-10-03 18:54:13','admin','2026-10-02 23:48:30','admin','2026-10-04 10:06:37','超级管理员'),(117,101,'finance','财务人员','00','','','1','','$2a$10$jnaGlQ2Jjn6LPbsRFbrv2eEoi2jLF1vp5ZQFVINFULInu9RQheG66','0','0','127.0.0.1','2026-10-05 17:23:08',NULL,'admin','2026-10-05 17:21:38','',NULL,'本地业务权限验收账号'),(118,101,'operator','运营人员','00','','','1','','$2a$10$Ve6b8rjRzDG/xTdEWTMpx.mEJKOA4zQ5kStpxAGOTJPFJk72OKHAW','0','0','127.0.0.1','2026-10-05 17:23:08',NULL,'admin','2026-10-05 17:21:39','',NULL,'本地业务权限验收账号'),(119,101,'verifier','核销员','00','','','1','','$2a$10$bZerqV8LbvTdrdqVUmYIbOQPL1pv3EYLMT1WnrPBtOYErj/gHUfYa','0','0','127.0.0.1','2026-10-05 17:24:43',NULL,'admin','2026-10-05 17:21:39','',NULL,'本地业务权限验收账号');
/*!40000 ALTER TABLE `sys_user` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sys_user_post`
--

DROP TABLE IF EXISTS `sys_user_post`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_user_post` (
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `post_id` bigint NOT NULL COMMENT '岗位ID',
  PRIMARY KEY (`user_id`,`post_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户与岗位关联表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_user_post`
--

LOCK TABLES `sys_user_post` WRITE;
/*!40000 ALTER TABLE `sys_user_post` DISABLE KEYS */;
INSERT INTO `sys_user_post` VALUES (1,1);
/*!40000 ALTER TABLE `sys_user_post` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sys_user_role`
--

DROP TABLE IF EXISTS `sys_user_role`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_user_role` (
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `role_id` bigint NOT NULL COMMENT '角色ID',
  PRIMARY KEY (`user_id`,`role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户和角色关联表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_user_role`
--

LOCK TABLES `sys_user_role` WRITE;
/*!40000 ALTER TABLE `sys_user_role` DISABLE KEYS */;
INSERT INTO `sys_user_role` VALUES (1,1),(117,10),(118,20),(119,30);
/*!40000 ALTER TABLE `sys_user_role` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping routines for database 'jinse_wine'
--
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-10-06  6:16:46
