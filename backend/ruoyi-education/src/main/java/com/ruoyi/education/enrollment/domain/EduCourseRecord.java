package com.ruoyi.education.enrollment.domain;

import java.math.BigDecimal;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 已修课程与成绩对象 edu_course_record
 *
 * <p>非持久化字段（列表联查回显）：termCode、termName。units / courseTitle 在快照为空时
 * 由 edu_course 回填（见 EduCourseRecordMapper.xml）。</p>
 *
 * @author ruoyi-education
 */
public class EduCourseRecord extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 成绩记录ID */
    private Long recordId;

    /** 学生ID */
    private Long studentId;

    /** 课程ID（课程目录命中时写入） */
    private Long courseId;

    /** 课程代码快照 */
    private String courseCode;

    /** 课程名称快照 */
    private String courseTitle;

    /** 学期ID */
    private Long termId;

    /** 等级制成绩，如 A-/B+ */
    private String grade;

    /** 百分制成绩 */
    private BigDecimal score;

    /** 学分 */
    private BigDecimal units;

    /** 成绩类别（Major/GenEd/English/Chinese/IT/PE） */
    private String category;

    /** 是否通过（0否 1是） */
    private String passed;

    /** 状态：COMPLETED/IN_PROGRESS/WITHDRAWN */
    private String recordStatus;

    /** 数据来源：SIS_IMPORT/DEMO/MANUAL */
    private String source;

    /** 学期代码（联查 edu_term） */
    private String termCode;

    /** 学期名称（联查 edu_term） */
    private String termName;

    public Long getRecordId()
    {
        return recordId;
    }

    public void setRecordId(Long recordId)
    {
        this.recordId = recordId;
    }

    public Long getStudentId()
    {
        return studentId;
    }

    public void setStudentId(Long studentId)
    {
        this.studentId = studentId;
    }

    public Long getCourseId()
    {
        return courseId;
    }

    public void setCourseId(Long courseId)
    {
        this.courseId = courseId;
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

    public Long getTermId()
    {
        return termId;
    }

    public void setTermId(Long termId)
    {
        this.termId = termId;
    }

    public String getGrade()
    {
        return grade;
    }

    public void setGrade(String grade)
    {
        this.grade = grade;
    }

    public BigDecimal getScore()
    {
        return score;
    }

    public void setScore(BigDecimal score)
    {
        this.score = score;
    }

    public BigDecimal getUnits()
    {
        return units;
    }

    public void setUnits(BigDecimal units)
    {
        this.units = units;
    }

    public String getCategory()
    {
        return category;
    }

    public void setCategory(String category)
    {
        this.category = category;
    }

    public String getPassed()
    {
        return passed;
    }

    public void setPassed(String passed)
    {
        this.passed = passed;
    }

    public String getRecordStatus()
    {
        return recordStatus;
    }

    public void setRecordStatus(String recordStatus)
    {
        this.recordStatus = recordStatus;
    }

    public String getSource()
    {
        return source;
    }

    public void setSource(String source)
    {
        this.source = source;
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

    @Override
    public String toString()
    {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
            .append("recordId", getRecordId())
            .append("studentId", getStudentId())
            .append("courseId", getCourseId())
            .append("courseCode", getCourseCode())
            .append("courseTitle", getCourseTitle())
            .append("termId", getTermId())
            .append("grade", getGrade())
            .append("score", getScore())
            .append("units", getUnits())
            .append("category", getCategory())
            .append("passed", getPassed())
            .append("recordStatus", getRecordStatus())
            .append("source", getSource())
            .append("termCode", getTermCode())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .toString();
    }
}
