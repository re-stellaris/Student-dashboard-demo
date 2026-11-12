package com.ruoyi.education.analytics.dto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import com.ruoyi.education.enrollment.dto.CreditCategoryVO;
import com.ruoyi.education.enrollment.dto.CreditProgressVO;

/**
 * 学习仪表盘聚合（对外 JSON）
 *
 * <p>对应 api-contract.md 第 6 节 /edu/analytics/dashboard：
 * {@code {"credits":{...},"gpa":3.11,"gpaTrend":[{"termCode":"2022-23-1","gpa":3.2}],
 * "categoryBreakdown":[...],"planSummary":{"draft":1,"submitted":0},"pendingAssignments":2}}。</p>
 *
 * @author ruoyi-education
 */
public class DashboardVO
{
    /** 学分进度（结构同 /edu/students/me/progress） */
    private CreditProgressVO credits;

    /** 累计 GPA（4.0 制，取自 edu_student.cum_gpa，缺失时按成绩记录加权计算） */
    private BigDecimal gpa = BigDecimal.ZERO;

    /** 各学期 GPA 趋势 */
    private List<GpaTrendPointVO> gpaTrend = new ArrayList<>();

    /** 学分分类明细 */
    private List<CreditCategoryVO> categoryBreakdown = new ArrayList<>();

    /** 学习规划状态汇总 */
    private PlanSummaryVO planSummary = new PlanSummaryVO();

    /** 待提交作业数量 */
    private Integer pendingAssignments = 0;

    public CreditProgressVO getCredits()
    {
        return credits;
    }

    public void setCredits(CreditProgressVO credits)
    {
        this.credits = credits;
    }

    public BigDecimal getGpa()
    {
        return gpa;
    }

    public void setGpa(BigDecimal gpa)
    {
        this.gpa = gpa;
    }

    public List<GpaTrendPointVO> getGpaTrend()
    {
        return gpaTrend;
    }

    public void setGpaTrend(List<GpaTrendPointVO> gpaTrend)
    {
        this.gpaTrend = gpaTrend;
    }

    public List<CreditCategoryVO> getCategoryBreakdown()
    {
        return categoryBreakdown;
    }

    public void setCategoryBreakdown(List<CreditCategoryVO> categoryBreakdown)
    {
        this.categoryBreakdown = categoryBreakdown;
    }

    public PlanSummaryVO getPlanSummary()
    {
        return planSummary;
    }

    public void setPlanSummary(PlanSummaryVO planSummary)
    {
        this.planSummary = planSummary;
    }

    public Integer getPendingAssignments()
    {
        return pendingAssignments;
    }

    public void setPendingAssignments(Integer pendingAssignments)
    {
        this.pendingAssignments = pendingAssignments;
    }
}
