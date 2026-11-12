package com.ruoyi.education.transcript.rule;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.education.transcript.domain.TranscriptCourseRef;
import com.ruoyi.education.transcript.domain.TranscriptRecordRef;
import com.ruoyi.education.transcript.dto.TranscriptRow;

/**
 * 成绩单导入判定器单元测试（纯 Java，不依赖 Spring / 数据库）
 *
 * <p>覆盖 docs/architecture/api-contract.md §10.3 的判定分支：
 * 新增、同源覆盖、异源跳过、未知学期、未知课程、超限拒绝、重复导入幂等，
 * 以及 passed / record_status 的默认口径。</p>
 *
 * @author ruoyi-education
 */
class TranscriptImportPlannerTest
{
    /** 学期 2022-23-1 的 ID */
    private static final Long TERM_2022_1 = 1000L;

    /** 学期 2023-24-1 的 ID */
    private static final Long TERM_2023_1 = 1002L;

    @Test
    void insertsNewRowsAndResolvesCourseAndTerm()
    {
        FakeLookup lookup = new FakeLookup();
        lookup.addTerm("2022-23-1", TERM_2022_1);
        lookup.addTerm("2023-24-1", TERM_2023_1);
        lookup.addCourse("CSC3100", 100597L, "数据结构", "3.00", "Major");
        lookup.addCourse("ACT2111", 100001L, "会计学原理", "3.00", "Major");
        lookup.addCourse("MAT3040", 100640L, "高等数学", "3.00", "Major");

        TranscriptImportPlan plan = TranscriptImportPlanner.plan(Arrays.asList(
                row("CSC3100", "2023-24-1", "A-"),
                row("ACT2111", "2023-24-1", "B+"),
                row("MAT3040", "2022-23-1", "B")), lookup);

        assertEquals(3, plan.getRowCount());
        assertEquals(3, plan.getInserted());
        assertEquals(0, plan.getUpdated());
        assertEquals(0, plan.getSkipped());
        assertTrue(plan.getUnknownCourses().isEmpty());
        assertTrue(plan.getUnknownTerms().isEmpty());

        PlannedRow first = plan.getRows().get(0);
        assertEquals(PlannedRow.Action.INSERT, first.getAction());
        assertEquals(100597L, first.getCourseId());
        assertEquals(TERM_2023_1, first.getTermId());
        // 自填行没给课程名 / 学分 / 分类 → 由课程目录回填
        assertEquals("数据结构", first.getCourseTitle());
        assertEquals(0, new BigDecimal("3.00").compareTo(first.getUnits()));
        assertEquals("Major", first.getCategory());
        assertEquals("1", first.getPassed());
        assertEquals(TranscriptImportPlanner.STATUS_COMPLETED, first.getRecordStatus());
    }

    @Test
    void repeatImportOfSameBatchIsIdempotentAndUpdates()
    {
        FakeLookup lookup = new FakeLookup();
        lookup.addTerm("2022-23-1", TERM_2022_1);
        List<TranscriptRow> rows = Arrays.asList(
                row("CSC3100", "2022-23-1", "A"),
                row("ACT2111", "2022-23-1", "B"),
                row("MAT3040", "2022-23-1", "C+"));

        // 第一次导入：库里没有记录 → 全部新增
        TranscriptImportPlan first = TranscriptImportPlanner.plan(rows, lookup);
        assertEquals(3, first.getInserted());

        // 模拟落库后再次导入同一批数据：同主键记录已存在且同源 → 全部更新
        lookup.addRecord("CSC3100", TERM_2022_1, 2001L, TranscriptImportPlanner.SOURCE_TRANSCRIPT_UPLOAD);
        lookup.addRecord("ACT2111", TERM_2022_1, 2002L, TranscriptImportPlanner.SOURCE_TRANSCRIPT_UPLOAD);
        lookup.addRecord("MAT3040", TERM_2022_1, 2003L, TranscriptImportPlanner.SOURCE_TRANSCRIPT_UPLOAD);

        TranscriptImportPlan second = TranscriptImportPlanner.plan(rows, lookup);
        assertEquals(0, second.getInserted());
        assertEquals(3, second.getUpdated());
        assertEquals(0, second.getSkipped());
        for (PlannedRow planned : second.getRows())
        {
            assertEquals(PlannedRow.Action.UPDATE, planned.getAction());
            assertNotNull(planned.getExistingRecordId());
        }
    }

    @Test
    void skipsOfficialRecordWithoutOverwriting()
    {
        FakeLookup lookup = new FakeLookup();
        lookup.addTerm("2022-23-1", TERM_2022_1);
        lookup.addCourse("CSC1001", 100580L, "计算机科学导论", "3.00", "Major");
        // 官方（非 TRANSCRIPT_UPLOAD）来源的同主键记录
        lookup.addRecord("CSC1001", TERM_2022_1, 1000L, "SIS_IMPORT");

        TranscriptImportPlan plan = TranscriptImportPlanner.plan(
                Arrays.asList(row("CSC1001", "2022-23-1", "A+")), lookup);

        assertEquals(0, plan.getInserted());
        assertEquals(0, plan.getUpdated());
        assertEquals(1, plan.getSkipped());
        assertEquals(1, plan.getSkippedDetails().size());
        assertEquals(TranscriptImportPlanner.REASON_OFFICIAL_RECORD,
                plan.getSkippedDetails().get(0).getReason());
        assertEquals("CSC1001", plan.getSkippedDetails().get(0).getCourseCode());
        assertEquals("2022-23-1", plan.getSkippedDetails().get(0).getTermCode());
    }

    @Test
    void unknownTermIsSkippedAndReported()
    {
        FakeLookup lookup = new FakeLookup();
        lookup.addTerm("2022-23-1", TERM_2022_1);

        TranscriptImportPlan plan = TranscriptImportPlanner.plan(
                Arrays.asList(row("CSC3100", "2099-00-1", "A")), lookup);

        assertEquals(0, plan.getInserted());
        assertEquals(1, plan.getSkipped());
        assertEquals(Arrays.asList("2099-00-1"), plan.getUnknownTerms());
        assertEquals(TranscriptImportPlanner.REASON_UNKNOWN_TERM, plan.getSkippedDetails().get(0).getReason());
        assertNull(plan.getRows().get(0).getTermId());
    }

    @Test
    void unknownCourseIsInsertedWithNullCourseId()
    {
        FakeLookup lookup = new FakeLookup();
        lookup.addTerm("2022-23-1", TERM_2022_1);

        TranscriptRow row = row("MIS23012", "2022-23-1", "B");
        row.setCourseTitle("管理信息系统");
        row.setUnits(new BigDecimal("3.0"));
        row.setCategory("Major");

        TranscriptImportPlan plan = TranscriptImportPlanner.plan(Arrays.asList(row), lookup);

        assertEquals(1, plan.getInserted());
        assertEquals(0, plan.getSkipped());
        assertEquals(Arrays.asList("MIS23012"), plan.getUnknownCourses());
        PlannedRow planned = plan.getRows().get(0);
        assertNull(planned.getCourseId());
        // 快照必须保留自填的课程名 / 学分
        assertEquals("MIS23012", planned.getCourseCode());
        assertEquals("管理信息系统", planned.getCourseTitle());
        assertEquals(0, new BigDecimal("3.0").compareTo(planned.getUnits()));
    }

    @Test
    void rejectsEmptyAndOversizedBatches()
    {
        FakeLookup lookup = new FakeLookup();
        lookup.addTerm("2022-23-1", TERM_2022_1);

        ServiceException empty = assertThrows(ServiceException.class,
                () -> TranscriptImportPlanner.plan(new ArrayList<>(), lookup));
        assertTrue(empty.getMessage().contains("没有可导入的行"));

        ServiceException nullRows = assertThrows(ServiceException.class,
                () -> TranscriptImportPlanner.plan(null, lookup));
        assertTrue(nullRows.getMessage().contains("没有可导入的行"));

        List<TranscriptRow> tooMany = new ArrayList<>();
        for (int i = 0; i < TranscriptImportPlanner.MAX_ROWS + 1; i++)
        {
            tooMany.add(row("CSC" + i, "2022-23-1", "A"));
        }
        ServiceException oversized = assertThrows(ServiceException.class,
                () -> TranscriptImportPlanner.plan(tooMany, lookup));
        assertTrue(oversized.getMessage().contains("500"));
        assertTrue(oversized.getMessage().contains("501"));

        // 恰好 500 行是允许的边界
        List<TranscriptRow> exactlyMax = tooMany.subList(0, TranscriptImportPlanner.MAX_ROWS);
        assertEquals(TranscriptImportPlanner.MAX_ROWS,
                TranscriptImportPlanner.plan(exactlyMax, lookup).getRowCount());
    }

    @Test
    void passedIsDecidedByGrade()
    {
        assertFalse(TranscriptImportPlanner.resolvePassed("F", null, Boolean.TRUE));
        assertFalse(TranscriptImportPlanner.resolvePassed("W", null, Boolean.TRUE));
        assertFalse(TranscriptImportPlanner.resolvePassed("U", null, Boolean.TRUE));
        assertFalse(TranscriptImportPlanner.resolvePassed("ip", null, Boolean.TRUE));
        assertTrue(TranscriptImportPlanner.resolvePassed("A-", null, Boolean.FALSE));
        assertTrue(TranscriptImportPlanner.resolvePassed(" c ", null, Boolean.FALSE));

        // 无等级时用百分制兜底
        assertFalse(TranscriptImportPlanner.resolvePassed(null, new BigDecimal("59.9"), Boolean.TRUE));
        assertTrue(TranscriptImportPlanner.resolvePassed(" ", new BigDecimal("60"), Boolean.FALSE));
        // 等级与分数都缺失时用前端标记，再兜底为通过
        assertTrue(TranscriptImportPlanner.resolvePassed(null, null, Boolean.TRUE));
        assertFalse(TranscriptImportPlanner.resolvePassed(null, null, Boolean.FALSE));
        assertTrue(TranscriptImportPlanner.resolvePassed(null, null, null));
    }

    @Test
    void recordStatusDefaultsToCompleted()
    {
        assertEquals(TranscriptImportPlanner.STATUS_COMPLETED, TranscriptImportPlanner.resolveRecordStatus(null));
        assertEquals(TranscriptImportPlanner.STATUS_COMPLETED, TranscriptImportPlanner.resolveRecordStatus("Completed"));
        assertEquals(TranscriptImportPlanner.STATUS_IN_PROGRESS, TranscriptImportPlanner.resolveRecordStatus("In Progress"));
        assertEquals(TranscriptImportPlanner.STATUS_WITHDRAWN, TranscriptImportPlanner.resolveRecordStatus("Withdrawn"));

        TranscriptImportPlan plan = TranscriptImportPlanner.plan(
                Arrays.asList(row("X", "2022-23-1", "A")), termOnlyLookup());
        assertEquals(TranscriptImportPlanner.STATUS_COMPLETED, plan.getRows().get(0).getRecordStatus());
    }

    @Test
    void blankCodesAreSkippedWithChineseReasons()
    {
        TranscriptImportPlan plan = TranscriptImportPlanner.plan(Arrays.asList(
                row("", "2022-23-1", "A"),
                row("CSC3100", "", "A"),
                null), termOnlyLookup());

        assertEquals(3, plan.getSkipped());
        assertEquals(0, plan.getInserted());
        assertEquals(TranscriptImportPlanner.REASON_BLANK_COURSE, plan.getSkippedDetails().get(0).getReason());
        assertEquals(TranscriptImportPlanner.REASON_BLANK_TERM, plan.getSkippedDetails().get(1).getReason());
        assertEquals(TranscriptImportPlanner.REASON_EMPTY_ROW, plan.getSkippedDetails().get(2).getReason());
        // 空学期不算「未知学期」，避免把空串塞进 unknownTerms
        assertTrue(plan.getUnknownTerms().isEmpty());
    }

    @Test
    void duplicateRowInSameBatchIsSkippedOnce()
    {
        TranscriptImportPlan plan = TranscriptImportPlanner.plan(Arrays.asList(
                row("CSC3100", "2022-23-1", "A"),
                row("csc3100 ", "2022-23-1", "B+")), termOnlyLookup());

        assertEquals(1, plan.getInserted());
        assertEquals(1, plan.getSkipped());
        assertEquals(TranscriptImportPlanner.REASON_DUPLICATE_ROW, plan.getSkippedDetails().get(0).getReason());
    }

    @Test
    void rowCountAlwaysEqualsInsertedPlusUpdatedPlusSkipped()
    {
        FakeLookup lookup = new FakeLookup();
        lookup.addTerm("2022-23-1", TERM_2022_1);
        lookup.addCourse("CSC3100", 100597L, "数据结构", "3.00", "Major");
        lookup.addCourse("ACT2111", 100001L, "会计学原理", "3.00", "Major");
        lookup.addCourse("CSC1001", 100580L, "计算机科学导论", "3.00", "Major");
        lookup.addRecord("CSC1001", TERM_2022_1, 1000L, "DEMO");
        lookup.addRecord("ACT2111", TERM_2022_1, 2002L, TranscriptImportPlanner.SOURCE_TRANSCRIPT_UPLOAD);

        TranscriptImportPlan plan = TranscriptImportPlanner.plan(Arrays.asList(
                row("CSC3100", "2022-23-1", "A"),   // 新增
                row("ACT2111", "2022-23-1", "B"),   // 同源覆盖
                row("CSC1001", "2022-23-1", "B"),   // 异源跳过
                row("MIS23012", "2022-23-1", "C"),  // 未知课程，新增
                row("CSC3100", "2099-00-1", "A")), lookup); // 未知学期，跳过

        assertEquals(plan.getRowCount(), plan.getInserted() + plan.getUpdated() + plan.getSkipped());
        assertEquals(2, plan.getInserted());
        assertEquals(1, plan.getUpdated());
        assertEquals(2, plan.getSkipped());
        assertEquals(Arrays.asList("MIS23012"), plan.getUnknownCourses());
        assertEquals(Arrays.asList("2099-00-1"), plan.getUnknownTerms());
    }

    /**
     * 只认识 2022-23-1 的查询桩
     *
     * @return 查询桩
     */
    private FakeLookup termOnlyLookup()
    {
        FakeLookup lookup = new FakeLookup();
        lookup.addTerm("2022-23-1", TERM_2022_1);
        return lookup;
    }

    /**
     * 构造一行成绩
     *
     * @param courseCode 课程代码
     * @param termCode 学期代码
     * @param grade 等级成绩
     * @return 成绩行
     */
    private TranscriptRow row(String courseCode, String termCode, String grade)
    {
        TranscriptRow row = new TranscriptRow();
        row.setCourseCode(courseCode);
        row.setTermCode(termCode);
        row.setGrade(grade);
        return row;
    }

    /**
     * 内存版 {@link TranscriptLookup}（替代 Mapper，实现纯函数测试）
     */
    private static final class FakeLookup implements TranscriptLookup
    {
        private final Map<String, Long> termIds = new HashMap<>();

        private final Map<String, TranscriptCourseRef> courses = new HashMap<>();

        private final Map<String, TranscriptRecordRef> records = new HashMap<>();

        void addTerm(String termCode, Long termId)
        {
            termIds.put(termCode, termId);
        }

        void addCourse(String code, Long courseId, String title, String credits, String category)
        {
            TranscriptCourseRef ref = new TranscriptCourseRef();
            ref.setCourseCode(code);
            ref.setCourseId(courseId);
            ref.setTitle(title);
            ref.setCredits(new BigDecimal(credits));
            ref.setCategory(category);
            courses.put(code, ref);
        }

        void addRecord(String courseCode, Long termId, Long recordId, String source)
        {
            TranscriptRecordRef ref = new TranscriptRecordRef();
            ref.setCourseCode(courseCode);
            ref.setTermId(termId);
            ref.setRecordId(recordId);
            ref.setSource(source);
            records.put(courseCode + "#" + termId, ref);
        }

        @Override
        public Long termId(String termCode)
        {
            return termIds.get(termCode);
        }

        @Override
        public TranscriptCourseRef course(String courseCode)
        {
            return courses.get(courseCode);
        }

        @Override
        public TranscriptRecordRef existing(String courseCode, Long termId)
        {
            return records.get(courseCode + "#" + termId);
        }
    }
}
