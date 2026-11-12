package com.ruoyi.education.planning.spi;

import java.util.List;
import com.ruoyi.education.planning.rule.CurriculumRule;

/**
 * 培养方案只读端口：按学生专业 + 入学年份解析适用版本，读取课程要求条目。
 *
 * <p>对应培养方案域的能力：
 * {@code com.ruoyi.education.curriculum.mapper.EduCurriculumMapper#selectByMajorAndYear} 与
 * {@code EduCurriculumItemMapper#selectItemsByCurriculumId}。
 * 默认实现见 {@code DefaultPlanningReadPorts}。</p>
 *
 * @author ruoyi
 */
public interface CurriculumPort
{
    /**
     * 读取适用培养方案条目（仅 {@code course_id} 非空的条目参与逐课校验）。
     *
     * @param majorId 专业ID，可为 {@code null}
     * @param entryYear 入学年份，可为 {@code null}
     * @return 培养方案条目；无适用版本时返回空列表（不返回 {@code null}）
     */
    List<CurriculumRule> loadCurriculumRules(Long majorId, Integer entryYear);
}
