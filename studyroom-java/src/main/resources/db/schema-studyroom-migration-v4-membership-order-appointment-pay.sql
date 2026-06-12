-- v4：会员购买订单 + 预约计价字段（在库 study-room 执行；ALTER 若列已存在可忽略该条报错）
USE `study-room`;

CREATE TABLE IF NOT EXISTS bas_membership_order (
  id BIGINT NOT NULL AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  card_id BIGINT NOT NULL,
  order_no VARCHAR(32) NOT NULL,
  card_name VARCHAR(100) NULL,
  amount_yuan DECIMAL(10,2) NOT NULL DEFAULT 0,
  pay_status TINYINT NOT NULL DEFAULT 0 COMMENT '0待支付 1已支付',
  valid_from DATETIME NULL,
  valid_to DATETIME NULL,
  create_time DATETIME(3) NULL,
  pay_time DATETIME(3) NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_bas_membership_order_no (order_no),
  KEY idx_bas_membership_order_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

ALTER TABLE bas_appointment ADD COLUMN pay_amount DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '计价金额(模拟)';
ALTER TABLE bas_appointment ADD COLUMN pay_status TINYINT NOT NULL DEFAULT 0 COMMENT '0会员免费 1按次已付(模拟)';
