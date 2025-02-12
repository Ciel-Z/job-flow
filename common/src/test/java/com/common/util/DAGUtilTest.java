package com.common.util;

import com.common.dag.JobFlowDAG;
import com.common.dag.NodeEdgeDAG;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

class DAGUtilTest {

    @Test
    void convertFromNodeEdgeDAG() throws JsonProcessingException {
        // 1. 构造点—边形式的 DAG (NodeEdgeDAG)
        // 构造节点
        NodeEdgeDAG.Node node1 = new NodeEdgeDAG.Node(1L).setJobId(100L).setNodeName("Task1");
        NodeEdgeDAG.Node node2 = new NodeEdgeDAG.Node(2L).setJobId(100L).setNodeName("Task2");
        NodeEdgeDAG.Node node3 = new NodeEdgeDAG.Node(3L).setJobId(100L).setNodeName("Task3");

        List<NodeEdgeDAG.Node> nodes = Arrays.asList(node1, node2, node3);

        // 构造边：1 -> 2, 1 -> 3, 2 -> 3
        NodeEdgeDAG.Edge edge1 = new NodeEdgeDAG.Edge(1L, 2L);
        NodeEdgeDAG.Edge edge2 = new NodeEdgeDAG.Edge(1L, 3L);
        NodeEdgeDAG.Edge edge3 = new NodeEdgeDAG.Edge(2L, 3L);
        List<NodeEdgeDAG.Edge> edges = Arrays.asList(edge1, edge2, edge3);

        NodeEdgeDAG nodeEdgeDAG = new NodeEdgeDAG(nodes, edges);

        System.out.println("==== 原始 NodeEdgeDAG ====");
        printNodeEdgeDAG(nodeEdgeDAG);

        // 2. 转换为 JobFlowDAG（前驱/后继形式）
        JobFlowDAG jobFlowDAG = DAGUtil.convert(nodeEdgeDAG);
        System.out.println("\n==== 转换后的 JobFlowDAG ====");
        printJobFlowDAG(jobFlowDAG);

        // 3. 再将 JobFlowDAG 转换回 NodeEdgeDAG
        NodeEdgeDAG convertedBack = DAGUtil.convert(jobFlowDAG);
        System.out.println("\n==== 转换回的 NodeEdgeDAG ====");
        printNodeEdgeDAG(convertedBack);

    }

    @Test
    void convertToNodeEdgeDAG() {
    }


    /**
     * 打印 NodeEdgeDAG 信息
     */
    private static void printNodeEdgeDAG(NodeEdgeDAG dag) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            mapper.enable(SerializationFeature.INDENT_OUTPUT);
            String json = mapper.writeValueAsString(dag);
            System.out.println(json);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 打印 JobFlowDAG 的各节点依赖和后继关系
     */
    private static void printJobFlowDAG(JobFlowDAG dag) {
        Map<Long, JobFlowDAG.Node> nodeMap = dag.getNodeMap();
        if (nodeMap == null || nodeMap.isEmpty()) {
            System.out.println("JobFlowDAG 节点为空！");
            return;
        }
        for (JobFlowDAG.Node node : nodeMap.values()) {
            StringBuilder sb = new StringBuilder();
            sb.append("节点 id = ").append(node.getNodeId());
            sb.append("，依赖：[");
            if (node.getDependencies() != null) {
                node.getDependencies().forEach(dep -> sb.append(dep.getNodeId()).append(" "));
            }
            sb.append("]，后继：[");
            if (node.getSuccessors() != null) {
                node.getSuccessors().forEach(suc -> sb.append(suc.getNodeId()).append(" "));
            }
            sb.append("]");
            System.out.println(sb.toString());
        }

        // 打印根节点信息
        if (dag.getRoots() != null) {
            System.out.print("根节点：");
            dag.getRoots().forEach(root -> System.out.print(root.getNodeId() + " "));
            System.out.println();
        }
    }
}