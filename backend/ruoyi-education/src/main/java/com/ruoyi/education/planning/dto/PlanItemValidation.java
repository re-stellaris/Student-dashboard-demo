package com.ruoyi.education.planning.dto;

import java.math.BigDecimal;

/**
 * 单门课程的校验结果（对应 api-contract.md 4.1 的 items 元素）。
 *
 * @author ruoyi
 */
public class PlanItemValidation
{
    /** 课程 ID */
    private Long courseId;

    /** 课程代码 */
    private String code;

    /** 课程名称 */
    private String title;

    /** 学分 */
    private BigDecimal units;

    /** 可解释说明（中文，面向学生） */
    private String reason;

    /** 校验状态：VALID / WARN / INVALID / UNKNOWN */
    private String validationStatus;

    /** 校验信息（缺少的先修课、冲突说明等），无问题时为空串 */
    private String validationMessage;

    public PlanItemValidation()
    {
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

    @Override
    public String toString()
    {
        return "PlanItemValidation{courseId=" + courseId + ", code=" + code + ", status=" + validationStatus
                + ", reason=" + reason + ", message=" + validationMessage + "}";
    }
}
