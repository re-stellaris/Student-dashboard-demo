package com.ruoyi.education.transcript.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.education.transcript.dto.TranscriptImportBody;
import com.ruoyi.education.transcript.dto.TranscriptImportResultVO;
import com.ruoyi.education.transcript.service.IEduTranscriptService;

/**
 * 成绩单上传（学生自填）信息操作处理
 *
 * <p>对应 docs/architecture/api-contract.md §10：页面 {@code /student/transcript} 由前端解析
 * xlsx/csv 后把行数据提交到这里，后端只负责校验、落库与学分/GPA 回算。</p>
 *
 * @author ruoyi-education
 */
@RestController
@RequestMapping("/edu/transcripts")
public class EduTranscriptController extends BaseController
{
    @Autowired
    private IEduTranscriptService transcriptService;

    /**
     * 导入成绩单行
     *
     * @param body 请求体（fileName + rows）
     * @return 导入结果
     */
    @PreAuthorize("@ss.hasPermi('edu:transcript:upload')")
    @Log(title = "成绩单导入", businessType = BusinessType.INSERT)
    @PostMapping("/import")
    public AjaxResult importTranscripts(@Validated @RequestBody TranscriptImportBody body)
    {
        TranscriptImportResultVO result = transcriptService.importTranscripts(body);
        return success(result);
    }
}
