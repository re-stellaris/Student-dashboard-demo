package com.ruoyi.education.transcript.dto;

/**
 * 被跳过的成绩行及原因（docs/architecture/api-contract.md §10.2 skippedDetails）
 *
 * @author ruoyi-education
 */
public class SkippedRow
{
    /** 课程代码（为空时保持空串） */
    private String courseCode;

    /** 学期代码（为空时保持空串） */
    private String termCode;

    /** 中文原因 */
    private String reason;

    public SkippedRow()
    {
    }

    public SkippedRow(String courseCode, String termCode, String reason)
    {
        this.courseCode = courseCode;
        this.termCode = termCode;
        this.reason = reason;
    }

    public String getCourseCode()
    {
        return courseCode;
    }

    public void setCourseCode(String courseCode)
    {
        this.courseCode = courseCode;
    }

    public String getTermCode()
    {
        return termCode;
    }

    public void setTermCode(String termCode)
    {
        this.termCode = termCode;
    }

    public String getReason()
    {
        return reason;
    }

    public void setReason(String reason)
    {
        this.reason = reason;
    }
}
