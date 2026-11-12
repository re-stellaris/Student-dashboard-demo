package com.ruoyi.education.transcript.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.education.analytics.util.GpaCalculator;
import com.ruoyi.education.common.EducationConstants;
import com.ruoyi.education.enrollment.domain.EduCourseRecord;
import com.ruoyi.education.enrollment.domain.EduStudent;
import com.ruoyi.education.enrollment.dto.TermGradeRow;
import com.ruoyi.education.enrollment.mapper.EduCourseRecordMapper;
import com.ruoyi.education.enrollment.support.EduIdentityResolver;
import com.ruoyi.education.transcript.domain.TranscriptCourseRef;
import com.ruoyi.education.transcript.domain.TranscriptRecordRef;
import com.ruoyi.education.transcript.domain.TranscriptTermRef;
import com.ruoyi.education.transcript.dto.TranscriptImportBody;
import com.ruoyi.education.transcript.dto.TranscriptImportResultVO;
import com.ruoyi.education.transcript.dto.TranscriptRow;
import com.ruoyi.education.transcript.mapper.EduTranscriptMapper;
import com.ruoyi.education.transcript.rule.PlannedRow;
import com.ruoyi.education.transcript.rule.TranscriptImportPlan;
import com.ruoyi.education.transcript.rule.TranscriptImportPlanner;
import com.ruoyi.education.transcript.rule.TranscriptLookup;
import com.ruoyi.education.transcript.service.IEduTranscriptService;

/**
 * 成绩单导入业务实现
 *
 * <p>实现 docs/architecture/api-contract.md §10.3 的 7 条规则，职责划分：</p>
 * <ul>
 *   <li>身份：{@link EduIdentityResolver} 由 JWT 反查 edu_student，绝不读请求里的 studentId；</li>
 *   <li>判定：全部委托 {@link TranscriptImportPlanner}（纯函数，可单测）；</li>
 *   <li>落库 + 学分/GPA 回算：本类负责，整体一个事务；</li>
 *   <li>GPA：复用 analytics 的 {@link GpaCalculator}，不另写换算表。</li>
 * </ul>
 *
 * @author ruoyi-education
 */
@Service
public class EduTranscriptServiceImpl implements IEduTranscriptService
{
    /** 学分精度（edu_student.total_credits 为 decimal(5,1)） */
    private static final int CREDIT_SCALE = 1;

    @Autowired
    private EduIdentityResolver identityResolver;

    @Autowired
    private EduTranscriptMapper transcriptMapper;

    @Autowired
    private EduCourseRecordMapper recordMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TranscriptImportResultVO importTranscripts(TranscriptImportBody body)
    {
        if (body == null)
        {
            throw new ServiceException("导入请求体不能为空");
        }
        List<TranscriptRow> rows = body.getRows();
        // 规则：rows 为空或超过 500 行直接拒绝（先校验，避免用超大批次打库）
        TranscriptImportPlanner.validateRowCount(rows);

        // 规则 1：身份只取 JWT → edu_student.sys_user_id
        EduStudent student = identityResolver.findStudent();
        if (student == null)
        {
            throw new ServiceException(EducationConstants.MSG_STUDENT_NOT_BOUND);
        }

        TranscriptImportPlan plan = TranscriptImportPlanner.plan(rows, buildLookup(student.getStudentId(), rows));

        String username = identityResolver.currentUsername();
        for (PlannedRow planned : plan.getRows())
        {
            if (PlannedRow.Action.INSERT == planned.getAction())
            {
                transcriptMapper.insertTranscriptRecord(toRecord(planned, student.getStudentId(), username));
            }
            else if (PlannedRow.Action.UPDATE == planned.getAction())
            {
                EduCourseRecord record = toRecord(planned, student.getStudentId(), username);
                record.setRecordId(planned.getExistingRecordId());
                record.setUpdateBy(username);
                transcriptMapper.updateTranscriptRecord(record);
            }
        }

        refreshStudentSummary(student.getStudentId(), username);
        return toResult(plan);
    }

    /**
     * 批量装载判定所需的只读数据（学期 / 课程 / 已存在记录）
     *
     * @param studentId 当前学生ID
     * @param rows 成绩行
     * @return 只读查询端口
     */
    private TranscriptLookup buildLookup(Long studentId, List<TranscriptRow> rows)
    {
        Set<String> termCodes = new LinkedHashSet<>();
        Set<String> courseCodes = new LinkedHashSet<>();
        for (TranscriptRow row : rows)
        {
            if (row == null)
            {
                continue;
            }
            String termCode = TranscriptImportPlanner.normalizeCode(row.getTermCode());
            String courseCode = TranscriptImportPlanner.normalizeCode(row.getCourseCode());
            if (!termCode.isEmpty())
            {
                termCodes.add(termCode);
            }
            if (!courseCode.isEmpty())
            {
                courseCodes.add(courseCode);
            }
        }

        MapLookup lookup = new MapLookup();
        if (!termCodes.isEmpty())
        {
            for (TranscriptTermRef ref : transcriptMapper.selectTermRefs(termCodes))
            {
                lookup.termIds.put(TranscriptImportPlanner.normalizeCode(ref.getTermCode()), ref.getTermId());
            }
        }
        if (!courseCodes.isEmpty())
        {
            for (TranscriptCourseRef ref : transcriptMapper.selectCourseRefs(courseCodes))
            {
                lookup.courses.put(TranscriptImportPlanner.normalizeCode(ref.getCourseCode()), ref);
            }
            for (TranscriptRecordRef ref : transcriptMapper.selectRecordRefs(studentId, courseCodes))
            {
                lookup.records.put(recordKey(TranscriptImportPlanner.normalizeCode(ref.getCourseCode()), ref.getTermId()),
                        ref);
            }
        }
        return lookup;
    }

    /**
     * 组装待写入的成绩记录（source 恒为 TRANSCRIPT_UPLOAD）
     *
     * @param planned 判定结果
     * @param studentId 当前学生ID（来自 JWT，不来自请求体）
     * @param username 当前登录用户名
     * @return 成绩记录
     */
    private EduCourseRecord toRecord(PlannedRow planned, Long studentId, String username)
    {
        EduCourseRecord record = new EduCourseRecord();
        record.setStudentId(studentId);
        record.setCourseId(planned.getCourseId());
        record.setCourseCode(planned.getCourseCode());
        record.setCourseTitle(planned.getCourseTitle());
        record.setTermId(planned.getTermId());
        record.setGrade(planned.getGrade());
        record.setScore(planned.getScore());
        record.setUnits(planned.getUnits());
        record.setCategory(planned.getCategory());
        record.setPassed(planned.getPassed());
        record.setRecordStatus(planned.getRecordStatus());
        record.setSource(TranscriptImportPlanner.SOURCE_TRANSCRIPT_UPLOAD);
        record.setCreateBy(username);
        return record;
    }

    /**
     * 回算 edu_student.total_credits 与 cum_gpa（§10.3 规则 6）
     *
     * <p>只统计 {@code passed = '1'} 且 {@code record_status = 'COMPLETED'} 的记录，
     * SQL 口径直接复用 {@code EduCourseRecordMapper.selectCompletedRecords}；
     * GPA 复用 {@link GpaCalculator}（等级优先、百分制兜底、按学分加权）。</p>
     *
     * @param studentId 学生ID
     * @param username 当前登录用户名
     */
    private void refreshStudentSummary(Long studentId, String username)
    {
        List<EduCourseRecord> completed = recordMapper.selectCompletedRecords(studentId);
        BigDecimal totalCredits = BigDecimal.ZERO;
        List<TermGradeRow> gpaRows = new ArrayList<>(completed == null ? 0 : completed.size());
        if (completed != null)
        {
            for (EduCourseRecord record : completed)
            {
                BigDecimal units = record.getUnits() == null ? BigDecimal.ZERO : record.getUnits();
                if (units.signum() > 0)
                {
                    totalCredits = totalCredits.add(units);
                }
                TermGradeRow gpaRow = new TermGradeRow();
                gpaRow.setTermCode(record.getTermCode());
                gpaRow.setGrade(record.getGrade());
                gpaRow.setScore(record.getScore());
                gpaRow.setUnits(units);
                gpaRows.add(gpaRow);
            }
        }
        BigDecimal gpa = GpaCalculator.weightedGpa(gpaRows);
        transcriptMapper.updateStudentSummary(studentId,
                totalCredits.setScale(CREDIT_SCALE, RoundingMode.HALF_UP), gpa, username);
    }

    /**
     * 计划 → 对外 VO
     *
     * @param plan 导入计划
     * @return 导入结果
     */
    private TranscriptImportResultVO toResult(TranscriptImportPlan plan)
    {
        TranscriptImportResultVO result = new TranscriptImportResultVO();
        result.setRowCount(plan.getRowCount());
        result.setInserted(plan.getInserted());
        result.setUpdated(plan.getUpdated());
        result.setSkipped(plan.getSkipped());
        result.setUnknownCourses(plan.getUnknownCourses());
        result.setUnknownTerms(plan.getUnknownTerms());
        result.setSkippedDetails(plan.getSkippedDetails());
        return result;
    }

    /**
     * 记录索引键：课程代码 + 学期ID
     *
     * @param courseCode 归一化课程代码
     * @param termId 学期ID
     * @return 索引键
     */
    private static String recordKey(String courseCode, Long termId)
    {
        return courseCode + "#" + termId;
    }

    /**
     * 基于内存 Map 的 {@link TranscriptLookup} 实现（一次批量查询后按需命中）
     */
    private static final class MapLookup implements TranscriptLookup
    {
        /** 学期代码 → 学期ID */
        private final Map<String, Long> termIds = new HashMap<>();

        /** 课程代码 → 课程目录快照 */
        private final Map<String, TranscriptCourseRef> courses = new HashMap<>();

        /** 课程代码#学期ID → 已存在记录 */
        private final Map<String, TranscriptRecordRef> records = new HashMap<>();

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
            return records.get(recordKey(courseCode, termId));
        }
    }
}
