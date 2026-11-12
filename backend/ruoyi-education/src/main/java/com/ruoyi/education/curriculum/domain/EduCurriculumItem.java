package com.ruoyi.education.curriculum.domain;

import java.math.BigDecimal;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 培养方案课程要求表 edu_curriculum_item
 *
 * @author ruoyi
 */
public class EduCurriculumItem extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 主键 */
    private Long itemId;

    /** 培养方案ID */
    private Long curriculumId;

    /** 课程ID（可为空，表示纯学分池） */
    private Long courseId;

    /** 课程代码快照（课程未入库时保留） */
    private String courseCode;

    /** 要求类型：REQUIRED/ELECTIVE/GE/BREADTH/DEPTH/STREAM */
    private String requirementType;

    /** 方案中的章节名称（如 School Package） */
    private String section;

    /** 方向/流派（可选） */
    private String stream;

    /** 该条目最低学分 */
    private BigDecimal minUnits;

    /** 排序 */
    private Integer sortOrder;

    /** 课程名称（关联查询字段，非表字段） */
    private String courseTitle;

    /** 课程学分（关联查询字段，非表字段） */
    private BigDecimal courseUnits;

    public Long getItemId()
    {
        return itemId;
    }

    public void setItemId(Long itemId)
    {
        this.itemId = itemId;
    }

    public Long getCurriculumId()
    {
        return curriculumId;
    }

    public void setCurriculumId(Long curriculumId)
    {
        this.curriculumId = curriculumId;
    }

    public Long getCourseId()
    {
        return courseId;
    }

    public void setCourseId(Long courseId)
    {
        this.courseId = courseId;
    }

    @Size(min = 0, max = 32, message = "课程代码不能超过32个字符")
    public String getCourseCode()
    {
        return courseCode;
    }

    public void setCourseCode(String courseCode)
    {
        this.courseCode = courseCode;
    }

    @NotBlank(message = "要求类型不能为空")
    @Size(min = 0, max = 32, message = "要求类型不能超过32个字符")
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

    @DecimalMin(value = "0.0", message = "最低学分不能为负数")
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

    public String getCourseTitle()
    {
        return courseTitle;
    }

    public void setCourseTitle(String courseTitle)
    {
        this.courseTitle = courseTitle;
    }

    public BigDecimal getCourseUnits()
    {
        return courseUnits;
    }

    public void setCourseUnits(BigDecimal courseUnits)
    {
        this.courseUnits = courseUnits;
    }

    @Override
    public String toString()
    {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
            .append("itemId", getItemId())
            .append("curriculumId", getCurriculumId())
            .append("courseId", getCourseId())
            .append("courseCode", getCourseCode())
            .append("requirementType", getRequirementType())
            .append("section", getSection())
            .append("stream", getStream())
            .append("minUnits", getMinUnits())
            .append("sortOrder", getSortOrder())
            .append("courseTitle", getCourseTitle())
            .append("courseUnits", getCourseUnits())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .toString();
    }
}
