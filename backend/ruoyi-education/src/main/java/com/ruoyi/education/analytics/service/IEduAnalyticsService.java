package com.ruoyi.education.analytics.service;

import java.util.List;
import com.ruoyi.education.analytics.domain.EduCourseCompetency;
import com.ruoyi.education.analytics.domain.EduSkillTag;
import com.ruoyi.education.analytics.dto.DashboardVO;
import com.ruoyi.education.analytics.dto.SkillProfileVO;

/**
 * 学情与技能画像 业务接口
 *
 * <p>所有方法都以当前登录学生（SecurityUtils.getUserId → edu_student）为数据边界。</p>
 *
 * @author ruoyi-education
 */
public interface IEduAnalyticsService
{
    /**
     * 学习仪表盘聚合：学分进度、GPA、GPA 趋势、分类明细、规划汇总、待交作业
     *
     * @return 仪表盘数据
     */
    DashboardVO dashboard();

    /**
     * 当前学生技能画像（聚合已通过课程的技能关联度）
     *
     * @return 技能画像集合
     */
    List<SkillProfileVO> selectMySkillProfile();

    /**
     * 学院能力池
     *
     * @param schoolAbbr 学院缩写，可为 null
     * @param keyword 关键词，可为 null
     * @return 技能标签集合（含课程数与平均关联度）
     */
    List<EduSkillTag> selectCompetencyPool(String schoolAbbr, String keyword);

    /**
     * 单门课程的技能关联明细
     *
     * @param courseId 课程ID
     * @return 关联明细
     */
    List<EduCourseCompetency> selectCourseCompetency(Long courseId);
}
