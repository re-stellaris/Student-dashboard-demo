package com.ruoyi.education.enrollment.dto;

import java.math.BigDecimal;

/**
 * 学分分类汇总行（edu_course_record / edu_curriculum_item 聚合结果）
 *
 * <p>mapper 层行对象，不直接返回前端；对外返回 {@link CreditCategoryVO}。</p>
 *
 * @author ruoyi-education
 */
public class CreditCategoryRow
{
    /** 分类编码（成绩类别或培养方案要求类型） */
    private String category;

    /** 该分类的学分合计 */
    private BigDecimal units;

    public String getCategory()
    {
        return category;
    }

    public void setCategory(String category)
    {
        this.category = category;
    }

    public BigDecimal getUnits()
    {
        return units;
    }

    public void setUnits(BigDecimal units)
    {
        this.units = units;
    }
}
