-- =====================================================================
-- V8：成绩单上传（学生自填）权限
-- ---------------------------------------------------------------------
-- 页面：/student/transcript（学生端常量路由，不在后端菜单里）
-- 接口：POST /edu/transcripts/import
--
-- 这里只需要一条「按钮权限」来承载 edu:transcript:upload，
-- 因为若依的 permissions 来自 sys_menu.perms，角色授权也走 sys_role_menu。
-- menu_type='F' 表示按钮，不会生成路由。
--
-- 只授予 STUDENT(3)：教师/管理员不需要上传自己的成绩单；
-- ADMIN(1) 是超级管理员，自动拥有全部权限，无需 sys_role_menu。
-- =====================================================================

SET NAMES utf8mb4;

-- 幂等
delete from sys_role_menu where menu_id = 2091;
delete from sys_menu where menu_id = 2091;

insert into sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values (2091, '成绩单上传', 2000, 10, '', null, null, '', 1, 0, 'F', '0', '0', 'edu:transcript:upload', '#', 'admin', sysdate(), '', null, '学生自填成绩单导入（前端页 /student/transcript）');

insert into sys_role_menu (role_id, menu_id) values (3, 2091);

-- 校验：应返回 1 行，role_grants=1
select m.menu_id, m.menu_name, m.menu_type, m.perms,
       (select count(*) from sys_role_menu rm where rm.menu_id = m.menu_id) as role_grants
from sys_menu m where m.menu_id = 2091;
