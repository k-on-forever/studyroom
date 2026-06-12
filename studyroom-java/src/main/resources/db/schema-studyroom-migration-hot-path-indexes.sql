-- 热路径索引：小程序鉴权 tb_token、预约列表/统计 bas_appointment、座位按房间查询 bas_seat
-- 已有库执行本文件；索引已存在时会报错，可跳过对应语句。
USE `study-room`;

ALTER TABLE tb_token
  ADD INDEX idx_tb_token_token (token(191));

ALTER TABLE bas_appointment
  ADD INDEX idx_bas_appointment_user (user_id),
  ADD INDEX idx_bas_appointment_biz_state (biz_date, seat_state),
  ADD INDEX idx_bas_appointment_seat_biz (seat_id, biz_date),
  ADD INDEX idx_bas_appointment_create_time (create_time);

ALTER TABLE bas_seat
  ADD INDEX idx_bas_seat_room (room_id);

ALTER TABLE bas_study_room
  ADD INDEX idx_bas_study_room_floor (floor_id);
