package com.ruoyi.education.planning.service;

import java.util.List;
import com.ruoyi.education.planning.domain.EduStudyPlan;
import com.ruoyi.education.planning.dto.PlanDetailVO;
import com.ruoyi.education.planning.dto.PlanItemBody;
import com.ruoyi.education.planning.dto.PlanRecommendBody;
import com.ruoyi.education.planning.dto.PlanSaveBody;
import com.ruoyi.education.planning.dto.PlanValidateBody;
import com.ruoyi.education.planning.dto.PlanValidationResult;
import com.ruoyi.education.planning.dto.RecommendedCourse;

/**
 * 选课规划服务。
 *
 * <p>所有方法都只操作“当前登录学生”的数据：学生ID由
 * {@code SecurityUtils.getUserId()} 反查 {@code edu_student.sys_user_id} 得到，
 * <b>绝不接受</b>前端传入的 studentId（docs/architecture/backend-conventions.md 第 4 节）。</p>
 *
 * @author ruoyi
 */
public interface IEduPlanningService
{
    /**
     * 查询本人的规划列表。
     *
     * @param query 过滤条件（termId / status；studentId 由服务端强制注入）
     * @return 规划列表
     */
    List<EduStudyPlan> selectPlanList(EduStudyPlan query);

    /**
     * 查询规划详情（含明细与可解释说明）。
     *
     * @param planId 规划ID
     * @return 规划详情
     */
    PlanDetailVO selectPlanDetail(Long planId);

    /**
     * 新建规划草稿。
     *
     * @param body 入参（termId、title）
     * @return 新建的规划
     */
    EduStudyPlan createPlan(PlanSaveBody body);

    /**
     * 覆盖式保存规划课程，保存即重新校验并写入 reason / validation_status / validation_message。
     *
     * @param planId 规划ID
     * @param items 明细入参
     * @return 本次校验结果
     */
    PlanValidationResult savePlanItems(Long planId, List<PlanItemBody> items);

    /**
     * 试算校验（不落库）。
     *
     * @param body 入参（termId、courseIds）
     * @return 校验结果
     */
    PlanValidationResult validate(PlanValidateBody body);

    /**
     * 校验并持久化校验结果（含逐条明细的 reason 重算）。
     *
     * @param planId 规划ID
     * @return 校验结果
     */
    PlanValidationResult persistValidate(Long planId);

    /**
     * 规则推荐下学期可选课程。
     *
     * @param body 入参（termId、limit）
     * @return 推荐课程
     */
    List<RecommendedCourse> recommend(PlanRecommendBody body);

    /**
     * 提交规划（DRAFT/VALIDATED → SUBMITTED，提交前强制校验）。
     *
     * @param planId 规划ID
     * @return 影响行数
     */
    int submit(Long planId);

    /**
     * 批量删除本人规划。
     *
     * @param planIds 规划ID数组
     * @return 影响行数
     */
    int deletePlans(Long[] planIds);
}
