package com.ruoyi.education.enrollment.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.education.enrollment.domain.EduEnrollment;
import com.ruoyi.education.enrollment.dto.PrerequisiteRow;

/**
 * 选课关系 Mapper 接口
 *
 * @author ruoyi-education
 */
public interface EduEnrollmentMapper
{
    /**
     * 查询选课列表（联查开课、课程、学期、学生）
     *
     * @param enrollment 查询条件（studentId、offeringId、status）
     * @return 选课集合
     */
    List<EduEnrollment> selectEnrollmentList(EduEnrollment enrollment);

    /**
     * 按主键查询选课
     *
     * @param enrollmentId 选课ID
     * @return 选课
     */
    EduEnrollment selectEnrollmentById(Long enrollmentId);

    /**
     * 按学生 + 开课查询选课（含已退课记录，用于重复选课判定）
     *
     * @param studentId 学生ID
     * @param offeringId 开课ID
     * @return 选课
     */
    EduEnrollment selectByStudentAndOffering(@Param("studentId") Long studentId, @Param("offeringId") Long offeringId);

    /**
     * 查询某学生的全部选课
     *
     * @param studentId 学生ID
     * @return 选课集合
     */
    List<EduEnrollment> selectByStudentId(Long studentId);

    /**
     * 新增选课
     *
     * @param enrollment 选课
     * @return 影响行数
     */
    int insertEnrollment(EduEnrollment enrollment);

    /**
     * 更新选课状态（ENROLLED/DROPPED/COMPLETED）
     *
     * @param enrollmentId 选课ID
     * @param status 目标状态
     * @param updateBy 更新者
     * @return 影响行数
     */
    int updateStatus(@Param("enrollmentId") Long enrollmentId, @Param("status") String status,
            @Param("updateBy") String updateBy);

    /**
     * 批量删除选课
     *
     * @param enrollmentIds 选课ID数组
     * @return 影响行数
     */
    int deleteEnrollmentByIds(Long[] enrollmentIds);

    /**
     * 统计某开课的已选人数（status = ENROLLED）
     *
     * @param offeringId 开课ID
     * @return 人数
     */
    int countByOffering(Long offeringId);

    /**
     * 查询某课程的先修课行（group_no + 先修课程信息）
     *
     * @param courseId 课程ID
     * @return 先修行集合
     */
    List<PrerequisiteRow> selectPrereqRows(Long courseId);
}
