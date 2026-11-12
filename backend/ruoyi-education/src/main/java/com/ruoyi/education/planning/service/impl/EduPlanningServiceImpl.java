package com.ruoyi.education.planning.service.impl;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.education.common.EducationConstants;
import com.ruoyi.education.common.enums.PlanItemStatus;
import com.ruoyi.education.common.enums.PlanStatus;
import com.ruoyi.education.planning.domain.EduStudyPlan;
import com.ruoyi.education.planning.domain.EduStudyPlanItem;
import com.ruoyi.education.planning.dto.PlanDetailVO;
import com.ruoyi.education.planning.dto.PlanItemBody;
import com.ruoyi.education.planning.dto.PlanItemValidation;
import com.ruoyi.education.planning.dto.PlanRecommendBody;
import com.ruoyi.education.planning.dto.PlanSaveBody;
import com.ruoyi.education.planning.dto.PlanValidateBody;
import com.ruoyi.education.planning.dto.PlanValidationResult;
import com.ruoyi.education.planning.dto.RecommendedCourse;
import com.ruoyi.education.planning.mapper.EduStudyPlanItemMapper;
import com.ruoyi.education.planning.mapper.EduStudyPlanMapper;
import com.ruoyi.education.planning.rule.CurriculumRule;
import com.ruoyi.education.planning.rule.PlanCourse;
import com.ruoyi.education.planning.rule.PlanRuleEngine;
import com.ruoyi.education.planning.rule.PlanRuleInput;
import com.ruoyi.education.planning.rule.PrereqEdge;
import com.ruoyi.education.planning.service.IEduPlanningService;
import com.ruoyi.education.planning.spi.CourseCatalogPort;
import com.ruoyi.education.planning.spi.CurriculumPort;
import com.ruoyi.education.planning.spi.OfferingPort;
import com.ruoyi.education.planning.spi.StudentDirectoryPort;
import com.ruoyi.education.planning.spi.StudentProfile;
import com.ruoyi.education.planning.spi.TranscriptPort;

/**
 * 选课规划服务实现。
 *
 * <p><b>协作方装配</b>：本类通过构造器注入 {@link StudentDirectoryPort}、
 * {@link CourseCatalogPort}、{@link TranscriptPort}、{@link CurriculumPort}、
 * {@link OfferingPort} 五个只读端口，Spring 装配集中在此。
 * 默认实现为 {@code planning.spi.DefaultPlanningReadPorts}（@Primary，纯读）。
 * 归属域可提供自己的 @Primary Bean 替换任一端口，本类无需改动。</p>
 *
 * <p><b>数据边界</b>：所有方法只处理当前登录学生的规划；
 * 越权访问统一抛 {@code ServiceException("无权访问该资源")}，不泄露资源是否存在。</p>
 *
 * @author ruoyi
 */
@Service
public class EduPlanningServiceImpl implements IEduPlanningService
{
    private final EduStudyPlanMapper planMapper;

    private final EduStudyPlanItemMapper itemMapper;

    private final StudentDirectoryPort studentDirectoryPort;

    private final CourseCatalogPort courseCatalogPort;

    private final TranscriptPort transcriptPort;

    private final CurriculumPort curriculumPort;

    private final OfferingPort offeringPort;

    /** 纯计算规则引擎（无状态，可直接复用） */
    private final PlanRuleEngine ruleEngine = new PlanRuleEngine();

    public EduPlanningServiceImpl(EduStudyPlanMapper planMapper, EduStudyPlanItemMapper itemMapper,
            StudentDirectoryPort studentDirectoryPort, CourseCatalogPort courseCatalogPort,
            TranscriptPort transcriptPort, CurriculumPort curriculumPort, OfferingPort offeringPort)
    {
        this.planMapper = planMapper;
        this.itemMapper = itemMapper;
        this.studentDirectoryPort = studentDirectoryPort;
        this.courseCatalogPort = courseCatalogPort;
        this.transcriptPort = transcriptPort;
        this.curriculumPort = curriculumPort;
        this.offeringPort = offeringPort;
    }

    // ------------------------------------------------------------------
    // 查询
    // ------------------------------------------------------------------

    @Override
    public List<EduStudyPlan> selectPlanList(EduStudyPlan query)
    {
        Long studentId = currentStudentId();
        EduStudyPlan condition = query == null ? new EduStudyPlan() : query;
        // 身份只取自登录态，忽略前端可能传入的 studentId
        condition.setStudentId(studentId);
        return planMapper.selectPlanList(condition);
    }

    @Override
    public PlanDetailVO selectPlanDetail(Long planId)
    {
        Long studentId = currentStudentId();
        EduStudyPlan plan = requireOwnedPlan(planId, studentId);
        return new PlanDetailVO(plan, itemMapper.selectItemsByPlanId(plan.getPlanId()));
    }

    // ------------------------------------------------------------------
    // 写入
    // ------------------------------------------------------------------

    @Override
    @Transactional
    public EduStudyPlan createPlan(PlanSaveBody body)
    {
        Long studentId = currentStudentId();
        if (body == null || body.getTermId() == null)
        {
            throw new ServiceException("目标学期不能为空");
        }
        if (planMapper.selectPlanByStudentAndTerm(studentId, body.getTermId()) != null)
        {
            throw new ServiceException("该学期已存在学习规划，请直接编辑");
        }
        EduStudyPlan plan = new EduStudyPlan();
        plan.setStudentId(studentId);
        plan.setTermId(body.getTermId());
        String title = trimToNull(body.getTitle());
        plan.setTitle(title == null ? "学习规划" : title);
        plan.setStatus(PlanStatus.DRAFT.getCode());
        plan.setTotalUnits(BigDecimal.ZERO);
        plan.setUnitLimit(EducationConstants.DEFAULT_UNIT_LIMIT);
        plan.setValidationState("");
        plan.setValidationNote("");
        plan.setDelFlag(EducationConstants.DEL_FLAG_NORMAL);
        plan.setCreateBy(currentUsername());
        planMapper.insertPlan(plan);
        return plan;
    }

    @Override
    @Transactional
    public PlanValidationResult savePlanItems(Long planId, List<PlanItemBody> items)
    {
        Long studentId = currentStudentId();
        EduStudyPlan plan = requireOwnedPlan(planId, studentId);
        ensureEditable(plan);
        List<Long> courseIds = normalizeCourseIds(items);
        PlanValidationResult result = evaluate(plan, studentId, courseIds);
        syncValidationStatus(plan, result);
        persistValidation(plan, result);
        return result;
    }

    @Override
    public PlanValidationResult validate(PlanValidateBody body)
    {
        Long studentId = currentStudentId();
        if (body == null || body.getTermId() == null)
        {
            throw new ServiceException("目标学期不能为空");
        }
        EduStudyPlan plan = new EduStudyPlan();
        plan.setStudentId(studentId);
        plan.setTermId(body.getTermId());
        // 若该学期已有草稿，沿用其学分上限，保证试算与落库结果一致
        EduStudyPlan existing = planMapper.selectPlanByStudentAndTerm(studentId, body.getTermId());
        plan.setUnitLimit(existing == null || existing.getUnitLimit() == null ? EducationConstants.DEFAULT_UNIT_LIMIT
                : existing.getUnitLimit());
        return evaluate(plan, studentId, distinct(body.getCourseIds()));
    }

    @Override
    @Transactional
    public PlanValidationResult persistValidate(Long planId)
    {
        Long studentId = currentStudentId();
        EduStudyPlan plan = requireOwnedPlan(planId, studentId);
        ensureEditable(plan);
        PlanValidationResult result = evaluate(plan, studentId, loadItemCourseIds(planId));
        syncValidationStatus(plan, result);
        persistValidation(plan, result);
        return result;
    }

    @Override
    public List<RecommendedCourse> recommend(PlanRecommendBody body)
    {
        Long studentId = currentStudentId();
        if (body == null || body.getTermId() == null)
        {
            throw new ServiceException("目标学期不能为空");
        }
        List<CurriculumRule> rules = loadCurriculumRules(studentId);
        List<PlanCourse> pool;
        if (rules.isEmpty())
        {
            // 没有可用培养方案时退回全部课程，仍按先修/开课/学分预算收敛
            pool = courseCatalogPort.loadCourses(null);
        }
        else
        {
            Set<Long> curriculumCourseIds = new LinkedHashSet<>();
            for (CurriculumRule rule : rules)
            {
                if (rule != null && rule.getCourseId() != null)
                {
                    curriculumCourseIds.add(rule.getCourseId());
                }
            }
            pool = curriculumCourseIds.isEmpty() ? Collections.<PlanCourse>emptyList()
                    : courseCatalogPort.loadCourses(curriculumCourseIds);
        }
        Set<Long> courseIds = new LinkedHashSet<>();
        Map<Long, PlanCourse> poolIndex = new LinkedHashMap<>();
        for (PlanCourse course : pool)
        {
            if (course != null && course.getCourseId() != null)
            {
                courseIds.add(course.getCourseId());
                poolIndex.put(course.getCourseId(), course);
            }
        }
        List<PrereqEdge> prereqEdges = courseCatalogPort.loadPrereqEdges(courseIds);
        PlanRuleInput input = new PlanRuleInput()
                .setTermId(body.getTermId())
                .setUnitLimit(EducationConstants.DEFAULT_UNIT_LIMIT)
                .setCourses(pool)
                .setPrereqEdges(prereqEdges)
                .setReferenceCourses(loadReferenceCourses(prereqEdges, poolIndex))
                .setPassedCourseIds(transcriptPort.loadPassedCourseIds(studentId))
                .setCurriculumRules(rules)
                .setOfferedCourseIds(loadOfferedCourseIds(body.getTermId()))
                .setScheduleSlots(offeringPort.loadScheduleSlots(body.getTermId()));
        int limit = body.getLimit() == null ? EducationConstants.DEFAULT_RECOMMEND_LIMIT : body.getLimit().intValue();
        return ruleEngine.recommend(input, limit);
    }

    @Override
    @Transactional
    public int submit(Long planId)
    {
        Long studentId = currentStudentId();
        EduStudyPlan plan = requireOwnedPlan(planId, studentId);
        if (PlanStatus.SUBMITTED.getCode().equals(plan.getStatus()))
        {
            throw new ServiceException("该规划已提交，请勿重复提交");
        }
        if (PlanStatus.ARCHIVED.getCode().equals(plan.getStatus()))
        {
            throw new ServiceException("该规划已归档，无法提交");
        }
        if (itemMapper.countItemsByPlanId(plan.getPlanId()) == 0)
        {
            throw new ServiceException("规划为空，请先选择课程");
        }
        PlanValidationResult result = evaluate(plan, studentId, loadItemCourseIds(plan.getPlanId()));
        if (EducationConstants.VALIDATION_STATE_FAIL.equals(result.getState()))
        {
            throw new ServiceException("规划未通过校验：" + joinMessages(result));
        }
        plan.setStatus(PlanStatus.SUBMITTED.getCode());
        persistValidation(plan, result);
        return 1;
    }

    @Override
    @Transactional
    public int deletePlans(Long[] planIds)
    {
        if (planIds == null || planIds.length == 0)
        {
            throw new ServiceException("请选择要删除的规划");
        }
        Long studentId = currentStudentId();
        for (Long planId : planIds)
        {
            requireOwnedPlan(planId, studentId);
        }
        return planMapper.deletePlanByIds(planIds);
    }

    // ------------------------------------------------------------------
    // 规则计算与持久化
    // ------------------------------------------------------------------

    /**
     * 按 api-contract.md 4.1 的顺序装配规则输入并计算校验结果。
     */
    private PlanValidationResult evaluate(EduStudyPlan plan, Long studentId, List<Long> courseIds)
    {
        List<Long> orderedIds = distinct(courseIds);
        List<PlanCourse> loaded = orderedIds.isEmpty() ? Collections.<PlanCourse>emptyList()
                : courseCatalogPort.loadCourses(orderedIds);
        Map<Long, PlanCourse> index = new LinkedHashMap<>();
        for (PlanCourse course : loaded)
        {
            if (course != null && course.getCourseId() != null)
            {
                index.put(course.getCourseId(), course);
            }
        }
        List<Long> unknown = new ArrayList<>();
        List<PlanCourse> courses = new ArrayList<>(orderedIds.size());
        for (Long courseId : orderedIds)
        {
            PlanCourse course = index.get(courseId);
            if (course == null)
            {
                unknown.add(courseId);
            }
            else
            {
                courses.add(course);
            }
        }
        if (!unknown.isEmpty())
        {
            throw new ServiceException("课程不存在或已停用：" + unknown);
        }
        List<PrereqEdge> prereqEdges = courseCatalogPort.loadPrereqEdges(orderedIds);
        List<PlanCourse> referenceCourses = loadReferenceCourses(prereqEdges, index);
        PlanRuleInput input = new PlanRuleInput()
                .setTermId(plan.getTermId())
                .setUnitLimit(plan.getUnitLimit() == null ? EducationConstants.DEFAULT_UNIT_LIMIT
                        : plan.getUnitLimit())
                .setCourses(courses)
                .setPrereqEdges(prereqEdges)
                .setReferenceCourses(referenceCourses)
                .setPassedCourseIds(transcriptPort.loadPassedCourseIds(studentId))
                .setCurriculumRules(loadCurriculumRules(studentId))
                .setOfferedCourseIds(loadOfferedCourseIds(plan.getTermId()))
                .setScheduleSlots(offeringPort.loadScheduleSlots(plan.getTermId()));
        return ruleEngine.evaluate(input);
    }

    /**
     * 无开课数据（空集合）时返回 {@code null}，表示“跳过开课校验”，避免整份规划被误标为未开课。
     */
    private Set<Long> loadOfferedCourseIds(Long termId)
    {
        Set<Long> offered = offeringPort.loadOfferedCourseIds(termId);
        return offered == null || offered.isEmpty() ? null : offered;
    }

    /**
     * 先修课可能不在本次规划内，单独载入其代码 / 名称，避免出现“缺少先修课程 #123”这类不可读提示。
     */
    private List<PlanCourse> loadReferenceCourses(List<PrereqEdge> prereqEdges, Map<Long, PlanCourse> known)
    {
        if (prereqEdges == null || prereqEdges.isEmpty())
        {
            return Collections.emptyList();
        }
        Set<Long> referenceIds = new LinkedHashSet<>();
        for (PrereqEdge edge : prereqEdges)
        {
            if (edge != null && edge.getPrereqCourseId() != null && !known.containsKey(edge.getPrereqCourseId()))
            {
                referenceIds.add(edge.getPrereqCourseId());
            }
        }
        return referenceIds.isEmpty() ? Collections.<PlanCourse>emptyList() : courseCatalogPort.loadCourses(referenceIds);
    }

    /**
     * 校验通过则草稿升级为 VALIDATED，校验不通过则退回 DRAFT；已提交 / 已归档状态不受影响。
     */
    private void syncValidationStatus(EduStudyPlan plan, PlanValidationResult result)
    {
        String status = plan.getStatus();
        if (PlanStatus.SUBMITTED.getCode().equals(status) || PlanStatus.ARCHIVED.getCode().equals(status))
        {
            return;
        }
        if (EducationConstants.VALIDATION_STATE_FAIL.equals(result.getState()))
        {
            plan.setStatus(PlanStatus.DRAFT.getCode());
        }
        else
        {
            plan.setStatus(PlanStatus.VALIDATED.getCode());
        }
    }

    /**
     * 覆盖式写入明细并回写规划主表的校验结论。
     */
    private void persistValidation(EduStudyPlan plan, PlanValidationResult result)
    {
        itemMapper.deleteItemsByPlanId(plan.getPlanId());
        List<EduStudyPlanItem> rows = toItems(plan.getPlanId(), result);
        if (!rows.isEmpty())
        {
            itemMapper.insertItemBatch(rows);
        }
        plan.setTotalUnits(result.getTotalUnits() == null ? BigDecimal.ZERO : result.getTotalUnits());
        plan.setUnitLimit(result.getUnitLimit());
        plan.setValidationState(result.getState());
        plan.setValidationNote(EducationConstants.truncate(joinMessages(result), EducationConstants.MAX_NOTE_LENGTH));
        plan.setUpdateBy(currentUsername());
        planMapper.updatePlan(plan);
    }

    private List<EduStudyPlanItem> toItems(Long planId, PlanValidationResult result)
    {
        List<EduStudyPlanItem> rows = new ArrayList<>();
        int sortOrder = 1;
        for (PlanItemValidation validation : result.getItems())
        {
            EduStudyPlanItem item = new EduStudyPlanItem();
            item.setPlanId(planId);
            item.setCourseId(validation.getCourseId());
            item.setReason(EducationConstants.truncate(validation.getReason(), EducationConstants.MAX_REASON_LENGTH));
            item.setValidationStatus(validation.getValidationStatus() == null ? PlanItemStatus.UNKNOWN.getCode()
                    : validation.getValidationStatus());
            item.setValidationMessage(
                    EducationConstants.truncate(validation.getValidationMessage(), EducationConstants.MAX_MESSAGE_LENGTH));
            item.setSortOrder(sortOrder++);
            rows.add(item);
        }
        return rows;
    }

    // ------------------------------------------------------------------
    // 内部工具
    // ------------------------------------------------------------------

    /**
     * 解析当前登录学生：SecurityUtils.getUserId() -> edu_student.sys_user_id。
     */
    private Long currentStudentId()
    {
        Long sysUserId = SecurityUtils.getUserId();
        Long studentId = studentDirectoryPort.findStudentIdBySysUserId(sysUserId);
        if (studentId == null)
        {
            throw new ServiceException(EducationConstants.MSG_STUDENT_NOT_BOUND);
        }
        return studentId;
    }

    /**
     * 资源级归属校验：不存在与不属于本人统一返回“无权访问该资源”。
     */
    private EduStudyPlan requireOwnedPlan(Long planId, Long studentId)
    {
        if (planId == null)
        {
            throw new ServiceException("规划ID不能为空");
        }
        EduStudyPlan plan = planMapper.selectPlanById(planId);
        if (plan == null || !Objects.equals(plan.getStudentId(), studentId))
        {
            throw new ServiceException(EducationConstants.MSG_NO_PERMISSION);
        }
        return plan;
    }

    private void ensureEditable(EduStudyPlan plan)
    {
        String status = plan.getStatus();
        if (PlanStatus.SUBMITTED.getCode().equals(status) || PlanStatus.ARCHIVED.getCode().equals(status))
        {
            throw new ServiceException("已提交或已归档的规划不可修改");
        }
    }

    private List<CurriculumRule> loadCurriculumRules(Long studentId)
    {
        StudentProfile profile = studentDirectoryPort.findProfile(studentId);
        if (profile == null || profile.getMajorId() == null)
        {
            return Collections.emptyList();
        }
        List<CurriculumRule> rules = curriculumPort.loadCurriculumRules(profile.getMajorId(), profile.getEntryYear());
        return rules == null ? Collections.<CurriculumRule>emptyList() : rules;
    }

    private List<Long> loadItemCourseIds(Long planId)
    {
        List<EduStudyPlanItem> items = itemMapper.selectItemsByPlanId(planId);
        List<Long> courseIds = new ArrayList<>();
        if (items != null)
        {
            for (EduStudyPlanItem item : items)
            {
                if (item != null)
                {
                    courseIds.add(item.getCourseId());
                }
            }
        }
        return courseIds;
    }

    private static List<Long> normalizeCourseIds(List<PlanItemBody> items)
    {
        if (items == null || items.isEmpty())
        {
            return new ArrayList<>();
        }
        List<Long> courseIds = new ArrayList<>(items.size());
        for (PlanItemBody item : items)
        {
            if (item != null && item.getCourseId() != null)
            {
                courseIds.add(item.getCourseId());
            }
        }
        return distinct(courseIds);
    }

    private static List<Long> distinct(List<Long> courseIds)
    {
        if (courseIds == null || courseIds.isEmpty())
        {
            return new ArrayList<>();
        }
        LinkedHashSet<Long> unique = new LinkedHashSet<>();
        for (Long courseId : courseIds)
        {
            if (courseId != null)
            {
                unique.add(courseId);
            }
        }
        return new ArrayList<>(unique);
    }

    private static String joinMessages(PlanValidationResult result)
    {
        if (result == null || result.getMessages() == null || result.getMessages().isEmpty())
        {
            return "";
        }
        return String.join("；", result.getMessages());
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

    /**
     * 非 Web 上下文（如单元测试直接 new）下不抛异常。
     */
    private static String currentUsername()
    {
        try
        {
            return SecurityUtils.getUsername();
        }
        catch (RuntimeException ex)
        {
            return null;
        }
    }
}
