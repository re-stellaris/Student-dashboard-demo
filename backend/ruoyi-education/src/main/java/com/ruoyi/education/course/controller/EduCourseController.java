package com.ruoyi.education.course.controller;

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
import com.ruoyi.education.course.domain.EduCourse;
import com.ruoyi.education.course.domain.EduCoursePrereq;
import com.ruoyi.education.course.dto.CourseQuery;
import com.ruoyi.education.course.service.IEduCourseService;

/**
 * 课程中心（课程目录 + 先修关系）接口
 *
 * <p>路径与权限标识严格对齐 {@code docs/architecture/api-contract.md} 第 1 节。</p>
 *
 * @author ruoyi
 */
@RestController
@RequestMapping("/edu/courses")
public class EduCourseController extends BaseController
{
    @Autowired
    private IEduCourseService courseService;

    /**
     * 分页查询课程
     *
     * <p>参数：code、title、school、category、level、hasOutline</p>
     */
    @GetMapping("/list")
    public TableDataInfo list(CourseQuery query)
    {
        startPage();
        List<EduCourse> list = courseService.selectCourseList(query);
        return getDataTable(list);
    }

    /**
     * 课程筛选选项：schools、categories、levels
     */
    @GetMapping("/options")
    public AjaxResult options()
    {
        return success(courseService.selectCourseOptions());
    }

    /**
     * 按课程代码查询课程（转录 / 规划补全用）
     */
    @GetMapping("/byCode/{code}")
    public AjaxResult getByCode(@PathVariable("code") String code)
    {
        return success(courseService.selectCourseByCode(code));
    }

    /**
     * 新增课程
     */
    @PreAuthorize("@ss.hasPermi('edu:course:add')")
    @Log(title = "课程管理", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody EduCourse course)
    {
        course.setCreateBy(getUsername());
        return toAjax(courseService.insertCourse(course));
    }

    /**
     * 修改课程
     */
    @PreAuthorize("@ss.hasPermi('edu:course:edit')")
    @Log(title = "课程管理", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody EduCourse course)
    {
        course.setUpdateBy(getUsername());
        return toAjax(courseService.updateCourse(course));
    }

    /**
     * 课程详情
     */
    @GetMapping("/{courseId}")
    public AjaxResult getInfo(@PathVariable("courseId") Long courseId)
    {
        return success(courseService.selectCourseById(courseId));
    }

    /**
     * 批量删除课程（逗号分隔 ID）
     */
    @PreAuthorize("@ss.hasPermi('edu:course:remove')")
    @Log(title = "课程管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{courseIds}")
    public AjaxResult remove(@PathVariable Long[] courseIds)
    {
        return toAjax(courseService.deleteCourseByIds(courseIds));
    }

    /**
     * 先修关系（含 groupNo、rawText、先修课程代码 / 名称）
     */
    @GetMapping("/{courseId}/prereqs")
    public AjaxResult prereqs(@PathVariable("courseId") Long courseId)
    {
        return success(courseService.selectPrereqByCourseId(courseId));
    }

    /**
     * 覆盖式更新先修关系，body：[{"prereqCourseId":100123,"groupNo":1}]
     *
     * <p>写入前做自依赖与环检测。</p>
     */
    @PreAuthorize("@ss.hasPermi('edu:prereq:edit')")
    @Log(title = "课程先修", businessType = BusinessType.UPDATE)
    @PutMapping("/{courseId}/prereqs")
    public AjaxResult editPrereqs(@PathVariable("courseId") Long courseId,
        @RequestBody List<EduCoursePrereq> prereqs)
    {
        if (prereqs != null)
        {
            for (EduCoursePrereq prereq : prereqs)
            {
                if (prereq != null)
                {
                    prereq.setCreateBy(getUsername());
                }
            }
        }
        courseService.replacePrereqs(courseId, prereqs);
        return success();
    }

    /**
     * 以当前学生已修课程判断先修是否满足
     *
     * <p>学生身份由后端按 {@code SecurityUtils.getUserId()} 反查 edu_student 得到，
     * 不接受前端传入的 studentId。</p>
     */
    @GetMapping("/{courseId}/prereq-check")
    public AjaxResult prereqCheck(@PathVariable("courseId") Long courseId)
    {
        return success(courseService.checkPrerequisite(courseId));
    }
}
