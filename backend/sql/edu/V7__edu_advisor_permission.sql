-- =====================================================================
-- V7：导师看板（Advisor Dashboard）权限
-- ---------------------------------------------------------------------
-- 背景：旧版 aa_dashboard_v2.html（导师看板）本轮迁移为 /education/advisor。
-- 教育平台的页面路由由后端菜单驱动，因此必须补一条菜单记录，否则前端拿不到路由。
--
-- 授予：
--   TEACHER(4) —— 导师看板的本职使用者
--   ADMIN(1)   —— 若依超级管理员自动拥有全部权限，无需 sys_role_menu
--
-- 说明：按钮级权限暂未细分（看板是只读聚合页），
--       若后续出现「导出/配置权重」等写操作，再补 edu:advisor:export 等按钮权限。
-- =====================================================================

SET NAMES utf8mb4;

-- 幂等：重复执行不报错
delete from sys_role_menu where menu_id = 2090;
delete from sys_menu where menu_id = 2090;

insert into sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values (2090, '导师看板', 2000, 9, 'advisor', 'education/advisor/index', null, '', 1, 0, 'C', '0', '0', 'edu:advisor:view', 'peoples', 'admin', sysdate(), '', null, '导师视角：GPA 分布/趋势、学分进度、风险学生、学生画像');

insert into sys_role_menu (role_id, menu_id) values (4, 2090);

-- 校验：应返回 1 行菜单 + 1 行角色授权
select m.menu_id, m.menu_name, m.path, m.component, m.perms,
       (select count(*) from sys_role_menu rm where rm.menu_id = m.menu_id) as role_grants
from sys_menu m where m.menu_id = 2090;
