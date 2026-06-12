-- 管理端「预约规则」、小程序预约时间范围依赖本表。若未执行过完整 core/v2 脚本，可在业务库中单独执行本文件。
-- 在 Navicat 中请先选中与 application-dev.yml 中 datasource 相同的数据库（默认库名 study-room）再执行。

CREATE TABLE IF NOT EXISTS sys_reservation_rule (
  id TINYINT NOT NULL DEFAULT 1,
  advance_booking_days INT NOT NULL DEFAULT 14,
  max_duration_minutes INT NOT NULL DEFAULT 870,
  cancel_advance_minutes INT NOT NULL DEFAULT 30,
  update_time DATETIME(3) NULL,
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO sys_reservation_rule (id, advance_booking_days, max_duration_minutes, cancel_advance_minutes, update_time) VALUES
(1, 14, 870, 30, NOW(3))
ON DUPLICATE KEY UPDATE
  advance_booking_days = VALUES(advance_booking_days),
  max_duration_minutes = VALUES(max_duration_minutes),
  cancel_advance_minutes = VALUES(cancel_advance_minutes),
  update_time = VALUES(update_time);
