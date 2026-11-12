package com.ruoyi.education.course.service.impl;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.education.common.PrerequisiteGraph;
import com.ruoyi.education.course.domain.EduCourse;
import com.ruoyi.education.course.domain.EduCourseOption;
import com.ruoyi.education.course.domain.EduCoursePrereq;
import com.ruoyi.education.course.dto.CourseOptionsVO;
import com.ruoyi.education.course.dto.CourseQuery;
import com.ruoyi.education.course.dto.PrereqCheckVO;
import com.ruoyi.education.course.mapper.EduCourseMapper;
import com.ruoyi.education.course.mapper.EduCoursePrereqMapper;
import com.ruoyi.education.course.service.IEduCourseService;

/**
 * 课程目录与先修关系 服务层实现
 *
 * <p>先修规则语义（与 DDL 及 backend-conventions.md 第 6 节一致）：
 * 同一 {@code group_no} 内为 AND，不同 {@code group_no} 之间为 OR。</p>
 *
 * <p>环检测与“最少阻力路径”判定统一委托
 * {@link com.ruoyi.education.common.PrerequisiteGraph}，避免同一规则在两处实现出现分叉。</p>
 *
 * @author ruoyi
 */
@Service
public class EduCourseServiceImpl implements IEduCourseService
{
    @Autowired
    private EduCourseMapper courseMapper;

    @Autowired
    private EduCoursePrereqMapper prereqMapper;

    @Override
    public List<EduCourse> selectCourseList(CourseQuery query)
    {
        return courseMapper.selectCourseList(query == null ? new CourseQuery() : query);
    }

    @Override
    public EduCourse selectCourseById(Long courseId)
    {
        if (courseId == null)
        {
            throw new ServiceException("课程ID不能为空");
        }
        return courseMapper.selectCourseById(courseId);
    }

    @Override
    public EduCourse selectCourseByCode(String code)
    {
        if (StringUtils.isEmpty(code))
        {
            throw new ServiceException("课程代码不能为空");
        }
        return courseMapper.selectCourseByCode(code.trim());
    }

    @Override
    public List<EduCourse> selectCourseByCodes(List<String> codes)
    {
        List<String> normalized = new ArrayList<>();
        if (codes != null)
        {
            for (String code : codes)
            {
                if (StringUtils.isNotEmpty(code))
                {
                    normalized.add(code.trim());
                }
            }
        }
        if (normalized.isEmpty())
        {
            return new ArrayList<>();
        }
        return courseMapper.selectCourseByCodes(normalized);
    }

    @Override
    public int insertCourse(EduCourse course)
    {
        if (course == null)
        {
            throw new ServiceException("课程信息不能为空");
        }
        String code = StringUtils.trim(course.getCode());
        if (StringUtils.isEmpty(code))
        {
            throw new ServiceException("课程代码不能为空");
        }
        if (courseMapper.countCourseByCode(code) > 0)
        {
            throw new ServiceException("课程代码已存在：" + code);
        }
        course.setCode(code);
        return courseMapper.insertCourse(course);
    }

    @Override
    public int updateCourse(EduCourse course)
    {
        if (course == null || course.getCourseId() == null)
        {
            throw new ServiceException("课程ID不能为空");
        }
        EduCourse exist = courseMapper.selectCourseById(course.getCourseId());
        if (exist == null)
        {
            throw new ServiceException("课程不存在");
        }
        String code = StringUtils.trim(course.getCode());
        if (StringUtils.isEmpty(code))
        {
            throw new ServiceException("课程代码不能为空");
        }
        EduCourse sameCode = courseMapper.selectCourseByCode(code);
        if (sameCode != null && !sameCode.getCourseId().equals(course.getCourseId()))
        {
            throw new ServiceException("课程代码已被其他课程占用：" + code);
        }
        course.setCode(code);
        return courseMapper.updateCourse(course);
    }

    @Override
    @Transactional
    public int deleteCourseByIds(Long[] courseIds)
    {
        if (courseIds == null || courseIds.length == 0)
        {
            return 0;
        }
        return courseMapper.deleteCourseByIds(courseIds);
    }

    @Override
    public CourseOptionsVO selectCourseOptions()
    {
        List<EduCourseOption> options = courseMapper.selectCourseOptions();
        Set<String> schools = new TreeSet<>();
        Set<String> categories = new TreeSet<>();
        Set<Integer> levels = new TreeSet<>();
        if (options != null)
        {
            for (EduCourseOption option : options)
            {
                if (option == null)
                {
                    continue;
                }
                if (StringUtils.isNotBlank(option.getSchool()))
                {
                    schools.add(option.getSchool().trim());
                }
                if (StringUtils.isNotBlank(option.getCategory()))
                {
                    categories.add(option.getCategory().trim());
                }
                if (option.getLevel() != null)
                {
                    levels.add(option.getLevel());
                }
            }
        }
        CourseOptionsVO vo = new CourseOptionsVO();
        vo.setSchools(new ArrayList<>(schools));
        vo.setCategories(new ArrayList<>(categories));
        vo.setLevels(new ArrayList<>(levels));
        return vo;
    }

    @Override
    public List<EduCoursePrereq> selectPrereqByCourseId(Long courseId)
    {
        if (courseId == null)
        {
            throw new ServiceException("课程ID不能为空");
        }
        List<EduCoursePrereq> prereqs = prereqMapper.selectPrereqByCourseId(courseId);
        if (prereqs == null)
        {
            return new ArrayList<>();
        }
        Set<Long> passedCourseIds = resolveCurrentStudentPassedCourseIdsQuietly();
        if (passedCourseIds != null)
        {
            for (EduCoursePrereq prereq : prereqs)
            {
                prereq.setSatisfied(passedCourseIds.contains(prereq.getPrereqCourseId()));
            }
        }
        return prereqs;
    }

    @Override
    @Transactional
    public void replacePrereqs(Long courseId, List<EduCoursePrereq> prereqs)
    {
        if (courseId == null)
        {
            throw new ServiceException("课程ID不能为空");
        }
        EduCourse course = courseMapper.selectCourseById(courseId);
        if (course == null)
        {
            throw new ServiceException("课程不存在，无法维护先修关系");
        }
        List<EduCoursePrereq> rows = normalizePrereqs(courseId, prereqs);
        Map<Long, List<EduCoursePrereq>> graph = buildProspectiveGraph(courseId, rows);
        List<Long> cycleCourseIds = new PrerequisiteGraph(graph).detectCycles();
        if (!cycleCourseIds.isEmpty())
        {
            throw new ServiceException("先修关系存在环路，涉及课程：" + describeCourses(cycleCourseIds));
        }
        prereqMapper.deletePrereqByCourseId(courseId);
        if (!rows.isEmpty())
        {
            prereqMapper.insertPrereqBatch(rows);
        }
    }

    @Override
    public PrereqCheckVO checkPrerequisite(Long courseId)
    {
        if (courseId == null)
        {
            throw new ServiceException("课程ID不能为空");
        }
        EduCourse course = courseMapper.selectCourseById(courseId);
        if (course == null)
        {
            throw new ServiceException("课程不存在");
        }
        // 学生身份只取自当前登录用户，绝不接受前端传入的 studentId
        Set<Long> passedCourseIds = resolveCurrentStudentPassedCourseIds();
        List<EduCoursePrereq> prereqs = prereqMapper.selectPrereqByCourseId(courseId);
        PrereqCheckVO result = new PrereqCheckVO();
        if (prereqs == null || prereqs.isEmpty())
        {
            result.setSatisfied(true);
            result.setReason("该课程未设置先修要求");
            return result;
        }
        Map<Integer, List<Long>> groups = new TreeMap<>();
        Map<Long, EduCoursePrereq> prereqIndex = new LinkedHashMap<>();
        for (EduCoursePrereq prereq : prereqs)
        {
            if (prereq == null || prereq.getPrereqCourseId() == null)
            {
                continue;
            }
            Integer groupNo = prereq.getGroupNo() == null ? Integer.valueOf(1) : prereq.getGroupNo();
            groups.computeIfAbsent(groupNo, key -> new ArrayList<>()).add(prereq.getPrereqCourseId());
            prereqIndex.putIfAbsent(prereq.getPrereqCourseId(), prereq);
        }
        Map<Long, Map<Integer, List<Long>>> grouped = new LinkedHashMap<>();
        grouped.put(courseId, groups);
        List<Long> missingIds = PrerequisiteGraph.missingPrerequisiteIds(grouped, courseId, passedCourseIds);
        if (missingIds.isEmpty())
        {
            result.setSatisfied(true);
            result.setReason("先修要求已满足");
            return result;
        }
        List<PrereqCheckVO.MissingCourse> missingCourses = new ArrayList<>();
        for (Long missingId : missingIds)
        {
            EduCoursePrereq prereq = prereqIndex.get(missingId);
            missingCourses.add(new PrereqCheckVO.MissingCourse(missingId,
                prereq == null ? null : prereq.getPrereqCode(),
                prereq == null ? null : prereq.getPrereqTitle()));
        }
        result.setSatisfied(false);
        result.setMissing(missingCourses);
        result.setReason(buildMissingReason(missingCourses));
        return result;
    }

    /**
     * 解析当前登录学生的已通过课程ID（学生由 sys_user_id 反查 edu_student 得到）
     *
     * @return 已通过课程ID集合
     */
    private Set<Long> resolveCurrentStudentPassedCourseIds()
    {
        Long userId = SecurityUtils.getUserId();
        Long studentId = courseMapper.selectStudentIdBySysUserId(userId);
        if (studentId == null)
        {
            throw new ServiceException("当前账号未绑定学生学籍");
        }
        return loadPassedCourseIds(studentId);
    }

    /**
     * 解析当前登录学生的已通过课程ID；未登录或未绑定学籍时返回 null（不抛异常）
     *
     * @return 已通过课程ID集合，无法解析时返回 null
     */
    private Set<Long> resolveCurrentStudentPassedCourseIdsQuietly()
    {
        Long userId;
        try
        {
            userId = SecurityUtils.getUserId();
        }
        catch (RuntimeException ex)
        {
            return null;
        }
        if (userId == null)
        {
            return null;
        }
        Long studentId = courseMapper.selectStudentIdBySysUserId(userId);
        if (studentId == null)
        {
            return null;
        }
        try
        {
            return loadPassedCourseIds(studentId);
        }
        catch (RuntimeException ex)
        {
            return null;
        }
    }

    /**
     * 按学生ID载入已通过课程ID集合（课程ID缺失时按课程代码兜底匹配）
     *
     * @param studentId 学生ID
     * @return 已通过课程ID集合
     */
    private Set<Long> loadPassedCourseIds(Long studentId)
    {
        Set<Long> passedCourseIds = new LinkedHashSet<>();
        List<Long> courseIds = courseMapper.selectPassedCourseIdsByStudentId(studentId);
        if (courseIds != null)
        {
            for (Long courseId : courseIds)
            {
                if (courseId != null)
                {
                    passedCourseIds.add(courseId);
                }
            }
        }
        List<String> codes = courseMapper.selectPassedCourseCodesByStudentId(studentId);
        if (codes != null && !codes.isEmpty())
        {
            List<EduCourse> courses = selectCourseByCodes(codes);
            for (EduCourse course : courses)
            {
                if (course != null && course.getCourseId() != null)
                {
                    passedCourseIds.add(course.getCourseId());
                }
            }
        }
        return passedCourseIds;
    }

    /**
     * 校验并规整先修关系入参
     *
     * @param courseId 课程ID
     * @param prereqs 前端入参
     * @return 可直接入库的先修关系
     */
    private List<EduCoursePrereq> normalizePrereqs(Long courseId, List<EduCoursePrereq> prereqs)
    {
        List<EduCoursePrereq> rows = new ArrayList<>();
        if (prereqs == null || prereqs.isEmpty())
        {
            return rows;
        }
        Set<Long> seen = new LinkedHashSet<>();
        String operator = currentUsernameQuietly();
        for (EduCoursePrereq prereq : prereqs)
        {
            if (prereq == null || prereq.getPrereqCourseId() == null)
            {
                throw new ServiceException("先修课程ID不能为空");
            }
            Long prereqCourseId = prereq.getPrereqCourseId();
            if (prereqCourseId.equals(courseId))
            {
                throw new ServiceException("先修课程不能为课程自身");
            }
            if (!seen.add(prereqCourseId))
            {
                throw new ServiceException("先修课程重复：" + prereqCourseId);
            }
            Integer groupNo = prereq.getGroupNo() == null ? Integer.valueOf(1) : prereq.getGroupNo();
            if (groupNo.intValue() < 1)
            {
                throw new ServiceException("先修组号必须大于 0");
            }
            EduCourse prereqCourse = courseMapper.selectCourseById(prereqCourseId);
            if (prereqCourse == null)
            {
                throw new ServiceException("先修课程不存在：" + prereqCourseId);
            }
            EduCoursePrereq row = new EduCoursePrereq();
            row.setCourseId(courseId);
            row.setPrereqCourseId(prereqCourseId);
            row.setGroupNo(groupNo);
            row.setRawText(prereq.getRawText());
            row.setCreateBy(StringUtils.isNotEmpty(prereq.getCreateBy()) ? prereq.getCreateBy() : operator);
            rows.add(row);
        }
        return rows;
    }

    /**
     * 构建写入后的先修有向图（全量边，替换掉当前课程原有的边）
     *
     * @param courseId 课程ID
     * @param newEdges 新的先修边
     * @return 先修行：课程ID -&gt; 先修行
     */
    private Map<Long, List<EduCoursePrereq>> buildProspectiveGraph(Long courseId, List<EduCoursePrereq> newEdges)
    {
        Map<Long, List<EduCoursePrereq>> graph = new LinkedHashMap<>();
        List<EduCoursePrereq> existing = prereqMapper.selectAllPrereq();
        if (existing != null)
        {
            for (EduCoursePrereq edge : existing)
            {
                if (edge == null || edge.getCourseId() == null || edge.getPrereqCourseId() == null)
                {
                    continue;
                }
                if (courseId.equals(edge.getCourseId()))
                {
                    continue;
                }
                graph.computeIfAbsent(edge.getCourseId(), key -> new ArrayList<>()).add(edge);
            }
        }
        for (EduCoursePrereq edge : newEdges)
        {
            graph.computeIfAbsent(courseId, key -> new ArrayList<>()).add(edge);
        }
        return graph;
    }

    /**
     * 把课程ID列表转成可读的课程代码链
     *
     * @param courseIds 课程ID
     * @return 可读说明
     */
    private String describeCourses(List<Long> courseIds)
    {
        StringBuilder builder = new StringBuilder();
        for (Long courseId : courseIds)
        {
            if (builder.length() > 0)
            {
                builder.append("、");
            }
            builder.append(courseCode(courseId));
        }
        return builder.toString();
    }

    /**
     * 按课程ID取课程代码，取不到时回退为ID
     *
     * @param courseId 课程ID
     * @return 课程代码
     */
    private String courseCode(Long courseId)
    {
        EduCourse course = courseMapper.selectCourseById(courseId);
        return course == null || StringUtils.isEmpty(course.getCode()) ? String.valueOf(courseId) : course.getCode();
    }

    /**
     * 生成缺少先修课程的可读说明
     *
     * @param missingCourses 缺少的先修课程
     * @return 可读说明
     */
    private String buildMissingReason(List<PrereqCheckVO.MissingCourse> missingCourses)
    {
        StringBuilder builder = new StringBuilder("缺少先修课程：");
        for (int i = 0; i < missingCourses.size(); i++)
        {
            if (i > 0)
            {
                builder.append("、");
            }
            PrereqCheckVO.MissingCourse missing = missingCourses.get(i);
            builder.append(StringUtils.isNotEmpty(missing.getCode()) ? missing.getCode()
                : String.valueOf(missing.getCourseId()));
            if (StringUtils.isNotEmpty(missing.getTitle()))
            {
                builder.append("（").append(missing.getTitle()).append("）");
            }
        }
        return builder.toString();
    }

    /**
     * 取当前登录用户名，非请求上下文（如定时任务）下返回 null
     *
     * @return 用户名或 null
     */
    private String currentUsernameQuietly()
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
