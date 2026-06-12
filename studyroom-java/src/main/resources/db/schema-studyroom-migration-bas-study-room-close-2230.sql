-- 将自习室展示用「营业结束」与全局预约规则（study.reservation.day-end 22:30）对齐。
-- 已有库若仍为 22:00，执行本脚本一次即可（可按需修改 WHERE 条件）。

UPDATE bas_study_room
SET close_time = '22:30'
WHERE close_time IN ('22:00', '22:00:00');
