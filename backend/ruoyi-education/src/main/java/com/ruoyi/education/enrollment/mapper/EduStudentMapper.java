package com.ruoyi.education.enrollment.mapper;

import java.util.List;
import com.ruoyi.education.enrollment.domain.EduStudent;
import com.ruoyi.education.enrollment.dto.CreditCategoryRow;
import com.ruoyi.education.enrollment.dto.CurriculumCreditRow;
import com.ruoyi.education.enrollment.dto.PlanStatusCountRow;

/**
 * 学生学籍 Mapper 接口
 *
 * <p>除学籍 CRUD 外，本 Mapper 还承载“学分进度”所需的只读聚合：
 * 适用培养方案的学分要求、培养方案要求分类汇总、学习规划状态计数。
 * 这些查询只读，避免与 curriculum / planning 域写入逻辑耦合。</p>
 *
 * @author ruoyi-education
 */
public interface EduStudentMapper
{
    /**
     * 按主键查询学生（联查专业信息）
     *
     * @param studentId 学生ID
     * @return 学生
     */
    EduStudent selectStudentById(Long studentId);

    /**
     * 按系统用户ID查询学生（“本人”接口的唯一身份入口）
     *
     * @param sysUserId 系统用户ID
     * @return 学生
     */
    EduStudent selectStudentBySysUserId(Long sysUserId);

    /**
     * 按学号查询学生
     *
     * @param studentNo 学号
     * @return 学生
     */
    EduStudent selectStudentByNo(String studentNo);

    /**
     * 查询学生列表
     *
     * @param student 查询条件（学号、姓名、专业、入学年）
     * @return 学生集合
     */
    List<EduStudent> selectStudentList(EduStudent student);

    /**
     * 新增学生
     *
     * @param student 学生
     * @return 影响行数
     */
    int insertStudent(EduStudent student);

    /**
     * 修改学生
     *
     * @param student 学生
     * @return 影响行数
     */
    int updateStudent(EduStudent student);

    /**
     * 解析学生适用的培养方案（major_id + entry_year 命中 effective 区间，取最新版本）
     *
     * @param studentId 学生ID
     * @return 培养方案学分要求；无匹配时返回 null
     */
    CurriculumCreditRow selectApplicableCurriculum(Long studentId);

    /**
     * 按要求类型汇总培养方案条目学分
     *
     * @param curriculumId 培养方案ID
     * @return 分类汇总行（requirement_type + SUM(min_units)）
     */
    List<CreditCategoryRow> selectCurriculumRequirementSummary(Long curriculumId);

    /**
     * 按状态汇总学生学习规划数量
     *
     * @param studentId 学生ID
     * @return 状态计数行
     */
    List<PlanStatusCountRow> selectPlanStatusSummary(Long studentId);
}
