-- 管理后台增强：学时套餐、学时钱包、优惠券、座位类型、订单优惠字段

ALTER TABLE bas_membership_card
  ADD COLUMN benefit_mode TINYINT NOT NULL DEFAULT 0 COMMENT '0期限畅约 1学时包' AFTER card_kind,
  ADD COLUMN included_hours DECIMAL(10,2) NULL COMMENT '学时包含小时数' AFTER validity_days;

ALTER TABLE bas_membership_order
  ADD COLUMN coupon_id BIGINT NULL COMMENT '使用的优惠券' AFTER card_id,
  ADD COLUMN discount_yuan DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '优惠金额' AFTER amount_yuan;

ALTER TABLE bas_seat
  ADD COLUMN seat_type TINYINT NOT NULL DEFAULT 0 COMMENT '0单人座 1双人座 2包厢' AFTER grid_col;

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

-- 上架学时套餐（已存在同名则跳过）
INSERT INTO bas_membership_card (card_kind, benefit_mode, card_name, price_yuan, validity_days, included_hours, benefit_desc, on_shelf, sort_order, create_time, update_time)
SELECT 'HOUR_4', 1, '4小时学习包', 13.90, 365, 4.00, '购买后增加4小时可预约学时', 1, 10, NOW(3), NOW(3)
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM bas_membership_card c WHERE c.card_name = '4小时学习包');

INSERT INTO bas_membership_card (card_kind, benefit_mode, card_name, price_yuan, validity_days, included_hours, benefit_desc, on_shelf, sort_order, create_time, update_time)
SELECT 'HOUR_50', 1, '50小时灵活卡', 173.00, 730, 50.00, '购买后增加50小时可预约学时', 1, 11, NOW(3), NOW(3)
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM bas_membership_card c WHERE c.card_name = '50小时灵活卡');

INSERT INTO bas_membership_card (card_kind, benefit_mode, card_name, price_yuan, validity_days, included_hours, benefit_desc, on_shelf, sort_order, create_time, update_time)
SELECT 'HOUR_1', 1, '1小时体验包', 6.00, 180, 1.00, '购买后增加1小时可预约学时', 0, 12, NOW(3), NOW(3)
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM bas_membership_card c WHERE c.card_name = '1小时体验包');
