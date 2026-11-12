package com.ruoyi.education.enrollment.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.education.enrollment.dto.EnrollmentBody;
import com.ruoyi.education.enrollment.service.IEduEnrollmentService;

/**
 * 选课 信息操作处理
 *
 * @author ruoyi-education
 */
@RestController
@RequestMapping("/edu/enrollments")
public class EduEnrollmentController extends BaseController
{
    @Autowired
    private IEduEnrollmentService enrollmentService;

    /**
     * 学生选课（校验容量、重复选课、先修条件）
     */
    @Log(title = "选课", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody EnrollmentBody body)
    {
        return success(enrollmentService.addEnrollment(body.getOfferingId()));
    }

    /**
     * 退课
     */
    @Log(title = "选课", businessType = BusinessType.DELETE)
    @DeleteMapping("/{enrollmentId}")
    public AjaxResult remove(@PathVariable Long enrollmentId)
    {
        return toAjax(enrollmentService.dropEnrollment(enrollmentId));
    }
}
