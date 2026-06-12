-- 下架学时包（含 1 小时体验包），小程序仅售期限会员卡（benefit_mode=0）
UPDATE bas_membership_card
SET on_shelf = 0, update_time = NOW(3)
WHERE benefit_mode = 1 OR card_kind IN ('HOUR_1', 'HOUR_4', 'HOUR_50');
