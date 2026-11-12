package com.ruoyi.education.advisor.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * 学生列表分页结果（服务层内部使用）
 *
 * <p>{@code riskLevel} 是实时计算出来的派生条件，无法下推 SQL，因此分页在服务层
 * 对“过滤 + 排序”后的结果做切片；这里显式携带 {@code total}，
 * Controller 用它覆盖 {@code TableDataInfo.total}，保证分页总数与过滤结果一致。</p>
 *
 * @author ruoyi-education
 */
public class AdvisorStudentPageVO
{
    /** 当前页数据 */
    private List<AdvisorStudentVO> rows = new ArrayList<>();

    /** 过滤后的总记录数 */
    private long total;

    public List<AdvisorStudentVO> getRows()
    {
        return rows;
    }

    public void setRows(List<AdvisorStudentVO> rows)
    {
        this.rows = rows;
    }

    public long getTotal()
    {
        return total;
    }

    public void setTotal(long total)
    {
        this.total = total;
    }
}
