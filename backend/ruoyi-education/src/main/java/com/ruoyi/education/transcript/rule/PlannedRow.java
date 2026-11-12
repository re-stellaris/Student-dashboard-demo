package com.ruoyi.education.transcript.rule;

import java.math.BigDecimal;
import com.ruoyi.education.transcript.dto.TranscriptRow;

/**
 * 单行的导入计划（判定结果），由 {@link TranscriptImportPlanner} 产出
 *
 * <p>这里保存的是「最终要写库的值」：课程代码 / 学期已归一化，课程名、学分、分类已按
 * 课程目录回填，{@code passed} / {@code recordStatus} 已按成绩与状态文本判定。
 * Service 只负责把 {@link Action#INSERT} / {@link Action#UPDATE} 的行落库。</p>
 *
 * @author ruoyi-education
 */
public class PlannedRow
{
    /** 单行的处置方式 */
    public enum Action
    {
        /** 新增 */
        INSERT,
        /** 覆盖同源记录 */
        UPDATE,
        /** 跳过 */
        SKIP
    }

    /** 处置方式 */
    private Action action;

    /** 原始请求行（空行为 null） */
    private TranscriptRow source;

    /** 归一化后的课程代码 */
    private String courseCode;

    /** 归一化后的学期代码 */
    private String termCode;

    /** 解析到的学期ID（跳过时为 null） */
    private Long termId;

    /** 命中的课程ID；课程目录未命中时为 null */
    private Long courseId;

    /** 课程名称（自填优先，其次课程目录） */
    private String courseTitle;

    /** 学分（自填优先，其次课程目录，均缺失时 0） */
    private BigDecimal units;

    /** 成绩分类（自填优先，其次课程目录） */
    private String category;

    /** 等级制成绩（归一化，空为 null） */
    private String grade;

    /** 百分制成绩 */
    private BigDecimal score;

    /** 是否通过，写入 edu_course_record.passed（"0"/"1"） */
    private String passed;

    /** 写入 edu_course_record.record_status */
    private String recordStatus;

    /** 已存在记录的主键（仅 UPDATE 时有值） */
    private Long existingRecordId;

    /** 跳过原因（仅 SKIP 时有值，中文，直接回给前端） */
    private String reason;

    public PlannedRow()
    {
    }

    public PlannedRow(Action action, TranscriptRow source)
    {
        this.action = action;
        this.source = source;
    }

    public Action getAction()
    {
        return action;
    }

    public void setAction(Action action)
    {
        this.action = action;
    }

    public TranscriptRow getSource()
    {
        return source;
    }

    public void setSource(TranscriptRow source)
    {
        this.source = source;
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

    public Long getTermId()
    {
        return termId;
    }

    public void setTermId(Long termId)
    {
        this.termId = termId;
    }

    public Long getCourseId()
    {
        return courseId;
    }

    public void setCourseId(Long courseId)
    {
        this.courseId = courseId;
    }

    public String getCourseTitle()
    {
        return courseTitle;
    }

    public void setCourseTitle(String courseTitle)
    {
        this.courseTitle = courseTitle;
    }

    public BigDecimal getUnits()
    {
        return units;
    }

    public void setUnits(BigDecimal units)
    {
        this.units = units;
    }

    public String getCategory()
    {
        return category;
    }

    public void setCategory(String category)
    {
        this.category = category;
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

    public String getPassed()
    {
        return passed;
    }

    public void setPassed(String passed)
    {
        this.passed = passed;
    }

    public String getRecordStatus()
    {
        return recordStatus;
    }

    public void setRecordStatus(String recordStatus)
    {
        this.recordStatus = recordStatus;
    }

    public Long getExistingRecordId()
    {
        return existingRecordId;
    }

    public void setExistingRecordId(Long existingRecordId)
    {
        this.existingRecordId = existingRecordId;
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
