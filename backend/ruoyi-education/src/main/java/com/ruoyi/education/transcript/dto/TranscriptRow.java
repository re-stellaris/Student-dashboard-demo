package com.ruoyi.education.transcript.dto;

import java.math.BigDecimal;

/**
 * 成绩单导入的单行（前端 SheetJS 解析结果）
 *
 * <p>字段与 docs/architecture/api-contract.md §10.1 一一对应。所有字段都由学生自填，
 * 因此只用于写「自填成绩」；服务端会以 {@code courseCode + termCode} 重新解析
 * {@code course_id} / {@code term_id}，不采信前端可能给出的任何 ID。</p>
 *
 * @author ruoyi-education
 */
public class TranscriptRow
{
    /** 课程代码，如 CSC3100（未命中课程目录时仍写入快照） */
    private String courseCode;

    /** 课程名称（自填，允许为空，空时用课程目录回填） */
    private String courseTitle;

    /** 学期代码，如 2022-23-1（必须命中 edu_term.term_code） */
    private String termCode;

    /** 学分 */
    private BigDecimal units;

    /** 等级制成绩，如 A-；为空时按百分制调用 {@link #score} 判定通过 */
    private String grade;

    /** 百分制成绩 */
    private BigDecimal score;

    /** 成绩类别（Major/GenEd/English/Chinese/IT/PE/Other） */
    private String category;

    /** 前端解析出的通过标记（仅当 grade 与 score 都缺失时作为兜底） */
    private Boolean passed;

    /** 前端解析出的状态文本（Completed / In Progress / Withdrawn） */
    private String status;

    public String getCourseCode()
    {
        return courseCode;
    }

    public void setCourseCode(String courseCode)
    {
        this.courseCode = courseCode;
    }

    public String getCourseTitle()
    {
        return courseTitle;
    }

    public void setCourseTitle(String courseTitle)
    {
        this.courseTitle = courseTitle;
    }

    public String getTermCode()
    {
        return termCode;
    }

    public void setTermCode(String termCode)
    {
        this.termCode = termCode;
    }

    public BigDecimal getUnits()
    {
        return units;
    }

    public void setUnits(BigDecimal units)
    {
        this.units = units;
    }

    public String getGrade()
    {
        return grade;
    }

    public void setGrade(String grade)
    {
        this.grade = grade;
    }

    public BigDecimal getScore()
    {
        return score;
    }

    public void setScore(BigDecimal score)
    {
        this.score = score;
    }

    public String getCategory()
    {
        return category;
    }

    public void setCategory(String category)
    {
        this.category = category;
    }

    public Boolean getPassed()
    {
        return passed;
    }

    public void setPassed(Boolean passed)
    {
        this.passed = passed;
    }

    public String getStatus()
    {
        return status;
    }

    public void setStatus(String status)
    {
        this.status = status;
    }
}
