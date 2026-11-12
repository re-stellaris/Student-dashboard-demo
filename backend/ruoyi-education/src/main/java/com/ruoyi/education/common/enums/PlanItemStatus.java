package com.ruoyi.education.common.enums;

import java.util.Locale;

/**
 * 学习规划明细的校验状态（对应 edu_study_plan_item.validation_status）。
 *
 * @author ruoyi
 */
public enum PlanItemStatus
{
    /** 校验通过 */
    VALID("VALID", "通过"),
    /** 校验存在风险，但可提交（如目标学期未开课、时间冲突） */
    WARN("WARN", "警告"),
    /** 校验不通过（缺少先修课、已修读等） */
    INVALID("INVALID", "不通过"),
    /** 尚未校验 */
    UNKNOWN("UNKNOWN", "未知");

    private final String code;

    private final String label;

    PlanItemStatus(String code, String label)
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
     * @param code 存储值，如 VALID
     * @return 匹配的枚举；无法匹配或入参为空时返回 {@code null}
     */
    public static PlanItemStatus from(String code)
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
        for (PlanItemStatus value : values())
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
    public static PlanItemStatus from(String code, PlanItemStatus fallback)
    {
        PlanItemStatus matched = from(code);
        return matched == null ? fallback : matched;
    }
}
