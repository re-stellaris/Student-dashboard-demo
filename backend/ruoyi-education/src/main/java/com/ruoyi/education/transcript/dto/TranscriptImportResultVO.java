package com.ruoyi.education.transcript.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * 成绩单导入结果（docs/architecture/api-contract.md §10.2）
 *
 * <p>不变式：{@code rowCount = inserted + updated + skipped}，每一行都必须落到三类之一，
 * 前端据此展示「插入 / 更新 / 跳过」与未知课程代码清单。</p>
 *
 * @author ruoyi-education
 */
public class TranscriptImportResultVO
{
    /** 提交的总行数 */
    private int rowCount;

    /** 新增行数 */
    private int inserted;

    /** 覆盖（同源更新）行数，重复导入同一批数据时应全部落在这里 */
    private int updated;

    /** 跳过行数（未知学期、官方记录不覆盖、空行等） */
    private int skipped;

    /** 未命中 edu_course.code 的课程代码（已按快照插入，course_id = null） */
    private List<String> unknownCourses = new ArrayList<>();

    /** 未命中 edu_term.term_code 的学期代码（该行未写入） */
    private List<String> unknownTerms = new ArrayList<>();

    /** 跳过明细 */
    private List<SkippedRow> skippedDetails = new ArrayList<>();

    public int getRowCount()
    {
        return rowCount;
    }

    public void setRowCount(int rowCount)
    {
        this.rowCount = rowCount;
    }

    public int getInserted()
    {
        return inserted;
    }

    public void setInserted(int inserted)
    {
        this.inserted = inserted;
    }

    public int getUpdated()
    {
        return updated;
    }

    public void setUpdated(int updated)
    {
        this.updated = updated;
    }

    public int getSkipped()
    {
        return skipped;
    }

    public void setSkipped(int skipped)
    {
        this.skipped = skipped;
    }

    public List<String> getUnknownCourses()
    {
        return unknownCourses;
    }

    public void setUnknownCourses(List<String> unknownCourses)
    {
        this.unknownCourses = unknownCourses;
    }

    public List<String> getUnknownTerms()
    {
        return unknownTerms;
    }

    public void setUnknownTerms(List<String> unknownTerms)
    {
        this.unknownTerms = unknownTerms;
    }

    public List<SkippedRow> getSkippedDetails()
    {
        return skippedDetails;
    }

    public void setSkippedDetails(List<SkippedRow> skippedDetails)
    {
        this.skippedDetails = skippedDetails;
    }
}
