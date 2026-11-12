package com.ruoyi.education.planning.domain;

import java.math.BigDecimal;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 学习规划主表 edu_study_plan。
 *
 * @author ruoyi
 */
public class EduStudyPlan extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 规划ID */
    private Long planId;

    /** 学生ID（只允许由后端按登录用户反查，禁止前端传入） */
    private Long studentId;

    /** 目标学期ID */
    private Long termId;

    /** 规划标题 */
    private String title;

    /** 状态：DRAFT/VALIDATED/SUBMITTED/ARCHIVED */
    private String status;

    /** 计划总学分 */
    private BigDecimal totalUnits;

    /** 学分上限（可由管理员配置） */
    private BigDecimal unitLimit;

    /** 最近一次校验结果：PASS/WARN/FAIL */
    private String validationState;

    /** 校验摘要 */
    private String validationNote;

    /** 删除标志（0存在 2删除） */
    private String delFlag;

    /** 学期代码（关联查询，非表字段） */
    private String termCode;

    /** 学期名称（关联查询，非表字段） */
    private String termName;

    /** 学号（关联查询，非表字段） */
    private String studentNo;

    /** 学生姓名（关联查询，非表字段） */
    private String studentName;

    public Long getPlanId()
    {
        return planId;
    }

    public void setPlanId(Long planId)
    {
        this.planId = planId;
    }

    public Long getStudentId()
    {
        return studentId;
    }

    public void setStudentId(Long studentId)
    {
        this.studentId = studentId;
    }

    public Long getTermId()
    {
        return termId;
    }

    public void setTermId(Long termId)
    {
        this.termId = termId;
    }

    public String getTitle()
    {
        return title;
    }

    public void setTitle(String title)
    {
        this.title = title;
    }

    public String getStatus()
    {
        return status;
    }

    public void setStatus(String status)
    {
        this.status = status;
    }

    public BigDecimal getTotalUnits()
    {
        return totalUnits;
    }

    public void setTotalUnits(BigDecimal totalUnits)
    {
        this.totalUnits = totalUnits;
    }

    public BigDecimal getUnitLimit()
    {
        return unitLimit;
    }

    public void setUnitLimit(BigDecimal unitLimit)
    {
        this.unitLimit = unitLimit;
    }

    public String getValidationState()
    {
        return validationState;
    }

    public void setValidationState(String validationState)
    {
        this.validationState = validationState;
    }

    public String getValidationNote()
    {
        return validationNote;
    }

    public void setValidationNote(String validationNote)
    {
        this.validationNote = validationNote;
    }

    public String getDelFlag()
    {
        return delFlag;
    }

    public void setDelFlag(String delFlag)
    {
        this.delFlag = delFlag;
    }

    public String getTermCode()
    {
        return termCode;
    }

    public void setTermCode(String termCode)
    {
        this.termCode = termCode;
    }

    public String getTermName()
    {
        return termName;
    }

    public void setTermName(String termName)
    {
        this.termName = termName;
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
}
