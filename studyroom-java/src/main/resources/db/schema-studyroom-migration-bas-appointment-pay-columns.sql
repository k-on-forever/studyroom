-- 管理端「营收 / 仪表盘」、AdminStatsDao.sumAppointmentPayOn 依赖下列列。
-- 报错 Unknown column 'pay_amount' in 'field list' 时在业务库执行（列已存在会报错，可忽略）。

ALTER TABLE bas_appointment
  ADD COLUMN pay_amount DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '计价金额(模拟)';
ALTER TABLE bas_appointment
  ADD COLUMN pay_status TINYINT NOT NULL DEFAULT 0 COMMENT '0会员免费 1按次已付(模拟)';
