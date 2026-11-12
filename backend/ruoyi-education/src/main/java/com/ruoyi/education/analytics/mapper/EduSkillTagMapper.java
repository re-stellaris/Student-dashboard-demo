package com.ruoyi.education.analytics.mapper;

import java.util.List;
import com.ruoyi.education.analytics.domain.EduSkillTag;

/**
 * 公共技能标签 Mapper 接口
 *
 * @author ruoyi-education
 */
public interface EduSkillTagMapper
{
    /**
     * 查询技能标签列表
     *
     * @param skillTag 查询条件（code、name、category、schoolAbbr、status）
     * @return 标签集合
     */
    List<EduSkillTag> selectSkillTagList(EduSkillTag skillTag);

    /**
     * 按主键查询技能标签
     *
     * @param skillTagId 技能标签ID
     * @return 标签
     */
    EduSkillTag selectSkillTagById(Long skillTagId);
}
