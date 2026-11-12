package com.ruoyi.education.advisor.rule;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import com.ruoyi.education.advisor.domain.AdvisorRiskLevel;

/**
 * 导师看板风险规则（透明、可解释、可调）
 * =====================================================================
 * <p>四条规则都实现为纯函数，输入 {@link AdvisorRiskInput}，输出命中的
 * {@link AdvisorRiskSignal} 列表；不依赖 Spring / 数据库，可直接单元测试。</p>
 *
 * <pre>
 * GPA_LOW         累计 GPA &lt; 2.0（高危）或 &lt; 2.5（警戒）
 * CREDIT_BEHIND   已修学分 &lt; 按入学年推算的应修学分 × 80%
 * FAILED_COURSE   存在 passed='0' 或等级 F 的成绩记录
 * NO_RECENT_TERM  最近两个（有数据的）学期都没有该学生的成绩记录
 * </pre>
 *
 * <p><b>权重可调</b>：下面 {@code WEIGHT_*} 常量就是每条规则的基础权重，默认 1.0（等权）。
 * 调整风险模型时只改这些常量即可，不需要改动业务代码；前端也会拿到每条原因的
 * {@code weight}，按导师在页面上输入的四项权重做展示层重新加权（不再请求后端）。
 * 总分 = Σ命中规则权重 / Σ全部规则权重 × 100，再做 60/30 分档。</p>
 *
 * @author ruoyi-education
 */
public final class AdvisorRiskRules
{
    /* ---------------- 可调权重（默认等权 1.0） ---------------- */

    /** 规则权重：GPA 偏低（提高该值会让 GPA 问题更快变为高风险） */
    public static final BigDecimal WEIGHT_GPA_LOW = BigDecimal.ONE;

    /** 规则权重：学分进度滞后 */
    public static final BigDecimal WEIGHT_CREDIT_BEHIND = BigDecimal.ONE;

    /** 规则权重：存在不及格课程 */
    public static final BigDecimal WEIGHT_FAILED_COURSE = BigDecimal.ONE;

    /** 规则权重：最近学期无成绩记录 */
    public static final BigDecimal WEIGHT_NO_RECENT_TERM = BigDecimal.ONE;

    /* ---------------- 可调阈值 ---------------- */

    /** 累计 GPA 高危线（严格小于） */
    public static final BigDecimal GPA_HIGH_MAX = new BigDecimal("2.0");

    /** 累计 GPA 警戒线（严格小于） */
    public static final BigDecimal GPA_MEDIUM_MAX = new BigDecimal("2.5");

    /** 学分进度告警比例：已修 < 应修 × 0.8 视为滞后 */
    public static final BigDecimal CREDIT_BEHIND_RATIO = new BigDecimal("0.8");

    /** 每个学年的应修学分（四年 120 学分口径） */
    public static final int CREDITS_PER_ACADEMIC_YEAR = 30;

    /** 标准学制年限（用于封顶应修学分，避免第 5 年学生被要求修满 150 学分） */
    public static final int STANDARD_DURATION_YEARS = 4;

    /** 毕业总学分默认值；edu_curriculum.total_credits 当前为 0，故以常量兜底 */
    public static final BigDecimal DEFAULT_GRADUATION_CREDITS = new BigDecimal("120.0");

    /** “最近学期”窗口：最近 2 个有数据的学期 */
    public static final int RECENT_TERM_WINDOW = 2;

    /** 总分 ≥ 60 判为高风险 */
    public static final int SCORE_HIGH_THRESHOLD = 60;

    /** 总分 ≥ 30 判为中风险 */
    public static final int SCORE_MEDIUM_THRESHOLD = 30;

    /* ---------------- 规则编码与标题（前端按 code 做加权映射） ---------------- */

    public static final String CODE_GPA_LOW = "GPA_LOW";
    public static final String CODE_CREDIT_BEHIND = "CREDIT_BEHIND";
    public static final String CODE_FAILED_COURSE = "FAILED_COURSE";
    public static final String CODE_NO_RECENT_TERM = "NO_RECENT_TERM";

    public static final String LABEL_GPA_LOW = "GPA 偏低";
    public static final String LABEL_CREDIT_BEHIND = "学分进度滞后";
    public static final String LABEL_FAILED_COURSE = "存在不及格课程";
    public static final String LABEL_NO_RECENT_TERM = "近期无成绩记录";

    /** 风险等级：高 */
    public static final String LEVEL_HIGH = AdvisorRiskLevel.HIGH.getCode();

    /** 风险等级：中 */
    public static final String LEVEL_MEDIUM = AdvisorRiskLevel.MEDIUM.getCode();

    /** 风险等级：低 */
    public static final String LEVEL_LOW = AdvisorRiskLevel.LOW.getCode();

    private AdvisorRiskRules()
    {
    }

    /**
     * 依次执行四条规则，返回命中的风险信号（按规则顺序，便于前端稳定展示）。
     *
     * @param input 规则输入（GPA / 学分 / 不及格数 / 学期覆盖）
     * @return 命中的风险信号；全部未命中时返回空列表
     */
    public static List<AdvisorRiskSignal> evaluate(AdvisorRiskInput input)
    {
        List<AdvisorRiskSignal> signals = new ArrayList<>();
        if (input == null)
        {
            return signals;
        }

        // 规则 1：GPA 偏低（< 2.0 高危，< 2.5 警戒）
        BigDecimal cumGpa = input.getCumGpa();
        if (cumGpa != null)
        {
            if (cumGpa.compareTo(GPA_HIGH_MAX) < 0)
            {
                signals.add(new AdvisorRiskSignal(CODE_GPA_LOW, LABEL_GPA_LOW,
                        "累计 GPA " + formatGpa(cumGpa) + "，低于高危线 " + formatGpa(GPA_HIGH_MAX),
                        WEIGHT_GPA_LOW));
            }
            else if (cumGpa.compareTo(GPA_MEDIUM_MAX) < 0)
            {
                signals.add(new AdvisorRiskSignal(CODE_GPA_LOW, LABEL_GPA_LOW,
                        "累计 GPA " + formatGpa(cumGpa) + "，低于警戒线 " + formatGpa(GPA_MEDIUM_MAX),
                        WEIGHT_GPA_LOW));
            }
        }

        // 规则 2：学分进度滞后（已修 < 应修 × 80%）
        BigDecimal expected = input.getExpectedCredits();
        BigDecimal earned = input.getEarnedCredits();
        if (expected != null && expected.signum() > 0)
        {
            BigDecimal warnLine = expected.multiply(CREDIT_BEHIND_RATIO)
                    .setScale(1, RoundingMode.HALF_UP);
            if (earned.compareTo(warnLine) < 0)
            {
                signals.add(new AdvisorRiskSignal(CODE_CREDIT_BEHIND, LABEL_CREDIT_BEHIND,
                        "已修 " + format(earned) + " 学分，低于按入学年推算的应修 "
                                + format(expected) + " 学分的 80%（" + format(warnLine) + "）",
                        WEIGHT_CREDIT_BEHIND));
            }
        }

        // 规则 3：存在不及格课程（passed='0' 或等级 F）
        if (input.getFailedCourseCount() > 0)
        {
            signals.add(new AdvisorRiskSignal(CODE_FAILED_COURSE, LABEL_FAILED_COURSE,
                    "存在 " + input.getFailedCourseCount() + " 条未通过成绩（passed='0' 或等级 F）",
                    WEIGHT_FAILED_COURSE));
        }

        // 规则 4：最近 N 个有数据的学期都没有成绩记录
        Set<String> recent = input.getRecentTermCodes();
        if (!recent.isEmpty())
        {
            boolean hasRecent = recent.stream().anyMatch(input.getTermCodesWithRecords()::contains);
            if (!hasRecent)
            {
                signals.add(new AdvisorRiskSignal(CODE_NO_RECENT_TERM, LABEL_NO_RECENT_TERM,
                        "最近 " + recent.size() + " 个学期（" + String.join("、", recent)
                                + "）均无成绩记录",
                        WEIGHT_NO_RECENT_TERM));
            }
        }

        return signals;
    }

    /**
     * 全部规则权重之和（归一化分母）。
     *
     * @return 权重合计
     */
    public static BigDecimal totalWeight()
    {
        return WEIGHT_GPA_LOW
                .add(WEIGHT_CREDIT_BEHIND)
                .add(WEIGHT_FAILED_COURSE)
                .add(WEIGHT_NO_RECENT_TERM);
    }

    /**
     * 把命中的风险信号归一化为 0-100 分。
     *
     * <p>总分 = Σ命中权重 / Σ全部权重 × 100，四舍五入取整。默认等权时：
     * 命中 1 条 = 25 分、2 条 = 50 分、3 条 = 75 分、4 条 = 100 分。</p>
     *
     * @param signals 命中的风险信号
     * @return 0-100 的风险分
     */
    public static int normalizeScore(List<AdvisorRiskSignal> signals)
    {
        BigDecimal total = totalWeight();
        if (signals == null || signals.isEmpty() || total.signum() <= 0)
        {
            return 0;
        }
        BigDecimal hit = BigDecimal.ZERO;
        for (AdvisorRiskSignal signal : signals)
        {
            hit = hit.add(signal.getWeight());
        }
        return hit.multiply(BigDecimal.valueOf(100))
                .divide(total, 0, RoundingMode.HALF_UP)
                .intValue();
    }

    /**
     * 按归一化分数分档（域内枚举）。
     *
     * @param score 0-100 风险分
     * @return 风险等级
     */
    public static AdvisorRiskLevel levelEnumOf(int score)
    {
        if (score >= SCORE_HIGH_THRESHOLD)
        {
            return AdvisorRiskLevel.HIGH;
        }
        if (score >= SCORE_MEDIUM_THRESHOLD)
        {
            return AdvisorRiskLevel.MEDIUM;
        }
        return AdvisorRiskLevel.LOW;
    }

    /**
     * 按归一化分数分档。
     *
     * @param score 0-100 风险分
     * @return high / medium / low
     */
    public static String levelOf(int score)
    {
        return levelEnumOf(score).getCode();
    }

    /**
     * 把命中信号转换成便于排查的 code 列表。
     *
     * @param signals 命中的风险信号
     * @return 编码列表
     */
    public static List<String> codesOf(List<AdvisorRiskSignal> signals)
    {
        if (signals == null || signals.isEmpty())
        {
            return new ArrayList<>();
        }
        return signals.stream().map(AdvisorRiskSignal::getCode).collect(Collectors.toList());
    }

    /**
     * GPA 统一保留 2 位小数展示（如 2.40、1.85）。
     *
     * @param value GPA
     * @return 形如 2.40 的字符串
     */
    private static String formatGpa(BigDecimal value)
    {
        return value == null ? "0.00" : value.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    /**
     * 学分等数值的两位小数展示（去掉无意义的尾零）。
     *
     * @param value 数值
     * @return 形如 3.11 / 120 的字符串
     */
    private static String format(BigDecimal value)
    {
        if (value == null)
        {
            return "0";
        }
        return value.setScale(2, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();
    }
}
