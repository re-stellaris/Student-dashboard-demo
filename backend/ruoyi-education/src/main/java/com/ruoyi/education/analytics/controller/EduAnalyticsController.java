package com.ruoyi.education.analytics.controller;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.education.analytics.domain.EduCourseCompetency;
import com.ruoyi.education.analytics.domain.EduSkillTag;
import com.ruoyi.education.analytics.service.IEduAnalyticsService;

/**
 * 学情与技能画像 信息操作处理
 *
 * @author ruoyi-education
 */
@RestController
@RequestMapping("/edu/analytics")
public class EduAnalyticsController extends BaseController
{
    @Autowired
    private IEduAnalyticsService analyticsService;

    /**
     * 学习仪表盘聚合
     */
    @PreAuthorize("@ss.hasPermi('edu:dashboard:view')")
    @GetMapping("/dashboard")
    public AjaxResult dashboard()
    {
        return success(analyticsService.dashboard());
    }

    /**
     * 当前学生技能画像
     */
    @PreAuthorize("@ss.hasPermi('edu:skill:view')")
    @GetMapping("/skill-profile")
    public AjaxResult skillProfile()
    {
        return success(analyticsService.selectMySkillProfile());
    }

    /**
     * 学院能力池
     */
    @PreAuthorize("@ss.hasPermi('edu:analytics:view')")
    @GetMapping("/competency-pool")
    public TableDataInfo competencyPool(String schoolAbbr, String keyword)
    {
        startPage();
        List<EduSkillTag> list = analyticsService.selectCompetencyPool(schoolAbbr, keyword);
        return getDataTable(list);
    }

    /**
     * 单门课程的技能关联明细
     */
    @GetMapping("/course-competency")
    public AjaxResult courseCompetency(@RequestParam("courseId") Long courseId)
    {
        return success(analyticsService.selectCourseCompetency(courseId));
    }
}
