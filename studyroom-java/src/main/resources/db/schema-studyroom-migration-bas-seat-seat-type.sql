-- 若接口 getSeatByRoom 等返回 500，且 IDEA 日志为 Unknown column 'seat_type' in 'field list'，在业务库执行本段（列已存在会报错，可忽略）。
-- 完整增强脚本见 schema-studyroom-migration-admin-platform.sql

ALTER TABLE bas_seat
  ADD COLUMN seat_type TINYINT NOT NULL DEFAULT 0 COMMENT '0单人座 1双人座 2包厢' AFTER grid_col;
