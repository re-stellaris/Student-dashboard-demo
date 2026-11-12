package com.ruoyi.education.enrollment.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.PageUtils;
import com.ruoyi.education.common.PrerequisiteGraph;
import com.ruoyi.education.enrollment.domain.EduCourseOffering;
import com.ruoyi.education.enrollment.domain.EduCourseRecord;
import com.ruoyi.education.enrollment.domain.EduEnrollment;
import com.ruoyi.education.enrollment.domain.EduStudent;
import com.ruoyi.education.enrollment.domain.EduTeacher;
import com.ruoyi.education.enrollment.domain.EduTerm;
import com.ruoyi.education.enrollment.dto.CreditCategoryRow;
import com.ruoyi.education.enrollment.dto.CreditCategoryVO;
import com.ruoyi.education.enrollment.dto.CreditProgressVO;
import com.ruoyi.education.enrollment.dto.CurriculumCreditRow;
import com.ruoyi.education.enrollment.dto.PrerequisiteRow;
import com.ruoyi.education.enrollment.mapper.EduCourseOfferingMapper;
import com.ruoyi.education.enrollment.mapper.EduCourseRecordMapper;
import com.ruoyi.education.enrollment.mapper.EduEnrollmentMapper;
import com.ruoyi.education.enrollment.mapper.EduStudentMapper;
import com.ruoyi.education.enrollment.mapper.EduTermMapper;
import com.ruoyi.education.enrollment.service.IEduEnrollmentService;
import com.ruoyi.education.enrollment.support.EduIdentityResolver;

/**
 * 学期 / 开课 / 选课 / 成绩 业务实现
 *
 * @author ruoyi-education
 */
@Service
public class EduEnrollmentServiceImpl implements IEduEnrollmentService
{
    /** 选课状态：在读 */
    private static final String ENROLLMENT_ENROLLED = "ENROLLED";

    /** 选课状态：已退课 */
    private static final String ENROLLMENT_DROPPED = "DROPPED";

    /** 选课状态：已修完 */
    private static final String ENROLLMENT_COMPLETED = "COMPLETED";

    /** 开课状态：开放选课 */
    private static final String OFFERING_OPEN = "0";

    /** 学分显示精度 */
    private static final int UNIT_SCALE = 1;

    /**
     * 成绩分类 → 培养方案要求类型（edu_curriculum_item.requirement_type）对照。
     *
     * <p>只映射语义明确的组合：Major ← REQUIRED、GenEd ← GE、Other ← ELECTIVE。
     * English/Chinese/IT/PE 属于 GE 的子口径，BREADTH/DEPTH/STREAM 属于专业内的选课规则，
     * 均不作为独立分类重复计入要求学分，仅展示已获学分（required = 0）。</p>
     */
    private static final Map<String, String> CATEGORY_TO_REQUIREMENT = new LinkedHashMap<>();

    /** 要求类型 → 成绩分类 的反向对照（由 CATEGORY_TO_REQUIREMENT 生成） */
    private static final Map<String, String> REQUIREMENT_TO_CATEGORY = new HashMap<>();

    static
    {
        CATEGORY_TO_REQUIREMENT.put("Major", "REQUIRED");
        CATEGORY_TO_REQUIREMENT.put("GenEd", "GE");
        CATEGORY_TO_REQUIREMENT.put("Other", "ELECTIVE");
        for (Map.Entry<String, String> entry : CATEGORY_TO_REQUIREMENT.entrySet())
        {
            REQUIREMENT_TO_CATEGORY.put(entry.getValue(), entry.getKey());
        }
    }

    @Autowired
    private EduTermMapper termMapper;

    @Autowired
    private EduCourseOfferingMapper offeringMapper;

    @Autowired
    private EduEnrollmentMapper enrollmentMapper;

    @Autowired
    private EduCourseRecordMapper recordMapper;

    @Autowired
    private EduStudentMapper studentMapper;

    @Autowired
    private EduIdentityResolver identityResolver;

    @Override
    public List<EduTerm> selectTermList(EduTerm term)
    {
        return termMapper.selectTermList(term == null ? new EduTerm() : term);
    }

    @Override
    public EduTerm selectCurrentTerm()
    {
        return termMapper.selectCurrentTerm();
    }

    @Override
    public List<EduCourseOffering> selectOfferingList(EduCourseOffering offering)
    {
        return offeringMapper.selectOfferingList(offering == null ? new EduCourseOffering() : offering);
    }

    @Override
    public EduCourseOffering selectOfferingById(Long offeringId)
    {
        if (offeringId == null)
        {
            throw new ServiceException("开课ID不能为空");
        }
        return offeringMapper.selectOfferingById(offeringId);
    }

    @Override
    public List<EduCourseOffering> selectMyOfferings()
    {
        EduTeacher teacher = identityResolver.currentTeacher();
        // 分页必须紧贴列表查询：前面的身份查询会吃掉 PageHelper 的 LIMIT
        PageUtils.startPage();
        return offeringMapper.selectOfferingByTeacherId(teacher.getTeacherId());
    }

    @Override
    public int insertOffering(EduCourseOffering offering)
    {
        if (offering == null || offering.getCourseId() == null)
        {
            throw new ServiceException("课程不能为空");
        }
        if (offering.getTermId() == null)
        {
            throw new ServiceException("学期不能为空");
        }
        EduCourseOffering exists = offeringMapper.selectOfferingByCourseAndTerm(offering.getCourseId(),
                offering.getTermId());
        if (exists != null)
        {
            throw new ServiceException("该课程在所选学期已存在开课记录");
        }
        if (!identityResolver.isAdmin())
        {
            EduTeacher teacher = identityResolver.currentTeacher();
            if (offering.getTeacherId() != null && !teacher.getTeacherId().equals(offering.getTeacherId()))
            {
                throw identityResolver.forbidden();
            }
            offering.setTeacherId(teacher.getTeacherId());
        }
        if (offering.getCapacity() == null)
        {
            offering.setCapacity(0);
        }
        if (offering.getEnrolledCount() == null)
        {
            offering.setEnrolledCount(0);
        }
        if (offering.getStatus() == null || offering.getStatus().isEmpty())
        {
            offering.setStatus(OFFERING_OPEN);
        }
        return offeringMapper.insertOffering(offering);
    }

    @Override
    public int updateOffering(EduCourseOffering offering)
    {
        if (offering == null || offering.getOfferingId() == null)
        {
            throw new ServiceException("开课ID不能为空");
        }
        EduCourseOffering current = offeringMapper.selectOfferingById(offering.getOfferingId());
        if (current == null)
        {
            throw new ServiceException("开课记录不存在");
        }
        if (!identityResolver.isAdmin())
        {
            EduTeacher teacher = identityResolver.currentTeacher();
            if (current.getTeacherId() == null || !current.getTeacherId().equals(teacher.getTeacherId()))
            {
                throw identityResolver.forbidden();
            }
            if (offering.getTeacherId() != null && !teacher.getTeacherId().equals(offering.getTeacherId()))
            {
                throw identityResolver.forbidden();
            }
            offering.setTeacherId(teacher.getTeacherId());
        }
        // 已选人数只能由选课/退课流程维护，禁止直接改写
        offering.setEnrolledCount(null);

        Long targetCourseId = offering.getCourseId() != null ? offering.getCourseId() : current.getCourseId();
        Long targetTermId = offering.getTermId() != null ? offering.getTermId() : current.getTermId();
        if (targetCourseId != null && targetTermId != null
                && (!targetCourseId.equals(current.getCourseId()) || !targetTermId.equals(current.getTermId())))
        {
            EduCourseOffering duplicate = offeringMapper.selectOfferingByCourseAndTerm(targetCourseId, targetTermId);
            if (duplicate != null && !duplicate.getOfferingId().equals(offering.getOfferingId()))
            {
                throw new ServiceException("该课程在所选学期已存在开课记录");
            }
        }
        return offeringMapper.updateOffering(offering);
    }

    @Override
    public EduStudent selectMyStudent()
    {
        return identityResolver.currentStudent();
    }

    @Override
    public CreditProgressVO selectMyProgress()
    {
        EduStudent student = identityResolver.currentStudent();

        // 1. 已获得学分（通过且完成的成绩记录，按分类汇总）
        Map<String, BigDecimal> earnedByCategory = new HashMap<>();
        BigDecimal earned = BigDecimal.ZERO;
        List<CreditCategoryRow> earnedRows = recordMapper.selectCategorySummary(student.getStudentId());
        for (CreditCategoryRow row : earnedRows)
        {
            String category = normalizeCategory(row.getCategory());
            BigDecimal units = nvl(row.getUnits());
            earnedByCategory.merge(category, units, BigDecimal::add);
            earned = earned.add(units);
        }

        // 2. 适用培养方案的学分要求
        BigDecimal curriculumTotal = BigDecimal.ZERO;
        BigDecimal majorRequired = BigDecimal.ZERO;
        Map<String, BigDecimal> requiredByCategory = new HashMap<>();
        CurriculumCreditRow curriculum = studentMapper.selectApplicableCurriculum(student.getStudentId());
        if (curriculum != null)
        {
            curriculumTotal = nvl(curriculum.getTotalCredits());
            majorRequired = nvl(curriculum.getMajorCredits());
            List<CreditCategoryRow> requirementRows = studentMapper
                    .selectCurriculumRequirementSummary(curriculum.getCurriculumId());
            for (CreditCategoryRow row : requirementRows)
            {
                String requirementType = row.getCategory() == null ? "" : row.getCategory().trim().toUpperCase();
                String category = REQUIREMENT_TO_CATEGORY.get(requirementType);
                if (category == null)
                {
                    continue;
                }
                requiredByCategory.merge(category, nvl(row.getUnits()), BigDecimal::add);
            }
        }

        // 3. 分类明细：固定分类顺序 + 记录中出现过的其它分类
        Set<String> categories = new LinkedHashSet<>(CATEGORY_TO_REQUIREMENT.keySet());
        categories.addAll(earnedByCategory.keySet());
        categories.addAll(requiredByCategory.keySet());

        List<CreditCategoryVO> breakdown = new ArrayList<>();
        for (String category : categories)
        {
            BigDecimal categoryEarned = scale(earnedByCategory.get(category));
            BigDecimal categoryRequired = scale(requiredByCategory.get(category));
            if (categoryEarned.signum() == 0 && categoryRequired.signum() == 0)
            {
                continue;
            }
            breakdown.add(new CreditCategoryVO(category, categoryEarned, categoryRequired));
        }

        CreditProgressVO vo = new CreditProgressVO();
        vo.setCurriculumTotal(scale(curriculumTotal));
        vo.setMajorRequired(scale(majorRequired));
        vo.setEarned(scale(earned));
        BigDecimal remaining = curriculumTotal.subtract(earned);
        vo.setRemaining(scale(remaining.signum() < 0 ? BigDecimal.ZERO : remaining));
        vo.setCategoryBreakdown(breakdown);
        return vo;
    }

    @Override
    public List<EduCourseRecord> selectMyRecords(EduCourseRecord query)
    {
        EduStudent student = identityResolver.currentStudent();
        EduCourseRecord condition = query == null ? new EduCourseRecord() : query;
        // 身份锚点由服务端强制写入，忽略前端传入的 studentId
        condition.setStudentId(student.getStudentId());
        // 分页必须紧贴列表查询
        PageUtils.startPage();
        return recordMapper.selectRecordList(condition);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long addEnrollment(Long offeringId)
    {
        if (offeringId == null)
        {
            throw new ServiceException("开课ID不能为空");
        }
        EduStudent student = identityResolver.currentStudent();
        EduCourseOffering offering = offeringMapper.selectOfferingById(offeringId);
        if (offering == null)
        {
            throw new ServiceException("开课记录不存在");
        }
        if (!OFFERING_OPEN.equals(offering.getStatus()))
        {
            throw new ServiceException("该课程当前未开放选课");
        }
        int enrolled = enrollmentMapper.countByOffering(offeringId);
        Integer capacity = offering.getCapacity();
        if (capacity != null && capacity > 0 && enrolled >= capacity)
        {
            throw new ServiceException("该课程选课人数已满");
        }

        EduEnrollment exists = enrollmentMapper.selectByStudentAndOffering(student.getStudentId(), offeringId);
        if (exists != null)
        {
            if (ENROLLMENT_ENROLLED.equals(exists.getStatus()))
            {
                throw new ServiceException("您已选修该课程");
            }
            if (ENROLLMENT_COMPLETED.equals(exists.getStatus()))
            {
                throw new ServiceException("该课程已修完，无需重复选课");
            }
            // 已退课 → 恢复为在读
            enrollmentMapper.updateStatus(exists.getEnrollmentId(), ENROLLMENT_ENROLLED,
                    identityResolver.currentUsername());
            offeringMapper.incrementEnrolledCount(offeringId, 1);
            return exists.getEnrollmentId();
        }

        checkPrerequisites(student, offering.getCourseId());

        EduEnrollment enrollment = new EduEnrollment();
        enrollment.setStudentId(student.getStudentId());
        enrollment.setOfferingId(offeringId);
        enrollment.setStatus(ENROLLMENT_ENROLLED);
        enrollment.setEnrolledAt(DateUtils.getNowDate());
        enrollment.setCreateBy(identityResolver.currentUsername());
        enrollmentMapper.insertEnrollment(enrollment);
        offeringMapper.incrementEnrolledCount(offeringId, 1);
        return enrollment.getEnrollmentId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int dropEnrollment(Long enrollmentId)
    {
        if (enrollmentId == null)
        {
            throw new ServiceException("选课ID不能为空");
        }
        EduEnrollment enrollment = enrollmentMapper.selectEnrollmentById(enrollmentId);
        if (enrollment == null)
        {
            throw new ServiceException("选课记录不存在");
        }
        EduStudent student = identityResolver.currentStudent();
        if (!student.getStudentId().equals(enrollment.getStudentId()))
        {
            throw identityResolver.forbidden();
        }
        if (!ENROLLMENT_ENROLLED.equals(enrollment.getStatus()))
        {
            throw new ServiceException("当前选课状态不可退课");
        }
        int rows = enrollmentMapper.updateStatus(enrollmentId, ENROLLMENT_DROPPED,
                identityResolver.currentUsername());
        if (rows > 0)
        {
            offeringMapper.incrementEnrolledCount(enrollment.getOfferingId(), -1);
        }
        return rows;
    }

    @Override
    public int updateRecord(EduCourseRecord record)
    {
        if (record == null || record.getRecordId() == null)
        {
            throw new ServiceException("成绩记录ID不能为空");
        }
        EduCourseRecord current = recordMapper.selectRecordById(record.getRecordId());
        if (current == null)
        {
            throw new ServiceException("成绩记录不存在");
        }
        if (!identityResolver.isAdmin())
        {
            EduTeacher teacher = identityResolver.currentTeacher();
            Long courseId = record.getCourseId() != null ? record.getCourseId() : current.getCourseId();
            if (courseId == null)
            {
                throw identityResolver.forbidden();
            }
            Long termId = record.getTermId() != null ? record.getTermId() : current.getTermId();
            EduCourseOffering offering = termId == null ? null
                    : offeringMapper.selectOfferingByCourseAndTerm(courseId, termId);
            if (offering == null)
            {
                EduCourseOffering query = new EduCourseOffering();
                query.setCourseId(courseId);
                query.setTeacherId(teacher.getTeacherId());
                if (offeringMapper.selectOfferingList(query).isEmpty())
                {
                    throw identityResolver.forbidden();
                }
            }
            else if (offering.getTeacherId() == null || !offering.getTeacherId().equals(teacher.getTeacherId()))
            {
                throw identityResolver.forbidden();
            }
        }
        // 成绩归属学生不可通过修改接口变更
        record.setStudentId(null);
        record.setUpdateBy(identityResolver.currentUsername());
        return recordMapper.updateRecord(record);
    }

    /**
     * 先修校验：组内 AND、组间 OR（与 docs/architecture/backend-conventions.md 第 6 节语义一致）。
     *
     * <p>先把 edu_course_prereq 的只读行按 group_no 归一化为
     * {@code courseId -> groupNo -> prereqCourseIds}，再交给域内共享的
     * {@link PrerequisiteGraph} 判定，保证选课校验与规划校验使用同一套规则实现。</p>
     *
     * @param student 当前学生
     * @param courseId 目标课程ID
     */
    private void checkPrerequisites(EduStudent student, Long courseId)
    {
        if (courseId == null)
        {
            return;
        }
        List<PrerequisiteRow> rows = enrollmentMapper.selectPrereqRows(courseId);
        if (rows == null || rows.isEmpty())
        {
            return;
        }
        Set<Long> passed = new HashSet<>(recordMapper.selectPassedCourseIds(student.getStudentId()));

        Map<Integer, List<Long>> groups = new LinkedHashMap<>();
        Map<Long, PrerequisiteRow> rowIndex = new HashMap<>();
        for (PrerequisiteRow row : rows)
        {
            if (row.getPrereqCourseId() == null)
            {
                continue;
            }
            Integer groupNo = row.getGroupNo() == null ? Integer.valueOf(1) : row.getGroupNo();
            List<Long> prereqIds = groups.computeIfAbsent(groupNo, key -> new ArrayList<>());
            if (!prereqIds.contains(row.getPrereqCourseId()))
            {
                prereqIds.add(row.getPrereqCourseId());
            }
            rowIndex.putIfAbsent(row.getPrereqCourseId(), row);
        }
        if (groups.isEmpty())
        {
            return;
        }

        Map<Long, Map<Integer, List<Long>>> edges = new HashMap<>();
        edges.put(courseId, groups);
        if (PrerequisiteGraph.isSatisfied(edges, courseId, passed))
        {
            return;
        }

        List<String> missing = new ArrayList<>();
        for (Long missingId : PrerequisiteGraph.missingPrerequisiteIds(edges, courseId, passed))
        {
            PrerequisiteRow row = rowIndex.get(missingId);
            if (row == null)
            {
                missing.add(String.valueOf(missingId));
            }
            else
            {
                missing.add(formatCourse(row.getPrereqCode(), row.getPrereqTitle()));
            }
        }
        throw new ServiceException("缺少先修课程：" + String.join("、", missing));
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
     * 空分类归入 Other
     *
     * @param category 原始分类
     * @return 规范分类
     */
    private String normalizeCategory(String category)
    {
        if (category == null || category.trim().isEmpty())
        {
            return "Other";
        }
        return category.trim();
    }

    /**
     * BigDecimal 空值转 0
     *
     * @param value 原值
     * @return 非空值
     */
    private BigDecimal nvl(BigDecimal value)
    {
        return value == null ? BigDecimal.ZERO : value;
    }

    /**
     * 统一保留 1 位小数
     *
     * @param value 原值
     * @return 规约后的值
     */
    private BigDecimal scale(BigDecimal value)
    {
        return nvl(value).setScale(UNIT_SCALE, RoundingMode.HALF_UP);
    }
}
