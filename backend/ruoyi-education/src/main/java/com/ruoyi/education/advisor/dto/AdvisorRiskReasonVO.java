package com.ruoyi.education.advisor.dto;

import java.math.BigDecimal;
import com.ruoyi.education.advisor.rule.AdvisorRiskSignal;

/**
 * 风险原因（规则命中明细，对外 JSON）
 *
 * <p>{@code code} 固定为 GPA_LOW / CREDIT_BEHIND / FAILED_COURSE / NO_RECENT_TERM 之一，
 * 前端据此把导师输入的四个权重映射到具体规则；{@code weight} 是后端基础权重
 * （见 {@code AdvisorRiskRules}，默认 1.0，可在常量里调整）。</p>
 *
 * @author ruoyi-education
 */
public class AdvisorRiskReasonVO
{
    /** 规则编码 */
    private String code;

    /** 规则可读标题 */
    private String label;

    /** 可读明细（含具体数值） */
    private String detail;

    /** 基础权重（默认 1.0） */
    private BigDecimal weight;

    public AdvisorRiskReasonVO()
    {
    }

    public AdvisorRiskReasonVO(String code, String label, String detail, BigDecimal weight)
    {
        this.code = code;
        this.label = label;
        this.detail = detail;
        this.weight = weight;
    }

    /**
     * 由规则信号转换（保留 code/label/detail/weight 四要素）。
     *
     * @param signal 规则信号
     * @return 对外 DTO
     */
    public static AdvisorRiskReasonVO from(AdvisorRiskSignal signal)
    {
        AdvisorRiskReasonVO vo = new AdvisorRiskReasonVO();
        vo.setCode(signal.getCode());
        vo.setLabel(signal.getLabel());
        vo.setDetail(signal.getDetail());
        vo.setWeight(signal.getWeight());
        return vo;
    }

    public String getCode()
    {
        return code;
    }

    public void setCode(String code)
    {
        this.code = code;
    }

    public String getLabel()
    {
        return label;
    }

    public void setLabel(String label)
    {
        this.label = label;
    }

    public String getDetail()
    {
        return detail;
    }

    public void setDetail(String detail)
    {
        this.detail = detail;
    }

    public BigDecimal getWeight()
    {
        return weight;
    }

    public void setWeight(BigDecimal weight)
    {
        this.weight = weight;
    }
}
