package com.ruoyi.education.curriculum.service;

import java.util.List;
import com.ruoyi.education.curriculum.domain.EduCurriculum;
import com.ruoyi.education.curriculum.domain.EduCurriculumItem;
import com.ruoyi.education.curriculum.domain.EduMajor;
import com.ruoyi.education.curriculum.dto.CurriculumQuery;

/**
 * 专业与培养方案 服务层
 *
 * @author ruoyi
 */
public interface IEduCurriculumService
{
    /**
     * 查询专业列表
     *
     * @param major 查询条件
     * @return 专业集合
     */
    public List<EduMajor> selectMajorList(EduMajor major);

    /**
     * 根据专业ID查询专业
     *
     * @param majorId 专业ID
     * @return 专业信息
     */
    public EduMajor selectMajorById(Long majorId);

    /**
     * 查询培养方案列表
     *
     * @param query 查询条件
     * @return 培养方案集合
     */
    public List<EduCurriculum> selectCurriculumList(CurriculumQuery query);

    /**
     * 查询培养方案详情（含条目）
     *
     * @param curriculumId 培养方案ID
     * @return 培养方案信息
     */
    public EduCurriculum selectCurriculumById(Long curriculumId);

    /**
     * 新增培养方案（校验同专业同版本唯一）
     *
     * @param curriculum 培养方案信息
     * @return 结果
     */
    public int insertCurriculum(EduCurriculum curriculum);

    /**
     * 修改培养方案（校验同专业同版本唯一）
     *
     * @param curriculum 培养方案信息
     * @return 结果
     */
    public int updateCurriculum(EduCurriculum curriculum);

    /**
     * 批量删除培养方案（连同条目一起删除）
     *
     * @param curriculumIds 需要删除的培养方案ID
     * @return 结果
     */
    public int deleteCurriculumByIds(Long[] curriculumIds);

    /**
     * 查询某培养方案的全部条目
     *
     * @param curriculumId 培养方案ID
     * @return 条目集合
     */
    public List<EduCurriculumItem> selectItemsByCurriculumId(Long curriculumId);

    /**
     * 统计某培养方案的条目数量
     *
     * @param curriculumId 培养方案ID
     * @return 条目数量
     */
    public int countItemsByCurriculumId(Long curriculumId);

    /**
     * 覆盖式更新培养方案条目
     *
     * @param curriculumId 培养方案ID
     * @param items 条目集合
     */
    public void replaceItems(Long curriculumId, List<EduCurriculumItem> items);

    /**
     * 按专业 + 入学年份解析适用的培养方案版本（含条目）
     *
     * @param majorId 专业ID
     * @param entryYear 入学年份
     * @return 适用的培养方案，无匹配版本时返回 null
     */
    public EduCurriculum resolveByMajor(Long majorId, Integer entryYear);
}
