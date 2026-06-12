-- 从旧版表结构升级：在库 study-room 中执行（列已存在会报错，可逐条执行并忽略重复列错误）
USE `study-room`;

ALTER TABLE bas_study_room ADD COLUMN room_location VARCHAR(500) NULL COMMENT '位置' AFTER room_name;
ALTER TABLE bas_study_room ADD COLUMN seat_rows INT NOT NULL DEFAULT 0 COMMENT '座位行数' AFTER close_time;
ALTER TABLE bas_study_room ADD COLUMN seat_cols INT NOT NULL DEFAULT 0 COMMENT '座位列数' AFTER seat_rows;

ALTER TABLE bas_seat ADD COLUMN grid_row INT NULL DEFAULT 0 AFTER seat_name;
ALTER TABLE bas_seat ADD COLUMN grid_col INT NULL DEFAULT 0 AFTER grid_row;
ALTER TABLE bas_seat ADD COLUMN locked TINYINT NOT NULL DEFAULT 0 COMMENT '0正常1锁座' AFTER grid_col;

CREATE TABLE IF NOT EXISTS sys_reservation_rule (
  id TINYINT NOT NULL DEFAULT 1,
  advance_booking_days INT NOT NULL DEFAULT 7 COMMENT '可预约未来天数',
  max_duration_minutes INT NOT NULL DEFAULT 870 COMMENT '单次最大时长(分钟)，与营业窗 08:00-22:30 对齐',
  cancel_advance_minutes INT NOT NULL DEFAULT 30 COMMENT '开席前多少分钟内不可取消',
  update_time DATETIME(3) NULL,
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO sys_reservation_rule (id, advance_booking_days, max_duration_minutes, cancel_advance_minutes, update_time)
VALUES (1, 7, 870, 30, NOW(3))
ON DUPLICATE KEY UPDATE id = id;

-- 新菜单：预约规则（需为角色1授权；若已存在会冲突则改 menu_id 或先删后插）
INSERT INTO sys_menu (menu_id, parent_id, name, url, perms, type, icon, order_num) VALUES
(104, 100, '预约规则', 'bas/reservation-rule', 'bas:reservation:rule', 1, 'log', 4)
ON DUPLICATE KEY UPDATE
  name = VALUES(name), url = VALUES(url), perms = VALUES(perms), order_num = VALUES(order_num);

INSERT INTO sys_role_menu (role_id, menu_id) VALUES (1, 104)
ON DUPLICATE KEY UPDATE role_id = VALUES(role_id);
