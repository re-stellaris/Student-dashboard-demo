package com.ruoyi.education.advisor.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Year;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.education.advisor.dto.AdvisorCategoryAverageVO;
import com.ruoyi.education.advisor.dto.AdvisorCategoryCreditRow;
import com.ruoyi.education.advisor.dto.AdvisorCategoryCreditVO;
import com.ruoyi.education.advisor.dto.AdvisorGpaBucketVO;
import com.ruoyi.education.advisor.dto.AdvisorGpaTrendVO;
import com.ruoyi.education.advisor.dto.AdvisorGradeRow;
import com.ruoyi.education.advisor.dto.AdvisorOverviewVO;
import com.ruoyi.education.advisor.dto.AdvisorProgressVO;
import com.ruoyi.education.advisor.dto.AdvisorRiskCountVO;
import com.ruoyi.education.advisor.dto.AdvisorRiskReasonVO;
import com.ruoyi.education.advisor.dto.AdvisorStudentDetailVO;
import com.ruoyi.education.advisor.dto.AdvisorStudentPageVO;
import com.ruoyi.education.advisor.dto.AdvisorStudentQuery;
import com.ruoyi.education.advisor.dto.AdvisorStudentRow;
import com.ruoyi.education.advisor.dto.AdvisorStudentVO;
import com.ruoyi.education.advisor.dto.AdvisorTermRow;
import com.ruoyi.education.advisor.domain.AdvisorRiskLevel;
import com.ruoyi.education.advisor.mapper.EduAdvisorMapper;
import com.ruoyi.education.advisor.rule.AdvisorRiskInput;
import com.ruoyi.education.advisor.rule.AdvisorRiskRules;
import com.ruoyi.education.advisor.rule.AdvisorRiskSignal;
import com.ruoyi.education.advisor.service.IEduAdvisorService;
import com.ruoyi.education.common.GradeUtil;

/**
 * 导师看板 业务实现
 * =====================================================================
 * <p>三个接口都只读，聚合口径如下：</p>
 *
 * <ul>
 *   <li><b>GPA</b>：优先取 edu_student.cum_gpa；缺失时按 edu_course_record 的等级成绩
 *       做学分加权（{@link GradeUtil#weightedGpa}）。学期 GPA 一律按成绩记录重新加权。</li>
 *   <li><b>学分</b>：优先取 edu_student.total_credits；缺失时按 passed='1' + COMPLETED 的
 *       记录学分汇总。</li>
 *   <li><b>应修学分</b>：{@code 30 学分/学年 × 已完成学年数}，封顶毕业要求 120 学分
 *       （edu_curriculum.total_credits 当前为 0，故用 {@link AdvisorRiskRules#DEFAULT_GRADUATION_CREDITS} 兜底）。</li>
 *   <li><b>风险</b>：完全交给 {@link AdvisorRiskRules} 的四条规则，命中原因逐条返回，
 *       总分归一化到 0-100 后分档（≥60 high、≥30 medium、否则 low）。</li>
 * </ul>
 *
 * <p><b>已知限制（重要）</b>：当前数据库没有 advisor-student（导师-学生）关系表，
 * 也没有联系记录表。因此本轮实现允许 TEACHER / ADMIN 查看<b>全部</b>学生：
 * 查询不带“导师名下”过滤，也不接受前端传入的“我是谁”。
 * 身份只用于权限校验（edu:advisor:view，由 @PreAuthorize 完成），不会从请求里读取
 * teacherId / advisorId。后续补齐 edu_advisor_student 关系表后，只需在
 * {@link EduAdvisorMapper} 的查询里按当前教师ID过滤即可，接口契约不变。</p>
 *
 * @author ruoyi-education
 */
@Service
public class EduAdvisorServiceImpl implements IEduAdvisorService
{
    /** 成绩记录状态：已完成 */
    private static final String RECORD_COMPLETED = "COMPLETED";

    /** 通过标志：通过 */
    private static final String PASSED_YES = "1";

    /** 通过标志：未通过 */
    private static final String PASSED_NO = "0";

    /** 等级制不及格 */
    private static final String FAILED_GRADE = "F";

    /** 学期是否当前学期 */
    private static final String CURRENT_TERM_YES = "1";

    /** GPA 分布桶标签（与旧版看板一致，5 档） */
    private static final String[] GPA_BUCKET_LABELS = { "<2.0", "2.0-2.5", "2.5-3.0", "3.0-3.5", "3.5-4.0" };

    /** GPA 分布桶上界（不含），最后一段为 4.0 封顶 */
    private static final BigDecimal[] GPA_BUCKET_UPPER = { new BigDecimal("2.0"), new BigDecimal("2.5"),
            new BigDecimal("3.0"), new BigDecimal("3.5") };

    /** 100（百分比换算） */
    private static final BigDecimal HUNDRED = new BigDecimal("100");

    /** 列表分页默认每页条数 */
    private static final int DEFAULT_PAGE_SIZE = 10;

    /** 单页最大条数（防止前端一次拉全表过大） */
    private static final int MAX_PAGE_SIZE = 500;

    @Autowired
    private EduAdvisorMapper advisorMapper;

    @Override
    public AdvisorOverviewVO overview()
    {
        List<AdvisorStudentRow> students = advisorMapper.selectAdvisorStudents(new AdvisorStudentQuery());
        List<AdvisorGradeRow> gradeRows = advisorMapper.selectAdvisorGradeRows(null);
        List<AdvisorTermRow> terms = advisorMapper.selectAdvisorTerms();

        int currentYear = resolveCurrentAcademicYear(terms);
        Map<Long, List<AdvisorGradeRow>> gradesByStudent = groupByStudent(gradeRows);
        Set<String> recentTermCodes = recentTermCodes(gradeRows);

        AdvisorOverviewVO overview = new AdvisorOverviewVO();
        overview.setStudentCount(students.size());

        BigDecimal gpaSum = BigDecimal.ZERO;
        int gpaCount = 0;
        BigDecimal creditSum = BigDecimal.ZERO;
        int creditCount = 0;
        int[] bucketCounts = new int[GPA_BUCKET_LABELS.length];
        AdvisorRiskCountVO risk = new AdvisorRiskCountVO();
        AdvisorProgressVO progress = new AdvisorProgressVO();

        for (AdvisorStudentRow student : students)
        {
            List<AdvisorGradeRow> rows = gradesByStudent.getOrDefault(student.getStudentId(),
                    Collections.emptyList());
            BigDecimal gpa = resolveGpa(student, rows);
            if (gpa != null)
            {
                gpaSum = gpaSum.add(gpa);
                gpaCount++;
                bucketCounts[bucketIndex(gpa)]++;
            }

            BigDecimal credits = resolveEarnedCredits(student, rows);
            BigDecimal expected = expectedCredits(student.getEntryYear(), currentYear);
            if (credits != null)
            {
                creditSum = creditSum.add(credits);
                creditCount++;
            }

            // 风险等级分布
            RiskAssessment assessment = assess(gpa, credits, expected, rows, recentTermCodes);
            AdvisorRiskLevel level = AdvisorRiskLevel.of(assessment.getLevel());
            if (level == AdvisorRiskLevel.HIGH)
            {
                risk.setHigh(risk.getHigh() + 1);
            }
            else if (level == AdvisorRiskLevel.MEDIUM)
            {
                risk.setMedium(risk.getMedium() + 1);
            }
            else
            {
                risk.setLow(risk.getLow() + 1);
            }

            // 学分进度分布（无法推算应修学分时记为 unknown）
            if (credits == null || expected == null || expected.signum() <= 0)
            {
                progress.setUnknown(progress.getUnknown() + 1);
            }
            else if (credits.compareTo(expected.multiply(AdvisorRiskRules.CREDIT_BEHIND_RATIO)) >= 0)
            {
                progress.setOnTrack(progress.getOnTrack() + 1);
            }
            else
            {
                progress.setBehind(progress.getBehind() + 1);
            }
        }

        overview.setAvgGpa(average(gpaSum, gpaCount, 2));
        overview.setAvgCredits(average(creditSum, creditCount, 1));

        List<AdvisorGpaBucketVO> buckets = new ArrayList<>();
        for (int i = 0; i < GPA_BUCKET_LABELS.length; i++)
        {
            buckets.add(new AdvisorGpaBucketVO(GPA_BUCKET_LABELS[i], bucketCounts[i]));
        }
        overview.setGpaBuckets(buckets);
        overview.setRisk(risk);
        overview.setProgress(progress);
        overview.setGpaTrend(buildCohortGpaTrend(gradeRows));
        overview.setCategoryBreakdown(buildCategoryAverages(gradeRows, students.size()));
        return overview;
    }

    @Override
    public AdvisorStudentPageVO selectStudentPage(AdvisorStudentQuery query, int pageNum, int pageSize)
    {
        AdvisorStudentQuery safeQuery = query == null ? new AdvisorStudentQuery() : query;
        safeQuery.setKeyword(trimToNull(safeQuery.getKeyword()));
        safeQuery.setRiskLevel(trimToNull(safeQuery.getRiskLevel()));

        List<AdvisorStudentRow> students = advisorMapper.selectAdvisorStudents(safeQuery);
        List<AdvisorGradeRow> gradeRows = advisorMapper.selectAdvisorGradeRows(null);
        List<AdvisorTermRow> terms = advisorMapper.selectAdvisorTerms();

        int currentYear = resolveCurrentAcademicYear(terms);
        Map<Long, List<AdvisorGradeRow>> gradesByStudent = groupByStudent(gradeRows);
        Set<String> recentTermCodes = recentTermCodes(gradeRows);

        List<AdvisorStudentVO> rows = new ArrayList<>();
        for (AdvisorStudentRow student : students)
        {
            List<AdvisorGradeRow> studentRows = gradesByStudent.getOrDefault(student.getStudentId(),
                    Collections.emptyList());
            BigDecimal gpa = resolveGpa(student, studentRows);
            BigDecimal credits = resolveEarnedCredits(student, studentRows);
            BigDecimal expected = expectedCredits(student.getEntryYear(), currentYear);
            RiskAssessment assessment = assess(gpa, credits, expected, studentRows, recentTermCodes);

            // 风险等级是派生指标（受可调权重影响），只能在服务层过滤
            if (safeQuery.getRiskLevel() != null
                    && !safeQuery.getRiskLevel().equalsIgnoreCase(assessment.getLevel()))
            {
                continue;
            }

            AdvisorStudentVO vo = new AdvisorStudentVO();
            vo.setStudentId(student.getStudentId());
            vo.setStudentNo(student.getStudentNo());
            vo.setName(student.getName());
            vo.setMajorName(student.getMajorName());
            vo.setEntryYear(student.getEntryYear());
            vo.setCumGpa(gpa);
            vo.setTotalCredits(credits);
            vo.setCreditProgressPct(creditProgressPct(credits));
            vo.setAvgScore(averageScore(studentRows));
            vo.setRiskLevel(assessment.getLevel());
            vo.setRiskScore(assessment.getScore());
            vo.setRiskReasons(toReasonVOs(assessment.getSignals()));
            rows.add(vo);
        }

        // 与旧版一致：默认按动态风险分降序，其次按学号
        rows.sort(Comparator
                .comparing(AdvisorStudentVO::getRiskScore, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(AdvisorStudentVO::getStudentNo, Comparator.nullsLast(Comparator.naturalOrder())));

        int total = rows.size();
        int safePageNum = Math.max(pageNum, 1);
        int safePageSize = pageSize <= 0 ? DEFAULT_PAGE_SIZE : Math.min(pageSize, MAX_PAGE_SIZE);
        int from = (int) Math.min((long) (safePageNum - 1) * safePageSize, total);
        int to = Math.min(from + safePageSize, total);

        AdvisorStudentPageVO page = new AdvisorStudentPageVO();
        page.setRows(new ArrayList<>(rows.subList(from, to)));
        page.setTotal(total);
        return page;
    }

    @Override
    public AdvisorStudentDetailVO selectStudentDetail(Long studentId)
    {
        if (studentId == null)
        {
            throw new ServiceException("学生ID不能为空");
        }
        AdvisorStudentRow student = advisorMapper.selectAdvisorStudentById(studentId);
        if (student == null)
        {
            throw new ServiceException("学生不存在");
        }

        // 近学期判断需要“全体学生”的成绩分布，故一次性读取后按学生过滤
        List<AdvisorGradeRow> allGradeRows = advisorMapper.selectAdvisorGradeRows(null);
        List<AdvisorGradeRow> rows = allGradeRows.stream()
                .filter(row -> Objects.equals(row.getStudentId(), studentId))
                .collect(Collectors.toList());
        List<AdvisorTermRow> terms = advisorMapper.selectAdvisorTerms();

        int currentYear = resolveCurrentAcademicYear(terms);
        Set<String> recentTermCodes = recentTermCodes(allGradeRows);
        BigDecimal gpa = resolveGpa(student, rows);
        BigDecimal credits = resolveEarnedCredits(student, rows);
        BigDecimal expected = expectedCredits(student.getEntryYear(), currentYear);
        RiskAssessment assessment = assess(gpa, credits, expected, rows, recentTermCodes);

        AdvisorStudentDetailVO detail = new AdvisorStudentDetailVO();
        detail.setStudentId(student.getStudentId());
        detail.setStudentNo(student.getStudentNo());
        detail.setName(student.getName());
        detail.setMajorId(student.getMajorId());
        detail.setMajorName(student.getMajorName());
        detail.setMajorCode(student.getMajorCode());
        detail.setEntryYear(student.getEntryYear());
        detail.setCollege(student.getCollege());
        detail.setCumGpa(gpa);
        detail.setTotalCredits(credits);
        detail.setGraduationCredits(AdvisorRiskRules.DEFAULT_GRADUATION_CREDITS);
        detail.setExpectedCredits(expected);
        detail.setCreditProgressPct(creditProgressPct(credits));
        detail.setAvgScore(averageScore(rows));
        detail.setCompletedCourseCount(countCompleted(rows));
        detail.setFailedCourseCount(countFailed(rows));
        detail.setRiskLevel(assessment.getLevel());
        detail.setRiskScore(assessment.getScore());
        detail.setRiskReasons(toReasonVOs(assessment.getSignals()));

        List<AdvisorGpaTrendVO> trend = buildStudentGpaTrend(rows);
        detail.setGpaTrend(trend);
        if (!trend.isEmpty())
        {
            detail.setLatestTermGpa(trend.get(trend.size() - 1).getAvgGpa());
        }

        List<AdvisorCategoryCreditVO> categories = new ArrayList<>();
        for (AdvisorCategoryCreditRow row : advisorMapper.selectCategoryCredits(studentId))
        {
            categories.add(new AdvisorCategoryCreditVO(row.getCategory(),
                    scale(row.getCredits(), 1), row.getCourseCount()));
        }
        detail.setCategoryCredits(categories);
        return detail;
    }

    /* =================================================================
     * 风险判定
     * ================================================================= */

    /**
     * 组装规则输入并执行四条规则。
     *
     * @param gpa 累计 GPA（可能为 null）
     * @param credits 已修学分（可能为 null）
     * @param expected 应修学分（可能为 null）
     * @param rows 该学生的成绩行
     * @param recentTermCodes 全体学生最近 N 个有数据的学期
     * @return 风险分、等级与命中原因
     */
    private RiskAssessment assess(BigDecimal gpa, BigDecimal credits, BigDecimal expected,
            List<AdvisorGradeRow> rows, Set<String> recentTermCodes)
    {
        AdvisorRiskInput input = new AdvisorRiskInput();
        input.setCumGpa(gpa);
        input.setEarnedCredits(credits == null ? BigDecimal.ZERO : credits);
        input.setExpectedCredits(expected);
        input.setFailedCourseCount(countFailed(rows));
        input.setTermCodesWithRecords(termCodesOf(rows));
        input.setRecentTermCodes(recentTermCodes);

        List<AdvisorRiskSignal> signals = AdvisorRiskRules.evaluate(input);
        int score = AdvisorRiskRules.normalizeScore(signals);
        return new RiskAssessment(score, AdvisorRiskRules.levelOf(score), signals);
    }

    /* =================================================================
     * 聚合计算
     * ================================================================= */

    /**
     * 学年平均 GPA 趋势：每个学期内先算每位学生的学期 GPA，再对学生取平均。
     *
     * @param gradeRows 全体学生成绩行
     * @return 按学期代码升序的趋势点
     */
    private List<AdvisorGpaTrendVO> buildCohortGpaTrend(List<AdvisorGradeRow> gradeRows)
    {
        Map<String, List<AdvisorGradeRow>> byTerm = new LinkedHashMap<>();
        for (AdvisorGradeRow row : gradeRows)
        {
            if (row.getTermCode() == null || row.getTermCode().trim().isEmpty())
            {
                continue;
            }
            byTerm.computeIfAbsent(row.getTermCode(), key -> new ArrayList<>()).add(row);
        }

        List<AdvisorGpaTrendVO> trend = new ArrayList<>();
        for (Map.Entry<String, List<AdvisorGradeRow>> entry : byTerm.entrySet())
        {
            Map<Long, List<AdvisorGradeRow>> byStudent = groupByStudent(entry.getValue());
            BigDecimal sum = BigDecimal.ZERO;
            int count = 0;
            for (List<AdvisorGradeRow> studentRows : byStudent.values())
            {
                BigDecimal termGpa = weightedGpa(studentRows);
                if (termGpa != null)
                {
                    sum = sum.add(termGpa);
                    count++;
                }
            }
            if (count > 0)
            {
                trend.add(new AdvisorGpaTrendVO(entry.getKey(), average(sum, count, 2)));
            }
        }
        return trend;
    }

    /**
     * 单个学生的学期 GPA 趋势。
     *
     * @param rows 该学生成绩行
     * @return 按学期代码升序的趋势点
     */
    private List<AdvisorGpaTrendVO> buildStudentGpaTrend(List<AdvisorGradeRow> rows)
    {
        Map<String, List<AdvisorGradeRow>> byTerm = new LinkedHashMap<>();
        for (AdvisorGradeRow row : rows)
        {
            if (row.getTermCode() == null || row.getTermCode().trim().isEmpty())
            {
                continue;
            }
            byTerm.computeIfAbsent(row.getTermCode(), key -> new ArrayList<>()).add(row);
        }
        List<AdvisorGpaTrendVO> trend = new ArrayList<>();
        for (Map.Entry<String, List<AdvisorGradeRow>> entry : byTerm.entrySet())
        {
            BigDecimal termGpa = weightedGpa(entry.getValue());
            if (termGpa != null)
            {
                trend.add(new AdvisorGpaTrendVO(entry.getKey(), termGpa));
            }
        }
        return trend;
    }

    /**
     * 分类学分均值（按学生数平均）。
     *
     * @param gradeRows 全体学生成绩行
     * @param studentCount 学生数
     * @return 分类均值，按均值降序
     */
    private List<AdvisorCategoryAverageVO> buildCategoryAverages(List<AdvisorGradeRow> gradeRows, int studentCount)
    {
        Map<String, BigDecimal> creditByCategory = new LinkedHashMap<>();
        Map<String, Integer> countByCategory = new LinkedHashMap<>();
        for (AdvisorGradeRow row : gradeRows)
        {
            if (!isPassedCompleted(row))
            {
                continue;
            }
            String category = normalizeCategory(row.getCategory());
            creditByCategory.merge(category, nvl(row.getUnits()), BigDecimal::add);
            countByCategory.merge(category, 1, Integer::sum);
        }

        List<AdvisorCategoryAverageVO> list = new ArrayList<>();
        for (Map.Entry<String, BigDecimal> entry : creditByCategory.entrySet())
        {
            BigDecimal avg = studentCount <= 0 ? BigDecimal.ZERO.setScale(1)
                    : entry.getValue().divide(BigDecimal.valueOf(studentCount), 1, RoundingMode.HALF_UP);
            list.add(new AdvisorCategoryAverageVO(entry.getKey(), avg,
                    countByCategory.getOrDefault(entry.getKey(), 0)));
        }
        list.sort(Comparator.comparing(AdvisorCategoryAverageVO::getAvgCredits,
                Comparator.nullsLast(Comparator.reverseOrder())));
        return list;
    }

    /**
     * 取累计 GPA：优先学籍快照，缺失时按成绩记录加权。
     *
     * @param student 学生
     * @param rows 成绩行
     * @return GPA（2 位小数），可能为 null
     */
    private BigDecimal resolveGpa(AdvisorStudentRow student, List<AdvisorGradeRow> rows)
    {
        if (student != null && student.getCumGpa() != null)
        {
            return scale(student.getCumGpa(), 2);
        }
        return weightedGpa(rows);
    }

    /**
     * 取已修学分：优先学籍快照，缺失时按通过记录汇总。
     *
     * @param student 学生
     * @param rows 成绩行
     * @return 学分（1 位小数），可能为 null
     */
    private BigDecimal resolveEarnedCredits(AdvisorStudentRow student, List<AdvisorGradeRow> rows)
    {
        if (student != null && student.getTotalCredits() != null)
        {
            return scale(student.getTotalCredits(), 1);
        }
        BigDecimal sum = BigDecimal.ZERO;
        boolean hasRecord = false;
        for (AdvisorGradeRow row : rows)
        {
            if (isPassedCompleted(row))
            {
                sum = sum.add(nvl(row.getUnits()));
                hasRecord = true;
            }
        }
        return hasRecord ? scale(sum, 1) : null;
    }

    /**
     * 按入学年推算应修学分：30 学分/学年 × 已完成学年数，封顶 120。
     *
     * <p>例：2022 年入学、当前学年 2026-27 → 4 个已完成学年 → 120 学分；
     * 2025 年入学 → 1 个已完成学年 → 30 学分。入学当年尚未完成学年时返回 0。</p>
     *
     * @param entryYear 入学年份
     * @param currentAcademicYear 当前学年的起始年份
     * @return 应修学分；入学年份缺失时返回 null
     */
    private BigDecimal expectedCredits(Integer entryYear, int currentAcademicYear)
    {
        if (entryYear == null)
        {
            return null;
        }
        int completedYears = currentAcademicYear - entryYear;
        if (completedYears <= 0)
        {
            return BigDecimal.ZERO.setScale(1);
        }
        completedYears = Math.min(completedYears, AdvisorRiskRules.STANDARD_DURATION_YEARS);
        BigDecimal expected = BigDecimal
                .valueOf((long) completedYears * AdvisorRiskRules.CREDITS_PER_ACADEMIC_YEAR)
                .setScale(1);
        return expected.min(AdvisorRiskRules.DEFAULT_GRADUATION_CREDITS);
    }

    /**
     * 学分完成度（相对毕业要求 120 学分，封顶 100）。
     *
     * @param credits 已修学分
     * @return 百分比（1 位小数）
     */
    private BigDecimal creditProgressPct(BigDecimal credits)
    {
        if (credits == null || credits.signum() <= 0)
        {
            return BigDecimal.ZERO.setScale(1);
        }
        BigDecimal pct = credits.multiply(HUNDRED)
                .divide(AdvisorRiskRules.DEFAULT_GRADUATION_CREDITS, 1, RoundingMode.HALF_UP);
        return pct.min(HUNDRED.setScale(1));
    }

    /**
     * 百分制成绩均值。
     *
     * @param rows 成绩行
     * @return 平均分（1 位小数），无成绩时返回 null
     */
    private BigDecimal averageScore(List<AdvisorGradeRow> rows)
    {
        BigDecimal sum = BigDecimal.ZERO;
        int count = 0;
        for (AdvisorGradeRow row : rows)
        {
            if (row.getScore() != null && isCompleted(row))
            {
                sum = sum.add(row.getScore());
                count++;
            }
        }
        return count == 0 ? null : average(sum, count, 1);
    }

    /**
     * 学期 GPA：按成绩记录的学分加权（未知等级如 PA/P/DI 不参与）。
     *
     * @param rows 同一学期的成绩行
     * @return GPA（2 位小数），无法计算时返回 null
     */
    private BigDecimal weightedGpa(List<AdvisorGradeRow> rows)
    {
        if (rows == null || rows.isEmpty())
        {
            return null;
        }
        List<GradeUtil.GradedRecord> records = new ArrayList<>(rows.size());
        for (AdvisorGradeRow row : rows)
        {
            if (!isCompleted(row))
            {
                continue;
            }
            records.add(new GradeRecord(row.getGrade(), nvl(row.getUnits())));
        }
        return GradeUtil.weightedGpa(records);
    }

    /**
     * 已通过且已完成的课程数。
     *
     * @param rows 成绩行
     * @return 课程数
     */
    private int countCompleted(List<AdvisorGradeRow> rows)
    {
        int count = 0;
        for (AdvisorGradeRow row : rows)
        {
            if (isPassedCompleted(row))
            {
                count++;
            }
        }
        return count;
    }

    /**
     * 未通过记录数：passed='0' 或等级 F。
     *
     * @param rows 成绩行
     * @return 记录数
     */
    private int countFailed(List<AdvisorGradeRow> rows)
    {
        int count = 0;
        for (AdvisorGradeRow row : rows)
        {
            String passed = row.getPassed() == null ? null : row.getPassed().trim();
            String grade = row.getGrade() == null ? null : row.getGrade().trim().toUpperCase();
            if (PASSED_NO.equals(passed) || FAILED_GRADE.equals(grade))
            {
                count++;
            }
        }
        return count;
    }

    /* =================================================================
     * 小工具
     * ================================================================= */

    private boolean isCompleted(AdvisorGradeRow row)
    {
        return row != null && RECORD_COMPLETED.equalsIgnoreCase(
                row.getRecordStatus() == null ? "" : row.getRecordStatus().trim());
    }

    private boolean isPassedCompleted(AdvisorGradeRow row)
    {
        return isCompleted(row) && PASSED_YES.equals(row.getPassed() == null ? "" : row.getPassed().trim());
    }

    private Set<String> termCodesOf(List<AdvisorGradeRow> rows)
    {
        Set<String> codes = new LinkedHashSet<>();
        for (AdvisorGradeRow row : rows)
        {
            if (row.getTermCode() != null && !row.getTermCode().trim().isEmpty())
            {
                codes.add(row.getTermCode());
            }
        }
        return codes;
    }

    /**
     * 最近 N 个“有成绩数据”的学期（不含尚无成绩的未来学期，避免误报）。
     *
     * @param gradeRows 全体学生成绩行
     * @return 学期代码集合（按升序）
     */
    private Set<String> recentTermCodes(List<AdvisorGradeRow> gradeRows)
    {
        Set<String> all = new LinkedHashSet<>();
        for (AdvisorGradeRow row : gradeRows)
        {
            if (row.getTermCode() != null && !row.getTermCode().trim().isEmpty())
            {
                all.add(row.getTermCode());
            }
        }
        List<String> sorted = new ArrayList<>(all);
        sorted.sort(Comparator.naturalOrder());
        int from = Math.max(0, sorted.size() - AdvisorRiskRules.RECENT_TERM_WINDOW);
        return new LinkedHashSet<>(sorted.subList(from, sorted.size()));
    }

    private Map<Long, List<AdvisorGradeRow>> groupByStudent(List<AdvisorGradeRow> rows)
    {
        Map<Long, List<AdvisorGradeRow>> map = new LinkedHashMap<>();
        for (AdvisorGradeRow row : rows)
        {
            if (row.getStudentId() == null)
            {
                continue;
            }
            map.computeIfAbsent(row.getStudentId(), key -> new ArrayList<>()).add(row);
        }
        return map;
    }

    private int bucketIndex(BigDecimal gpa)
    {
        for (int i = 0; i < GPA_BUCKET_UPPER.length; i++)
        {
            if (gpa.compareTo(GPA_BUCKET_UPPER[i]) < 0)
            {
                return i;
            }
        }
        return GPA_BUCKET_LABELS.length - 1;
    }

    private List<AdvisorRiskReasonVO> toReasonVOs(List<AdvisorRiskSignal> signals)
    {
        List<AdvisorRiskReasonVO> reasons = new ArrayList<>();
        if (signals == null)
        {
            return reasons;
        }
        for (AdvisorRiskSignal signal : signals)
        {
            reasons.add(AdvisorRiskReasonVO.from(signal));
        }
        return reasons;
    }

    /**
     * 当前学年的起始年份：优先 is_current='1' 的学期，其次最后一个学期，最后系统年份。
     *
     * @param terms 学期列表
     * @return 学年起始年份，如 2026
     */
    private int resolveCurrentAcademicYear(List<AdvisorTermRow> terms)
    {
        if (terms != null)
        {
            for (AdvisorTermRow term : terms)
            {
                if (CURRENT_TERM_YES.equals(term.getIsCurrent()) && term.getTermCode() != null)
                {
                    return parseAcademicYear(term.getTermCode());
                }
            }
            for (int i = terms.size() - 1; i >= 0; i--)
            {
                AdvisorTermRow term = terms.get(i);
                if (term.getTermCode() != null)
                {
                    return parseAcademicYear(term.getTermCode());
                }
            }
        }
        return Year.now().getValue();
    }

    /**
     * 从学期代码（2026-27-1）或学年（2026-27）解析起始年份。
     *
     * @param code 学期代码/学年
     * @return 起始年份
     */
    private int parseAcademicYear(String code)
    {
        String trimmed = code.trim();
        String first = trimmed.split("-")[0];
        try
        {
            return Integer.parseInt(first);
        }
        catch (NumberFormatException ex)
        {
            return Year.now().getValue();
        }
    }

    private String normalizeCategory(String category)
    {
        return category == null || category.trim().isEmpty() ? "Other" : category.trim();
    }

    private String trimToNull(String value)
    {
        if (value == null || value.trim().isEmpty())
        {
            return null;
        }
        return value.trim();
    }

    private BigDecimal nvl(BigDecimal value)
    {
        return value == null ? BigDecimal.ZERO : value;
    }

    private BigDecimal scale(BigDecimal value, int scale)
    {
        return value == null ? null : value.setScale(scale, RoundingMode.HALF_UP);
    }

    private BigDecimal average(BigDecimal sum, int count, int scale)
    {
        if (count <= 0)
        {
            return BigDecimal.ZERO.setScale(scale);
        }
        return sum.divide(BigDecimal.valueOf(count), scale, RoundingMode.HALF_UP);
    }

    /**
     * 成绩行到 GPA 计算记录的适配器。
     */
    private static final class GradeRecord implements GradeUtil.GradedRecord
    {
        private final String grade;

        private final BigDecimal units;

        private GradeRecord(String grade, BigDecimal units)
        {
            this.grade = grade;
            this.units = units;
        }

        @Override
        public String grade()
        {
            return grade;
        }

        @Override
        public BigDecimal units()
        {
            return units;
        }
    }

    /**
     * 风险判定结果（服务层内部使用）。
     */
    private static final class RiskAssessment
    {
        private final int score;

        private final String level;

        private final List<AdvisorRiskSignal> signals;

        private RiskAssessment(int score, String level, List<AdvisorRiskSignal> signals)
        {
            this.score = score;
            this.level = level;
            this.signals = signals;
        }

        public int getScore()
        {
            return score;
        }

        public String getLevel()
        {
            return level;
        }

        public List<AdvisorRiskSignal> getSignals()
        {
            return signals;
        }
    }
}
