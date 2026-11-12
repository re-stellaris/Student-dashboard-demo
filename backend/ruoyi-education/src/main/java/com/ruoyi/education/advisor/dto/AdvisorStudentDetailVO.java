package com.ruoyi.education.advisor.dto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 学生画像（GET /edu/advisors/students/{studentId}）
 *
 * <p>在列表行字段之外补充：书院、毕业要求学分、按入学年推算的应修学分、
 * 已修课程数、分类学分、学期 GPA 趋势与风险明细。前端 Student Profile /
 * Risk Snapshot 详情面板共用这一份数据。</p>
 *
 * @author ruoyi-education
 */
public class AdvisorStudentDetailVO
{
    /** 学生ID */
    private Long studentId;

    /** 学号 */
    private String studentNo;

    /** 姓名 */
    private String name;

    /** 专业ID */
    private Long majorId;

    /** 专业名称 */
    private String majorName;

    /** 专业编码 */
    private String majorCode;

    /** 入学年份 */
    private Integer entryYear;

    /** 书院/学院 */
    private String college;

    /** 累计 GPA */
    private BigDecimal cumGpa;

    /** 已修总学分 */
    private BigDecimal totalCredits;

    /** 毕业要求学分（默认 120，培养方案补齐后可替换） */
    private BigDecimal graduationCredits;

    /** 按入学年推算的应修学分；无法推算时为 null */
    private BigDecimal expectedCredits;

    /** 学分完成度（%，封顶 100） */
    private BigDecimal creditProgressPct;

    /** 平均分（百分制成绩均值） */
    private BigDecimal avgScore;

    /** 已通过课程数 */
    private Integer completedCourseCount;

    /** 未通过课程数（passed='0' 或等级 F） */
    private Integer failedCourseCount;

    /** 本学期/最近学期的 GPA（趋势末点，便于头部展示） */
    private BigDecimal latestTermGpa;

    /** 风险等级：high / medium / low */
    private String riskLevel;

    /** 风险分（0-100） */
    private Integer riskScore;

    /** 命中的风险原因 */
    private List<AdvisorRiskReasonVO> riskReasons = new ArrayList<>();

    /** 分类学分 */
    private List<AdvisorCategoryCreditVO> categoryCredits = new ArrayList<>();

    /** 学期 GPA 趋势（按学期代码升序） */
    private List<AdvisorGpaTrendVO> gpaTrend = new ArrayList<>();

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

    public Long getMajorId()
    {
        return majorId;
    }

    public void setMajorId(Long majorId)
    {
        this.majorId = majorId;
    }

    public String getMajorName()
    {
        return majorName;
    }

    public void setMajorName(String majorName)
    {
        this.majorName = majorName;
    }

    public String getMajorCode()
    {
        return majorCode;
    }

    public void setMajorCode(String majorCode)
    {
        this.majorCode = majorCode;
    }

    public Integer getEntryYear()
    {
        return entryYear;
    }

    public void setEntryYear(Integer entryYear)
    {
        this.entryYear = entryYear;
    }

    public String getCollege()
    {
        return college;
    }

    public void setCollege(String college)
    {
        this.college = college;
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

    public BigDecimal getGraduationCredits()
    {
        return graduationCredits;
    }

    public void setGraduationCredits(BigDecimal graduationCredits)
    {
        this.graduationCredits = graduationCredits;
    }

    public BigDecimal getExpectedCredits()
    {
        return expectedCredits;
    }

    public void setExpectedCredits(BigDecimal expectedCredits)
    {
        this.expectedCredits = expectedCredits;
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

    public Integer getCompletedCourseCount()
    {
        return completedCourseCount;
    }

    public void setCompletedCourseCount(Integer completedCourseCount)
    {
        this.completedCourseCount = completedCourseCount;
    }

    public Integer getFailedCourseCount()
    {
        return failedCourseCount;
    }

    public void setFailedCourseCount(Integer failedCourseCount)
    {
        this.failedCourseCount = failedCourseCount;
    }

    public BigDecimal getLatestTermGpa()
    {
        return latestTermGpa;
    }

    public void setLatestTermGpa(BigDecimal latestTermGpa)
    {
        this.latestTermGpa = latestTermGpa;
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

    public List<AdvisorCategoryCreditVO> getCategoryCredits()
    {
        return categoryCredits;
    }

    public void setCategoryCredits(List<AdvisorCategoryCreditVO> categoryCredits)
    {
        this.categoryCredits = categoryCredits;
    }

    public List<AdvisorGpaTrendVO> getGpaTrend()
    {
        return gpaTrend;
    }

    public void setGpaTrend(List<AdvisorGpaTrendVO> gpaTrend)
    {
        this.gpaTrend = gpaTrend;
    }
}
