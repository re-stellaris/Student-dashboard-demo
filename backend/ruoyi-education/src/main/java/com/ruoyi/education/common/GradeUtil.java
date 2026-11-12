package com.ruoyi.education.common;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 成绩与 GPA 计算工具。
 *
 * <p>纯 Java 实现，不依赖 Spring / 数据库，可直接单元测试。
 * 等级制成绩到 4.0 绩点的映射：</p>
 *
 * <pre>
 * A+ / A = 4.0   A- = 3.7
 * B+ = 3.3       B  = 3.0   B- = 2.7
 * C+ = 2.3       C  = 2.0   C- = 1.7
 * D+ = 1.3       D  = 1.0   F  = 0.0
 * </pre>
 *
 * <p>GPA 以学分为权重：{@code Σ(绩点 × 学分) / Σ(学分)}，结果保留 2 位小数。</p>
 *
 * @author ruoyi
 */
public final class GradeUtil
{
    /** 未知等级或未修完的记录不参与 GPA 计算 */
    private static final BigDecimal ZERO = BigDecimal.ZERO;

    private static final Map<String, BigDecimal> LETTER_POINTS;

    /**
     * GPA 计算的最小记录抽象，便于在不依赖数据库实体的前提下复用。
     * 可用 record 直接实现：{@code record Row(String grade, BigDecimal units) implements GradeUtil.GradedRecord {}}
     */
    public interface GradedRecord
    {
        /** 等级制成绩，如 A- */
        String grade();

        /** 学分 */
        BigDecimal units();
    }

    static
    {
        Map<String, BigDecimal> points = new LinkedHashMap<>();
        points.put("A+", new BigDecimal("4.0"));
        points.put("A", new BigDecimal("4.0"));
        points.put("A-", new BigDecimal("3.7"));
        points.put("B+", new BigDecimal("3.3"));
        points.put("B", new BigDecimal("3.0"));
        points.put("B-", new BigDecimal("2.7"));
        points.put("C+", new BigDecimal("2.3"));
        points.put("C", new BigDecimal("2.0"));
        points.put("C-", new BigDecimal("1.7"));
        points.put("D+", new BigDecimal("1.3"));
        points.put("D", new BigDecimal("1.0"));
        points.put("F", new BigDecimal("0.0"));
        LETTER_POINTS = Collections.unmodifiableMap(points);
    }

    private GradeUtil()
    {
    }

    /**
     * 归一化等级制成绩：去空格、转大写。
     *
     * @param letterGrade 原始成绩，如 " a- "
     * @return 归一化后的成绩；入参为空时返回空串
     */
    public static String normalizeGrade(String letterGrade)
    {
        if (letterGrade == null)
        {
            return "";
        }
        return letterGrade.replaceAll("\\s+", "").toUpperCase(Locale.ROOT);
    }

    /**
     * 等级制成绩转 4.0 绩点。
     *
     * @param letterGrade 等级制成绩，如 A-
     * @return 绩点；未知等级或空值返回 {@code null}（调用方需自行决定是否按 0 处理）
     */
    public static BigDecimal gradePoint(String letterGrade)
    {
        return LETTER_POINTS.get(normalizeGrade(letterGrade));
    }

    /**
     * 等级制成绩转 4.0 绩点，未知等级按 0 计。
     *
     * @param letterGrade 等级制成绩
     * @return 绩点，永不为 {@code null}
     */
    public static BigDecimal gradePointOrZero(String letterGrade)
    {
        BigDecimal point = gradePoint(letterGrade);
        return point == null ? ZERO : point;
    }

    /**
     * 是否通过。
     *
     * @param letterGrade 等级制成绩
     * @return 属于 {@link EducationConstants#PASS_GRADE_CODES} 时返回 true
     */
    public static boolean isPassing(String letterGrade)
    {
        return EducationConstants.PASS_GRADE_CODES.contains(normalizeGrade(letterGrade));
    }

    /**
     * 已获得的学分总数（仅统计通过且有学分的记录）。
     *
     * @param records 成绩记录
     * @return 学分合计
     */
    public static BigDecimal earnedUnits(Collection<? extends GradedRecord> records)
    {
        BigDecimal total = ZERO;
        if (records == null)
        {
            return total;
        }
        for (GradedRecord record : records)
        {
            if (record == null || !isPassing(record.grade()))
            {
                continue;
            }
            BigDecimal units = EducationConstants.nullToZero(record.units());
            if (units.signum() > 0)
            {
                total = total.add(units);
            }
        }
        return total;
    }

    /**
     * 按学分加权的 GPA。
     *
     * @param records 成绩记录
     * @return GPA（2 位小数）；没有可计算的记录时返回 {@code null}
     */
    public static BigDecimal weightedGpa(Collection<? extends GradedRecord> records)
    {
        if (records == null || records.isEmpty())
        {
            return null;
        }
        BigDecimal weightedPoints = ZERO;
        BigDecimal totalUnits = ZERO;
        for (GradedRecord record : records)
        {
            if (record == null)
            {
                continue;
            }
            BigDecimal point = gradePoint(record.grade());
            BigDecimal units = EducationConstants.nullToZero(record.units());
            if (point == null || units.signum() <= 0)
            {
                continue;
            }
            weightedPoints = weightedPoints.add(point.multiply(units));
            totalUnits = totalUnits.add(units);
        }
        if (totalUnits.signum() == 0)
        {
            return null;
        }
        return weightedPoints.divide(totalUnits, EducationConstants.GPA_SCALE, RoundingMode.HALF_UP);
    }

    /**
     * 按学分加权的 GPA（平行列表便捷重载）。
     *
     * @param grades 等级制成绩列表
     * @param units 与成绩一一对应的学分列表
     * @return GPA（2 位小数）；无可计算记录时返回 {@code null}
     * @throws IllegalArgumentException 两个列表长度不一致时抛出
     */
    public static BigDecimal weightedGpa(List<String> grades, List<BigDecimal> units)
    {
        if (grades == null || units == null)
        {
            return null;
        }
        if (grades.size() != units.size())
        {
            throw new IllegalArgumentException("成绩与学分数量不一致");
        }
        List<GradedRecord> records = new java.util.ArrayList<>(grades.size());
        for (int i = 0; i < grades.size(); i++)
        {
            String grade = grades.get(i);
            BigDecimal credit = units.get(i);
            records.add(new GradedRecord()
            {
                @Override
                public String grade()
                {
                    return grade;
                }

                @Override
                public BigDecimal units()
                {
                    return credit;
                }
            });
        }
        return weightedGpa(records);
    }
}
