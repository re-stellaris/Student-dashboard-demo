package com.ruoyi.education.analytics.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.education.analytics.domain.EduCourseCompetency;
import com.ruoyi.education.analytics.domain.EduSkillTag;
import com.ruoyi.education.analytics.dto.DashboardVO;
import com.ruoyi.education.analytics.dto.GpaTrendPointVO;
import com.ruoyi.education.analytics.dto.PlanSummaryVO;
import com.ruoyi.education.analytics.dto.SkillProfileVO;
import com.ruoyi.education.enrollment.dto.TermGradeRow;
import com.ruoyi.education.analytics.mapper.EduCourseCompetencyMapper;
import com.ruoyi.education.analytics.service.IEduAnalyticsService;
import com.ruoyi.education.analytics.util.GpaCalculator;
import com.ruoyi.education.assignment.mapper.EduSubmissionMapper;
import com.ruoyi.education.enrollment.domain.EduStudent;
import com.ruoyi.education.enrollment.dto.CreditProgressVO;
import com.ruoyi.education.enrollment.dto.PlanStatusCountRow;
import com.ruoyi.education.enrollment.mapper.EduCourseRecordMapper;
import com.ruoyi.education.enrollment.mapper.EduStudentMapper;
import com.ruoyi.education.enrollment.service.IEduEnrollmentService;
import com.ruoyi.education.enrollment.support.EduIdentityResolver;

/**
 * 学情与技能画像 业务实现
 *
 * @author ruoyi-education
 */
@Service
public class EduAnalyticsServiceImpl implements IEduAnalyticsService
{
    /** 每个技能标签参与聚合的课程数上限（top-N） */
    private static final int TOP_COURSES_PER_SKILL = 3;

    /** 技能关联度精度 */
    private static final int SCORE_SCALE = 1;

    /** 规划状态：草稿 */
    private static final String PLAN_DRAFT = "DRAFT";

    /** 规划状态：已校验 */
    private static final String PLAN_VALIDATED = "VALIDATED";

    /** 规划状态：已提交 */
    private static final String PLAN_SUBMITTED = "SUBMITTED";

    /** 规划状态：已归档 */
    private static final String PLAN_ARCHIVED = "ARCHIVED";

    @Autowired
    private EduIdentityResolver identityResolver;

    @Autowired
    private IEduEnrollmentService enrollmentService;

    @Autowired
    private EduCourseRecordMapper recordMapper;

    @Autowired
    private EduStudentMapper studentMapper;

    @Autowired
    private EduSubmissionMapper submissionMapper;

    @Autowired
    private EduCourseCompetencyMapper competencyMapper;

    @Override
    public DashboardVO dashboard()
    {
        EduStudent student = identityResolver.currentStudent();
        CreditProgressVO credits = enrollmentService.selectMyProgress();

        DashboardVO vo = new DashboardVO();
        vo.setCredits(credits);
        vo.setCategoryBreakdown(credits.getCategoryBreakdown());

        List<TermGradeRow> gradeRows = recordMapper.selectTermGradeRows(student.getStudentId());
        if (student.getCumGpa() != null)
        {
            vo.setGpa(GpaCalculator.round(student.getCumGpa()));
        }
        else
        {
            vo.setGpa(GpaCalculator.weightedGpa(gradeRows));
        }
        vo.setGpaTrend(buildGpaTrend(gradeRows));
        vo.setPlanSummary(buildPlanSummary(student.getStudentId()));
        vo.setPendingAssignments(submissionMapper.countPendingByStudent(student.getStudentId()));
        return vo;
    }

    @Override
    public List<SkillProfileVO> selectMySkillProfile()
    {
        EduStudent student = identityResolver.currentStudent();
        List<EduCourseCompetency> rows = competencyMapper.selectByStudentPassedCourses(student.getStudentId());

        Map<Long, List<EduCourseCompetency>> grouped = new LinkedHashMap<>();
        for (EduCourseCompetency row : rows)
        {
            if (row.getSkillTagId() == null)
            {
                continue;
            }
            grouped.computeIfAbsent(row.getSkillTagId(), key -> new ArrayList<>()).add(row);
        }

        List<SkillProfileVO> profiles = new ArrayList<>();
        for (Map.Entry<Long, List<EduCourseCompetency>> entry : grouped.entrySet())
        {
            List<EduCourseCompetency> items = entry.getValue();
            items.sort(Comparator.comparing(EduCourseCompetency::getScore,
                    Comparator.nullsLast(Comparator.<BigDecimal>reverseOrder())));
            List<EduCourseCompetency> top = items.size() > TOP_COURSES_PER_SKILL
                    ? items.subList(0, TOP_COURSES_PER_SKILL) : items;

            BigDecimal scoreSum = BigDecimal.ZERO;
            int scored = 0;
            for (EduCourseCompetency item : top)
            {
                if (item.getScore() != null)
                {
                    scoreSum = scoreSum.add(item.getScore());
                    scored++;
                }
            }
            BigDecimal score = scored == 0 ? BigDecimal.ZERO
                    : scoreSum.divide(BigDecimal.valueOf(scored), SCORE_SCALE, RoundingMode.HALF_UP);

            EduCourseCompetency first = items.get(0);
            SkillProfileVO profile = new SkillProfileVO();
            profile.setSkillTagId(entry.getKey());
            profile.setName(first.getSkillName());
            profile.setCategory(first.getSkillCategory());
            profile.setScore(score);
            profile.setCourseCount(items.size());
            profile.setRationale(buildRationale(top, items.size()));
            profiles.add(profile);
        }

        profiles.sort(Comparator.comparing(SkillProfileVO::getScore,
                Comparator.nullsLast(Comparator.<BigDecimal>reverseOrder())));
        return profiles;
    }

    @Override
    public List<EduSkillTag> selectCompetencyPool(String schoolAbbr, String keyword)
    {
        return competencyMapper.selectPoolBySchool(trimToNull(schoolAbbr), trimToNull(keyword));
    }

    @Override
    public List<EduCourseCompetency> selectCourseCompetency(Long courseId)
    {
        if (courseId == null)
        {
            throw new ServiceException("课程ID不能为空");
        }
        return competencyMapper.selectByCourseId(courseId);
    }

    /**
     * 构造各学期 GPA 趋势（按学期代码升序，SQL 已排序）
     *
     * @param gradeRows 成绩行
     * @return 趋势点集合
     */
    private List<GpaTrendPointVO> buildGpaTrend(List<TermGradeRow> gradeRows)
    {
        Map<String, List<TermGradeRow>> grouped = new LinkedHashMap<>();
        for (TermGradeRow row : gradeRows)
        {
            String termCode = row.getTermCode();
            if (termCode == null || termCode.trim().isEmpty())
            {
                continue;
            }
            grouped.computeIfAbsent(termCode, key -> new ArrayList<>()).add(row);
        }
        List<GpaTrendPointVO> trend = new ArrayList<>();
        for (Map.Entry<String, List<TermGradeRow>> entry : grouped.entrySet())
        {
            String termName = entry.getValue().isEmpty() ? null : entry.getValue().get(0).getTermName();
            trend.add(new GpaTrendPointVO(entry.getKey(), termName, GpaCalculator.weightedGpa(entry.getValue())));
        }
        return trend;
    }

    /**
     * 学习规划状态汇总
     *
     * @param studentId 学生ID
     * @return 状态计数
     */
    private PlanSummaryVO buildPlanSummary(Long studentId)
    {
        PlanSummaryVO summary = new PlanSummaryVO();
        List<PlanStatusCountRow> rows = studentMapper.selectPlanStatusSummary(studentId);
        int total = 0;
        for (PlanStatusCountRow row : rows)
        {
            int count = row.getCnt() == null ? 0 : row.getCnt().intValue();
            total += count;
            String status = row.getStatus() == null ? "" : row.getStatus().trim().toUpperCase();
            if (PLAN_DRAFT.equals(status))
            {
                summary.setDraft(count);
            }
            else if (PLAN_VALIDATED.equals(status))
            {
                summary.setValidated(count);
            }
            else if (PLAN_SUBMITTED.equals(status))
            {
                summary.setSubmitted(count);
            }
            else if (PLAN_ARCHIVED.equals(status))
            {
                summary.setArchived(count);
            }
        }
        summary.setTotal(total);
        return summary;
    }

    /**
     * 生成可解释的画像依据
     *
     * @param top 参与聚合的课程关联行
     * @param totalCourses 该标签贡献课程总数
     * @return 依据文案
     */
    private String buildRationale(List<EduCourseCompetency> top, int totalCourses)
    {
        List<String> parts = new ArrayList<>();
        for (EduCourseCompetency item : top)
        {
            String course = formatCourse(item.getCourseCode(), item.getCourseTitle());
            String scoreText = item.getScore() == null ? "暂无评分"
                    : item.getScore().setScale(SCORE_SCALE, RoundingMode.HALF_UP).toPlainString();
            parts.add(course + "（关联度 " + scoreText + "）");
        }
        StringBuilder builder = new StringBuilder("基于已修课程 ");
        builder.append(String.join("、", parts));
        if (totalCourses > top.size())
        {
            builder.append(" 等 ").append(totalCourses).append(" 门课程");
        }
        builder.append(" 聚合");
        return builder.toString();
    }

    /**
     * 格式化课程提示文案
     *
     * @param code 课程代码
     * @param title 课程名称
     * @return 形如 “CSC1001 程序设计”
     */
    private String formatCourse(String code, String title)
    {
        String safeCode = code == null ? "" : code.trim();
        String safeTitle = title == null ? "" : title.trim();
        if (safeCode.isEmpty())
        {
            return safeTitle;
        }
        return safeTitle.isEmpty() ? safeCode : safeCode + " " + safeTitle;
    }

    /**
     * 空串转 null（避免 mapper 中拼出 like '%%'）
     *
     * @param value 原值
     * @return 去空后的值
     */
    private String trimToNull(String value)
    {
        if (value == null || value.trim().isEmpty())
        {
            return null;
        }
        return value.trim();
    }
}
