package com.ruoyi.education.enrollment.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.education.enrollment.domain.EduTerm;

/**
 * 学期 Mapper 接口
 *
 * @author ruoyi-education
 */
public interface EduTermMapper
{
    /**
     * 查询学期列表
     *
     * @param term 查询条件（学期代码、名称、状态、是否当前学期）
     * @return 学期集合
     */
    List<EduTerm> selectTermList(EduTerm term);

    /**
     * 查询当前学期（is_current = '1'，回退到最新学期）
     *
     * @return 学期
     */
    EduTerm selectCurrentTerm();

    /**
     * 按学期代码查询学期
     *
     * @param termCode 学期代码，如 2026-27-1
     * @return 学期
     */
    EduTerm selectTermByCode(@Param("termCode") String termCode);

    /**
     * 按主键查询学期
     *
     * @param termId 学期ID
     * @return 学期
     */
    EduTerm selectTermById(@Param("termId") Long termId);

    /**
     * 新增学期
     *
     * @param term 学期
     * @return 影响行数
     */
    int insertTerm(EduTerm term);

    /**
     * 修改学期
     *
     * @param term 学期
     * @return 影响行数
     */
    int updateTerm(EduTerm term);

    /**
     * 批量删除学期
     *
     * @param termIds 学期ID数组
     * @return 影响行数
     */
    int deleteTermByIds(Long[] termIds);
}
