package com.ruoyi.education.advisor.rule;

import java.math.BigDecimal;

/**
 * 单条风险信号（规则命中结果）
 *
 * <p>一条信号就是一条可解释的“风险原因”：{@code code} 供前端做展示层加权映射，
 * {@code label}/{@code detail} 面向导师直接展示，{@code weight} 是后端的基础权重，
 * 统一取自 {@link AdvisorRiskRules} 的常量（默认等权 1.0，可调）。</p>
 *
 * @author ruoyi-education
 */
public final class AdvisorRiskSignal
{
    /** 风险规则编码，如 GPA_LOW */
    private final String code;

    /** 可读标题，如 “GPA 偏低” */
    private final String label;

    /** 可读明细（含具体数值，便于导师复核） */
    private final String detail;

    /** 基础权重（默认 1.0，见 {@link AdvisorRiskRules}） */
    private final BigDecimal weight;

    public AdvisorRiskSignal(String code, String label, String detail, BigDecimal weight)
    {
        this.code = code;
        this.label = label;
        this.detail = detail;
        this.weight = weight == null ? BigDecimal.ONE : weight;
    }

    public String getCode()
    {
        return code;
    }

    public String getLabel()
    {
        return label;
    }

    public String getDetail()
    {
        return detail;
    }

    public BigDecimal getWeight()
    {
        return weight;
    }
}
