package com.ruoyi.education.assignment.controller;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
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
import com.ruoyi.education.assignment.domain.EduAssignment;
import com.ruoyi.education.assignment.service.IEduAssignmentService;

/**
 * 作业 信息操作处理
 *
 * @author ruoyi-education
 */
@RestController
@RequestMapping("/edu/assignments")
public class EduAssignmentController extends BaseController
{
    @Autowired
    private IEduAssignmentService assignmentService;

    /**
     * 查询作业列表（教师看本人授课，学生看已选课程）
     */
    @GetMapping("/list")
    public TableDataInfo list(EduAssignment assignment)
    {
        startPage();
        List<EduAssignment> list = assignmentService.selectAssignmentList(assignment);
        return getDataTable(list);
    }

    /**
     * 查询作业详情
     */
    @GetMapping("/{assignmentId}")
    public AjaxResult getInfo(@PathVariable Long assignmentId)
    {
        return success(assignmentService.selectAssignmentById(assignmentId));
    }

    /**
     * 教师发布作业
     */
    @PreAuthorize("@ss.hasPermi('edu:assignment:add')")
    @Log(title = "作业", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody EduAssignment assignment)
    {
        assignment.setCreateBy(getUsername());
        return toAjax(assignmentService.insertAssignment(assignment));
    }

    /**
     * 修改作业
     */
    @PreAuthorize("@ss.hasPermi('edu:assignment:edit')")
    @Log(title = "作业", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody EduAssignment assignment)
    {
        assignment.setUpdateBy(getUsername());
        return toAjax(assignmentService.updateAssignment(assignment));
    }

    /**
     * 删除作业
     */
    @PreAuthorize("@ss.hasPermi('edu:assignment:edit')")
    @Log(title = "作业", businessType = BusinessType.DELETE)
    @DeleteMapping("/{assignmentIds}")
    public AjaxResult remove(@PathVariable Long[] assignmentIds)
    {
        return toAjax(assignmentService.deleteAssignmentByIds(assignmentIds));
    }
}
