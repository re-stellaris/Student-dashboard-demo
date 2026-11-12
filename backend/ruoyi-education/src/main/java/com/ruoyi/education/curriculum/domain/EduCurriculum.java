package com.ruoyi.education.curriculum.domain;

import java.math.BigDecimal;
import java.util.List;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 培养方案表 edu_curriculum
 *
 * <p>按版本保存，避免历史规则被覆盖。同一专业同一生效年份唯一
 * （{@code uk_curriculum_version (major_id, version_year)}）。</p>
 *
 * @author ruoyi
 */
public class EduCurriculum extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 培养方案ID */
    private Long curriculumId;

    /** 专业ID */
    private Long majorId;

    /** 版本标签，如 2025-26 and thereafter */
    private String versionLabel;

    /** 生效入学年份 */
    private Integer versionYear;

    /** 适用起始入学年 */
    private Integer effectiveFrom;

    /** 适用结束入学年（null 表示一直有效） */
    private Integer effectiveTo;

    /** 毕业总学分要求 */
    private BigDecimal totalCredits;

    /** 专业学分要求 */
    private BigDecimal majorCredits;

    /** 来源文件名 */
    private String sourceDoc;

    /** 状态（0启用 1停用） */
    private String status;

    /** 删除标志（0存在 2删除） */
    private String delFlag;

    /** 方案条目（关联查询字段，非表字段） */
    private List<EduCurriculumItem> items;

    public Long getCurriculumId()
    {
        return curriculumId;
    }

    public void setCurriculumId(Long curriculumId)
    {
        this.curriculumId = curriculumId;
    }

    @NotNull(message = "专业ID不能为空")
    public Long getMajorId()
    {
        return majorId;
    }

    public void setMajorId(Long majorId)
    {
        this.majorId = majorId;
    }

    @NotNull(message = "版本标签不能为空")
    @Size(min = 0, max = 64, message = "版本标签不能超过64个字符")
    public String getVersionLabel()
    {
        return versionLabel;
    }

    public void setVersionLabel(String versionLabel)
    {
        this.versionLabel = versionLabel;
    }

    @NotNull(message = "生效入学年份不能为空")
    public Integer getVersionYear()
    {
        return versionYear;
    }

    public void setVersionYear(Integer versionYear)
    {
        this.versionYear = versionYear;
    }

    public Integer getEffectiveFrom()
    {
        return effectiveFrom;
    }

    public void setEffectiveFrom(Integer effectiveFrom)
    {
        this.effectiveFrom = effectiveFrom;
    }

    public Integer getEffectiveTo()
    {
        return effectiveTo;
    }

    public void setEffectiveTo(Integer effectiveTo)
    {
        this.effectiveTo = effectiveTo;
    }

    @DecimalMin(value = "0.0", message = "毕业总学分不能为负数")
    public BigDecimal getTotalCredits()
    {
        return totalCredits;
    }

    public void setTotalCredits(BigDecimal totalCredits)
    {
        this.totalCredits = totalCredits;
    }

    @DecimalMin(value = "0.0", message = "专业学分不能为负数")
    public BigDecimal getMajorCredits()
    {
        return majorCredits;
    }

    public void setMajorCredits(BigDecimal majorCredits)
    {
        this.majorCredits = majorCredits;
    }

    public String getSourceDoc()
    {
        return sourceDoc;
    }

    public void setSourceDoc(String sourceDoc)
    {
        this.sourceDoc = sourceDoc;
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

    public List<EduCurriculumItem> getItems()
    {
        return items;
    }

    public void setItems(List<EduCurriculumItem> items)
    {
        this.items = items;
    }

    @Override
    public String toString()
    {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
            .append("curriculumId", getCurriculumId())
            .append("majorId", getMajorId())
            .append("versionLabel", getVersionLabel())
            .append("versionYear", getVersionYear())
            .append("effectiveFrom", getEffectiveFrom())
            .append("effectiveTo", getEffectiveTo())
            .append("totalCredits", getTotalCredits())
            .append("majorCredits", getMajorCredits())
            .append("sourceDoc", getSourceDoc())
            .append("status", getStatus())
            .append("delFlag", getDelFlag())
            .append("items", getItems())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .append("remark", getRemark())
            .toString();
    }
}
