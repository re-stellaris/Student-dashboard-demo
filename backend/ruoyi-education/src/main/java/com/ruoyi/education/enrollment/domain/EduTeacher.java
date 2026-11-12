package com.ruoyi.education.enrollment.domain;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 教师对象 edu_teacher
 *
 * @author ruoyi-education
 */
public class EduTeacher extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 教师ID */
    private Long teacherId;

    /** 关联的系统用户ID */
    private Long sysUserId;

    /** 教工号（唯一） */
    private String teacherNo;

    /** 姓名 */
    private String name;

    /** 部门ID（沿用 sys_dept） */
    private Long deptId;

    /** 所属学院 */
    private String school;

    /** 职称 */
    private String title;

    /** 状态（0正常 1停用） */
    private String status;

    public Long getTeacherId()
    {
        return teacherId;
    }

    public void setTeacherId(Long teacherId)
    {
        this.teacherId = teacherId;
    }

    public Long getSysUserId()
    {
        return sysUserId;
    }

    public void setSysUserId(Long sysUserId)
    {
        this.sysUserId = sysUserId;
    }

    public String getTeacherNo()
    {
        return teacherNo;
    }

    public void setTeacherNo(String teacherNo)
    {
        this.teacherNo = teacherNo;
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public Long getDeptId()
    {
        return deptId;
    }

    public void setDeptId(Long deptId)
    {
        this.deptId = deptId;
    }

    public String getSchool()
    {
        return school;
    }

    public void setSchool(String school)
    {
        this.school = school;
    }

    public String getTitle()
    {
        return title;
    }

    public void setTitle(String title)
    {
        this.title = title;
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
            .append("teacherId", getTeacherId())
            .append("sysUserId", getSysUserId())
            .append("teacherNo", getTeacherNo())
            .append("name", getName())
            .append("deptId", getDeptId())
            .append("school", getSchool())
            .append("title", getTitle())
            .append("status", getStatus())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .toString();
    }
}
