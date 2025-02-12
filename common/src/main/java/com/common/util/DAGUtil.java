package com.common.util;

import com.common.dag.JobFlowDAG;
import com.common.dag.NodeEdgeDAG;
import com.common.enums.JobStatusEnum;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

@Slf4j
public class DAGUtil {

    /**
     * 将点—边形式的 NodeEdgeDAG 转换为 JobFlowDAG 表示。
     * <p>
     * 转换思路：
     * <ol>
     *     <li>遍历 NodeEdgeDAG.nodes，针对每个 NodeEdgeDAG.Node 创建一个 JobFlowDAG.Node 对象，
     *         并建立以 nodeId 为键的映射（nodeMap）。</li>
     *     <li>遍历 NodeEdgeDAG.edges，对于每个边，取出 from 与 to 的 JobFlowDAG.Node，
     *         将 to 节点加入 from 节点的后继列表及后继边映射，同时将 from 节点加入 to 节点的依赖列表及依赖边映射。</li>
     *     <li>最后扫描所有节点，将依赖列表为空的节点作为根节点填入 JobFlowDAG.roots。</li>
     * </ol>
     *
     * @param nodeEdgeDag 点边形式的 DAG
     * @return 前驱/后继形式的 DAG
     */
    public static JobFlowDAG convert(@NonNull NodeEdgeDAG nodeEdgeDag) {
        JobFlowDAG jobFlowDAG = new JobFlowDAG();
        Map<Long, JobFlowDAG.Node> nodeMap = new HashMap<>();

        // 1. 根据所有 NodeEdgeDAG.Node 构造 JobFlowDAG.Node 并建立映射
        for (NodeEdgeDAG.Node edgeNode : nodeEdgeDag.getNodes()) {
            JobFlowDAG.Node jobFlowNode = new JobFlowDAG.Node(edgeNode);
            // nodeId 已在构造函数中赋值，如有需要可再次设置
            nodeMap.put(edgeNode.getNodeId(), jobFlowNode);
        }

        // 2. 根据边信息构建依赖/后继关系
        if (nodeEdgeDag.getEdges() != null) {
            for (NodeEdgeDAG.Edge edge : nodeEdgeDag.getEdges()) {
                Long fromId = edge.getFrom();
                Long toId = edge.getTo();
                JobFlowDAG.Node fromNode = nodeMap.get(fromId);
                JobFlowDAG.Node toNode = nodeMap.get(toId);
                if (fromNode != null && toNode != null) {
                    // 建立 from -> to 的后继关系
                    fromNode.getSuccessors().add(toNode);
                    // 建立 to 的依赖关系
                    toNode.getDependencies().add(fromNode);
                }
            }
        }

        // 3. 找出所有无依赖的节点作为根节点
        List<JobFlowDAG.Node> roots = new ArrayList<>();
        for (JobFlowDAG.Node node : nodeMap.values()) {
            if (node.getDependencies() == null || node.getDependencies().isEmpty()) {
                roots.add(node);
            }
        }
        jobFlowDAG.setRoots(roots);
        jobFlowDAG.setNodeMap(nodeMap);

        return jobFlowDAG;
    }


    /**
     * 将 JobFlowDAG 转换为点—边形式的 NodeEdgeDAG。
     * <p>
     * 转换思路：
     * <ol>
     *     <li>遍历 JobFlowDAG.nodeMap 中所有节点，收集各节点中封装的 NodeEdgeDAG.Node 对象（根据 nodeId 保证唯一性）。</li>
     *     <li>遍历每个 JobFlowDAG.Node 的 successorEdgeMap，将所有边收集到边集合中，
     *         同时避免重复添加（可利用 List.contains 判断或借助 Set）。</li>
     *     <li>构造 NodeEdgeDAG 对象返回。</li>
     * </ol>
     *
     * @param jobFlowDAG 前驱/后继形式的 DAG
     * @return 点—边形式的 DAG
     */
    public static NodeEdgeDAG convert(@NonNull JobFlowDAG jobFlowDAG) {
        Map<Long, NodeEdgeDAG.Node> nodeMap = new HashMap<>();
        List<NodeEdgeDAG.Node> nodeList = new ArrayList<>();
        List<NodeEdgeDAG.Edge> edgeList = new ArrayList<>();

        if (jobFlowDAG.getNodeMap() != null) {
            for (JobFlowDAG.Node jobNode : jobFlowDAG.getNodeMap().values()) {
                // 收集节点
                NodeEdgeDAG.Node edgeNode = jobNode.getNode();
                if (edgeNode != null && !nodeMap.containsKey(edgeNode.getNodeId())) {
                    nodeMap.put(edgeNode.getNodeId(), edgeNode);
                    nodeList.add(edgeNode);
                }
            }
        }

        return new NodeEdgeDAG(nodeList, edgeList);
    }


    /**
     * 验证 DAG 是否合法（无环）
     *
     * @param dag DAG 对象
     * @return true：合法；false：不合法
     */
    public static boolean validate(NodeEdgeDAG dag) {
        if (dag == null || dag.getNodes() == null || dag.getNodes().isEmpty()) {
            return false;
        }

        // 验证节点非空且 nodeId 唯一
        List<NodeEdgeDAG.Node> nodes = dag.getNodes();
        Set<Long> nodeIds = new HashSet<>();
        for (NodeEdgeDAG.Node node : nodes) {
            if (node == null || node.getNodeId() == null || !nodeIds.add(node.getNodeId())) {
                return false;
            }
        }

        // 验证边有效性
        List<NodeEdgeDAG.Edge> edges = dag.getEdges();
        if (edges != null) {
            for (NodeEdgeDAG.Edge edge : edges) {
                if (edge == null || edge.getFrom() == null || edge.getTo() == null) {
                    return false;
                }
                if (!nodeIds.contains(edge.getFrom())) {
                    // 前驱节点不存在
                    return false;
                }
                if (!nodeIds.contains(edge.getTo())) {
                    // 后继节点不存在
                    return false;
                }
            }
        }

        // 无边亦无环
        if (edges == null || edges.isEmpty()) {
            return true;
        }

        // 验证无环性, 入度统计
        Map<Long, List<Long>> graph = new HashMap<>();
        Map<Long, Integer> inDegree = new HashMap<>();
        for (Long id : nodeIds) {
            graph.put(id, new ArrayList<>());
            inDegree.put(id, 0);
        }
        for (NodeEdgeDAG.Edge edge : edges) {
            // 添加边：from -> to
            graph.get(edge.getFrom()).add(edge.getTo());
            inDegree.put(edge.getTo(), inDegree.get(edge.getTo()) + 1);
        }
        // 拓扑排序
        Queue<Long> queue = new LinkedList<>();
        for (Map.Entry<Long, Integer> entry : inDegree.entrySet()) {
            if (entry.getValue() == 0) {
                queue.offer(entry.getKey());
            }
        }
        int count = 0;
        while (!queue.isEmpty()) {
            Long cur = queue.poll();
            count++;
            for (Long neighbor : graph.get(cur)) {
                inDegree.put(neighbor, inDegree.get(neighbor) - 1);
                if (inDegree.get(neighbor) == 0) {
                    queue.offer(neighbor);
                }
            }
        }
        return count == nodeIds.size();
    }


    public static String toJSONString(JobFlowDAG dag) {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            objectMapper.registerModule(new JavaTimeModule());
            return objectMapper.writeValueAsString(dag);
        } catch (Exception e) {
            log.error("Error converting JobFlowDAG to JSON string", e);
            return "{}";
        }
    }


    public static JobFlowDAG fromJSONString(String jsonString) {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            objectMapper.registerModule(new JavaTimeModule());
            return objectMapper.readValue(jsonString, JobFlowDAG.class);
        } catch (Exception e) {
            log.error("Error converting JSON string to JobFlowDAG", e);
            return new JobFlowDAG();
        }
    }

    /**
     * 获取 DAG 中所有就绪的任务列表。一个任务就绪的条件是：所有依赖的任务都已成功。
     *
     * @param nodeMap 所有节点的映射（nodeId -> Node）
     * @return 就绪任务列表
     */
    public static List<NodeEdgeDAG.Node> getReadyNodes(Map<Long, JobFlowDAG.Node> nodeMap, Long nodeId) {
        List<NodeEdgeDAG.Node> readyNodes = new ArrayList<>();
        JobFlowDAG.Node jobNode = nodeMap.get(nodeId);
        for (JobFlowDAG.Node node : jobNode.getSuccessors()) {
            if (node.getNode().getStatus() != null) {// 非空其他前驱节点已触发
                continue;
            }
            for (JobFlowDAG.Node dependency : node.getDependencies()) {
                if (!JobStatusEnum.SUCCESS.getCode().equals(dependency.getNode().getStatus())) {
                    break;
                }
            }
            readyNodes.add(node.getNode());
        }
        return readyNodes;
    }


    /**
     * 判断 DAG 中是否所有任务都已经完成。
     *
     * @param jobFlowDAG DAG 对象
     * @return true 如果所有节点都完成，false 否则
     */
    public static boolean areAllTasksCompleted(@NonNull JobFlowDAG jobFlowDAG) {
        // 检查每个节点的状态，判断是否所有任务都已完成（成功或失败）
        for (JobFlowDAG.Node node : jobFlowDAG.getNodeMap().values()) {
            if (!JobStatusEnum.SUCCESS.getCode().equals(node.getNode().getStatus())) {
                return false;
            }
        }
        return true;
    }

    /**
     * 从指定起始节点开始，采用广度优先遍历获取所有后继节点，
     * 包括直接或间接的后继节点，放入一个 Set 中。
     *
     * @param startNode 重试的起始节点
     * @return 包含 startNode 及其所有后继节点的 Set
     */
    public static Set<JobFlowDAG.Node> getAllSuccessor(JobFlowDAG.Node startNode) {
        Set<JobFlowDAG.Node> affectedNodes = new HashSet<>();
        if (startNode == null) {
            return affectedNodes;
        }
        Queue<JobFlowDAG.Node> queue = new LinkedList<>();
        queue.offer(startNode);
        affectedNodes.add(startNode);

        while (!queue.isEmpty()) {
            JobFlowDAG.Node current = queue.poll();
            if (current.getSuccessors() != null) {
                for (JobFlowDAG.Node succ : current.getSuccessors()) {
                    if (!affectedNodes.contains(succ)) {
                        affectedNodes.add(succ);
                        queue.offer(succ);
                    }
                }
            }
        }
        return affectedNodes;
    }


    /**
     * 在全局节点集合中（如 DAG 的 nodeMap），遍历每个节点，
     * 如果该节点在受影响节点集中且其状态不是 SUCCESS，则重置为 PENDING。
     *
     * @param dag       DAG 对象
     * @param startNode 重试起始节点
     */
    public static boolean retryOverride(JobFlowDAG dag, JobFlowDAG.Node startNode) {
        // 收集受影响节点（从 startNode 出发的所有后继）
        Set<JobFlowDAG.Node> successors = getAllSuccessor(startNode);
        Set<Integer> statuses = Set.of(JobStatusEnum.RUNNING.getCode(), JobStatusEnum.PAUSE.getCode(), JobStatusEnum.FAIL.getCode());
        // 全局遍历 DAG 中的节点，针对在受影响集合中的节点重置状态
        for (JobFlowDAG.Node node : dag.getNodeMap().values()) {
            NodeEdgeDAG.Node edgeNode = node.getNode();
            if (edgeNode.getStatus() == null || statuses.contains(edgeNode.getStatus())) {
                if (!successors.contains(node)) {
                    return false;
                }
            }
        }
        return true;
    }


    /**
     * 重置从指定节点开始的子树状态
     *
     * @param startNode 起始节点
     */
    public static void resetSubStatus(JobFlowDAG.Node startNode) {
        if (startNode == null) {
            return;
        }
        // 采用广度优先遍历（BFS）方式
        Set<JobFlowDAG.Node> visited = new HashSet<>();
        Queue<JobFlowDAG.Node> queue = new LinkedList<>();
        queue.offer(startNode);
        visited.add(startNode);

        while (!queue.isEmpty()) {
            JobFlowDAG.Node current = queue.poll();
            initNode(current.getNode());

            // 将当前节点的所有后继节点加入队列
            if (current.getSuccessors() != null) {
                for (JobFlowDAG.Node successor : current.getSuccessors()) {
                    if (!visited.contains(successor)) {
                        visited.add(successor);
                        queue.offer(successor);
                    }
                }
            }
        }
    }


    /**
     * 初始化节点状态
     *
     * @param node 节点
     */
    public static void initNode(NodeEdgeDAG.Node node) {
        node.setStatus(null);
        node.setInstanceId(null);
        node.setStartTime(null);
        node.setEndTime(null);
        node.setResult(null);
        node.setWorkerAddress(null);
    }
}
