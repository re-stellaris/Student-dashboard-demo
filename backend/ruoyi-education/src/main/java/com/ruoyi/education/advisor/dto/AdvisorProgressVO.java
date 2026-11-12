package com.ruoyi.education.advisor.dto;

/**
 * 学分进度分布（Progress Overview）
 *
 * <p>口径：已修学分 ≥ 应修学分 × 80% 记为 onTrack，低于记为 behind；
 * 缺少入学年份或学分数据无法判断时记为 unknown。</p>
 *
 * @author ruoyi-education
 */
public class AdvisorProgressVO
{
    /** 进度正常 */
    private Integer onTrack = 0;

    /** 进度滞后 */
    private Integer behind = 0;

    /** 数据不足无法判断 */
    private Integer unknown = 0;

    public Integer getOnTrack()
    {
        return onTrack;
    }

    public void setOnTrack(Integer onTrack)
    {
        this.onTrack = onTrack;
    }

    public Integer getBehind()
    {
        return behind;
    }

    public void setBehind(Integer behind)
    {
        this.behind = behind;
    }

    public Integer getUnknown()
    {
        return unknown;
    }

    public void setUnknown(Integer unknown)
    {
        this.unknown = unknown;
    }
}
