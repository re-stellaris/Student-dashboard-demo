package com.ruoyi.education.enrollment.domain;

import java.math.BigDecimal;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 学生学籍对象 edu_student
 *
 * <p>非持久化字段（联查回显）：majorName、majorCode、sysUserName。</p>
 *
 * @author ruoyi-education
 */
public class EduStudent extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 学生ID */
    private Long studentId;

    /** 关联的系统用户ID */
    private Long sysUserId;

    /** 学号（唯一） */
    private String studentNo;

    /** 姓名 */
    private String name;

    /** 专业ID */
    private Long majorId;

    /** 入学年份 */
    private Integer entryYear;

    /** 书院/学院 */
    private String college;

    /** 邮箱 */
    private String email;

    /** 累计 GPA */
    private BigDecimal cumGpa;

    /** 已修总学分 */
    private BigDecimal totalCredits;

    /** 状态（0正常 1停用） */
    private String status;

    /** 专业中文名（联查 edu_major） */
    private String majorName;

    /** 专业编码（联查 edu_major） */
    private String majorCode;

    /** 系统登录账号（联查 sys_user） */
    private String sysUserName;

    public Long getStudentId()
    {
        return studentId;
    }

    public void setStudentId(Long studentId)
    {
        this.studentId = studentId;
    }

    public Long getSysUserId()
    {
        return sysUserId;
    }

    public void setSysUserId(Long sysUserId)
    {
        this.sysUserId = sysUserId;
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

    public String getEmail()
    {
        return email;
    }

    public void setEmail(String email)
    {
        this.email = email;
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

    public String getStatus()
    {
        return status;
    }

    public void setStatus(String status)
    {
        this.status = status;
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

    public String getSysUserName()
    {
        return sysUserName;
    }

    public void setSysUserName(String sysUserName)
    {
        this.sysUserName = sysUserName;
    }

    @Override
    public String toString()
    {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
            .append("studentId", getStudentId())
            .append("sysUserId", getSysUserId())
            .append("studentNo", getStudentNo())
            .append("name", getName())
            .append("majorId", getMajorId())
            .append("entryYear", getEntryYear())
            .append("college", getCollege())
            .append("email", getEmail())
            .append("cumGpa", getCumGpa())
            .append("totalCredits", getTotalCredits())
            .append("status", getStatus())
            .append("majorName", getMajorName())
            .append("majorCode", getMajorCode())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .toString();
    }
}
