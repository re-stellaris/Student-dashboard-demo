package com.ruoyi.education.enrollment.dto;

import java.math.BigDecimal;

/**
 * 学分分类进度（对外 JSON）
 *
 * <p>字段与 api-contract.md 第 3 节 categoryBreakdown 元素一致：
 * {@code {"category":"Major","earned":..,"required":..}}。</p>
 *
 * @author ruoyi-education
 */
public class CreditCategoryVO
{
    /** 分类编码，如 Major / GenEd / English / Chinese / IT / PE / Other */
    private String category;

    /** 已获得学分 */
    private BigDecimal earned;

    /** 培养方案要求学分（无适用方案时为 0） */
    private BigDecimal required;

    public CreditCategoryVO()
    {
    }

    public CreditCategoryVO(String category, BigDecimal earned, BigDecimal required)
    {
        this.category = category;
        this.earned = earned;
        this.required = required;
    }

    public String getCategory()
    {
        return category;
    }

    public void setCategory(String category)
    {
        this.category = category;
    }

    public BigDecimal getEarned()
    {
        return earned;
    }

    public void setEarned(BigDecimal earned)
    {
        this.earned = earned;
    }

    public BigDecimal getRequired()
    {
        return required;
    }

    public void setRequired(BigDecimal required)
    {
        this.required = required;
    }
}
