-- Public member numbers are independent of internal primary keys.
SET @member_number_column = (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='biz_member' AND column_name='member_no');
SET @member_number_ddl = IF(@member_number_column=0, 'ALTER TABLE biz_member ADD COLUMN member_no varchar(32) DEFAULT NULL AFTER member_id', 'SELECT 1');
PREPARE member_number_statement FROM @member_number_ddl;
EXECUTE member_number_statement;
DEALLOCATE PREPARE member_number_statement;
SET @member_number_index = (SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name='biz_member' AND index_name='uk_biz_member_no');
SET @member_number_ddl = IF(@member_number_index=0, 'ALTER TABLE biz_member ADD UNIQUE KEY uk_biz_member_no (member_no)', 'SELECT 1');
PREPARE member_number_statement FROM @member_number_ddl;
EXECUTE member_number_statement;
DEALLOCATE PREPARE member_number_statement;
UPDATE biz_member SET member_no=CONCAT('HY',HEX(RANDOM_BYTES(10))),update_time=update_time WHERE member_no IS NULL OR member_no='';
