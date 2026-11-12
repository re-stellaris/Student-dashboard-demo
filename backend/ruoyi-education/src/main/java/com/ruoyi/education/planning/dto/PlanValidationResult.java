package com.ruoyi.education.planning.dto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 规划校验结果（api-contract.md 4.1）。
 *
 * <pre>
 * {
 *   "termId": 1, "unitLimit": 21.0, "totalUnits": 18.0, "state": "WARN",
 *   "messages": ["超出学分上限 21.0：当前 24.0"],
 *   "items": [ ... ]
 * }
 * </pre>
 *
 * @author ruoyi
 */
public class PlanValidationResult
{
    /** 目标学期 */
    private Long termId;

    /** 学分上限 */
    private BigDecimal unitLimit;

    /** 计划总学分：本次规划全部课程学分之和（含需要学生调整的课程） */
    private BigDecimal totalUnits;

    /** 整体结论：PASS / WARN / FAIL */
    private String state;

    /** 计划级提示（超出学分上限、先修环等） */
    private List<String> messages = new ArrayList<>();

    /** 逐门课程校验结果 */
    private List<PlanItemValidation> items = new ArrayList<>();

    public PlanValidationResult()
    {
    }

    public Long getTermId()
    {
        return termId;
    }

    public void setTermId(Long termId)
    {
        this.termId = termId;
    }

    public BigDecimal getUnitLimit()
    {
        return unitLimit;
    }

    public void setUnitLimit(BigDecimal unitLimit)
    {
        this.unitLimit = unitLimit;
    }

    public BigDecimal getTotalUnits()
    {
        return totalUnits;
    }

    public void setTotalUnits(BigDecimal totalUnits)
    {
        this.totalUnits = totalUnits;
    }

    public String getState()
    {
        return state;
    }

    public void setState(String state)
    {
        this.state = state;
    }

    public List<String> getMessages()
    {
        return messages;
    }

    public void setMessages(List<String> messages)
    {
        this.messages = messages == null ? new ArrayList<>() : messages;
    }

    public List<PlanItemValidation> getItems()
    {
        return items;
    }

    public void setItems(List<PlanItemValidation> items)
    {
        this.items = items == null ? new ArrayList<>() : items;
    }

    @Override
    public String toString()
    {
        return "PlanValidationResult{termId=" + termId + ", unitLimit=" + unitLimit + ", totalUnits=" + totalUnits
                + ", state=" + state + ", messages=" + messages + ", items=" + items + "}";
    }
}
