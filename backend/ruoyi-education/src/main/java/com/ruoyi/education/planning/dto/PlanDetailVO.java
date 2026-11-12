package com.ruoyi.education.planning.dto;

import java.util.ArrayList;
import java.util.List;
import com.ruoyi.education.planning.domain.EduStudyPlan;
import com.ruoyi.education.planning.domain.EduStudyPlanItem;

/**
 * 规划详情（api-contract.md 4：GET /edu/plans/{planId} → {"plan":{...},"items":[...]}）。
 *
 * @author ruoyi
 */
public class PlanDetailVO
{
    /** 规划主表 */
    private EduStudyPlan plan;

    /** 规划明细（含课程代码 / 名称 / 学分 / 可解释说明） */
    private List<EduStudyPlanItem> items = new ArrayList<>();

    public PlanDetailVO()
    {
    }

    public PlanDetailVO(EduStudyPlan plan, List<EduStudyPlanItem> items)
    {
        this.plan = plan;
        this.items = items == null ? new ArrayList<>() : items;
    }

    public EduStudyPlan getPlan()
    {
        return plan;
    }

    public void setPlan(EduStudyPlan plan)
    {
        this.plan = plan;
    }

    public List<EduStudyPlanItem> getItems()
    {
        return items;
    }

    public void setItems(List<EduStudyPlanItem> items)
    {
        this.items = items == null ? new ArrayList<>() : items;
    }
}
