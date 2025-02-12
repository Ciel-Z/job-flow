package com.admin.service.impl;

import com.admin.mapper.JobFlowInstanceMapper;
import com.admin.mapper.JobFlowMapper;
import com.admin.mapper.JobInfoMapper;
import com.admin.service.JobDispatchService;
import com.admin.service.JobFlowDispatchService;
import com.common.constant.Constant;
import com.common.dag.JobFlowDAG;
import com.common.dag.NodeEdgeDAG;
import com.common.entity.JobFlow;
import com.common.entity.JobFlowInstance;
import com.common.entity.JobInfo;
import com.common.entity.JobInstance;
import com.common.enums.JobStatusEnum;
import com.common.util.AssertUtils;
import com.common.util.DAGUtil;
import com.google.common.collect.Sets;
import com.hazelcast.core.HazelcastInstance;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class JobFlowDispatchServiceImpl implements JobFlowDispatchService {

    // lock + websocket 通知

    private final HazelcastInstance hazelcast;

    private final JobInfoMapper jobInfoMapper;

    private final JobFlowMapper jobFlowMapper;

    private final JobDispatchService jobDispatchService;

    private final JobFlowInstanceMapper jobFlowInstanceMapper;

    @Override
    public Long start(Long jobFlowId) {
        JobFlow jobFlow = jobFlowMapper.selectByPrimaryKey(jobFlowId);
        AssertUtils.isNotNull(jobFlow, "工作流不存在");
        AssertUtils.notEmpty(jobFlow.getDag(), "DAG为空");

        JobFlowDAG jobDAG = jobFlow.getJobFlowDAG();
        NodeEdgeDAG edgeDAG = DAGUtil.convert(jobDAG);
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
        instance.setJobFlowDAG(jobDAG);
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
        AssertUtils.isTrue(instance.getStatus() > 1, "工作流状态不支持停止");

        // 更新工作流实例及 DAG
        instance.setStatus(JobStatusEnum.FAIL.getCode());
        instance.setEndTime(LocalDateTime.now());
        instance.setResult("工作流被强制停止");
        JobFlowDAG dag = instance.getJobFlowDAG();
        for (JobFlowDAG.Node jobNode : dag.getNodeMap().values()) {
            NodeEdgeDAG.Node node = jobNode.getNode();
            if (JobStatusEnum.RUNNING.getCode().equals(node.getStatus())) {
                node.setStatus(JobStatusEnum.FAIL.getCode());
                node.setEndTime(LocalDateTime.now());
                node.setResult("工作流被强制停止");
            }
        }
        instance.setJobFlowDAG(dag);
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
        AssertUtils.isTrue(instance.getStatus() == 1, "工作流状态不支持重试");

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
