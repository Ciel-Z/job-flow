package com.worker.config;

import com.common.config.VertxConfiguration;
import com.common.constant.Constant;
import com.common.entity.JobHandler;
import com.common.entity.NodeInfo;
import com.common.entity.ServerInfo;
import com.common.util.PathUtil;
import com.hazelcast.core.HazelcastInstance;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Configuration
@RequiredArgsConstructor
public class WorkerMappingManager {

    private final VertxConfiguration vertxConfiguration;

    private final HazelcastInstance hazelcast;

    private final List<JobHandler> jobHandlers;


    @PostConstruct
    public void initialize() {
        ScheduledThreadPoolExecutor scheduledThreadPoolExecutor = new ScheduledThreadPoolExecutor(1);
        scheduledThreadPoolExecutor.scheduleAtFixedRate(this::registerJobs, 15, 60, TimeUnit.SECONDS);
    }

    /**
     * Register the job binding to the hazelcast. | 注册任务绑定到 hazelcast 实例 (移除交由 admin)
     */
    private void registerJobs() {
        if (isNodeRegistered(ServerInfo.getIp())) {
            return;
        }
        for (JobHandler jobHandler : jobHandlers) {
            String processorInfo = jobHandler.getClass().getName();
            register(ServerInfo.getIp(), processorInfo);
        }
    }

    private boolean isNodeRegistered(String ip) {
        return hazelcast.getMultiMap(Constant.NODE_HOLDER).containsKey(ip);
    }


    private void register(String ip, String processorInfo) {
        NodeInfo nodeInfo = NodeInfo.of(PathUtil.getGlobalPath(Constant.DISPATCH), vertxConfiguration.getTag());
        hazelcast.getMultiMap(Constant.FEATURE_HOLDER).put(processorInfo, nodeInfo);
        hazelcast.getMultiMap(Constant.NODE_HOLDER).put(ip, processorInfo);
    }

}