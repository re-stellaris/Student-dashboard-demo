package com.ruoyi.education.transcript.domain;

/**
 * 学期代码 → 学期ID 的只读投影（edu_term）
 *
 * <p>成绩单导入时需要把前端解析出的 {@code termCode} 解析为 {@code edu_term.term_id}，
 * 因此只读取两列，避免为了一个 ID 把整行 {@code EduTerm} 查出来。</p>
 *
 * @author ruoyi-education
 */
public class TranscriptTermRef
{
    /** 学期ID（edu_term.term_id） */
    private Long termId;

    /** 学期代码（edu_term.term_code），如 2022-23-1 */
    private String termCode;

    public Long getTermId()
    {
        return termId;
    }

    public void setTermId(Long termId)
    {
        this.termId = termId;
    }

    public String getTermCode()
    {
        return termCode;
    }

    public void setTermCode(String termCode)
    {
        this.termCode = termCode;
    }
}
