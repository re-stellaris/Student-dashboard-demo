package com.ruoyi.education.curriculum.service.impl;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.education.course.domain.EduCourse;
import com.ruoyi.education.course.mapper.EduCourseMapper;
import com.ruoyi.education.curriculum.domain.EduCurriculum;
import com.ruoyi.education.curriculum.domain.EduCurriculumItem;
import com.ruoyi.education.curriculum.domain.EduMajor;
import com.ruoyi.education.curriculum.dto.CurriculumQuery;
import com.ruoyi.education.curriculum.mapper.EduCurriculumItemMapper;
import com.ruoyi.education.curriculum.mapper.EduCurriculumMapper;
import com.ruoyi.education.curriculum.mapper.EduMajorMapper;
import com.ruoyi.education.curriculum.service.IEduCurriculumService;

/**
 * 专业与培养方案 服务层实现
 *
 * <p>培养方案按版本保存；{@code resolveByMajor} 的匹配规则为
 * {@code version_year <= entryYear and (effective_to is null or entryYear <= effective_to)}，
 * 取版本年份最大的一条。</p>
 *
 * @author ruoyi
 */
@Service
public class EduCurriculumServiceImpl implements IEduCurriculumService
{
    @Autowired
    private EduMajorMapper majorMapper;

    @Autowired
    private EduCurriculumMapper curriculumMapper;

    @Autowired
    private EduCurriculumItemMapper curriculumItemMapper;

    @Autowired
    private EduCourseMapper courseMapper;

    @Override
    public List<EduMajor> selectMajorList(EduMajor major)
    {
        return majorMapper.selectMajorList(major == null ? new EduMajor() : major);
    }

    @Override
    public EduMajor selectMajorById(Long majorId)
    {
        if (majorId == null)
        {
            throw new ServiceException("专业ID不能为空");
        }
        return majorMapper.selectMajorById(majorId);
    }

    @Override
    public List<EduCurriculum> selectCurriculumList(CurriculumQuery query)
    {
        return curriculumMapper.selectCurriculumList(query == null ? new CurriculumQuery() : query);
    }

    @Override
    public EduCurriculum selectCurriculumById(Long curriculumId)
    {
        if (curriculumId == null)
        {
            throw new ServiceException("培养方案ID不能为空");
        }
        EduCurriculum curriculum = curriculumMapper.selectCurriculumById(curriculumId);
        if (curriculum == null)
        {
            return null;
        }
        curriculum.setItems(curriculumItemMapper.selectItemsByCurriculumId(curriculumId));
        return curriculum;
    }

    @Override
    public int insertCurriculum(EduCurriculum curriculum)
    {
        validateCurriculum(curriculum, false);
        int sameVersion = curriculumMapper.countCurriculumByVersion(curriculum.getMajorId(),
            curriculum.getVersionYear(), null);
        if (sameVersion > 0)
        {
            throw new ServiceException("该专业已存在 " + curriculum.getVersionYear() + " 入学年份的培养方案，请勿重复创建");
        }
        return curriculumMapper.insertCurriculum(curriculum);
    }

    @Override
    public int updateCurriculum(EduCurriculum curriculum)
    {
        if (curriculum == null || curriculum.getCurriculumId() == null)
        {
            throw new ServiceException("培养方案ID不能为空");
        }
        EduCurriculum exist = curriculumMapper.selectCurriculumById(curriculum.getCurriculumId());
        if (exist == null)
        {
            throw new ServiceException("培养方案不存在");
        }
        validateCurriculum(curriculum, true);
        int sameVersion = curriculumMapper.countCurriculumByVersion(curriculum.getMajorId(),
            curriculum.getVersionYear(), curriculum.getCurriculumId());
        if (sameVersion > 0)
        {
            throw new ServiceException("该专业已存在相同生效年份的培养方案，版本不可重复");
        }
        return curriculumMapper.updateCurriculum(curriculum);
    }

    @Override
    @Transactional
    public int deleteCurriculumByIds(Long[] curriculumIds)
    {
        if (curriculumIds == null || curriculumIds.length == 0)
        {
            return 0;
        }
        for (Long curriculumId : curriculumIds)
        {
            if (curriculumId != null)
            {
                curriculumItemMapper.deleteItemsByCurriculumId(curriculumId);
            }
        }
        return curriculumMapper.deleteCurriculumByIds(curriculumIds);
    }

    @Override
    public List<EduCurriculumItem> selectItemsByCurriculumId(Long curriculumId)
    {
        if (curriculumId == null)
        {
            throw new ServiceException("培养方案ID不能为空");
        }
        List<EduCurriculumItem> items = curriculumItemMapper.selectItemsByCurriculumId(curriculumId);
        return items == null ? new ArrayList<>() : items;
    }

    @Override
    public int countItemsByCurriculumId(Long curriculumId)
    {
        if (curriculumId == null)
        {
            throw new ServiceException("培养方案ID不能为空");
        }
        return curriculumItemMapper.countItemsByCurriculumId(curriculumId);
    }

    @Override
    @Transactional
    public void replaceItems(Long curriculumId, List<EduCurriculumItem> items)
    {
        if (curriculumId == null)
        {
            throw new ServiceException("培养方案ID不能为空");
        }
        EduCurriculum exist = curriculumMapper.selectCurriculumById(curriculumId);
        if (exist == null)
        {
            throw new ServiceException("培养方案不存在");
        }
        List<EduCurriculumItem> rows = normalizeItems(curriculumId, items);
        curriculumItemMapper.deleteItemsByCurriculumId(curriculumId);
        if (!rows.isEmpty())
        {
            curriculumItemMapper.insertItemBatch(rows);
        }
    }

    @Override
    public EduCurriculum resolveByMajor(Long majorId, Integer entryYear)
    {
        if (majorId == null)
        {
            throw new ServiceException("专业ID不能为空");
        }
        if (entryYear == null)
        {
            throw new ServiceException("入学年份不能为空");
        }
        EduCurriculum curriculum = curriculumMapper.selectCurriculumByMajorAndYear(majorId, entryYear);
        if (curriculum == null)
        {
            return null;
        }
        curriculum.setItems(curriculumItemMapper.selectItemsByCurriculumId(curriculum.getCurriculumId()));
        return curriculum;
    }

    /**
     * 校验培养方案入参
     *
     * @param curriculum 培养方案
     * @param forUpdate 是否为修改场景
     */
    private void validateCurriculum(EduCurriculum curriculum, boolean forUpdate)
    {
        if (curriculum == null)
        {
            throw new ServiceException("培养方案信息不能为空");
        }
        if (curriculum.getMajorId() == null)
        {
            throw new ServiceException("专业ID不能为空");
        }
        if (majorMapper.selectMajorById(curriculum.getMajorId()) == null)
        {
            throw new ServiceException("专业不存在：" + curriculum.getMajorId());
        }
        if (StringUtils.isEmpty(curriculum.getVersionLabel()))
        {
            throw new ServiceException("版本标签不能为空");
        }
        if (curriculum.getVersionYear() == null)
        {
            throw new ServiceException("生效入学年份不能为空");
        }
        if (curriculum.getVersionYear().intValue() < 1900 || curriculum.getVersionYear().intValue() > 2999)
        {
            throw new ServiceException("生效入学年份不合法：" + curriculum.getVersionYear());
        }
        Integer effectiveFrom = curriculum.getEffectiveFrom();
        Integer effectiveTo = curriculum.getEffectiveTo();
        if (effectiveFrom != null && effectiveTo != null && effectiveFrom.intValue() > effectiveTo.intValue())
        {
            throw new ServiceException("适用起始入学年不能大于适用结束入学年");
        }
        if (curriculum.getTotalCredits() != null
            && curriculum.getTotalCredits().compareTo(BigDecimal.ZERO) < 0)
        {
            throw new ServiceException("毕业总学分不能为负数");
        }
        if (curriculum.getMajorCredits() != null
            && curriculum.getMajorCredits().compareTo(BigDecimal.ZERO) < 0)
        {
            throw new ServiceException("专业学分不能为负数");
        }
        if (!forUpdate)
        {
            curriculum.setCurriculumId(null);
        }
    }

    /**
     * 校验并规整培养方案条目入参
     *
     * @param curriculumId 培养方案ID
     * @param items 前端入参
     * @return 可直接入库的条目
     */
    private List<EduCurriculumItem> normalizeItems(Long curriculumId, List<EduCurriculumItem> items)
    {
        List<EduCurriculumItem> rows = new ArrayList<>();
        if (items == null || items.isEmpty())
        {
            return rows;
        }
        int index = 0;
        for (EduCurriculumItem item : items)
        {
            if (item == null)
            {
                continue;
            }
            index++;
            String requirementType = StringUtils.trim(item.getRequirementType());
            if (StringUtils.isEmpty(requirementType))
            {
                throw new ServiceException("培养方案条目的要求类型不能为空");
            }
            BigDecimal minUnits = item.getMinUnits() == null ? BigDecimal.ZERO : item.getMinUnits();
            if (minUnits.compareTo(BigDecimal.ZERO) < 0)
            {
                throw new ServiceException("培养方案条目的最低学分不能为负数");
            }
            EduCurriculumItem row = new EduCurriculumItem();
            row.setCurriculumId(curriculumId);
            row.setCourseId(item.getCourseId());
            row.setCourseCode(StringUtils.trim(item.getCourseCode()));
            row.setRequirementType(requirementType);
            row.setSection(item.getSection());
            row.setStream(item.getStream());
            row.setMinUnits(minUnits);
            row.setSortOrder(item.getSortOrder() == null ? Integer.valueOf(index) : item.getSortOrder());
            row.setCreateBy(item.getCreateBy());
            if (row.getCourseId() != null)
            {
                EduCourse course = courseMapper.selectCourseById(row.getCourseId());
                if (course == null)
                {
                    throw new ServiceException("培养方案条目引用的课程不存在：" + row.getCourseId());
                }
                if (StringUtils.isEmpty(row.getCourseCode()))
                {
                    row.setCourseCode(course.getCode());
                }
            }
            rows.add(row);
        }
        return rows;
    }
}
