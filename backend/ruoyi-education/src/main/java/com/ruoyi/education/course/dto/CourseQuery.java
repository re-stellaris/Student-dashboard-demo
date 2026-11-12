package com.ruoyi.education.course.dto;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

/**
 * 课程列表查询条件
 *
 * <p>对应接口 {@code GET /edu/courses/list} 的查询参数：
 * {@code code}、{@code title}、{@code school}、{@code category}、{@code level}、{@code hasOutline}。</p>
 *
 * @author ruoyi
 */
public class CourseQuery
{
    /** 课程代码（模糊匹配） */
    private String code;

    /** 课程名称（同时匹配中文名与英文名，模糊匹配） */
    private String title;

    /** 开课学院（精确匹配） */
    private String school;

    /** 课程类别（精确匹配） */
    private String category;

    /** 课程层级（精确匹配） */
    private Integer level;

    /** 是否只筛选已保存课程大纲的课程（true：仅 saved_outline） */
    private Boolean hasOutline;

    public String getCode()
    {
        return code;
    }

    public void setCode(String code)
    {
        this.code = code;
    }

    public String getTitle()
    {
        return title;
    }

    public void setTitle(String title)
    {
        this.title = title;
    }

    public String getSchool()
    {
        return school;
    }

    public void setSchool(String school)
    {
        this.school = school;
    }

    public String getCategory()
    {
        return category;
    }

    public void setCategory(String category)
    {
        this.category = category;
    }

    public Integer getLevel()
    {
        return level;
    }

    public void setLevel(Integer level)
    {
        this.level = level;
    }

    public Boolean getHasOutline()
    {
        return hasOutline;
    }

    public void setHasOutline(Boolean hasOutline)
    {
        this.hasOutline = hasOutline;
    }

    @Override
    public String toString()
    {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
            .append("code", getCode())
            .append("title", getTitle())
            .append("school", getSchool())
            .append("category", getCategory())
            .append("level", getLevel())
            .append("hasOutline", getHasOutline())
            .toString();
    }
}
