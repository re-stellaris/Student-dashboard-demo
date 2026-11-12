package com.ruoyi.education.planning.spi;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import com.ruoyi.education.planning.mapper.PlanningReadMapper;
import com.ruoyi.education.planning.rule.CurriculumRule;
import com.ruoyi.education.planning.rule.PlanCourse;
import com.ruoyi.education.planning.rule.PrereqEdge;

/**
 * 规划域只读端口的默认实现（纯读，不写任何表）。
 *
 * <p>设计意图：规划域的规则引擎只依赖 {@code planning.spi} 的端口，Spring 装配统一收敛在
 * {@code EduPlanningServiceImpl}。本类是“开箱可用”的默认适配器，
 * 归属域（course / curriculum / enrollment）提供正式 Service 后，
 * 只需新增一个 {@code @Primary} 的同类型 Bean 即可替换，规划域其余代码零改动。</p>
 *
 * @author ruoyi
 */
@Component
@Primary
public class DefaultPlanningReadPorts
        implements StudentDirectoryPort, CourseCatalogPort, TranscriptPort, CurriculumPort, OfferingPort
{
    private static final String[] SCHEDULE_SEPARATORS = new String[] { ";", "；", ",", "，", "\n", "\r" };

    private final PlanningReadMapper readMapper;

    public DefaultPlanningReadPorts(PlanningReadMapper readMapper)
    {
        this.readMapper = readMapper;
    }

    @Override
    public Long findStudentIdBySysUserId(Long sysUserId)
    {
        if (sysUserId == null)
        {
            return null;
        }
        return readMapper.selectStudentIdBySysUserId(sysUserId);
    }

    @Override
    public StudentProfile findProfile(Long studentId)
    {
        if (studentId == null)
        {
            return null;
        }
        return readMapper.selectStudentProfile(studentId);
    }

    @Override
    public List<PlanCourse> loadCourses(Collection<Long> courseIds)
    {
        List<PlanCourse> all = readMapper.selectCourseCatalog();
        if (all == null || all.isEmpty())
        {
            return Collections.emptyList();
        }
        if (courseIds == null || courseIds.isEmpty())
        {
            return all;
        }
        Map<Long, PlanCourse> index = new LinkedHashMap<>();
        for (PlanCourse course : all)
        {
            if (course != null && course.getCourseId() != null)
            {
                index.put(course.getCourseId(), course);
            }
        }
        List<PlanCourse> selected = new ArrayList<>();
        for (Long courseId : new LinkedHashSet<>(courseIds))
        {
            PlanCourse course = index.get(courseId);
            if (course != null)
            {
                selected.add(course);
            }
        }
        return selected;
    }

    @Override
    public List<PrereqEdge> loadPrereqEdges(Collection<Long> courseIds)
    {
        List<PrereqEdge> all = readMapper.selectAllPrereqEdges();
        if (all == null || all.isEmpty())
        {
            return Collections.emptyList();
        }
        if (courseIds == null || courseIds.isEmpty())
        {
            return all;
        }
        Set<Long> wanted = new LinkedHashSet<>(courseIds);
        List<PrereqEdge> selected = new ArrayList<>();
        for (PrereqEdge edge : all)
        {
            if (edge != null && edge.getCourseId() != null && wanted.contains(edge.getCourseId()))
            {
                selected.add(edge);
            }
        }
        return selected;
    }

    @Override
    public Set<Long> loadPassedCourseIds(Long studentId)
    {
        Set<Long> passed = new LinkedHashSet<>();
        if (studentId == null)
        {
            return passed;
        }
        List<Long> courseIds = readMapper.selectPassedCourseIds(studentId);
        if (courseIds != null)
        {
            for (Long courseId : courseIds)
            {
                if (courseId != null)
                {
                    passed.add(courseId);
                }
            }
        }
        return passed;
    }

    @Override
    public List<CurriculumRule> loadCurriculumRules(Long majorId, Integer entryYear)
    {
        if (majorId == null)
        {
            return Collections.emptyList();
        }
        Integer year = entryYear == null ? Integer.valueOf(LocalDate.now().getYear()) : entryYear;
        Long curriculumId = readMapper.selectCurriculumId(majorId, year);
        if (curriculumId == null)
        {
            return Collections.emptyList();
        }
        List<CurriculumRule> rules = readMapper.selectCurriculumRules(curriculumId);
        return rules == null ? Collections.<CurriculumRule>emptyList() : rules;
    }

    @Override
    public Set<Long> loadOfferedCourseIds(Long termId)
    {
        Set<Long> offered = new LinkedHashSet<>();
        if (termId == null)
        {
            return offered;
        }
        List<Long> courseIds = readMapper.selectOfferedCourseIds(termId);
        if (courseIds != null)
        {
            for (Long courseId : courseIds)
            {
                if (courseId != null)
                {
                    offered.add(courseId);
                }
            }
        }
        return offered;
    }

    @Override
    public Map<Long, List<String>> loadScheduleSlots(Long termId)
    {
        if (termId == null)
        {
            return null;
        }
        List<OfferingSlot> schedules = readMapper.selectOfferingSchedules(termId);
        if (schedules == null || schedules.isEmpty())
        {
            return null;
        }
        Map<Long, List<String>> slots = new LinkedHashMap<>();
        for (OfferingSlot schedule : schedules)
        {
            if (schedule == null || schedule.getCourseId() == null)
            {
                continue;
            }
            List<String> tokens = splitSchedule(schedule.getScheduleText());
            if (!tokens.isEmpty())
            {
                slots.put(schedule.getCourseId(), tokens);
            }
        }
        return slots.isEmpty() ? null : slots;
    }

    /**
     * 把 "Mon 10:00-11:50; Wed 14:00-15:50" 拆成可比较的时间槽。
     *
     * @param scheduleText 排课原文
     * @return 时间槽列表
     */
    private static List<String> splitSchedule(String scheduleText)
    {
        List<String> tokens = new ArrayList<>();
        if (scheduleText == null || scheduleText.trim().isEmpty())
        {
            return tokens;
        }
        String normalized = scheduleText;
        for (String separator : SCHEDULE_SEPARATORS)
        {
            normalized = normalized.replace(separator, "|");
        }
        for (String part : normalized.split("\\|"))
        {
            String token = part.trim();
            if (!token.isEmpty() && !tokens.contains(token))
            {
                tokens.add(token);
            }
        }
        return tokens;
    }
}
