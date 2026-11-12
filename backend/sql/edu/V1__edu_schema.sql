-- =====================================================================
-- 教育业务模块数据库结构（首版）
-- ---------------------------------------------------------------------
-- 说明：
--   1. 本文件只包含教育业务表（edu_ 前缀），若依基础表见 backend/sql/ry_20260417.sql。
--   2. 执行顺序：ry_20260417.sql -> quartz.sql -> edu/V1 -> V2 -> V3 -> V4 -> V5 -> V6。
--   3. V3~V6 为演示数据，由 pipeline/course_import/build_edu_seed.py 从
--      data/course_catalog、data/demo、data/competency、data/graduation_rules 重新生成，
--      请勿手工编辑生成结果。
--   4. 字段、约束与索引的评审记录见 docs/architecture/er-diagram.md。
-- =====================================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ---------------------------------------------------------------------
-- 1. 专业（培养方案的归属对象）
--    方案原文的 edu_curriculum.major_id 需要一个专业主表，故补充本表。
-- ---------------------------------------------------------------------
drop table if exists edu_major;
create table edu_major (
  major_id        bigint(20)   not null auto_increment  comment '专业ID',
  major_code      varchar(64)  not null                 comment '专业编码（唯一，建议用英文缩写）',
  major_name_zh   varchar(128) default ''               comment '专业中文名',
  major_name_en   varchar(128) not null                 comment '专业英文名',
  school_abbr     varchar(32)  default ''               comment '所属学院缩写，如 SDS / SME',
  school_name     varchar(128) default ''               comment '所属学院名称',
  degree_type     varchar(32)  default ''               comment '学位类型',
  duration_years  decimal(3,1) default 4.0              comment '标准学制（年）',
  status          char(1)      default '0'              comment '状态（0正常 1停用）',
  del_flag        char(1)      default '0'              comment '删除标志（0存在 2删除）',
  create_by       varchar(64)  default ''               comment '创建者',
  create_time     datetime                              comment '创建时间',
  update_by       varchar(64)  default ''               comment '更新者',
  update_time     datetime                              comment '更新时间',
  remark          varchar(500) default null             comment '备注',
  primary key (major_id),
  unique key uk_major_code (major_code)
) engine=innodb auto_increment=1000 comment = '专业表';

-- ---------------------------------------------------------------------
-- 2. 课程目录
--    数据来源：SIS 课程大纲导出（data/course_catalog/sis_course_outlines_export.xlsx）
-- ---------------------------------------------------------------------
drop table if exists edu_course;
create table edu_course (
  course_id        bigint(20)   not null auto_increment  comment '课程ID',
  code             varchar(32)  not null                 comment '课程代码，如 CSC1001',
  title_zh         varchar(255) default ''               comment '课程中文名',
  title_en         varchar(255) default ''               comment '课程英文名',
  subject          varchar(128) default ''               comment '开课学科（SIS subject）',
  course_number    varchar(32)  default ''               comment '课程序号，如 1001',
  letter           char(1)      default ''               comment '首字母分组（A-Z）',
  credits          decimal(4,2) not null default 0.00    comment '学分',
  level            int(4)       default 0                comment '课程层级（由课程代码推算，如 1001->1000）',
  category         varchar(32)  default ''               comment '课程类别（Major/GenEd/English/Chinese/IT/PE/Other）',
  grading_basis    varchar(32)  default ''               comment '评分方式',
  component        varchar(32)  default ''               comment '课程组成部分',
  campus           varchar(64)  default ''               comment '校区',
  school           varchar(128) default ''               comment '开课学院',
  academic_org     varchar(128) default ''               comment '教学组织',
  outline_status   varchar(32)  default ''               comment '大纲状态（saved_outline / ghost_outline_unavailable）',
  terms_status     varchar(32)  default ''               comment '开课学期状态',
  offered_terms    text                                  comment '历史开课学期（换行分隔）',
  prereq_text      varchar(1000) default ''              comment '先修课原始文本（保留 SIS 原文）',
  coreq_text       varchar(500) default ''               comment '共修课原始文本',
  description_zh   text                                  comment '课程简介（中文）',
  description_en   text                                  comment '课程简介（英文）',
  status           char(1)      default '0'              comment '状态（0正常 1停用）',
  del_flag         char(1)      default '0'              comment '删除标志（0存在 2删除）',
  create_by        varchar(64)  default ''               comment '创建者',
  create_time      datetime                              comment '创建时间',
  update_by        varchar(64)  default ''               comment '更新者',
  update_time      datetime                              comment '更新时间',
  remark           varchar(500) default null             comment '备注',
  primary key (course_id),
  unique key uk_course_code (code),
  key idx_course_subject (subject),
  key idx_course_school (school),
  key idx_course_level (level),
  key idx_course_category (category)
) engine=innodb auto_increment=100000 comment = '课程目录表';

-- ---------------------------------------------------------------------
-- 3. 先修依赖（有向边）
--    语义：同一 group_no 内的课程为 AND 关系（组内全部满足）；
--          不同 group_no 之间为 OR 关系（任一组合满足即可）。
--          例：(ACT3111 & ACT3112) or ACT3011 -> group1={3111,3112}, group2={3011}
-- ---------------------------------------------------------------------
drop table if exists edu_course_prereq;
create table edu_course_prereq (
  id                  bigint(20)   not null auto_increment comment '主键',
  course_id           bigint(20)   not null                comment '课程ID',
  prereq_course_id    bigint(20)   not null                comment '先修课程ID',
  group_no            int(4)       not null default 1      comment '先修组号：组内 AND，组间 OR',
  raw_text            varchar(500) default ''              comment 'SIS 原始先修文本片段（可追溯）',
  create_by           varchar(64)  default ''              comment '创建者',
  create_time         datetime                             comment '创建时间',
  primary key (id),
  unique key uk_course_prereq (course_id, prereq_course_id),
  key idx_prereq_target (prereq_course_id),
  constraint fk_prereq_course foreign key (course_id) references edu_course (course_id) on delete cascade,
  constraint fk_prereq_target foreign key (prereq_course_id) references edu_course (course_id) on delete cascade,
  constraint ck_prereq_not_self check (course_id <> prereq_course_id)
) engine=innodb comment = '课程先修关系表';

-- ---------------------------------------------------------------------
-- 4. 培养方案（按版本保存，避免历史规则被覆盖）
-- ---------------------------------------------------------------------
drop table if exists edu_curriculum;
create table edu_curriculum (
  curriculum_id    bigint(20)   not null auto_increment  comment '培养方案ID',
  major_id         bigint(20)   not null                 comment '专业ID',
  version_label    varchar(64)  not null                 comment '版本标签，如 2025-26 and thereafter',
  version_year     int(4)       not null                 comment '生效入学年份',
  effective_from   int(4)       default null             comment '适用起始入学年',
  effective_to     int(4)       default null             comment '适用结束入学年（null 表示一直有效）',
  total_credits    decimal(5,1) default 0.0              comment '毕业总学分要求',
  major_credits    decimal(5,1) default 0.0              comment '专业学分要求',
  source_doc       varchar(255) default ''               comment '来源文件名',
  status           char(1)      default '0'              comment '状态（0启用 1停用）',
  del_flag         char(1)      default '0'              comment '删除标志',
  create_by        varchar(64)  default ''               comment '创建者',
  create_time      datetime                              comment '创建时间',
  update_by        varchar(64)  default ''               comment '更新者',
  update_time      datetime                              comment '更新时间',
  remark           varchar(500) default null             comment '备注',
  primary key (curriculum_id),
  unique key uk_curriculum_version (major_id, version_year),
  constraint fk_curriculum_major foreign key (major_id) references edu_major (major_id) on delete restrict
) engine=innodb auto_increment=1000 comment = '培养方案表';

-- ---------------------------------------------------------------------
-- 5. 培养方案课程要求
-- ---------------------------------------------------------------------
drop table if exists edu_curriculum_item;
create table edu_curriculum_item (
  item_id          bigint(20)   not null auto_increment  comment '主键',
  curriculum_id    bigint(20)   not null                 comment '培养方案ID',
  course_id        bigint(20)   default null             comment '课程ID（可为空，表示纯学分池）',
  course_code      varchar(32)  default ''               comment '课程代码快照（课程未入库时保留）',
  requirement_type varchar(32)  not null                 comment '要求类型：REQUIRED/ELECTIVE/GE/BREADTH/DEPTH/STREAM',
  section          varchar(255) default ''               comment '方案中的章节名称（如 School Package）',
  stream           varchar(128) default ''               comment '方向/流派（可选）',
  min_units        decimal(4,1) default 0.0              comment '该条目最低学分',
  sort_order       int(4)       default 0                comment '排序',
  create_by        varchar(64)  default ''               comment '创建者',
  create_time      datetime                              comment '创建时间',
  primary key (item_id),
  key idx_item_curriculum (curriculum_id),
  key idx_item_course (course_id),
  constraint fk_item_curriculum foreign key (curriculum_id) references edu_curriculum (curriculum_id) on delete cascade,
  constraint fk_item_course foreign key (course_id) references edu_course (course_id) on delete set null
) engine=innodb comment = '培养方案课程要求表';

-- ---------------------------------------------------------------------
-- 6. 学期
-- ---------------------------------------------------------------------
drop table if exists edu_term;
create table edu_term (
  term_id          bigint(20)   not null auto_increment  comment '学期ID',
  term_code        varchar(32)  not null                 comment '学期代码，如 2026-27-1',
  term_name        varchar(64)  not null                 comment '学期名称',
  academic_year    varchar(16)  default ''               comment '学年',
  term_index       int(4)       default 1                comment '学期序号（1/2/3=暑期）',
  start_date       date         default null             comment '开始日期',
  end_date         date         default null             comment '结束日期',
  is_current       char(1)      default '0'              comment '是否当前学期（0否 1是）',
  status           char(1)      default '0'              comment '状态（0正常 1停用）',
  create_time      datetime                              comment '创建时间',
  primary key (term_id),
  unique key uk_term_code (term_code)
) engine=innodb auto_increment=1000 comment = '学期表';

-- ---------------------------------------------------------------------
-- 7. 学生（学籍侧资料，与 sys_user 一对一）
--    不以姓名作为唯一键；成绩查询必须由后端绑定当前登录用户。
-- ---------------------------------------------------------------------
drop table if exists edu_student;
create table edu_student (
  student_id       bigint(20)   not null auto_increment  comment '学生ID',
  sys_user_id      bigint(20)   not null                 comment '关联的系统用户ID',
  student_no       varchar(32)  not null                 comment '学号（唯一）',
  name             varchar(64)  default ''               comment '姓名（演示数据，正式环境需脱敏）',
  major_id         bigint(20)   default null             comment '专业ID',
  entry_year       int(4)       default null             comment '入学年份',
  college          varchar(64)  default ''               comment '书院/学院',
  email            varchar(128) default ''               comment '邮箱',
  cum_gpa          decimal(3,2) default null             comment '累计 GPA（演示用）',
  total_credits    decimal(5,1) default 0.0              comment '已修总学分（演示用）',
  status           char(1)      default '0'              comment '状态（0正常 1停用）',
  del_flag         char(1)      default '0'              comment '删除标志',
  create_by        varchar(64)  default ''               comment '创建者',
  create_time      datetime                              comment '创建时间',
  update_by        varchar(64)  default ''               comment '更新者',
  update_time      datetime                              comment '更新时间',
  remark           varchar(500) default null             comment '备注',
  primary key (student_id),
  unique key uk_student_no (student_no),
  unique key uk_student_sys_user (sys_user_id),
  key idx_student_major (major_id),
  key idx_student_entry_year (entry_year),
  constraint fk_student_major foreign key (major_id) references edu_major (major_id) on delete set null
) engine=innodb auto_increment=1000 comment = '学生学籍表';

-- ---------------------------------------------------------------------
-- 8. 教师（与 sys_user 一对一）
-- ---------------------------------------------------------------------
drop table if exists edu_teacher;
create table edu_teacher (
  teacher_id       bigint(20)   not null auto_increment  comment '教师ID',
  sys_user_id      bigint(20)   not null                 comment '关联的系统用户ID',
  teacher_no       varchar(32)  not null                 comment '教工号（唯一）',
  name             varchar(64)  default ''               comment '姓名',
  dept_id          bigint(20)   default null             comment '部门ID（沿用 sys_dept）',
  school           varchar(128) default ''               comment '所属学院',
  title            varchar(64)  default ''               comment '职称',
  status           char(1)      default '0'              comment '状态（0正常 1停用）',
  del_flag         char(1)      default '0'              comment '删除标志',
  create_by        varchar(64)  default ''               comment '创建者',
  create_time      datetime                              comment '创建时间',
  update_by        varchar(64)  default ''               comment '更新者',
  update_time      datetime                              comment '更新时间',
  primary key (teacher_id),
  unique key uk_teacher_no (teacher_no),
  unique key uk_teacher_sys_user (sys_user_id)
) engine=innodb auto_increment=1000 comment = '教师表';

-- ---------------------------------------------------------------------
-- 9. 学期开课
-- ---------------------------------------------------------------------
drop table if exists edu_course_offering;
create table edu_course_offering (
  offering_id      bigint(20)   not null auto_increment  comment '开课ID',
  course_id        bigint(20)   not null                 comment '课程ID',
  term_id          bigint(20)   not null                 comment '学期ID',
  teacher_id       bigint(20)   default null             comment '授课教师ID',
  capacity         int(4)       default 0                comment '容量',
  enrolled_count   int(4)       default 0                comment '已选人数',
  schedule_text    varchar(255) default ''               comment '上课时间地点',
  status           char(1)      default '0'              comment '状态（0开放 1关闭）',
  create_by        varchar(64)  default ''               comment '创建者',
  create_time      datetime                              comment '创建时间',
  update_by        varchar(64)  default ''               comment '更新者',
  update_time      datetime                              comment '更新时间',
  primary key (offering_id),
  unique key uk_offering_course_term (course_id, term_id),
  key idx_offering_term (term_id),
  key idx_offering_teacher (teacher_id),
  constraint fk_offering_course foreign key (course_id) references edu_course (course_id) on delete cascade,
  constraint fk_offering_term foreign key (term_id) references edu_term (term_id) on delete restrict,
  constraint fk_offering_teacher foreign key (teacher_id) references edu_teacher (teacher_id) on delete set null
) engine=innodb auto_increment=1000 comment = '学期开课表';

-- ---------------------------------------------------------------------
-- 10. 选课关系
-- ---------------------------------------------------------------------
drop table if exists edu_enrollment;
create table edu_enrollment (
  enrollment_id    bigint(20)   not null auto_increment  comment '选课ID',
  student_id       bigint(20)   not null                 comment '学生ID',
  offering_id      bigint(20)   not null                 comment '开课ID',
  status           varchar(16)  not null default 'ENROLLED' comment '状态：ENROLLED/DROPPED/COMPLETED',
  enrolled_at      datetime                              comment '选课时间',
  create_by        varchar(64)  default ''               comment '创建者',
  create_time      datetime                              comment '创建时间',
  update_by        varchar(64)  default ''               comment '更新者',
  update_time      datetime                              comment '更新时间',
  primary key (enrollment_id),
  unique key uk_enrollment (student_id, offering_id),
  key idx_enrollment_offering (offering_id),
  key idx_enrollment_status (status),
  constraint fk_enrollment_student foreign key (student_id) references edu_student (student_id) on delete cascade,
  constraint fk_enrollment_offering foreign key (offering_id) references edu_course_offering (offering_id) on delete cascade
) engine=innodb auto_increment=1000 comment = '选课关系表';

-- ---------------------------------------------------------------------
-- 11. 已修课程与成绩
--    正式成绩必须在获得校方数据授权后写入，演示环境使用演示数据。
-- ---------------------------------------------------------------------
drop table if exists edu_course_record;
create table edu_course_record (
  record_id        bigint(20)   not null auto_increment  comment '成绩记录ID',
  student_id       bigint(20)   not null                 comment '学生ID',
  course_id        bigint(20)   default null             comment '课程ID（课程目录命中时写入）',
  course_code      varchar(32)  not null                 comment '课程代码快照',
  course_title     varchar(255) default ''               comment '课程名称快照',
  term_id          bigint(20)   default null             comment '学期ID',
  grade            varchar(8)   default ''               comment '等级制成绩，如 A-/B+',
  score            decimal(5,2) default null             comment '百分制成绩',
  units            decimal(4,2) default 0.00             comment '学分',
  category         varchar(32)  default ''               comment '成绩类别（Major/GenEd/English/Chinese/IT/PE）',
  passed           char(1)      default '1'              comment '是否通过（0否 1是）',
  record_status    varchar(16)  default 'COMPLETED'      comment '状态：COMPLETED/IN_PROGRESS/WITHDRAWN',
  source           varchar(32)  default 'DEMO'           comment '数据来源：SIS_IMPORT/DEMO/MANUAL',
  del_flag         char(1)      default '0'              comment '删除标志',
  create_by        varchar(64)  default ''               comment '创建者',
  create_time      datetime                              comment '创建时间',
  update_by        varchar(64)  default ''               comment '更新者',
  update_time      datetime                              comment '更新时间',
  primary key (record_id),
  unique key uk_record_student_course_term (student_id, course_code, term_id),
  key idx_record_student (student_id),
  key idx_record_course (course_id),
  key idx_record_term (term_id),
  constraint fk_record_student foreign key (student_id) references edu_student (student_id) on delete cascade,
  constraint fk_record_course foreign key (course_id) references edu_course (course_id) on delete set null,
  constraint fk_record_term foreign key (term_id) references edu_term (term_id) on delete set null,
  constraint ck_record_units check (units >= 0)
) engine=innodb auto_increment=1000 comment = '已修课程与成绩表';

-- ---------------------------------------------------------------------
-- 12. 作业
-- ---------------------------------------------------------------------
drop table if exists edu_assignment;
create table edu_assignment (
  assignment_id    bigint(20)   not null auto_increment  comment '作业ID',
  offering_id      bigint(20)   not null                 comment '开课ID',
  title            varchar(200) not null                 comment '作业标题',
  description      text                                  comment '作业说明',
  due_at           datetime                              comment '截止时间',
  total_score      decimal(5,2) default 100.00           comment '满分',
  status           varchar(16)  default 'DRAFT'          comment '状态：DRAFT/PUBLISHED/CLOSED',
  del_flag         char(1)      default '0'              comment '删除标志',
  create_by        varchar(64)  default ''               comment '创建者',
  create_time      datetime                              comment '创建时间',
  update_by        varchar(64)  default ''               comment '更新者',
  update_time      datetime                              comment '更新时间',
  primary key (assignment_id),
  key idx_assignment_offering (offering_id),
  constraint fk_assignment_offering foreign key (offering_id) references edu_course_offering (offering_id) on delete cascade
) engine=innodb auto_increment=1000 comment = '作业任务表';

-- ---------------------------------------------------------------------
-- 13. 作业提交
-- ---------------------------------------------------------------------
drop table if exists edu_submission;
create table edu_submission (
  submission_id    bigint(20)   not null auto_increment  comment '提交ID',
  assignment_id    bigint(20)   not null                 comment '作业ID',
  student_id       bigint(20)   not null                 comment '学生ID',
  content          text                                  comment '提交内容',
  attachment_url   varchar(500) default ''               comment '附件地址',
  submitted_at     datetime                              comment '提交时间',
  grade            decimal(5,2) default null             comment '得分',
  feedback         varchar(1000) default ''              comment '教师评语',
  status           varchar(16)  default 'SUBMITTED'      comment '状态：SUBMITTED/GRADED/RETURNED',
  create_by        varchar(64)  default ''               comment '创建者',
  create_time      datetime                              comment '创建时间',
  update_by        varchar(64)  default ''               comment '更新者',
  update_time      datetime                              comment '更新时间',
  primary key (submission_id),
  unique key uk_submission (assignment_id, student_id),
  key idx_submission_student (student_id),
  constraint fk_submission_assignment foreign key (assignment_id) references edu_assignment (assignment_id) on delete cascade,
  constraint fk_submission_student foreign key (student_id) references edu_student (student_id) on delete cascade
) engine=innodb auto_increment=1000 comment = '作业提交表';

-- ---------------------------------------------------------------------
-- 14. 学习规划主表
-- ---------------------------------------------------------------------
drop table if exists edu_study_plan;
create table edu_study_plan (
  plan_id          bigint(20)   not null auto_increment  comment '规划ID',
  student_id       bigint(20)   not null                 comment '学生ID',
  term_id          bigint(20)   not null                 comment '目标学期ID',
  title            varchar(200) default ''               comment '规划标题',
  status           varchar(16)  default 'DRAFT'          comment '状态：DRAFT/VALIDATED/SUBMITTED/ARCHIVED',
  total_units      decimal(5,1) default 0.0              comment '计划总学分',
  unit_limit       decimal(5,1) default 21.0             comment '学分上限（可由管理员配置）',
  validation_state varchar(16)  default ''               comment '最近一次校验结果：PASS/WARN/FAIL',
  validation_note  varchar(1000) default ''              comment '校验摘要',
  del_flag         char(1)      default '0'              comment '删除标志',
  create_by        varchar(64)  default ''               comment '创建者',
  create_time      datetime                              comment '创建时间',
  update_by        varchar(64)  default ''               comment '更新者',
  update_time      datetime                              comment '更新时间',
  primary key (plan_id),
  unique key uk_plan_student_term (student_id, term_id),
  key idx_plan_student (student_id),
  key idx_plan_term (term_id),
  constraint fk_plan_student foreign key (student_id) references edu_student (student_id) on delete cascade,
  constraint fk_plan_term foreign key (term_id) references edu_term (term_id) on delete restrict
) engine=innodb auto_increment=1000 comment = '学习规划表';

-- ---------------------------------------------------------------------
-- 15. 学习规划明细（含可解释的规则说明）
-- ---------------------------------------------------------------------
drop table if exists edu_study_plan_item;
create table edu_study_plan_item (
  item_id           bigint(20)   not null auto_increment comment '主键',
  plan_id           bigint(20)   not null                comment '规划ID',
  course_id         bigint(20)   not null                comment '课程ID',
  reason            varchar(500) default ''              comment '推荐理由（可解释说明）',
  validation_status varchar(16)  default 'UNKNOWN'       comment '校验状态：VALID/WARN/INVALID/UNKNOWN',
  validation_message varchar(1000) default ''            comment '校验信息（缺少的先修课等）',
  sort_order        int(4)       default 0               comment '排序',
  primary key (item_id),
  unique key uk_plan_item (plan_id, course_id),
  key idx_plan_item_course (course_id),
  constraint fk_plan_item_plan foreign key (plan_id) references edu_study_plan (plan_id) on delete cascade,
  constraint fk_plan_item_course foreign key (course_id) references edu_course (course_id) on delete cascade
) engine=innodb comment = '学习规划明细表';

-- ---------------------------------------------------------------------
-- 16. 公共技能标签
-- ---------------------------------------------------------------------
drop table if exists edu_skill_tag;
create table edu_skill_tag (
  skill_tag_id     bigint(20)   not null auto_increment  comment '技能标签ID',
  code             varchar(64)  not null                 comment '标签编码（唯一）',
  name             varchar(200) not null                 comment '标签名称',
  category         varchar(64)  default ''               comment '分类（学院/能力域）',
  school_abbr      varchar(32)  default ''               comment '所属学院缩写',
  description      varchar(1000) default ''              comment '定义说明',
  keywords         varchar(1000) default ''              comment '匹配关键词',
  sort_order       int(4)       default 0                comment '排序',
  status           char(1)      default '0'              comment '状态（0正常 1停用）',
  del_flag         char(1)      default '0'              comment '删除标志',
  create_time      datetime                              comment '创建时间',
  primary key (skill_tag_id),
  unique key uk_skill_tag_code (code),
  key idx_skill_tag_category (category)
) engine=innodb auto_increment=1000 comment = '公共技能标签表';

-- ---------------------------------------------------------------------
-- 17. 课程—技能关联度（学院能力面板数据来源）
-- ---------------------------------------------------------------------
drop table if exists edu_course_competency;
create table edu_course_competency (
  id               bigint(20)   not null auto_increment  comment '主键',
  course_id        bigint(20)   not null                 comment '课程ID',
  skill_tag_id     bigint(20)   not null                 comment '技能标签ID',
  score            decimal(4,1) default 0.0              comment '关联度评分（0-10）',
  level            varchar(32)  default ''               comment '关联等级（Very high/High/Moderate/Low）',
  rationale        varchar(1000) default ''              comment '判断依据',
  matched_keywords varchar(500) default ''               comment '命中的关键词',
  create_time      datetime                              comment '创建时间',
  primary key (id),
  unique key uk_course_competency (course_id, skill_tag_id),
  key idx_competency_skill (skill_tag_id),
  constraint fk_competency_course foreign key (course_id) references edu_course (course_id) on delete cascade,
  constraint fk_competency_skill foreign key (skill_tag_id) references edu_skill_tag (skill_tag_id) on delete cascade
) engine=innodb comment = '课程技能关联表';

-- ---------------------------------------------------------------------
-- 18. 学校身份绑定（SSO，后期启用）
--     说明：本表只保存外部身份映射，不保存学校密码。
-- ---------------------------------------------------------------------
drop table if exists edu_sso_identity;
create table edu_sso_identity (
  identity_id      bigint(20)   not null auto_increment  comment '主键',
  sys_user_id      bigint(20)   not null                 comment '平台用户ID',
  provider         varchar(64)  not null                 comment '身份提供方，如 CUHKSZ_OIDC',
  external_subject varchar(255) not null                 comment '外部唯一标识（subject / 学工号）',
  bound_at         datetime                              comment '绑定时间',
  status           char(1)      default '0'              comment '状态（0正常 1停用）',
  create_by        varchar(64)  default ''               comment '创建者',
  create_time      datetime                              comment '创建时间',
  update_by        varchar(64)  default ''               comment '更新者',
  update_time      datetime                              comment '更新时间',
  primary key (identity_id),
  unique key uk_sso_provider_subject (provider, external_subject),
  unique key uk_sso_user_provider (sys_user_id, provider)
) engine=innodb comment = '学校身份绑定表';

set FOREIGN_KEY_CHECKS = 1;

-- ---------------------------------------------------------------------
-- 注意：本方案刻意不创建 edu_user_profile 明文表。
-- 个人技能画像默认只保存在浏览器 IndexedDB（Dexie.js），
-- 详见 docs/architecture/adr/ADR-0004-local-first-skill-profile.md。
-- ---------------------------------------------------------------------
