package com.ruoyi.education.enrollment.dto;

import jakarta.validation.constraints.NotNull;

/**
 * 选课请求体（POST /edu/enrollments）
 *
 * <p>学生身份只取当前登录用户，**不接受** studentId。</p>
 *
 * @author ruoyi-education
 */
public class EnrollmentBody
{
    /** 开课ID */
    @NotNull(message = "开课ID不能为空")
    private Long offeringId;

    public Long getOfferingId()
    {
        return offeringId;
    }

    public void setOfferingId(Long offeringId)
    {
        this.offeringId = offeringId;
    }
}
