package com.ruoyi.education.assignment.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.education.assignment.domain.EduAssignment;

/**
 * 作业任务 Mapper 接口
 *
 * @author ruoyi-education
 */
public interface EduAssignmentMapper
{
    /**
     * 查询作业列表（联查开课与课程）
     *
     * @param assignment 查询条件（offeringId、status）
     * @return 作业集合
     */
    List<EduAssignment> selectAssignmentList(EduAssignment assignment);

    /**
     * 查询指定教师所授全部课程的作业
     *
     * @param teacherId 教师ID
     * @return 作业集合
     */
    List<EduAssignment> selectAssignmentListByTeacherId(Long teacherId);

    /**
     * 查询指定学生已选课程中的作业（不含 DRAFT）
     *
     * @param studentId 学生ID
     * @param offeringId 开课ID，可为 null
     * @return 作业集合
     */
    List<EduAssignment> selectAssignmentListByStudentId(@Param("studentId") Long studentId,
            @Param("offeringId") Long offeringId);

    /**
     * 按主键查询作业
     *
     * @param assignmentId 作业ID
     * @return 作业
     */
    EduAssignment selectAssignmentById(Long assignmentId);

    /**
     * 新增作业
     *
     * @param assignment 作业
     * @return 影响行数
     */
    int insertAssignment(EduAssignment assignment);

    /**
     * 修改作业
     *
     * @param assignment 作业
     * @return 影响行数
     */
    int updateAssignment(EduAssignment assignment);

    /**
     * 批量删除作业（逻辑删除 del_flag = '2'）
     *
     * @param assignmentIds 作业ID数组
     * @return 影响行数
     */
    int deleteAssignmentByIds(Long[] assignmentIds);

    /**
     * 统计某开课的作业数量
     *
     * @param offeringId 开课ID
     * @return 作业数量
     */
    int countByOfferingId(@Param("offeringId") Long offeringId);
}
