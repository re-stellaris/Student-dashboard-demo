package com.ruoyi.education.planning.spi;

import java.util.Set;

/**
 * 成绩单只读端口：读取学生“已通过”的课程集合。
 *
 * <p>对应选课/成绩域的能力：
 * {@code com.ruoyi.education.enrollment.mapper.EduCourseRecordMapper#selectPassedCourseIds}。
 * 默认实现见 {@code DefaultPlanningReadPorts}。</p>
 *
 * @author ruoyi
 */
public interface TranscriptPort
{
    /**
     * 读取学生已通过（{@code passed='1'} 且成绩为 COMPLETED）的课程ID。
     *
     * @param studentId 学生ID
     * @return 已通过课程ID集合；无记录时返回空集合（不返回 {@code null}）
     */
    Set<Long> loadPassedCourseIds(Long studentId);
}
