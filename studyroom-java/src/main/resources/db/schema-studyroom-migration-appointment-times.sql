ALTER TABLE bas_appointment
  ADD COLUMN check_in_at DATETIME(3) NULL COMMENT '签到时间' AFTER version,
  ADD COLUMN study_end_at DATETIME(3) NULL COMMENT '结束学习（完成）时间' AFTER check_in_at;
