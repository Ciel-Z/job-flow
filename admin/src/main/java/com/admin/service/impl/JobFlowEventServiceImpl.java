package com.admin.service.impl;

import com.admin.mapper.JobFlowInstanceMapper;
import com.admin.mapper.JobInfoMapper;
import com.admin.service.JobDispatchService;
import com.admin.service.JobFlowEventService;
import com.alibaba.fastjson2.JSON;
import com.common.constant.Constant;
import com.common.dag.JobFlowDAG;
import com.common.dag.NodeEdgeDAG;
import com.common.entity.JobFlowInstance;
import com.common.entity.JobInfo;
import com.common.entity.JobInstance;
import com.common.entity.JobReport;
import com.common.enums.JobStatusEnum;
import com.common.enums.LockEnum;
import com.common.lock.GlobalLock;
import com.common.util.DAGUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.hazelcast.core.HazelcastInstance;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class JobFlowEventServiceImpl implements JobFlowEventService {

    private final HazelcastInstance hazelcast;

    private final JobInfoMapper jobInfoMapper;

    private final JobFlowInstanceMapper jobFlowInstanceMapper;

    // 解决循环依赖, 依赖 job 进行启动任务
    @Lazy
    @Autowired
    private  JobDispatchService jobDispatchService;

    private static final Set<Integer> STOPPED_STATUSES = Set.of(JobStatusEnum.FAIL.getCode(), JobStatusEnum.PAUSE.getCode());


    @Override
    @GlobalLock(type = LockEnum.LOCK, key = "#jobReport.flowInstanceId", leaseTime = 10000)
    public void processJobFlowEvent(JobReport jobReport) {
        // 工作流实例被删除 || 工作流版本已过时 (防止出现因重试导致的调度混乱)
        JobFlowInstance instance = jobFlowInstanceMapper.selectByPrimaryKey(jobReport.getFlowInstanceId());
        if (instance == null || instance.getVersion() > jobReport.getJobFlowVersion()) {
            return;
        }

        // 更新当前节点信息
        NodeEdgeDAG nodeEdgeDAG = instance.getNodeEdgeDAG();
        JobFlowDAG dag = DAGUtil.convert(nodeEdgeDAG);
        NodeEdgeDAG.Node edgeNode = dag.getNode(jobReport.getFlowNodeId()).getNode();
        BeanUtils.copyProperties(jobReport, edgeNode);
        edgeNode.setEndTime(jobReport.getTimestamp());

        // 任务开始运行 || 工作流实例已停止 (场景 1.任务回执开始运行 2.其他节点已报错 3.页面触发强制停止 4.任务心跳超时被终止)
        if (JobStatusEnum.RUNNING.getCode().equals(jobReport.getStatus()) || STOPPED_STATUSES.contains(instance.getStatus())) {
            updateInstance(nodeEdgeDAG, instance);
            return;
        }

        // 失败或暂停, 更新工作流状态
        if (STOPPED_STATUSES.contains(jobReport.getStatus())) {
            instanceEnd(jobReport, instance, edgeNode.errorMassage());
            updateInstance(nodeEdgeDAG, instance);
            return;
        }

        // 工作流完成
        if (DAGUtil.areAllTasksCompleted(dag)) {
            instanceEnd(jobReport, instance, "工作流全部任务执行成功");
            updateInstance(nodeEdgeDAG, instance);
            return;
        }

        // 获取后继节点中就绪节点
        List<NodeEdgeDAG.Node> readyNodes = DAGUtil.getReadyNodes(dag.getNodeMap(), jobReport.getFlowNodeId());
        if (readyNodes.isEmpty()) {// 可能存在多个前置条件的节点, 此时前置条件并未全部完成
            updateInstance(nodeEdgeDAG, instance);
            return;
        }

        // 判断就绪节点是否全部存在
        Set<Long> jobIds = readyNodes.stream().map(NodeEdgeDAG.Node::getJobId).collect(Collectors.toSet());
        List<JobInfo> jobInfos = jobInfoMapper.selectByIds(jobIds);
        Map<Long, JobInfo> jobMap = jobInfos.stream().collect(Collectors.toMap(JobInfo::getJobId, Function.identity()));

        // 工作流中就绪节点对应任务不存在
        if (!validateReadyNodes(readyNodes, jobMap, instance)) {
            updateInstance(nodeEdgeDAG, instance);
            return;
        }

        // 更新本次运行结果, 和即将运行节点的结果
        updateReadyNodesStatus(readyNodes);
        updateInstance(nodeEdgeDAG, instance);

        // 启动就绪任务
        startReadyJobs(readyNodes, instance, dag, jobMap);
    }


    @Override
    public void startJob(JobFlowInstance instance, JobFlowDAG dag, Long nodeId, JobInfo jobInfo) {
        NodeEdgeDAG.Node node = dag.getNode(nodeId).getNode();
        JobInstance jobInstance = jobDispatchService.instance(jobInfo, node.getParams(), instance.getId(), nodeId);
        jobInstance.setJobFlowVersion(instance.getVersion());
        jobDispatchService.start(jobInstance);
        log.info("StartJobFlowNode flowInstanceId: {}, node: {} jobId: {}", instance.getId(), node.getJobName(), node.getJobId());
    }

    private void startReadyJobs(List<NodeEdgeDAG.Node> readyNodes, JobFlowInstance instance, JobFlowDAG dag, Map<Long, JobInfo> jobMap) {
        for (NodeEdgeDAG.Node readyNode : readyNodes) {
            startJob(instance, dag, readyNode.getNodeId(), jobMap.get(readyNode.getJobId()));
        }
    }

    private void updateInstance(NodeEdgeDAG dag, JobFlowInstance flowInstance) {
        flowInstance.setDag(JSON.toJSONString(dag));
        jobFlowInstanceMapper.updateByPrimaryKey(flowInstance);
        // 发送任务流实例状态更新消息
        hazelcast.getTopic(Constant.JOB_FLOW_EVENT).publishAsync(instance2Json(flowInstance));
    }

    private static void instanceEnd(JobReport jobReport, JobFlowInstance instance, String result) {
        instance.setStatus(jobReport.getStatus());
        instance.setEndTime(jobReport.getTimestamp());
        instance.setResult(result);
    }

    private static boolean validateReadyNodes(List<NodeEdgeDAG.Node> readyNodes, Map<Long, JobInfo> jobMap, JobFlowInstance instance) {
        boolean flag = true;
        for (NodeEdgeDAG.Node node : readyNodes) {
            if (!jobMap.containsKey(node.getJobId())) {
                node.setResult("任务不存在");
                node.setStatus(JobStatusEnum.FAIL.getCode());
                instance.setStatus(JobStatusEnum.FAIL.getCode());
                instance.setEndTime(LocalDateTime.now());
                instance.setResult("工作流中任务不存在");
                flag = false;
            }
        }
        return flag;
    }

    private static void updateReadyNodesStatus(List<NodeEdgeDAG.Node> readyNodes) {
        for (NodeEdgeDAG.Node node : readyNodes) {
            node.setStartTime(LocalDateTime.now());
            node.setStatus(JobStatusEnum.DISPATCH.getCode());
        }
    }



    private static String instance2Json(JobFlowInstance flowInstance) {
        try {
            return new ObjectMapper().registerModule(new JavaTimeModule()).writeValueAsString(flowInstance);
        } catch (JsonProcessingException e) {
            log.info("Failed to serialize JobFlowInstance to JSON", e);
            return "";
        }
    }
}
