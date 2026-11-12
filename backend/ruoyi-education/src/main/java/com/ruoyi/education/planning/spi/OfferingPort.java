package com.ruoyi.education.planning.spi;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 学期开课只读端口：提供目标学期的开课课程与排课片段，用于“未开课 / 时间冲突”校验。
 *
 * <p>对应选课域的能力：{@code edu_course_offering}（term_id + course_id + schedule_text）。
 * 默认实现见 {@code DefaultPlanningReadPorts}。</p>
 *
 * @author ruoyi
 */
public interface OfferingPort
{
    /**
     * 目标学期开放选课的课程ID。
     *
     * @param termId 学期ID
     * @return 课程ID集合；无开课数据时返回空集合，服务层会视为“该校期无开课数据”并跳过开课校验
     */
    Set<Long> loadOfferedCourseIds(Long termId);

    /**
     * 目标学期的排课时间槽。
     *
     * @param termId 学期ID
     * @return courseId -> 时间槽列表；{@code null} 表示无排课数据，跳过冲突校验
     */
    Map<Long, List<String>> loadScheduleSlots(Long termId);
}
