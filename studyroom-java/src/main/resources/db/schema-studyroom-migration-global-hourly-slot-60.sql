-- 将自习室时段粒度改为 60 分钟（与 application.yml study.reservation.slot-minutes: 60 一致）。
-- 注意：若库内已有 bas_appointment 是在「30 分钟槽」下产生的，slot_start/slot_end 与高峰统计含义会与改造后不兼容，
--       测试库可清空预约表或接受统计偏差；生产环境改粒度前须专门迁移历史预约数据。

UPDATE bas_study_room SET slot_step_minutes = 60 WHERE slot_step_minutes = 30;
