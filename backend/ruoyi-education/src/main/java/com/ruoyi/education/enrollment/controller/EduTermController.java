package com.ruoyi.education.enrollment.controller;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.education.enrollment.domain.EduTerm;
import com.ruoyi.education.enrollment.service.IEduEnrollmentService;

/**
 * 学期 信息操作处理
 *
 * @author ruoyi-education
 */
@RestController
@RequestMapping("/edu/terms")
public class EduTermController extends BaseController
{
    @Autowired
    private IEduEnrollmentService enrollmentService;

    /**
     * 查询学期列表
     */
    @GetMapping("/list")
    public TableDataInfo list(EduTerm term)
    {
        startPage();
        List<EduTerm> list = enrollmentService.selectTermList(term);
        return getDataTable(list);
    }

    /**
     * 查询当前学期
     */
    @GetMapping("/current")
    public AjaxResult current()
    {
        return success(enrollmentService.selectCurrentTerm());
    }
}
