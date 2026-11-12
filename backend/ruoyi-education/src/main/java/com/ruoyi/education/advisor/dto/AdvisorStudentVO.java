package com.ruoyi.education.advisor.dto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 导师看板学生列表行（GET /edu/advisors/students）
 *
 * <p>字段与 docs/architecture/api-contract.md 第 9 节一致：
 * {@code studentId/studentNo/name/majorName/entryYear/cumGpa/totalCredits/
 * creditProgressPct/avgScore/riskLevel/riskScore/riskReasons}。</p>
 *
 * @author ruoyi-education
 */
public class AdvisorStudentVO
{
    /** 学生ID */
    private Long studentId;

    /** 学号 */
    private String studentNo;

    /** 姓名 */
    private String name;

    /** 专业名称 */
    private String majorName;

    /** 入学年份 */
    private Integer entryYear;

    /** 累计 GPA */
    private BigDecimal cumGpa;

    /** 已修总学分 */
    private BigDecimal totalCredits;

    /** 学分完成度（%，学分 / 毕业要求学分，封顶 100） */
    private BigDecimal creditProgressPct;

    /** 平均分（百分制成绩均值） */
    private BigDecimal avgScore;

    /** 风险等级：high / medium / low */
    private String riskLevel;

    /** 风险分（0-100，按 AdvisorRiskRules 归一化） */
    private Integer riskScore;

    /** 命中的风险原因（可解释） */
    private List<AdvisorRiskReasonVO> riskReasons = new ArrayList<>();

    public Long getStudentId()
    {
        return studentId;
    }

    public void setStudentId(Long studentId)
    {
        this.studentId = studentId;
    }

    public String getStudentNo()
    {
        return studentNo;
    }

    public void setStudentNo(String studentNo)
    {
        this.studentNo = studentNo;
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public String getMajorName()
    {
        return majorName;
    }

    public void setMajorName(String majorName)
    {
        this.majorName = majorName;
    }

    public Integer getEntryYear()
    {
        return entryYear;
    }

    public void setEntryYear(Integer entryYear)
    {
        this.entryYear = entryYear;
    }

    public BigDecimal getCumGpa()
    {
        return cumGpa;
    }

    public void setCumGpa(BigDecimal cumGpa)
    {
        this.cumGpa = cumGpa;
    }

    public BigDecimal getTotalCredits()
    {
        return totalCredits;
    }

    public void setTotalCredits(BigDecimal totalCredits)
    {
        this.totalCredits = totalCredits;
    }

    public BigDecimal getCreditProgressPct()
    {
        return creditProgressPct;
    }

    public void setCreditProgressPct(BigDecimal creditProgressPct)
    {
        this.creditProgressPct = creditProgressPct;
    }

    public BigDecimal getAvgScore()
    {
        return avgScore;
    }

    public void setAvgScore(BigDecimal avgScore)
    {
        this.avgScore = avgScore;
    }

    public String getRiskLevel()
    {
        return riskLevel;
    }

    public void setRiskLevel(String riskLevel)
    {
        this.riskLevel = riskLevel;
    }

    public Integer getRiskScore()
    {
        return riskScore;
    }

    public void setRiskScore(Integer riskScore)
    {
        this.riskScore = riskScore;
    }

    public List<AdvisorRiskReasonVO> getRiskReasons()
    {
        return riskReasons;
    }

    public void setRiskReasons(List<AdvisorRiskReasonVO> riskReasons)
    {
        this.riskReasons = riskReasons;
    }
}
