package com.ruoyi.education.planning.rule;

/**
 * 一条先修边（对应 edu_course_prereq 的一行）：{@code courseId} 依赖 {@code prereqCourseId}。
 *
 * <p>同一 {@code groupNo} 内为 AND，不同 {@code groupNo} 之间为 OR。</p>
 *
 * @author ruoyi
 */
public class PrereqEdge
{
    /** 目标课程 ID */
    private Long courseId;

    /** 先修课程 ID */
    private Long prereqCourseId;

    /** 先修组号：组内 AND，组间 OR */
    private Integer groupNo;

    public PrereqEdge()
    {
    }

    public PrereqEdge(Long courseId, Long prereqCourseId, Integer groupNo)
    {
        this.courseId = courseId;
        this.prereqCourseId = prereqCourseId;
        this.groupNo = groupNo;
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
}
