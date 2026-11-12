package com.ruoyi.education.advisor.domain;

/**
 * 风险等级（导师看板域内枚举）
 *
 * <p>把 high / medium / low 三个“魔法字符串”收敛到域内枚举，供规则层与
 * VO 层共用；等级由 {@code AdvisorRiskRules.normalizeScore} 算出的 0-100 分决定
 * （≥60 high、≥30 medium、否则 low）。</p>
 *
 * @author ruoyi-education
 */
public enum AdvisorRiskLevel
{
    /** 高风险 */
    HIGH("high", "高风险"),

    /** 中风险 */
    MEDIUM("medium", "中风险"),

    /** 低风险 */
    LOW("low", "低风险");

    /** 对外编码（小写，前端按此渲染徽章） */
    private final String code;

    /** 中文名称 */
    private final String label;

    AdvisorRiskLevel(String code, String label)
    {
        this.code = code;
        this.label = label;
    }

    public String getCode()
    {
        return code;
    }

    public String getLabel()
    {
        return label;
    }

    /**
     * 按编码解析风险等级（大小写不敏感）。
     *
     * @param code high / medium / low
     * @return 风险等级；无法识别时回退 {@link #LOW}
     */
    public static AdvisorRiskLevel of(String code)
    {
        if (code != null)
        {
            for (AdvisorRiskLevel level : values())
            {
                if (level.code.equalsIgnoreCase(code.trim()))
                {
                    return level;
                }
            }
        }
        return LOW;
    }
}
