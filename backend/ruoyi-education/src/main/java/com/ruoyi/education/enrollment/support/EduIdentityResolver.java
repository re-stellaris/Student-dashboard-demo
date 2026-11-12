package com.ruoyi.education.enrollment.support;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.education.enrollment.domain.EduStudent;
import com.ruoyi.education.enrollment.domain.EduTeacher;
import com.ruoyi.education.enrollment.mapper.EduStudentMapper;
import com.ruoyi.education.enrollment.mapper.EduTeacherMapper;

/**
 * 当前登录身份解析器
 *
 * <p>教育模块所有 “本人” 接口的唯一身份来源：先由 SecurityUtils.getUserId() 取系统用户，
 * 再反查 edu_student.sys_user_id / edu_teacher.sys_user_id，绝不采信请求参数中的
 * studentId / teacherId。</p>
 *
 * @author ruoyi-education
 */
@Component
public class EduIdentityResolver
{
    /** 越权访问统一提示（不暴露数据是否存在） */
    public static final String FORBIDDEN_MESSAGE = "无权访问该资源";

    @Autowired
    private EduStudentMapper studentMapper;

    @Autowired
    private EduTeacherMapper teacherMapper;

    /**
     * 当前登录用户对应的学生学籍（不存在时返回 null，用于角色自适应分支）
     *
     * @return 学生，可能为 null
     */
    public EduStudent findStudent()
    {
        return studentMapper.selectStudentBySysUserId(SecurityUtils.getUserId());
    }

    /**
     * 当前登录用户对应的教师档案（不存在时返回 null，用于角色自适应分支）
     *
     * @return 教师，可能为 null
     */
    public EduTeacher findTeacher()
    {
        return teacherMapper.selectTeacherBySysUserId(SecurityUtils.getUserId());
    }

    /**
     * 当前登录用户对应的学生学籍
     *
     * @return 学生
     */
    public EduStudent currentStudent()
    {
        EduStudent student = findStudent();
        if (student == null)
        {
            throw new ServiceException("当前账号未绑定学生信息");
        }
        return student;
    }

    /**
     * 当前登录用户对应的教师档案
     *
     * @return 教师
     */
    public EduTeacher currentTeacher()
    {
        EduTeacher teacher = findTeacher();
        if (teacher == null)
        {
            throw new ServiceException("当前账号未绑定教师信息");
        }
        return teacher;
    }

    /**
     * 当前登录用户是否超级管理员
     *
     * @return true 表示管理员
     */
    public boolean isAdmin()
    {
        return SecurityUtils.isAdmin(SecurityUtils.getUserId());
    }

    /**
     * 当前登录用户名（写入 createBy / updateBy）
     *
     * @return 用户名
     */
    public String currentUsername()
    {
        return SecurityUtils.getUsername();
    }

    /**
     * 构造越权异常
     *
     * @return 业务异常
     */
    public ServiceException forbidden()
    {
        return new ServiceException(FORBIDDEN_MESSAGE);
    }
}
