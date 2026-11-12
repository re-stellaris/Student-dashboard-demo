package com.ruoyi.education.planning.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.education.planning.domain.EduStudyPlan;

/**
 * 学习规划主表 Mapper（edu_study_plan）。
 *
 * @author ruoyi
 */
public interface EduStudyPlanMapper
{
    /**
     * 查询规划列表。
     *
     * @param plan 过滤条件（studentId / termId / status；studentId 由服务层按登录用户强制注入）
     * @return 规划列表
     */
    List<EduStudyPlan> selectPlanList(EduStudyPlan plan);

    /**
     * 按主键查询规划。
     *
     * @param planId 规划ID
     * @return 规划
     */
    EduStudyPlan selectPlanById(Long planId);

    /**
     * 按学生 + 学期查询规划（唯一键 uk_plan_student_term）。
     *
     * @param studentId 学生ID
     * @param termId 学期ID
     * @return 规划
     */
    EduStudyPlan selectPlanByStudentAndTerm(@Param("studentId") Long studentId, @Param("termId") Long termId);

    /**
     * 新增规划。
     *
     * @param plan 规划
     * @return 影响行数
     */
    int insertPlan(EduStudyPlan plan);

    /**
     * 修改规划（标题、总学分、校验结论、学分上限等）。
     *
     * @param plan 规划
     * @return 影响行数
     */
    int updatePlan(EduStudyPlan plan);

    /**
     * 只更新规划状态与校验结论。
     *
     * @param plan 规划
     * @return 影响行数
     */
    int updatePlanStatus(EduStudyPlan plan);

    /**
     * 批量删除规划（明细由外键 on delete cascade 一并删除）。
     *
     * @param planIds 规划ID数组
     * @return 影响行数
     */
    int deletePlanByIds(Long[] planIds);
}
