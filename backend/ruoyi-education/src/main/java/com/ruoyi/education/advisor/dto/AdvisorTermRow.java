package com.ruoyi.education.advisor.dto;

/**
 * 学期读模型（edu_term），用于推算“当前学年 / 最近学期”。
 *
 * @author ruoyi-education
 */
public class AdvisorTermRow
{
    /** 学期ID */
    private Long termId;

    /** 学期代码，如 2026-27-1 */
    private String termCode;

    /** 学期名称 */
    private String termName;

    /** 学年，如 2026-27 */
    private String academicYear;

    /** 学期序号（1/2/3=暑期） */
    private Integer termIndex;

    /** 是否当前学期（0否 1是） */
    private String isCurrent;

    public Long getTermId()
    {
        return termId;
    }

    public void setTermId(Long termId)
    {
        this.termId = termId;
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

    public String getAcademicYear()
    {
        return academicYear;
    }

    public void setAcademicYear(String academicYear)
    {
        this.academicYear = academicYear;
    }

    public Integer getTermIndex()
    {
        return termIndex;
    }

    public void setTermIndex(Integer termIndex)
    {
        this.termIndex = termIndex;
    }

    public String getIsCurrent()
    {
        return isCurrent;
    }

    public void setIsCurrent(String isCurrent)
    {
        this.isCurrent = isCurrent;
    }
}
