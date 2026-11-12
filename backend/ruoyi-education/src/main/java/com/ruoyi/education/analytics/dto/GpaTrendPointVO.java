package com.ruoyi.education.analytics.dto;

import java.math.BigDecimal;

/**
 * GPA 趋势点（对外 JSON）
 *
 * <p>对应 api-contract.md 第 6 节 gpaTrend 元素：
 * {@code {"termCode":"2022-23-1","gpa":3.2}}（额外附带 termName 便于展示）。</p>
 *
 * @author ruoyi-education
 */
public class GpaTrendPointVO
{
    /** 学期代码 */
    private String termCode;

    /** 学期名称 */
    private String termName;

    /** 该学期加权 GPA（4.0 制） */
    private BigDecimal gpa;

    public GpaTrendPointVO()
    {
    }

    public GpaTrendPointVO(String termCode, String termName, BigDecimal gpa)
    {
        this.termCode = termCode;
        this.termName = termName;
        this.gpa = gpa;
    }

    public String getTermCode()
    {
        return termCode;
    }

    public void setTermCode(String termCode)
    {
        this.termCode = termCode;
    }

    public String getTermName()
    {
        return termName;
    }

    public void setTermName(String termName)
    {
        this.termName = termName;
    }

    public BigDecimal getGpa()
    {
        return gpa;
    }

    public void setGpa(BigDecimal gpa)
    {
        this.gpa = gpa;
    }
}
