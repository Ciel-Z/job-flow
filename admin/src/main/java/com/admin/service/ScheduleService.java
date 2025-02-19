package com.admin.service;

import com.admin.mapper.JobFlowInstanceMapper;
import com.admin.mapper.JobInfoMapper;
import com.admin.mapper.JobInstanceMapper;
import com.admin.util.CronUtil;
import com.common.config.VertxConfiguration;
import com.common.constant.Constant;
import com.common.dag.JobFlowDAG;
import com.common.dag.NodeEdgeDAG;
import com.common.entity.*;
import com.common.enums.JobStatusEnum;
import com.common.enums.LockEnum;
import com.common.lock.GlobalLock;
import com.common.vertx.AbstractEventVerticle;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import com.hazelcast.multimap.MultiMap;
import io.netty.util.HashedWheelTimer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScheduleService implements InitializingBean {

    public static final Long RUNNING_INTERVAL = 15000L;

    private final NodeInfo localNode;

    private final HazelcastInstance hazelcast;

    private final List<AbstractEventVerticle<?>> verticles;

    private final VertxConfiguration vertxConfiguration;

    private final JobInfoMapper jobInfoMapper;

    private final JobInstanceMapper jobInstanceMapper;

    private final JobFlowInstanceMapper jobFlowInstanceMapper;

    private final JobDispatchService jobDispatchService;

    private MultiMap<String, MappingInfo> featureHolderMap;

    private MultiMap<String, String> nodeHolderMap;

    private IMap<String, NodeInfo> serverNodeMap;

    // 时间轮
    private final HashedWheelTimer wheelTimer = new HashedWheelTimer(4, TimeUnit.MILLISECONDS, 512);


    @Override
    public void afterPropertiesSet() {
        serverNodeMap = hazelcast.getMap(Constant.SERVER_LIST);
        nodeHolderMap = hazelcast.getMultiMap(Constant.NODE_HOLDER);
        featureHolderMap = hazelcast.getMultiMap(Constant.FEATURE_HOLDER);
    }


    /**
     * 心跳检测 | 首次上线离线时重新注册服务
     */
    public void heartbeat() {
        NodeInfo remoteNodeInfo = serverNodeMap.get(localNode.getServerAddress());
        // 减少重复注册, 节点发生变化节点超出配置离线时间时重新注册
        if (isChanged(remoteNodeInfo) || isNodeOffline(remoteNodeInfo)) {
            List<String> list = verticles.stream().map(AbstractEventVerticle::fullAddress).toList();
            register(list);
        }
        log.debug("Heartbeat check for node {}", localNode.getServerAddress());
        serverNodeMap.set(localNode.getServerAddress(), localNode.updateTimeStamp(), RUNNING_INTERVAL * 4, java.util.concurrent.TimeUnit.SECONDS);
    }


    /**
     * 任务调度 | 尝试将逾期未处理的任务重新分配
     * 全局锁任意一个 admin 处理即可
     */
    @GlobalLock(type = LockEnum.TRY_LOCK, key = "processOverdueJob", leaseTime = 15000)
    public void processOverdueJob() {
        // 如果没有可用的服务器，则直接返回
        IMap<String, NodeInfo> serverNodeMap = hazelcast.getMap(Constant.SERVER_LIST);
        if (serverNodeMap.isEmpty()) {
            return;
        }

        // 计算任务超时的截止时间
        long timestamp = System.currentTimeMillis() - (RUNNING_INTERVAL * 2);
        List<Long> jobIds = jobInfoMapper.selectOverdueJob(timestamp);
        if (jobIds.isEmpty()) {
            return;
        }

        // 根据服务器数量对任务进行分片
        int serverCount = serverNodeMap.size();
        // 每个任务根据 jobId % serverCount 分到对应的组中
        Map<Integer, List<Long>> sliceMap = jobIds.stream().collect(Collectors.groupingBy(jobId -> Math.toIntExact(jobId % serverCount)));

        // 将超时任务分配到平均分配到对应的当前在线服务器
        List<String> serverIps = new ArrayList<>(serverNodeMap.keySet());
        for (int i = 0; i < serverIps.size(); i++) {
            List<Long> sliceIds = sliceMap.get(i);
            if (sliceIds != null && !sliceIds.isEmpty()) {
                jobInfoMapper.updateJobServerIp(sliceIds, serverIps.get(i));
            }
        }
    }


    /**
     * cron 任务调度 (netty 时间轮实现) | 暂不支持秒级任务
     * 每个 admin 处理需要自己调度的任务
     */
    public void scheduleJob() {
        Long timestamp = System.currentTimeMillis() + RUNNING_INTERVAL * 2;
        List<JobInfo> jobs = jobInfoMapper.selectByServerIpAndLessTimestamp(localNode.getServerAddress(), timestamp);
        if (jobs.isEmpty()) {
            return;
        }
        // 将本轮执行的任务压入 netty 时间轮
        for (JobInfo job : jobs) {
            long delay = Math.max(job.getNextTriggerTime() - System.currentTimeMillis(), 5);
            wheelTimer.newTimeout(timeout -> jobDispatchService.start(job.getJobId(), null, 0), delay, TimeUnit.MILLISECONDS);
        }
        // 更新下次执行的时间
        for (JobInfo job : jobs) {
            Long nextTriggerTime = CronUtil.nextTime(job.getCron(), Math.max(job.getNextTriggerTime(), System.currentTimeMillis()));
            jobInfoMapper.updateJobNextTriggerTime(job.getJobId(), nextTriggerTime);
        }
    }


    /**
     * 任务实例心跳检查：将心跳超时的任务实例更新为失败状态。当 worker 宕机或网络离线导致任务失败时，主观认定为不可恢复的失败状态。
     * 全局锁任意一个 admin 处理即可
     */
    @GlobalLock(type = LockEnum.TRY_LOCK, key = "checkJobHeartbeat", leaseTime = 15000)
    public void checkJobHeartbeat() {
        // 获取当前时间 | 计算超时临界时间
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime timeoutTimestamp = now.minusSeconds(RUNNING_INTERVAL * 4);

        // 查询心跳超时的任务实例
        List<JobInstance> instances = jobInstanceMapper.selectRunningJobTimeout(timeoutTimestamp);
        if (instances.isEmpty()) {
            return;
        }

        // 更新任务实例为失败状态
        List<Long> ids = instances.stream().map(JobInstance::getId).collect(Collectors.toList());
        jobInstanceMapper.updateTimeoutByIds(ids);

        // 将有 Flow 信息的任务实例按 flowInstanceId 分组
        Map<Long, List<JobInstance>> flowInstanceMap = instances.stream().filter(instance -> instance.getFlowNodeId() != null && instance.getFlowInstanceId() != null).collect(Collectors.groupingBy(JobInstance::getFlowInstanceId));

        // 更新工作流实例及节点状态
        if (!flowInstanceMap.isEmpty()) {
            List<JobFlowInstance> jobFlowInstances = jobFlowInstanceMapper.selectByIds(flowInstanceMap.keySet());
            for (JobFlowInstance flowInstance : jobFlowInstances) {
                JobFlowDAG dag = flowInstance.getJobFlowDAG();
                for (JobInstance instance : flowInstanceMap.get(flowInstance.getId())) {
                    NodeEdgeDAG.Node node = dag.getNode(instance.getFlowNodeId()).getNode();
                    BeanUtils.copyProperties(instance, node);
                }
                // 更新 Flow 状态和结束时间
                flowInstance.setStatus(JobStatusEnum.FAIL.getCode());
                flowInstance.setEndTime(now);
                flowInstance.setJobFlowDAG(dag);
                flowInstance.setResult("任务心跳超时导致失败");
                jobFlowInstanceMapper.updateByPrimaryKey(flowInstance);
                // 发布工作流实例更新事件
                hazelcast.getTopic(Constant.JOB_FLOW_EVENT).publishAsync(instance2Json(flowInstance));
            }
        }
    }


    private void register(List<String> addresslist) {
        for (String address : addresslist) {
            featureHolderMap.put(address, MappingInfo.of(localNode.getServerAddress(), vertxConfiguration.getTag()));
        }
        log.info("Registered {} verticles for node {}", verticles.size(), localNode.getServerAddress());
        nodeHolderMap.putAllAsync(localNode.getServerAddress(), addresslist);
    }

    private boolean isChanged(NodeInfo nodeInfo) {
        return nodeInfo == null || !nodeInfo.getId().equals(localNode.getId());
    }

    private boolean isNodeOffline(NodeInfo nodeInfo) {
        return Duration.between(nodeInfo.getTimestamp(), LocalDateTime.now()).getSeconds() > vertxConfiguration.getTimeout();
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
