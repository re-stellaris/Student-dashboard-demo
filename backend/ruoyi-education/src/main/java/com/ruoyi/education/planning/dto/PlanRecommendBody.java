package com.ruoyi.education.planning.dto;

import jakarta.validation.constraints.NotNull;

/**
 * 规则推荐入参（api-contract.md 4：POST /edu/plans/recommend）。
 *
 * <pre>{"termId": 1, "limit": 10}</pre>
 *
 * @author ruoyi
 */
public class PlanRecommendBody
{
    /** 目标学期 ID */
    @NotNull(message = "目标学期不能为空")
    private Long termId;

    /** 推荐条数上限（默认 10，最大 50） */
    private Integer limit;

    public Long getTermId()
    {
        return termId;
    }

    public void setTermId(Long termId)
    {
        this.termId = termId;
    }

    public Integer getLimit()
    {
        return limit;
    }

    public void setLimit(Integer limit)
    {
        this.limit = limit;
    }
}
