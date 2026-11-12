package com.ruoyi.education.transcript.mapper;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.education.enrollment.domain.EduCourseRecord;
import com.ruoyi.education.transcript.domain.TranscriptCourseRef;
import com.ruoyi.education.transcript.domain.TranscriptRecordRef;
import com.ruoyi.education.transcript.domain.TranscriptTermRef;

/**
 * 成绩单导入 Mapper
 *
 * <p>只做三件与本次导入强相关的事，避免改动 enrollment 域已有的 SQL：</p>
 * <ol>
 *   <li>批量解析学期代码 / 课程代码（一次查询，不用逐行查库）；</li>
 *   <li>批量取当前学生已存在的成绩记录（判定新增 / 覆盖 / 跳过）；</li>
 *   <li>自填成绩的写入与回写 {@code edu_student.total_credits / cum_gpa}。</li>
 * </ol>
 *
 * <p>所有列名均来自 backend/sql/edu/V1__edu_schema.sql。</p>
 *
 * @author ruoyi-education
 */
public interface EduTranscriptMapper
{
    /**
     * 按学期代码批量查询学期ID
     *
     * @param termCodes 学期代码集合（非空，调用方已归一化为大写）
     * @return 学期代码 → 学期ID 投影
     */
    List<TranscriptTermRef> selectTermRefs(@Param("termCodes") Collection<String> termCodes);

    /**
     * 按课程代码批量查询课程目录快照
     *
     * @param courseCodes 课程代码集合（非空，调用方已归一化为大写）
     * @return 课程目录快照
     */
    List<TranscriptCourseRef> selectCourseRefs(@Param("courseCodes") Collection<String> courseCodes);

    /**
     * 查询某学生在给定课程代码下的全部成绩记录（不区分 del_flag）
     *
     * <p>不过滤 {@code del_flag}，因为唯一键 {@code uk_record_student_course_term}
     * 不包含该列：逻辑删除的行仍然占用主键，必须被识别出来才能避免插入冲突。</p>
     *
     * @param studentId 学生ID
     * @param courseCodes 课程代码集合（非空）
     * @return 已存在记录投影
     */
    List<TranscriptRecordRef> selectRecordRefs(@Param("studentId") Long studentId,
            @Param("courseCodes") Collection<String> courseCodes);

    /**
     * 新增自填成绩记录
     *
     * @param record 成绩记录（source 恒为 TRANSCRIPT_UPLOAD）
     * @return 影响行数
     */
    int insertTranscriptRecord(EduCourseRecord record);

    /**
     * 覆盖自填成绩记录（只用于 source = 'TRANSCRIPT_UPLOAD' 的行）
     *
     * <p>不修改 course_code / term_id / student_id 这三个唯一键列；同时把 del_flag 复位为 '0'，
     * 使「删除后重新导入」也能恢复为正常数据。</p>
     *
     * @param record 成绩记录（recordId 必填）
     * @return 影响行数
     */
    int updateTranscriptRecord(EduCourseRecord record);

    /**
     * 回写学生累计学分与 GPA（只统计 passed='1' 且 record_status='COMPLETED'，由调用方汇总）
     *
     * @param studentId 学生ID
     * @param totalCredits 已获得总学分
     * @param cumGpa 累计 GPA
     * @param updateBy 更新人（当前登录用户名）
     * @return 影响行数
     */
    int updateStudentSummary(@Param("studentId") Long studentId,
            @Param("totalCredits") BigDecimal totalCredits,
            @Param("cumGpa") BigDecimal cumGpa,
            @Param("updateBy") String updateBy);
}
