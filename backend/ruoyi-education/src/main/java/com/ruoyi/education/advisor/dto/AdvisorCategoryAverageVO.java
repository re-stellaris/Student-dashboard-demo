package com.ruoyi.education.advisor.dto;

import java.math.BigDecimal;

/**
 * 分类学分均值（Progress Overview · Avg. Completion by Credit Category）
 *
 * <p>当前培养方案的 {@code total_credits} 全为 0，无法给出“分类要求学分”，
 * 因此这里返回“全体学生在该分类下的平均已修学分 + 修读课程数”，
 * 作为可视化口径；后续培养方案补全后可扩展 {@code requiredCredits} 字段。</p>
 *
 * @author ruoyi-education
 */
public class AdvisorCategoryAverageVO
{
    /** 成绩类别（Major/GenEd/English/Chinese/IT/PE） */
    private String category;

    /** 平均已修学分（总学分 / 学生数） */
    private BigDecimal avgCredits;

    /** 修读记录数（全体学生） */
    private Integer courseCount;

    public AdvisorCategoryAverageVO()
    {
    }

    public AdvisorCategoryAverageVO(String category, BigDecimal avgCredits, Integer courseCount)
    {
        this.category = category;
        this.avgCredits = avgCredits;
        this.courseCount = courseCount;
    }

    public String getCategory()
    {
        return category;
    }

    public void setCategory(String category)
    {
        this.category = category;
    }

    public BigDecimal getAvgCredits()
    {
        return avgCredits;
    }

    public void setAvgCredits(BigDecimal avgCredits)
    {
        this.avgCredits = avgCredits;
    }

    public Integer getCourseCount()
    {
        return courseCount;
    }

    public void setCourseCount(Integer courseCount)
    {
        this.courseCount = courseCount;
    }
}
