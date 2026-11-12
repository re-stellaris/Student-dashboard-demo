package com.ruoyi.education.planning.dto;

import jakarta.validation.constraints.NotNull;

/**
 * 新建规划入参（api-contract.md 4：POST /edu/plans）。
 *
 * <pre>{"termId": 1, "title": "2026-27 秋季计划"}</pre>
 *
 * <p>注意：请求体中<b>不接受</b> studentId，学生身份由后端从登录态反查。</p>
 *
 * @author ruoyi
 */
public class PlanSaveBody
{
    /** 目标学期 ID */
    @NotNull(message = "目标学期不能为空")
    private Long termId;

    /** 规划标题（为空时使用默认标题） */
    private String title;

    public Long getTermId()
    {
        return termId;
    }

    public void setTermId(Long termId)
    {
        this.termId = termId;
    }

    public String getTitle()
    {
        return title;
    }

    public void setTitle(String title)
    {
        this.title = title;
    }
}
