-- v4：运营管理菜单（预约 / 公告 / 留言 / 小程序用户）
USE `study-room`;

INSERT INTO sys_menu (menu_id, parent_id, name, url, perms, type, icon, order_num) VALUES
(200, 0, '运营管理', NULL, NULL, 0, 'menu', 2),
(201, 200, '预约管理', 'bas/appointment', 'bas:appointment:list', 1, 'log', 1),
(202, 200, '公告管理', 'bas/notice', 'bas:notice:list', 1, 'log', 2),
(203, 200, '留言管理', 'bas/message', 'bas:message:list', 1, 'log', 3),
(204, 200, '小程序用户', 'bas/mini-user', 'bas:miniuser:list', 1, 'log', 4)
ON DUPLICATE KEY UPDATE
  parent_id = VALUES(parent_id),
  name = VALUES(name),
  url = VALUES(url),
  perms = VALUES(perms),
  type = VALUES(type),
  icon = VALUES(icon),
  order_num = VALUES(order_num);

INSERT INTO sys_role_menu (role_id, menu_id) VALUES
(1, 200),
(1, 201),
(1, 202),
(1, 203),
(1, 204)
ON DUPLICATE KEY UPDATE role_id = VALUES(role_id);
