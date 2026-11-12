package com.ruoyi.education.enrollment.service;

import java.util.List;
import com.ruoyi.education.enrollment.domain.EduCourseOffering;
import com.ruoyi.education.enrollment.domain.EduCourseRecord;
import com.ruoyi.education.enrollment.domain.EduStudent;
import com.ruoyi.education.enrollment.domain.EduTerm;
import com.ruoyi.education.enrollment.dto.CreditProgressVO;

/**
 * 学期 / 开课 / 选课 / 成绩 业务接口
 *
 * <p>所有 “本人” 语义的方法只以当前登录用户（SecurityUtils.getUserId）解析身份，
 * 不接受前端传入的 studentId / teacherId。</p>
 *
 * @author ruoyi-education
 */
public interface IEduEnrollmentService
{
    /**
     * 查询学期列表
     *
     * @param term 查询条件
     * @return 学期集合
     */
    List<EduTerm> selectTermList(EduTerm term);

    /**
     * 查询当前学期
     *
     * @return 当前学期
     */
    EduTerm selectCurrentTerm();

    /**
     * 查询开课列表
     *
     * @param offering 查询条件（termId / courseId / teacherId）
     * @return 开课集合
     */
    List<EduCourseOffering> selectOfferingList(EduCourseOffering offering);

    /**
     * 查询开课详情
     *
     * @param offeringId 开课ID
     * @return 开课
     */
    EduCourseOffering selectOfferingById(Long offeringId);

    /**
     * 查询当前教师所授课程（含选课人数、作业数量）
     *
     * @return 开课集合
     */
    List<EduCourseOffering> selectMyOfferings();

    /**
     * 新增开课（非管理员只能以本人为授课教师）
     *
     * @param offering 开课
     * @return 影响行数
     */
    int insertOffering(EduCourseOffering offering);

    /**
     * 修改开课（非管理员必须为本人授课的开课）
     *
     * @param offering 开课
     * @return 影响行数
     */
    int updateOffering(EduCourseOffering offering);

    /**
     * 查询当前登录学生的学籍信息
     *
     * @return 学生
     */
    EduStudent selectMyStudent();

    /**
     * 当前学生学分进度（对照适用培养方案）
     *
     * @return 学分进度
     */
    CreditProgressVO selectMyProgress();

    /**
     * 当前学生成绩单
     *
     * @param query 查询条件（termCode / category），studentId 由服务端覆盖
     * @return 成绩集合
     */
    List<EduCourseRecord> selectMyRecords(EduCourseRecord query);

    /**
     * 学生选课：校验容量、重复选课与先修条件
     *
     * @param offeringId 开课ID
     * @return 选课ID
     */
    Long addEnrollment(Long offeringId);

    /**
     * 学生退课
     *
     * @param enrollmentId 选课ID
     * @return 影响行数
     */
    int dropEnrollment(Long enrollmentId);

    /**
     * 管理员/教师修正成绩
     *
     * @param record 成绩记录
     * @return 影响行数
     */
    int updateRecord(EduCourseRecord record);
}
