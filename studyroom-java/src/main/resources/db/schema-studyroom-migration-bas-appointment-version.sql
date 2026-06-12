-- 预约记录乐观锁（与 BasAppointmentEntity @Version 对应）
ALTER TABLE bas_appointment ADD COLUMN version INT NOT NULL DEFAULT 0 COMMENT '乐观锁';
