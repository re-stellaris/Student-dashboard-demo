package com.ruoyi.education.transcript.rule;

import java.util.ArrayList;
import java.util.List;
import com.ruoyi.education.transcript.dto.SkippedRow;

/**
 * 整批导入计划（{@link TranscriptImportPlanner} 的返回值）
 *
 * <p>计数与明细在 {@link #add(PlannedRow)} 时一次算好，Service 不再重复统计，
 * 从而保证 {@code rowCount = inserted + updated + skipped} 这条不变式只有一处实现。</p>
 *
 * @author ruoyi-education
 */
public class TranscriptImportPlan
{
    /** 提交的总行数 */
    private final int rowCount;

    /** 每行的处置结果（与请求顺序一致） */
    private final List<PlannedRow> rows = new ArrayList<>();

    /** 未命中课程目录的课程代码（去重、保持出现顺序） */
    private final List<String> unknownCourses = new ArrayList<>();

    /** 未命中 edu_term 的学期代码（去重、保持出现顺序） */
    private final List<String> unknownTerms = new ArrayList<>();

    /** 跳过明细 */
    private final List<SkippedRow> skippedDetails = new ArrayList<>();

    /** 新增行数 */
    private int inserted;

    /** 覆盖行数 */
    private int updated;

    /** 跳过行数 */
    private int skipped;

    public TranscriptImportPlan(int rowCount)
    {
        this.rowCount = rowCount;
    }

    /**
     * 登记一行判定结果，并同步计数与跳过明细
     *
     * @param row 判定结果
     */
    public void add(PlannedRow row)
    {
        if (row == null)
        {
            return;
        }
        rows.add(row);
        PlannedRow.Action action = row.getAction();
        if (PlannedRow.Action.INSERT == action)
        {
            inserted++;
        }
        else if (PlannedRow.Action.UPDATE == action)
        {
            updated++;
        }
        else
        {
            skipped++;
            skippedDetails.add(new SkippedRow(nullToEmpty(row.getCourseCode()), nullToEmpty(row.getTermCode()),
                    nullToEmpty(row.getReason())));
        }
    }

    /**
     * 记录一个未知课程代码（去重）
     *
     * @param courseCode 课程代码
     */
    public void markUnknownCourse(String courseCode)
    {
        if (courseCode != null && !courseCode.isEmpty() && !unknownCourses.contains(courseCode))
        {
            unknownCourses.add(courseCode);
        }
    }

    /**
     * 记录一个未知学期代码（去重）
     *
     * @param termCode 学期代码
     */
    public void markUnknownTerm(String termCode)
    {
        if (termCode != null && !termCode.isEmpty() && !unknownTerms.contains(termCode))
        {
            unknownTerms.add(termCode);
        }
    }

    private static String nullToEmpty(String value)
    {
        return value == null ? "" : value;
    }

    public int getRowCount()
    {
        return rowCount;
    }

    public List<PlannedRow> getRows()
    {
        return rows;
    }

    public List<String> getUnknownCourses()
    {
        return unknownCourses;
    }

    public List<String> getUnknownTerms()
    {
        return unknownTerms;
    }

    public List<SkippedRow> getSkippedDetails()
    {
        return skippedDetails;
    }

    public int getInserted()
    {
        return inserted;
    }

    public int getUpdated()
    {
        return updated;
    }

    public int getSkipped()
    {
        return skipped;
    }
}
