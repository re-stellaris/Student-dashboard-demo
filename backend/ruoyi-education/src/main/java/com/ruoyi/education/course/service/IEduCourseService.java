package com.ruoyi.education.course.service;

import java.util.List;
import com.ruoyi.education.course.domain.EduCourse;
import com.ruoyi.education.course.domain.EduCoursePrereq;
import com.ruoyi.education.course.dto.CourseOptionsVO;
import com.ruoyi.education.course.dto.CourseQuery;
import com.ruoyi.education.course.dto.PrereqCheckVO;

/**
 * 课程目录与先修关系 服务层
 *
 * @author ruoyi
 */
public interface IEduCourseService
{
    /**
     * 查询课程列表
     *
     * @param query 查询条件
     * @return 课程集合
     */
    public List<EduCourse> selectCourseList(CourseQuery query);

    /**
     * 根据课程ID查询课程
     *
     * @param courseId 课程ID
     * @return 课程信息
     */
    public EduCourse selectCourseById(Long courseId);

    /**
     * 根据课程代码查询课程
     *
     * @param code 课程代码
     * @return 课程信息
     */
    public EduCourse selectCourseByCode(String code);

    /**
     * 根据课程代码批量查询课程
     *
     * @param codes 课程代码集合
     * @return 课程集合
     */
    public List<EduCourse> selectCourseByCodes(List<String> codes);

    /**
     * 新增课程
     *
     * @param course 课程信息
     * @return 结果
     */
    public int insertCourse(EduCourse course);

    /**
     * 修改课程
     *
     * @param course 课程信息
     * @return 结果
     */
    public int updateCourse(EduCourse course);

    /**
     * 批量删除课程
     *
     * @param courseIds 需要删除的课程ID
     * @return 结果
     */
    public int deleteCourseByIds(Long[] courseIds);

    /**
     * 查询课程筛选选项
     *
     * @return 学院 / 类别 / 层级选项
     */
    public CourseOptionsVO selectCourseOptions();

    /**
     * 查询某门课程的先修关系
     *
     * @param courseId 课程ID
     * @return 先修关系集合（当前登录学生已绑定时会带上 satisfied 标记）
     */
    public List<EduCoursePrereq> selectPrereqByCourseId(Long courseId);

    /**
     * 覆盖式更新某门课程的先修关系（写入前做自依赖与环检测）
     *
     * @param courseId 课程ID
     * @param prereqs 先修关系（仅取 prereqCourseId / groupNo / rawText）
     */
    public void replacePrereqs(Long courseId, List<EduCoursePrereq> prereqs);

    /**
     * 以当前登录学生的已通过课程判断先修条件是否满足
     *
     * @param courseId 课程ID
     * @return 校验结果
     */
    public PrereqCheckVO checkPrerequisite(Long courseId);
}
