package com.ruoyi.education.course.dto;

import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

/**
 * 课程筛选选项出参
 *
 * <p>对应接口 {@code GET /edu/courses/options}，返回结构：
 * {@code {"schools":["..."],"categories":["..."],"levels":[1000]}}。</p>
 *
 * @author ruoyi
 */
public class CourseOptionsVO
{
    /** 开课学院列表（去重、升序） */
    private List<String> schools = new ArrayList<>();

    /** 课程类别列表（去重、升序） */
    private List<String> categories = new ArrayList<>();

    /** 课程层级列表（去重、升序） */
    private List<Integer> levels = new ArrayList<>();

    public List<String> getSchools()
    {
        return schools;
    }

    public void setSchools(List<String> schools)
    {
        this.schools = schools;
    }

    public List<String> getCategories()
    {
        return categories;
    }

    public void setCategories(List<String> categories)
    {
        this.categories = categories;
    }

    public List<Integer> getLevels()
    {
        return levels;
    }

    public void setLevels(List<Integer> levels)
    {
        this.levels = levels;
    }

    @Override
    public String toString()
    {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
            .append("schools", getSchools())
            .append("categories", getCategories())
            .append("levels", getLevels())
            .toString();
    }
}
