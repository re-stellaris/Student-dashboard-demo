package com.ruoyi.education.planning.domain;

import java.math.BigDecimal;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 学习规划明细 edu_study_plan_item。
 *
 * <p>{@code courseCode} / {@code courseTitle} / {@code units} 为联表查询得到的临时字段，
 * 不参与写入。</p>
 *
 * @author ruoyi
 */
public class EduStudyPlanItem extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 主键 */
    private Long itemId;

    /** 规划ID */
    private Long planId;

    /** 课程ID */
    private Long courseId;

    /** 推荐理由（可解释说明，面向学生） */
    private String reason;

    /** 校验状态：VALID/WARN/INVALID/UNKNOWN */
    private String validationStatus;

    /** 校验信息（缺少的先修课等） */
    private String validationMessage;

    /** 排序 */
    private Integer sortOrder;

    /** 课程代码（关联 edu_course，非表字段） */
    private String courseCode;

    /** 课程名称（关联 edu_course，非表字段） */
    private String courseTitle;

    /** 学分（关联 edu_course.credits，非表字段） */
    private BigDecimal units;

    public Long getItemId()
    {
        return itemId;
    }

    public void setItemId(Long itemId)
    {
        this.itemId = itemId;
    }

    public Long getPlanId()
    {
        return planId;
    }

    public void setPlanId(Long planId)
    {
        this.planId = planId;
    }

    public Long getCourseId()
    {
        return courseId;
    }

    public void setCourseId(Long courseId)
    {
        this.courseId = courseId;
    }

    public String getReason()
    {
        return reason;
    }

    public void setReason(String reason)
    {
        this.reason = reason;
    }

    public String getValidationStatus()
    {
        return validationStatus;
    }

    public void setValidationStatus(String validationStatus)
    {
        this.validationStatus = validationStatus;
    }

    public String getValidationMessage()
    {
        return validationMessage;
    }

    public void setValidationMessage(String validationMessage)
    {
        this.validationMessage = validationMessage;
    }

    public Integer getSortOrder()
    {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder)
    {
        this.sortOrder = sortOrder;
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

    public BigDecimal getUnits()
    {
        return units;
    }

    public void setUnits(BigDecimal units)
    {
        this.units = units;
    }
}
