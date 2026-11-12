package com.ruoyi.education.advisor.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.education.advisor.dto.AdvisorCategoryCreditRow;
import com.ruoyi.education.advisor.dto.AdvisorGradeRow;
import com.ruoyi.education.advisor.dto.AdvisorStudentQuery;
import com.ruoyi.education.advisor.dto.AdvisorStudentRow;
import com.ruoyi.education.advisor.dto.AdvisorTermRow;

/**
 * 导师看板只读 Mapper
 *
 * <p>只做“批量读取 + 联表回填”，不做任何写操作；风险规则、GPA 聚合等逻辑
 * 都在 Service 与 {@code rule} 包里，便于单元测试。</p>
 *
 * <p>已知限制：当前库没有 advisor-student（导师-学生）关系表，因此这里按
 * “TEACHER/ADMIN 可看全部学生”实现，查询不带导师过滤条件。</p>
 *
 * @author ruoyi-education
 */
public interface EduAdvisorMapper
{
    /**
     * 查询学生读模型（可按入学年份 / 专业 / 关键字过滤）
     *
     * @param query 查询条件，可为空对象
     * @return 学生读模型集合（按学号升序）
     */
    List<AdvisorStudentRow> selectAdvisorStudents(AdvisorStudentQuery query);

    /**
     * 按主键查询单个学生读模型
     *
     * @param studentId 学生ID
     * @return 学生读模型；不存在时返回 null
     */
    AdvisorStudentRow selectAdvisorStudentById(@Param("studentId") Long studentId);

    /**
     * 查询成绩行（联查课程目录回填课程名/学分，联查学期取 term_code）
     *
     * @param studentId 学生ID；null 表示查询全部学生（总览聚合用）
     * @return 成绩行集合（按学期代码、记录ID升序）
     */
    List<AdvisorGradeRow> selectAdvisorGradeRows(@Param("studentId") Long studentId);

    /**
     * 查询全部学期（推算当前学年 / 最近学期）
     *
     * @return 学期集合（按学期代码升序）
     */
    List<AdvisorTermRow> selectAdvisorTerms();

    /**
     * 按分类汇总某学生已获得学分
     *
     * @param studentId 学生ID
     * @return 分类学分行（学分降序）
     */
    List<AdvisorCategoryCreditRow> selectCategoryCredits(@Param("studentId") Long studentId);
}
