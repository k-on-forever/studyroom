-- 白名单列语义：优先存手机号 JSON 数组，如 ["13800138000","13900139000"]；历史数据若为 [1,2] 用户ID 仍可读
ALTER TABLE bas_seat_access_rule
  MODIFY COLUMN whitelist_user_ids VARCHAR(4000) DEFAULT NULL
  COMMENT '白名单JSON：手机号数组 ["138..."]；兼容旧库用户ID数组 [1,2]';
