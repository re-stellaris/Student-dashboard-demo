package com.ruoyi.education;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import com.ruoyi.education.common.GradeUtil;

/**
 * 等级制成绩 → 4.0 绩点映射与学分加权 GPA 的单元测试（无 Spring、无数据库）。
 *
 * @author ruoyi
 */
class GradeUtilTest
{
    /** 复用 {@link GradeUtil.GradedRecord} 的最小记录 */
    private record Row(String grade, BigDecimal units) implements GradeUtil.GradedRecord
    {
    }

    @DisplayName("等级制成绩 → 4.0 绩点映射")
    @ParameterizedTest(name = "{0} -> {1}")
    @CsvSource({
            "A+, 4.0",
            "A,  4.0",
            "A-, 3.7",
            "B+, 3.3",
            "B,  3.0",
            "B-, 2.7",
            "C+, 2.3",
            "C,  2.0",
            "C-, 1.7",
            "D+, 1.3",
            "D,  1.0",
            "F,  0.0"
    })
    void letterGradeMapsToPoint(String grade, String expected)
    {
        assertEquals(new BigDecimal(expected), GradeUtil.gradePoint(grade));
    }

    @DisplayName("未知成绩与空值返回 null，gradePointOrZero 兜底为 0")
    @Test
    void unknownGradeReturnsNull()
    {
        assertNull(GradeUtil.gradePoint("P"));
        assertNull(GradeUtil.gradePoint(""));
        assertNull(GradeUtil.gradePoint(null));
        assertEquals(BigDecimal.ZERO, GradeUtil.gradePointOrZero("PASS"));
    }

    @DisplayName("成绩归一化忽略大小写与空格")
    @Test
    void normalizesGrade()
    {
        assertEquals("A-", GradeUtil.normalizeGrade(" a- "));
        assertEquals("B+", GradeUtil.normalizeGrade("b+\t"));
        assertEquals("", GradeUtil.normalizeGrade(null));
        assertEquals(new BigDecimal("3.7"), GradeUtil.gradePoint(" a- "));
    }

    @DisplayName("通过判定：A+ ~ D 通过，F 与空值不通过")
    @ParameterizedTest
    @ValueSource(strings = { "A+", "A", "A-", "B+", "B", "B-", "C+", "C", "C-", "D+", "D" })
    void passingGrades(String grade)
    {
        assertTrue(GradeUtil.isPassing(grade));
    }

    @Test
    void failingGrades()
    {
        assertFalse(GradeUtil.isPassing("F"));
        assertFalse(GradeUtil.isPassing(""));
        assertFalse(GradeUtil.isPassing(null));
    }

    @DisplayName("GPA 按学分加权（等学分）")
    @Test
    void gpaWithEqualUnits()
    {
        List<Row> rows = Arrays.asList(
                new Row("A", new BigDecimal("3.0")),
                new Row("C", new BigDecimal("3.0")));
        // (4.0 * 3 + 2.0 * 3) / 6 = 3.00
        assertEquals(new BigDecimal("3.00"), GradeUtil.weightedGpa(rows));
    }

    @DisplayName("GPA 按学分加权（不等学分，高学分课程主导）")
    @Test
    void gpaWithWeightedUnits()
    {
        List<Row> rows = Arrays.asList(
                new Row("A", new BigDecimal("1.0")),
                new Row("C", new BigDecimal("3.0")));
        // (4.0 * 1 + 2.0 * 3) / 4 = 2.50
        assertEquals(new BigDecimal("2.50"), GradeUtil.weightedGpa(rows));
    }

    @DisplayName("GPA 计算忽略未知成绩与 0 学分记录")
    @Test
    void gpaIgnoresUnknownGradeAndZeroUnits()
    {
        List<Row> rows = Arrays.asList(
                new Row("A", new BigDecimal("2.0")),
                new Row("P", new BigDecimal("4.0")),
                new Row("B", BigDecimal.ZERO));
        assertEquals(new BigDecimal("4.00"), GradeUtil.weightedGpa(rows));
    }

    @DisplayName("无可计算记录时 GPA 返回 null")
    @Test
    void gpaWithoutEligibleRecords()
    {
        assertNull(GradeUtil.weightedGpa(null));
        assertNull(GradeUtil.weightedGpa(Collections.emptyList()));
        assertNull(GradeUtil.weightedGpa(Collections.singletonList(new Row("F", BigDecimal.ZERO))));
    }

    @DisplayName("已获得学分只统计通过且学分大于 0 的记录")
    @Test
    void earnedUnits()
    {
        List<Row> rows = Arrays.asList(
                new Row("A", new BigDecimal("3.0")),
                new Row("F", new BigDecimal("3.0")),
                new Row("B-", new BigDecimal("2.5")));
        assertEquals(new BigDecimal("5.5"), GradeUtil.earnedUnits(rows));
        assertEquals(BigDecimal.ZERO, GradeUtil.earnedUnits(null));
    }

    @DisplayName("平行列表重载：长度不一致抛异常")
    @Test
    void parallelListOverload()
    {
        BigDecimal gpa = GradeUtil.weightedGpa(
                Arrays.asList("A", "B"),
                Arrays.asList(new BigDecimal("2.0"), new BigDecimal("2.0")));
        assertEquals(new BigDecimal("3.50"), gpa);
        assertThrows(IllegalArgumentException.class, () -> GradeUtil.weightedGpa(
                Arrays.asList("A", "B"),
                Collections.singletonList(new BigDecimal("2.0"))));
    }
}
