package com.ruoyi.education.transcript.dto;

import java.util.List;

/**
 * 成绩单导入请求体（docs/architecture/api-contract.md §10.1）
 *
 * <p>示例：</p>
 * <pre>
 * {
 *   "fileName": "transcript.xlsx",
 *   "rows": [ { "courseCode": "CSC3100", "termCode": "2023-24-1", "units": 3.0,
 *               "grade": "A-", "score": 45.7, "category": "Major",
 *               "passed": true, "status": "Completed" } ]
 * }
 * </pre>
 *
 * <p>请求体不包含 {@code studentId}：身份只从 JWT 解析（§10.3 规则 1）。</p>
 *
 * @author ruoyi-education
 */
public class TranscriptImportBody
{
    /** 原始文件名，仅用于展示 / 排查，不参与业务判定 */
    private String fileName;

    /** 成绩行；为空或超过 500 行由 Service 直接拒绝 */
    private List<TranscriptRow> rows;

    public String getFileName()
    {
        return fileName;
    }

    public void setFileName(String fileName)
    {
        this.fileName = fileName;
    }

    public List<TranscriptRow> getRows()
    {
        return rows;
    }

    public void setRows(List<TranscriptRow> rows)
    {
        this.rows = rows;
    }
}
