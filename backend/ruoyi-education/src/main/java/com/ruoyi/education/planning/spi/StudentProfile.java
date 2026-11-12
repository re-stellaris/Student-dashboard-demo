package com.ruoyi.education.planning.spi;

/**
 * 学生学籍快照（只读端口返回值）。
 *
 * @author ruoyi
 */
public class StudentProfile
{
    /** 学生ID */
    private Long studentId;

    /** 学号 */
    private String studentNo;

    /** 姓名 */
    private String name;

    /** 专业ID */
    private Long majorId;

    /** 入学年份 */
    private Integer entryYear;

    /** 书院 / 学院 */
    private String college;

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
}
