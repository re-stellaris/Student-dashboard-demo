package com.ruoyi.education.planning.rule;

import java.math.BigDecimal;

/**
 * 规划规则计算的课程快照（纯数据，不依赖课程域实体与数据库）。
 *
 * <p>{@code course.domain.EduCourse} 由课程域维护，规则引擎只消费本快照；
 * 两者之间的转换在 {@code planning.spi} 的只读端口适配器中完成。</p>
 *
 * @author ruoyi
 */
public class PlanCourse
{
    /** 课程 ID */
    private Long courseId;

    /** 课程代码，如 CSC3100 */
    private String code;

    /** 课程名称（中文优先，缺失时回退英文） */
    private String title;

    /** 学分 */
    private BigDecimal units;

    /** 课程类别（Major / GenEd / English / ...） */
    private String category;

    /** 课程层级（由课程代码推算，如 1001 -> 1000） */
    private Integer level;

    public PlanCourse()
    {
    }

    public PlanCourse(Long courseId, String code, String title, BigDecimal units)
    {
        this.courseId = courseId;
        this.code = code;
        this.title = title;
        this.units = units;
    }

    public Long getCourseId()
    {
        return courseId;
    }

    public void setCourseId(Long courseId)
    {
        this.courseId = courseId;
    }

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

    public BigDecimal getUnits()
    {
        return units;
    }

    public void setUnits(BigDecimal units)
    {
        this.units = units;
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
}
