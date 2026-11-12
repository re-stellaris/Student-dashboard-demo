package com.ruoyi.education.enrollment.mapper;

import java.util.List;
import com.ruoyi.education.enrollment.dto.TermGradeRow;
import com.ruoyi.education.enrollment.domain.EduCourseRecord;
import com.ruoyi.education.enrollment.dto.CreditCategoryRow;

/**
 * 已修课程与成绩 Mapper 接口
 *
 * @author ruoyi-education
 */
public interface EduCourseRecordMapper
{
    /**
     * 查询成绩列表（联查 edu_course 回填学分/课程名，联查 edu_term 取学期代码）
     *
     * @param record 查询条件（studentId、termCode、category）
     * @return 成绩集合
     */
    List<EduCourseRecord> selectRecordList(EduCourseRecord record);

    /**
     * 按主键查询成绩记录
     *
     * @param recordId 成绩记录ID
     * @return 成绩记录
     */
    EduCourseRecord selectRecordById(Long recordId);

    /**
     * 查询某学生的全部成绩记录
     *
     * @param studentId 学生ID
     * @return 成绩集合
     */
    List<EduCourseRecord> selectByStudentId(Long studentId);

    /**
     * 查询某学生已通过的课程ID集合（passed = '1' 且 record_status = 'COMPLETED'）
     *
     * @param studentId 学生ID
     * @return 课程ID集合
     */
    List<Long> selectPassedCourseIds(Long studentId);

    /**
     * 查询某学生已完成的成绩记录（passed = '1' 且 record_status = 'COMPLETED'）
     *
     * @param studentId 学生ID
     * @return 成绩集合
     */
    List<EduCourseRecord> selectCompletedRecords(Long studentId);

    /**
     * 按分类汇总已获得学分
     *
     * @param studentId 学生ID
     * @return 分类汇总行（category + SUM(units)）
     */
    List<CreditCategoryRow> selectCategorySummary(Long studentId);

    /**
     * 按学期汇总成绩行（GPA 趋势计算用）
     *
     * @param studentId 学生ID
     * @return 学期成绩行
     */
    List<TermGradeRow> selectTermGradeRows(Long studentId);

    /**
     * 新增成绩记录
     *
     * @param record 成绩记录
     * @return 影响行数
     */
    int insertRecord(EduCourseRecord record);

    /**
     * 修改成绩记录
     *
     * @param record 成绩记录
     * @return 影响行数
     */
    int updateRecord(EduCourseRecord record);

    /**
     * 批量删除成绩记录（逻辑删除 del_flag = '2'）
     *
     * @param recordIds 成绩记录ID数组
     * @return 影响行数
     */
    int deleteRecordByIds(Long[] recordIds);
}
