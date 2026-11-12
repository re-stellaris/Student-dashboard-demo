package com.ruoyi.education.common.enums;

import java.util.Locale;

/**
 * 作业状态（对应 edu_assignment.status）。
 *
 * @author ruoyi
 */
public enum AssignmentStatus
{
    /** 草稿 */
    DRAFT("DRAFT", "草稿"),
    /** 已发布 */
    PUBLISHED("PUBLISHED", "已发布"),
    /** 已截止 */
    CLOSED("CLOSED", "已截止");

    private final String code;

    private final String label;

    AssignmentStatus(String code, String label)
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
     * @param code 存储值，如 PUBLISHED
     * @return 匹配的枚举；无法匹配或入参为空时返回 {@code null}
     */
    public static AssignmentStatus from(String code)
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
        for (AssignmentStatus value : values())
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
    public static AssignmentStatus from(String code, AssignmentStatus fallback)
    {
        AssignmentStatus matched = from(code);
        return matched == null ? fallback : matched;
    }
}
