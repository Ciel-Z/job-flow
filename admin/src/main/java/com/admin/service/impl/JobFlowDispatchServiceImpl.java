package com.admin.service.impl;

import com.admin.mapper.JobFlowInstanceMapper;
import com.admin.mapper.JobFlowMapper;
import com.admin.mapper.JobInfoMapper;
import com.admin.mapper.JobInstanceMapper;
import com.admin.service.JobFlowDispatchService;
import com.admin.service.JobFlowEventService;
import com.alibaba.fastjson2.JSON;
import com.common.constant.Constant;
import com.common.dag.JobFlowDAG;
import com.common.dag.NodeEdgeDAG;
import com.common.entity.JobFlow;
import com.common.entity.JobFlowInstance;
import com.common.entity.JobInfo;
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
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class JobFlowDispatchServiceImpl implements JobFlowDispatchService {

    private final HazelcastInstance hazelcast;

    private final JobInfoMapper jobInfoMapper;

    private final JobInstanceMapper jobInstanceMapper;

    private final JobFlowMapper jobFlowMapper;

    private final JobFlowInstanceMapper jobFlowInstanceMapper;

    private final JobFlowEventService jobFlowEventService;


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
        List<JobInfo> jobs = jobInfoMapper.selectByIds(jobIds);
        Map<Long, JobInfo> jobMap = jobs.stream().collect(Collectors.toMap(JobInfo::getJobId, Function.identity()));
        Set<Long> notExistJobs = new HashSet<>(Sets.difference(jobIds, jobMap.keySet()));
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
            jobFlowEventService.startJob(instance, jobDAG, node.getNodeId(), jobMap.get(node.getJobId()));
        });
        return instance.getId();
    }

    @Override
    public void stop(Long instanceId) {
        JobFlowInstance instance = jobFlowInstanceMapper.selectByPrimaryKey(instanceId);
        AssertUtils.isNotNull(instance, "工作流实例不存在");
        AssertUtils.isTrue(instance.getStatus() == 1, "工作流状态不支持停止");

        // 更新工作流实例版本
        jobFlowInstanceMapper.updateVersionById(instance);

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
        jobInstanceMapper.updateStopByFlowInstanceId(instance.getId());

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
        instance = jobFlowInstanceMapper.selectByPrimaryKey(flowInstanceId);
        jobFlowEventService.startJob(instance, dag, nodeId, jobInfo);
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
