package com.ruoyi.education.assignment.domain;

import java.math.BigDecimal;
import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 作业任务对象 edu_assignment
 *
 * <p>非持久化字段（联查回显）：courseCode、courseTitle、termCode。</p>
 *
 * @author ruoyi-education
 */
public class EduAssignment extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 作业ID */
    private Long assignmentId;

    /** 开课ID */
    private Long offeringId;

    /** 作业标题 */
    private String title;

    /** 作业说明 */
    private String description;

    /** 截止时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date dueAt;

    /** 满分 */
    private BigDecimal totalScore;

    /** 状态：DRAFT/PUBLISHED/CLOSED */
    private String status;

    /** 课程代码（联查） */
    private String courseCode;

    /** 课程名称（联查） */
    private String courseTitle;

    /** 学期代码（联查） */
    private String termCode;

    public Long getAssignmentId()
    {
        return assignmentId;
    }

    public void setAssignmentId(Long assignmentId)
    {
        this.assignmentId = assignmentId;
    }

    public Long getOfferingId()
    {
        return offeringId;
    }

    public void setOfferingId(Long offeringId)
    {
        this.offeringId = offeringId;
    }

    public String getTitle()
    {
        return title;
    }

    public void setTitle(String title)
    {
        this.title = title;
    }

    public String getDescription()
    {
        return description;
    }

    public void setDescription(String description)
    {
        this.description = description;
    }

    public Date getDueAt()
    {
        return dueAt;
    }

    public void setDueAt(Date dueAt)
    {
        this.dueAt = dueAt;
    }

    public BigDecimal getTotalScore()
    {
        return totalScore;
    }

    public void setTotalScore(BigDecimal totalScore)
    {
        this.totalScore = totalScore;
    }

    public String getStatus()
    {
        return status;
    }

    public void setStatus(String status)
    {
        this.status = status;
    }

    public String getCourseCode()
    {
        return courseCode;
    }

    public void setCourseCode(String courseCode)
    {
        this.courseCode = courseCode;
    }

    public String getCourseTitle()
    {
        return courseTitle;
    }

    public void setCourseTitle(String courseTitle)
    {
        this.courseTitle = courseTitle;
    }

    public String getTermCode()
    {
        return termCode;
    }

    public void setTermCode(String termCode)
    {
        this.termCode = termCode;
    }

    @Override
    public String toString()
    {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
            .append("assignmentId", getAssignmentId())
            .append("offeringId", getOfferingId())
            .append("title", getTitle())
            .append("description", getDescription())
            .append("dueAt", getDueAt())
            .append("totalScore", getTotalScore())
            .append("status", getStatus())
            .append("courseCode", getCourseCode())
            .append("courseTitle", getCourseTitle())
            .append("termCode", getTermCode())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .append("remark", getRemark())
            .toString();
    }
}
