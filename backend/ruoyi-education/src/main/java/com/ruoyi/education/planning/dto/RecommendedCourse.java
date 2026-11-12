package com.ruoyi.education.planning.dto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 规则推荐课程的出参（api-contract.md 4：POST /edu/plans/recommend）。
 *
 * @author ruoyi
 */
public class RecommendedCourse
{
    /** 课程 ID */
    private Long courseId;

    /** 课程代码 */
    private String code;

    /** 课程名称 */
    private String title;

    /** 学分 */
    private BigDecimal units;

    /** 推荐理由（中文可解释说明） */
    private String reason;

    /** 缺少的先修课程代码（可为空列表；非空表示“先补先修课”） */
    private List<String> missingPrereqs = new ArrayList<>();

    public RecommendedCourse()
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

    public List<String> getMissingPrereqs()
    {
        return missingPrereqs;
    }

    public void setMissingPrereqs(List<String> missingPrereqs)
    {
        this.missingPrereqs = missingPrereqs == null ? new ArrayList<>() : missingPrereqs;
    }
}
