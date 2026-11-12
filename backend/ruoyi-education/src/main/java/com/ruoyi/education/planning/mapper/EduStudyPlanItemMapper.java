package com.ruoyi.education.planning.mapper;

import java.util.List;
import com.ruoyi.education.planning.domain.EduStudyPlanItem;

/**
 * 学习规划明细 Mapper（edu_study_plan_item）。
 *
 * @author ruoyi
 */
public interface EduStudyPlanItemMapper
{
    /**
     * 查询规划明细，并联表补全课程代码 / 名称 / 学分。
     *
     * @param planId 规划ID
     * @return 明细列表（按 sort_order 升序）
     */
    List<EduStudyPlanItem> selectItemsByPlanId(Long planId);

    /**
     * 批量写入明细（保存即重算 reason / validation_status / validation_message）。
     *
     * @param items 明细列表
     * @return 影响行数
     */
    int insertItemBatch(List<EduStudyPlanItem> items);

    /**
     * 按规划清空明细（覆盖式保存）。
     *
     * @param planId 规划ID
     * @return 影响行数
     */
    int deleteItemsByPlanId(Long planId);

    /**
     * 统计规划明细条数（提交前非空校验）。
     *
     * @param planId 规划ID
     * @return 明细条数
     */
    int countItemsByPlanId(Long planId);
}
