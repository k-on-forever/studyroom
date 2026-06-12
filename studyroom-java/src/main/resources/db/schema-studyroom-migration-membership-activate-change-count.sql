-- 会员卡：记录顾客修改激活日期的次数（每张期限卡最多 3 次修改，不含首次激活）
ALTER TABLE bas_membership_order
  ADD COLUMN activate_change_count INT NOT NULL DEFAULT 0 COMMENT '激活日期已修改次数' AFTER valid_to;
