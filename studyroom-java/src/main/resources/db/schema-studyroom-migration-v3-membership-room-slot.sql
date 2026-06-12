-- v3：自习室默认时段粒度 + 会员卡类型表 + 菜单（在库 study-room 执行）
USE `study-room`;

ALTER TABLE bas_study_room ADD COLUMN slot_step_minutes INT NOT NULL DEFAULT 30 COMMENT '时段粒度(分钟) 30/60，须与 study.reservation.slot-minutes 一致' AFTER seat_cols;

CREATE TABLE IF NOT EXISTS bas_membership_card (
  id BIGINT NOT NULL AUTO_INCREMENT,
  card_kind VARCHAR(20) NOT NULL DEFAULT 'OTHER' COMMENT 'MONTH/QUARTER/YEAR/OTHER',
  card_name VARCHAR(100) NOT NULL COMMENT '展示名称，如月卡',
  price_yuan DECIMAL(10,2) NOT NULL DEFAULT 0.00,
  validity_days INT NOT NULL DEFAULT 30 COMMENT '有效天数',
  benefit_desc VARCHAR(500) NULL COMMENT '权益说明',
  on_shelf TINYINT NOT NULL DEFAULT 1 COMMENT '1上架0下架',
  sort_order INT NOT NULL DEFAULT 0,
  create_time DATETIME(3) NULL,
  update_time DATETIME(3) NULL,
  PRIMARY KEY (id),
  KEY idx_bas_membership_card_shelf (on_shelf),
  KEY idx_bas_membership_card_sort (sort_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO sys_menu (menu_id, parent_id, name, url, perms, type, icon, order_num) VALUES
(105, 100, '会员卡', 'bas/membership-card', 'bas:membership:list', 1, 'log', 5)
ON DUPLICATE KEY UPDATE
  name = VALUES(name), url = VALUES(url), perms = VALUES(perms), order_num = VALUES(order_num);

INSERT INTO sys_role_menu (role_id, menu_id) VALUES (1, 105)
ON DUPLICATE KEY UPDATE role_id = VALUES(role_id);
