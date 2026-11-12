package com.ruoyi.education.advisor.dto;

/**
 * GPA 分布桶（GPA Performance · 分布柱状图）
 *
 * @author ruoyi-education
 */
public class AdvisorGpaBucketVO
{
    /** 桶标签，如 3.5-4.0 */
    private String label;

    /** 落入该桶的学生数 */
    private Integer count;

    public AdvisorGpaBucketVO()
    {
    }

    public AdvisorGpaBucketVO(String label, Integer count)
    {
        this.label = label;
        this.count = count;
    }

    public String getLabel()
    {
        return label;
    }

    public void setLabel(String label)
    {
        this.label = label;
    }

    public Integer getCount()
    {
        return count;
    }

    public void setCount(Integer count)
    {
        this.count = count;
    }
}
