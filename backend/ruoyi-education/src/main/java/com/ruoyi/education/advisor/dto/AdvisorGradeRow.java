package com.ruoyi.education.advisor.dto;

import java.math.BigDecimal;

/**
 * 导师看板成绩读模型（edu_course_record + edu_course + edu_term 联查结果）
 *
 * <p>一次取回全体（或单个）学生的成绩行，在服务层按学期聚合 GPA、统计不及格课程、
 * 计算学期覆盖情况，避免 N+1 查询。</p>
 *
 * @author ruoyi-education
 */
public class AdvisorGradeRow
{
    /** 学生ID */
    private Long studentId;

    /** 课程ID（可能为空） */
    private Long courseId;

    /** 课程代码快照 */
    private String courseCode;

    /** 课程名称（快照为空时用课程目录回填） */
    private String courseTitle;

    /** 等级制成绩 */
    private String grade;

    /** 百分制成绩 */
    private BigDecimal score;

    /** 学分（记录为 0 时用课程目录学分回填） */
    private BigDecimal units;

    /** 成绩类别（Major/GenEd/English/Chinese/IT/PE） */
    private String category;

    /** 是否通过（0否 1是） */
    private String passed;

    /** 记录状态：COMPLETED/IN_PROGRESS/WITHDRAWN */
    private String recordStatus;

    /** 学期代码，如 2022-23-1 */
    private String termCode;

    /** 学期名称 */
    private String termName;

    /** 学年，如 2022-23 */
    private String academicYear;

    /** 学期序号（1/2/3=暑期） */
    private Integer termIndex;

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
}
