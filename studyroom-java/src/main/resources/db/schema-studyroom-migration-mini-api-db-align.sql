-- =============================================================================
-- 小程序会员卡 / 我的会员 / 预约 等接口报 HTTP 500 时，多为「表已有但列不齐」或缺表。
-- 在业务库（如 study-room）执行；若某列已存在会报 Duplicate column，可跳过该条。
-- 建议：在 IDEA Database 控制台逐段执行，或整文件执行后根据报错补跑未成功的语句。
-- 与当前实体对齐的完整建表见：schema-studyroom-core.sql
-- =============================================================================

-- ----- bas_membership_card：/applet/membership/cards 会 SELECT 全部列 -----
-- 缺 benefit_mode / included_hours 时 cards 接口易 500（分两条便于已执行过其中一条时补另一条）
ALTER TABLE bas_membership_card
  ADD COLUMN benefit_mode TINYINT NOT NULL DEFAULT 0 COMMENT '0期限畅约 1学时包' AFTER card_kind;
ALTER TABLE bas_membership_card
  ADD COLUMN included_hours DECIMAL(10,2) NULL COMMENT '学时包含小时数' AFTER validity_days;

-- ----- bas_membership_order：/applet/membership/mine 会查订单列 -----
-- 缺 coupon_id / discount_yuan 时 mine 接口易 500
ALTER TABLE bas_membership_order
  ADD COLUMN coupon_id BIGINT NULL COMMENT '使用的优惠券' AFTER card_id;
ALTER TABLE bas_membership_order
  ADD COLUMN discount_yuan DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '优惠金额' AFTER amount_yuan;

-- ----- bas_seat：选座、getSeatByRoom -----
ALTER TABLE bas_seat
  ADD COLUMN seat_type TINYINT NOT NULL DEFAULT 0 COMMENT '0单人座 1双人座 2包厢' AFTER grid_col;

-- ----- mine 会查学时钱包；缺表则 getBalanceHours 读库失败 -----
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

-- ----- bas_appointment：预约落库与查询（与 BasAppointmentEntity 一致；不写 AFTER 以兼容旧表列顺序）-----
ALTER TABLE bas_appointment ADD COLUMN biz_date VARCHAR(20) NULL COMMENT 'yyyy-MM-dd';
ALTER TABLE bas_appointment ADD COLUMN slot_start INT NULL COMMENT '槽位下标起';
ALTER TABLE bas_appointment ADD COLUMN slot_end INT NULL COMMENT '槽位下标止';
ALTER TABLE bas_appointment ADD COLUMN pay_amount DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '计价金额(模拟)';
ALTER TABLE bas_appointment ADD COLUMN pay_status TINYINT NOT NULL DEFAULT 0 COMMENT '0会员免费 1按次已付(模拟)';
ALTER TABLE bas_appointment ADD COLUMN version INT NOT NULL DEFAULT 0 COMMENT '乐观锁';
ALTER TABLE bas_appointment ADD COLUMN check_in_at DATETIME(3) NULL COMMENT '签到时间';
ALTER TABLE bas_appointment ADD COLUMN study_end_at DATETIME(3) NULL COMMENT '结束学习时间';

-- ----- bas_study_room：时段粒度（与预约编码一致）-----
ALTER TABLE bas_study_room ADD COLUMN slot_step_minutes INT NOT NULL DEFAULT 30 COMMENT '时段粒度分钟 30/60';
