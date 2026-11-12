package com.ruoyi.education.enrollment.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.education.enrollment.domain.EduCourseOffering;

/**
 * 学期开课 Mapper 接口
 *
 * @author ruoyi-education
 */
public interface EduCourseOfferingMapper
{
    /**
     * 查询开课列表（联查 edu_course / edu_term / edu_teacher）
     *
     * @param offering 查询条件（termId、courseId、teacherId、status）
     * @return 开课集合
     */
    List<EduCourseOffering> selectOfferingList(EduCourseOffering offering);

    /**
     * 按主键查询开课（含课程、学期、教师回显字段）
     *
     * @param offeringId 开课ID
     * @return 开课
     */
    EduCourseOffering selectOfferingById(Long offeringId);

    /**
     * 查询指定教师所授课程（含实时选课人数与作业数量）
     *
     * @param teacherId 教师ID
     * @return 开课集合
     */
    List<EduCourseOffering> selectOfferingByTeacherId(Long teacherId);

    /**
     * 按课程 + 学期查询开课
     *
     * @param courseId 课程ID
     * @param termId 学期ID
     * @return 开课
     */
    EduCourseOffering selectOfferingByCourseAndTerm(@Param("courseId") Long courseId, @Param("termId") Long termId);

    /**
     * 新增开课
     *
     * @param offering 开课
     * @return 影响行数
     */
    int insertOffering(EduCourseOffering offering);

    /**
     * 修改开课
     *
     * @param offering 开课
     * @return 影响行数
     */
    int updateOffering(EduCourseOffering offering);

    /**
     * 批量删除开课
     *
     * @param offeringIds 开课ID数组
     * @return 影响行数
     */
    int deleteOfferingByIds(Long[] offeringIds);

    /**
     * 原子增减已选人数（delta 为 1 / -1），不会减到 0 以下
     *
     * @param offeringId 开课ID
     * @param delta 增减量
     * @return 影响行数
     */
    int incrementEnrolledCount(@Param("offeringId") Long offeringId, @Param("delta") int delta);
}
