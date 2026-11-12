package com.ruoyi.education.analytics.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.ruoyi.education.enrollment.dto.TermGradeRow;

/**
 * GPA 计算工具（标准 4.0 制）
 *
 * <p>优先使用等级制成绩映射绩点；等级缺失时按百分制成绩分段映射。
 * 学期 / 累计 GPA 均按学分加权：{@code sum(gp * units) / sum(units)}。</p>
 *
 * @author ruoyi-education
 */
public final class GpaCalculator
{
    /** GPA 显示精度 */
    private static final int GPA_SCALE = 2;

    /** 等级制成绩 → 绩点 */
    private static final Map<String, BigDecimal> GRADE_POINTS = new HashMap<>();

    static
    {
        GRADE_POINTS.put("A+", new BigDecimal("4.0"));
        GRADE_POINTS.put("A", new BigDecimal("4.0"));
        GRADE_POINTS.put("A-", new BigDecimal("3.7"));
        GRADE_POINTS.put("B+", new BigDecimal("3.3"));
        GRADE_POINTS.put("B", new BigDecimal("3.0"));
        GRADE_POINTS.put("B-", new BigDecimal("2.7"));
        GRADE_POINTS.put("C+", new BigDecimal("2.3"));
        GRADE_POINTS.put("C", new BigDecimal("2.0"));
        GRADE_POINTS.put("C-", new BigDecimal("1.7"));
        GRADE_POINTS.put("D+", new BigDecimal("1.3"));
        GRADE_POINTS.put("D", new BigDecimal("1.0"));
        GRADE_POINTS.put("D-", new BigDecimal("0.7"));
        GRADE_POINTS.put("F", BigDecimal.ZERO);
    }

    private GpaCalculator()
    {
    }

    /**
     * 计算绩点：优先等级制成绩，其次百分制成绩
     *
     * @param grade 等级制成绩，如 A-
     * @param score 百分制成绩
     * @return 绩点；两者都不可用时返回 null
     */
    public static BigDecimal toPoints(String grade, BigDecimal score)
    {
        if (grade != null && !grade.trim().isEmpty())
        {
            BigDecimal points = GRADE_POINTS.get(grade.trim().toUpperCase());
            if (points != null)
            {
                return points;
            }
        }
        if (score != null)
        {
            return fromScore(score);
        }
        return null;
    }

    /**
     * 百分制成绩 → 绩点（90+ 为 4.0，60 以下为 0）
     *
     * @param score 百分制成绩
     * @return 绩点
     */
    public static BigDecimal fromScore(BigDecimal score)
    {
        double value = score.doubleValue();
        if (value >= 90)
        {
            return new BigDecimal("4.0");
        }
        if (value >= 87)
        {
            return new BigDecimal("3.7");
        }
        if (value >= 83)
        {
            return new BigDecimal("3.3");
        }
        if (value >= 80)
        {
            return new BigDecimal("3.0");
        }
        if (value >= 77)
        {
            return new BigDecimal("2.7");
        }
        if (value >= 73)
        {
            return new BigDecimal("2.3");
        }
        if (value >= 70)
        {
            return new BigDecimal("2.0");
        }
        if (value >= 67)
        {
            return new BigDecimal("1.7");
        }
        if (value >= 63)
        {
            return new BigDecimal("1.3");
        }
        if (value >= 60)
        {
            return new BigDecimal("1.0");
        }
        return BigDecimal.ZERO;
    }

    /**
     * 按学分加权计算 GPA
     *
     * @param rows 成绩行（可为 null 或空）
     * @return 加权 GPA，保留两位小数；无有效数据时返回 0.00
     */
    public static BigDecimal weightedGpa(List<TermGradeRow> rows)
    {
        if (rows == null || rows.isEmpty())
        {
            return zero();
        }
        BigDecimal weightedSum = BigDecimal.ZERO;
        BigDecimal unitSum = BigDecimal.ZERO;
        for (TermGradeRow row : rows)
        {
            if (row == null)
            {
                continue;
            }
            BigDecimal points = toPoints(row.getGrade(), row.getScore());
            if (points == null)
            {
                continue;
            }
            BigDecimal units = row.getUnits() == null ? BigDecimal.ZERO : row.getUnits();
            if (units.signum() <= 0)
            {
                continue;
            }
            weightedSum = weightedSum.add(points.multiply(units));
            unitSum = unitSum.add(units);
        }
        if (unitSum.signum() <= 0)
        {
            return zero();
        }
        return weightedSum.divide(unitSum, GPA_SCALE, RoundingMode.HALF_UP);
    }

    /**
     * 统一保留两位小数
     *
     * @param value 原值
     * @return 规约后的值
     */
    public static BigDecimal round(BigDecimal value)
    {
        if (value == null)
        {
            return zero();
        }
        return value.setScale(GPA_SCALE, RoundingMode.HALF_UP);
    }

    /**
     * 0.00
     *
     * @return 零值
     */
    private static BigDecimal zero()
    {
        return BigDecimal.ZERO.setScale(GPA_SCALE, RoundingMode.HALF_UP);
    }
}
