-- 微信支付统一下单记录（预约 / 会员卡）
CREATE TABLE IF NOT EXISTS bas_wx_pay_order (
  id BIGINT NOT NULL AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  biz_type VARCHAR(32) NOT NULL COMMENT 'APPOINTMENT / MEMBERSHIP',
  biz_ref VARCHAR(64) NULL COMMENT '会员卡订单 id 等',
  out_trade_no VARCHAR(64) NOT NULL,
  amount_fen INT NOT NULL,
  status TINYINT NOT NULL DEFAULT 0 COMMENT '0待支付 1已支付 2已关闭',
  payload_json MEDIUMTEXT NULL COMMENT '预约参数 JSON 等',
  wx_transaction_id VARCHAR(64) NULL,
  paid_time DATETIME(3) NULL,
  create_time DATETIME(3) NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_bas_wx_pay_out_trade_no (out_trade_no),
  KEY idx_bas_wx_pay_user (user_id),
  KEY idx_bas_wx_pay_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
