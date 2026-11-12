package com.ruoyi.education.common.enums;

import java.util.Locale;

/**
 * 培养方案要求类型（对应 edu_curriculum_item.requirement_type）。
 *
 * <p>取值与 docs/architecture/backend-conventions.md 第 5 节保持一致。</p>
 *
 * @author ruoyi
 */
public enum RequirementType
{
    /** 专业必修 */
    REQUIRED("REQUIRED", "专业必修"),
    /** 专业选修 */
    ELECTIVE("ELECTIVE", "专业选修"),
    /** 通识教育 */
    GE("GE", "通识教育"),
    /** 知识广度 */
    BREADTH("BREADTH", "知识广度"),
    /** 知识深度 */
    DEPTH("DEPTH", "知识深度"),
    /** 方向 / 流派课程 */
    STREAM("STREAM", "方向课程");

    /** 数据库存储值 */
    private final String code;

    /** 中文可读名称 */
    private final String label;

    RequirementType(String code, String label)
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
     * 按数据库存储值解析枚举，大小写与首尾空格不敏感。
     *
     * @param code 存储值，如 REQUIRED
     * @return 匹配的枚举；无法匹配或入参为空时返回 {@code null}
     */
    public static RequirementType from(String code)
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
        for (RequirementType value : values())
        {
            if (value.code.equals(normalized) || value.name().equals(normalized))
            {
                return value;
            }
        }
        return null;
    }

    /**
     * 按数据库存储值解析枚举，无法匹配时回退到给定默认值。
     *
     * @param code 存储值
     * @param fallback 默认值
     * @return 匹配的枚举或默认值
     */
    public static RequirementType from(String code, RequirementType fallback)
    {
        RequirementType matched = from(code);
        return matched == null ? fallback : matched;
    }
}
