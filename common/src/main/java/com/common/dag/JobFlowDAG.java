package com.common.dag;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import com.google.common.collect.Lists;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * DAG 工作流对象
 * 节点中均记录了上游以及下游的连接关系(无法使用 JSON 来序列化以及反序列化)
 *
 * @author tjq
 * @author Echo009
 * @since 2020/5/26
 */
@Data
public class JobFlowDAG {

    private List<Node> roots;

    private Map<Long, Node> nodeMap;

    public JobFlowDAG() {
        this.roots = new ArrayList<>();
        this.nodeMap = new HashMap<>();
    }

    public Node getNode(Long nodeId) {
        if (nodeMap == null) {
            return null;
        }
        return nodeMap.get(nodeId);
    }

    @Data
    @JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "nodeId")
    @EqualsAndHashCode(exclude = {"dependencies", "node", "successors"})
    @ToString(exclude = {"dependencies", "node", "successors"})
    @NoArgsConstructor
    public static final class Node {

        public Node(NodeEdgeDAG.Node node) {
            this.nodeId = node.getNodeId();
            this.node = node;
            this.dependencies = Lists.newLinkedList();
            this.successors = Lists.newLinkedList();
        }

        /**
         * node id
         */
        private Long nodeId;

        private NodeEdgeDAG.Node node;

        /**
         * 依赖的上游节点
         */
        private List<Node> dependencies;
        /**
         * 后继者，子节点
         */
        private List<Node> successors;
    }
}