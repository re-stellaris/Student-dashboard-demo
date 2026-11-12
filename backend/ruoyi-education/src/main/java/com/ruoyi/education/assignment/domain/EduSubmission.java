package com.ruoyi.education.assignment.domain;

import java.math.BigDecimal;
import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 作业提交对象 edu_submission
 *
 * <p>非持久化字段（联查回显）：studentNo、studentName、assignmentTitle、totalScore。</p>
 *
 * @author ruoyi-education
 */
public class EduSubmission extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 提交ID */
    private Long submissionId;

    /** 作业ID */
    private Long assignmentId;

    /** 学生ID */
    private Long studentId;

    /** 提交内容 */
    private String content;

    /** 附件地址 */
    private String attachmentUrl;

    /** 提交时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date submittedAt;

    /** 得分 */
    private BigDecimal grade;

    /** 教师评语 */
    private String feedback;

    /** 状态：SUBMITTED/GRADED/RETURNED */
    private String status;

    /** 学号（联查 edu_student） */
    private String studentNo;

    /** 学生姓名（联查 edu_student） */
    private String studentName;

    /** 作业标题（联查 edu_assignment） */
    private String assignmentTitle;

    /** 作业满分（联查 edu_assignment） */
    private BigDecimal totalScore;

    public Long getSubmissionId()
    {
        return submissionId;
    }

    public void setSubmissionId(Long submissionId)
    {
        this.submissionId = submissionId;
    }

    public Long getAssignmentId()
    {
        return assignmentId;
    }

    public void setAssignmentId(Long assignmentId)
    {
        this.assignmentId = assignmentId;
    }

    public Long getStudentId()
    {
        return studentId;
    }

    public void setStudentId(Long studentId)
    {
        this.studentId = studentId;
    }

    public String getContent()
    {
        return content;
    }

    public void setContent(String content)
    {
        this.content = content;
    }

    public String getAttachmentUrl()
    {
        return attachmentUrl;
    }

    public void setAttachmentUrl(String attachmentUrl)
    {
        this.attachmentUrl = attachmentUrl;
    }

    public Date getSubmittedAt()
    {
        return submittedAt;
    }

    public void setSubmittedAt(Date submittedAt)
    {
        this.submittedAt = submittedAt;
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

    public String getStatus()
    {
        return status;
    }

    public void setStatus(String status)
    {
        this.status = status;
    }

    public String getStudentNo()
    {
        return studentNo;
    }

    public void setStudentNo(String studentNo)
    {
        this.studentNo = studentNo;
    }

    public String getStudentName()
    {
        return studentName;
    }

    public void setStudentName(String studentName)
    {
        this.studentName = studentName;
    }

    public String getAssignmentTitle()
    {
        return assignmentTitle;
    }

    public void setAssignmentTitle(String assignmentTitle)
    {
        this.assignmentTitle = assignmentTitle;
    }

    public BigDecimal getTotalScore()
    {
        return totalScore;
    }

    public void setTotalScore(BigDecimal totalScore)
    {
        this.totalScore = totalScore;
    }

    @Override
    public String toString()
    {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
            .append("submissionId", getSubmissionId())
            .append("assignmentId", getAssignmentId())
            .append("studentId", getStudentId())
            .append("content", getContent())
            .append("attachmentUrl", getAttachmentUrl())
            .append("submittedAt", getSubmittedAt())
            .append("grade", getGrade())
            .append("feedback", getFeedback())
            .append("status", getStatus())
            .append("studentNo", getStudentNo())
            .append("studentName", getStudentName())
            .append("assignmentTitle", getAssignmentTitle())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .toString();
    }
}
