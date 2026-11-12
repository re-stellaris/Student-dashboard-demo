package com.ruoyi.education.common.enums;

import java.util.Locale;

/**
 * 选课状态（对应 edu_enrollment.status）。
 *
 * @author ruoyi
 */
public enum EnrollmentStatus
{
    /** 已选课 */
    ENROLLED("ENROLLED", "已选课"),
    /** 已退课 */
    DROPPED("DROPPED", "已退课"),
    /** 已完成 */
    COMPLETED("COMPLETED", "已完成");

    private final String code;

    private final String label;

    EnrollmentStatus(String code, String label)
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
     * @param code 存储值，如 ENROLLED
     * @return 匹配的枚举；无法匹配或入参为空时返回 {@code null}
     */
    public static EnrollmentStatus from(String code)
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
        for (EnrollmentStatus value : values())
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
    public static EnrollmentStatus from(String code, EnrollmentStatus fallback)
    {
        EnrollmentStatus matched = from(code);
        return matched == null ? fallback : matched;
    }
}
