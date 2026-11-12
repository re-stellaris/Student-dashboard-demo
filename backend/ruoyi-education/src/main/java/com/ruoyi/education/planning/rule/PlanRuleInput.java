package com.ruoyi.education.planning.rule;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 规划规则计算的输入（纯数据）。
 *
 * <p>方案顺序见 api-contract.md 4.1：
 * ① 已通过课程 → ② 先修依赖图 → ③ 培养方案未修筛选 → ④ 学分上限 / 开课学期 / 时间冲突
 * → ⑤ 生成可解释说明。所有外部数据都由 {@code planning.spi} 端口一次性装配进来，
 * 规则引擎本身不做任何 IO，因而可脱离 Spring 与数据库单元测试。</p>
 *
 * @author ruoyi
 */
public class PlanRuleInput
{
    /** 目标学期 */
    private Long termId;

    /** 学分上限（null 时取 EducationConstants.DEFAULT_UNIT_LIMIT） */
    private BigDecimal unitLimit;

    /** 本次规划的课程（按学生提交顺序） */
    private List<PlanCourse> courses = new ArrayList<>();

    /** 先修边（至少覆盖本次规划用到的课程） */
    private List<PrereqEdge> prereqEdges = new ArrayList<>();

    /** 参考课程快照（先修课等未在本次规划中、但需要显示代码与名称的课程） */
    private List<PlanCourse> referenceCourses = new ArrayList<>();

    /** 已通过课程 ID */
    private Set<Long> passedCourseIds = new LinkedHashSet<>();

    /** 适用培养方案条目 */
    private List<CurriculumRule> curriculumRules = new ArrayList<>();

    /** 目标学期实际开课课程 ID；null 表示“无开课数据，跳过开课校验” */
    private Set<Long> offeredCourseIds;

    /** 目标学期各课程上课时间槽（courseId -> slot 列表）；null 表示“无排课数据，跳过冲突校验” */
    private Map<Long, List<String>> scheduleSlots;

    public PlanRuleInput()
    {
    }

    public Long getTermId()
    {
        return termId;
    }

    public PlanRuleInput setTermId(Long termId)
    {
        this.termId = termId;
        return this;
    }

    public BigDecimal getUnitLimit()
    {
        return unitLimit;
    }

    public PlanRuleInput setUnitLimit(BigDecimal unitLimit)
    {
        this.unitLimit = unitLimit;
        return this;
    }

    public List<PlanCourse> getCourses()
    {
        return courses;
    }

    public PlanRuleInput setCourses(List<PlanCourse> courses)
    {
        this.courses = courses == null ? new ArrayList<>() : courses;
        return this;
    }

    public List<PrereqEdge> getPrereqEdges()
    {
        return prereqEdges;
    }

    public PlanRuleInput setPrereqEdges(List<PrereqEdge> prereqEdges)
    {
        this.prereqEdges = prereqEdges == null ? new ArrayList<>() : prereqEdges;
        return this;
    }

    public List<PlanCourse> getReferenceCourses()
    {
        return referenceCourses;
    }

    public PlanRuleInput setReferenceCourses(List<PlanCourse> referenceCourses)
    {
        this.referenceCourses = referenceCourses == null ? new ArrayList<>() : referenceCourses;
        return this;
    }

    public Set<Long> getPassedCourseIds()
    {
        return passedCourseIds;
    }
    public PlanRuleInput setPassedCourseIds(Set<Long> passedCourseIds)
    {
        this.passedCourseIds = passedCourseIds == null ? new LinkedHashSet<>() : passedCourseIds;
        return this;
    }

    public List<CurriculumRule> getCurriculumRules()
    {
        return curriculumRules;
    }

    public PlanRuleInput setCurriculumRules(List<CurriculumRule> curriculumRules)
    {
        this.curriculumRules = curriculumRules == null ? new ArrayList<>() : curriculumRules;
        return this;
    }

    public Set<Long> getOfferedCourseIds()
    {
        return offeredCourseIds;
    }

    public PlanRuleInput setOfferedCourseIds(Set<Long> offeredCourseIds)
    {
        this.offeredCourseIds = offeredCourseIds;
        return this;
    }

    public Map<Long, List<String>> getScheduleSlots()
    {
        return scheduleSlots;
    }

    public PlanRuleInput setScheduleSlots(Map<Long, List<String>> scheduleSlots)
    {
        this.scheduleSlots = scheduleSlots;
        return this;
    }

    /**
     * 只读视图，便于日志与测试断言。
     *
     * @return 已通过课程 ID 的只读集合
     */
    public Set<Long> passedView()
    {
        return Collections.unmodifiableSet(passedCourseIds);
    }
}
