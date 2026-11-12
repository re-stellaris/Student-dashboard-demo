package com.ruoyi.education.course.mapper;

import java.util.Collection;
import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.education.course.domain.EduCourse;
import com.ruoyi.education.course.domain.EduCourseOption;
import com.ruoyi.education.course.dto.CourseQuery;

/**
 * 课程目录 数据层
 *
 * @author ruoyi
 */
public interface EduCourseMapper
{
    /**
     * 查询课程列表
     *
     * @param query 查询条件（code/title/school/category/level/hasOutline）
     * @return 课程集合
     */
    public List<EduCourse> selectCourseList(CourseQuery query);

    /**
     * 根据课程ID查询课程
     *
     * @param courseId 课程ID
     * @return 课程信息
     */
    public EduCourse selectCourseById(@Param("courseId") Long courseId);

    /**
     * 根据课程代码查询课程
     *
     * @param code 课程代码
     * @return 课程信息
     */
    public EduCourse selectCourseByCode(@Param("code") String code);

    /**
     * 根据课程代码批量查询课程
     *
     * @param codes 课程代码集合
     * @return 课程集合
     */
    public List<EduCourse> selectCourseByCodes(@Param("codes") List<String> codes);

    /**
     * 根据课程ID批量查询课程（供规划 / 学情等跨域只读端口使用）
     *
     * @param courseIds 课程ID集合；{@code null} 或空集合表示查询全部课程
     * @return 课程集合
     */
    public List<EduCourse> selectCourseByIds(@Param("courseIds") Collection<Long> courseIds);

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
    public int deleteCourseByIds(@Param("courseIds") Long[] courseIds);

    /**
     * 查询筛选选项（学院 / 类别 / 层级去重组合）
     *
     * @return 选项集合
     */
    public List<EduCourseOption> selectCourseOptions();

    /**
     * 校验课程代码是否已存在
     *
     * @param code 课程代码
     * @return 已存在的数量
     */
    public int countCourseByCode(@Param("code") String code);

    /**
     * 按系统用户ID查询学生ID（edu_student.sys_user_id 反查，绝不采信前端传入的 studentId）
     *
     * @param sysUserId 系统用户ID
     * @return 学生ID，未绑定学籍时返回 null
     */
    public Long selectStudentIdBySysUserId(@Param("sysUserId") Long sysUserId);

    /**
     * 查询指定学生已通过课程的课程ID（仅取已入库课程的行）
     *
     * @param studentId 学生ID
     * @return 课程ID集合
     */
    public List<Long> selectPassedCourseIdsByStudentId(@Param("studentId") Long studentId);

    /**
     * 查询指定学生已通过课程的课程代码快照（课程目录未命中时按代码兜底匹配）
     *
     * @param studentId 学生ID
     * @return 课程代码集合
     */
    public List<String> selectPassedCourseCodesByStudentId(@Param("studentId") Long studentId);
}
