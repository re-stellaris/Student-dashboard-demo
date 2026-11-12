package com.ruoyi.education.assignment.mapper;

import java.math.BigDecimal;
import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.education.assignment.domain.EduSubmission;

/**
 * 作业提交 Mapper 接口
 *
 * @author ruoyi-education
 */
public interface EduSubmissionMapper
{
    /**
     * 查询提交列表（联查作业与学生）
     *
     * @param submission 查询条件（assignmentId、studentId、status）
     * @return 提交集合
     */
    List<EduSubmission> selectSubmissionList(EduSubmission submission);

    /**
     * 查询指定教师所授课程的全部提交
     *
     * @param teacherId 教师ID
     * @return 提交集合
     */
    List<EduSubmission> selectSubmissionListByTeacherId(Long teacherId);

    /**
     * 按主键查询提交
     *
     * @param submissionId 提交ID
     * @return 提交
     */
    EduSubmission selectSubmissionById(Long submissionId);

    /**
     * 按作业 + 学生查询提交
     *
     * @param assignmentId 作业ID
     * @param studentId 学生ID
     * @return 提交
     */
    EduSubmission selectByAssignmentAndStudent(@Param("assignmentId") Long assignmentId,
            @Param("studentId") Long studentId);

    /**
     * 查询某学生的全部提交
     *
     * @param studentId 学生ID
     * @return 提交集合
     */
    List<EduSubmission> selectByStudentId(Long studentId);

    /**
     * 新增提交
     *
     * @param submission 提交
     * @return 影响行数
     */
    int insertSubmission(EduSubmission submission);

    /**
     * 学生重新提交（覆盖内容与附件，状态回到 SUBMITTED）
     *
     * @param submission 提交
     * @return 影响行数
     */
    int updateSubmission(EduSubmission submission);

    /**
     * 教师评分
     *
     * @param submissionId 提交ID
     * @param grade 得分
     * @param feedback 评语
     * @param updateBy 更新者
     * @return 影响行数
     */
    int updateGrade(@Param("submissionId") Long submissionId, @Param("grade") BigDecimal grade,
            @Param("feedback") String feedback, @Param("updateBy") String updateBy);

    /**
     * 统计指定教师待批改的提交数量（status = SUBMITTED）
     *
     * @param teacherId 教师ID
     * @return 待批改数量
     */
    int countUngradedByTeacher(Long teacherId);

    /**
     * 统计指定学生待提交的作业数量（已发布且未提交，且学生已选该开课）
     *
     * @param studentId 学生ID
     * @return 待提交数量
     */
    int countPendingByStudent(Long studentId);
}
