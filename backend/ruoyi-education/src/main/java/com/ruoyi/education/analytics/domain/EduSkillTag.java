package com.ruoyi.education.analytics.domain;

import java.math.BigDecimal;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 公共技能标签对象 edu_skill_tag
 *
 * <p>非持久化字段（学院能力池聚合回显）：courseCount、avgScore。</p>
 *
 * @author ruoyi-education
 */
public class EduSkillTag extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 技能标签ID */
    private Long skillTagId;

    /** 标签编码（唯一） */
    private String code;

    /** 标签名称 */
    private String name;

    /** 分类（学院/能力域） */
    private String category;

    /** 所属学院缩写 */
    private String schoolAbbr;

    /** 定义说明 */
    private String description;

    /** 匹配关键词 */
    private String keywords;

    /** 排序 */
    private Integer sortOrder;

    /** 状态（0正常 1停用） */
    private String status;

    /** 关联课程数（学院能力池聚合） */
    private Integer courseCount;

    /** 平均关联度（学院能力池聚合） */
    private BigDecimal avgScore;

    public Long getSkillTagId()
    {
        return skillTagId;
    }

    public void setSkillTagId(Long skillTagId)
    {
        this.skillTagId = skillTagId;
    }

    public String getCode()
    {
        return code;
    }

    public void setCode(String code)
    {
        this.code = code;
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public String getCategory()
    {
        return category;
    }

    public void setCategory(String category)
    {
        this.category = category;
    }

    public String getSchoolAbbr()
    {
        return schoolAbbr;
    }

    public void setSchoolAbbr(String schoolAbbr)
    {
        this.schoolAbbr = schoolAbbr;
    }

    public String getDescription()
    {
        return description;
    }

    public void setDescription(String description)
    {
        this.description = description;
    }

    public String getKeywords()
    {
        return keywords;
    }

    public void setKeywords(String keywords)
    {
        this.keywords = keywords;
    }

    public Integer getSortOrder()
    {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder)
    {
        this.sortOrder = sortOrder;
    }

    public String getStatus()
    {
        return status;
    }

    public void setStatus(String status)
    {
        this.status = status;
    }

    public Integer getCourseCount()
    {
        return courseCount;
    }

    public void setCourseCount(Integer courseCount)
    {
        this.courseCount = courseCount;
    }

    public BigDecimal getAvgScore()
    {
        return avgScore;
    }

    public void setAvgScore(BigDecimal avgScore)
    {
        this.avgScore = avgScore;
    }

    @Override
    public String toString()
    {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
            .append("skillTagId", getSkillTagId())
            .append("code", getCode())
            .append("name", getName())
            .append("category", getCategory())
            .append("schoolAbbr", getSchoolAbbr())
            .append("description", getDescription())
            .append("keywords", getKeywords())
            .append("sortOrder", getSortOrder())
            .append("status", getStatus())
            .append("courseCount", getCourseCount())
            .append("avgScore", getAvgScore())
            .append("createTime", getCreateTime())
            .toString();
    }
}
