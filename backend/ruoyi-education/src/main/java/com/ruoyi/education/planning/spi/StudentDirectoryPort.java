package com.ruoyi.education.planning.spi;

/**
 * 学生身份只读端口：把登录态的系统用户映射到 edu_student 学籍。
 *
 * <p>规划域<b>只</b>通过本端口解析当前学生，绝不采信前端传入的 studentId
 * （docs/architecture/backend-conventions.md 第 4 节）。</p>
 *
 * <p>默认实现见 {@code DefaultPlanningReadPorts}；所属域（enrollment/student）
 * 也可提供更权威的实现替换它。</p>
 *
 * @author ruoyi
 */
public interface StudentDirectoryPort
{
    /**
     * 按系统用户ID反查学生ID。
     *
     * @param sysUserId 若依 sys_user.user_id
     * @return 学生ID；未绑定学籍时返回 {@code null}
     */
    Long findStudentIdBySysUserId(Long sysUserId);

    /**
     * 查询学生学籍快照（用于解析培养方案版本）。
     *
     * @param studentId 学生ID
     * @return 学籍快照；不存在时返回 {@code null}
     */
    StudentProfile findProfile(Long studentId);
}
