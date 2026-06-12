-- 1F/2F/3F 每间自习室扩展为 4×5=20 个座位（保留原 id 1–10，新增 11–60）
-- 在业务库执行（默认 study-room）；可重复执行

USE `study-room`;

UPDATE bas_study_room
SET seat_rows = 4,
    seat_cols = 5,
    slot_step_minutes = 60
WHERE id IN (1, 2, 3);

-- 校正原有 10 个座位在 4×5 网格中的位置
UPDATE bas_seat SET seat_name = 'A-01', grid_row = 1, grid_col = 1, room_id = 1 WHERE id = 1;
UPDATE bas_seat SET seat_name = 'A-02', grid_row = 1, grid_col = 2, room_id = 1 WHERE id = 2;
UPDATE bas_seat SET seat_name = 'B-01', grid_row = 1, grid_col = 1, room_id = 2 WHERE id = 3;
UPDATE bas_seat SET seat_name = 'B-02', grid_row = 1, grid_col = 2, room_id = 2 WHERE id = 4;
UPDATE bas_seat SET seat_name = 'B-03', grid_row = 1, grid_col = 3, room_id = 2 WHERE id = 5;
UPDATE bas_seat SET seat_name = 'B-04', grid_row = 1, grid_col = 4, room_id = 2 WHERE id = 6;
UPDATE bas_seat SET seat_name = 'C-01', grid_row = 1, grid_col = 1, room_id = 3 WHERE id = 7;
UPDATE bas_seat SET seat_name = 'C-02', grid_row = 1, grid_col = 2, room_id = 3 WHERE id = 8;
UPDATE bas_seat SET seat_name = 'C-03', grid_row = 1, grid_col = 3, room_id = 3 WHERE id = 9;
UPDATE bas_seat SET seat_name = 'C-04', grid_row = 1, grid_col = 4, room_id = 3 WHERE id = 10;

-- 1F A 自习室 A-03 … A-20（id 11–28）
INSERT INTO bas_seat (id, room_id, seat_name, grid_row, grid_col, seat_type, locked) VALUES
(11, 1, 'A-03', 1, 3, 0, 0),
(12, 1, 'A-04', 1, 4, 0, 0),
(13, 1, 'A-05', 1, 5, 0, 0),
(14, 1, 'A-06', 2, 1, 0, 0),
(15, 1, 'A-07', 2, 2, 0, 0),
(16, 1, 'A-08', 2, 3, 0, 0),
(17, 1, 'A-09', 2, 4, 0, 0),
(18, 1, 'A-10', 2, 5, 0, 0),
(19, 1, 'A-11', 3, 1, 0, 0),
(20, 1, 'A-12', 3, 2, 0, 0),
(21, 1, 'A-13', 3, 3, 0, 0),
(22, 1, 'A-14', 3, 4, 0, 0),
(23, 1, 'A-15', 3, 5, 0, 0),
(24, 1, 'A-16', 4, 1, 0, 0),
(25, 1, 'A-17', 4, 2, 0, 0),
(26, 1, 'A-18', 4, 3, 0, 0),
(27, 1, 'A-19', 4, 4, 0, 0),
(28, 1, 'A-20', 4, 5, 0, 0)
ON DUPLICATE KEY UPDATE
  room_id = VALUES(room_id),
  seat_name = VALUES(seat_name),
  grid_row = VALUES(grid_row),
  grid_col = VALUES(grid_col),
  seat_type = VALUES(seat_type),
  locked = VALUES(locked);

-- 2F B 自习室 B-05 … B-20（id 29–44）
INSERT INTO bas_seat (id, room_id, seat_name, grid_row, grid_col, seat_type, locked) VALUES
(29, 2, 'B-05', 1, 5, 0, 0),
(30, 2, 'B-06', 2, 1, 0, 0),
(31, 2, 'B-07', 2, 2, 0, 0),
(32, 2, 'B-08', 2, 3, 0, 0),
(33, 2, 'B-09', 2, 4, 0, 0),
(34, 2, 'B-10', 2, 5, 0, 0),
(35, 2, 'B-11', 3, 1, 0, 0),
(36, 2, 'B-12', 3, 2, 0, 0),
(37, 2, 'B-13', 3, 3, 0, 0),
(38, 2, 'B-14', 3, 4, 0, 0),
(39, 2, 'B-15', 3, 5, 0, 0),
(40, 2, 'B-16', 4, 1, 0, 0),
(41, 2, 'B-17', 4, 2, 0, 0),
(42, 2, 'B-18', 4, 3, 0, 0),
(43, 2, 'B-19', 4, 4, 0, 0),
(44, 2, 'B-20', 4, 5, 0, 0)
ON DUPLICATE KEY UPDATE
  room_id = VALUES(room_id),
  seat_name = VALUES(seat_name),
  grid_row = VALUES(grid_row),
  grid_col = VALUES(grid_col),
  seat_type = VALUES(seat_type),
  locked = VALUES(locked);

-- 3F C 自习室 C-05 … C-20（id 45–60）
INSERT INTO bas_seat (id, room_id, seat_name, grid_row, grid_col, seat_type, locked) VALUES
(45, 3, 'C-05', 1, 5, 0, 0),
(46, 3, 'C-06', 2, 1, 0, 0),
(47, 3, 'C-07', 2, 2, 0, 0),
(48, 3, 'C-08', 2, 3, 0, 0),
(49, 3, 'C-09', 2, 4, 0, 0),
(50, 3, 'C-10', 2, 5, 0, 0),
(51, 3, 'C-11', 3, 1, 0, 0),
(52, 3, 'C-12', 3, 2, 0, 0),
(53, 3, 'C-13', 3, 3, 0, 0),
(54, 3, 'C-14', 3, 4, 0, 0),
(55, 3, 'C-15', 3, 5, 0, 0),
(56, 3, 'C-16', 4, 1, 0, 0),
(57, 3, 'C-17', 4, 2, 0, 0),
(58, 3, 'C-18', 4, 3, 0, 0),
(59, 3, 'C-19', 4, 4, 0, 0),
(60, 3, 'C-20', 4, 5, 0, 0)
ON DUPLICATE KEY UPDATE
  room_id = VALUES(room_id),
  seat_name = VALUES(seat_name),
  grid_row = VALUES(grid_row),
  grid_col = VALUES(grid_col),
  seat_type = VALUES(seat_type),
  locked = VALUES(locked);
