package com.ruoyi.education.curriculum.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.education.curriculum.domain.EduCurriculum;
import com.ruoyi.education.curriculum.dto.CurriculumQuery;

/**
 * 培养方案 数据层
 *
 * @author ruoyi
 */
public interface EduCurriculumMapper
{
    /**
     * 查询培养方案列表
     *
     * @param query 查询条件（majorId / versionYear）
     * @return 培养方案集合
     */
    public List<EduCurriculum> selectCurriculumList(CurriculumQuery query);

    /**
     * 根据培养方案ID查询培养方案
     *
     * @param curriculumId 培养方案ID
     * @return 培养方案信息
     */
    public EduCurriculum selectCurriculumById(@Param("curriculumId") Long curriculumId);

    /**
     * 按专业与入学年份解析适用的培养方案版本
     *
     * <p>规则：{@code version_year <= entryYear and (effective_to is null or entryYear <= effective_to)}，
     * 取版本年份最大的一条。</p>
     *
     * @param majorId 专业ID
     * @param entryYear 入学年份
     * @return 适用的培养方案，无匹配版本时返回 null
     */
    public EduCurriculum selectCurriculumByMajorAndYear(@Param("majorId") Long majorId,
        @Param("entryYear") Integer entryYear);

    /**
     * 新增培养方案
     *
     * @param curriculum 培养方案信息
     * @return 结果
     */
    public int insertCurriculum(EduCurriculum curriculum);

    /**
     * 修改培养方案
     *
     * @param curriculum 培养方案信息
     * @return 结果
     */
    public int updateCurriculum(EduCurriculum curriculum);

    /**
     * 批量删除培养方案
     *
     * @param curriculumIds 需要删除的培养方案ID
     * @return 结果
     */
    public int deleteCurriculumByIds(@Param("curriculumIds") Long[] curriculumIds);

    /**
     * 校验同一专业同一生效年份的培养方案是否已存在
     *
     * @param majorId 专业ID
     * @param versionYear 生效入学年份
     * @param excludeCurriculumId 需要排除的培养方案ID（修改时传自身ID，新增时传 null）
     * @return 已存在的数量
     */
    public int countCurriculumByVersion(@Param("majorId") Long majorId,
        @Param("versionYear") Integer versionYear,
        @Param("excludeCurriculumId") Long excludeCurriculumId);
}
