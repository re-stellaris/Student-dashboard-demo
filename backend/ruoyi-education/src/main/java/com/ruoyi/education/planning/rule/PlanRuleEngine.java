package com.ruoyi.education.planning.rule;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import com.ruoyi.education.common.EducationConstants;
import com.ruoyi.education.common.PrerequisiteGraph;
import com.ruoyi.education.common.enums.PlanItemStatus;
import com.ruoyi.education.common.enums.RequirementType;
import com.ruoyi.education.planning.dto.PlanItemValidation;
import com.ruoyi.education.planning.dto.PlanValidationResult;
import com.ruoyi.education.planning.dto.RecommendedCourse;

/**
 * 选课规划规则引擎：纯计算，无 Spring、无 IO、无数据库，可直接单元测试。
 *
 * <p>规则顺序严格对齐 api-contract.md 4.1：</p>
 * <ol>
 *   <li>读取已通过课程（由调用方放入 {@link PlanRuleInput#getPassedCourseIds()}）；</li>
 *   <li>先修依赖图判定（组内 AND、组间 OR，见 {@link PrerequisiteGraph}）；</li>
 *   <li>培养方案未修筛选（已修读 / 不在方案内）；</li>
 *   <li>学分上限 / 开课学期 / 时间冲突；</li>
 *   <li>为每门课程生成中文可解释说明（reason）与校验信息（validationMessage）。</li>
 * </ol>
 *
 * @author ruoyi
 */
public class PlanRuleEngine
{
    /** 无先修要求时的说明前缀 */
    private static final String REASON_MISSING_PREREQ = "缺少先修课程：";

    /** 不在培养方案内的说明 */
    private static final String REASON_NOT_IN_CURRICULUM = "不在本专业培养方案内，按自由选修处理";

    /** 培养方案条目缺失时的兜底标签 */
    private static final String LABEL_CURRICULUM_COURSE = "培养方案课程";

    /** 已修读说明 */
    private static final String REASON_ALREADY_PASSED = "已通过该课程，无需重复修读";

    /**
     * 计算规划校验结果。
     *
     * @param input 规则输入，{@code null} 视为空规划
     * @return 校验结果（永不为 {@code null}）
     */
    public PlanValidationResult evaluate(PlanRuleInput input)
    {
        PlanRuleInput data = input == null ? new PlanRuleInput() : input;
        BigDecimal unitLimit = data.getUnitLimit() == null ? EducationConstants.DEFAULT_UNIT_LIMIT : data.getUnitLimit();
        List<PlanCourse> courses = data.getCourses() == null ? Collections.<PlanCourse>emptyList() : data.getCourses();
        Map<Long, PlanCourse> courseIndex = indexCourses(courses, data.getReferenceCourses());
        Map<Long, CurriculumRule> rulesByCourse = indexRules(data.getCurriculumRules());
        Map<Long, Map<Integer, List<Long>>> grouped = groupEdges(data.getPrereqEdges());
        Set<Long> passed = data.getPassedCourseIds() == null ? Collections.<Long>emptySet()
                : data.getPassedCourseIds();
        Set<Long> offered = data.getOfferedCourseIds();
        Map<Long, List<String>> schedules = data.getScheduleSlots();

        List<String> messages = new ArrayList<>();
        List<Long> cycles = PrerequisiteGraph.detectCycles(grouped);
        if (!cycles.isEmpty())
        {
            messages.add("先修关系存在环：" + joinCodes(cycles, courseIndex));
        }

        List<PlanItemValidation> items = new ArrayList<>(courses.size());
        BigDecimal totalUnits = BigDecimal.ZERO;
        Map<String, String> slotOwner = new LinkedHashMap<>();

        for (PlanCourse course : courses)
        {
            if (course == null || course.getCourseId() == null)
            {
                continue;
            }
            Long courseId = course.getCourseId();
            totalUnits = totalUnits.add(EducationConstants.nullToZero(course.getUnits()));

            PlanItemValidation item = new PlanItemValidation();
            item.setCourseId(courseId);
            item.setCode(course.getCode());
            item.setTitle(course.getTitle());
            item.setUnits(course.getUnits());

            String status = PlanItemStatus.VALID.getCode();
            String message = "";
            String reason;

            if (passed.contains(courseId))
            {
                // ③ 培养方案未修筛选：已修读课程不再重复规划
                status = PlanItemStatus.INVALID.getCode();
                reason = REASON_ALREADY_PASSED;
                message = "课程成绩已通过";
            }
            else
            {
                CurriculumRule rule = rulesByCourse.get(courseId);
                List<Long> missing = PrerequisiteGraph.missingPrerequisiteIds(grouped, courseId, passed);
                if (!missing.isEmpty())
                {
                    // ② 先修依赖图判定
                    status = PlanItemStatus.INVALID.getCode();
                    reason = REASON_MISSING_PREREQ + joinTitlesAndCodes(missing, courseIndex);
                    message = "缺少先修课程 " + joinCodes(missing, courseIndex);
                }
                else
                {
                    String baseReason = baseReason(rule);
                    List<Long> satisfied = PrerequisiteGraph.satisfiedGroup(grouped, courseId, passed);
                    if (satisfied.isEmpty())
                    {
                        reason = baseReason;
                    }
                    else
                    {
                        reason = baseReason + "，且已通过先修课程 " + joinCodes(satisfied, courseIndex);
                    }
                    if (rule == null)
                    {
                        status = PlanItemStatus.WARN.getCode();
                        message = "课程不在本专业培养方案中";
                    }
                }
            }

            // ④ 开课学期
            if (offered != null && !offered.contains(courseId))
            {
                if (!PlanItemStatus.INVALID.getCode().equals(status))
                {
                    reason = appendClause(reason, "目标学期未开课");
                    if (PlanItemStatus.VALID.getCode().equals(status))
                    {
                        status = PlanItemStatus.WARN.getCode();
                    }
                }
                message = appendMessage(message, "目标学期未开课");
            }

            // ④ 时间冲突（只统计仍可选的课程，避免冲突被已失效课程放大）
            List<String> slots = schedules == null ? null : schedules.get(courseId);
            if (slots != null && !slots.isEmpty())
            {
                String conflict = findConflict(slots, slotOwner);
                if (conflict != null)
                {
                    if (!PlanItemStatus.INVALID.getCode().equals(status))
                    {
                        reason = appendClause(reason, "上课时间冲突");
                        if (PlanItemStatus.VALID.getCode().equals(status))
                        {
                            status = PlanItemStatus.WARN.getCode();
                        }
                    }
                    message = appendMessage(message, "上课时间与已选课程冲突：" + conflict);
                }
                else if (!PlanItemStatus.INVALID.getCode().equals(status))
                {
                    String label = courseLabel(course);
                    for (String slot : slots)
                    {
                        slotOwner.putIfAbsent(slot, label);
                    }
                }
            }

            item.setReason(EducationConstants.truncate(reason, EducationConstants.MAX_REASON_LENGTH));
            item.setValidationStatus(status);
            item.setValidationMessage(EducationConstants.truncate(message, EducationConstants.MAX_MESSAGE_LENGTH));
            items.add(item);
        }

        if (items.isEmpty())
        {
            messages.add("未选择任何课程");
        }

        // ④ 学分上限
        if (totalUnits.compareTo(unitLimit) > 0)
        {
            messages.add("超出学分上限 " + EducationConstants.formatUnits(unitLimit) + "：当前 "
                    + EducationConstants.formatUnits(totalUnits));
        }
        if (totalUnits.compareTo(EducationConstants.MAX_UNITS) > 0)
        {
            messages.add("超出单学期学分硬上限 " + EducationConstants.formatUnits(EducationConstants.MAX_UNITS)
                    + "：当前 " + EducationConstants.formatUnits(totalUnits));
        }

        PlanValidationResult result = new PlanValidationResult();
        result.setTermId(data.getTermId());
        result.setUnitLimit(unitLimit);
        result.setTotalUnits(totalUnits);
        result.setState(computeState(items, messages, !cycles.isEmpty()));
        result.setMessages(messages);
        result.setItems(items);
        return result;
    }

    /**
     * 规则推荐：优先推荐培养方案内、先修已满足、目标学期开课的课程。
     *
     * @param input 规则输入（{@code courses} 为候选池）
     * @param limit 推荐条数上限（小于等于 0 时取默认值，超出硬上限时截断）
     * @return 推荐课程列表（永不为 {@code null}）
     */
    public List<RecommendedCourse> recommend(PlanRuleInput input, int limit)
    {
        PlanRuleInput data = input == null ? new PlanRuleInput() : input;
        int max = limit <= 0 ? EducationConstants.DEFAULT_RECOMMEND_LIMIT
                : Math.min(limit, EducationConstants.MAX_RECOMMEND_LIMIT);
        BigDecimal unitLimit = data.getUnitLimit() == null ? EducationConstants.DEFAULT_UNIT_LIMIT : data.getUnitLimit();
        List<PlanCourse> courses = data.getCourses() == null ? Collections.<PlanCourse>emptyList() : data.getCourses();
        Map<Long, PlanCourse> courseIndex = indexCourses(courses, data.getReferenceCourses());
        Map<Long, CurriculumRule> rulesByCourse = indexRules(data.getCurriculumRules());
        Map<Long, Map<Integer, List<Long>>> grouped = groupEdges(data.getPrereqEdges());
        Set<Long> passed = data.getPassedCourseIds() == null ? Collections.<Long>emptySet()
                : data.getPassedCourseIds();
        Set<Long> offered = data.getOfferedCourseIds();
        boolean hasCurriculum = !rulesByCourse.isEmpty();

        List<Candidate> candidates = new ArrayList<>();
        for (PlanCourse course : courses)
        {
            if (course == null || course.getCourseId() == null || passed.contains(course.getCourseId()))
            {
                continue;
            }
            CurriculumRule rule = rulesByCourse.get(course.getCourseId());
            if (hasCurriculum && rule == null)
            {
                // 有培养方案时只推荐方案内课程，避免推荐“无关课程”
                continue;
            }
            List<Long> missing = PrerequisiteGraph.missingPrerequisiteIds(grouped, course.getCourseId(), passed);
            boolean isOffered = offered == null || offered.contains(course.getCourseId());
            candidates.add(new Candidate(course, rule, missing, isOffered));
        }
        candidates.sort(PlanRuleEngine::compareCandidates);

        List<RecommendedCourse> recommended = new ArrayList<>();
        BigDecimal plannedUnits = BigDecimal.ZERO;
        for (Candidate candidate : candidates)
        {
            if (recommended.size() >= max)
            {
                break;
            }
            BigDecimal units = EducationConstants.nullToZero(candidate.course.getUnits());
            if (!recommended.isEmpty() && plannedUnits.add(units).compareTo(unitLimit) > 0)
            {
                // 学分预算已满：跳过会超限的课程，继续寻找更小的课
                continue;
            }
            plannedUnits = plannedUnits.add(units);

            RecommendedCourse row = new RecommendedCourse();
            row.setCourseId(candidate.course.getCourseId());
            row.setCode(candidate.course.getCode());
            row.setTitle(candidate.course.getTitle());
            row.setUnits(candidate.course.getUnits());
            row.setMissingPrereqs(codesOf(candidate.missing, courseIndex));

            String reason;
            if (!candidate.missing.isEmpty())
            {
                reason = REASON_MISSING_PREREQ + joinTitlesAndCodes(candidate.missing, courseIndex);
            }
            else
            {
                List<Long> satisfied = PrerequisiteGraph.satisfiedGroup(grouped, candidate.course.getCourseId(),
                        passed);
                String baseReason = baseReason(candidate.rule);
                reason = satisfied.isEmpty() ? baseReason
                        : baseReason + "，且已通过先修课程 " + joinCodes(satisfied, courseIndex);
            }
            if (!candidate.offered)
            {
                reason = appendClause(reason, "目标学期未开课");
            }
            row.setReason(EducationConstants.truncate(reason, EducationConstants.MAX_REASON_LENGTH));
            recommended.add(row);
        }
        return recommended;
    }

    /**
     * 把先修边按 {@code group_no} 分组：courseId -> groupNo -> prereqCourseIds。
     *
     * <p>自依赖行直接丢弃（DDL 有 check，此处为规则侧兜底）。</p>
     *
     * @param edges 先修边
     * @return 分组结果
     */
    public static Map<Long, Map<Integer, List<Long>>> groupEdges(List<PrereqEdge> edges)
    {
        Map<Long, Map<Integer, List<Long>>> grouped = new LinkedHashMap<>();
        if (edges == null || edges.isEmpty())
        {
            return grouped;
        }
        for (PrereqEdge edge : edges)
        {
            if (edge == null || edge.getCourseId() == null || edge.getPrereqCourseId() == null)
            {
                continue;
            }
            if (edge.getCourseId().equals(edge.getPrereqCourseId()))
            {
                continue;
            }
            int groupNo = edge.getGroupNo() == null ? 1 : edge.getGroupNo().intValue();
            List<Long> prereqIds = grouped.computeIfAbsent(edge.getCourseId(), key -> new LinkedHashMap<>())
                    .computeIfAbsent(Integer.valueOf(groupNo), key -> new ArrayList<>());
            if (!prereqIds.contains(edge.getPrereqCourseId()))
            {
                prereqIds.add(edge.getPrereqCourseId());
            }
        }
        return grouped;
    }

    /**
     * 先修环检测（迭代实现）。
     *
     * @param edges 先修边
     * @return 参与环的课程ID（升序）
     */
    public static List<Long> detectCycles(List<PrereqEdge> edges)
    {
        return PrerequisiteGraph.detectCycles(groupEdges(edges));
    }

    // ------------------------------------------------------------------
    // 内部实现
    // ------------------------------------------------------------------

    private static String computeState(List<PlanItemValidation> items, List<String> messages, boolean cycleDetected)
    {
        if (cycleDetected)
        {
            return EducationConstants.VALIDATION_STATE_FAIL;
        }
        boolean hasWarn = false;
        for (PlanItemValidation item : items)
        {
            if (PlanItemStatus.INVALID.getCode().equals(item.getValidationStatus()))
            {
                return EducationConstants.VALIDATION_STATE_FAIL;
            }
            if (PlanItemStatus.WARN.getCode().equals(item.getValidationStatus()))
            {
                hasWarn = true;
            }
        }
        if (hasWarn || !messages.isEmpty())
        {
            return EducationConstants.VALIDATION_STATE_WARN;
        }
        return EducationConstants.VALIDATION_STATE_PASS;
    }

    private static Map<Long, PlanCourse> indexCourses(List<PlanCourse> courses, List<PlanCourse> referenceCourses)
    {
        Map<Long, PlanCourse> index = new LinkedHashMap<>();
        if (referenceCourses != null)
        {
            for (PlanCourse course : referenceCourses)
            {
                if (course != null && course.getCourseId() != null)
                {
                    index.put(course.getCourseId(), course);
                }
            }
        }
        if (courses != null)
        {
            for (PlanCourse course : courses)
            {
                if (course != null && course.getCourseId() != null)
                {
                    // 本次规划内课程优先（以最新学分为准）
                    index.put(course.getCourseId(), course);
                }
            }
        }
        return index;
    }

    private static Map<Long, CurriculumRule> indexRules(List<CurriculumRule> rules)
    {
        Map<Long, CurriculumRule> index = new LinkedHashMap<>();
        if (rules == null)
        {
            return index;
        }
        for (CurriculumRule rule : rules)
        {
            if (rule != null && rule.getCourseId() != null && !index.containsKey(rule.getCourseId()))
            {
                index.put(rule.getCourseId(), rule);
            }
        }
        return index;
    }

    /**
     * 生成培养方案侧的基础说明，如“专业必修（School Package）”。
     */
    private static String baseReason(CurriculumRule rule)
    {
        if (rule == null)
        {
            return REASON_NOT_IN_CURRICULUM;
        }
        RequirementType type = RequirementType.from(rule.getRequirementType());
        String label = type == null ? LABEL_CURRICULUM_COURSE : type.getLabel();
        String section = trimToNull(rule.getSection());
        return section == null ? label : label + "（" + section + "）";
    }

    private static String appendClause(String reason, String clause)
    {
        if (reason == null || reason.isEmpty())
        {
            return clause;
        }
        return reason + "；" + clause;
    }

    private static String appendMessage(String message, String addition)
    {
        if (addition == null || addition.isEmpty())
        {
            return message == null ? "" : message;
        }
        if (message == null || message.isEmpty())
        {
            return addition;
        }
        return message + "；" + addition;
    }

    private static String findConflict(List<String> slots, Map<String, String> slotOwner)
    {
        for (String slot : slots)
        {
            if (slot == null)
            {
                continue;
            }
            String owner = slotOwner.get(slot);
            if (owner != null)
            {
                return owner + "（" + slot + "）";
            }
        }
        return null;
    }

    /**
     * 缺少先修课的可读描述：{@code 数据结构（CSC3100）}。
     */
    private static String joinTitlesAndCodes(List<Long> courseIds, Map<Long, PlanCourse> courseIndex)
    {
        if (courseIds == null || courseIds.isEmpty())
        {
            return "";
        }
        List<String> parts = new ArrayList<>(courseIds.size());
        for (Long courseId : courseIds)
        {
            PlanCourse course = courseIndex.get(courseId);
            String code = course == null ? null : trimToNull(course.getCode());
            String title = course == null ? null : trimToNull(course.getTitle());
            if (title != null && code != null)
            {
                parts.add(title + "（" + code + "）");
            }
            else if (code != null)
            {
                parts.add(code);
            }
            else if (title != null)
            {
                parts.add(title);
            }
            else
            {
                parts.add("#" + courseId);
            }
        }
        return String.join("、", parts);
    }

    private static String joinCodes(List<Long> courseIds, Map<Long, PlanCourse> courseIndex)
    {
        return String.join("、", codesOf(courseIds, courseIndex));
    }

    private static List<String> codesOf(List<Long> courseIds, Map<Long, PlanCourse> courseIndex)
    {
        List<String> codes = new ArrayList<>();
        if (courseIds == null)
        {
            return codes;
        }
        for (Long courseId : courseIds)
        {
            PlanCourse course = courseIndex.get(courseId);
            String code = course == null ? null : trimToNull(course.getCode());
            codes.add(code == null ? "#" + courseId : code);
        }
        return codes;
    }

    private static String courseLabel(PlanCourse course)
    {
        String code = course == null ? null : trimToNull(course.getCode());
        if (code != null)
        {
            return code;
        }
        String title = course == null ? null : trimToNull(course.getTitle());
        return title == null ? "该课程" : title;
    }

    private static String trimToNull(String value)
    {
        if (value == null)
        {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    /** 培养方案要求类型的推荐优先级，数值越小越优先 */
    private static int requirementPriority(CurriculumRule rule)
    {
        RequirementType type = rule == null ? null : RequirementType.from(rule.getRequirementType());
        if (type == null)
        {
            return 6;
        }
        switch (type)
        {
            case REQUIRED:
                return 0;
            case GE:
                return 1;
            case BREADTH:
                return 2;
            case DEPTH:
                return 3;
            case STREAM:
                return 4;
            case ELECTIVE:
                return 5;
            default:
                return 6;
        }
    }

    /**
     * 推荐排序：先修已满足 > 培养方案优先级 > 目标学期开课 > 课程层级 > 课程代码。
     */
    private static int compareCandidates(Candidate left, Candidate right)
    {
        int result = Integer.compare(left.missing.isEmpty() ? 0 : 1, right.missing.isEmpty() ? 0 : 1);
        if (result != 0)
        {
            return result;
        }
        result = Integer.compare(requirementPriority(left.rule), requirementPriority(right.rule));
        if (result != 0)
        {
            return result;
        }
        result = Integer.compare(left.offered ? 0 : 1, right.offered ? 0 : 1);
        if (result != 0)
        {
            return result;
        }
        Integer leftLevel = left.course.getLevel();
        Integer rightLevel = right.course.getLevel();
        result = Integer.compare(leftLevel == null ? Integer.MAX_VALUE : leftLevel.intValue(),
                rightLevel == null ? Integer.MAX_VALUE : rightLevel.intValue());
        if (result != 0)
        {
            return result;
        }
        String leftCode = left.course.getCode() == null ? "" : left.course.getCode();
        String rightCode = right.course.getCode() == null ? "" : right.course.getCode();
        return leftCode.compareTo(rightCode);
    }

    /**
     * 推荐候选（内部使用）。
     */
    private static final class Candidate
    {
        private final PlanCourse course;

        private final CurriculumRule rule;

        private final List<Long> missing;

        private final boolean offered;

        private Candidate(PlanCourse course, CurriculumRule rule, List<Long> missing, boolean offered)
        {
            this.course = course;
            this.rule = rule;
            this.missing = missing == null ? Collections.<Long>emptyList() : missing;
            this.offered = offered;
        }
    }
}
