-- 与 application.yml study.reservation.slot-minutes: 10 及小程序选座栅格对齐
UPDATE bas_study_room SET slot_step_minutes = 10 WHERE slot_step_minutes IS NULL OR slot_step_minutes <> 10;
