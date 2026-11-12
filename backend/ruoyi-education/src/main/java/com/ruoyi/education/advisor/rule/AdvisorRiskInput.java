package com.ruoyi.education.advisor.rule;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * 风险规则输入（纯内存对象，不依赖 Spring / 数据库）
 *
 * <p>由 {@code EduAdvisorServiceImpl} 从 edu_student / edu_course_record / edu_term
 * 的聚合结果装配，再交给 {@link AdvisorRiskRules#evaluate(AdvisorRiskInput)} 做纯函数判定。
 * 这样规则可以脱离容器做单元测试，也方便后续替换/扩展。</p>
 *
 * @author ruoyi-education
 */
public class AdvisorRiskInput
{
    /** 累计 GPA；null 表示暂无可信的 GPA 数据（规则跳过，不误报） */
    private BigDecimal cumGpa;

    /** 已获得学分（通过课程） */
    private BigDecimal earnedCredits = BigDecimal.ZERO;

    /** 按入学年推算的应修学分；null 或 0 表示无法推算（规则跳过） */
    private BigDecimal expectedCredits;

    /** 未通过成绩记录数（passed='0' 或等级 F） */
    private int failedCourseCount;

    /** 该学生有成绩记录的学期代码集合 */
    private Set<String> termCodesWithRecords = new LinkedHashSet<>();

    /** 全体学生最近 N 个“有数据”的学期代码（N = AdvisorRiskRules.RECENT_TERM_WINDOW） */
    private Set<String> recentTermCodes = new LinkedHashSet<>();

    public BigDecimal getCumGpa()
    {
        return cumGpa;
    }

    public void setCumGpa(BigDecimal cumGpa)
    {
        this.cumGpa = cumGpa;
    }

    public BigDecimal getEarnedCredits()
    {
        return earnedCredits;
    }

    public void setEarnedCredits(BigDecimal earnedCredits)
    {
        this.earnedCredits = earnedCredits == null ? BigDecimal.ZERO : earnedCredits;
    }

    public BigDecimal getExpectedCredits()
    {
        return expectedCredits;
    }

    public void setExpectedCredits(BigDecimal expectedCredits)
    {
        this.expectedCredits = expectedCredits;
    }

    public int getFailedCourseCount()
    {
        return failedCourseCount;
    }

    public void setFailedCourseCount(int failedCourseCount)
    {
        this.failedCourseCount = failedCourseCount;
    }

    public Set<String> getTermCodesWithRecords()
    {
        return termCodesWithRecords == null ? Collections.emptySet() : termCodesWithRecords;
    }

    public void setTermCodesWithRecords(Set<String> termCodesWithRecords)
    {
        this.termCodesWithRecords = termCodesWithRecords == null ? new LinkedHashSet<>() : termCodesWithRecords;
    }

    public Set<String> getRecentTermCodes()
    {
        return recentTermCodes == null ? Collections.emptySet() : recentTermCodes;
    }

    public void setRecentTermCodes(Set<String> recentTermCodes)
    {
        this.recentTermCodes = recentTermCodes == null ? new LinkedHashSet<>() : recentTermCodes;
    }
}
