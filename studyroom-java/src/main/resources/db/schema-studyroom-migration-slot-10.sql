-- 与 application.yml study.reservation.slot-minutes: 10 一致；TimeSlotCodec 会拒绝「自习室粒度 ≠ 全局」的预约
-- 将仍为 5/60/30 等旧值的行统一改为 10（若只改部分自习室，可改为按 id 更新）
UPDATE bas_study_room
SET slot_step_minutes = 10
WHERE slot_step_minutes IS NULL OR slot_step_minutes <> 10;
