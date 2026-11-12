-- =====================================================================
-- 教育平台权限与演示账号
-- ---------------------------------------------------------------------
-- 内容：
--   1. 学院部门（sys_dept）
--   2. 角色：STUDENT / TEACHER（ADMIN 复用若依超级管理员）
--   3. 演示账号（学生 8 个、教师 1 个）
--   4. 教育菜单与按钮权限（menu_id 2000 起，避免与若依内置菜单冲突）
--   5. 角色-菜单授权
-- ---------------------------------------------------------------------
-- 安全提示：
--   * 所有演示账号的初始密码均为 admin123（与若依默认一致），上线前必须修改。
--   * 菜单权限只是入口权限，不能替代服务端资源级鉴权，
--     详见 docs/architecture/api-contract.md「访问控制」一节。
-- =====================================================================

SET NAMES utf8mb4;

-- ---------------------------------------------------------------------
-- 1. 学院部门
-- ---------------------------------------------------------------------
insert into sys_dept (dept_id, parent_id, ancestors, dept_name, order_num, leader, phone, email, status, del_flag, create_by, create_time, update_by, update_time)
values (110, 100, '0,100', '数据科学学院', 10, 'SDS', '84273000', 'sds@example.edu.cn', '0', '0', 'admin', sysdate(), '', null);
insert into sys_dept (dept_id, parent_id, ancestors, dept_name, order_num, leader, phone, email, status, del_flag, create_by, create_time, update_by, update_time)
values (111, 100, '0,100', '经管学院', 11, 'SME', '84273001', 'sme@example.edu.cn', '0', '0', 'admin', sysdate(), '', null);

-- ---------------------------------------------------------------------
-- 2. 角色
-- ---------------------------------------------------------------------
insert into sys_role (role_id, role_name, role_key, role_sort, data_scope, menu_check_strictly, dept_check_strictly, status, del_flag, create_by, create_time, update_by, update_time, remark)
values (3, '学生', 'STUDENT', 3, '5', 1, 1, '0', '0', 'admin', sysdate(), '', null, '学生角色：仅可访问本人数据');

insert into sys_role (role_id, role_name, role_key, role_sort, data_scope, menu_check_strictly, dept_check_strictly, status, del_flag, create_by, create_time, update_by, update_time, remark)
values (4, '教师', 'TEACHER', 4, '4', 1, 1, '0', '0', 'admin', sysdate(), '', null, '教师角色：仅可访问本人授课数据');

-- ---------------------------------------------------------------------
-- 3. 演示账号
--    密码统一为 admin123 的 BCrypt 哈希（与若依默认 admin 相同），上线前必须修改。
--    user_id 与学生学号映射关系同时写入 edu_student（见 V5 演示数据）。
-- ---------------------------------------------------------------------
insert into sys_user (user_id, dept_id, user_name, nick_name, user_type, email, phonenumber, sex, avatar, password, status, del_flag, login_ip, login_date, pwd_update_date, create_by, create_time, update_by, update_time, remark)
values
 (101, 110, 'stu_chen',    'CHEN MICHAEL', '00', '122040156@link.cuhk.edu.cn', '', '0', '', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '0', '0', '', null, sysdate(), 'admin', sysdate(), '', null, '演示学生：CSE'),
 (102, 110, 'stu_wang',    'WANG SOPHIA',  '00', '122040267@link.cuhk.edu.cn', '', '1', '', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '0', '0', '', null, sysdate(), 'admin', sysdate(), '', null, '演示学生：DSBDT'),
 (103, 110, 'stu_liu',     'LIU JAMES',    '00', '122040384@link.cuhk.edu.cn', '', '0', '', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '0', '0', '', null, sysdate(), 'admin', sysdate(), '', null, '演示学生：FE'),
 (104, 110, 'stu_ean',     'EAN JIE',      '00', '122040491@link.cuhk.edu.cn', '', '0', '', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '0', '0', '', null, sysdate(), 'admin', sysdate(), '', null, '演示学生：Statistics'),
 (105, 111, 'stu_zixuan',  'Zixuan Wang',  '00', '122010001@link.cuhk.edu.cn', '', '1', '', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '0', '0', '', null, sysdate(), 'admin', sysdate(), '', null, '演示学生：ADA'),
 (106, 111, 'stu_muyang',  'Muyang Liu',   '00', '123030001@link.cuhk.edu.cn', '', '0', '', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '0', '0', '', null, sysdate(), 'admin', sysdate(), '', null, '演示学生：AFR'),
 (107, 111, 'stu_zihan',   'Zihan Chen',   '00', '124010001@link.cuhk.edu.cn', '', '1', '', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '0', '0', '', null, sysdate(), 'admin', sysdate(), '', null, '演示学生：ADA'),
 (108, 111, 'stu_ruohan',  'Ruohan Zhou',  '00', '125030003@link.cuhk.edu.cn', '', '1', '', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '0', '0', '', null, sysdate(), 'admin', sysdate(), '', null, '演示学生：AFR'),
 (201, 110, 'teacher_li',  '李老师',       '00', 'teach.li@example.edu.cn',    '', '0', '', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '0', '0', '', null, sysdate(), 'admin', sysdate(), '', null, '演示教师：SDS');

insert into sys_user_role (user_id, role_id) values
 (101, 3), (102, 3), (103, 3), (104, 3), (105, 3), (106, 3), (107, 3), (108, 3),
 (201, 4);

-- ---------------------------------------------------------------------
-- 4. 教育菜单
--    menu_type：M 目录 / C 菜单 / F 按钮
-- ---------------------------------------------------------------------
insert into sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values
 (2000, '教育平台', 0, 5, 'education', null, null, '', 1, 0, 'M', '0', '0', '', 'education', 'admin', sysdate(), '', null, '教育业务一级目录'),

 (2010, '学习仪表盘', 2000, 1, 'dashboard', 'education/dashboard/index', null, '', 1, 0, 'C', '0', '0', 'edu:dashboard:view', 'dashboard', 'admin', sysdate(), '', null, '学分进度与成绩趋势'),
 (2020, '课程中心',   2000, 2, 'course',    'education/course/index',    null, '', 1, 0, 'C', '0', '0', 'edu:course:list',     'list',      'admin', sysdate(), '', null, '课程目录与先修关系'),
 (2030, '培养方案',   2000, 3, 'curriculum','education/curriculum/index',null, '', 1, 0, 'C', '0', '0', 'edu:curriculum:list', 'tree-table','admin', sysdate(), '', null, '专业培养方案与学分要求'),
 (2040, '选课规划',   2000, 4, 'planner',   'education/planner/index',   null, '', 1, 0, 'C', '0', '0', 'edu:plan:list',       'guide',     'admin', sysdate(), '', null, '学期选课规划与规则校验'),
 (2050, '我的成绩',   2000, 5, 'records',   'education/records/index',   null, '', 1, 0, 'C', '0', '0', 'edu:record:list',     'documentation','admin', sysdate(), '', null, '本人已修课程与成绩'),
 (2060, '教师授课',   2000, 6, 'teaching',  'education/teaching/index',  null, '', 1, 0, 'C', '0', '0', 'edu:offering:list',   'peoples',   'admin', sysdate(), '', null, '本人授课与作业成绩'),
 (2070, '技能画像',   2000, 7, 'skill',     'education/skill/index',     null, '', 1, 0, 'C', '0', '0', 'edu:skill:view',      'skill',     'admin', sysdate(), '', null, '本地技能画像（IndexedDB）'),
 (2080, '学情分析',   2000, 8, 'analytics', 'education/analytics/index', null, '', 1, 0, 'C', '0', '0', 'edu:analytics:view',  'chart',     'admin', sysdate(), '', null, '学情统计与能力画像'),

 (2021, '课程查询', 2020, 1, '', null, null, '', 1, 0, 'F', '0', '0', 'edu:course:query',   '#', 'admin', sysdate(), '', null, ''),
 (2022, '课程新增', 2020, 2, '', null, null, '', 1, 0, 'F', '0', '0', 'edu:course:add',     '#', 'admin', sysdate(), '', null, ''),
 (2023, '课程修改', 2020, 3, '', null, null, '', 1, 0, 'F', '0', '0', 'edu:course:edit',    '#', 'admin', sysdate(), '', null, ''),
 (2024, '课程删除', 2020, 4, '', null, null, '', 1, 0, 'F', '0', '0', 'edu:course:remove',  '#', 'admin', sysdate(), '', null, ''),
 (2025, '先修维护', 2020, 5, '', null, null, '', 1, 0, 'F', '0', '0', 'edu:prereq:edit',    '#', 'admin', sysdate(), '', null, '维护先修有向边'),

 (2031, '方案新增', 2030, 1, '', null, null, '', 1, 0, 'F', '0', '0', 'edu:curriculum:add',    '#', 'admin', sysdate(), '', null, ''),
 (2032, '方案修改', 2030, 2, '', null, null, '', 1, 0, 'F', '0', '0', 'edu:curriculum:edit',   '#', 'admin', sysdate(), '', null, ''),
 (2033, '方案删除', 2030, 3, '', null, null, '', 1, 0, 'F', '0', '0', 'edu:curriculum:remove', '#', 'admin', sysdate(), '', null, ''),
 (2034, '条目维护', 2030, 4, '', null, null, '', 1, 0, 'F', '0', '0', 'edu:curriculum:item:edit', '#', 'admin', sysdate(), '', null, '维护培养方案课程要求'),

 (2041, '保存规划', 2040, 1, '', null, null, '', 1, 0, 'F', '0', '0', 'edu:plan:add',      '#', 'admin', sysdate(), '', null, ''),
 (2042, '校验规划', 2040, 2, '', null, null, '', 1, 0, 'F', '0', '0', 'edu:plan:validate', '#', 'admin', sysdate(), '', null, ''),
 (2043, '提交规划', 2040, 3, '', null, null, '', 1, 0, 'F', '0', '0', 'edu:plan:submit',   '#', 'admin', sysdate(), '', null, ''),

 (2051, '成绩查询', 2050, 1, '', null, null, '', 1, 0, 'F', '0', '0', 'edu:record:query', '#', 'admin', sysdate(), '', null, ''),
 (2052, '成绩修正', 2050, 2, '', null, null, '', 1, 0, 'F', '0', '0', 'edu:record:edit',  '#', 'admin', sysdate(), '', null, '管理员/教师修正成绩'),

 (2061, '作业发布', 2060, 1, '', null, null, '', 1, 0, 'F', '0', '0', 'edu:assignment:add',    '#', 'admin', sysdate(), '', null, ''),
 (2062, '作业修改', 2060, 2, '', null, null, '', 1, 0, 'F', '0', '0', 'edu:assignment:edit',   '#', 'admin', sysdate(), '', null, ''),
 (2063, '成绩录入', 2060, 3, '', null, null, '', 1, 0, 'F', '0', '0', 'edu:submission:grade',  '#', 'admin', sysdate(), '', null, ''),
 (2064, '开课维护', 2060, 4, '', null, null, '', 1, 0, 'F', '0', '0', 'edu:offering:edit',     '#', 'admin', sysdate(), '', null, ''),

 (2071, '画像导出', 2070, 1, '', null, null, '', 1, 0, 'F', '0', '0', 'edu:skill:export', '#', 'admin', sysdate(), '', null, '导出/恢复本地画像备份');

-- ---------------------------------------------------------------------
-- 5. 角色-菜单授权
--    STUDENT(3)：仪表盘、课程（只读）、培养方案（只读）、选课规划、我的成绩、技能画像
--    TEACHER(4)：仪表盘、课程（只读）、培养方案（只读）、教师授课、学情分析
-- ---------------------------------------------------------------------
insert into sys_role_menu (role_id, menu_id) values
 (3, 2000), (3, 2010), (3, 2020), (3, 2021), (3, 2030), (3, 2040), (3, 2041), (3, 2042), (3, 2043), (3, 2050), (3, 2051), (3, 2070), (3, 2071),
 (4, 2000), (4, 2010), (4, 2020), (4, 2021), (4, 2030), (4, 2052), (4, 2060), (4, 2061), (4, 2062), (4, 2063), (4, 2064), (4, 2080);
