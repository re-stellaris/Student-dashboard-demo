package com.ruoyi.education.common.enums;

import java.util.Locale;

/**
 * 成绩记录状态（对应 edu_course_record.record_status）。
 *
 * @author ruoyi
 */
public enum RecordStatus
{
    /** 已完成（成绩已出） */
    COMPLETED("COMPLETED", "已完成"),
    /** 修读中 */
    IN_PROGRESS("IN_PROGRESS", "修读中"),
    /** 已退课 */
    WITHDRAWN("WITHDRAWN", "已退课");

    private final String code;

    private final String label;

    RecordStatus(String code, String label)
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
     * @param code 存储值，如 COMPLETED
     * @return 匹配的枚举；无法匹配或入参为空时返回 {@code null}
     */
    public static RecordStatus from(String code)
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
        for (RecordStatus value : values())
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
    public static RecordStatus from(String code, RecordStatus fallback)
    {
        RecordStatus matched = from(code);
        return matched == null ? fallback : matched;
    }
}
