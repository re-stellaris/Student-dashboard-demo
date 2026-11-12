package com.ruoyi.education.advisor.dto;

import java.math.BigDecimal;

/**
 * 全院平均 GPA 趋势点（GPA Performance · 趋势折线图）
 *
 * @author ruoyi-education
 */
public class AdvisorGpaTrendVO
{
    /** 学期代码，如 2022-23-1 */
    private String termCode;

    /** 该学期全体学生的平均 GPA */
    private BigDecimal avgGpa;

    public AdvisorGpaTrendVO()
    {
    }

    public AdvisorGpaTrendVO(String termCode, BigDecimal avgGpa)
    {
        this.termCode = termCode;
        this.avgGpa = avgGpa;
    }

    public String getTermCode()
    {
        return termCode;
    }

    public void setTermCode(String termCode)
    {
        this.termCode = termCode;
    }

    public BigDecimal getAvgGpa()
    {
        return avgGpa;
    }

    public void setAvgGpa(BigDecimal avgGpa)
    {
        this.avgGpa = avgGpa;
    }
}
