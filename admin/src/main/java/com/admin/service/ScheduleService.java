package com.admin.service;

import com.admin.mapper.JobInfoMapper;
import com.admin.mapper.JobInstanceMapper;
import com.admin.util.CronUtil;
import com.common.config.VertxConfiguration;
import com.common.constant.Constant;
import com.common.entity.JobInfo;
import com.common.entity.MappingInfo;
import com.common.entity.NodeInfo;
import com.common.vertx.AbstractEventVerticle;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import com.hazelcast.multimap.MultiMap;
import io.netty.util.HashedWheelTimer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

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

    private final JobDispatchService jobDispatchService;

    private MultiMap<String, MappingInfo> featureHolderMap;

    private MultiMap<String, String> nodeHolderMap;

    private IMap<String, NodeInfo> serverNodeMap;

    // 时间轮
    private HashedWheelTimer wheelTimer = new HashedWheelTimer(4, TimeUnit.MILLISECONDS, 512);


    @Override
    public void afterPropertiesSet() throws Exception {
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
     */
    public void processOverdueJob() {
        String lock = "processOverdueJob";
        IMap<String, NodeInfo> serverNodeMap = hazelcast.getMap(Constant.SERVER_LIST);
        if (serverNodeMap.isEmpty() || serverNodeMap.isLocked(lock)) {
            return;
        }
        boolean locked = false;
        try {
            // 尝试将逾期未处理的任务重新分配
            locked = serverNodeMap.tryLock(lock, RUNNING_INTERVAL, TimeUnit.MILLISECONDS);
            if (locked) {
                Long timestamp = System.currentTimeMillis() - RUNNING_INTERVAL * 2;
                List<Long> jobIds = jobInfoMapper.selectOverdueJob(timestamp);
                if (jobIds.isEmpty()) {
                    return;
                }
                int size = serverNodeMap.size();
                Map<Long, List<Long>> sliceMap = new HashMap<>();
                jobIds.forEach(jobId -> sliceMap.computeIfAbsent(jobId % size, k -> new ArrayList<>()).add(jobId));
                long idx = 0L;
                for (String serverIp : serverNodeMap.keySet()) {
                    List<Long> sliceIds = sliceMap.get(idx);
                    if (sliceIds != null) {
                        jobInfoMapper.updateJobServerIp(sliceIds, serverIp);
                    }
                    idx += 1;
                }
            }
        } catch (Exception e) {
            log.error("Schedule job error: {}", e.getMessage(), e);
        } finally {
            if (locked) {
                serverNodeMap.unlock(lock);
            }
        }
    }

    public void scheduleJob() {
        Long timestamp = System.currentTimeMillis() + RUNNING_INTERVAL * 2;
        List<JobInfo> jobs = jobInfoMapper.selectByServerIpAndLessTimestamp(localNode.getServerAddress() , timestamp);
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

    public void checkJobHeartbeat() {
        String lock = "checkJobHeartbeat";
        if (serverNodeMap.isLocked(lock)) {
            return;
        }
        boolean locked = false;
        try {
            // 将心跳超时的任务设置为失败
            locked = serverNodeMap.tryLock(lock, RUNNING_INTERVAL, TimeUnit.MILLISECONDS);
            if (locked) {
                LocalDateTime timestamp = LocalDateTime.now().minusSeconds(RUNNING_INTERVAL * 4);
                jobInstanceMapper.updateRunningJobTimeOut(timestamp);
            }
        } catch (Exception e) {
            log.error("Schedule job error: {}", e.getMessage(), e);
        } finally {
            if (locked) {
                serverNodeMap.unlock(lock);
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

}
