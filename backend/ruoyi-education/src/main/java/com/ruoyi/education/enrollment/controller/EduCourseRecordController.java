package com.ruoyi.education.enrollment.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.education.enrollment.domain.EduCourseRecord;
import com.ruoyi.education.enrollment.service.IEduEnrollmentService;

/**
 * 成绩记录 信息操作处理
 *
 * @author ruoyi-education
 */
@RestController
@RequestMapping("/edu/records")
public class EduCourseRecordController extends BaseController
{
    @Autowired
    private IEduEnrollmentService enrollmentService;

    /**
     * 管理员/教师修正成绩
     */
    @PreAuthorize("@ss.hasPermi('edu:record:edit')")
    @Log(title = "成绩记录", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody EduCourseRecord record)
    {
        record.setUpdateBy(getUsername());
        return toAjax(enrollmentService.updateRecord(record));
    }
}
