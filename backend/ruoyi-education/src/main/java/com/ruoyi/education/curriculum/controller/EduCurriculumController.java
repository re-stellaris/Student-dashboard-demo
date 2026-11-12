package com.ruoyi.education.curriculum.controller;

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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.education.curriculum.domain.EduCurriculum;
import com.ruoyi.education.curriculum.domain.EduCurriculumItem;
import com.ruoyi.education.curriculum.dto.CurriculumQuery;
import com.ruoyi.education.curriculum.service.IEduCurriculumService;

/**
 * 培养方案接口
 *
 * <p>路径与权限标识严格对齐 {@code docs/architecture/api-contract.md} 第 2 节。</p>
 *
 * @author ruoyi
 */
@RestController
@RequestMapping("/edu/curricula")
public class EduCurriculumController extends BaseController
{
    @Autowired
    private IEduCurriculumService curriculumService;

    /**
     * 培养方案列表，参数 majorId、versionYear
     */
    @PreAuthorize("@ss.hasPermi('edu:curriculum:list')")
    @GetMapping("/list")
    public TableDataInfo list(CurriculumQuery query)
    {
        startPage();
        List<EduCurriculum> list = curriculumService.selectCurriculumList(query);
        return getDataTable(list);
    }

    /**
     * 按专业 + 入学年份解析适用版本
     *
     * <p>无匹配版本时 data 为 null。</p>
     */
    @GetMapping("/by-major")
    public AjaxResult byMajor(@RequestParam(value = "majorId", required = false) Long majorId,
        @RequestParam(value = "entryYear", required = false) Integer entryYear)
    {
        AjaxResult result = AjaxResult.success();
        result.put(AjaxResult.DATA_TAG, curriculumService.resolveByMajor(majorId, entryYear));
        return result;
    }

    /**
     * 方案详情（含 items）
     */
    @PreAuthorize("@ss.hasPermi('edu:curriculum:list')")
    @GetMapping("/{curriculumId}")
    public AjaxResult getInfo(@PathVariable("curriculumId") Long curriculumId)
    {
        return success(curriculumService.selectCurriculumById(curriculumId));
    }

    /**
     * 新增培养方案
     */
    @PreAuthorize("@ss.hasPermi('edu:curriculum:add')")
    @Log(title = "培养方案", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody EduCurriculum curriculum)
    {
        curriculum.setCreateBy(getUsername());
        return toAjax(curriculumService.insertCurriculum(curriculum));
    }

    /**
     * 修改培养方案
     */
    @PreAuthorize("@ss.hasPermi('edu:curriculum:edit')")
    @Log(title = "培养方案", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody EduCurriculum curriculum)
    {
        curriculum.setUpdateBy(getUsername());
        return toAjax(curriculumService.updateCurriculum(curriculum));
    }

    /**
     * 批量删除培养方案
     */
    @PreAuthorize("@ss.hasPermi('edu:curriculum:remove')")
    @Log(title = "培养方案", businessType = BusinessType.DELETE)
    @DeleteMapping("/{curriculumIds}")
    public AjaxResult remove(@PathVariable Long[] curriculumIds)
    {
        return toAjax(curriculumService.deleteCurriculumByIds(curriculumIds));
    }

    /**
     * 覆盖式更新方案条目
     *
     * <p>body：[{"courseId":..,"courseCode":"..","requirementType":"REQUIRED","section":"..",
     * "stream":"","minUnits":3.0,"sortOrder":1}]</p>
     */
    @PreAuthorize("@ss.hasPermi('edu:curriculum:item:edit')")
    @Log(title = "培养方案条目", businessType = BusinessType.UPDATE)
    @PutMapping("/{curriculumId}/items")
    public AjaxResult editItems(@PathVariable("curriculumId") Long curriculumId,
        @RequestBody List<EduCurriculumItem> items)
    {
        if (items != null)
        {
            for (EduCurriculumItem item : items)
            {
                if (item != null)
                {
                    item.setCreateBy(getUsername());
                }
            }
        }
        curriculumService.replaceItems(curriculumId, items);
        return success();
    }
}
