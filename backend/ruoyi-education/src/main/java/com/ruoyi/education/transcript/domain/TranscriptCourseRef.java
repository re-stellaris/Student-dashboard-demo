package com.ruoyi.education.transcript.domain;

import java.math.BigDecimal;

/**
 * 课程代码 → 课程目录快照的只读投影（edu_course）
 *
 * <p>用途有两个：</p>
 * <ol>
 *   <li>判断 {@code courseCode} 是否命中课程目录（未命中时 {@code course_id} 允许为 null）；</li>
 *   <li>当导入行缺少课程名 / 学分 / 分类时，用课程目录回填，与
 *       {@code EduCourseRecordMapper.xml} 中「快照为空时用课程目录回填」的口径保持一致。</li>
 * </ol>
 *
 * @author ruoyi-education
 */
public class TranscriptCourseRef
{
    /** 课程ID（edu_course.course_id） */
    private Long courseId;

    /** 课程代码（edu_course.code） */
    private String courseCode;

    /** 课程中文名（edu_course.title_zh） */
    private String title;

    /** 学分（edu_course.credits） */
    private BigDecimal credits;

    /** 课程类别（edu_course.category） */
    private String category;

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

    public String getTitle()
    {
        return title;
    }

    public void setTitle(String title)
    {
        this.title = title;
    }

    public BigDecimal getCredits()
    {
        return credits;
    }

    public void setCredits(BigDecimal credits)
    {
        this.credits = credits;
    }

    public String getCategory()
    {
        return category;
    }

    public void setCategory(String category)
    {
        this.category = category;
    }
}
