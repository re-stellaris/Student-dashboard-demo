package com.ruoyi.education;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import com.ruoyi.education.common.PrerequisiteGraph;

/**
 * 先修图语义测试：组内 AND、组间 OR、最少阻力路径、自依赖、环检测。
 *
 * <p>测试只使用“已分组边”（{@code courseId -> groupNo -> prereqCourseIds}）构造图，
 * 不依赖课程域实体、Spring 容器或数据库。</p>
 *
 * @author ruoyi
 */
class PrerequisiteGraphTest
{
    private static final long CSC3100 = 1000L;

    private static final long CSC1001 = 1001L;

    private static final long CSC1002 = 1002L;

    private static final long CSC2001 = 2001L;

    @DisplayName("组内 AND：同组先修课程必须全部通过")
    @Test
    void andWithinGroup()
    {
        // CSC3100 需要 (CSC1001 & CSC1002)
        PrerequisiteGraph graph = graphOf(CSC3100, 1, CSC1001, CSC1002);

        assertFalse(graph.isSatisfied(CSC3100, Set.of()), "未修任何先修课时应不满足");
        assertFalse(graph.isSatisfied(CSC3100, Set.of(CSC1001)), "缺同组另一门先修课时应不满足");
        assertTrue(graph.isSatisfied(CSC3100, Set.of(CSC1001, CSC1002)), "同组全部通过后应满足");
        assertTrue(graph.isSatisfied(CSC3100, Set.of(CSC1001, CSC1002, CSC2001)), "多余课程不影响判定");
    }

    @DisplayName("组间 OR：任一先修组被完整满足即可")
    @Test
    void orAcrossGroups()
    {
        // CSC3100 需要 (CSC1001 & CSC1002) or CSC2001
        PrerequisiteGraph graph = PrerequisiteGraph.fromGrouped(grouped(
                CSC3100, Map.of(1, List.of(CSC1001, CSC1002), 2, List.of(CSC2001))));

        assertFalse(graph.isSatisfied(CSC3100, Set.of(CSC1001)), "仅满足组内一半且未满足另一组");
        assertTrue(graph.isSatisfied(CSC3100, Set.of(CSC2001)), "第二个先修组完整满足即可");
        assertTrue(graph.isSatisfied(CSC3100, Set.of(CSC1001, CSC1002)), "第一个先修组完整满足即可");
    }

    @DisplayName("未满足：返回最少阻力路径（缺少课程最少的那一组）")
    @Test
    void missingPrerequisitesOnMinimalPath()
    {
        // CSC3100 需要 (CSC1001 & CSC1002) or CSC2001
        Map<Long, Map<Integer, List<Long>>> grouped = grouped(
                CSC3100, Map.of(1, List.of(CSC1001, CSC1002), 2, List.of(CSC2001)));

        // 两组都没有满足：组 1 缺 2 门，组 2 缺 1 门 → 取组 2
        assertEquals(List.of(CSC2001), PrerequisiteGraph.missingPrerequisiteIds(grouped, CSC3100, Set.of()));

        // 只差 CSC1002：组 1 缺 1 门 → 取组 1
        assertEquals(List.of(CSC1002), PrerequisiteGraph.missingPrerequisiteIds(grouped, CSC3100, Set.of(CSC1001)));

        // 任一组满足后不再缺课
        assertEquals(Collections.emptyList(),
                PrerequisiteGraph.missingPrerequisiteIds(grouped, CSC3100, Set.of(CSC2001)));

        // 已完整满足的组用于生成“且已通过先修课程 CSC2001”
        assertEquals(List.of(CSC2001), PrerequisiteGraph.satisfiedGroup(grouped, CSC3100, Set.of(CSC2001)));
    }

    @DisplayName("没有先修要求的课程直接视为满足")
    @Test
    void courseWithoutPrerequisiteIsSatisfied()
    {
        PrerequisiteGraph graph = graphOf(CSC3100, 1, CSC1001);

        assertTrue(graph.isSatisfied(CSC2001, Set.of()));
        assertTrue(graph.groupsOf(CSC2001).isEmpty());
        assertEquals(Collections.emptyList(), PrerequisiteGraph.missingPrerequisiteIds(
                Map.of(CSC3100, Map.of(1, List.of(CSC1001))), CSC2001, Set.of()));
    }

    @DisplayName("自依赖非法：载入时丢弃，且不产生环")
    @Test
    void selfReferenceIsRejected()
    {
        // CSC3100 依赖自身，同时依赖 CSC1001
        Map<Long, Map<Integer, List<Long>>> grouped = grouped(
                CSC3100, Map.of(1, List.of(CSC3100, CSC1001)));

        PrerequisiteGraph graph = PrerequisiteGraph.fromGrouped(grouped);

        assertEquals(List.of(CSC1001), graph.groupsOf(CSC3100).get(1), "自依赖边应被丢弃");
        assertFalse(graph.isSatisfied(CSC3100, Set.of()), "仍应按 CSC1001 判定");
        assertTrue(graph.isSatisfied(CSC3100, Set.of(CSC1001)));
        // 只剩自依赖时等同于没有先修要求
        PrerequisiteGraph selfOnly = PrerequisiteGraph.fromGrouped(grouped(CSC3100, Map.of(1, List.of(CSC3100))));
        assertTrue(selfOnly.groupsOf(CSC3100).isEmpty());
        assertEquals(Collections.emptyList(), selfOnly.detectCycles());
        assertTrue(selfOnly.isSatisfied(CSC3100, Set.of()));
    }

    @DisplayName("环检测：3 节点环只返回环内节点")
    @Test
    void threeNodeCycle()
    {
        // CSC1001 -> CSC1002 -> CSC2001 -> CSC1001
        Map<Long, Map<Integer, List<Long>>> grouped = new LinkedHashMap<>();
        grouped.put(CSC1001, Map.of(1, List.of(CSC1002)));
        grouped.put(CSC1002, Map.of(1, List.of(CSC2001)));
        grouped.put(CSC2001, Map.of(1, List.of(CSC1001)));

        List<Long> cycles = PrerequisiteGraph.fromGrouped(grouped).detectCycles();

        assertEquals(Arrays.asList(CSC1001, CSC1002, CSC2001), cycles);
    }

    @DisplayName("环检测：无环链路返回空")
    @Test
    void acyclicChainHasNoCycle()
    {
        // CSC2001 -> CSC1002 -> CSC1001（无环），另有旁支 CSC3100 -> CSC1001
        Map<Long, Map<Integer, List<Long>>> grouped = new LinkedHashMap<>();
        grouped.put(CSC2001, Map.of(1, List.of(CSC1002)));
        grouped.put(CSC1002, Map.of(1, List.of(CSC1001)));
        grouped.put(CSC3100, Map.of(1, List.of(CSC1001)));

        assertEquals(Collections.emptyList(), PrerequisiteGraph.fromGrouped(grouped).detectCycles());
        assertFalse(PrerequisiteGraph.hasCycle(grouped));
    }

    @DisplayName("环检测：环外的前置节点不被误报为环内节点")
    @Test
    void cycleDetectionExcludesUpstreamNodes()
    {
        // 环 1001 <-> 1002，且 1001 需要 2001（环外）
        Map<Long, Map<Integer, List<Long>>> grouped = new LinkedHashMap<>();
        grouped.put(CSC1001, Map.of(1, List.of(CSC1002, CSC2001)));
        grouped.put(CSC1002, Map.of(1, List.of(CSC1001)));

        assertEquals(Arrays.asList(CSC1001, CSC1002), PrerequisiteGraph.fromGrouped(grouped).detectCycles());
    }

    @DisplayName("环检测：5000 节点深链不栈溢出，且能识别整链成环")
    @Test
    void deepChainDoesNotOverflow()
    {
        final int size = 5000;
        Map<Long, Map<Integer, List<Long>>> chain = new LinkedHashMap<>();
        for (long i = 0; i < size; i++)
        {
            chain.put(i, Map.of(1, List.of((i + 1) % size)));
        }

        // 首尾相连 → 整条链是一个 5000 节点的大环，必须迭代实现才不会 StackOverflow
        List<Long> cycles = assertDoesNotThrow(() -> PrerequisiteGraph.fromGrouped(chain).detectCycles());
        assertEquals(size, cycles.size());
        assertEquals(0L, cycles.get(0).longValue());
        assertEquals(size - 1L, cycles.get(size - 1).longValue());

        // 断开最后一环 → 退化为普通长链，无环
        chain.put(size - 1L, Map.of(1, List.of()));
        List<Long> noCycle = assertDoesNotThrow(() -> PrerequisiteGraph.fromGrouped(chain).detectCycles());
        assertTrue(noCycle.isEmpty());
    }

    @DisplayName("空图与空入参安全")
    @Test
    void emptyGraphIsSafe()
    {
        PrerequisiteGraph graph = PrerequisiteGraph.fromGrouped(null);

        assertTrue(graph.loadGraph().isEmpty());
        assertTrue(graph.isSatisfied(CSC1001, null));
        assertEquals(Collections.emptyList(), graph.detectCycles());
        assertEquals(Collections.emptyList(), PrerequisiteGraph.missingPrerequisiteIds(null, CSC1001, Set.of()));
    }

    // ------------------------------------------------------------------
    // 测试夹具
    // ------------------------------------------------------------------

    private static PrerequisiteGraph graphOf(long courseId, int groupNo, Long... prereqIds)
    {
        return PrerequisiteGraph.fromGrouped(grouped(courseId, Map.of(groupNo, Arrays.asList(prereqIds))));
    }

    private static Map<Long, Map<Integer, List<Long>>> grouped(long courseId, Map<Integer, List<Long>> groups)
    {
        Map<Long, Map<Integer, List<Long>>> grouped = new LinkedHashMap<>();
        grouped.put(Long.valueOf(courseId), new LinkedHashMap<>(groups));
        return grouped;
    }
}
