package com.ruoyi.education.advisor.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.PageDomain;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.core.page.TableSupport;
import com.ruoyi.education.advisor.dto.AdvisorStudentPageVO;
import com.ruoyi.education.advisor.dto.AdvisorStudentQuery;
import com.ruoyi.education.advisor.service.IEduAdvisorService;

/**
 * 导师看板 信息操作处理
 * =====================================================================
 * <p>三个只读接口，权限统一为 {@code edu:advisor:view}（菜单 + V7 迁移已就绪）：</p>
 * <ul>
 *   <li>{@code GET /edu/advisors/overview} —— 总览聚合</li>
 *   <li>{@code GET /edu/advisors/students} —— 学生列表（分页，TableDataInfo）</li>
 *   <li>{@code GET /edu/advisors/students/{studentId}} —— 单个学生画像</li>
 * </ul>
 *
 * <p><b>数据边界（已知限制）</b>：当前库没有 advisor-student（导师-学生）关系表，
 * 因此 TEACHER / ADMIN 可以看到<b>全部</b>学生；接口不接受前端传入的
 * “我是谁”（teacherId / advisorId / 当前用户身份），身份只在服务端由
 * {@code @PreAuthorize} 的权限校验使用。补齐关系表后，把过滤条件下推到
 * {@code EduAdvisorMapper} 即可，接口契约不变。</p>
 *
 * @author ruoyi-education
 */
@RestController
@RequestMapping("/edu/advisors")
public class EduAdvisorController extends BaseController
{
    @Autowired
    private IEduAdvisorService advisorService;

    /**
     * 导师看板总览（学生数、平均 GPA/学分、GPA 分布与趋势、风险与进度分布）
     */
    @PreAuthorize("@ss.hasPermi('edu:advisor:view')")
    @GetMapping("/overview")
    public AjaxResult overview()
    {
        return success(advisorService.overview());
    }

    /**
     * 学生列表（分页）
     *
     * <p>参数：entryYear、majorId、riskLevel、keyword；风险等级由服务层实时计算后再过滤，
     * 因此分页在服务层完成，total 与过滤后的结果一致。</p>
     */
    @PreAuthorize("@ss.hasPermi('edu:advisor:view')")
    @GetMapping("/students")
    public TableDataInfo list(AdvisorStudentQuery query)
    {
        PageDomain page = TableSupport.buildPageRequest();
        AdvisorStudentPageVO result = advisorService.selectStudentPage(query, page.getPageNum(),
                page.getPageSize());
        TableDataInfo data = getDataTable(result.getRows());
        data.setTotal(result.getTotal());
        return data;
    }

    /**
     * 单个学生画像（基本信息 + 分类学分 + 学期 GPA 趋势 + 已修课程数 + 风险明细）
     */
    @PreAuthorize("@ss.hasPermi('edu:advisor:view')")
    @GetMapping("/students/{studentId}")
    public AjaxResult detail(@PathVariable("studentId") Long studentId)
    {
        return success(advisorService.selectStudentDetail(studentId));
    }
}
