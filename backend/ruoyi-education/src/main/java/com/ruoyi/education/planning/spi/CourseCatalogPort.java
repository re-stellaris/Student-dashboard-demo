package com.ruoyi.education.planning.spi;

import java.util.Collection;
import java.util.List;
import com.ruoyi.education.planning.rule.PlanCourse;
import com.ruoyi.education.planning.rule.PrereqEdge;

/**
 * 课程目录只读端口：提供规划规则所需的课程快照与先修边。
 *
 * <p>对应课程域的能力：
 * {@code com.ruoyi.education.course.mapper.EduCourseMapper#selectCourseByIds}、
 * {@code #selectAllPrereq}。默认实现见 {@code DefaultPlanningReadPorts}。</p>
 *
 * @author ruoyi
 */
public interface CourseCatalogPort
{
    /**
     * 批量读取课程快照。
     *
     * @param courseIds 课程ID集合；{@code null} 或空集合表示读取全部课程
     * @return 课程快照列表（不含已删除课程）
     */
    List<PlanCourse> loadCourses(Collection<Long> courseIds);

    /**
     * 读取先修边。
     *
     * @param courseIds 目标课程ID集合；{@code null} 或空集合表示读取全部先修边
     * @return 先修边列表（自依赖行允许返回，规则层会再次过滤）
     */
    List<PrereqEdge> loadPrereqEdges(Collection<Long> courseIds);
}
