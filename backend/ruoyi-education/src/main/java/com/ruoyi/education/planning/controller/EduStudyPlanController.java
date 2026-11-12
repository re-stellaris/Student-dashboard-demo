package com.ruoyi.education.planning.controller;

import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.education.planning.domain.EduStudyPlan;
import com.ruoyi.education.planning.dto.PlanItemBody;
import com.ruoyi.education.planning.dto.PlanRecommendBody;
import com.ruoyi.education.planning.dto.PlanSaveBody;
import com.ruoyi.education.planning.dto.PlanValidateBody;
import com.ruoyi.education.planning.service.IEduPlanningService;

/**
 * 选课规划接口（api-contract.md 第 4 节）。
 *
 * <p>所有接口的“本人”身份都由后端从登录态解析，请求体中的 studentId 会被忽略。</p>
 *
 * @author ruoyi
 */
@RestController
@RequestMapping("/edu/plans")
public class EduStudyPlanController extends BaseController
{
    private final IEduPlanningService planningService;

    public EduStudyPlanController(IEduPlanningService planningService)
    {
        this.planningService = planningService;
    }

    /**
     * 查询规划列表（学生仅本人）。
     */
    @PreAuthorize("@ss.hasPermi('edu:plan:list')")
    @GetMapping("/list")
    public TableDataInfo list(EduStudyPlan query)
    {
        startPage();
        List<EduStudyPlan> list = planningService.selectPlanList(query);
        return getDataTable(list);
    }

    /**
     * 查询规划详情（含明细与可解释说明）。
     */
    @PreAuthorize("@ss.hasPermi('edu:plan:list')")
    @GetMapping("/{planId}")
    public AjaxResult getInfo(@PathVariable("planId") Long planId)
    {
        return success(planningService.selectPlanDetail(planId));
    }

    /**
     * 新建规划草稿。
     */
    @PreAuthorize("@ss.hasPermi('edu:plan:add')")
    @Log(title = "学习规划", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody PlanSaveBody body)
    {
        return success(planningService.createPlan(body));
    }

    /**
     * 覆盖式保存规划课程（保存即重新校验并写入可解释说明）。
     */
    @PreAuthorize("@ss.hasPermi('edu:plan:add')")
    @Log(title = "学习规划", businessType = BusinessType.UPDATE)
    @PutMapping("/{planId}/items")
    public AjaxResult saveItems(@PathVariable("planId") Long planId, @RequestBody List<PlanItemBody> items)
    {
        return success(planningService.savePlanItems(planId, items));
    }

    /**
     * 试算校验（不落库）。
     */
    @PreAuthorize("@ss.hasPermi('edu:plan:validate')")
    @PostMapping("/validate")
    public AjaxResult validate(@Validated @RequestBody PlanValidateBody body)
    {
        return success(planningService.validate(body));
    }

    /**
     * 校验并持久化校验结果。
     */
    @PreAuthorize("@ss.hasPermi('edu:plan:validate')")
    @Log(title = "学习规划", businessType = BusinessType.UPDATE)
    @PostMapping("/{planId}/validate")
    public AjaxResult validatePlan(@PathVariable("planId") Long planId)
    {
        return success(planningService.persistValidate(planId));
    }

    /**
     * 规则推荐下学期可选课程。
     */
    @PreAuthorize("@ss.hasPermi('edu:plan:list')")
    @PostMapping("/recommend")
    public AjaxResult recommend(@Validated @RequestBody PlanRecommendBody body)
    {
        return success(planningService.recommend(body));
    }

    /**
     * 提交规划（DRAFT → SUBMITTED，提交前强制校验）。
     */
    @PreAuthorize("@ss.hasPermi('edu:plan:submit')")
    @Log(title = "学习规划", businessType = BusinessType.UPDATE)
    @PostMapping("/{planId}/submit")
    public AjaxResult submit(@PathVariable("planId") Long planId)
    {
        return toAjax(planningService.submit(planId));
    }

    /**
     * 批量删除规划。
     */
    @PreAuthorize("@ss.hasPermi('edu:plan:add')")
    @Log(title = "学习规划", businessType = BusinessType.DELETE)
    @DeleteMapping("/{planIds}")
    public AjaxResult remove(@PathVariable Long[] planIds)
    {
        return toAjax(planningService.deletePlans(planIds));
    }
}
