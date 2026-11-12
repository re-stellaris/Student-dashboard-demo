package com.ruoyi.education.curriculum.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.education.curriculum.domain.EduMajor;

/**
 * 专业 数据层
 *
 * @author ruoyi
 */
public interface EduMajorMapper
{
    /**
     * 查询专业列表
     *
     * @param major 专业信息（作为查询条件）
     * @return 专业集合
     */
    public List<EduMajor> selectMajorList(EduMajor major);

    /**
     * 根据专业ID查询专业
     *
     * @param majorId 专业ID
     * @return 专业信息
     */
    public EduMajor selectMajorById(@Param("majorId") Long majorId);

    /**
     * 校验专业编码是否已存在
     *
     * @param majorCode 专业编码
     * @return 已存在的数量
     */
    public int countMajorByCode(@Param("majorCode") String majorCode);
}
