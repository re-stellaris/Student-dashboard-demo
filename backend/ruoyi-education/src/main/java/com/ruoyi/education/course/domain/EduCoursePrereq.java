package com.ruoyi.education.course.domain;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 课程先修关系表 edu_course_prereq
 *
 * <p>语义：同一 group_no 内为 AND（组内全部满足），不同 group_no 之间为 OR（任一组合满足）。
 * 例：(ACT3111 &amp; ACT3112) or ACT3011 -&gt; group_no=1{ACT3111,ACT3112}、group_no=2{ACT3011}。</p>
 *
 * @author ruoyi
 */
public class EduCoursePrereq extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 主键 */
    private Long id;

    /** 课程ID */
    private Long courseId;

    /** 先修课程ID */
    private Long prereqCourseId;

    /** 先修组号：组内 AND，组间 OR */
    private Integer groupNo;

    /** SIS 原始先修文本片段（可追溯） */
    private String rawText;

    /** 当前课程代码（关联查询字段，非表字段） */
    private String courseCode;

    /** 先修课程代码（关联查询字段，非表字段） */
    private String prereqCode;

    /** 先修课程名称（关联查询字段，非表字段） */
    private String prereqTitle;

    /** 当前学生是否已通过该先修课程（按当前登录学生计算，非表字段） */
    private Boolean satisfied;

    public Long getId()
    {
        return id;
    }

    public void setId(Long id)
    {
        this.id = id;
    }

    public Long getCourseId()
    {
        return courseId;
    }

    public void setCourseId(Long courseId)
    {
        this.courseId = courseId;
    }

    public Long getPrereqCourseId()
    {
        return prereqCourseId;
    }

    public void setPrereqCourseId(Long prereqCourseId)
    {
        this.prereqCourseId = prereqCourseId;
    }

    public Integer getGroupNo()
    {
        return groupNo;
    }

    public void setGroupNo(Integer groupNo)
    {
        this.groupNo = groupNo;
    }

    public String getRawText()
    {
        return rawText;
    }

    public void setRawText(String rawText)
    {
        this.rawText = rawText;
    }

    public String getCourseCode()
    {
        return courseCode;
    }

    public void setCourseCode(String courseCode)
    {
        this.courseCode = courseCode;
    }

    public String getPrereqCode()
    {
        return prereqCode;
    }

    public void setPrereqCode(String prereqCode)
    {
        this.prereqCode = prereqCode;
    }

    public String getPrereqTitle()
    {
        return prereqTitle;
    }

    public void setPrereqTitle(String prereqTitle)
    {
        this.prereqTitle = prereqTitle;
    }

    public Boolean getSatisfied()
    {
        return satisfied;
    }

    public void setSatisfied(Boolean satisfied)
    {
        this.satisfied = satisfied;
    }

    @Override
    public String toString()
    {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
            .append("id", getId())
            .append("courseId", getCourseId())
            .append("prereqCourseId", getPrereqCourseId())
            .append("groupNo", getGroupNo())
            .append("rawText", getRawText())
            .append("courseCode", getCourseCode())
            .append("prereqCode", getPrereqCode())
            .append("prereqTitle", getPrereqTitle())
            .append("satisfied", getSatisfied())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .toString();
    }
}
