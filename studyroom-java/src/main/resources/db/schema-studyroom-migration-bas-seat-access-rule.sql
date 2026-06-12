-- 座位时段策略：全员不可订 / 仅白名单用户可订（与 bas_seat.locked 全局维修开关并存）
CREATE TABLE IF NOT EXISTS bas_seat_access_rule (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  seat_id BIGINT NOT NULL COMMENT '座位ID',
  date_from DATE NOT NULL COMMENT '生效起始日(含)',
  date_to DATE NOT NULL COMMENT '生效结束日(含)',
  time_from TIME NOT NULL COMMENT '每日时段起(含)',
  time_to TIME NOT NULL COMMENT '每日时段止(不含，须晚于 time_from)',
  lock_mode TINYINT NOT NULL DEFAULT 0 COMMENT '0该时段全员不可订 1仅白名单用户可订',
  whitelist_user_ids VARCHAR(4000) DEFAULT NULL COMMENT '白名单JSON：手机号 ["138..."]；兼容旧数据用户ID [1,2]；lock_mode=1 时必填',
  enabled TINYINT NOT NULL DEFAULT 1,
  remark VARCHAR(255) DEFAULT NULL,
  create_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  update_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  KEY idx_seat (seat_id),
  KEY idx_seat_date (seat_id, date_from, date_to)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='座位时段访问策略(后台锁座/白名单)';
