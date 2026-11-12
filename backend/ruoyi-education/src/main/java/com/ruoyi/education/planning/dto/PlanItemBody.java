package com.ruoyi.education.planning.dto;

import jakarta.validation.constraints.NotNull;

/**
 * 规划明细入参（api-contract.md 4：PUT /edu/plans/{planId}/items，body 为 {@code [{"courseId":..}]}）。
 *
 * @author ruoyi
 */
public class PlanItemBody
{
    /** 课程 ID */
    @NotNull(message = "课程不能为空")
    private Long courseId;

    public PlanItemBody()
    {
    }

    public PlanItemBody(Long courseId)
    {
        this.courseId = courseId;
    }

    public Long getCourseId()
    {
        return courseId;
    }

    public void setCourseId(Long courseId)
    {
        this.courseId = courseId;
    }
}
