package com.ruoyi.education.enrollment.dto;

/**
 * 学习规划状态计数行（edu_study_plan 只读聚合）
 *
 * @author ruoyi-education
 */
public class PlanStatusCountRow
{
    /** 规划状态：DRAFT/VALIDATED/SUBMITTED/ARCHIVED */
    private String status;

    /** 该状态下的规划数量 */
    private Long cnt;

    public String getStatus()
    {
        return status;
    }

    public void setStatus(String status)
    {
        this.status = status;
    }

    public Long getCnt()
    {
        return cnt;
    }

    public void setCnt(Long cnt)
    {
        this.cnt = cnt;
    }
}
