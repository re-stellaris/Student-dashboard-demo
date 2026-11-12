package com.ruoyi.education.curriculum.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.education.curriculum.domain.EduCurriculumItem;

/**
 * 培养方案课程要求 数据层
 *
 * @author ruoyi
 */
public interface EduCurriculumItemMapper
{
    /**
     * 查询某培养方案的全部条目（关联 edu_course 返回课程名称与学分）
     *
     * @param curriculumId 培养方案ID
     * @return 条目集合
     */
    public List<EduCurriculumItem> selectItemsByCurriculumId(@Param("curriculumId") Long curriculumId);

    /**
     * 批量新增培养方案条目
     *
     * @param items 条目集合
     * @return 结果
     */
    public int insertItemBatch(@Param("items") List<EduCurriculumItem> items);

    /**
     * 删除某培养方案的全部条目
     *
     * @param curriculumId 培养方案ID
     * @return 结果
     */
    public int deleteItemsByCurriculumId(@Param("curriculumId") Long curriculumId);

    /**
     * 统计某培养方案的条目数量
     *
     * @param curriculumId 培养方案ID
     * @return 条目数量
     */
    public int countItemsByCurriculumId(@Param("curriculumId") Long curriculumId);
}
