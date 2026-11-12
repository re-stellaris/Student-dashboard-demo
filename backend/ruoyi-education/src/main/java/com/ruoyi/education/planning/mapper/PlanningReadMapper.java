package com.ruoyi.education.planning.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import com.ruoyi.education.planning.rule.CurriculumRule;
import com.ruoyi.education.planning.rule.PlanCourse;
import com.ruoyi.education.planning.rule.PrereqEdge;
import com.ruoyi.education.planning.spi.OfferingSlot;
import com.ruoyi.education.planning.spi.StudentProfile;

/**
 * 规划域只读查询（注解 SQL，无 XML）。
 *
 * <p>仅用于 {@code DefaultPlanningReadPorts} 的默认端口实现：只读、不做任何写入，
 * 不属于课程 / 培养方案 / 选课域的业务逻辑。所属域提供更权威的 Service 后，
 * 可替换 {@code DefaultPlanningReadPorts} 而不影响规划域其余代码。</p>
 *
 * @author ruoyi
 */
public interface PlanningReadMapper
{
    /**
     * 读取课程目录（按课程ID批量过滤在内存完成，避免动态 SQL）。
     *
     * @return 课程快照
     */
    @Select("select course_id as courseId, code, ifnull(nullif(title_zh, ''), title_en) as title, "
            + "credits as units, category, level from edu_course where del_flag = '0'")
    List<PlanCourse> selectCourseCatalog();

    /**
     * 读取全部先修边。
     *
     * @return 先修边
     */
    @Select("select course_id as courseId, prereq_course_id as prereqCourseId, group_no as groupNo "
            + "from edu_course_prereq")
    List<PrereqEdge> selectAllPrereqEdges();

    /**
     * 按系统用户ID反查学生ID。
     *
     * @param sysUserId 系统用户ID
     * @return 学生ID
     */
    @Select("select student_id from edu_student where sys_user_id = #{sysUserId} and del_flag = '0' limit 1")
    Long selectStudentIdBySysUserId(@Param("sysUserId") Long sysUserId);

    /**
     * 查询学生学籍快照。
     *
     * @param studentId 学生ID
     * @return 学籍快照
     */
    @Select("select student_id as studentId, student_no as studentNo, name, major_id as majorId, "
            + "entry_year as entryYear, college from edu_student where student_id = #{studentId} and del_flag = '0'")
    StudentProfile selectStudentProfile(@Param("studentId") Long studentId);

    /**
     * 读取已通过课程ID。
     *
     * @param studentId 学生ID
     * @return 课程ID列表
     */
    @Select("select course_id from edu_course_record where student_id = #{studentId} and del_flag = '0' "
            + "and passed = '1' and record_status = 'COMPLETED' and course_id is not null")
    List<Long> selectPassedCourseIds(@Param("studentId") Long studentId);

    /**
     * 按专业 + 入学年份解析适用培养方案ID。
     *
     * @param majorId 专业ID
     * @param entryYear 入学年份
     * @return 培养方案ID
     */
    @Select("select curriculum_id from edu_curriculum where major_id = #{majorId} and del_flag = '0' "
            + "and status = '0' "
            + "and (effective_from is null or effective_from <= #{entryYear}) "
            + "and (effective_to is null or effective_to >= #{entryYear}) "
            + "order by version_year desc limit 1")
    Long selectCurriculumId(@Param("majorId") Long majorId, @Param("entryYear") Integer entryYear);

    /**
     * 读取培养方案课程要求条目。
     *
     * @param curriculumId 培养方案ID
     * @return 条目
     */
    @Select("select item_id as itemId, course_id as courseId, course_code as courseCode, "
            + "requirement_type as requirementType, section, stream, min_units as minUnits, sort_order as sortOrder "
            + "from edu_curriculum_item where curriculum_id = #{curriculumId} and course_id is not null")
    List<CurriculumRule> selectCurriculumRules(@Param("curriculumId") Long curriculumId);

    /**
     * 读取目标学期开放选课的课程ID。
     *
     * @param termId 学期ID
     * @return 课程ID列表
     */
    @Select("select course_id from edu_course_offering where term_id = #{termId} and status = '0'")
    List<Long> selectOfferedCourseIds(@Param("termId") Long termId);

    /**
     * 读取目标学期的排课片段。
     *
     * @param termId 学期ID
     * @return 排课片段
     */
    @Select("select course_id as courseId, schedule_text as scheduleText from edu_course_offering "
            + "where term_id = #{termId} and status = '0'")
    List<OfferingSlot> selectOfferingSchedules(@Param("termId") Long termId);
}
