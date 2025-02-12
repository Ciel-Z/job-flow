package com.common.dag;

import com.common.enums.JobStatusEnum;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.google.common.collect.Lists;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Points & edges for DAG, making it easier to describe or transfer.
 *
 * @author tjq
 * @since 2020/5/26
 */
@Data
public class NodeEdgeDAG {

    /**
     * Nodes of DAG diagram.
     */
    private List<Node> nodes;
    /**
     * Edges of DAG diagram.
     */
    private List<Edge> edges;

    /**
     * Point.
     */
    @Data
    @Accessors(chain = true)
    @NoArgsConstructor
    public static class Node implements Serializable {

        /**
         * node id
         *
         * @since 20210128
         */
        private Long nodeId;
        /* Instance running param, which is not required by DAG. */

        /**
         * node name
         */
        private String nodeName;

        /**
         * job id or workflow id (if this Node type is a nested workflow)
         */
        private Long jobId;

        /**
         * job name or workflow name (if this Node type is a nested workflow)
         */
        private String jobName;


        @JsonSerialize(using = ToStringSerializer.class)
        private Long instanceId;

        /**
         * worker ip
         */
        private String workerAddress;

        /**
         * for decision node, it is JavaScript code
         */
        private String params;

        private Integer status;
        /**
         * for decision node, it only be can "true" or "false"
         */
        private String result;

        private LocalDateTime startTime;

        private LocalDateTime endTime;

        public Node(Long nodeId) {
            this.nodeId = nodeId;
        }

        public String errorMassage() {
            if (!JobStatusEnum.FAIL.getCode().equals(status) || result == null || result.isEmpty()) {
                return null;
            }
            return String.format("[%s] 节点出现异常, 错误信息为：%s", jobName, result);
        }
    }

    /**
     * Edge formed by two node ids.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Edge implements Serializable {

        private Long from;

        private Long to;

        /**
         * property,support for complex flow control
         * for decision node , it can be "true" or "false"
         */
        private String property;

        public Edge(long from, long to) {
            this.from = from;
            this.to = to;
        }
    }

    public NodeEdgeDAG(@Nonnull List<Node> nodes, @Nullable List<Edge> edges) {
        this.nodes = nodes;
        this.edges = edges == null ? Lists.newLinkedList() : edges;
    }

    public NodeEdgeDAG() {
        this.nodes = Lists.newLinkedList();
        this.edges = Lists.newLinkedList();
    }
}
