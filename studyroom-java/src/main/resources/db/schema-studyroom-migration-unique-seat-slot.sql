-- 防超售：为预约表添加唯一约束，确保同一座位同一时段只能有一个有效预约
-- 有效预约状态：0(待签到)、1(已签到使用中)
-- 注意：此索引仅约束有效状态，已取消(2)、已完成(3)、爽约(4)的记录不参与唯一性检查

USE `study-room`;

-- 方案1：使用部分索引（MySQL 8.0+ 支持函数索引）
-- 如果 MySQL 版本不支持，使用方案2的存储列方案

-- 方案2：添加存储列 + 唯一索引（兼容性更好）
ALTER TABLE bas_appointment
  ADD COLUMN seat_slot_active TINYINT GENERATED ALWAYS AS (
    CASE WHEN seat_state IN (0, 1) THEN 1 ELSE NULL END
  ) STORED,
  ADD UNIQUE KEY uk_seat_slot_active (seat_id, biz_date, slot_start, slot_end, seat_slot_active);

-- 说明：
-- 1. 当 seat_state 为 0 或 1 时，seat_slot_active = 1，参与唯一性检查
-- 2. 当 seat_state 为 2、3、4 时，seat_slot_active = NULL，不参与唯一性检查
-- 3. 这样已取消/已完成/爽约的记录不会影响新的预约
