package com.ruoyi.education.enrollment.dto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 学分进度（对外 JSON）
 *
 * <p>对应 api-contract.md 第 3 节 /edu/students/me/progress：
 * {@code {"curriculumTotal":..,"majorRequired":..,"earned":..,"remaining":..,
 * "categoryBreakdown":[{"category":"Major","earned":..,"required":..}]}}。</p>
 *
 * @author ruoyi-education
 */
public class CreditProgressVO
{
    /** 适用培养方案毕业总学分要求（无匹配方案时为 0） */
    private BigDecimal curriculumTotal = BigDecimal.ZERO;

    /** 适用培养方案专业学分要求（无匹配方案时为 0） */
    private BigDecimal majorRequired = BigDecimal.ZERO;

    /** 已获得学分（通过且已完成的成绩记录） */
    private BigDecimal earned = BigDecimal.ZERO;

    /** 距毕业要求剩余学分（curriculumTotal - earned，下限 0） */
    private BigDecimal remaining = BigDecimal.ZERO;

    /** 分类学分明细 */
    private List<CreditCategoryVO> categoryBreakdown = new ArrayList<>();

    public BigDecimal getCurriculumTotal()
    {
        return curriculumTotal;
    }

    public void setCurriculumTotal(BigDecimal curriculumTotal)
    {
        this.curriculumTotal = curriculumTotal;
    }

    public BigDecimal getMajorRequired()
    {
        return majorRequired;
    }

    public void setMajorRequired(BigDecimal majorRequired)
    {
        this.majorRequired = majorRequired;
    }

    public BigDecimal getEarned()
    {
        return earned;
    }

    public void setEarned(BigDecimal earned)
    {
        this.earned = earned;
    }

    public BigDecimal getRemaining()
    {
        return remaining;
    }

    public void setRemaining(BigDecimal remaining)
    {
        this.remaining = remaining;
    }

    public List<CreditCategoryVO> getCategoryBreakdown()
    {
        return categoryBreakdown;
    }

    public void setCategoryBreakdown(List<CreditCategoryVO> categoryBreakdown)
    {
        this.categoryBreakdown = categoryBreakdown;
    }
}
