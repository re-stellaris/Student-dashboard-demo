package com.ruoyi.education.enrollment.domain;

import java.math.BigDecimal;
import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 选课关系对象 edu_enrollment
 *
 * <p>非持久化字段（列表联查回显）：courseCode、courseTitle、units、termCode、termName、
 * studentNo、studentName。</p>
 *
 * @author ruoyi-education
 */
public class EduEnrollment extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 选课ID */
    private Long enrollmentId;

    /** 学生ID */
    private Long studentId;

    /** 开课ID */
    private Long offeringId;

    /** 状态：ENROLLED/DROPPED/COMPLETED */
    private String status;

    /** 选课时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date enrolledAt;

    /** 课程代码（联查） */
    private String courseCode;

    /** 课程名称（联查） */
    private String courseTitle;

    /** 课程学分（联查） */
    private BigDecimal units;

    /** 学期代码（联查） */
    private String termCode;

    /** 学期名称（联查） */
    private String termName;

    /** 学号（联查） */
    private String studentNo;

    /** 学生姓名（联查） */
    private String studentName;

    public Long getEnrollmentId()
    {
        return enrollmentId;
    }

    public void setEnrollmentId(Long enrollmentId)
    {
        this.enrollmentId = enrollmentId;
    }

    public Long getStudentId()
    {
        return studentId;
    }

    public void setStudentId(Long studentId)
    {
        this.studentId = studentId;
    }

    public Long getOfferingId()
    {
        return offeringId;
    }

    public void setOfferingId(Long offeringId)
    {
        this.offeringId = offeringId;
    }

    public String getStatus()
    {
        return status;
    }

    public void setStatus(String status)
    {
        this.status = status;
    }

    public Date getEnrolledAt()
    {
        return enrolledAt;
    }

    public void setEnrolledAt(Date enrolledAt)
    {
        this.enrolledAt = enrolledAt;
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

    public BigDecimal getUnits()
    {
        return units;
    }

    public void setUnits(BigDecimal units)
    {
        this.units = units;
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

    public String getStudentNo()
    {
        return studentNo;
    }

    public void setStudentNo(String studentNo)
    {
        this.studentNo = studentNo;
    }

    public String getStudentName()
    {
        return studentName;
    }

    public void setStudentName(String studentName)
    {
        this.studentName = studentName;
    }

    @Override
    public String toString()
    {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
            .append("enrollmentId", getEnrollmentId())
            .append("studentId", getStudentId())
            .append("offeringId", getOfferingId())
            .append("status", getStatus())
            .append("enrolledAt", getEnrolledAt())
            .append("courseCode", getCourseCode())
            .append("courseTitle", getCourseTitle())
            .append("units", getUnits())
            .append("termCode", getTermCode())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .toString();
    }
}
