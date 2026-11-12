package com.ruoyi.education.advisor.dto;

import java.math.BigDecimal;

/**
 * 单个学生的分类学分（Student Profile · Credit Completion / 分类学分）
 *
 * @author ruoyi-education
 */
public class AdvisorCategoryCreditVO
{
    /** 成绩类别 */
    private String category;

    /** 该分类已获得学分 */
    private BigDecimal credits;

    /** 该分类已通过课程数 */
    private Integer courseCount;

    public AdvisorCategoryCreditVO()
    {
    }

    public AdvisorCategoryCreditVO(String category, BigDecimal credits, Integer courseCount)
    {
        this.category = category;
        this.credits = credits;
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
