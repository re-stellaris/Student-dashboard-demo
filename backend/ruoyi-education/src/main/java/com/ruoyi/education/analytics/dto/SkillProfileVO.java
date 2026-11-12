package com.ruoyi.education.analytics.dto;

import java.math.BigDecimal;

/**
 * 技能画像条目（对外 JSON）
 *
 * <p>对应 api-contract.md 第 6 节 /edu/analytics/skill-profile：
 * {@code [{"skillTagId":..,"name":"..","category":"..","score":8.4,"courseCount":3,"rationale":".."}]}。</p>
 *
 * @author ruoyi-education
 */
public class SkillProfileVO
{
    /** 技能标签ID */
    private Long skillTagId;

    /** 技能标签名称 */
    private String name;

    /** 技能分类 */
    private String category;

    /** 聚合关联度（0-10，按贡献课程取平均） */
    private BigDecimal score;

    /** 贡献课程数量 */
    private Integer courseCount;

    /** 可解释依据（基于哪些课程聚合而来） */
    private String rationale;

    public Long getSkillTagId()
    {
        return skillTagId;
    }

    public void setSkillTagId(Long skillTagId)
    {
        this.skillTagId = skillTagId;
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

    public BigDecimal getScore()
    {
        return score;
    }

    public void setScore(BigDecimal score)
    {
        this.score = score;
    }

    public Integer getCourseCount()
    {
        return courseCount;
    }

    public void setCourseCount(Integer courseCount)
    {
        this.courseCount = courseCount;
    }

    public String getRationale()
    {
        return rationale;
    }

    public void setRationale(String rationale)
    {
        this.rationale = rationale;
    }
}
