package com.ruoyi.education.assignment.service;

import java.util.List;
import com.ruoyi.education.assignment.domain.EduAssignment;
import com.ruoyi.education.assignment.domain.EduSubmission;
import com.ruoyi.education.assignment.dto.GradeBody;
import com.ruoyi.education.assignment.dto.SubmissionBody;

/**
 * 作业与提交 业务接口
 *
 * <p>教师侧接口均校验 edu_course_offering.teacher_id == 当前教师；
 * 学生侧接口均校验 edu_enrollment 存在且未退课。</p>
 *
 * @author ruoyi-education
 */
public interface IEduAssignmentService
{
    /**
     * 查询作业列表（按当前用户角色收窄范围）
     *
     * @param query 查询条件（offeringId、status）
     * @return 作业集合
     */
    List<EduAssignment> selectAssignmentList(EduAssignment query);

    /**
     * 查询作业详情（按当前用户角色校验归属）
     *
     * @param assignmentId 作业ID
     * @return 作业
     */
    EduAssignment selectAssignmentById(Long assignmentId);

    /**
     * 教师发布作业
     *
     * @param assignment 作业
     * @return 影响行数
     */
    int insertAssignment(EduAssignment assignment);

    /**
     * 修改作业（必须为本人授课课程）
     *
     * @param assignment 作业
     * @return 影响行数
     */
    int updateAssignment(EduAssignment assignment);

    /**
     * 批量删除作业（必须为本人授课课程）
     *
     * @param assignmentIds 作业ID数组
     * @return 影响行数
     */
    int deleteAssignmentByIds(Long[] assignmentIds);

    /**
     * 查询提交列表（教师看本人课程，学生仅看本人）
     *
     * @param query 查询条件（assignmentId、status）
     * @return 提交集合
     */
    List<EduSubmission> selectSubmissionList(EduSubmission query);

    /**
     * 学生提交作业（必须已选该开课）
     *
     * @param body 提交内容
     * @return 提交ID
     */
    Long submitAssignment(SubmissionBody body);

    /**
     * 教师评分（必须为本人授课课程）
     *
     * @param body 评分内容
     * @return 影响行数
     */
    int gradeSubmission(GradeBody body);
}
