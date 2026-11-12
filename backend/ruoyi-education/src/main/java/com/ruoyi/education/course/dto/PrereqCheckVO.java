package com.ruoyi.education.course.dto;

import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

/**
 * 先修校验结果
 *
 * <p>对应接口 {@code GET /edu/courses/{courseId}/prereq-check}，返回结构：
 * {@code {"satisfied":true,"missing":[{"courseId":1,"code":"CSC1001","title":"..."}],"reason":"..."}}。</p>
 *
 * <p>判断依据是 <b>当前登录学生</b> 的已通过课程，学生身份由后端按
 * {@code edu_student.sys_user_id} 反查得到，绝不接受前端传入的 studentId。</p>
 *
 * @author ruoyi
 */
public class PrereqCheckVO
{
    /** 先修条件是否满足 */
    private boolean satisfied;

    /** 未满足的先修课程（取“最少阻力”的先修组） */
    private List<MissingCourse> missing = new ArrayList<>();

    /** 可读说明 */
    private String reason;

    public boolean isSatisfied()
    {
        return satisfied;
    }

    public void setSatisfied(boolean satisfied)
    {
        this.satisfied = satisfied;
    }

    public List<MissingCourse> getMissing()
    {
        return missing;
    }

    public void setMissing(List<MissingCourse> missing)
    {
        this.missing = missing;
    }

    public String getReason()
    {
        return reason;
    }

    public void setReason(String reason)
    {
        this.reason = reason;
    }

    @Override
    public String toString()
    {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
            .append("satisfied", isSatisfied())
            .append("missing", getMissing())
            .append("reason", getReason())
            .toString();
    }

    /**
     * 未满足的先修课程
     */
    public static class MissingCourse
    {
        /** 先修课程ID */
        private Long courseId;

        /** 先修课程代码 */
        private String code;

        /** 先修课程名称 */
        private String title;

        public MissingCourse()
        {
        }

        public MissingCourse(Long courseId, String code, String title)
        {
            this.courseId = courseId;
            this.code = code;
            this.title = title;
        }

        public Long getCourseId()
        {
            return courseId;
        }

        public void setCourseId(Long courseId)
        {
            this.courseId = courseId;
        }

        public String getCode()
        {
            return code;
        }

        public void setCode(String code)
        {
            this.code = code;
        }

        public String getTitle()
        {
            return title;
        }

        public void setTitle(String title)
        {
            this.title = title;
        }

        @Override
        public String toString()
        {
            return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
                .append("courseId", getCourseId())
                .append("code", getCode())
                .append("title", getTitle())
                .toString();
        }
    }
}
