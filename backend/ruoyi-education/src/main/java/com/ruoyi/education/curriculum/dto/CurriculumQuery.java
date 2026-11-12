package com.ruoyi.education.curriculum.dto;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

/**
 * 培养方案列表查询条件
 *
 * <p>对应接口 {@code GET /edu/curricula/list} 的查询参数：{@code majorId}、{@code versionYear}。</p>
 *
 * @author ruoyi
 */
public class CurriculumQuery
{
    /** 专业ID */
    private Long majorId;

    /** 生效入学年份 */
    private Integer versionYear;

    public Long getMajorId()
    {
        return majorId;
    }

    public void setMajorId(Long majorId)
    {
        this.majorId = majorId;
    }

    public Integer getVersionYear()
    {
        return versionYear;
    }

    public void setVersionYear(Integer versionYear)
    {
        this.versionYear = versionYear;
    }

    @Override
    public String toString()
    {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
            .append("majorId", getMajorId())
            .append("versionYear", getVersionYear())
            .toString();
    }
}
