package com.ruoyi.education.analytics.domain;

import java.math.BigDecimal;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

/**
 * 课程—技能关联度对象 edu_course_competency
 *
 * <p>本表没有 create_by / update_by 等审计列，故不继承 BaseEntity；
 * 非持久化字段（联查回显）：courseCode、courseTitle、skillCode、skillName、skillCategory。</p>
 *
 * @author ruoyi-education
 */
public class EduCourseCompetency
{
    /** 主键 */
    private Long id;

    /** 课程ID */
    private Long courseId;

    /** 技能标签ID */
    private Long skillTagId;

    /** 关联度评分（0-10） */
    private BigDecimal score;

    /** 关联等级（Very high/High/Moderate/Low） */
    private String level;

    /** 判断依据 */
    private String rationale;

    /** 命中的关键词 */
    private String matchedKeywords;

    /** 课程代码（联查 edu_course） */
    private String courseCode;

    /** 课程名称（联查 edu_course） */
    private String courseTitle;

    /** 标签编码（联查 edu_skill_tag） */
    private String skillCode;

    /** 标签名称（联查 edu_skill_tag） */
    private String skillName;

    /** 标签分类（联查 edu_skill_tag） */
    private String skillCategory;

    public Long getId()
    {
        return id;
    }

    public void setId(Long id)
    {
        this.id = id;
    }

    public Long getCourseId()
    {
        return courseId;
    }

    public void setCourseId(Long courseId)
    {
        this.courseId = courseId;
    }

    public Long getSkillTagId()
    {
        return skillTagId;
    }

    public void setSkillTagId(Long skillTagId)
    {
        this.skillTagId = skillTagId;
    }

    public BigDecimal getScore()
    {
        return score;
    }

    public void setScore(BigDecimal score)
    {
        this.score = score;
    }

    public String getLevel()
    {
        return level;
    }

    public void setLevel(String level)
    {
        this.level = level;
    }

    public String getRationale()
    {
        return rationale;
    }

    public void setRationale(String rationale)
    {
        this.rationale = rationale;
    }

    public String getMatchedKeywords()
    {
        return matchedKeywords;
    }

    public void setMatchedKeywords(String matchedKeywords)
    {
        this.matchedKeywords = matchedKeywords;
    }

    public String getCourseCode()
    {
        return courseCode;
    }

    public void setCourseCode(String courseCode)
    {
        this.courseCode = courseCode;
    }

    public String getCourseTitle()
    {
        return courseTitle;
    }

    public void setCourseTitle(String courseTitle)
    {
        this.courseTitle = courseTitle;
    }

    public String getSkillCode()
    {
        return skillCode;
    }

    public void setSkillCode(String skillCode)
    {
        this.skillCode = skillCode;
    }

    public String getSkillName()
    {
        return skillName;
    }

    public void setSkillName(String skillName)
    {
        this.skillName = skillName;
    }

    public String getSkillCategory()
    {
        return skillCategory;
    }

    public void setSkillCategory(String skillCategory)
    {
        this.skillCategory = skillCategory;
    }

    @Override
    public String toString()
    {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
            .append("id", getId())
            .append("courseId", getCourseId())
            .append("skillTagId", getSkillTagId())
            .append("score", getScore())
            .append("level", getLevel())
            .append("rationale", getRationale())
            .append("matchedKeywords", getMatchedKeywords())
            .append("courseCode", getCourseCode())
            .append("courseTitle", getCourseTitle())
            .append("skillCode", getSkillCode())
            .append("skillName", getSkillName())
            .append("skillCategory", getSkillCategory())
            .toString();
    }
}
