package com.ruoyi.education.enrollment.dto;

import java.math.BigDecimal;

/**
 * 适用培养方案学分要求行（只读聚合）
 *
 * @author ruoyi-education
 */
public class CurriculumCreditRow
{
    /** 培养方案ID */
    private Long curriculumId;

    /** 版本标签 */
    private String versionLabel;

    /** 毕业总学分要求 */
    private BigDecimal totalCredits;

    /** 专业学分要求 */
    private BigDecimal majorCredits;

    public Long getCurriculumId()
    {
        return curriculumId;
    }

    public void setCurriculumId(Long curriculumId)
    {
        this.curriculumId = curriculumId;
    }

    public String getVersionLabel()
    {
        return versionLabel;
    }

    public void setVersionLabel(String versionLabel)
    {
        this.versionLabel = versionLabel;
    }

    public BigDecimal getTotalCredits()
    {
        return totalCredits;
    }

    public void setTotalCredits(BigDecimal totalCredits)
    {
        this.totalCredits = totalCredits;
    }

    public BigDecimal getMajorCredits()
    {
        return majorCredits;
    }

    public void setMajorCredits(BigDecimal majorCredits)
    {
        this.majorCredits = majorCredits;
    }
}
