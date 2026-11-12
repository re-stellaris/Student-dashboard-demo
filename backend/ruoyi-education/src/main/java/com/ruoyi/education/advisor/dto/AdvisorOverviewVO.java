package com.ruoyi.education.advisor.dto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 导师看板总览（GET /edu/advisors/overview）
 *
 * <p>对应 docs/architecture/api-contract.md 第 9 节：
 * {@code {"studentCount":8,"avgGpa":3.11,"avgCredits":126.0,
 * "gpaBuckets":[...],"gpaTrend":[...],"risk":{...},"progress":{...}}}。
 * 额外的 {@code categoryBreakdown} 用于 Progress Overview 的分类学分均值展示
 * （培养方案暂无分类要求学分，故返回平均值而非“完成率”）。</p>
 *
 * @author ruoyi-education
 */
public class AdvisorOverviewVO
{
    /** 学生总数 */
    private Integer studentCount = 0;

    /** 平均累计 GPA */
    private BigDecimal avgGpa = BigDecimal.ZERO;

    /** 平均已修学分 */
    private BigDecimal avgCredits = BigDecimal.ZERO;

    /** GPA 分布桶（5 档） */
    private List<AdvisorGpaBucketVO> gpaBuckets = new ArrayList<>();

    /** 各学期平均 GPA 趋势（按学期代码升序） */
    private List<AdvisorGpaTrendVO> gpaTrend = new ArrayList<>();

    /** 风险等级分布 */
    private AdvisorRiskCountVO risk = new AdvisorRiskCountVO();

    /** 学分进度分布 */
    private AdvisorProgressVO progress = new AdvisorProgressVO();

    /** 分类学分均值（Progress Overview 扩展字段） */
    private List<AdvisorCategoryAverageVO> categoryBreakdown = new ArrayList<>();

    public Integer getStudentCount()
    {
        return studentCount;
    }

    public void setStudentCount(Integer studentCount)
    {
        this.studentCount = studentCount;
    }

    public BigDecimal getAvgGpa()
    {
        return avgGpa;
    }

    public void setAvgGpa(BigDecimal avgGpa)
    {
        this.avgGpa = avgGpa;
    }

    public BigDecimal getAvgCredits()
    {
        return avgCredits;
    }

    public void setAvgCredits(BigDecimal avgCredits)
    {
        this.avgCredits = avgCredits;
    }

    public List<AdvisorGpaBucketVO> getGpaBuckets()
    {
        return gpaBuckets;
    }

    public void setGpaBuckets(List<AdvisorGpaBucketVO> gpaBuckets)
    {
        this.gpaBuckets = gpaBuckets;
    }

    public List<AdvisorGpaTrendVO> getGpaTrend()
    {
        return gpaTrend;
    }

    public void setGpaTrend(List<AdvisorGpaTrendVO> gpaTrend)
    {
        this.gpaTrend = gpaTrend;
    }

    public AdvisorRiskCountVO getRisk()
    {
        return risk;
    }

    public void setRisk(AdvisorRiskCountVO risk)
    {
        this.risk = risk;
    }

    public AdvisorProgressVO getProgress()
    {
        return progress;
    }

    public void setProgress(AdvisorProgressVO progress)
    {
        this.progress = progress;
    }

    public List<AdvisorCategoryAverageVO> getCategoryBreakdown()
    {
        return categoryBreakdown;
    }

    public void setCategoryBreakdown(List<AdvisorCategoryAverageVO> categoryBreakdown)
    {
        this.categoryBreakdown = categoryBreakdown;
    }
}
