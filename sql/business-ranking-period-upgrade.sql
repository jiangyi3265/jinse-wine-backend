-- 可重复执行：为旧版排行榜奖励补充统计周期字段和周期级幂等索引。
USE jinse_wine;
SET NAMES utf8mb4;

SET @sql = (SELECT IF(COUNT(*) > 0,
  'ALTER TABLE biz_ranking_reward DROP INDEX uk_biz_ranking_reward_member',
  'SELECT 1') FROM information_schema.statistics WHERE table_schema = DATABASE()
  AND table_name = 'biz_ranking_reward' AND index_name = 'uk_biz_ranking_reward_member');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(COUNT(*) = 0,
  'ALTER TABLE biz_ranking_reward ADD COLUMN period_start datetime NULL AFTER config_id',
  'SELECT 1') FROM information_schema.columns WHERE table_schema = DATABASE()
  AND table_name = 'biz_ranking_reward' AND column_name = 'period_start');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(COUNT(*) = 0,
  'ALTER TABLE biz_ranking_reward ADD COLUMN period_end datetime NULL AFTER period_start',
  'SELECT 1') FROM information_schema.columns WHERE table_schema = DATABASE()
  AND table_name = 'biz_ranking_reward' AND column_name = 'period_end');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

UPDATE biz_ranking_reward
SET period_start='1000-01-01 00:00:00', period_end='1000-01-02 00:00:00'
WHERE period_start IS NULL OR period_end IS NULL;

ALTER TABLE biz_ranking_reward
  MODIFY period_start datetime NOT NULL,
  MODIFY period_end datetime NOT NULL;

SET @sql = (SELECT IF(COUNT(*) = 0,
  'ALTER TABLE biz_ranking_reward ADD UNIQUE KEY uk_biz_ranking_reward_period_member (config_id, period_start, member_id)',
  'SELECT 1') FROM information_schema.statistics WHERE table_schema = DATABASE()
  AND table_name = 'biz_ranking_reward' AND index_name = 'uk_biz_ranking_reward_period_member');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
