package com.ruoyi.education.course.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.education.course.domain.EduCoursePrereq;

/**
 * 课程先修关系 数据层
 *
 * @author ruoyi
 */
public interface EduCoursePrereqMapper
{
    /**
     * 查询某门课程的先修关系（关联 edu_course 返回先修课程代码与名称）
     *
     * @param courseId 课程ID
     * @return 先修关系集合
     */
    public List<EduCoursePrereq> selectPrereqByCourseId(@Param("courseId") Long courseId);

    /**
     * 查询全部先修关系（用于构建先修有向图）
     *
     * @return 先修关系集合
     */
    public List<EduCoursePrereq> selectAllPrereq();

    /**
     * 批量新增先修关系
     *
     * @param prereqs 先修关系集合
     * @return 结果
     */
    public int insertPrereqBatch(@Param("prereqs") List<EduCoursePrereq> prereqs);

    /**
     * 删除某门课程的全部先修关系
     *
     * @param courseId 课程ID
     * @return 结果
     */
    public int deletePrereqByCourseId(@Param("courseId") Long courseId);

    /**
     * 统计自依赖（course_id = prereq_course_id）的脏数据数量，正常应为 0
     *
     * @return 数量
     */
    public int countSelfReference();
}
