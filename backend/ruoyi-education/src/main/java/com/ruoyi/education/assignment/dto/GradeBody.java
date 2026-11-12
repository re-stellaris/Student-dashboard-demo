package com.ruoyi.education.assignment.dto;

import java.math.BigDecimal;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 作业评分请求体（PUT /edu/submissions/grade）
 *
 * @author ruoyi-education
 */
public class GradeBody
{
    /** 提交ID */
    @NotNull(message = "提交ID不能为空")
    private Long submissionId;

    /** 得分 */
    @NotNull(message = "得分不能为空")
    @DecimalMin(value = "0", message = "得分不能为负数")
    private BigDecimal grade;

    /** 教师评语 */
    @Size(max = 1000, message = "教师评语不能超过1000个字符")
    private String feedback;

    public Long getSubmissionId()
    {
        return submissionId;
    }

    public void setSubmissionId(Long submissionId)
    {
        this.submissionId = submissionId;
    }

    public BigDecimal getGrade()
    {
        return grade;
    }

    public void setGrade(BigDecimal grade)
    {
        this.grade = grade;
    }

    public String getFeedback()
    {
        return feedback;
    }

    public void setFeedback(String feedback)
    {
        this.feedback = feedback;
    }
}
