package com.ruoyi.education.curriculum.controller;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.education.curriculum.domain.EduMajor;
import com.ruoyi.education.curriculum.service.IEduCurriculumService;

/**
 * 专业接口
 *
 * <p>路径与权限标识严格对齐 {@code docs/architecture/api-contract.md} 第 2 节。</p>
 *
 * @author ruoyi
 */
@RestController
@RequestMapping("/edu/majors")
public class EduMajorController extends BaseController
{
    @Autowired
    private IEduCurriculumService curriculumService;

    /**
     * 专业列表
     */
    @GetMapping("/list")
    public TableDataInfo list(EduMajor major)
    {
        startPage();
        List<EduMajor> list = curriculumService.selectMajorList(major);
        return getDataTable(list);
    }
}
