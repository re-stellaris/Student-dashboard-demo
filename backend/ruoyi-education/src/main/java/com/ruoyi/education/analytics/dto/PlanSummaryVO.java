package com.ruoyi.education.analytics.dto;

/**
 * 学习规划状态汇总（对外 JSON）
 *
 * <p>对应 api-contract.md 第 6 节 planSummary：
 * 至少包含 {@code {"draft":1,"submitted":0}}，此处额外给出 validated / archived / total。</p>
 *
 * @author ruoyi-education
 */
public class PlanSummaryVO
{
    /** 草稿数量 */
    private Integer draft = 0;

    /** 已校验数量 */
    private Integer validated = 0;

    /** 已提交数量 */
    private Integer submitted = 0;

    /** 已归档数量 */
    private Integer archived = 0;

    /** 规划总数 */
    private Integer total = 0;

    public Integer getDraft()
    {
        return draft;
    }

    public void setDraft(Integer draft)
    {
        this.draft = draft;
    }

    public Integer getValidated()
    {
        return validated;
    }

    public void setValidated(Integer validated)
    {
        this.validated = validated;
    }

    public Integer getSubmitted()
    {
        return submitted;
    }

    public void setSubmitted(Integer submitted)
    {
        this.submitted = submitted;
    }

    public Integer getArchived()
    {
        return archived;
    }

    public void setArchived(Integer archived)
    {
        this.archived = archived;
    }

    public Integer getTotal()
    {
        return total;
    }

    public void setTotal(Integer total)
    {
        this.total = total;
    }
}
