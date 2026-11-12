package com.ruoyi.education.enrollment.domain;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 学期对象 edu_term
 *
 * @author ruoyi-education
 */
public class EduTerm extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 学期ID */
    private Long termId;

    /** 学期代码，如 2026-27-1 */
    private String termCode;

    /** 学期名称 */
    private String termName;

    /** 学年 */
    private String academicYear;

    /** 学期序号（1/2/3=暑期） */
    private Integer termIndex;

    /** 开始日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date startDate;

    /** 结束日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date endDate;

    /** 是否当前学期（0否 1是） */
    private String isCurrent;

    /** 状态（0正常 1停用） */
    private String status;

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

    public Date getStartDate()
    {
        return startDate;
    }

    public void setStartDate(Date startDate)
    {
        this.startDate = startDate;
    }

    public Date getEndDate()
    {
        return endDate;
    }

    public void setEndDate(Date endDate)
    {
        this.endDate = endDate;
    }

    public String getIsCurrent()
    {
        return isCurrent;
    }

    public void setIsCurrent(String isCurrent)
    {
        this.isCurrent = isCurrent;
    }

    public String getStatus()
    {
        return status;
    }

    public void setStatus(String status)
    {
        this.status = status;
    }

    @Override
    public String toString()
    {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
            .append("termId", getTermId())
            .append("termCode", getTermCode())
            .append("termName", getTermName())
            .append("academicYear", getAcademicYear())
            .append("termIndex", getTermIndex())
            .append("startDate", getStartDate())
            .append("endDate", getEndDate())
            .append("isCurrent", getIsCurrent())
            .append("status", getStatus())
            .append("createTime", getCreateTime())
            .toString();
    }
}
