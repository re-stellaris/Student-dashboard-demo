package com.ruoyi.education.enrollment.mapper;

import java.util.List;
import com.ruoyi.education.enrollment.domain.EduTeacher;

/**
 * 教师 Mapper 接口
 *
 * @author ruoyi-education
 */
public interface EduTeacherMapper
{
    /**
     * 按系统用户ID查询教师（教师侧“本人”接口的唯一身份入口）
     *
     * @param sysUserId 系统用户ID
     * @return 教师
     */
    EduTeacher selectTeacherBySysUserId(Long sysUserId);

    /**
     * 按主键查询教师
     *
     * @param teacherId 教师ID
     * @return 教师
     */
    EduTeacher selectTeacherById(Long teacherId);

    /**
     * 按教工号查询教师
     *
     * @param teacherNo 教工号
     * @return 教师
     */
    EduTeacher selectTeacherByNo(String teacherNo);

    /**
     * 查询教师列表
     *
     * @param teacher 查询条件（教工号、姓名、学院）
     * @return 教师集合
     */
    List<EduTeacher> selectTeacherList(EduTeacher teacher);

    /**
     * 新增教师
     *
     * @param teacher 教师
     * @return 影响行数
     */
    int insertTeacher(EduTeacher teacher);

    /**
     * 修改教师
     *
     * @param teacher 教师
     * @return 影响行数
     */
    int updateTeacher(EduTeacher teacher);
}
