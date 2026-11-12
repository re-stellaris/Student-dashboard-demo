package com.ruoyi.education.advisor.dto;

import java.math.BigDecimal;

/**
 * 分类学分汇总行（Mapper 聚合结果）
 *
 * @author ruoyi-education
 */
public class AdvisorCategoryCreditRow
{
    /** 成绩类别 */
    private String category;

    /** 该分类已获得学分 */
    private BigDecimal credits;

    /** 该分类记录数 */
    private Integer courseCount;

    public String getCategory()
    {
        return category;
    }

    public void setCategory(String category)
    {
        this.category = category;
    }

    public BigDecimal getCredits()
    {
        return credits;
    }

    public void setCredits(BigDecimal credits)
    {
        this.credits = credits;
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
