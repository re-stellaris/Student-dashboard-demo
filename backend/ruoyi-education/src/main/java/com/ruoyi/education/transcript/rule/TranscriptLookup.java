package com.ruoyi.education.transcript.rule;

import com.ruoyi.education.transcript.domain.TranscriptCourseRef;
import com.ruoyi.education.transcript.domain.TranscriptRecordRef;

/**
 * 成绩单导入判定所需的只读查询端口
 *
 * <p>由 Service 用一次批量查询实现（学期 / 课程 / 已存在记录），
 * 使 {@link TranscriptImportPlanner} 的判定逻辑成为纯函数，单测不需要 Spring 与数据库。</p>
 *
 * <p>约定：入参已由 Planner 归一化（去空格 + 大写），实现方可直接按原值精确匹配。</p>
 *
 * @author ruoyi-education
 */
public interface TranscriptLookup
{
    /**
     * 按学期代码解析学期ID
     *
     * @param termCode 归一化后的学期代码
     * @return 学期ID；未命中返回 {@code null}（该行应跳过并计入 unknownTerms）
     */
    Long termId(String termCode);

    /**
     * 按课程代码解析课程目录快照
     *
     * @param courseCode 归一化后的课程代码
     * @return 课程快照；未命中返回 {@code null}（仍允许插入，course_id = null）
     */
    TranscriptCourseRef course(String courseCode);

    /**
     * 查询当前学生的同主键成绩记录（不区分 del_flag，避免唯一键冲突）
     *
     * @param courseCode 归一化后的课程代码
     * @param termId 学期ID
     * @return 已存在记录；不存在返回 {@code null}
     */
    TranscriptRecordRef existing(String courseCode, Long termId);
}
