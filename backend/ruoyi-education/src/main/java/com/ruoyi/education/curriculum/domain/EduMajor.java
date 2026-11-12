package com.ruoyi.education.curriculum.domain;

import java.math.BigDecimal;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 专业表 edu_major
 *
 * @author ruoyi
 */
public class EduMajor extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 专业ID */
    private Long majorId;

    /** 专业编码（唯一，建议用英文缩写） */
    private String majorCode;

    /** 专业中文名 */
    private String majorNameZh;

    /** 专业英文名 */
    private String majorNameEn;

    /** 所属学院缩写，如 SDS / SME */
    private String schoolAbbr;

    /** 所属学院名称 */
    private String schoolName;

    /** 学位类型 */
    private String degreeType;

    /** 标准学制（年） */
    private BigDecimal durationYears;

    /** 状态（0正常 1停用） */
    private String status;

    /** 删除标志（0存在 2删除） */
    private String delFlag;

    public Long getMajorId()
    {
        return majorId;
    }

    public void setMajorId(Long majorId)
    {
        this.majorId = majorId;
    }

    @NotBlank(message = "专业编码不能为空")
    @Size(min = 0, max = 64, message = "专业编码不能超过64个字符")
    public String getMajorCode()
    {
        return majorCode;
    }

    public void setMajorCode(String majorCode)
    {
        this.majorCode = majorCode;
    }

    public String getMajorNameZh()
    {
        return majorNameZh;
    }

    public void setMajorNameZh(String majorNameZh)
    {
        this.majorNameZh = majorNameZh;
    }

    public String getMajorNameEn()
    {
        return majorNameEn;
    }

    public void setMajorNameEn(String majorNameEn)
    {
        this.majorNameEn = majorNameEn;
    }

    public String getSchoolAbbr()
    {
        return schoolAbbr;
    }

    public void setSchoolAbbr(String schoolAbbr)
    {
        this.schoolAbbr = schoolAbbr;
    }

    public String getSchoolName()
    {
        return schoolName;
    }

    public void setSchoolName(String schoolName)
    {
        this.schoolName = schoolName;
    }

    public String getDegreeType()
    {
        return degreeType;
    }

    public void setDegreeType(String degreeType)
    {
        this.degreeType = degreeType;
    }

    public BigDecimal getDurationYears()
    {
        return durationYears;
    }

    public void setDurationYears(BigDecimal durationYears)
    {
        this.durationYears = durationYears;
    }

    public String getStatus()
    {
        return status;
    }

    public void setStatus(String status)
    {
        this.status = status;
    }

    public String getDelFlag()
    {
        return delFlag;
    }

    public void setDelFlag(String delFlag)
    {
        this.delFlag = delFlag;
    }

    @Override
    public String toString()
    {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
            .append("majorId", getMajorId())
            .append("majorCode", getMajorCode())
            .append("majorNameZh", getMajorNameZh())
            .append("majorNameEn", getMajorNameEn())
            .append("schoolAbbr", getSchoolAbbr())
            .append("schoolName", getSchoolName())
            .append("degreeType", getDegreeType())
            .append("durationYears", getDurationYears())
            .append("status", getStatus())
            .append("delFlag", getDelFlag())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .append("remark", getRemark())
            .toString();
    }
}
