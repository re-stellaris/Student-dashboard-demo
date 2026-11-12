package com.ruoyi.education.enrollment.controller;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.education.enrollment.domain.EduCourseRecord;
import com.ruoyi.education.enrollment.service.IEduEnrollmentService;

/**
 * 学生本人学籍与成绩 信息操作处理
 *
 * <p>身份一律由 SecurityUtils.getUserId() 反查 edu_student，不接受 studentId 参数。</p>
 *
 * @author ruoyi-education
 */
@RestController
@RequestMapping("/edu/students")
public class EduStudentController extends BaseController
{
    @Autowired
    private IEduEnrollmentService enrollmentService;

    /**
     * 当前学生学籍信息
     */
    @GetMapping("/me")
    public AjaxResult me()
    {
        return success(enrollmentService.selectMyStudent());
    }

    /**
     * 当前学生学分进度
     */
    @GetMapping("/me/progress")
    public AjaxResult progress()
    {
        return success(enrollmentService.selectMyProgress());
    }

    /**
     * 当前学生成绩单
     */
    @PreAuthorize("@ss.hasPermi('edu:record:list')")
    @GetMapping("/me/records")
    public TableDataInfo records(EduCourseRecord query)
    {
        startPage();
        List<EduCourseRecord> list = enrollmentService.selectMyRecords(query);
        return getDataTable(list);
    }
}
