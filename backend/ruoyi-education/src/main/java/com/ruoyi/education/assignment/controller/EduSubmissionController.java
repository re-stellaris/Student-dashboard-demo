package com.ruoyi.education.assignment.controller;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
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
import com.ruoyi.education.assignment.domain.EduSubmission;
import com.ruoyi.education.assignment.dto.GradeBody;
import com.ruoyi.education.assignment.dto.SubmissionBody;
import com.ruoyi.education.assignment.service.IEduAssignmentService;

/**
 * 作业提交 信息操作处理
 *
 * @author ruoyi-education
 */
@RestController
@RequestMapping("/edu/submissions")
public class EduSubmissionController extends BaseController
{
    @Autowired
    private IEduAssignmentService assignmentService;

    /**
     * 查询提交列表（教师看本人课程，学生仅看本人）
     */
    @GetMapping("/list")
    public TableDataInfo list(EduSubmission submission)
    {
        startPage();
        List<EduSubmission> list = assignmentService.selectSubmissionList(submission);
        return getDataTable(list);
    }

    /**
     * 学生提交作业
     */
    @Log(title = "作业提交", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody SubmissionBody body)
    {
        return success(assignmentService.submitAssignment(body));
    }

    /**
     * 教师评分
     */
    @PreAuthorize("@ss.hasPermi('edu:submission:grade')")
    @Log(title = "作业提交", businessType = BusinessType.UPDATE)
    @PutMapping("/grade")
    public AjaxResult grade(@Validated @RequestBody GradeBody body)
    {
        return toAjax(assignmentService.gradeSubmission(body));
    }
}
