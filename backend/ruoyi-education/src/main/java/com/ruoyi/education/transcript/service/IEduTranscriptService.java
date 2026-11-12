package com.ruoyi.education.transcript.service;

import com.ruoyi.education.transcript.dto.TranscriptImportBody;
import com.ruoyi.education.transcript.dto.TranscriptImportResultVO;

/**
 * 成绩单导入 Service
 *
 * @author ruoyi-education
 */
public interface IEduTranscriptService
{
    /**
     * 导入当前登录学生自填的成绩单（身份只取 JWT）
     *
     * @param body 请求体（fileName + rows，不接受 studentId）
     * @return 导入结果（插入 / 更新 / 跳过 / 未知课程 / 未知学期）
     */
    TranscriptImportResultVO importTranscripts(TranscriptImportBody body);
}
