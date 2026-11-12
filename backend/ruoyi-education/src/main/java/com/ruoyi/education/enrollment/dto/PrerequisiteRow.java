package com.ruoyi.education.enrollment.dto;

/**
 * 先修课行（edu_course_prereq 联查 edu_course）
 *
 * <p>选课先修校验的只读行对象：同一 groupNo 内为 AND，不同 groupNo 之间为 OR。</p>
 *
 * @author ruoyi-education
 */
public class PrerequisiteRow
{
    /** 先修组号 */
    private Integer groupNo;

    /** 先修课程ID */
    private Long prereqCourseId;

    /** 先修课程代码 */
    private String prereqCode;

    /** 先修课程名称 */
    private String prereqTitle;

    public Integer getGroupNo()
    {
        return groupNo;
    }

    public void setGroupNo(Integer groupNo)
    {
        this.groupNo = groupNo;
    }

    public Long getPrereqCourseId()
    {
        return prereqCourseId;
    }

    public void setPrereqCourseId(Long prereqCourseId)
    {
        this.prereqCourseId = prereqCourseId;
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
}
