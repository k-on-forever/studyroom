-- 与 BasAppointmentEntity.checkInAt / studyEndAt 对应（MyBatis-Plus 映射为 check_in_at、study_end_at）
-- 若已存在会报 Duplicate column，跳过即可。
ALTER TABLE bas_appointment ADD COLUMN check_in_at DATETIME(3) NULL COMMENT '签到时间';
ALTER TABLE bas_appointment ADD COLUMN study_end_at DATETIME(3) NULL COMMENT '结束学习（完成）时间';
