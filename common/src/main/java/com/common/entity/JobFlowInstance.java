package com.common.entity;

import com.alibaba.fastjson2.JSON;
import com.common.dag.JobFlowDAG;
import com.common.dag.NodeEdgeDAG;
import com.common.util.DAGUtil;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 工作流实例表
 * @TableName t_job_flow_instance
 */
@Data
public class JobFlowInstance {
    /**
     * ID
     */
    private Long id;

    /**
     * 工作流 ID
     */
    private Long flowId;

    /**
     * 版本 (停止|重试时增加版本, 防止出现调度混乱)
     */
    private Integer version;

    /**
     * 任务流名称
     */
    private String name;

    /**
     * DAG
     */
    private String dag;

    /**
     * 执行状态（0-待执行 1-运行中 2-失败 3-成功 4-暂停）
     */
    private Integer status;

    /**
     * 结果
     */
    private String result;

    /**
     * 工作流级别参数
     */
    private String params;

    /**
     * 触发时间
     */
    private LocalDateTime triggerTime;

    /**
     * 结束时间
     */
    private LocalDateTime endTime;

    /**
     * 创建时间
     */
    private LocalDateTime createdDate;

    /**
     * 更新时间
     */
    private LocalDateTime updatedDate;


    public NodeEdgeDAG getNodeEdgeDAG() {
        return JSON.parseObject(this.dag, NodeEdgeDAG.class);
    }

    public JobFlowDAG getJobFlowDAG() {
        NodeEdgeDAG nodeEdgeDAG = JSON.parseObject(this.dag, NodeEdgeDAG.class);
        return DAGUtil.convert(nodeEdgeDAG);
    }

    public void setNodeEdgeDAG(NodeEdgeDAG nodeEdgeDAG) {
        this.dag = JSON.toJSONString(nodeEdgeDAG);
    }

    public void setJobFlowDAG(JobFlowDAG jobFlowDAG) {
        this.dag = JSON.toJSONString(DAGUtil.convert(jobFlowDAG));
    }
}