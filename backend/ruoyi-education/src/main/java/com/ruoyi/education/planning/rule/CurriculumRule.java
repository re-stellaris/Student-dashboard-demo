package com.ruoyi.education.planning.rule;

import java.math.BigDecimal;

/**
 * 一条培养方案要求（对应 edu_curriculum_item 的一行，纯数据快照）。
 *
 * @author ruoyi
 */
public class CurriculumRule
{
    /** 主键 */
    private Long itemId;

    /** 课程 ID（可为空：纯学分池条目） */
    private Long courseId;

    /** 课程代码快照（课程未入库时保留） */
    private String courseCode;

    /** 要求类型：REQUIRED / ELECTIVE / GE / BREADTH / DEPTH / STREAM */
    private String requirementType;

    /** 方案章节名称，如 School Package */
    private String section;

    /** 方向 / 流派 */
    private String stream;

    /** 该条目最低学分 */
    private BigDecimal minUnits;

    /** 排序 */
    private Integer sortOrder;

    public CurriculumRule()
    {
    }

    public Long getItemId()
    {
        return itemId;
    }

    public void setItemId(Long itemId)
    {
        this.itemId = itemId;
    }

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

    public String getRequirementType()
    {
        return requirementType;
    }

    public void setRequirementType(String requirementType)
    {
        this.requirementType = requirementType;
    }

    public String getSection()
    {
        return section;
    }

    public void setSection(String section)
    {
        this.section = section;
    }

    public String getStream()
    {
        return stream;
    }

    public void setStream(String stream)
    {
        this.stream = stream;
    }

    public BigDecimal getMinUnits()
    {
        return minUnits;
    }

    public void setMinUnits(BigDecimal minUnits)
    {
        this.minUnits = minUnits;
    }

    public Integer getSortOrder()
    {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder)
    {
        this.sortOrder = sortOrder;
    }
}
