-- 已有库升级：QQ 号可能超过 INT 上限，与 Java Long 一致
ALTER TABLE tb_user MODIFY COLUMN qq BIGINT NULL;
