package com.ruoyi.education.advisor.dto;

import java.math.BigDecimal;

/**
 * 导师看板学生读模型（edu_student + edu_major 联查结果）
 *
 * <p>只读投影，不继承 BaseEntity（不承载写操作）。</p>
 *
 * @author ruoyi-education
 */
public class AdvisorStudentRow
{
    /** 学生ID */
    private Long studentId;

    /** 学号 */
    private String studentNo;

    /** 姓名 */
    private String name;

    /** 专业ID */
    private Long majorId;

    /** 专业名称（中文优先，缺失时用英文/编码） */
    private String majorName;

    /** 专业编码 */
    private String majorCode;

    /** 入学年份 */
    private Integer entryYear;

    /** 书院/学院 */
    private String college;

    /** 累计 GPA（edu_student.cum_gpa，可能为空） */
    private BigDecimal cumGpa;

    /** 已修总学分（edu_student.total_credits，可能为空） */
    private BigDecimal totalCredits;

    public Long getStudentId()
    {
        return studentId;
    }

    public void setStudentId(Long studentId)
    {
        this.studentId = studentId;
    }

    public String getStudentNo()
    {
        return studentNo;
    }

    public void setStudentNo(String studentNo)
    {
        this.studentNo = studentNo;
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public Long getMajorId()
    {
        return majorId;
    }

    public void setMajorId(Long majorId)
    {
        this.majorId = majorId;
    }

    public String getMajorName()
    {
        return majorName;
    }

    public void setMajorName(String majorName)
    {
        this.majorName = majorName;
    }

    public String getMajorCode()
    {
        return majorCode;
    }

    public void setMajorCode(String majorCode)
    {
        this.majorCode = majorCode;
    }

    public Integer getEntryYear()
    {
        return entryYear;
    }

    public void setEntryYear(Integer entryYear)
    {
        this.entryYear = entryYear;
    }

    public String getCollege()
    {
        return college;
    }

    public void setCollege(String college)
    {
        this.college = college;
    }

    public BigDecimal getCumGpa()
    {
        return cumGpa;
    }

    public void setCumGpa(BigDecimal cumGpa)
    {
        this.cumGpa = cumGpa;
    }

    public BigDecimal getTotalCredits()
    {
        return totalCredits;
    }

    public void setTotalCredits(BigDecimal totalCredits)
    {
        this.totalCredits = totalCredits;
    }
}
