package com.ruoyi.education.advisor.dto;

/**
 * 导师看板学生列表查询条件
 *
 * <p>对应 {@code GET /edu/advisors/students} 的查询参数：入学年份、专业、风险等级、关键字。
 * {@code riskLevel} 是派生指标（由风险规则实时计算），服务层会先算出全部学生的风险等级
 * 再过滤，因此不能直接下推到 SQL。</p>
 *
 * @author ruoyi-education
 */
public class AdvisorStudentQuery
{
    /** 入学年份，如 2022；null 表示全部 */
    private Integer entryYear;

    /** 专业ID；null 表示全部 */
    private Long majorId;

    /** 风险等级：high / medium / low；null 表示全部 */
    private String riskLevel;

    /** 关键字：姓名或学号模糊匹配 */
    private String keyword;

    public Integer getEntryYear()
    {
        return entryYear;
    }

    public void setEntryYear(Integer entryYear)
    {
        this.entryYear = entryYear;
    }

    public Long getMajorId()
    {
        return majorId;
    }

    public void setMajorId(Long majorId)
    {
        this.majorId = majorId;
    }

    public String getRiskLevel()
    {
        return riskLevel;
    }

    public void setRiskLevel(String riskLevel)
    {
        this.riskLevel = riskLevel;
    }

    public String getKeyword()
    {
        return keyword;
    }

    public void setKeyword(String keyword)
    {
        this.keyword = keyword;
    }
}
