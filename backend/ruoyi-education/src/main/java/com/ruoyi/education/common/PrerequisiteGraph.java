package com.ruoyi.education.common;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import com.ruoyi.education.course.domain.EduCourse;
import com.ruoyi.education.course.domain.EduCoursePrereq;

/**
 * 课程先修依赖图。
 *
 * <p>规则语义（docs/architecture/backend-conventions.md 第 6 节）：</p>
 * <ul>
 *   <li>{@code edu_course_prereq} 的行按 {@code group_no} 分组；</li>
 *   <li><b>组内 AND</b>：同一 group_no 内的先修课程必须全部通过；</li>
 *   <li><b>组间 OR</b>：任一 group_no 组被完整满足，即视为先修条件满足；</li>
 *   <li>自依赖（course_id == prereq_course_id）非法，载入时直接丢弃；</li>
 *   <li>环检测必须迭代实现（不使用递归），可处理 5000+ 节点。</li>
 * </ul>
 *
 * <p>本类不是 Spring Bean：业务代码可直接 {@code new PrerequisiteGraph(rows)}
 * 或使用 {@link #fromGrouped(Map)} 在无课程实体的场景（单元测试、规则试算）下构造。</p>
 *
 * @author ruoyi
 */
public class PrerequisiteGraph
{
    private static final List<Long> EMPTY_IDS = Collections.emptyList();

    private static final List<EduCoursePrereq> EMPTY_ROWS = Collections.emptyList();

    /** 原始先修行：courseId -> 该课程的先修行（保持入库顺序） */
    private final Map<Long, List<EduCoursePrereq>> graph;

    /** 归一化后的分组边：courseId -> groupNo -> prereqCourseIds */
    private final Map<Long, Map<Integer, List<Long>>> grouped;

    /** 先修课程实体索引（用于 missingPrerequisites 返回可读课程） */
    private final Map<Long, EduCourse> catalog;

    /** 载入时发现的自依赖课程 ID */
    private final List<Long> selfReferences;

    /**
     * 构造空图。
     */
    public PrerequisiteGraph()
    {
        this(Collections.<Long, List<EduCoursePrereq>>emptyMap(), Collections.<EduCourse>emptyList());
    }

    /**
     * 按 {@code edu_course_prereq} 的行构造先修图。
     *
     * @param graph 先修行，key 为目标课程 ID，value 为该课程的全部先修行
     */
    public PrerequisiteGraph(Map<Long, List<EduCoursePrereq>> graph)
    {
        this(graph, Collections.<EduCourse>emptyList());
    }

    /**
     * 按先修行构造先修图，并提供课程目录用于补齐“缺少先修课”的可读信息。
     *
     * @param graph 先修行
     * @param catalog 课程目录（可为 {@code null}）
     */
    public PrerequisiteGraph(Map<Long, List<EduCoursePrereq>> graph, Collection<EduCourse> catalog)
    {
        Map<Long, List<EduCoursePrereq>> normalizedRows = normalizeRows(graph);
        Map<Long, Map<Integer, List<Long>>> normalizedGroups = groupRows(normalizedRows);
        this.graph = normalizedRows;
        this.grouped = normalizedGroups;
        this.catalog = indexCatalog(catalog);
        this.selfReferences = collectSelfReferences(graph);
    }

    /**
     * 使用“已分组边”构造先修图：{@code courseId -> groupNo -> prereqCourseIds}。
     *
     * <p>该工厂方法不依赖 {@link EduCoursePrereq}，供纯规则计算与单元测试使用。
     * 自依赖边同样会被丢弃。</p>
     *
     * @param grouped 分组边，可为 {@code null}
     * @return 先修图
     */
    public static PrerequisiteGraph fromGrouped(Map<Long, Map<Integer, List<Long>>> grouped)
    {
        Map<Long, Map<Integer, List<Long>>> normalized = normalizeGrouped(grouped);
        return new PrerequisiteGraph(normalized, Collections.<Long, List<EduCoursePrereq>>emptyMap(),
                Collections.<Long, EduCourse>emptyMap(), Collections.<Long>emptyList());
    }

    /**
     * 内部构造：直接使用已完成归一化的数据。
     */
    private PrerequisiteGraph(Map<Long, Map<Integer, List<Long>>> grouped,
            Map<Long, List<EduCoursePrereq>> graph, Map<Long, EduCourse> catalog, List<Long> selfReferences)
    {
        this.grouped = grouped;
        this.graph = graph;
        this.catalog = catalog;
        this.selfReferences = selfReferences;
    }

    /**
     * 一次性载入的先修关系，按目标课程分组。
     *
     * @return 只读视图：courseId -> 先修行
     */
    public Map<Long, List<EduCoursePrereq>> loadGraph()
    {
        return graph;
    }

    /**
     * 先修条件是否满足（组内 AND、组间 OR）。
     *
     * @param courseId 目标课程 ID
     * @param passedCourseIds 已通过课程 ID 集合
     * @return 满足或该课程没有先修要求时返回 true
     */
    public boolean isSatisfied(Long courseId, Set<Long> passedCourseIds)
    {
        return isSatisfied(grouped, courseId, passedCourseIds);
    }

    /**
     * 返回未满足的“最少阻力路径”上的先修课程：即缺少课程数量最少的那一组里尚未通过的课程。
     *
     * @param courseId 目标课程 ID
     * @param passedCourseIds 已通过课程 ID 集合
     * @return 未满足的先修课程；全部满足或无先修要求时返回空列表
     */
    public List<EduCourse> missingPrerequisites(Long courseId, Set<Long> passedCourseIds)
    {
        List<Long> missingIds = missingPrerequisiteIds(grouped, courseId, passedCourseIds);
        if (missingIds.isEmpty())
        {
            return Collections.emptyList();
        }
        List<EduCourse> courses = new ArrayList<>(missingIds.size());
        for (Long missingId : missingIds)
        {
            courses.add(resolveCourse(missingId));
        }
        return courses;
    }

    /**
     * 环检测：返回参与环的课程 ID（按 ID 升序）；空列表表示数据合法。
     *
     * @return 处于环中的课程 ID
     */
    public List<Long> detectCycles()
    {
        return detectCycles(grouped);
    }

    /**
     * 载入时被丢弃的自依赖课程 ID。
     *
     * @return 自依赖课程 ID（升序）
     */
    public List<Long> detectSelfReferences()
    {
        return selfReferences;
    }

    /**
     * 指定课程是否存在自依赖数据。
     *
     * @param courseId 课程 ID
     * @return 存在自依赖时返回 true
     */
    public boolean hasSelfReference(Long courseId)
    {
        return courseId != null && selfReferences.contains(courseId);
    }

    /**
     * 某一门课程的先修分组（只读）。
     *
     * @param courseId 课程 ID
     * @return groupNo -> prereqCourseIds；无先修要求时返回空 Map
     */
    public Map<Integer, List<Long>> groupsOf(Long courseId)
    {
        Map<Integer, List<Long>> groups = grouped.get(courseId);
        return groups == null ? Collections.<Integer, List<Long>>emptyMap() : Collections.unmodifiableMap(groups);
    }

    // ------------------------------------------------------------------
    // 静态工具方法：只依赖课程 ID，供规则引擎与单元测试复用
    // ------------------------------------------------------------------

    /**
     * 先修条件是否满足。
     *
     * @param grouped 分组边
     * @param courseId 目标课程 ID
     * @param passedCourseIds 已通过课程 ID 集合
     * @return 满足或该课程没有先修要求时返回 true
     */
    public static boolean isSatisfied(Map<Long, Map<Integer, List<Long>>> grouped, Long courseId,
            Set<Long> passedCourseIds)
    {
        if (courseId == null || grouped == null)
        {
            return true;
        }
        Map<Integer, List<Long>> groups = grouped.get(courseId);
        if (groups == null || groups.isEmpty())
        {
            return true;
        }
        for (List<Long> group : orderedGroups(groups))
        {
            if (isGroupSatisfied(group, passedCourseIds))
            {
                return true;
            }
        }
        return false;
    }

    /**
     * “最少阻力路径”上未满足的先修课程 ID。
     *
     * @param grouped 分组边
     * @param courseId 目标课程 ID
     * @param passedCourseIds 已通过课程 ID 集合
     * @return 缺少数量最少的那一组里尚未通过的课程 ID
     */
    public static List<Long> missingPrerequisiteIds(Map<Long, Map<Integer, List<Long>>> grouped, Long courseId,
            Set<Long> passedCourseIds)
    {
        if (courseId == null || grouped == null)
        {
            return EMPTY_IDS;
        }
        Map<Integer, List<Long>> groups = grouped.get(courseId);
        if (groups == null || groups.isEmpty())
        {
            return EMPTY_IDS;
        }
        List<Long> best = null;
        for (List<Long> group : orderedGroups(groups))
        {
            List<Long> missing = new ArrayList<>();
            for (Long prereqId : group)
            {
                if (!isPassed(prereqId, passedCourseIds))
                {
                    missing.add(prereqId);
                }
            }
            if (missing.isEmpty())
            {
                return EMPTY_IDS;
            }
            if (best == null || missing.size() < best.size())
            {
                best = missing;
            }
        }
        return best == null ? EMPTY_IDS : best;
    }

    /**
     * 已完整满足的组（用于生成“且已通过先修课程 CSC1001”这类可解释说明）。
     *
     * @param grouped 分组边
     * @param courseId 目标课程 ID
     * @param passedCourseIds 已通过课程 ID 集合
     * @return 第一个被完整满足的组内的课程 ID；不满足时返回空列表
     */
    public static List<Long> satisfiedGroup(Map<Long, Map<Integer, List<Long>>> grouped, Long courseId,
            Set<Long> passedCourseIds)
    {
        if (courseId == null || grouped == null)
        {
            return EMPTY_IDS;
        }
        Map<Integer, List<Long>> groups = grouped.get(courseId);
        if (groups == null || groups.isEmpty())
        {
            return EMPTY_IDS;
        }
        for (List<Long> group : orderedGroups(groups))
        {
            if (isGroupSatisfied(group, passedCourseIds))
            {
                return group;
            }
        }
        return EMPTY_IDS;
    }

    /**
     * 环检测（迭代实现，Kosaraju 双遍历，不递归，支持超大图）。
     *
     * @param grouped 分组边
     * @return 参与环的课程 ID（升序）；无环时返回空列表
     */
    public static List<Long> detectCycles(Map<Long, Map<Integer, List<Long>>> grouped)
    {
        Map<Long, List<Long>> adjacency = toAdjacency(grouped);
        if (adjacency.isEmpty())
        {
            return Collections.emptyList();
        }
        List<Long> nodes = new ArrayList<>(adjacency.keySet());

        // 第一遍：迭代 DFS 求完成顺序
        Set<Long> visited = new HashSet<>(Math.max(16, nodes.size() * 2));
        List<Long> finishOrder = new ArrayList<>(nodes.size());
        Deque<Frame> stack = new ArrayDeque<>();
        for (Long node : nodes)
        {
            if (!visited.add(node))
            {
                continue;
            }
            stack.push(new Frame(node));
            while (!stack.isEmpty())
            {
                Frame frame = stack.peek();
                List<Long> children = adjacency.getOrDefault(frame.node, EMPTY_IDS);
                if (frame.index < children.size())
                {
                    Long child = children.get(frame.index++);
                    if (visited.add(child))
                    {
                        stack.push(new Frame(child));
                    }
                }
                else
                {
                    finishOrder.add(frame.node);
                    stack.pop();
                }
            }
        }

        // 第二遍：在反向图上按完成顺序逆序 DFS，收集强连通分量
        Map<Long, List<Long>> reversed = reverse(adjacency);
        Set<Long> assigned = new HashSet<>(Math.max(16, nodes.size() * 2));
        Set<Long> cycleNodes = new TreeSet<>();
        Deque<Long> componentStack = new ArrayDeque<>();
        for (int i = finishOrder.size() - 1; i >= 0; i--)
        {
            Long root = finishOrder.get(i);
            if (!assigned.add(root))
            {
                continue;
            }
            List<Long> component = new ArrayList<>();
            componentStack.push(root);
            while (!componentStack.isEmpty())
            {
                Long node = componentStack.pop();
                component.add(node);
                for (Long dependent : reversed.getOrDefault(node, EMPTY_IDS))
                {
                    if (assigned.add(dependent))
                    {
                        componentStack.push(dependent);
                    }
                }
            }
            if (component.size() > 1)
            {
                cycleNodes.addAll(component);
            }
            else
            {
                Long only = component.get(0);
                if (adjacency.getOrDefault(only, EMPTY_IDS).contains(only))
                {
                    cycleNodes.add(only);
                }
            }
        }
        if (cycleNodes.isEmpty())
        {
            return Collections.emptyList();
        }
        return new ArrayList<>(cycleNodes);
    }

    /**
     * 是否存在先修环。
     *
     * @param grouped 分组边
     * @return 有环返回 true
     */
    public static boolean hasCycle(Map<Long, Map<Integer, List<Long>>> grouped)
    {
        return !detectCycles(grouped).isEmpty();
    }

    /**
     * 把“课程 -> 分组边”转换为邻接表：课程 -> 其全部先修课程 ID（含重复组内的边）。
     *
     * @param grouped 分组边
     * @return 邻接表
     */
    public static Map<Long, List<Long>> toAdjacency(Map<Long, Map<Integer, List<Long>>> grouped)
    {
        Map<Long, List<Long>> adjacency = new LinkedHashMap<>();
        if (grouped == null)
        {
            return adjacency;
        }
        for (Map.Entry<Long, Map<Integer, List<Long>>> entry : grouped.entrySet())
        {
            Long courseId = entry.getKey();
            if (courseId == null)
            {
                continue;
            }
            List<Long> children = adjacency.computeIfAbsent(courseId, key -> new ArrayList<>());
            Map<Integer, List<Long>> groups = entry.getValue();
            if (groups == null)
            {
                continue;
            }
            for (List<Long> group : groups.values())
            {
                if (group == null)
                {
                    continue;
                }
                for (Long prereqId : group)
                {
                    if (prereqId == null || prereqId.equals(courseId))
                    {
                        continue;
                    }
                    if (!children.contains(prereqId))
                    {
                        children.add(prereqId);
                    }
                    adjacency.computeIfAbsent(prereqId, key -> new ArrayList<>());
                }
            }
        }
        return adjacency;
    }

    // ------------------------------------------------------------------
    // 内部实现
    // ------------------------------------------------------------------

    /**
     * 按 group_no 升序展开先修组，保证“缺少先修课”提示与判定结果稳定可复现。
     */
    private static List<List<Long>> orderedGroups(Map<Integer, List<Long>> groups)
    {
        List<Integer> groupNos = new ArrayList<>(groups.keySet());
        groupNos.sort(Comparator.nullsFirst(Comparator.naturalOrder()));
        List<List<Long>> ordered = new ArrayList<>(groupNos.size());
        for (Integer groupNo : groupNos)
        {
            ordered.add(groups.get(groupNo));
        }
        return ordered;
    }

    private static boolean isGroupSatisfied(List<Long> group, Set<Long> passedCourseIds)
    {
        if (group == null || group.isEmpty())
        {
            return true;
        }
        for (Long prereqId : group)
        {
            if (!isPassed(prereqId, passedCourseIds))
            {
                return false;
            }
        }
        return true;
    }

    private static boolean isPassed(Long courseId, Set<Long> passedCourseIds)
    {
        return courseId != null && passedCourseIds != null && passedCourseIds.contains(courseId);
    }

    private EduCourse resolveCourse(Long courseId)
    {
        EduCourse course = catalog.get(courseId);
        if (course != null)
        {
            return course;
        }
        EduCourse placeholder = new EduCourse();
        placeholder.setCourseId(courseId);
        return placeholder;
    }

    private static Map<Long, EduCourse> indexCatalog(Collection<EduCourse> catalog)
    {
        Map<Long, EduCourse> index = new LinkedHashMap<>();
        if (catalog == null)
        {
            return index;
        }
        for (EduCourse course : catalog)
        {
            if (course != null && course.getCourseId() != null)
            {
                index.put(course.getCourseId(), course);
            }
        }
        return index;
    }

    /**
     * 把先修行按目标课程归并，保持同类内的入库顺序；自依赖行在此处丢弃。
     */
    private static Map<Long, List<EduCoursePrereq>> normalizeRows(Map<Long, List<EduCoursePrereq>> graph)
    {
        if (graph == null || graph.isEmpty())
        {
            return Collections.emptyMap();
        }
        Map<Long, List<EduCoursePrereq>> rows = new LinkedHashMap<>();
        for (Map.Entry<Long, List<EduCoursePrereq>> entry : graph.entrySet())
        {
            Long courseId = entry.getKey();
            List<EduCoursePrereq> source = entry.getValue() == null ? EMPTY_ROWS : entry.getValue();
            List<EduCoursePrereq> kept = new ArrayList<>(source.size());
            for (EduCoursePrereq row : source)
            {
                if (row == null || row.getPrereqCourseId() == null)
                {
                    continue;
                }
                Long rowCourseId = row.getCourseId() == null ? courseId : row.getCourseId();
                if (rowCourseId == null || rowCourseId.equals(row.getPrereqCourseId()))
                {
                    // 自依赖非法：DDL 已有 check，此处再兜底一次
                    continue;
                }
                kept.add(row);
            }
            if (!kept.isEmpty())
            {
                rows.put(courseId, Collections.unmodifiableList(kept));
            }
        }
        return Collections.unmodifiableMap(rows);
    }

    /**
     * 把先修行按 group_no 分组，得到 courseId -> groupNo -> prereqCourseIds。
     */
    private static Map<Long, Map<Integer, List<Long>>> groupRows(Map<Long, List<EduCoursePrereq>> rows)
    {
        Map<Long, Map<Integer, List<Long>>> result = new LinkedHashMap<>();
        for (Map.Entry<Long, List<EduCoursePrereq>> entry : rows.entrySet())
        {
            Long courseId = entry.getKey();
            if (courseId == null)
            {
                continue;
            }
            Map<Integer, List<Long>> groups = new LinkedHashMap<>();
            for (EduCoursePrereq row : entry.getValue())
            {
                if (row == null || row.getPrereqCourseId() == null || row.getPrereqCourseId().equals(courseId))
                {
                    continue;
                }
                // group_no 为 int not null default 1；兼容 Integer / int 两种实体写法
                Object rawGroupNo = row.getGroupNo();
                int groupNo = rawGroupNo instanceof Number ? ((Number) rawGroupNo).intValue() : 1;
                List<Long> prereqIds = groups.computeIfAbsent(groupNo, key -> new ArrayList<>());
                if (!prereqIds.contains(row.getPrereqCourseId()))
                {
                    prereqIds.add(row.getPrereqCourseId());
                }
            }
            if (!groups.isEmpty())
            {
                result.put(courseId, groups);
            }
        }
        return result;
    }

    /**
     * 归一化“已分组边”：去空、去重、丢弃自依赖。
     */
    private static Map<Long, Map<Integer, List<Long>>> normalizeGrouped(Map<Long, Map<Integer, List<Long>>> grouped)
    {
        if (grouped == null || grouped.isEmpty())
        {
            return Collections.emptyMap();
        }
        Map<Long, Map<Integer, List<Long>>> result = new LinkedHashMap<>();
        for (Map.Entry<Long, Map<Integer, List<Long>>> entry : grouped.entrySet())
        {
            Long courseId = entry.getKey();
            if (courseId == null || entry.getValue() == null)
            {
                continue;
            }
            Map<Integer, List<Long>> groups = new LinkedHashMap<>();
            for (Map.Entry<Integer, List<Long>> groupEntry : entry.getValue().entrySet())
            {
                Integer groupNo = groupEntry.getKey() == null ? Integer.valueOf(1) : groupEntry.getKey();
                List<Long> kept = new ArrayList<>();
                if (groupEntry.getValue() != null)
                {
                    for (Long prereqId : groupEntry.getValue())
                    {
                        if (prereqId == null || prereqId.equals(courseId) || kept.contains(prereqId))
                        {
                            continue;
                        }
                        kept.add(prereqId);
                    }
                }
                if (!kept.isEmpty())
                {
                    groups.put(groupNo, kept);
                }
            }
            if (!groups.isEmpty())
            {
                result.put(courseId, groups);
            }
        }
        return result;
    }

    private static List<Long> collectSelfReferences(Map<Long, List<EduCoursePrereq>> graph)
    {
        if (graph == null || graph.isEmpty())
        {
            return EMPTY_IDS;
        }
        Set<Long> references = new LinkedHashSet<>();
        for (Map.Entry<Long, List<EduCoursePrereq>> entry : graph.entrySet())
        {
            if (entry.getValue() == null)
            {
                continue;
            }
            for (EduCoursePrereq row : entry.getValue())
            {
                if (row == null || row.getCourseId() == null || row.getPrereqCourseId() == null)
                {
                    continue;
                }
                if (row.getCourseId().equals(row.getPrereqCourseId()))
                {
                    references.add(row.getCourseId());
                }
            }
        }
        return references.isEmpty() ? EMPTY_IDS : Collections.unmodifiableList(new ArrayList<>(references));
    }

    private static Map<Long, List<Long>> reverse(Map<Long, List<Long>> adjacency)
    {
        Map<Long, List<Long>> reversed = new LinkedHashMap<>();
        for (Long node : adjacency.keySet())
        {
            reversed.computeIfAbsent(node, key -> new ArrayList<>());
        }
        for (Map.Entry<Long, List<Long>> entry : adjacency.entrySet())
        {
            for (Long child : entry.getValue())
            {
                reversed.computeIfAbsent(child, key -> new ArrayList<>()).add(entry.getKey());
            }
        }
        return reversed;
    }

    /**
     * 迭代 DFS 的显式栈帧，避免递归导致 StackOverflowError。
     */
    private static final class Frame
    {
        private final Long node;

        private int index;

        private Frame(Long node)
        {
            this.node = node;
        }
    }
}
