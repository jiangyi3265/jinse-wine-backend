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
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='购物车';
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
INSERT INTO `biz_channel_config` VALUES ('wechat.merchant.appId','','2026-10-02 23:51:19'),('wechat.pay.enabled','false','2026-10-02 23:51:19'),('wechat.transfer.enabled','false','2026-10-02 23:51:19'),('wechat.video.finderUserName','','2026-10-02 23:51:19'),('wechat.video.shopId','','2026-10-02 23:51:19'),('wechat.video.syncEnabled','false','2026-10-02 23:51:19'),('wechat.work.groupQr','','2026-10-02 23:51:19'),('wechat.work.groupQrExpiresAt','','2026-10-02 23:51:19');
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
) ENGINE=InnoDB AUTO_INCREMENT=17 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='浼樻儬鍒告ā鏉';
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
INSERT INTO `biz_exchange_item` VALUES (1,'voucher20','20元代金券','coupon',4,'','/mini-assets/design/voucher.jpg',200,99,1,10,'0','seed','2026-10-02 23:45:52','seed','2026-10-03 19:09:09',NULL),(2,'cup','品鉴杯','goods',NULL,'精美陶瓷品鉴杯','/mini-assets/design/cup.jpg',300,30,1,20,'0','seed','2026-10-02 23:45:52','seed','2026-10-03 19:09:09',NULL),(3,'cups','双杯礼盒','goods',NULL,'金色酒业定制 · 双杯装','/mini-assets/design/cups.jpg',800,20,1,30,'0','seed','2026-10-02 23:45:52','seed','2026-10-03 19:09:09',NULL);
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
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='扫码抽奖活动';
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
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='抽奖二维码活动码';
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
  `status` char(1) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '0',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`prize_id`),
  UNIQUE KEY `uk_biz_lottery_prize_name` (`activity_id`,`prize_name`),
  KEY `idx_biz_lottery_prize_activity` (`activity_id`,`status`)
) ENGINE=InnoDB AUTO_INCREMENT=21 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='抽奖奖项及概率库存';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `biz_lottery_prize`
--

LOCK TABLES `biz_lottery_prize` WRITE;
/*!40000 ALTER TABLE `biz_lottery_prize` DISABLE KEYS */;
INSERT INTO `biz_lottery_prize` VALUES (1,1,'cash','现金红包',500,500,2000,0,NULL,-1,0,'','0','2026-10-02 23:51:19','2026-10-02 23:51:19'),(2,1,'points','积分奖励',5000,0,0,50,NULL,-1,0,'','0','2026-10-02 23:51:19','2026-10-02 23:51:19'),(3,1,'coupon','20元代金券',2500,0,0,0,4,-1,0,'','0','2026-10-02 23:51:19','2026-10-02 23:51:19'),(4,1,'goods','品鉴杯',2000,0,0,0,NULL,-1,0,'/mini-assets/design/cup.jpg','0','2026-10-02 23:51:19','2026-10-02 23:51:19'),(9,1,'cash','鐜伴噾绾㈠寘',500,500,2000,0,NULL,-1,0,'','1','2026-10-03 21:17:35','2026-10-03 23:13:08'),(10,1,'points','绉?垎濂栧姳',5000,0,0,50,NULL,-1,0,'','1','2026-10-03 21:17:35','2026-10-03 23:13:08'),(11,1,'coupon','20鍏冧唬閲戝埜',2500,0,0,0,4,-1,0,'','1','2026-10-03 21:17:35','2026-10-03 23:13:08'),(12,1,'goods','鍝侀壌鏉',2000,0,0,0,NULL,-1,0,'/mini-assets/design/cup.jpg','1','2026-10-03 21:17:35','2026-10-03 23:13:08');
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
  UNIQUE KEY `uk_biz_lottery_record_code` (`code_id`),
  KEY `idx_biz_lottery_record_member` (`member_id`,`create_time`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='抽奖流水';
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
) ENGINE=InnoDB AUTO_INCREMENT=8 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='灏忕▼搴忎細鍛';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `biz_member`
--

LOCK TABLES `biz_member` WRITE;
/*!40000 ALTER TABLE `biz_member` DISABLE KEYS */;
INSERT INTO `biz_member` VALUES (1,'local-h5-member',NULL,'微信用户','','',0,1,'0','2026-10-03 21:54:53','2026-10-03 23:00:47');
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
INSERT INTO `biz_member_address` VALUES (1,1,'张先生','13800002688','广西壮族自治区 贵港市 港北区','','','港北区金色酒业体验店示例地址',1,'2026-10-03 22:08:39','2026-10-03 22:08:39');
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
INSERT INTO `biz_member_coupon` VALUES (1,1,2,'CPdbcdf0fd76e44879a29a782b0a4b9d80','unused','{\"stock\": -1, \"end_time\": null, \"start_time\": null, \"amount_cent\": 1500, \"claim_limit\": 1, \"coupon_type\": \"fixed\", \"member_only\": false, \"points_only\": false, \"min_quantity\": 1, \"discount_rate\": 100, \"template_code\": \"full15\", \"template_name\": \"满减优惠券\", \"min_amount_cent\": 20000, \"max_discount_cent\": 0, \"coupon_template_id\": 2}',NULL,'2026-11-02 22:20:19','2026-10-03 22:20:18','2026-10-03 22:20:18');
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
INSERT INTO `biz_member_session` VALUES ('0ebee6215b0a7e84cea1fac640f50e5231ffa1732b80a79ff08b667c8160bc33',1,'2026-11-02 23:04:17','2026-10-03 23:04:16'),('21e593fa7f99aa9766ddab139c35a5a04627cc42f401ce504129ab32ed27d269',1,'2026-11-02 22:23:08','2026-10-03 22:23:08'),('2eb8808fb7c94d96a72e5f9dc6f680b780bccd616bd6b1f21a52d156e4424256',1,'2026-11-02 22:02:18','2026-10-03 22:02:18'),('47689916079b71b88a9155ad26304ded299ecf2efc6ce6a39d94bdb953d240f9',1,'2026-11-02 22:22:45','2026-10-03 22:22:44'),('68dd5db1e491cffd8f8ebdeb2356e3c2a50f77cf81205139b279f0ba028356e3',1,'2026-11-02 21:56:20','2026-10-03 21:56:20'),('72030c9a2c5461ee8e3e69d3852160e8db3998e9a3e324adad215ec64880fa57',1,'2026-11-02 21:54:53','2026-10-03 21:54:53'),('8be1b4d15a90b0e00575081326fccb8e0bd5cd0c9d49d31523a0b7e5309e7b66',1,'2026-11-02 22:23:31','2026-10-03 22:23:31');
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
) ENGINE=InnoDB AUTO_INCREMENT=89 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='灏忕▼搴忓叕寮?〉闈㈠唴瀹';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `biz_mini_content`
--

LOCK TABLES `biz_mini_content` WRITE;
/*!40000 ALTER TABLE `biz_mini_content` DISABLE KEYS */;
INSERT INTO `biz_mini_content` VALUES (1,'home.hero','banner','金色酒业','品质酱香，礼遇知己','{\"alt\": \"首页主视觉\"}','/mini-assets/design/hero-product.jpg','shop',10,'0','seed','2026-10-02 23:47:37','seed','2026-10-02 23:47:37','首页主视觉'),(2,'home.detail','image','品牌详情','','{\"alt\": \"品牌详情横幅\"}','/mini-assets/design/detail-landscape.jpg','',20,'0','seed','2026-10-02 23:47:37','seed','2026-10-02 23:47:37','详情横幅'),(3,'home.navigation','navigation','','','[{\"icon\": \"scan\", \"name\": \"扫码有礼\", \"route\": \"scan\"}, {\"icon\": \"shop\", \"name\": \"兑换网点\", \"route\": \"stores\"}, {\"icon\": \"coin\", \"name\": \"积分专区\", \"route\": \"points\"}, {\"icon\": \"users\", \"name\": \"VIP社群\", \"route\": \"vip\"}]','','',10,'0','seed','2026-10-02 23:47:37','seed','2026-10-02 23:47:37','首页快捷导航'),(4,'rules.shipping','rule','配送说明','','[{\"text\": \"具体配送范围和运费以结算页服务端报价为准。\", \"title\": \"配送范围\"}]','','',10,'0','seed','2026-10-02 23:47:37','seed','2026-10-02 23:47:37','规则页面内容'),(5,'rules.campaign','rule','活动规则','','[{\"text\": \"以后台配置的活动期限为准。\", \"title\": \"活动期限\"}]','','',20,'0','seed','2026-10-02 23:47:37','seed','2026-10-02 23:47:37','规则页面内容'),(6,'rules.points','rule','积分规则','','[{\"text\": \"积分到账和有效期以服务端记录为准。\", \"title\": \"积分获取\"}]','','',30,'0','seed','2026-10-02 23:47:37','seed','2026-10-02 23:47:37','规则页面内容'),(7,'rules.ranking','rule','排行榜规则','','[{\"text\": \"排行榜周期和奖励以后台活动配置为准。\", \"title\": \"统计周期\"}]','','',40,'0','seed','2026-10-02 23:47:37','seed','2026-10-02 23:47:37','规则页面内容'),(8,'rules.promotion','rule','推广规则','','[{\"text\": \"推广关系由服务端建立并确认。\", \"title\": \"推广关系\"}]','','',50,'0','seed','2026-10-02 23:47:37','seed','2026-10-02 23:47:37','规则页面内容'),(9,'rules.draw','rule','抽奖规则','','[{\"text\": \"活动码、次数、概率和奖池库存由服务端校验。\", \"title\": \"活动码\"}]','','',60,'0','seed','2026-10-02 23:47:37','seed','2026-10-02 23:47:37','规则页面内容'),(10,'rules.coupon','rule','优惠券规则','','[{\"text\": \"优惠券门槛、适用商品和有效期以服务端报价为准。\", \"title\": \"使用规则\"}]','','',70,'0','seed','2026-10-02 23:47:37','seed','2026-10-02 23:47:37','规则页面内容'),(11,'brand.avatar','image','默认头像','','{\"alt\": \"默认会员头像\"}','/mini-assets/design/avatar.jpg','',10,'0','seed','2026-10-02 23:47:37','seed','2026-10-02 23:47:37','默认头像图片'),(12,'brand.store','image','门店图片','','{\"alt\": \"门店展示图\"}','/mini-assets/design/store.jpg','',20,'0','seed','2026-10-02 23:47:37','seed','2026-10-02 23:47:37','门店默认图片'),(13,'brand.map','image','门店地图','','{\"alt\": \"门店地图\"}','/mini-assets/design/map.jpg','',30,'0','seed','2026-10-02 23:47:37','seed','2026-10-02 23:47:37','门店地图图片'),(14,'brand.points','image','积分页主图','','{\"alt\": \"积分页主图\"}','/mini-assets/design/detail-scene-clean.jpg','',40,'0','seed','2026-10-02 23:47:37','seed','2026-10-02 23:47:37','积分页图片'),(15,'brand.scan','image','扫码页主图','','{\"alt\": \"扫码页主图\"}','/mini-assets/design/detail-scene-clean.jpg','',50,'0','seed','2026-10-02 23:47:37','seed','2026-10-02 23:47:37','扫码页图片'),(16,'brand.ranking','image','排行榜主图','','{\"alt\": \"排行榜主图\"}','/mini-assets/design/landscape.jpg','',60,'0','seed','2026-10-02 23:47:37','seed','2026-10-02 23:47:37','排行榜图片'),(17,'brand.cup','image','品鉴杯图片','','{\"alt\": \"品鉴杯\"}','/mini-assets/design/cup.jpg','',70,'0','seed','2026-10-02 23:47:37','seed','2026-10-02 23:47:37','扫码页奖品图片'),(18,'brand.productDetail','image','商品详情图','','{\"alt\": \"商品详情图\"}','/mini-assets/design/detail-landscape.jpg','',80,'0','seed','2026-10-02 23:47:37','seed','2026-10-02 23:47:37','商品详情兜底图片'),(19,'brand.productStory','image','商品故事图','','{\"alt\": \"商品故事图\"}','/mini-assets/design/detail-scene-clean.jpg','',90,'0','seed','2026-10-02 23:47:37','seed','2026-10-02 23:47:37','商品故事图片'),(20,'brand.poster','image','推广海报主图','','{\"alt\": \"推广海报主图\"}','/mini-assets/design/detail-scene-clean.jpg','',100,'0','seed','2026-10-02 23:47:37','seed','2026-10-02 23:47:37','推广海报兜底图片'),(21,'brand.qr','image','二维码兜底图','','{\"alt\": \"二维码\"}','/mini-assets/design/qr-demo.png','',110,'0','seed','2026-10-02 23:47:37','seed','2026-10-02 23:47:37','推广二维码兜底图'),(22,'home.hero.2','banner','窖藏时光','匠心酿造，典藏好酒','{\"alt\": \"首页轮播图二\"}','/mini-assets/design/detail-scene.jpg','shop',20,'0','seed','2026-10-02 23:50:12','seed','2026-10-02 23:50:12','首页轮播图，可在后台替换图片'),(23,'home.hero.3','banner','举杯共饮','一杯好酒，礼遇知己','{\"alt\": \"首页轮播图三\"}','/mini-assets/design/landscape.jpg','shop',30,'0','seed','2026-10-02 23:50:12','seed','2026-10-02 23:50:12','首页轮播图，可在后台替换图片');
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
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='鍟嗗煄璁㈠崟';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `biz_order`
--

LOCK TABLES `biz_order` WRITE;
/*!40000 ALTER TABLE `biz_order` DISABLE KEYS */;
INSERT INTO `biz_order` VALUES (1,'JSCA3A4FB6C3644D5AB5AA',1,'{\"city\": \"\", \"detail\": \"港北区金色酒业体验店示例地址\", \"district\": \"\", \"province\": \"广西壮族自治区 贵港市 港北区\", \"address_id\": 1, \"receiver_name\": \"张先生\", \"receiver_phone\": \"13800002688\"}','delivery',NULL,39800,0,1200,41000,NULL,'cart-order-1791036696384-8tqfa','cancelled',NULL,NULL,'2026-10-03 22:16:59','2026-10-03 22:11:36','2026-10-03 22:16:59');
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='订单物流';
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
INSERT INTO `biz_order_idempotency` VALUES (1,'cart-order-1791036696384-8tqfa','646f69de1c20827b1a1b0456201908760c1dbe9633e0ec33b8bd63cc7af0980c',1,'2026-10-03 22:11:36');
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
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='璁㈠崟鍟嗗搧蹇?収';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `biz_order_item`
--

LOCK TABLES `biz_order_item` WRITE;
/*!40000 ALTER TABLE `biz_order_item` DISABLE KEYS */;
INSERT INTO `biz_order_item` VALUES (1,1,1,'','典藏酱香','53度 500mL','/mini-assets/design/product-classic.jpg',39800,1,39800);
/*!40000 ALTER TABLE `biz_order_item` ENABLE KEYS */;
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='商城门店自提核销';
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
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`refund_id`),
  UNIQUE KEY `uk_biz_order_refund_no` (`refund_no`),
  UNIQUE KEY `uk_biz_order_refund_request` (`member_id`,`request_id`),
  KEY `idx_biz_order_refund_order` (`order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='订单售后退款';
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
INSERT INTO `biz_point_account` VALUES (1,10,'2026-10-03 22:19:03');
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
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='绉?垎娴佹按';
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
) ENGINE=InnoDB AUTO_INCREMENT=16 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='积分获取及消费规则';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `biz_point_rule`
--

LOCK TABLES `biz_point_rule` WRITE;
/*!40000 ALTER TABLE `biz_point_rule` DISABLE KEYS */;
INSERT INTO `biz_point_rule` VALUES (1,'order_paid','商城消费积分','order_paid',1,-1,0,'0','{\"per_cent\": 1}','2026-10-02 23:51:19'),(2,'invite_register','邀请注册','invite_register',100,20,0,'0','{}','2026-10-02 23:51:19'),(3,'daily_sign','每日签到','daily_sign',10,1,0,'0','{}','2026-10-02 23:51:19');
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
INSERT INTO `biz_product` VALUES (1,'classic','典藏酱香','酱香','53度 500mL','金色典藏系列','53度酱香型 500mL','/mini-assets/design/product-classic.jpg',39800,49800,99,1286,10,'0','seed','2026-10-02 23:45:52','seed','2026-10-03 22:16:59','鍓嶇?婕旂ず鍒濆?鏁版嵁'),(2,'mellow','经典浓香','浓香','52度 500mL','金色经典系列','52度浓香型 500mL','/mini-assets/design/product-mellow.jpg',26800,32800,86,965,20,'0','seed','2026-10-02 23:45:52','seed','2026-10-03 19:09:09','鍓嶇?婕旂ず鍒濆?鏁版嵁'),(3,'double','双瓶礼盒','礼盒','53度 500mL × 2','金色礼赠系列','53度酱香型 双瓶礼盒','/mini-assets/design/product-double.jpg',59800,69800,50,658,30,'0','seed','2026-10-02 23:45:52','seed','2026-10-03 19:09:09','鍓嶇?婕旂ず鍒濆?鏁版嵁'),(4,'gift','珍藏礼盒','礼盒','53度 500mL','金色礼赠系列','53度酱香型 珍藏礼盒','/mini-assets/design/product-gift.jpg',89800,99800,30,328,40,'0','seed','2026-10-02 23:45:52','seed','2026-10-03 19:09:09','鍓嶇?婕旂ず鍒濆?鏁版嵁');
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='商品规格库存';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `biz_product_sku`
--

LOCK TABLES `biz_product_sku` WRITE;
/*!40000 ALTER TABLE `biz_product_sku` DISABLE KEYS */;
/*!40000 ALTER TABLE `biz_product_sku` ENABLE KEYS */;
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='推广佣金记录';
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
) ENGINE=InnoDB AUTO_INCREMENT=8 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='会员推广关系';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `biz_promotion_relation`
--

LOCK TABLES `biz_promotion_relation` WRITE;
/*!40000 ALTER TABLE `biz_promotion_relation` DISABLE KEYS */;
INSERT INTO `biz_promotion_relation` VALUES (1,1,NULL,1,'JS1PROMO','0','2026-10-03 21:54:53');
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
  `paid_at` datetime DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`settlement_id`),
  UNIQUE KEY `uk_biz_promotion_settlement_request` (`member_id`,`request_id`),
  KEY `idx_biz_promotion_settlement_member` (`member_id`,`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='佣金提现结算';
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
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='销冠天梯配置';
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
  `issued_at` datetime DEFAULT NULL,
  PRIMARY KEY (`reward_id`),
  UNIQUE KEY `uk_biz_ranking_reward_period_member` (`config_id`,`period_start`,`member_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='排行榜奖励发放';
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
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='闂ㄥ簵鏍搁攢璁板綍';
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
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='财务报表导出记录';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `biz_report_export`
--

LOCK TABLES `biz_report_export` WRITE;
/*!40000 ALTER TABLE `biz_report_export` DISABLE KEYS */;
INSERT INTO `biz_report_export` VALUES (1,'videoRefunds','{\"toDate\": \"2026-12-31\", \"fromDate\": \"2026-01-01\"}','videoRefunds-report-2026-01-01_2026-12-31.csv',0,'admin','2026-10-03 20:14:52');
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
  `exchange_code` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '',
  `image_url` varchar(512) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '' COMMENT '濂栧搧鍥剧墖鍦板潃',
  `redeem_store_id` bigint DEFAULT NULL,
  `status` varchar(16) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'pending' COMMENT 'pending/processing/received/redeemed/failed/closed',
  `expires_at` datetime DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`reward_id`),
  UNIQUE KEY `uk_biz_reward_exchange_code` (`exchange_code`),
  KEY `idx_biz_reward_member_status` (`member_id`,`status`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='濂栧搧涓庣孩鍖呮潈鐩';
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='微信现金奖商户转账尝试';
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='VIP社群运营活动';
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
-- Table structure for table `biz_social_subscription`
--

DROP TABLE IF EXISTS `biz_social_subscription`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `biz_social_subscription` (
  `subscription_id` bigint NOT NULL AUTO_INCREMENT,
  `member_id` bigint NOT NULL,
  `template_id` varchar(128) COLLATE utf8mb4_unicode_ci NOT NULL,
  `status` varchar(16) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'available' COMMENT 'available/reserved/consumed/rejected',
  `delivery_id` bigint DEFAULT NULL,
  `reserved_until` datetime DEFAULT NULL,
  `granted_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `consumed_at` datetime DEFAULT NULL,
  PRIMARY KEY (`subscription_id`),
  KEY `idx_biz_social_subscription_member` (`member_id`,`template_id`,`status`,`granted_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='会员主动授权的一次性订阅消息额度';
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
  `sort_num` int NOT NULL DEFAULT '0',
  `status` char(1) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '0' COMMENT '鐘舵?锛?钀ヤ笟 1鍋滅敤锛',
  `create_by` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_by` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `remark` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`store_id`),
  UNIQUE KEY `uk_biz_store_code` (`store_code`),
  KEY `idx_biz_store_status_sort` (`status`,`sort_num`)
) ENGINE=InnoDB AUTO_INCREMENT=9 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='鍏戞崲闂ㄥ簵';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `biz_store`
--

LOCK TABLES `biz_store` WRITE;
/*!40000 ALTER TABLE `biz_store` DISABLE KEYS */;
INSERT INTO `biz_store` VALUES (1,'guigang','金色酒业体验店','贵港','港北区','贵港市港北区 示例门店地址','',NULL,NULL,'09:00 - 21:00','/mini-assets/design/store.jpg','','',10,'0','seed','2026-10-02 23:45:52','seed','2026-10-03 19:09:09','璇锋浛鎹?负姝ｅ紡鍦板潃鍜屽潗鏍'),(2,'partner','金色酒业合作门店','贵港','港南区','贵港市港南区 示例门店地址','',NULL,NULL,'09:00 - 21:00','/mini-assets/design/store.jpg','','',20,'0','seed','2026-10-02 23:45:52','seed','2026-10-03 19:09:09','璇锋浛鎹?负姝ｅ紡鍦板潃鍜屽潗鏍');
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
) ENGINE=InnoDB AUTO_INCREMENT=150 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统访问记录';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_logininfor`
--

LOCK TABLES `sys_logininfor` WRITE;
/*!40000 ALTER TABLE `sys_logininfor` DISABLE KEYS */;
INSERT INTO `sys_logininfor` VALUES (100,'18042618531','127.0.0.1','内网IP','Edge 154','Windows >=10','1','用户不存在/密码错误','2026-10-03 18:48:05'),(101,'admin','127.0.0.1','内网IP','Edge 154','Windows >=10','1','用户不存在/密码错误','2026-10-03 18:48:06'),(102,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-03 18:55:23'),(103,'admin','127.0.0.1','内网IP','Edge 154','Windows >=10','0','登录成功','2026-10-03 18:57:49'),(104,'admin','127.0.0.1','内网IP','Chrome 154','Windows10','0','登录成功','2026-10-03 19:12:53'),(105,'admin','127.0.0.1','内网IP','Chrome 154','Windows10','0','退出成功','2026-10-03 19:16:44'),(106,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-03 19:24:55'),(107,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-03 19:25:13'),(108,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-03 19:26:56'),(109,'admin','127.0.0.1','内网IP','Chrome 154','Windows10','0','登录成功','2026-10-03 19:34:43'),(110,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-03 20:01:07'),(111,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-03 20:01:21'),(112,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-03 20:01:34'),(113,'admin','127.0.0.1','内网IP','Chrome 154','Windows10','0','登录成功','2026-10-03 20:01:59'),(114,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-03 20:09:41'),(115,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-03 20:14:52'),(116,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-03 20:31:30'),(117,'admin','127.0.0.1','内网IP','Chrome 154','Windows10','0','登录成功','2026-10-03 20:37:51'),(118,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-03 21:07:08'),(119,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-03 21:07:25'),(120,'admin','127.0.0.1','内网IP','Chrome 154','Windows10','0','登录成功','2026-10-03 21:13:06'),(121,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-03 21:24:38'),(122,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-03 22:27:50'),(123,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-03 22:28:14'),(124,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-03 22:56:25'),(125,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-03 22:56:58'),(126,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-03 23:00:47'),(127,'admin','127.0.0.1','内网IP','Chrome 154','Windows10','0','登录成功','2026-10-03 23:11:21'),(128,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-03 23:12:44'),(129,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-03 23:13:08'),(130,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-03 23:18:54'),(131,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-03 23:19:49'),(132,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-03 23:21:27'),(133,'qa_finance_20261003232127','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','1','用户不存在/密码错误','2026-10-03 23:21:27'),(134,'qa_operator_20261003232127','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','1','用户不存在/密码错误','2026-10-03 23:21:27'),(135,'qa_verifier_20261003232127','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','1','用户不存在/密码错误','2026-10-03 23:21:27'),(136,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-03 23:22:08'),(137,'qa_role_probe_20261003232207','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','1','用户不存在/密码错误','2026-10-03 23:22:08'),(138,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-03 23:23:09'),(139,'qa_role_probe_20261003232309','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','1','用户不存在/密码错误','2026-10-03 23:23:09'),(140,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-03 23:26:49'),(141,'qa_role_probe_20261003232649','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','1','用户不存在/密码错误','2026-10-03 23:26:49'),(142,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-03 23:28:20'),(143,'qa_role_probe_20261003232820','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','1','用户不存在/密码错误','2026-10-03 23:28:20'),(144,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-03 23:37:09'),(145,'qa_finance_20261003233709','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','1','用户不存在/密码错误','2026-10-03 23:37:09'),(146,'qa_operator_20261003233709','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','1','用户不存在/密码错误','2026-10-03 23:37:10'),(147,'qa_verifier_20261003233709','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','1','用户不存在/密码错误','2026-10-03 23:37:10'),(148,'admin','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','0','登录成功','2026-10-03 23:37:56'),(149,'qa_single_20261003233756','127.0.0.1','内网IP','PowerShell 7.6.5','Windows 10.0','1','用户不存在/密码错误','2026-10-03 23:37:57');
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
INSERT INTO `sys_menu` VALUES (3000,'业务管理',0,10,'business','Layout','','Business',1,0,'M','0','0','','shopping','admin','2026-10-02 23:51:58','',NULL,'金色酒业业务管理'),(3010,'商品管理',3000,1,'product','business/product/index','','BusinessProduct',1,0,'C','0','0','business:product:list','product','admin','2026-10-02 23:51:58','',NULL,''),(3011,'商品新增',3010,1,'','','','',1,0,'F','0','0','business:product:add','#','admin','2026-10-02 23:51:58','',NULL,''),(3012,'商品修改',3010,2,'','','','',1,0,'F','0','0','business:product:edit','#','admin','2026-10-02 23:51:58','',NULL,''),(3013,'商品删除',3010,3,'','','','',1,0,'F','0','0','business:product:remove','#','admin','2026-10-02 23:51:58','',NULL,''),(3014,'商品查询',3010,4,'','','','',1,0,'F','0','0','business:product:query','#','admin','2026-10-02 23:51:58','',NULL,''),(3020,'门店管理',3000,2,'store','business/store/index','','BusinessStore',1,0,'C','0','0','business:store:list','tree-table','admin','2026-10-02 23:51:58','',NULL,''),(3021,'门店新增',3020,1,'','','','',1,0,'F','0','0','business:store:add','#','admin','2026-10-02 23:51:58','',NULL,''),(3022,'门店修改',3020,2,'','','','',1,0,'F','0','0','business:store:edit','#','admin','2026-10-02 23:51:58','',NULL,''),(3023,'门店删除',3020,3,'','','','',1,0,'F','0','0','business:store:remove','#','admin','2026-10-02 23:51:58','',NULL,''),(3024,'门店查询',3020,4,'','','','',1,0,'F','0','0','business:store:query','#','admin','2026-10-02 23:51:58','',NULL,''),(3030,'优惠券模板',3000,3,'coupon','business/coupon/index','','BusinessCoupon',1,0,'C','0','0','business:couponTemplate:list','discount','admin','2026-10-02 23:51:58','',NULL,''),(3031,'券模板新增',3030,1,'','','','',1,0,'F','0','0','business:couponTemplate:add','#','admin','2026-10-02 23:51:58','',NULL,''),(3032,'券模板修改',3030,2,'','','','',1,0,'F','0','0','business:couponTemplate:edit','#','admin','2026-10-02 23:51:58','',NULL,''),(3033,'券模板删除',3030,3,'','','','',1,0,'F','0','0','business:couponTemplate:remove','#','admin','2026-10-02 23:51:58','',NULL,''),(3034,'券模板查询',3030,4,'','','','',1,0,'F','0','0','business:couponTemplate:query','#','admin','2026-10-02 23:51:58','',NULL,''),(3040,'积分兑换',3000,4,'exchange','business/exchange/index','','BusinessExchange',1,0,'C','0','0','business:exchangeItem:list','money','admin','2026-10-02 23:51:58','',NULL,''),(3041,'兑换项新增',3040,1,'','','','',1,0,'F','0','0','business:exchangeItem:add','#','admin','2026-10-02 23:51:58','',NULL,''),(3042,'兑换项修改',3040,2,'','','','',1,0,'F','0','0','business:exchangeItem:edit','#','admin','2026-10-02 23:51:58','',NULL,''),(3043,'兑换项删除',3040,3,'','','','',1,0,'F','0','0','business:exchangeItem:remove','#','admin','2026-10-02 23:51:58','',NULL,''),(3044,'兑换项查询',3040,4,'','','','',1,0,'F','0','0','business:exchangeItem:query','#','admin','2026-10-02 23:51:58','',NULL,''),(3050,'订单管理',3000,5,'order','business/order/index','','BusinessOrder',1,0,'C','0','0','business:order:list','shopping','admin','2026-10-02 23:51:58','',NULL,''),(3051,'订单查询',3050,1,'','','','',1,0,'F','0','0','business:order:query','#','admin','2026-10-02 23:51:58','',NULL,''),(3052,'订单处理',3050,2,'','','','',1,0,'F','0','0','business:order:edit','#','admin','2026-10-02 23:51:58','',NULL,''),(3060,'会员管理',3000,6,'member','business/member/index','','BusinessMember',1,0,'C','0','0','business:member:list','user','admin','2026-10-02 23:51:58','',NULL,''),(3061,'会员查询',3060,1,'','','','',1,0,'F','0','0','business:member:query','#','admin','2026-10-02 23:51:58','',NULL,''),(3062,'会员处理',3060,2,'','','','',1,0,'F','0','0','business:member:edit','#','admin','2026-10-02 23:51:58','',NULL,''),(3070,'奖品权益',3000,7,'reward','business/reward/index','','BusinessReward',1,0,'C','0','0','business:reward:list','gift','admin','2026-10-02 23:51:58','',NULL,''),(3071,'奖品查询',3070,1,'','','','',1,0,'F','0','0','business:reward:query','#','admin','2026-10-02 23:51:58','',NULL,''),(3072,'奖品处理',3070,2,'','','','',1,0,'F','0','0','business:reward:edit','#','admin','2026-10-02 23:51:58','',NULL,''),(3080,'核销记录',3000,8,'redemption','business/redemption/index','','BusinessRedemption',1,0,'C','0','0','business:redemption:list','documentation','admin','2026-10-02 23:51:58','',NULL,''),(3081,'核销查询',3080,1,'','','','',1,0,'F','0','0','business:redemption:query','#','admin','2026-10-02 23:51:58','',NULL,''),(3090,'品牌配置',3000,9,'brand','business/brand/index','','BusinessBrand',1,0,'C','0','0','business:brand:query','edit','admin','2026-10-02 23:51:58','',NULL,''),(3091,'品牌查询',3090,1,'','','','',1,0,'F','0','0','business:brand:query','#','admin','2026-10-02 23:51:58','',NULL,''),(3092,'品牌修改',3090,2,'','','','',1,0,'F','0','0','business:brand:edit','#','admin','2026-10-02 23:51:58','',NULL,''),(3100,'小程序内容',3000,10,'content','business/content/index','','BusinessContent',1,0,'C','0','0','business:pageContent:list','documentation','admin','2026-10-02 23:51:58','',NULL,'首页、活动、规则和导航内容'),(3101,'内容查询',3100,1,'','','','',1,0,'F','0','0','business:pageContent:query','#','admin','2026-10-02 23:51:58','',NULL,''),(3102,'内容新增',3100,2,'','','','',1,0,'F','0','0','business:pageContent:add','#','admin','2026-10-02 23:51:58','',NULL,''),(3103,'内容修改',3100,3,'','','','',1,0,'F','0','0','business:pageContent:edit','#','admin','2026-10-02 23:51:58','',NULL,''),(3104,'内容删除',3100,4,'','','','',1,0,'F','0','0','business:pageContent:remove','#','admin','2026-10-02 23:51:58','',NULL,''),(3110,'运营中心',3000,11,'operations','business/operations/index','','BusinessOperations',1,0,'C','0','0','','data-analysis','admin','2026-10-02 23:51:58','',NULL,'抽奖、积分、分销、渠道、报表'),(3111,'运营中心查询',3110,1,'','','','',1,0,'F','0','0','business:dashboard:query','#','admin','2026-10-02 23:51:58','',NULL,''),(3112,'抽奖码生成',3110,2,'','','','',1,0,'F','0','0','business:lottery:generate','#','admin','2026-10-02 23:51:58','',NULL,''),(3113,'抽奖记录查询',3110,3,'','','','',1,0,'F','0','0','business:lottery:list','#','admin','2026-10-02 23:51:58','',NULL,''),(3114,'积分规则管理',3110,4,'','','','',1,0,'F','0','0','business:pointRule:edit','#','admin','2026-10-02 23:51:58','',NULL,''),(3115,'分销规则管理',3110,5,'','','','',1,0,'F','0','0','business:promotion:edit','#','admin','2026-10-02 23:51:58','',NULL,''),(3116,'排行榜奖励发放',3110,6,'','','','',1,0,'F','0','0','business:ranking:issue','#','admin','2026-10-02 23:51:58','',NULL,''),(3117,'社群通知发送',3110,7,'','','','',1,0,'F','0','0','business:social:send','#','admin','2026-10-02 23:51:58','',NULL,''),(3118,'微信渠道配置',3110,8,'','','','',1,0,'F','0','0','business:channel:edit','#','admin','2026-10-02 23:51:58','',NULL,''),(3119,'财务报表导出',3110,9,'','','','',1,0,'F','0','0','business:report:export','#','admin','2026-10-02 23:51:58','',NULL,''),(3120,'积分规则查询',3110,10,'','','','',1,0,'F','0','0','business:pointRule:list','#','admin','2026-10-02 23:51:58','',NULL,''),(3121,'分销规则查询',3110,11,'','','','',1,0,'F','0','0','business:promotion:query','#','admin','2026-10-02 23:51:58','',NULL,''),(3122,'排行榜查询',3110,12,'','','','',1,0,'F','0','0','business:ranking:query','#','admin','2026-10-02 23:51:58','',NULL,''),(3123,'社群活动新增',3110,13,'','','','',1,0,'F','0','0','business:social:add','#','admin','2026-10-02 23:51:58','',NULL,''),(3124,'渠道配置查询',3110,14,'','','','',1,0,'F','0','0','business:channel:query','#','admin','2026-10-02 23:51:58','',NULL,''),(3125,'退款查询',3110,15,'','','','',1,0,'F','0','0','business:refund:list','#','admin','2026-10-02 23:51:58','',NULL,''),(3126,'退款处理',3110,16,'','','','',1,0,'F','0','0','business:refund:edit','#','admin','2026-10-02 23:51:58','',NULL,''),(3127,'社群活动查询',3110,17,'','','','',1,0,'F','0','0','business:social:list','#','admin','2026-10-02 23:51:58','',NULL,''),(3128,'抽奖奖项管理',3110,18,'','','','',1,0,'F','0','0','business:lottery:edit','#','admin','2026-10-02 23:51:58','',NULL,''),(3129,'佣金提现查询',3110,19,'','','','',1,0,'F','0','0','business:settlement:list','#','admin','2026-10-02 23:51:58','',NULL,''),(3130,'佣金提现处理',3110,20,'','','','',1,0,'F','0','0','business:settlement:edit','#','admin','2026-10-02 23:51:58','',NULL,'');
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
) ENGINE=InnoDB AUTO_INCREMENT=133 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='操作日志记录';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_oper_log`
--

LOCK TABLES `sys_oper_log` WRITE;
/*!40000 ALTER TABLE `sys_oper_log` DISABLE KEYS */;
INSERT INTO `sys_oper_log` VALUES (100,'短视频商品挂载配置',1,'com.ruoyi.web.controller.business.BusinessExtensionController.saveVideoFeedMount()','POST',1,'admin','默认部门','/business/extension/video/mounts','127.0.0.1','内网IP','{\"externalProductId\":\"wx-prod-demo\",\"feedId\":\"feed-demo\",\"mountTitle\":\"典藏酱香短视频\",\"productId\":1,\"nonceId\":\"nonce-demo\"} ','{\"msg\":\"操作成功\",\"code\":200,\"data\":{\"mountId\":1,\"productId\":1,\"externalProductId\":\"wx-prod-demo\",\"feedId\":\"feed-demo\",\"nonceId\":\"nonce-demo\",\"mountTitle\":\"典藏酱香短视频\",\"status\":\"draft\",\"lastError\":\"\",\"requestId\":\"\"}}',0,NULL,'2026-10-03 20:09:41',22),(101,'短视频商品挂载提交',2,'com.ruoyi.web.controller.business.BusinessExtensionController.submitVideoFeedMount()','POST',1,'admin','默认部门','/business/extension/video/mounts/1/submit','127.0.0.1','内网IP','1 ','{\"msg\":\"操作成功\",\"code\":200,\"data\":{\"mountId\":1,\"requestId\":\"713ccbde1b964e5c9b0e324380050d71\",\"status\":\"submitting\",\"feedId\":\"feed-demo\",\"nonceId\":\"nonce-demo\",\"externalProductId\":\"wx-prod-demo\",\"message\":\"已生成短视频挂载清单，请在视频号助手提交商品并回到此处确认平台结果\",\"requiresPlatformConfirmation\":true}}',0,NULL,'2026-10-03 20:09:41',9),(102,'短视频商品挂载结果确认',2,'com.ruoyi.web.controller.business.BusinessExtensionController.verifyVideoFeedMount()','PUT',1,'admin','默认部门','/business/extension/video/mounts/1/verify','127.0.0.1','内网IP','1 {\"platformConfirmed\":true,\"status\":\"verified\"} ','{\"msg\":\"操作成功\",\"code\":200,\"data\":{\"mountId\":1,\"productId\":1,\"externalProductId\":\"wx-prod-demo\",\"feedId\":\"feed-demo\",\"nonceId\":\"nonce-demo\",\"mountTitle\":\"典藏酱香短视频\",\"status\":\"verified\",\"lastError\":\"\",\"requestId\":\"713ccbde1b964e5c9b0e324380050d71\",\"submittedAt\":\"2026-10-03 20:09:41\",\"verifiedAt\":\"2026-10-03 20:09:41\"}}',0,NULL,'2026-10-03 20:09:41',11),(103,'业务财务报表',5,'com.ruoyi.web.controller.business.BusinessExtensionController.export()','GET',1,'admin','默认部门','/business/extension/reports/videoRefunds/export','127.0.0.1','内网IP','{\"fromDate\":\"2026-01-01\",\"toDate\":\"2026-12-31\"}',NULL,0,NULL,'2026-10-03 20:14:52',12),(104,'业务状态',2,'com.ruoyi.web.controller.business.BusinessAdminController.status()','PUT',1,'admin','默认部门','/business/member/1/status','127.0.0.1','内网IP','\"member\" \"1\" {\"staffStoreId\":1,\"status\":\"0\"} ','{\"msg\":\"操作成功\",\"code\":200}',0,NULL,'2026-10-03 23:00:47',10),(105,'抽奖奖品',2,'com.ruoyi.web.controller.business.BusinessExtensionController.disableLotteryPrize()','POST',1,'admin','默认部门','/business/extension/lottery/prizes/9/disable','127.0.0.1','内网IP','9 ','{\"msg\":\"操作成功\",\"code\":200}',0,NULL,'2026-10-03 23:13:08',6),(106,'抽奖奖品',2,'com.ruoyi.web.controller.business.BusinessExtensionController.disableLotteryPrize()','POST',1,'admin','默认部门','/business/extension/lottery/prizes/11/disable','127.0.0.1','内网IP','11 ','{\"msg\":\"操作成功\",\"code\":200}',0,NULL,'2026-10-03 23:13:08',4),(107,'抽奖奖品',2,'com.ruoyi.web.controller.business.BusinessExtensionController.disableLotteryPrize()','POST',1,'admin','默认部门','/business/extension/lottery/prizes/10/disable','127.0.0.1','内网IP','10 ','{\"msg\":\"操作成功\",\"code\":200}',0,NULL,'2026-10-03 23:13:08',5),(108,'抽奖奖品',2,'com.ruoyi.web.controller.business.BusinessExtensionController.disableLotteryPrize()','POST',1,'admin','默认部门','/business/extension/lottery/prizes/12/disable','127.0.0.1','内网IP','12 ','{\"msg\":\"操作成功\",\"code\":200}',0,NULL,'2026-10-03 23:13:08',4),(109,'用户管理',1,'com.ruoyi.web.controller.system.SysUserController.add()','POST',1,'admin','默认部门','/system/user','127.0.0.1','内网IP','{\"admin\":false,\"createBy\":\"admin\",\"deptId\":101,\"nickName\":\"qa_finance_20261003232127\",\"params\":{},\"postIds\":[],\"remark\":\"temporary permission verification\",\"roleIds\":[10],\"sex\":\"1\",\"status\":\"0\",\"userId\":100,\"userName\":\"qa_finance_20261003232127\"} ','{\"msg\":\"操作成功\",\"code\":200}',0,NULL,'2026-10-03 23:21:27',87),(110,'用户管理',1,'com.ruoyi.web.controller.system.SysUserController.add()','POST',1,'admin','默认部门','/system/user','127.0.0.1','内网IP','{\"admin\":false,\"createBy\":\"admin\",\"deptId\":101,\"nickName\":\"qa_operator_20261003232127\",\"params\":{},\"postIds\":[],\"remark\":\"temporary permission verification\",\"roleIds\":[20],\"sex\":\"1\",\"status\":\"0\",\"userId\":101,\"userName\":\"qa_operator_20261003232127\"} ','{\"msg\":\"操作成功\",\"code\":200}',0,NULL,'2026-10-03 23:21:27',68),(111,'用户管理',1,'com.ruoyi.web.controller.system.SysUserController.add()','POST',1,'admin','默认部门','/system/user','127.0.0.1','内网IP','{\"admin\":false,\"createBy\":\"admin\",\"deptId\":101,\"nickName\":\"qa_verifier_20261003232127\",\"params\":{},\"postIds\":[],\"remark\":\"temporary permission verification\",\"roleIds\":[30],\"sex\":\"1\",\"status\":\"0\",\"userId\":102,\"userName\":\"qa_verifier_20261003232127\"} ','{\"msg\":\"操作成功\",\"code\":200}',0,NULL,'2026-10-03 23:21:27',67),(112,'用户管理',3,'com.ruoyi.web.controller.system.SysUserController.remove()','DELETE',1,'admin','默认部门','/system/user/100','127.0.0.1','内网IP','[100] ','{\"msg\":\"操作成功\",\"code\":200}',0,NULL,'2026-10-03 23:21:27',12),(113,'用户管理',3,'com.ruoyi.web.controller.system.SysUserController.remove()','DELETE',1,'admin','默认部门','/system/user/101','127.0.0.1','内网IP','[101] ','{\"msg\":\"操作成功\",\"code\":200}',0,NULL,'2026-10-03 23:21:27',10),(114,'用户管理',3,'com.ruoyi.web.controller.system.SysUserController.remove()','DELETE',1,'admin','默认部门','/system/user/102','127.0.0.1','内网IP','[102] ','{\"msg\":\"操作成功\",\"code\":200}',0,NULL,'2026-10-03 23:21:27',9),(115,'用户管理',1,'com.ruoyi.web.controller.system.SysUserController.add()','POST',1,'admin','默认部门','/system/user','127.0.0.1','内网IP','{\"admin\":false,\"createBy\":\"admin\",\"deptId\":101,\"nickName\":\"qa_role_probe_20261003232207\",\"params\":{},\"postIds\":[],\"remark\":\"probe\",\"roleIds\":[30],\"sex\":\"1\",\"status\":\"0\",\"userId\":103,\"userName\":\"qa_role_probe_20261003232207\"} ','{\"msg\":\"操作成功\",\"code\":200}',0,NULL,'2026-10-03 23:22:08',68),(116,'用户管理',3,'com.ruoyi.web.controller.system.SysUserController.remove()','DELETE',1,'admin','默认部门','/system/user/103','127.0.0.1','内网IP','[103] ','{\"msg\":\"操作成功\",\"code\":200}',0,NULL,'2026-10-03 23:22:08',10),(117,'用户管理',1,'com.ruoyi.web.controller.system.SysUserController.add()','POST',1,'admin','默认部门','/system/user','127.0.0.1','内网IP','{\"admin\":false,\"createBy\":\"admin\",\"deptId\":101,\"nickName\":\"qa_role_probe_20261003232309\",\"params\":{},\"postIds\":[],\"remark\":\"probe\",\"roleIds\":[30],\"sex\":\"1\",\"status\":\"0\",\"userId\":104,\"userName\":\"qa_role_probe_20261003232309\"} ','{\"msg\":\"操作成功\",\"code\":200}',0,NULL,'2026-10-03 23:23:09',68),(118,'用户管理',3,'com.ruoyi.web.controller.system.SysUserController.remove()','DELETE',1,'admin','默认部门','/system/user/104','127.0.0.1','内网IP','[104] ','{\"msg\":\"操作成功\",\"code\":200}',0,NULL,'2026-10-03 23:23:09',13),(119,'用户管理',1,'com.ruoyi.web.controller.system.SysUserController.add()','POST',1,'admin','默认部门','/system/user','127.0.0.1','内网IP','{\"admin\":false,\"createBy\":\"admin\",\"deptId\":101,\"nickName\":\"qa_role_probe_20261003232649\",\"params\":{},\"postIds\":[],\"remark\":\"probe\",\"sex\":\"1\",\"status\":\"0\",\"userId\":105,\"userName\":\"qa_role_probe_20261003232649\"} ','{\"msg\":\"操作成功\",\"code\":200}',0,NULL,'2026-10-03 23:26:49',70),(120,'用户管理',4,'com.ruoyi.web.controller.system.SysUserController.insertAuthRole()','PUT',1,'admin','默认部门','/system/user/authRole','127.0.0.1','内网IP','{\"roleIds\":\"30\",\"userId\":\"105\"}','{\"msg\":\"操作成功\",\"code\":200}',0,NULL,'2026-10-03 23:26:49',9),(121,'用户管理',3,'com.ruoyi.web.controller.system.SysUserController.remove()','DELETE',1,'admin','默认部门','/system/user/105','127.0.0.1','内网IP','[105] ','{\"msg\":\"操作成功\",\"code\":200}',0,NULL,'2026-10-03 23:26:49',14),(122,'用户管理',1,'com.ruoyi.web.controller.system.SysUserController.add()','POST',1,'admin','默认部门','/system/user','127.0.0.1','内网IP','{\"admin\":false,\"createBy\":\"admin\",\"deptId\":101,\"nickName\":\"qa_role_probe_20261003232820\",\"params\":{},\"postIds\":[],\"remark\":\"probe\",\"roleIds\":[30],\"sex\":\"1\",\"status\":\"0\",\"userId\":106,\"userName\":\"qa_role_probe_20261003232820\"} ','{\"msg\":\"操作成功\",\"code\":200}',0,NULL,'2026-10-03 23:28:20',67),(123,'用户管理',2,'com.ruoyi.web.controller.system.SysUserController.resetPwd()','PUT',1,'admin','默认部门','/system/user/resetPwd','127.0.0.1','内网IP','{\"admin\":false,\"params\":{},\"updateBy\":\"admin\",\"userId\":106} ','{\"msg\":\"操作成功\",\"code\":200}',0,NULL,'2026-10-03 23:28:20',59),(124,'用户管理',4,'com.ruoyi.web.controller.system.SysUserController.insertAuthRole()','PUT',1,'admin','默认部门','/system/user/authRole','127.0.0.1','内网IP','{\"roleIds\":\"30\",\"userId\":\"106\"}','{\"msg\":\"操作成功\",\"code\":200}',0,NULL,'2026-10-03 23:28:20',8),(125,'用户管理',3,'com.ruoyi.web.controller.system.SysUserController.remove()','DELETE',1,'admin','默认部门','/system/user/106','127.0.0.1','内网IP','[106] ','{\"msg\":\"操作成功\",\"code\":200}',0,NULL,'2026-10-03 23:28:20',11),(126,'用户管理',1,'com.ruoyi.web.controller.system.SysUserController.add()','POST',1,'admin','默认部门','/system/user','127.0.0.1','内网IP','{\"admin\":false,\"createBy\":\"admin\",\"deptId\":101,\"nickName\":\"qa_finance_20261003233709\",\"params\":{},\"postIds\":[],\"remark\":\"temporary permission verification\",\"roleIds\":[],\"sex\":\"1\",\"status\":\"0\",\"userId\":107,\"userName\":\"qa_finance_20261003233709\"} ','{\"msg\":\"操作成功\",\"code\":200}',0,NULL,'2026-10-03 23:37:09',66),(127,'用户管理',4,'com.ruoyi.web.controller.system.SysUserController.insertAuthRole()','PUT',1,'admin','默认部门','/system/user/authRole','127.0.0.1','内网IP','{\"roleIds\":\"10\",\"userId\":\"107\"}','{\"msg\":\"操作成功\",\"code\":200}',0,NULL,'2026-10-03 23:37:09',7),(128,'用户管理',1,'com.ruoyi.web.controller.system.SysUserController.add()','POST',1,'admin','默认部门','/system/user','127.0.0.1','内网IP','{\"admin\":false,\"createBy\":\"admin\",\"deptId\":101,\"nickName\":\"qa_operator_20261003233709\",\"params\":{},\"postIds\":[],\"remark\":\"temporary permission verification\",\"roleIds\":[],\"sex\":\"1\",\"status\":\"0\",\"userId\":108,\"userName\":\"qa_operator_20261003233709\"} ','{\"msg\":\"操作成功\",\"code\":200}',0,NULL,'2026-10-03 23:37:09',64),(129,'用户管理',4,'com.ruoyi.web.controller.system.SysUserController.insertAuthRole()','PUT',1,'admin','默认部门','/system/user/authRole','127.0.0.1','内网IP','{\"roleIds\":\"20\",\"userId\":\"108\"}','{\"msg\":\"操作成功\",\"code\":200}',0,NULL,'2026-10-03 23:37:10',8),(130,'用户管理',1,'com.ruoyi.web.controller.system.SysUserController.add()','POST',1,'admin','默认部门','/system/user','127.0.0.1','内网IP','{\"admin\":false,\"createBy\":\"admin\",\"deptId\":101,\"nickName\":\"qa_verifier_20261003233709\",\"params\":{},\"postIds\":[],\"remark\":\"temporary permission verification\",\"roleIds\":[],\"sex\":\"1\",\"status\":\"0\",\"userId\":109,\"userName\":\"qa_verifier_20261003233709\"} ','{\"msg\":\"操作成功\",\"code\":200}',0,NULL,'2026-10-03 23:37:10',64),(131,'用户管理',4,'com.ruoyi.web.controller.system.SysUserController.insertAuthRole()','PUT',1,'admin','默认部门','/system/user/authRole','127.0.0.1','内网IP','{\"roleIds\":\"30\",\"userId\":\"109\"}','{\"msg\":\"操作成功\",\"code\":200}',0,NULL,'2026-10-03 23:37:10',9),(132,'用户管理',1,'com.ruoyi.web.controller.system.SysUserController.add()','POST',1,'admin','默认部门','/system/user','127.0.0.1','内网IP','{\"admin\":false,\"createBy\":\"admin\",\"deptId\":101,\"nickName\":\"qa_single_20261003233756\",\"params\":{},\"postIds\":[],\"remark\":\"probe\",\"roleIds\":[],\"sex\":\"1\",\"status\":\"0\",\"userId\":110,\"userName\":\"qa_single_20261003233756\"} ','{\"msg\":\"操作成功\",\"code\":200}',0,NULL,'2026-10-03 23:37:56',63);
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
INSERT INTO `sys_role` VALUES (1,'超级管理员','admin',1,'1',1,1,'0','0','admin','2026-10-02 23:48:30','',NULL,'超级管理员'),(10,'财务人员','finance',10,'1',1,1,'0','0','admin','2026-10-03 19:24:23','admin','2026-10-03 21:23:53','订单、退款、佣金和财务报表'),(20,'运营人员','operator',20,'1',1,1,'0','0','admin','2026-10-03 19:24:23','admin','2026-10-03 21:23:53','商品、活动、积分、分销和渠道运营'),(30,'核销员','verifier',30,'4',1,1,'0','0','admin','2026-10-03 19:24:23','admin','2026-10-03 21:23:53','门店核销及相关查询');
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
INSERT INTO `sys_role_menu` VALUES (1,3000),(1,3010),(1,3011),(1,3012),(1,3013),(1,3014),(1,3020),(1,3021),(1,3022),(1,3023),(1,3024),(1,3030),(1,3031),(1,3032),(1,3033),(1,3034),(1,3040),(1,3041),(1,3042),(1,3043),(1,3044),(1,3050),(1,3051),(1,3052),(1,3060),(1,3061),(1,3062),(1,3070),(1,3071),(1,3072),(1,3080),(1,3081),(1,3090),(1,3091),(1,3092),(1,3100),(1,3101),(1,3102),(1,3103),(1,3104),(1,3110),(1,3111),(1,3112),(1,3113),(1,3114),(1,3115),(1,3116),(1,3117),(1,3118),(1,3119),(1,3120),(1,3121),(1,3122),(1,3123),(1,3124),(1,3125),(1,3126),(1,3127),(1,3128),(1,3129),(1,3130),(10,3000),(10,3050),(10,3051),(10,3060),(10,3061),(10,3070),(10,3071),(10,3080),(10,3081),(10,3110),(10,3111),(10,3113),(10,3119),(10,3121),(10,3125),(10,3126),(10,3129),(10,3130),(20,3000),(20,3010),(20,3011),(20,3012),(20,3013),(20,3014),(20,3020),(20,3021),(20,3022),(20,3023),(20,3024),(20,3030),(20,3031),(20,3032),(20,3033),(20,3034),(20,3040),(20,3041),(20,3042),(20,3043),(20,3044),(20,3050),(20,3051),(20,3052),(20,3060),(20,3061),(20,3062),(20,3070),(20,3071),(20,3072),(20,3080),(20,3081),(20,3090),(20,3091),(20,3092),(20,3100),(20,3101),(20,3102),(20,3103),(20,3104),(20,3110),(20,3111),(20,3112),(20,3113),(20,3114),(20,3115),(20,3116),(20,3117),(20,3118),(20,3119),(20,3120),(20,3121),(20,3122),(20,3123),(20,3124),(20,3125),(20,3127),(20,3128),(20,3129),(30,3000),(30,3050),(30,3051),(30,3060),(30,3061),(30,3070),(30,3071),(30,3080),(30,3081);
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
) ENGINE=InnoDB AUTO_INCREMENT=111 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户信息表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_user`
--

LOCK TABLES `sys_user` WRITE;
/*!40000 ALTER TABLE `sys_user` DISABLE KEYS */;
INSERT INTO `sys_user` VALUES (1,101,'admin','管理员','00','','','1','','$2a$10$faroG3ddhhsQwAf.kDHDgOvnuYyMRdjDa2c3vD7vZTy0Rg7pRN0qS','0','0','127.0.0.1','2026-10-03 23:37:57','2026-10-03 18:54:13','admin','2026-10-02 23:48:30','admin','2026-10-03 18:54:13','超级管理员');
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
INSERT INTO `sys_user_role` VALUES (1,1);
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

-- Dump completed on 2026-10-03 23:43:47
