package com.ruoyi.education.enrollment.controller;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.education.enrollment.domain.EduCourseOffering;
import com.ruoyi.education.enrollment.service.IEduEnrollmentService;

/**
 * 学期开课 信息操作处理
 *
 * @author ruoyi-education
 */
@RestController
@RequestMapping("/edu/offerings")
public class EduOfferingController extends BaseController
{
    @Autowired
    private IEduEnrollmentService enrollmentService;

    /**
     * 查询开课列表
     */
    @GetMapping("/list")
    public TableDataInfo list(EduCourseOffering offering)
    {
        startPage();
        List<EduCourseOffering> list = enrollmentService.selectOfferingList(offering);
        return getDataTable(list);
    }

    /**
     * 当前教师所授课程（含选课人数、作业数量）
     */
    @PreAuthorize("@ss.hasPermi('edu:offering:list')")
    @GetMapping("/mine")
    public TableDataInfo mine()
    {
        startPage();
        List<EduCourseOffering> list = enrollmentService.selectMyOfferings();
        return getDataTable(list);
    }

    /**
     * 查询开课详情
     */
    @GetMapping("/{offeringId}")
    public AjaxResult getInfo(@PathVariable Long offeringId)
    {
        return success(enrollmentService.selectOfferingById(offeringId));
    }

    /**
     * 新增开课
     */
    @PreAuthorize("@ss.hasPermi('edu:offering:edit')")
    @Log(title = "学期开课", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody EduCourseOffering offering)
    {
        offering.setCreateBy(getUsername());
        return toAjax(enrollmentService.insertOffering(offering));
    }

    /**
     * 修改开课
     */
    @PreAuthorize("@ss.hasPermi('edu:offering:edit')")
    @Log(title = "学期开课", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody EduCourseOffering offering)
    {
        offering.setUpdateBy(getUsername());
        return toAjax(enrollmentService.updateOffering(offering));
    }
}
