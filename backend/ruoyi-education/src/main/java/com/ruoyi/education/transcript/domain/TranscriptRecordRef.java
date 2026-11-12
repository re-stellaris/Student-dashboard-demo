package com.ruoyi.education.transcript.domain;

/**
 * 已存在成绩记录的只读投影（edu_course_record）
 *
 * <p>导入判定只需要三件事：这条 {@code (student_id, course_code, term_id)} 记录是否存在、
 * 主键是多少（更新时定位）、以及它的 {@code source}（是否为 TRANSCRIPT_UPLOAD）。
 * 因此不做整行映射。</p>
 *
 * @author ruoyi-education
 */
public class TranscriptRecordRef
{
    /** 成绩记录ID（edu_course_record.record_id） */
    private Long recordId;

    /** 课程代码快照（edu_course_record.course_code） */
    private String courseCode;

    /** 学期ID（edu_course_record.term_id） */
    private Long termId;

    /** 数据来源（edu_course_record.source）：SIS_IMPORT / DEMO / MANUAL / TRANSCRIPT_UPLOAD */
    private String source;

    public Long getRecordId()
    {
        return recordId;
    }

    public void setRecordId(Long recordId)
    {
        this.recordId = recordId;
    }

    public String getCourseCode()
    {
        return courseCode;
    }

    public void setCourseCode(String courseCode)
    {
        this.courseCode = courseCode;
    }

    public Long getTermId()
    {
        return termId;
    }

    public void setTermId(Long termId)
    {
        this.termId = termId;
    }

    public String getSource()
    {
        return source;
    }

    public void setSource(String source)
    {
        this.source = source;
    }
}
