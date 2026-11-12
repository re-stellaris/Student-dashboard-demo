package com.ruoyi.education.planning.dto;

import java.util.List;
import jakarta.validation.constraints.NotNull;

/**
 * 试算校验入参（api-contract.md 4：POST /edu/plans/validate）。
 *
 * <pre>{"termId": 1, "courseIds": [100234, 100456]}</pre>
 *
 * @author ruoyi
 */
public class PlanValidateBody
{
    /** 目标学期 ID */
    @NotNull(message = "目标学期不能为空")
    private Long termId;

    /** 本次试算的课程 ID 列表（顺序即展示顺序） */
    private List<Long> courseIds;

    public Long getTermId()
    {
        return termId;
    }

    public void setTermId(Long termId)
    {
        this.termId = termId;
    }

    public List<Long> getCourseIds()
    {
        return courseIds;
    }

    public void setCourseIds(List<Long> courseIds)
    {
        this.courseIds = courseIds;
    }
}
