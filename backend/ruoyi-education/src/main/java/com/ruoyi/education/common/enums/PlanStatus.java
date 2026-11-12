package com.ruoyi.education.common.enums;

import java.util.Locale;

/**
 * 学习规划主表状态（对应 edu_study_plan.status）。
 *
 * @author ruoyi
 */
public enum PlanStatus
{
    /** 草稿 */
    DRAFT("DRAFT", "草稿"),
    /** 已校验通过（可提交） */
    VALIDATED("VALIDATED", "已校验"),
    /** 已提交 */
    SUBMITTED("SUBMITTED", "已提交"),
    /** 已归档 */
    ARCHIVED("ARCHIVED", "已归档");

    private final String code;

    private final String label;

    PlanStatus(String code, String label)
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
     * 按存储值解析枚举。
     *
     * @param code 存储值，如 DRAFT
     * @return 匹配的枚举；无法匹配或入参为空时返回 {@code null}
     */
    public static PlanStatus from(String code)
    {
        if (code == null)
        {
            return null;
        }
        String normalized = code.trim().toUpperCase(Locale.ROOT);
        if (normalized.isEmpty())
        {
            return null;
        }
        for (PlanStatus value : values())
        {
            if (value.code.equals(normalized) || value.name().equals(normalized))
            {
                return value;
            }
        }
        return null;
    }

    /**
     * 按存储值解析枚举，无法匹配时回退到给定默认值。
     *
     * @param code 存储值
     * @param fallback 默认值
     * @return 匹配的枚举或默认值
     */
    public static PlanStatus from(String code, PlanStatus fallback)
    {
        PlanStatus matched = from(code);
        return matched == null ? fallback : matched;
    }
}
