package com.ruoyi.education.advisor.rule;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.math.BigDecimal;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * 风险规则单元测试（纯 Java，不依赖 Spring / 数据库）
 *
 * <p>覆盖：四条规则的命中条件与可读明细、0-100 归一化、60/30 分档。</p>
 *
 * @author ruoyi-education
 */
class AdvisorRiskRulesTest
{
    /**
     * 健康学生：GPA 3.20、学分达标、无挂科、最近学期有成绩。
     *
     * @return 规则输入
     */
    private AdvisorRiskInput healthyInput()
    {
        AdvisorRiskInput input = new AdvisorRiskInput();
        input.setCumGpa(new BigDecimal("3.20"));
        input.setEarnedCredits(new BigDecimal("120.0"));
        input.setExpectedCredits(new BigDecimal("120.0"));
        input.setFailedCourseCount(0);
        Set<String> terms = new LinkedHashSet<>();
        terms.add("2025-26-2");
        input.setTermCodesWithRecords(terms);
        input.setRecentTermCodes(terms);
        return input;
    }

    @Test
    void healthyStudentHasNoSignal()
    {
        List<AdvisorRiskSignal> signals = AdvisorRiskRules.evaluate(healthyInput());
        assertTrue(signals.isEmpty());
        assertEquals(0, AdvisorRiskRules.normalizeScore(signals));
        assertEquals(AdvisorRiskRules.LEVEL_LOW, AdvisorRiskRules.levelOf(0));
    }

    @Test
    void gpaBelowTwoIsHighTierSignal()
    {
        AdvisorRiskInput input = healthyInput();
        input.setCumGpa(new BigDecimal("1.85"));

        List<AdvisorRiskSignal> signals = AdvisorRiskRules.evaluate(input);
        assertEquals(1, signals.size());
        AdvisorRiskSignal signal = signals.get(0);
        assertEquals(AdvisorRiskRules.CODE_GPA_LOW, signal.getCode());
        assertTrue(signal.getDetail().contains("1.85"));
        assertTrue(signal.getDetail().contains("高危"));
        assertEquals(AdvisorRiskRules.WEIGHT_GPA_LOW, signal.getWeight());
    }

    @Test
    void gpaBetweenTwoAndTwoPointFiveIsWarnTierSignal()
    {
        AdvisorRiskInput input = healthyInput();
        input.setCumGpa(new BigDecimal("2.40"));

        List<AdvisorRiskSignal> signals = AdvisorRiskRules.evaluate(input);
        assertEquals(1, signals.size());
        assertTrue(signals.get(0).getDetail().contains("2.40"));
        assertTrue(signals.get(0).getDetail().contains("警戒"));
    }

    @Test
    void creditBehindComparedWithEightyPercentLine()
    {
        AdvisorRiskInput behind = healthyInput();
        behind.setEarnedCredits(new BigDecimal("95.0"));
        List<AdvisorRiskSignal> signals = AdvisorRiskRules.evaluate(behind);
        assertEquals(1, signals.size());
        assertEquals(AdvisorRiskRules.CODE_CREDIT_BEHIND, signals.get(0).getCode());
        assertTrue(signals.get(0).getDetail().contains("96"));

        // 边界：恰好等于 120 × 80% 时不算滞后
        AdvisorRiskInput atLine = healthyInput();
        atLine.setEarnedCredits(new BigDecimal("96.0"));
        assertTrue(AdvisorRiskRules.evaluate(atLine).isEmpty());
    }

    @Test
    void failedCourseReportsCount()
    {
        AdvisorRiskInput input = healthyInput();
        input.setFailedCourseCount(2);

        List<AdvisorRiskSignal> signals = AdvisorRiskRules.evaluate(input);
        assertEquals(1, signals.size());
        assertEquals(AdvisorRiskRules.CODE_FAILED_COURSE, signals.get(0).getCode());
        assertTrue(signals.get(0).getDetail().contains("2 条"));
    }

    @Test
    void noRecentTermUsesRecentWindow()
    {
        AdvisorRiskInput input = healthyInput();
        Set<String> staleTerms = new LinkedHashSet<>();
        staleTerms.add("2022-23-1");
        input.setTermCodesWithRecords(staleTerms);
        Set<String> recent = new LinkedHashSet<>();
        recent.add("2025-26-1");
        recent.add("2025-26-2");
        input.setRecentTermCodes(recent);

        List<AdvisorRiskSignal> signals = AdvisorRiskRules.evaluate(input);
        assertEquals(1, signals.size());
        assertEquals(AdvisorRiskRules.CODE_NO_RECENT_TERM, signals.get(0).getCode());
        assertTrue(signals.get(0).getDetail().contains("2025-26"));
    }

    @Test
    void scoreIsNormalizedAndBucketed()
    {
        // 命中 1 条 = 25 分 → low
        AdvisorRiskInput one = healthyInput();
        one.setEarnedCredits(new BigDecimal("10.0"));
        assertEquals(25, AdvisorRiskRules.normalizeScore(AdvisorRiskRules.evaluate(one)));
        assertEquals(AdvisorRiskRules.LEVEL_LOW, AdvisorRiskRules.levelOf(25));

        // 命中 2 条 = 50 分 → medium
        AdvisorRiskInput two = healthyInput();
        two.setCumGpa(new BigDecimal("2.40"));
        two.setEarnedCredits(new BigDecimal("10.0"));
        assertEquals(50, AdvisorRiskRules.normalizeScore(AdvisorRiskRules.evaluate(two)));
        assertEquals(AdvisorRiskRules.LEVEL_MEDIUM, AdvisorRiskRules.levelOf(50));

        // 命中 4 条 = 100 分 → high
        AdvisorRiskInput four = healthyInput();
        four.setCumGpa(new BigDecimal("1.90"));
        four.setEarnedCredits(new BigDecimal("10.0"));
        four.setFailedCourseCount(1);
        Set<String> staleTerms = new LinkedHashSet<>();
        staleTerms.add("2022-23-1");
        four.setTermCodesWithRecords(staleTerms);
        Set<String> recent = new LinkedHashSet<>();
        recent.add("2025-26-1");
        recent.add("2025-26-2");
        four.setRecentTermCodes(recent);
        List<AdvisorRiskSignal> signals = AdvisorRiskRules.evaluate(four);
        assertEquals(4, signals.size());
        assertEquals(100, AdvisorRiskRules.normalizeScore(signals));
        assertEquals(AdvisorRiskRules.LEVEL_HIGH, AdvisorRiskRules.levelOf(100));
    }
}
