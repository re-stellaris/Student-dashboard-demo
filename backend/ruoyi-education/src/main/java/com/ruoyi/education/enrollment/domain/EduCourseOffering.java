package com.ruoyi.education.enrollment.domain;

import java.math.BigDecimal;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 学期开课对象 edu_course_offering
 *
 * <p>非持久化字段（列表联查回显）：courseCode、courseTitle、units、teacherName、termCode、
 * termName、courseCategory、assignmentCount。</p>
 *
 * @author ruoyi-education
 */
public class EduCourseOffering extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 开课ID */
    private Long offeringId;

    /** 课程ID */
    private Long courseId;

    /** 学期ID */
    private Long termId;

    /** 授课教师ID */
    private Long teacherId;

    /** 容量 */
    private Integer capacity;

    /** 已选人数（真实列 enrolled_count，教师视图下按 edu_enrollment 实时统计） */
    private Integer enrolledCount;

    /** 上课时间地点 */
    private String scheduleText;

    /** 状态（0开放 1关闭） */
    private String status;

    /** 课程代码（联查 edu_course） */
    private String courseCode;

    /** 课程名称（联查 edu_course.title_zh） */
    private String courseTitle;

    /** 课程学分（联查 edu_course.credits） */
    private BigDecimal units;

    /** 课程类别（联查 edu_course.category） */
    private String courseCategory;

    /** 教师姓名（联查 edu_teacher.name） */
    private String teacherName;

    /** 学期代码（联查 edu_term.term_code） */
    private String termCode;

    /** 学期名称（联查 edu_term.term_name） */
    private String termName;

    /** 作业数量（教师视图实时统计，非持久化） */
    private Integer assignmentCount;

    public Long getOfferingId()
    {
        return offeringId;
    }

    public void setOfferingId(Long offeringId)
    {
        this.offeringId = offeringId;
    }

    public Long getCourseId()
    {
        return courseId;
    }

    public void setCourseId(Long courseId)
    {
        this.courseId = courseId;
    }

    public Long getTermId()
    {
        return termId;
    }

    public void setTermId(Long termId)
    {
        this.termId = termId;
    }

    public Long getTeacherId()
    {
        return teacherId;
    }

    public void setTeacherId(Long teacherId)
    {
        this.teacherId = teacherId;
    }

    public Integer getCapacity()
    {
        return capacity;
    }

    public void setCapacity(Integer capacity)
    {
        this.capacity = capacity;
    }

    public Integer getEnrolledCount()
    {
        return enrolledCount;
    }

    public void setEnrolledCount(Integer enrolledCount)
    {
        this.enrolledCount = enrolledCount;
    }

    public String getScheduleText()
    {
        return scheduleText;
    }

    public void setScheduleText(String scheduleText)
    {
        this.scheduleText = scheduleText;
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

    public BigDecimal getUnits()
    {
        return units;
    }

    public void setUnits(BigDecimal units)
    {
        this.units = units;
    }

    public String getCourseCategory()
    {
        return courseCategory;
    }

    public void setCourseCategory(String courseCategory)
    {
        this.courseCategory = courseCategory;
    }

    public String getTeacherName()
    {
        return teacherName;
    }

    public void setTeacherName(String teacherName)
    {
        this.teacherName = teacherName;
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

    public Integer getAssignmentCount()
    {
        return assignmentCount;
    }

    public void setAssignmentCount(Integer assignmentCount)
    {
        this.assignmentCount = assignmentCount;
    }

    @Override
    public String toString()
    {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
            .append("offeringId", getOfferingId())
            .append("courseId", getCourseId())
            .append("termId", getTermId())
            .append("teacherId", getTeacherId())
            .append("capacity", getCapacity())
            .append("enrolledCount", getEnrolledCount())
            .append("scheduleText", getScheduleText())
            .append("status", getStatus())
            .append("courseCode", getCourseCode())
            .append("courseTitle", getCourseTitle())
            .append("units", getUnits())
            .append("teacherName", getTeacherName())
            .append("termCode", getTermCode())
            .append("assignmentCount", getAssignmentCount())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .toString();
    }
}
