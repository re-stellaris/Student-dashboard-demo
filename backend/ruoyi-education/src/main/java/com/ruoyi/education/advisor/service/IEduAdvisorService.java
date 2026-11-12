package com.ruoyi.education.advisor.service;

import com.ruoyi.education.advisor.dto.AdvisorOverviewVO;
import com.ruoyi.education.advisor.dto.AdvisorStudentDetailVO;
import com.ruoyi.education.advisor.dto.AdvisorStudentQuery;
import com.ruoyi.education.advisor.dto.AdvisorStudentPageVO;

/**
 * 导师看板 服务接口
 *
 * <p>只读聚合服务：总览、学生列表（服务层分页）、单个学生画像。</p>
 *
 * @author ruoyi-education
 */
public interface IEduAdvisorService
{
    /**
     * 导师看板总览：学生数、平均 GPA/学分、GPA 分布与趋势、风险与进度分布。
     *
     * @return 总览数据
     */
    AdvisorOverviewVO overview();

    /**
     * 学生列表（按入学年份 / 专业 / 关键字过滤，风险等级在服务层实时判定后分页）。
     *
     * @param query 查询条件
     * @param pageNum 页码（从 1 开始）
     * @param pageSize 每页条数
     * @return 当前页数据 + 过滤后的总数
     */
    AdvisorStudentPageVO selectStudentPage(AdvisorStudentQuery query, int pageNum, int pageSize);

    /**
     * 单个学生画像。
     *
     * @param studentId 学生ID
     * @return 画像数据
     */
    AdvisorStudentDetailVO selectStudentDetail(Long studentId);
}
