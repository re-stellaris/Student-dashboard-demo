package com.ruoyi.education.planning.spi;

/**
 * 学期开课的排课片段（只读端口返回值）。
 *
 * @author ruoyi
 */
public class OfferingSlot
{
    /** 课程ID */
    private Long courseId;

    /** 上课时间地点原文，如 "Mon 10:00-11:50; Wed 14:00-15:50" */
    private String scheduleText;

    public OfferingSlot()
    {
    }

    public OfferingSlot(Long courseId, String scheduleText)
    {
        this.courseId = courseId;
        this.scheduleText = scheduleText;
    }

    public Long getCourseId()
    {
        return courseId;
    }

    public void setCourseId(Long courseId)
    {
        this.courseId = courseId;
    }

    public String getScheduleText()
    {
        return scheduleText;
    }

    public void setScheduleText(String scheduleText)
    {
        this.scheduleText = scheduleText;
    }
}
