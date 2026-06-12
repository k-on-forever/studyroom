-- 自习室核心表（MySQL 8）。库名与 application-dev.yml 中 datasource.url 一致。
-- 执行前请先: CREATE DATABASE IF NOT EXISTS `study-room` DEFAULT CHARSET utf8mb4;

USE `study-room`;

CREATE TABLE IF NOT EXISTS tb_user (
  user_id BIGINT NOT NULL COMMENT '主键',
  username VARCHAR(100) NULL,
  account VARCHAR(100) NULL,
  mobile VARCHAR(50) NULL,
  password VARCHAR(200) NULL,
  status INT DEFAULT 1 COMMENT '0封禁1正常',
  name VARCHAR(100) NULL,
  qq BIGINT NULL,
  email VARCHAR(100) NULL,
  bz VARCHAR(500) NULL,
  create_time DATETIME NULL,
  user_img VARCHAR(500) NULL,
  open_id VARCHAR(100) NULL,
  PRIMARY KEY (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS tb_token (
  user_id BIGINT NOT NULL,
  token VARCHAR(500) NOT NULL,
  expire_time DATETIME NULL,
  update_time DATETIME NULL,
  PRIMARY KEY (user_id),
  KEY idx_tb_token_token (token(191))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS bas_floor (
  id BIGINT NOT NULL AUTO_INCREMENT,
  floor_name VARCHAR(100) NULL,
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS bas_study_room (
  id BIGINT NOT NULL AUTO_INCREMENT,
  floor_id BIGINT NULL,
  room_name VARCHAR(200) NULL,
  room_location VARCHAR(500) NULL COMMENT '位置/区域描述',
  opening_time VARCHAR(20) NULL,
  close_time VARCHAR(20) NULL,
  seat_rows INT NOT NULL DEFAULT 0 COMMENT '座位行数（与批量布局一致，可 0 表示未生成）',
  seat_cols INT NOT NULL DEFAULT 0 COMMENT '座位列数',
  slot_step_minutes INT NOT NULL DEFAULT 60 COMMENT '时段粒度(分钟) 30/60，须与 study.reservation.slot-minutes 一致',
  PRIMARY KEY (id),
  KEY idx_bas_study_room_floor (floor_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS bas_seat (
  id BIGINT NOT NULL AUTO_INCREMENT,
  room_id BIGINT NULL,
  seat_name VARCHAR(100) NULL,
  grid_row INT NULL DEFAULT 0,
  grid_col INT NULL DEFAULT 0,
  seat_type TINYINT NOT NULL DEFAULT 0 COMMENT '0单人 1双人 2包厢',
  locked TINYINT NOT NULL DEFAULT 0 COMMENT '0正常1锁座',
  PRIMARY KEY (id),
  KEY idx_bas_seat_room (room_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS bas_notice (
  id BIGINT NOT NULL AUTO_INCREMENT,
  title VARCHAR(200) NULL,
  content TEXT NULL,
  create_time DATETIME NULL,
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS bas_message (
  id BIGINT NOT NULL AUTO_INCREMENT,
  user_id BIGINT NULL,
  username VARCHAR(100) NULL,
  message VARCHAR(2000) NULL,
  message_type INT NULL,
  create_time DATETIME NULL,
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS bas_membership_card (
  id BIGINT NOT NULL AUTO_INCREMENT,
  card_kind VARCHAR(20) NOT NULL DEFAULT 'OTHER' COMMENT 'MONTH/QUARTER/YEAR/HOUR_4等',
  benefit_mode TINYINT NOT NULL DEFAULT 0 COMMENT '0期限畅约 1学时包',
  card_name VARCHAR(100) NOT NULL COMMENT '展示名称',
  price_yuan DECIMAL(10,2) NOT NULL DEFAULT 0.00,
  validity_days INT NOT NULL DEFAULT 30 COMMENT '有效天数(期限卡)或参考天数',
  included_hours DECIMAL(10,2) NULL COMMENT '学时包包含小时数',
  benefit_desc VARCHAR(500) NULL COMMENT '权益说明',
  on_shelf TINYINT NOT NULL DEFAULT 1 COMMENT '1上架0下架',
  sort_order INT NOT NULL DEFAULT 0,
  create_time DATETIME(3) NULL,
  update_time DATETIME(3) NULL,
  PRIMARY KEY (id),
  KEY idx_bas_membership_card_shelf (on_shelf),
  KEY idx_bas_membership_card_sort (sort_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS bas_membership_order (
  id BIGINT NOT NULL AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  card_id BIGINT NOT NULL,
  coupon_id BIGINT NULL COMMENT '拟使用的优惠券',
  order_no VARCHAR(32) NOT NULL,
  card_name VARCHAR(100) NULL,
  amount_yuan DECIMAL(10,2) NOT NULL DEFAULT 0,
  discount_yuan DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '优惠金额(支付时核销)',
  pay_status TINYINT NOT NULL DEFAULT 0 COMMENT '0待支付 1已支付',
  valid_from DATETIME NULL,
  valid_to DATETIME NULL,
  activate_change_count INT NOT NULL DEFAULT 0 COMMENT '激活日期已修改次数(最多3次,不含首次激活)',
  create_time DATETIME(3) NULL,
  pay_time DATETIME(3) NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_bas_membership_order_no (order_no),
  KEY idx_bas_membership_order_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS bas_user_hour_wallet (
  user_id BIGINT NOT NULL,
  balance_hours DECIMAL(12,2) NOT NULL DEFAULT 0.00,
  update_time DATETIME(3) NULL,
  PRIMARY KEY (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS bas_user_coupon (
  id BIGINT NOT NULL AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  title VARCHAR(100) NOT NULL DEFAULT '优惠券',
  balance_yuan DECIMAL(10,2) NOT NULL DEFAULT 0.00,
  status TINYINT NOT NULL DEFAULT 0 COMMENT '0有效 1用尽 2作废',
  expire_time DATETIME(3) NULL,
  create_time DATETIME(3) NULL,
  PRIMARY KEY (id),
  KEY idx_bas_user_coupon_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS bas_appointment (
  id BIGINT NOT NULL AUTO_INCREMENT,
  seat_id BIGINT NULL,
  user_id BIGINT NULL,
  seat_phone VARCHAR(50) NULL,
  seat_name VARCHAR(100) NULL,
  seat_class VARCHAR(200) NULL,
  seat_day VARCHAR(500) NULL,
  biz_date VARCHAR(20) NULL,
  slot_start INT NULL,
  slot_end INT NULL,
  seat_state INT NULL COMMENT '0待签到1使用中2取消3完成4爽约',
  pay_amount DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '计价金额(模拟)',
  pay_status TINYINT NOT NULL DEFAULT 0 COMMENT '0会员免费 1按次已付(模拟)',
  create_time DATETIME NULL,
  version INT NOT NULL DEFAULT 0 COMMENT '乐观锁',
  check_in_at DATETIME(3) NULL COMMENT '签到时间',
  study_end_at DATETIME(3) NULL COMMENT '结束学习（完成）时间',
  PRIMARY KEY (id),
  KEY idx_bas_appointment_user (user_id),
  KEY idx_bas_appointment_biz_state (biz_date, seat_state),
  KEY idx_bas_appointment_seat_biz (seat_id, biz_date),
  KEY idx_bas_appointment_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 全局限定预约：提前预约天数、单次最长时间、可取消的提前量（与 Spring 预约校验同步）
CREATE TABLE IF NOT EXISTS sys_reservation_rule (
  id TINYINT NOT NULL DEFAULT 1,
  advance_booking_days INT NOT NULL DEFAULT 14,
  max_duration_minutes INT NOT NULL DEFAULT 870,
  cancel_advance_minutes INT NOT NULL DEFAULT 30,
  update_time DATETIME(3) NULL,
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS sys_log (
  id BIGINT NOT NULL AUTO_INCREMENT,
  username VARCHAR(100) NULL,
  operation VARCHAR(500) NULL,
  method VARCHAR(500) NULL,
  params TEXT NULL,
  ip VARCHAR(64) NULL,
  time BIGINT NULL,
  create_date DATETIME NULL,
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 管理后台账号在 sys_admin（用户名 admin / 密码 admin123，password 为 SHA256 十六进制小写）；小程序用户用 tb_user，勿混用。

CREATE TABLE IF NOT EXISTS sys_admin (
  admin_id BIGINT NOT NULL AUTO_INCREMENT,
  username VARCHAR(100) NOT NULL,
  password VARCHAR(200) NOT NULL,
  name VARCHAR(100) NULL,
  status INT DEFAULT 1 COMMENT '0禁用1正常',
  create_time DATETIME(3) NULL,
  PRIMARY KEY (admin_id),
  UNIQUE KEY uk_sys_admin_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS sys_admin_token (
  admin_id BIGINT NOT NULL,
  token VARCHAR(500) NOT NULL,
  expire_time DATETIME NULL,
  update_time DATETIME NULL,
  PRIMARY KEY (admin_id),
  KEY idx_sys_admin_token_token (token(191))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS sys_role (
  role_id BIGINT NOT NULL AUTO_INCREMENT,
  role_name VARCHAR(100) NOT NULL,
  remark VARCHAR(500) NULL,
  PRIMARY KEY (role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS sys_menu (
  menu_id BIGINT NOT NULL AUTO_INCREMENT,
  parent_id BIGINT NOT NULL DEFAULT 0,
  name VARCHAR(100) NOT NULL,
  url VARCHAR(200) NULL,
  perms VARCHAR(200) NULL,
  type INT NOT NULL DEFAULT 0 COMMENT '0目录1菜单',
  icon VARCHAR(100) NULL,
  order_num INT NOT NULL DEFAULT 0,
  PRIMARY KEY (menu_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS sys_admin_role (
  id BIGINT NOT NULL AUTO_INCREMENT,
  admin_id BIGINT NOT NULL,
  role_id BIGINT NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_sys_admin_role (admin_id, role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS sys_role_menu (
  id BIGINT NOT NULL AUTO_INCREMENT,
  role_id BIGINT NOT NULL,
  menu_id BIGINT NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_sys_role_menu (role_id, menu_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO sys_role (role_id, role_name, remark) VALUES (1, '超级管理员', '系统内置')
ON DUPLICATE KEY UPDATE role_name = VALUES(role_name), remark = VALUES(remark);

INSERT INTO sys_admin (admin_id, username, password, name, status, create_time) VALUES (
  1,
  'admin',
  '240be518fabd2724ddb6f04eeb1da5967448d7e831c08c8fa822809f74c720a9',
  '超级管理员',
  1,
  NOW(3)
)
ON DUPLICATE KEY UPDATE
  password = VALUES(password),
  name = VALUES(name),
  status = VALUES(status);

INSERT INTO sys_admin_role (admin_id, role_id) VALUES (1, 1)
ON DUPLICATE KEY UPDATE admin_id = VALUES(admin_id);

INSERT INTO sys_menu (menu_id, parent_id, name, url, perms, type, icon, order_num) VALUES
(100, 0, '基础数据', NULL, NULL, 0, 'menu', 1),
(101, 100, '楼层管理', 'bas/floor', 'bas:floor:list', 1, 'log', 1),
(102, 100, '自习室', 'bas/studyroom', 'bas:studyroom:list', 1, 'log', 2),
(103, 100, '座位管理', 'bas/seat', 'bas:seat:list', 1, 'log', 3),
(104, 100, '预约规则', 'bas/reservation-rule', 'bas:reservation:rule', 1, 'log', 4),
(105, 100, '会员卡', 'bas/membership-card', 'bas:membership:list', 1, 'log', 5),
(200, 0, '运营管理', NULL, NULL, 0, 'menu', 2),
(201, 200, '预约管理', 'bas/appointment', 'bas:appointment:list', 1, 'log', 1),
(202, 200, '公告管理', 'bas/notice', 'bas:notice:list', 1, 'log', 2),
(203, 200, '留言管理', 'bas/message', 'bas:message:list', 1, 'log', 3),
(204, 200, '小程序用户', 'bas/mini-user', 'bas:miniuser:list', 1, 'log', 4)
ON DUPLICATE KEY UPDATE
  parent_id = VALUES(parent_id),
  name = VALUES(name),
  url = VALUES(url),
  perms = VALUES(perms),
  type = VALUES(type),
  icon = VALUES(icon),
  order_num = VALUES(order_num);

INSERT INTO sys_reservation_rule (id, advance_booking_days, max_duration_minutes, cancel_advance_minutes, update_time) VALUES
(1, 14, 870, 30, NOW(3))
ON DUPLICATE KEY UPDATE
  advance_booking_days = VALUES(advance_booking_days),
  max_duration_minutes = VALUES(max_duration_minutes),
  cancel_advance_minutes = VALUES(cancel_advance_minutes),
  update_time = VALUES(update_time);

INSERT INTO sys_role_menu (role_id, menu_id) VALUES
(1, 100),
(1, 101),
(1, 102),
(1, 103),
(1, 104),
(1, 105),
(1, 200),
(1, 201),
(1, 202),
(1, 203),
(1, 204)
ON DUPLICATE KEY UPDATE role_id = VALUES(role_id);

-- 基础数据示例（管理端楼层/自习室/座位；可按需在界面改，不必强依赖本段）
INSERT INTO bas_floor (id, floor_name) VALUES
(1, '1F 自习区'),
(2, '2F 自习区'),
(3, '3F 自习区')
ON DUPLICATE KEY UPDATE floor_name = VALUES(floor_name);

INSERT INTO bas_study_room (id, floor_id, room_name, room_location, opening_time, close_time, seat_rows, seat_cols, slot_step_minutes) VALUES
(1, 1, 'A 自习室', '东走廊北侧', '08:00', '22:30', 4, 5, 60),
(2, 2, 'B 自习室', '二楼东翼', '08:00', '22:30', 4, 5, 60),
(3, 3, 'C 自习室', '三楼南侧', '08:00', '22:30', 4, 5, 60)
ON DUPLICATE KEY UPDATE
  floor_id = VALUES(floor_id),
  room_name = VALUES(room_name),
  room_location = VALUES(room_location),
  opening_time = VALUES(opening_time),
  close_time = VALUES(close_time),
  seat_rows = VALUES(seat_rows),
  seat_cols = VALUES(seat_cols),
  slot_step_minutes = VALUES(slot_step_minutes);

INSERT INTO bas_membership_card (id, card_kind, benefit_mode, card_name, price_yuan, validity_days, included_hours, benefit_desc, on_shelf, sort_order, create_time) VALUES
(1, 'MONTH', 0, '月卡', 300.00, 30, NULL, '有效期内不限次数预约免单', 1, 1, NOW(3)),
(2, 'QUARTER', 0, '季卡', 869.00, 90, NULL, '有效期内不限次数预约免单', 1, 2, NOW(3)),
(3, 'YEAR', 0, '年卡', 899.00, 365, NULL, '有效期内不限次数预约免单', 1, 3, NOW(3)),
(4, 'HOUR_4', 1, '4小时学习包', 13.90, 365, 4.00, '购买后增加可预约学时', 0, 10, NOW(3)),
(5, 'HOUR_50', 1, '50小时灵活卡', 173.00, 730, 50.00, '购买后增加可预约学时', 0, 11, NOW(3)),
(6, 'HOUR_1', 1, '1小时体验包', 6.00, 180, 1.00, '购买后增加可预约学时', 0, 12, NOW(3))
ON DUPLICATE KEY UPDATE
  benefit_mode = VALUES(benefit_mode),
  card_name = VALUES(card_name),
  price_yuan = VALUES(price_yuan),
  validity_days = VALUES(validity_days),
  included_hours = VALUES(included_hours),
  benefit_desc = VALUES(benefit_desc);

-- 与 schema-studyroom-seed-three-floors.sql 一致：每间 4×5=20 座
INSERT INTO bas_seat (id, room_id, seat_name, grid_row, grid_col, seat_type, locked) VALUES
(1, 1, 'A-01', 1, 1, 0, 0),
(2, 1, 'A-02', 1, 2, 0, 0),
(11, 1, 'A-03', 1, 3, 0, 0),
(12, 1, 'A-04', 1, 4, 0, 0),
(13, 1, 'A-05', 1, 5, 0, 0),
(14, 1, 'A-06', 2, 1, 0, 0),
(15, 1, 'A-07', 2, 2, 0, 0),
(16, 1, 'A-08', 2, 3, 0, 0),
(17, 1, 'A-09', 2, 4, 0, 0),
(18, 1, 'A-10', 2, 5, 0, 0),
(19, 1, 'A-11', 3, 1, 0, 0),
(20, 1, 'A-12', 3, 2, 0, 0),
(21, 1, 'A-13', 3, 3, 0, 0),
(22, 1, 'A-14', 3, 4, 0, 0),
(23, 1, 'A-15', 3, 5, 0, 0),
(24, 1, 'A-16', 4, 1, 0, 0),
(25, 1, 'A-17', 4, 2, 0, 0),
(26, 1, 'A-18', 4, 3, 0, 0),
(27, 1, 'A-19', 4, 4, 0, 0),
(28, 1, 'A-20', 4, 5, 0, 0),
(3, 2, 'B-01', 1, 1, 0, 0),
(4, 2, 'B-02', 1, 2, 0, 0),
(5, 2, 'B-03', 1, 3, 0, 0),
(6, 2, 'B-04', 1, 4, 0, 0),
(29, 2, 'B-05', 1, 5, 0, 0),
(30, 2, 'B-06', 2, 1, 0, 0),
(31, 2, 'B-07', 2, 2, 0, 0),
(32, 2, 'B-08', 2, 3, 0, 0),
(33, 2, 'B-09', 2, 4, 0, 0),
(34, 2, 'B-10', 2, 5, 0, 0),
(35, 2, 'B-11', 3, 1, 0, 0),
(36, 2, 'B-12', 3, 2, 0, 0),
(37, 2, 'B-13', 3, 3, 0, 0),
(38, 2, 'B-14', 3, 4, 0, 0),
(39, 2, 'B-15', 3, 5, 0, 0),
(40, 2, 'B-16', 4, 1, 0, 0),
(41, 2, 'B-17', 4, 2, 0, 0),
(42, 2, 'B-18', 4, 3, 0, 0),
(43, 2, 'B-19', 4, 4, 0, 0),
(44, 2, 'B-20', 4, 5, 0, 0),
(7, 3, 'C-01', 1, 1, 0, 0),
(8, 3, 'C-02', 1, 2, 0, 0),
(9, 3, 'C-03', 1, 3, 0, 0),
(10, 3, 'C-04', 1, 4, 0, 0),
(45, 3, 'C-05', 1, 5, 0, 0),
(46, 3, 'C-06', 2, 1, 0, 0),
(47, 3, 'C-07', 2, 2, 0, 0),
(48, 3, 'C-08', 2, 3, 0, 0),
(49, 3, 'C-09', 2, 4, 0, 0),
(50, 3, 'C-10', 2, 5, 0, 0),
(51, 3, 'C-11', 3, 1, 0, 0),
(52, 3, 'C-12', 3, 2, 0, 0),
(53, 3, 'C-13', 3, 3, 0, 0),
(54, 3, 'C-14', 3, 4, 0, 0),
(55, 3, 'C-15', 3, 5, 0, 0),
(56, 3, 'C-16', 4, 1, 0, 0),
(57, 3, 'C-17', 4, 2, 0, 0),
(58, 3, 'C-18', 4, 3, 0, 0),
(59, 3, 'C-19', 4, 4, 0, 0),
(60, 3, 'C-20', 4, 5, 0, 0)
ON DUPLICATE KEY UPDATE
  room_id = VALUES(room_id),
  seat_name = VALUES(seat_name),
  grid_row = VALUES(grid_row),
  grid_col = VALUES(grid_col),
  seat_type = VALUES(seat_type),
  locked = VALUES(locked);
