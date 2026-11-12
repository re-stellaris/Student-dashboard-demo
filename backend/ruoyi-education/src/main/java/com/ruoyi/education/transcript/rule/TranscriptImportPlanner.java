package com.ruoyi.education.transcript.rule;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.education.common.GradeUtil;
import com.ruoyi.education.transcript.domain.TranscriptCourseRef;
import com.ruoyi.education.transcript.domain.TranscriptRecordRef;
import com.ruoyi.education.transcript.dto.TranscriptRow;

/**
 * 成绩单导入判定器（纯函数，无 Spring / 无数据库 / 无事务）
 *
 * <p>把 docs/architecture/api-contract.md §10.3 的规则从 Service 中抽出来，便于单元测试：</p>
 * <ol>
 *   <li>行数校验：{@code rows} 为空或超过 {@value #MAX_ROWS} 行 → 直接拒绝（防误传整库）；</li>
 *   <li>{@code termCode} 未命中 {@code edu_term.term_code} → 跳过并计入 unknownTerms；</li>
 *   <li>{@code courseCode} 未命中 {@code edu_course.code} → 允许插入（course_id = null），
 *       保留 course_code / course_title 快照，并计入 unknownCourses；</li>
 *   <li>同一 {@code (student_id, course_code, term_id)} 已存在：
 *       原记录 {@code source = 'TRANSCRIPT_UPLOAD'} → 更新，否则跳过且不覆盖官方数据；</li>
 *   <li>写入的 source 恒为 {@code TRANSCRIPT_UPLOAD}；record_status 默认 {@code COMPLETED}；
 *       passed 由 grade 判定（F/W/U/IP 视为未通过）；</li>
 *   <li>幂等：判定完全由「当前库里的记录」决定，所以同一批数据重复导入只会落到 UPDATE。</li>
 * </ol>
 *
 * <p>本类不写库，也不解析身份：student_id 由 Service 从 JWT 解析后传入（§10.3 规则 1）。</p>
 *
 * @author ruoyi-education
 */
public final class TranscriptImportPlanner
{
    /** 单次导入的行数上限（§10.2：超出直接拒绝） */
    public static final int MAX_ROWS = 500;

    /** 自填成绩的数据来源标识，与 SIS_IMPORT / DEMO / MANUAL 区分 */
    public static final String SOURCE_TRANSCRIPT_UPLOAD = "TRANSCRIPT_UPLOAD";

    /** 跳过原因：官方记录不覆盖 */
    public static final String REASON_OFFICIAL_RECORD = "已存在官方记录，未覆盖";

    /** 跳过原因：学期代码不存在 */
    public static final String REASON_UNKNOWN_TERM = "学期代码不存在，未写入";

    /** 跳过原因：学期代码为空 */
    public static final String REASON_BLANK_TERM = "学期代码为空，未写入";

    /** 跳过原因：课程代码为空 */
    public static final String REASON_BLANK_COURSE = "课程代码为空，未写入";

    /** 跳过原因：同一批次内重复 */
    public static final String REASON_DUPLICATE_ROW = "同一批次内重复行，仅保留首行";

    /** 跳过原因：学分为负数（edu_course_record 有 check units >= 0） */
    public static final String REASON_NEGATIVE_UNITS = "学分为负数，未写入";

    /** 跳过原因：空行 */
    public static final String REASON_EMPTY_ROW = "空行，已忽略";

    /** 记录状态：已修完 */
    public static final String STATUS_COMPLETED = "COMPLETED";

    /** 记录状态：修读中 */
    public static final String STATUS_IN_PROGRESS = "IN_PROGRESS";

    /** 记录状态：已退课 */
    public static final String STATUS_WITHDRAWN = "WITHDRAWN";

    /** passed 列：通过 */
    public static final String PASSED_FLAG = "1";

    /** passed 列：未通过 */
    public static final String NOT_PASSED_FLAG = "0";

    /** 视为未通过的等级（§10.3 规则 6：F/W/U/IP） */
    public static final Set<String> FAIL_GRADE_CODES = Collections
            .unmodifiableSet(new LinkedHashSet<>(Arrays.asList("F", "W", "U", "IP")));

    /** 无等级成绩时，百分制及格线 */
    private static final BigDecimal PASS_SCORE = new BigDecimal("60");

    private TranscriptImportPlanner()
    {
    }

    /**
     * 校验行数（§10.2：rows 为空或超过 500 行直接拒绝）
     *
     * @param rows 成绩行
     * @throws ServiceException 行数不合法时抛出中文原因
     */
    public static void validateRowCount(List<TranscriptRow> rows)
    {
        if (rows == null || rows.isEmpty())
        {
            throw new ServiceException("成绩单没有可导入的行，请确认文件解析结果");
        }
        if (rows.size() > MAX_ROWS)
        {
            throw new ServiceException("单次最多导入 " + MAX_ROWS + " 行，当前 " + rows.size()
                    + " 行，请拆分文件后重试");
        }
    }

    /**
     * 生成本批次的导入计划（纯判定，不写库）
     *
     * @param rows 成绩行（已由调用方确认非空）
     * @param lookup 只读查询端口（学期 / 课程 / 已存在记录）
     * @return 导入计划
     * @throws ServiceException 行数不合法时抛出
     */
    public static TranscriptImportPlan plan(List<TranscriptRow> rows, TranscriptLookup lookup)
    {
        validateRowCount(rows);
        if (lookup == null)
        {
            throw new IllegalArgumentException("lookup 不能为空");
        }

        TranscriptImportPlan plan = new TranscriptImportPlan(rows.size());
        // 同一批次内 (courseCode, termId) 去重：避免一次请求里同一门课写两次
        Set<String> seenKeys = new LinkedHashSet<>();

        for (TranscriptRow row : rows)
        {
            if (row == null)
            {
                plan.add(skip(row, null, null, REASON_EMPTY_ROW));
                continue;
            }

            String courseCode = normalizeCode(row.getCourseCode());
            String termCode = normalizeCode(row.getTermCode());

            // 规则 3 前置：课程代码是唯一键的一部分，为空无法写入
            if (courseCode.isEmpty())
            {
                plan.add(skip(row, courseCode, termCode, REASON_BLANK_COURSE));
                continue;
            }
            // 规则 2：学期必须有值才能命中 edu_term
            if (termCode.isEmpty())
            {
                plan.add(skip(row, courseCode, termCode, REASON_BLANK_TERM));
                continue;
            }

            Long termId = lookup.termId(termCode);
            if (termId == null)
            {
                plan.markUnknownTerm(termCode);
                plan.add(skip(row, courseCode, termCode, REASON_UNKNOWN_TERM));
                continue;
            }

            String key = courseCode + "#" + termId;
            if (!seenKeys.add(key))
            {
                plan.add(skip(row, courseCode, termCode, REASON_DUPLICATE_ROW));
                continue;
            }

            // 规则 3：课程未命中仍可插入，但 course_id 为空
            TranscriptCourseRef course = lookup.course(courseCode);
            if (course == null)
            {
                plan.markUnknownCourse(courseCode);
            }

            BigDecimal units = resolveUnits(row, course);
            if (units.signum() < 0)
            {
                plan.add(skip(row, courseCode, termCode, REASON_NEGATIVE_UNITS));
                continue;
            }

            // 规则 4：同主键记录是否可覆盖，只认 source
            TranscriptRecordRef existing = lookup.existing(courseCode, termId);
            PlannedRow.Action action;
            Long existingRecordId = null;
            if (existing == null)
            {
                action = PlannedRow.Action.INSERT;
            }
            else if (SOURCE_TRANSCRIPT_UPLOAD.equalsIgnoreCase(existing.getSource()))
            {
                action = PlannedRow.Action.UPDATE;
                existingRecordId = existing.getRecordId();
            }
            else
            {
                plan.add(skip(row, courseCode, termCode, REASON_OFFICIAL_RECORD));
                continue;
            }

            PlannedRow planned = new PlannedRow(action, row);
            planned.setCourseCode(courseCode);
            planned.setTermCode(termCode);
            planned.setTermId(termId);
            planned.setCourseId(course == null ? null : course.getCourseId());
            planned.setCourseTitle(resolveTitle(row, course));
            planned.setUnits(units);
            planned.setCategory(resolveCategory(row, course));
            planned.setGrade(resolveGrade(row.getGrade()));
            planned.setScore(row.getScore());
            planned.setPassed(toFlag(resolvePassed(row.getGrade(), row.getScore(), row.getPassed())));
            planned.setRecordStatus(resolveRecordStatus(row.getStatus()));
            planned.setExistingRecordId(existingRecordId);
            plan.add(planned);
        }
        return plan;
    }

    /**
     * 归一化课程代码 / 学期代码：去首尾空格并转大写
     *
     * <p>edu_course.code、edu_term.term_code 在库里均为大写形态，前端解析结果常带空格或小写，
     * 归一化后再匹配，避免同一条数据因为大小写产生「未知课程」。</p>
     *
     * @param value 原始值
     * @return 归一化结果（入参为 null 时返回空串）
     */
    public static String normalizeCode(String value)
    {
        if (value == null)
        {
            return "";
        }
        return value.trim().toUpperCase(Locale.ROOT);
    }

    /**
     * 判定是否通过（§10.3 规则 6：passed 由 grade 判定，F/W/U/IP 视为未通过）
     *
     * <p>取值顺序：等级制成绩 → 百分制成绩（≥60 通过）→ 前端标记 → 默认通过。
     * 之所以保留后两级兜底，是因为旧版成绩单存在只有分数没有等级（或 OCR 缺列）的行。</p>
     *
     * @param grade 等级制成绩
     * @param score 百分制成绩
     * @param explicitPassed 前端解析出的通过标记（可为 null）
     * @return true 表示通过
     */
    public static boolean resolvePassed(String grade, BigDecimal score, Boolean explicitPassed)
    {
        String normalized = normalizeGradeToken(grade);
        if (!normalized.isEmpty())
        {
            return !FAIL_GRADE_CODES.contains(normalized);
        }
        if (score != null)
        {
            return score.compareTo(PASS_SCORE) >= 0;
        }
        if (explicitPassed != null)
        {
            return explicitPassed.booleanValue();
        }
        return true;
    }

    /**
     * 归一化记录状态：默认 COMPLETED（§10.3 规则 6）
     *
     * @param status 前端状态文本，如 Completed / In Progress / Withdrawn
     * @return COMPLETED / IN_PROGRESS / WITHDRAWN
     */
    public static String resolveRecordStatus(String status)
    {
        String normalized = normalizeGradeToken(status).replace('-', '_').replace(' ', '_');
        if ("IN_PROGRESS".equals(normalized) || "INPROGRESS".equals(normalized) || "IP".equals(normalized))
        {
            return STATUS_IN_PROGRESS;
        }
        if ("WITHDRAWN".equals(normalized) || "WITHDRAW".equals(normalized))
        {
            return STATUS_WITHDRAWN;
        }
        return STATUS_COMPLETED;
    }

    /**
     * 归一化等级制成绩：去空白并转大写
     *
     * @param grade 原始成绩
     * @return 归一化成绩；空值返回空串
     */
    public static String normalizeGradeToken(String grade)
    {
        return GradeUtil.normalizeGrade(grade);
    }

    /**
     * 学分：自填优先，其次课程目录，都缺失记 0
     *
     * @param row 请求行
     * @param course 课程目录快照（可为 null）
     * @return 学分（非 null）
     */
    private static BigDecimal resolveUnits(TranscriptRow row, TranscriptCourseRef course)
    {
        if (row.getUnits() != null)
        {
            return row.getUnits();
        }
        if (course != null && course.getCredits() != null)
        {
            return course.getCredits();
        }
        return BigDecimal.ZERO;
    }

    /**
     * 课程名：自填优先，其次课程目录，都缺失为 null
     *
     * @param row 请求行
     * @param course 课程目录快照（可为 null）
     * @return 课程名或 null
     */
    private static String resolveTitle(TranscriptRow row, TranscriptCourseRef course)
    {
        if (row.getCourseTitle() != null && !row.getCourseTitle().trim().isEmpty())
        {
            return row.getCourseTitle().trim();
        }
        if (course != null && course.getTitle() != null && !course.getTitle().trim().isEmpty())
        {
            return course.getTitle().trim();
        }
        return null;
    }

    /**
     * 分类：自填优先，其次课程目录，都缺失为 null
     *
     * @param row 请求行
     * @param course 课程目录快照（可为 null）
     * @return 分类或 null
     */
    private static String resolveCategory(TranscriptRow row, TranscriptCourseRef course)
    {
        if (row.getCategory() != null && !row.getCategory().trim().isEmpty())
        {
            return row.getCategory().trim();
        }
        if (course != null && course.getCategory() != null && !course.getCategory().trim().isEmpty())
        {
            return course.getCategory().trim();
        }
        return null;
    }

    /**
     * 等级制成绩：归一化后为空则写 null，避免用空串覆盖课程目录成绩
     *
     * @param grade 原始成绩
     * @return 归一化成绩或 null
     */
    private static String resolveGrade(String grade)
    {
        String normalized = normalizeGradeToken(grade);
        return normalized.isEmpty() ? null : normalized;
    }

    /**
     * boolean → edu_course_record.passed（char(1)）
     *
     * @param passed 是否通过
     * @return "1" / "0"
     */
    private static String toFlag(boolean passed)
    {
        return passed ? PASSED_FLAG : NOT_PASSED_FLAG;
    }

    /**
     * 构造一条跳过结果
     *
     * @param source 原始行（可为 null）
     * @param courseCode 归一化课程代码
     * @param termCode 归一化学期代码
     * @param reason 中文原因
     * @return 判定结果
     */
    private static PlannedRow skip(TranscriptRow source, String courseCode, String termCode, String reason)
    {
        PlannedRow planned = new PlannedRow(PlannedRow.Action.SKIP, source);
        planned.setCourseCode(courseCode);
        planned.setTermCode(termCode);
        planned.setReason(reason);
        return planned;
    }
}
