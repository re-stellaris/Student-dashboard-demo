package com.ruoyi.education.assignment.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 作业提交请求体（POST /edu/submissions）
 *
 * <p>学生身份只取当前登录用户，**不接受** studentId。</p>
 *
 * @author ruoyi-education
 */
public class SubmissionBody
{
    /** 作业ID */
    @NotNull(message = "作业ID不能为空")
    private Long assignmentId;

    /** 提交内容 */
    @Size(max = 20000, message = "提交内容过长")
    private String content;

    /** 附件地址 */
    @Size(max = 500, message = "附件地址过长")
    private String attachmentUrl;

    public Long getAssignmentId()
    {
        return assignmentId;
    }

    public void setAssignmentId(Long assignmentId)
    {
        this.assignmentId = assignmentId;
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
}
