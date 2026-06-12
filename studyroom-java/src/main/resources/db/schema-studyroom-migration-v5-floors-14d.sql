-- 已有库升级：多楼层示例 + 预约最远 14 天（与小程序、TimeSlotCodec 一致）
USE `study-room`;

UPDATE sys_reservation_rule
SET advance_booking_days = 14, update_time = NOW(3)
WHERE id = 1;

INSERT INTO bas_floor (id, floor_name) VALUES
(2, '2F 自习区'),
(3, '3F 自习区')
ON DUPLICATE KEY UPDATE floor_name = VALUES(floor_name);

UPDATE bas_floor SET floor_name = '1F 自习区' WHERE id = 1 AND floor_name LIKE '%示例%';

INSERT INTO bas_study_room (id, floor_id, room_name, room_location, opening_time, close_time, seat_rows, seat_cols, slot_step_minutes) VALUES
(2, 2, 'B 自习室', '二楼东翼', '08:00', '22:30', 1, 4, 60),
(3, 3, 'C 自习室', '三楼南侧', '08:00', '22:30', 1, 4, 60)
ON DUPLICATE KEY UPDATE
  floor_id = VALUES(floor_id),
  room_name = VALUES(room_name),
  room_location = VALUES(room_location),
  opening_time = VALUES(opening_time),
  close_time = VALUES(close_time),
  seat_rows = VALUES(seat_rows),
  seat_cols = VALUES(seat_cols),
  slot_step_minutes = VALUES(slot_step_minutes);

INSERT INTO bas_seat (id, room_id, seat_name, grid_row, grid_col, locked) VALUES
(3, 2, 'B-01', 1, 1, 0),
(4, 2, 'B-02', 1, 2, 0),
(5, 2, 'B-03', 1, 3, 0),
(6, 2, 'B-04', 1, 4, 0),
(7, 3, 'C-01', 1, 1, 0),
(8, 3, 'C-02', 1, 2, 0),
(9, 3, 'C-03', 1, 3, 0),
(10, 3, 'C-04', 1, 4, 0)
ON DUPLICATE KEY UPDATE
  room_id = VALUES(room_id),
  seat_name = VALUES(seat_name),
  grid_row = VALUES(grid_row),
  grid_col = VALUES(grid_col),
  locked = VALUES(locked);
