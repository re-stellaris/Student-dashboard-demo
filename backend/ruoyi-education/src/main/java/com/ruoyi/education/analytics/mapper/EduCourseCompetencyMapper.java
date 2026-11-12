package com.ruoyi.education.analytics.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.education.analytics.domain.EduCourseCompetency;
import com.ruoyi.education.analytics.domain.EduSkillTag;

/**
 * 课程—技能关联度 Mapper 接口
 *
 * @author ruoyi-education
 */
public interface EduCourseCompetencyMapper
{
    /**
     * 查询某门课程的技能关联明细
     *
     * @param courseId 课程ID
     * @return 关联明细（含技能标签信息）
     */
    List<EduCourseCompetency> selectByCourseId(Long courseId);

    /**
     * 查询某学生已通过课程覆盖到的技能关联明细（技能画像聚合来源）
     *
     * @param studentId 学生ID
     * @return 关联明细（含技能标签与课程信息）
     */
    List<EduCourseCompetency> selectByStudentPassedCourses(Long studentId);

    /**
     * 学院能力池：按学院缩写聚合技能标签的课程数与平均关联度
     *
     * @param schoolAbbr 学院缩写，可为 null
     * @param keyword 关键词（匹配标签名称/编码/关键词/说明），可为 null
     * @return 技能标签集合（含 courseCount、avgScore）
     */
    List<EduSkillTag> selectPoolBySchool(@Param("schoolAbbr") String schoolAbbr,
            @Param("keyword") String keyword);
}
