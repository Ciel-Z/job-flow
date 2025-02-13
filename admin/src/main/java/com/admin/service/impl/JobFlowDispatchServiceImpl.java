package com.admin.service.impl;

import com.admin.mapper.JobFlowInstanceMapper;
import com.admin.mapper.JobFlowMapper;
import com.admin.mapper.JobInfoMapper;
import com.admin.service.JobDispatchService;
import com.admin.service.JobFlowDispatchService;
import com.alibaba.fastjson2.JSON;
import com.common.constant.Constant;
import com.common.dag.JobFlowDAG;
import com.common.dag.NodeEdgeDAG;
import com.common.entity.*;
import com.common.enums.JobStatusEnum;
import com.common.enums.LockEnum;
import com.common.lock.GlobalLock;
import com.common.util.AssertUtils;
import com.common.util.DAGUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.google.common.collect.Sets;
import com.hazelcast.core.HazelcastInstance;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class JobFlowDispatchServiceImpl implements JobFlowDispatchService {

    private final HazelcastInstance hazelcast;

    private final JobInfoMapper jobInfoMapper;

    private final JobFlowMapper jobFlowMapper;

    private final JobDispatchService jobDispatchService;

    private final JobFlowInstanceMapper jobFlowInstanceMapper;

    private static final Set<Integer> STOPPED_STATUSES = Set.of(JobStatusEnum.FAIL.getCode(), JobStatusEnum.PAUSE.getCode());


    @Override
    public Long start(Long jobFlowId) {
        JobFlow jobFlow = jobFlowMapper.selectByPrimaryKey(jobFlowId);
        AssertUtils.isNotNull(jobFlow, "工作流不存在");
        AssertUtils.notEmpty(jobFlow.getDag(), "DAG为空");

        NodeEdgeDAG edgeDAG = JSON.parseObject(jobFlow.getDag(), NodeEdgeDAG.class);
        JobFlowDAG jobDAG = DAGUtil.convert(edgeDAG);
        Set<Long> jobIds = edgeDAG.getNodes().stream().map(NodeEdgeDAG.Node::getJobId).collect(Collectors.toSet());
        AssertUtils.notEmpty(jobIds, "工作流为空");

        // DAG
        Set<Long> existJobIds = jobInfoMapper.selectIdByIds(jobIds);
        Set<Long> notExistJobs = new HashSet<>(Sets.difference(jobIds, existJobIds));
        AssertUtils.empty(notExistJobs, "工作流中 [{}] 任务不存在", notExistJobs);

        // 初始化 DAG
        JobFlowInstance instance = new JobFlowInstance();
        BeanUtils.copyProperties(jobFlow, instance, "createdDate", "updatedDate");
        instance.setTriggerTime(LocalDateTime.now());
        instance.setStatus(JobStatusEnum.RUNNING.getCode());
        initDag(jobDAG);
        instance.setVersion(0);
        instance.setNodeEdgeDAG(edgeDAG);
        jobFlowInstanceMapper.insert(instance);

        // 启动根节点对应任务
        jobDAG.getRoots().stream().map(JobFlowDAG.Node::getNode).forEach(node -> {
            startJob(instance, jobDAG, node.getNodeId());
        });
        return instance.getId();
    }

    @Override
    public void stop(Long instanceId) {
        JobFlowInstance instance = jobFlowInstanceMapper.selectByPrimaryKey(instanceId);
        AssertUtils.isNotNull(instance, "工作流实例不存在");
        AssertUtils.isTrue(instance.getStatus() == 1, "工作流状态不支持停止");

        // 更新工作流实例及 DAG
        instance.setStatus(JobStatusEnum.FAIL.getCode());
        instance.setEndTime(LocalDateTime.now());
        instance.setResult("工作流被强制停止");
        NodeEdgeDAG dag = instance.getNodeEdgeDAG();
        for (NodeEdgeDAG.Node node : dag.getNodes()) {
            if (JobStatusEnum.RUNNING.getCode().equals(node.getStatus())) {
                node.setStatus(JobStatusEnum.FAIL.getCode());
                node.setEndTime(LocalDateTime.now());
                node.setResult("工作流被强制停止");
            }
        }
        instance.setNodeEdgeDAG(dag);
        jobFlowInstanceMapper.updateByPrimaryKey(instance);
        jobFlowInstanceMapper.updateVersionById(instance);

        // 修改任务实例全局状态 | 任务监控线程上报状态检查此状态, 非运行状态时会停止任务线程
        String key = String.format("%d_%d", instance.getId(), instance.getVersion());
        hazelcast.getMap(Constant.JOB_FLOW_TERMINATION).put(key, JobStatusEnum.FAIL.getCode());
    }


    @Override
    public void retry(Long flowInstanceId, Long nodeId) {
        JobFlowInstance instance = jobFlowInstanceMapper.selectByPrimaryKey(flowInstanceId);
        AssertUtils.isNotNull(instance, "工作流实例不存在");
        AssertUtils.isTrue(instance.getStatus() > 1, "工作流状态不支持重试");

        // 获取 DAG
        JobFlowDAG dag = instance.getJobFlowDAG();
        JobFlowDAG.Node node = dag.getNode(nodeId);
        AssertUtils.isNotNull(node, "节点不存在");

        // 获取任务信息
        NodeEdgeDAG.Node edgeNode = node.getNode();
        JobInfo jobInfo = jobInfoMapper.selectByPrimaryKey(edgeNode.getJobId());
        AssertUtils.isNotNull(jobInfo, "{} 任务不存在", edgeNode.getNodeName());

        // 检查重试条件
        Set<Long> rootIds = dag.getRoots().stream().map(JobFlowDAG.Node::getNodeId).collect(Collectors.toSet());
        AssertUtils.isFalse(rootIds.contains(nodeId) && dag.getRoots().size() > 1, "多根节点情况不支持从根节点重试, 可以尝试重新启动工作流");
        AssertUtils.isTrue(DAGUtil.retryOverride(dag, node), "该节点不能覆盖所有需重试节点");

        // 重置此节点及后继节点状态
        DAGUtil.resetSubStatus(node);
        DAGUtil.initNode(edgeNode);
        edgeNode.setStatus(JobStatusEnum.DISPATCH.getCode());
        instance.setStatus(JobStatusEnum.RUNNING.getCode());
        instance.setResult(null);
        instance.setJobFlowDAG(dag);
        jobFlowInstanceMapper.updateByPrimaryKey(instance);
        jobFlowInstanceMapper.updateVersionById(instance);

        // 启动任务
        startJob(instance, dag, nodeId);
    }

    public void startJob(JobFlowInstance instance, JobFlowDAG dag, Long nodeId) {
        NodeEdgeDAG.Node node = dag.getNode(nodeId).getNode();
        JobInfo jobInfo = jobInfoMapper.selectByPrimaryKey(node.getJobId());
        if (jobInfo == null) {
            node.setResult("任务不存在");
            node.setStatus(JobStatusEnum.FAIL.getCode());
            instance.setStatus(JobStatusEnum.FAIL.getCode());
            instance.setEndTime(LocalDateTime.now());
            instance.setJobFlowDAG(dag);
            instance.setResult(node.errorMassage());
            jobFlowInstanceMapper.updateByPrimaryKey(instance);
            return;
        }
        JobInstance jobInstance = jobDispatchService.instance(jobInfo, node.getParams(), instance.getId(), nodeId);
        jobInstance.setJobFlowVersion(instance.getVersion());
        jobDispatchService.start(jobInstance);
        log.info("StartJobFlowNode flowInstanceId: {}, node: {} jobId: {}", instance.getId(), node.getJobName(), node.getJobId());
    }


    @Override
    @GlobalLock(type = LockEnum.LOCK, key = "#jobReport.flowNodeId", leaseTime = 10000)
    public void processJobFlowEvent(JobReport jobReport) {
        // 工作流实例被删除 || 工作流版本已过时 (防止出现因重试导致的调度混乱)
        JobFlowInstance flowInstance = jobFlowInstanceMapper.selectByPrimaryKey(jobReport.getFlowInstanceId());
        if (flowInstance == null || flowInstance.getVersion() > jobReport.getJobFlowVersion()) {
            return;
        }

        // 更新当前节点信息
        NodeEdgeDAG nodeEdgeDAG = flowInstance.getNodeEdgeDAG();
        JobFlowDAG dag = DAGUtil.convert(nodeEdgeDAG);
        NodeEdgeDAG.Node edgeNode = dag.getNode(jobReport.getFlowNodeId()).getNode();
        BeanUtils.copyProperties(jobReport, edgeNode);
        edgeNode.setEndTime(jobReport.getTimestamp());

        // 工作流实例已停止仅更新此节点信息 | 可能情况 1.其他节点已报错 2.页面触发强制停止
        if (STOPPED_STATUSES.contains(flowInstance.getStatus())) {
            updateInstance(nodeEdgeDAG, flowInstance);
            return;
        }

        // 失败或暂停, 更新工作流状态
        if (STOPPED_STATUSES.contains(jobReport.getStatus())) {
            flowInstance.setStatus(jobReport.getStatus());
            flowInstance.setEndTime(jobReport.getTimestamp());
            flowInstance.setResult(String.format("[%d %s] 任务出现问题", edgeNode.getNodeId(), edgeNode.getNodeName()));
            updateInstance(nodeEdgeDAG, flowInstance);
            return;
        }

        // 工作流完成
        if (DAGUtil.areAllTasksCompleted(dag)) {
            flowInstance.setStatus(JobStatusEnum.SUCCESS.getCode());
            flowInstance.setEndTime(jobReport.getTimestamp());
            flowInstance.setResult("工作流全部任务执行成功");
            updateInstance(nodeEdgeDAG, flowInstance);
            return;
        }

        // 获取就绪节点
        List<NodeEdgeDAG.Node> readyNodes = DAGUtil.getReadyNodes(dag.getNodeMap(), jobReport.getFlowNodeId());
        // 更新工作流实例
        readyNodes.forEach(node -> {
            node.setStartTime(LocalDateTime.now());
            node.setStatus(JobStatusEnum.DISPATCH.getCode());
        });
        updateInstance(nodeEdgeDAG, flowInstance);

        // 启动就绪任务
        for (NodeEdgeDAG.Node readyNode : readyNodes) {
            startJob(flowInstance, dag, readyNode.getNodeId());
        }
    }


    private void updateInstance(NodeEdgeDAG dag, JobFlowInstance flowInstance) {
        flowInstance.setDag(JSON.toJSONString(dag));
        jobFlowInstanceMapper.updateByPrimaryKey(flowInstance);
        // 发送任务流实例状态更新消息
        hazelcast.getTopic(Constant.JOB_FLOW_EVENT).publishAsync(instance2Json(flowInstance));
    }


    private static String instance2Json(JobFlowInstance flowInstance) {
        try {
            return new ObjectMapper().registerModule(new JavaTimeModule()).writeValueAsString(flowInstance);
        } catch (JsonProcessingException e) {
            log.info("Failed to serialize JobFlowInstance to JSON", e);
            return "";
        }
    }


    private static void initDag(JobFlowDAG jobDAG) {
        for (JobFlowDAG.Node jobNode : jobDAG.getNodeMap().values()) {
            DAGUtil.initNode(jobNode.getNode());
        }
        // 初始化根节点状态
        jobDAG.getRoots().forEach(node -> {
            node.getNode().setStartTime(LocalDateTime.now());
            node.getNode().setStatus(JobStatusEnum.DISPATCH.getCode());
        });
    }
}
