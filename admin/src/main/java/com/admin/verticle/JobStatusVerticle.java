package com.admin.verticle;

import com.admin.mapper.JobFlowInstanceMapper;
import com.admin.mapper.JobInstanceMapper;
import com.admin.service.JobFlowDispatchService;
import com.common.annotation.VerticlePath;
import com.common.constant.Constant;
import com.common.dag.JobFlowDAG;
import com.common.dag.NodeEdgeDAG;
import com.common.entity.JobEvent;
import com.common.entity.JobFlowInstance;
import com.common.entity.JobReport;
import com.common.enums.JobStatusEnum;
import com.common.util.DAGUtil;
import com.common.vertx.AbstractEventVerticle;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;

@Slf4j
@Component
@RequiredArgsConstructor
@VerticlePath(Constant.DISPATCH_REPORT)
public class JobStatusVerticle extends AbstractEventVerticle<JobReport> {

    private final JobInstanceMapper jobInstanceMapper;

    private final JobFlowInstanceMapper jobFlowInstanceMapper;

    private final JobFlowDispatchService jobFlowDispatchService;

    private static final Set<Integer> STOPPED_STATUSES = Set.of(JobStatusEnum.FAIL.getCode(), JobStatusEnum.PAUSE.getCode());


    @Override
    public void process(JobEvent<JobReport> jobEvent) {
        JobReport jobReport = jobEvent.getBody();
        log.info("WorkerJObReport {} instanceId = {} worker = {} status = {} {}", jobReport.getJobName(), jobReport.getInstanceId(), jobReport.getWorkerAddress(), jobReport.getStatus(), jobReport.getResult());

        // executeBlocking 处理阻塞任务
        getVertx().executeBlocking((Callable<Void>) () -> {
            processReport(jobReport);
            return null;
        }).onFailure(e -> {
            log.error("WorkerJObReport {} instanceId = {} worker = {} jobReport = {}", jobReport.getJobName(), jobReport.getInstanceId(), jobReport.getWorkerAddress(), jobReport, e);
        });
    }


    private void processReport(JobReport jobReport) {
        jobInstanceMapper.updateByEvent(jobReport);

        // 非工作流任务 || 任务未完成
        if (jobReport.getFlowInstanceId() == null || jobReport.getFlowNodeId() == null || JobStatusEnum.RUNNING.getCode().equals(jobReport.getStatus())) {
            return;
        }

        // 工作流实例被删除 || 工作流版本已过时 (防止出现因重试导致的调度混乱)
        JobFlowInstance flowInstance = jobFlowInstanceMapper.selectByPrimaryKey(jobReport.getFlowInstanceId());
        if (flowInstance == null || flowInstance.getVersion() > jobReport.getJobFlowVersion()) {
            return;
        }

        // 更新当前节点信息
        JobFlowDAG dag = flowInstance.getJobFlowDAG();
        NodeEdgeDAG.Node edgeNode = dag.getNode(jobReport.getFlowNodeId()).getNode();
        BeanUtils.copyProperties(jobReport, edgeNode);

        // 工作流实例已停止仅更新此节点信息 | 可能情况 1.其他节点已报错 2.页面触发强制停止
        if (STOPPED_STATUSES.contains(flowInstance.getStatus())) {
            updateInstance(dag, flowInstance);
            return;
        }

        // 失败或暂停, 更新工作流状态
        if (STOPPED_STATUSES.contains(jobReport.getStatus())) {
            flowInstance.setStatus(jobReport.getStatus());
            flowInstance.setEndTime(jobReport.getTimestamp());
            flowInstance.setResult(String.format("[%d %s] 任务出现问题", edgeNode.getNodeId(), edgeNode.getNodeName()));
            updateInstance(dag, flowInstance);
            return;
        }

        // 工作流完成
        if (DAGUtil.areAllTasksCompleted(dag)) {
            flowInstance.setStatus(JobStatusEnum.SUCCESS.getCode());
            flowInstance.setEndTime(jobReport.getTimestamp());
            flowInstance.setResult("工作流全部任务执行成功");
            updateInstance(dag, flowInstance);
            return;
        }

        // 获取就绪节点
        List<NodeEdgeDAG.Node> readyNodes = DAGUtil.getReadyNodes(dag.getNodeMap(), jobReport.getFlowNodeId());
        // 更新工作流实例
        readyNodes.forEach(node -> {
            node.setStartTime(LocalDateTime.now());
            node.setStatus(JobStatusEnum.DISPATCH.getCode());
        });
        updateInstance(dag, flowInstance);

        // 启动就绪任务
        for (NodeEdgeDAG.Node readyNode : readyNodes) {
            jobFlowDispatchService.startJob(flowInstance, dag, readyNode.getNodeId());
        }
    }

    private void updateInstance(JobFlowDAG dag, JobFlowInstance flowInstance) {
        flowInstance.setJobFlowDAG(dag);
        jobFlowInstanceMapper.updateByPrimaryKey(flowInstance);
    }
}
