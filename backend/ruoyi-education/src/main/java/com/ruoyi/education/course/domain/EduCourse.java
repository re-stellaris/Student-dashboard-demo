package com.ruoyi.education.course.domain;

import java.math.BigDecimal;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 课程目录表 edu_course
 *
 * @author ruoyi
 */
public class EduCourse extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 课程ID */
    private Long courseId;

    /** 课程代码，如 CSC1001 */
    private String code;

    /** 课程中文名 */
    private String titleZh;

    /** 课程英文名 */
    private String titleEn;

    /** 开课学科（SIS subject） */
    private String subject;

    /** 课程序号，如 1001 */
    private String courseNumber;

    /** 首字母分组（A-Z） */
    private String letter;

    /** 学分 */
    private BigDecimal credits;

    /** 课程层级（由课程代码推算，如 1001 -> 1000） */
    private Integer level;

    /** 课程类别（Major/GenEd/English/Chinese/IT/PE/Other） */
    private String category;

    /** 评分方式 */
    private String gradingBasis;

    /** 课程组成部分 */
    private String component;

    /** 校区 */
    private String campus;

    /** 开课学院 */
    private String school;

    /** 教学组织 */
    private String academicOrg;

    /** 大纲状态（saved_outline / ghost_outline_unavailable） */
    private String outlineStatus;

    /** 开课学期状态 */
    private String termsStatus;

    /** 历史开课学期（换行分隔） */
    private String offeredTerms;

    /** 先修课原始文本（保留 SIS 原文） */
    private String prereqText;

    /** 共修课原始文本 */
    private String coreqText;

    /** 课程简介（中文） */
    private String descriptionZh;

    /** 课程简介（英文） */
    private String descriptionEn;

    /** 状态（0正常 1停用） */
    private String status;

    /** 删除标志（0存在 2删除） */
    private String delFlag;

    public Long getCourseId()
    {
        return courseId;
    }

    public void setCourseId(Long courseId)
    {
        this.courseId = courseId;
    }

    @NotBlank(message = "课程代码不能为空")
    @Size(min = 0, max = 32, message = "课程代码不能超过32个字符")
    public String getCode()
    {
        return code;
    }

    public void setCode(String code)
    {
        this.code = code;
    }

    public String getTitleZh()
    {
        return titleZh;
    }

    public void setTitleZh(String titleZh)
    {
        this.titleZh = titleZh;
    }

    public String getTitleEn()
    {
        return titleEn;
    }

    public void setTitleEn(String titleEn)
    {
        this.titleEn = titleEn;
    }

    public String getSubject()
    {
        return subject;
    }

    public void setSubject(String subject)
    {
        this.subject = subject;
    }

    public String getCourseNumber()
    {
        return courseNumber;
    }

    public void setCourseNumber(String courseNumber)
    {
        this.courseNumber = courseNumber;
    }

    public String getLetter()
    {
        return letter;
    }

    public void setLetter(String letter)
    {
        this.letter = letter;
    }

    @DecimalMin(value = "0.00", message = "学分不能为负数")
    public BigDecimal getCredits()
    {
        return credits;
    }

    public void setCredits(BigDecimal credits)
    {
        this.credits = credits;
    }

    public Integer getLevel()
    {
        return level;
    }

    public void setLevel(Integer level)
    {
        this.level = level;
    }

    public String getCategory()
    {
        return category;
    }

    public void setCategory(String category)
    {
        this.category = category;
    }

    public String getGradingBasis()
    {
        return gradingBasis;
    }

    public void setGradingBasis(String gradingBasis)
    {
        this.gradingBasis = gradingBasis;
    }

    public String getComponent()
    {
        return component;
    }

    public void setComponent(String component)
    {
        this.component = component;
    }

    public String getCampus()
    {
        return campus;
    }

    public void setCampus(String campus)
    {
        this.campus = campus;
    }

    public String getSchool()
    {
        return school;
    }

    public void setSchool(String school)
    {
        this.school = school;
    }

    public String getAcademicOrg()
    {
        return academicOrg;
    }

    public void setAcademicOrg(String academicOrg)
    {
        this.academicOrg = academicOrg;
    }

    public String getOutlineStatus()
    {
        return outlineStatus;
    }

    public void setOutlineStatus(String outlineStatus)
    {
        this.outlineStatus = outlineStatus;
    }

    public String getTermsStatus()
    {
        return termsStatus;
    }

    public void setTermsStatus(String termsStatus)
    {
        this.termsStatus = termsStatus;
    }

    public String getOfferedTerms()
    {
        return offeredTerms;
    }

    public void setOfferedTerms(String offeredTerms)
    {
        this.offeredTerms = offeredTerms;
    }

    public String getPrereqText()
    {
        return prereqText;
    }

    public void setPrereqText(String prereqText)
    {
        this.prereqText = prereqText;
    }

    public String getCoreqText()
    {
        return coreqText;
    }

    public void setCoreqText(String coreqText)
    {
        this.coreqText = coreqText;
    }

    public String getDescriptionZh()
    {
        return descriptionZh;
    }

    public void setDescriptionZh(String descriptionZh)
    {
        this.descriptionZh = descriptionZh;
    }

    public String getDescriptionEn()
    {
        return descriptionEn;
    }

    public void setDescriptionEn(String descriptionEn)
    {
        this.descriptionEn = descriptionEn;
    }

    public String getStatus()
    {
        return status;
    }

    public void setStatus(String status)
    {
        this.status = status;
    }

    public String getDelFlag()
    {
        return delFlag;
    }

    public void setDelFlag(String delFlag)
    {
        this.delFlag = delFlag;
    }

    @Override
    public String toString()
    {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
            .append("courseId", getCourseId())
            .append("code", getCode())
            .append("titleZh", getTitleZh())
            .append("titleEn", getTitleEn())
            .append("subject", getSubject())
            .append("courseNumber", getCourseNumber())
            .append("letter", getLetter())
            .append("credits", getCredits())
            .append("level", getLevel())
            .append("category", getCategory())
            .append("gradingBasis", getGradingBasis())
            .append("component", getComponent())
            .append("campus", getCampus())
            .append("school", getSchool())
            .append("academicOrg", getAcademicOrg())
            .append("outlineStatus", getOutlineStatus())
            .append("termsStatus", getTermsStatus())
            .append("offeredTerms", getOfferedTerms())
            .append("prereqText", getPrereqText())
            .append("coreqText", getCoreqText())
            .append("descriptionZh", getDescriptionZh())
            .append("descriptionEn", getDescriptionEn())
            .append("status", getStatus())
            .append("delFlag", getDelFlag())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .append("remark", getRemark())
            .toString();
    }
}
