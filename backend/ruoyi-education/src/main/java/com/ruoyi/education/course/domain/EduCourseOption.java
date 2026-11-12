package com.ruoyi.education.course.domain;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

/**
 * 课程筛选选项（学院 / 类别 / 层级三元组去重结果）
 *
 * <p>由 {@code EduCourseMapper#selectCourseOptions} 返回，服务层再按维度展开为
 * {@code schools}、{@code categories}、{@code levels} 三个列表。</p>
 *
 * @author ruoyi
 */
public class EduCourseOption
{
    /** 开课学院 */
    private String school;

    /** 课程类别 */
    private String category;

    /** 课程层级 */
    private Integer level;

    public EduCourseOption()
    {
    }

    public EduCourseOption(String school, String category, Integer level)
    {
        this.school = school;
        this.category = category;
        this.level = level;
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

    @Override
    public String toString()
    {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
            .append("school", getSchool())
            .append("category", getCategory())
            .append("level", getLevel())
            .toString();
    }
}
