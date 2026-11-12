package com.ruoyi.education.advisor.dto;

/**
 * 风险等级分布（Risk Snapshot 顶部矩阵）
 *
 * @author ruoyi-education
 */
public class AdvisorRiskCountVO
{
    /** 高风险学生数 */
    private Integer high = 0;

    /** 中风险学生数 */
    private Integer medium = 0;

    /** 低风险学生数 */
    private Integer low = 0;

    public Integer getHigh()
    {
        return high;
    }

    public void setHigh(Integer high)
    {
        this.high = high;
    }

    public Integer getMedium()
    {
        return medium;
    }

    public void setMedium(Integer medium)
    {
        this.medium = medium;
    }

    public Integer getLow()
    {
        return low;
    }

    public void setLow(Integer low)
    {
        this.low = low;
    }
}
