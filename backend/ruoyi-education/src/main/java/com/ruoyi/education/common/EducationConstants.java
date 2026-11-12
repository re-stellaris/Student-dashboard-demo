package com.ruoyi.education.common;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * 教育业务常量。
 *
 * <p>集中放置学分上限、通过成绩代码、校验状态等“魔法值”，
 * 供 course / curriculum / enrollment / planning / analytics 各域复用。</p>
 *
 * @author ruoyi
 */
public final class EducationConstants
{
    /** 默认单学期学分上限（对应 edu_study_plan.unit_limit 默认值 21.0） */
    public static final BigDecimal DEFAULT_UNIT_LIMIT = new BigDecimal("21.0");

    /** 单学期学分硬上限：超过该值视为数据异常，任何校验结果都不得为 PASS */
    public static final BigDecimal MAX_UNITS = new BigDecimal("30.0");

    /** 未指定推荐条数时的默认值 */
    public static final int DEFAULT_RECOMMEND_LIMIT = 10;

    /** 推荐接口允许的最大条数 */
    public static final int MAX_RECOMMEND_LIMIT = 50;

    /** 视为“已通过”的成绩代码（F 与空值视为未通过） */
    public static final Set<String> PASS_GRADE_CODES;

    /** 校验结果：通过 */
    public static final String VALIDATION_STATE_PASS = "PASS";

    /** 校验结果：存在风险但仍可提交 */
    public static final String VALIDATION_STATE_WARN = "WARN";

    /** 校验结果：不通过 */
    public static final String VALIDATION_STATE_FAIL = "FAIL";

    /** 删除标志：存在 */
    public static final String DEL_FLAG_NORMAL = "0";

    /** 删除标志：删除 */
    public static final String DEL_FLAG_DELETED = "2";

    /** 当前账号未绑定学生学籍 */
    public static final String MSG_STUDENT_NOT_BOUND = "当前账号未绑定学生学籍";

    /** 越权访问统一提示（不泄露资源是否存在） */
    public static final String MSG_NO_PERMISSION = "无权访问该资源";

    /** 规划明细推荐理由最大长度（edu_study_plan_item.reason varchar(500)） */
    public static final int MAX_REASON_LENGTH = 500;

    /** 规划明细校验信息最大长度（edu_study_plan_item.validation_message varchar(1000)） */
    public static final int MAX_MESSAGE_LENGTH = 1000;

    /** 规划校验摘要最大长度（edu_study_plan.validation_note varchar(1000)） */
    public static final int MAX_NOTE_LENGTH = 1000;

    /** 成绩等级统一保留 2 位小数 */
    public static final int GPA_SCALE = 2;

    static
    {
        Set<String> passGrades = new LinkedHashSet<>();
        passGrades.add("A+");
        passGrades.add("A");
        passGrades.add("A-");
        passGrades.add("B+");
        passGrades.add("B");
        passGrades.add("B-");
        passGrades.add("C+");
        passGrades.add("C");
        passGrades.add("C-");
        passGrades.add("D+");
        passGrades.add("D");
        PASS_GRADE_CODES = Collections.unmodifiableSet(passGrades);
    }

    private EducationConstants()
    {
    }

    /**
     * 空安全取零。
     *
     * @param value 学分
     * @return 非空学分
     */
    public static BigDecimal nullToZero(BigDecimal value)
    {
        return value == null ? BigDecimal.ZERO : value;
    }

    /**
     * 学分统一保留 1 位小数，便于生成“超出学分上限 21.0：当前 24.0”这类可读文案。
     *
     * @param units 学分
     * @return 形如 21.0 的字符串
     */
    public static String formatUnits(BigDecimal units)
    {
        BigDecimal value = nullToZero(units).setScale(1, RoundingMode.HALF_UP);
        return value.toPlainString();
    }

    /**
     * 按数据库列长度截断文本，避免写入时因超长抛异常。
     *
     * @param text 原始文本
     * @param maxLength 最大长度
     * @return 截断后的文本（入参为 null 时返回空串）
     */
    public static String truncate(String text, int maxLength)
    {
        if (text == null)
        {
            return "";
        }
        if (maxLength <= 0 || text.length() <= maxLength)
        {
            return text;
        }
        return text.substring(0, maxLength);
    }
}
