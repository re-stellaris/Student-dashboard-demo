package com.ruoyi.education;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import com.ruoyi.education.common.EducationConstants;
import com.ruoyi.education.common.enums.PlanItemStatus;
import com.ruoyi.education.common.enums.RequirementType;
import com.ruoyi.education.planning.dto.PlanItemValidation;
import com.ruoyi.education.planning.dto.PlanValidationResult;
import com.ruoyi.education.planning.dto.RecommendedCourse;
import com.ruoyi.education.planning.rule.CurriculumRule;
import com.ruoyi.education.planning.rule.PlanCourse;
import com.ruoyi.education.planning.rule.PlanRuleEngine;
import com.ruoyi.education.planning.rule.PlanRuleInput;
import com.ruoyi.education.planning.rule.PrereqEdge;

/**
 * 规划规则引擎测试：合法规划、缺少先修课、超出学分上限、可解释说明等。
 *
 * <p>全部使用纯数据夹具，不启动 Spring、不连数据库、不构造课程域实体。</p>
 *
 * @author ruoyi
 */
class PlanRuleEngineTest
{
    private static final long CSC1001 = 1001L;

    private static final long CSC2001 = 2001L;

    private static final long CSC3100 = 3100L;

    private static final long CSC4001 = 4001L;

    private static final PlanRuleEngine ENGINE = new PlanRuleEngine();

    @DisplayName("合法规划：先修已满足、培养方案内、未超学分上限 → PASS")
    @Test
    void validPlan()
    {
        PlanRuleInput input = new PlanRuleInput()
                .setTermId(1L)
                .setCourses(List.of(course(CSC3100, "CSC3100", "数据结构", "3.0")))
                .setPrereqEdges(List.of(edge(CSC3100, CSC1001, 1)))
                .setReferenceCourses(List.of(course(CSC1001, "CSC1001", "程序设计基础", "3.0")))
                .setPassedCourseIds(setOf(CSC1001))
                .setCurriculumRules(List.of(rule(CSC3100, RequirementType.REQUIRED, "专业核心")));

        PlanValidationResult result = ENGINE.evaluate(input);

        assertEquals(EducationConstants.VALIDATION_STATE_PASS, result.getState());
        assertEquals(new BigDecimal("3.0"), result.getTotalUnits());
        assertEquals(new BigDecimal("21.0"), result.getUnitLimit());
        assertTrue(result.getMessages().isEmpty(), "合法规划不应有计划级提示");

        PlanItemValidation item = item(result, CSC3100);
        assertEquals(PlanItemStatus.VALID.getCode(), item.getValidationStatus());
        assertEquals("", item.getValidationMessage());
        assertTrue(item.getReason().contains("专业必修"), "说明应包含培养方案要求类型：" + item.getReason());
        assertTrue(item.getReason().contains("CSC1001"), "说明应包含已通过的先修课程：" + item.getReason());
        assertTrue(item.getReason().contains("先修"), "说明应体现先修判定：" + item.getReason());
    }

    @DisplayName("缺少先修课：明细 INVALID，整体 FAIL，说明可读")
    @Test
    void missingPrerequisite()
    {
        PlanRuleInput input = new PlanRuleInput()
                .setTermId(1L)
                .setCourses(List.of(course(CSC4001, "CSC4001", "算法设计", "3.0")))
                .setPrereqEdges(List.of(edge(CSC4001, CSC3100, 1)))
                .setReferenceCourses(List.of(course(CSC3100, "CSC3100", "数据结构", "3.0")))
                .setPassedCourseIds(setOf(CSC1001))
                .setCurriculumRules(List.of(rule(CSC4001, RequirementType.REQUIRED, "专业核心")));

        PlanValidationResult result = ENGINE.evaluate(input);

        assertEquals(EducationConstants.VALIDATION_STATE_FAIL, result.getState());
        PlanItemValidation item = item(result, CSC4001);
        assertEquals(PlanItemStatus.INVALID.getCode(), item.getValidationStatus());
        assertEquals("缺少先修课程 CSC3100", item.getValidationMessage());
        assertTrue(item.getReason().startsWith("缺少先修课程："), item.getReason());
        assertTrue(item.getReason().contains("数据结构"), "说明应包含先修课程名称：" + item.getReason());
        assertTrue(item.getReason().contains("CSC3100"), "说明应包含先修课程代码：" + item.getReason());
    }

    @DisplayName("超出学分上限：整体 WARN，并给出中文提示")
    @Test
    void overUnitLimit()
    {
        List<PlanCourse> courses = new ArrayList<>();
        List<CurriculumRule> rules = new ArrayList<>();
        for (int i = 0; i < 8; i++)
        {
            long courseId = 5000L + i;
            courses.add(course(courseId, "GEN" + (1000 + i), "通识课程" + i, "3.0"));
            rules.add(rule(courseId, RequirementType.REQUIRED, ""));
        }
        PlanRuleInput input = new PlanRuleInput()
                .setTermId(1L)
                .setCourses(courses)
                .setCurriculumRules(rules);

        PlanValidationResult result = ENGINE.evaluate(input);

        assertEquals(EducationConstants.VALIDATION_STATE_WARN, result.getState());
        assertEquals(new BigDecimal("24.0"), result.getTotalUnits());
        assertTrue(result.getMessages().contains("超出学分上限 21.0：当前 24.0"),
                "提示文案需与接口契约一致：" + result.getMessages());
        for (PlanItemValidation item : result.getItems())
        {
            assertEquals(PlanItemStatus.VALID.getCode(), item.getValidationStatus());
        }
    }

    @DisplayName("已修读课程：明细 INVALID 且说明可读")
    @Test
    void alreadyPassedCourse()
    {
        PlanRuleInput input = new PlanRuleInput()
                .setTermId(1L)
                .setCourses(List.of(course(CSC1001, "CSC1001", "程序设计基础", "3.0")))
                .setPassedCourseIds(setOf(CSC1001))
                .setCurriculumRules(List.of(rule(CSC1001, RequirementType.REQUIRED, "")));

        PlanValidationResult result = ENGINE.evaluate(input);

        assertEquals(EducationConstants.VALIDATION_STATE_FAIL, result.getState());
        PlanItemValidation item = item(result, CSC1001);
        assertEquals(PlanItemStatus.INVALID.getCode(), item.getValidationStatus());
        assertEquals("已通过该课程，无需重复修读", item.getReason());
        assertEquals("课程成绩已通过", item.getValidationMessage());
    }

    @DisplayName("目标学期未开课：明细 WARN，仍可解释")
    @Test
    void courseNotOfferedInTerm()
    {
        PlanRuleInput input = new PlanRuleInput()
                .setTermId(1L)
                .setCourses(List.of(course(CSC2001, "CSC2001", "离散数学", "3.0")))
                .setCurriculumRules(List.of(rule(CSC2001, RequirementType.GE, "")))
                .setOfferedCourseIds(Set.of(9999L));

        PlanValidationResult result = ENGINE.evaluate(input);

        assertEquals(EducationConstants.VALIDATION_STATE_WARN, result.getState());
        PlanItemValidation item = item(result, CSC2001);
        assertEquals(PlanItemStatus.WARN.getCode(), item.getValidationStatus());
        assertTrue(item.getValidationMessage().contains("目标学期未开课"), item.getValidationMessage());
        assertTrue(item.getReason().contains("目标学期未开课"), item.getReason());
        assertTrue(item.getReason().contains("通识教育"), item.getReason());
    }

    @DisplayName("上课时间冲突：后选课程 WARN 并指出冲突课程")
    @Test
    void timeConflict()
    {
        Map<Long, List<String>> slots = new LinkedHashMap<>();
        slots.put(CSC2001, List.of("Mon 10:00-11:50"));
        slots.put(CSC3100, List.of("Mon 10:00-11:50"));

        PlanRuleInput input = new PlanRuleInput()
                .setTermId(1L)
                .setCourses(List.of(
                        course(CSC2001, "CSC2001", "离散数学", "3.0"),
                        course(CSC3100, "CSC3100", "数据结构", "3.0")))
                .setCurriculumRules(List.of(
                        rule(CSC2001, RequirementType.GE, ""),
                        rule(CSC3100, RequirementType.REQUIRED, "")))
                .setScheduleSlots(slots);

        PlanValidationResult result = ENGINE.evaluate(input);

        PlanItemValidation first = item(result, CSC2001);
        PlanItemValidation second = item(result, CSC3100);
        assertEquals(PlanItemStatus.VALID.getCode(), first.getValidationStatus());
        assertEquals(PlanItemStatus.WARN.getCode(), second.getValidationStatus());
        assertTrue(second.getValidationMessage().contains("上课时间与已选课程冲突"), second.getValidationMessage());
        assertTrue(second.getValidationMessage().contains("CSC2001"), second.getValidationMessage());
    }

    @DisplayName("先修数据成环：整体 FAIL，并给出环内课程提示")
    @Test
    void prerequisiteCycleFailsPlan()
    {
        PlanRuleInput input = new PlanRuleInput()
                .setTermId(1L)
                .setCourses(List.of(course(CSC3100, "CSC3100", "数据结构", "3.0")))
                .setPrereqEdges(List.of(edge(CSC3100, CSC4001, 1), edge(CSC4001, CSC3100, 1)))
                .setCurriculumRules(List.of(rule(CSC3100, RequirementType.REQUIRED, "")));

        PlanValidationResult result = ENGINE.evaluate(input);

        assertEquals(EducationConstants.VALIDATION_STATE_FAIL, result.getState());
        assertTrue(result.getMessages().stream().anyMatch(message -> message.contains("先修关系存在环")),
                "应提示先修环：" + result.getMessages());
    }

    @DisplayName("每门课程的 reason 均非空（契约要求可解释说明）")
    @Test
    void everyItemHasReadableReason()
    {
        PlanRuleInput input = new PlanRuleInput()
                .setTermId(1L)
                .setCourses(List.of(
                        course(CSC3100, "CSC3100", "数据结构", "3.0"),
                        course(CSC4001, "CSC4001", "算法设计", "3.0"),
                        course(CSC1001, "CSC1001", "程序设计基础", "3.0"),
                        course(CSC2001, "CSC2001", "离散数学", "3.0")))
                .setPrereqEdges(List.of(edge(CSC3100, CSC1001, 1), edge(CSC4001, CSC3100, 1)))
                .setPassedCourseIds(setOf(CSC1001))
                .setCurriculumRules(List.of(
                        rule(CSC3100, RequirementType.REQUIRED, "专业核心"),
                        rule(CSC4001, RequirementType.REQUIRED, "专业核心"),
                        rule(CSC1001, RequirementType.REQUIRED, "")))
                .setOfferedCourseIds(setOf(CSC3100, CSC4001, CSC1001, CSC2001));

        PlanValidationResult result = ENGINE.evaluate(input);

        assertEquals(4, result.getItems().size());
        for (PlanItemValidation item : result.getItems())
        {
            assertNotNull(item.getReason(), "reason 不能为 null：" + item);
            assertFalse(item.getReason().trim().isEmpty(), "reason 不能为空：" + item);
            assertFalse(item.getValidationStatus().isEmpty(), "validationStatus 不能为空：" + item);
        }
    }

    @DisplayName("空规划：WARN 且给出提示")
    @Test
    void emptyPlan()
    {
        PlanValidationResult result = ENGINE.evaluate(new PlanRuleInput().setTermId(1L));

        assertEquals(EducationConstants.VALIDATION_STATE_WARN, result.getState());
        assertTrue(result.getItems().isEmpty());
        assertFalse(result.getMessages().isEmpty());
        assertEquals(new BigDecimal("0"), result.getTotalUnits());
    }

    @DisplayName("规则推荐：先修已满足优先，缺少先修课给出缺失清单")
    @Test
    void recommendCourses()
    {
        PlanRuleInput input = new PlanRuleInput()
                .setTermId(1L)
                .setCourses(List.of(
                        course(CSC4001, "CSC4001", "算法设计", "3.0"),
                        course(CSC3100, "CSC3100", "数据结构", "3.0"),
                        course(CSC2001, "CSC2001", "离散数学", "3.0"),
                        course(CSC1001, "CSC1001", "程序设计基础", "3.0")))
                .setPrereqEdges(List.of(edge(CSC3100, CSC1001, 1), edge(CSC4001, CSC3100, 1)))
                .setPassedCourseIds(setOf(CSC1001))
                .setCurriculumRules(List.of(
                        rule(CSC4001, RequirementType.ELECTIVE, ""),
                        rule(CSC3100, RequirementType.REQUIRED, ""),
                        rule(CSC2001, RequirementType.GE, ""),
                        rule(CSC1001, RequirementType.REQUIRED, "")));

        List<RecommendedCourse> recommended = ENGINE.recommend(input, 10);

        assertEquals(3, recommended.size(), "已修读课程不应再被推荐：" + recommended.size());
        assertEquals(Long.valueOf(CSC3100), recommended.get(0).getCourseId(), "先修已满足的必修课应排在最前");
        assertTrue(recommended.get(0).getMissingPrereqs().isEmpty());
        assertFalse(recommended.get(0).getReason().trim().isEmpty());

        RecommendedCourse elective = recommended.stream()
                .filter(row -> Long.valueOf(CSC4001).equals(row.getCourseId()))
                .findFirst()
                .orElseThrow();
        assertEquals(List.of("CSC3100"), elective.getMissingPrereqs());
        assertTrue(elective.getReason().startsWith("缺少先修课程："), elective.getReason());
    }

    // ------------------------------------------------------------------
    // 测试夹具
    // ------------------------------------------------------------------

    private static PlanCourse course(long courseId, String code, String title, String units)
    {
        PlanCourse course = new PlanCourse(courseId, code, title, new BigDecimal(units));
        course.setCategory("Major");
        return course;
    }

    private static CurriculumRule rule(Long courseId, RequirementType type, String section)
    {
        CurriculumRule rule = new CurriculumRule();
        rule.setCourseId(courseId);
        rule.setRequirementType(type.getCode());
        rule.setSection(section);
        rule.setMinUnits(new BigDecimal("3.0"));
        return rule;
    }

    private static PrereqEdge edge(long courseId, long prereqCourseId, int groupNo)
    {
        return new PrereqEdge(courseId, prereqCourseId, groupNo);
    }

    private static Set<Long> setOf(Long... ids)
    {
        return new LinkedHashSet<>(Arrays.asList(ids));
    }

    private static PlanItemValidation item(PlanValidationResult result, long courseId)
    {
        return result.getItems().stream()
                .filter(row -> Long.valueOf(courseId).equals(row.getCourseId()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("未找到课程 " + courseId + " 的校验结果"));
    }
}
