package com.ruoyi.education.enrollment.dto;

import java.math.BigDecimal;

/**
 * 学期成绩行（GPA 趋势计算的 mapper 行对象）。
 *
 * <p>归属说明：本类由 {@code EduCourseRecordMapper#selectTermGradeRows} 返回，
 * 属于 enrollment 域的读模型，因此放在 enrollment.dto，而不是 analytics.dto。
 * 这样依赖方向保持 analytics → enrollment（见 docs/architecture/module-boundaries.md），
 * 避免 enrollment.mapper 反向依赖 analytics，形成包级环。</p>
 *
 * @author ruoyi-education
 */
public class TermGradeRow
{
    /** 学期代码，如 2022-23-1 */
    private String termCode;

    /** 学期名称 */
    private String termName;

    /** 等级制成绩 */
    private String grade;

    /** 百分制成绩 */
    private BigDecimal score;

    /** 学分 */
    private BigDecimal units;

    public String getTermCode()
    {
        return termCode;
    }

    public void setTermCode(String termCode)
    {
        this.termCode = termCode;
    }

    public String getTermName()
    {
        return termName;
    }

    public void setTermName(String termName)
    {
        this.termName = termName;
    }

    public String getGrade()
    {
        return grade;
    }

    public void setGrade(String grade)
    {
        this.grade = grade;
    }

    public BigDecimal getScore()
    {
        return score;
    }

    public void setScore(BigDecimal score)
    {
        this.score = score;
    }

    public BigDecimal getUnits()
    {
        return units;
    }

    public void setUnits(BigDecimal units)
    {
        this.units = units;
    }
}
