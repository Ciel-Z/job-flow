package com.worker.config;

import com.common.config.VertxConfiguration;
import com.common.constant.Constant;
import com.common.entity.JobHandler;
import com.common.entity.MappingInfo;
import com.common.entity.NodeInfo;
import com.common.util.PathUtil;
import com.common.vertx.AbstractEventVerticle;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import com.hazelcast.multimap.MultiMap;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class WorkerMappingManager {

    private final VertxConfiguration vertxConfiguration;

    private final HazelcastInstance hazelcast;

    private final List<JobHandler> jobHandlers;

    private final List<AbstractEventVerticle<?>> verticles;

    private MultiMap<String, MappingInfo> featureHolderMap;

    private MultiMap<String, String> nodeHolderMap;

    private IMap<String, NodeInfo> nodeMap;


    @PostConstruct
    public void init() {
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        long initialDelay = 3L;
        long interval = vertxConfiguration.getHeartbeatInterval();
        scheduler.scheduleWithFixedDelay(this::registerWorker, initialDelay, interval, TimeUnit.SECONDS);
    }

    /**
     * Register the job binding to the hazelcast. | 注册任务绑定到 hazelcast 实例 (移除交由 @see com.common.listener.NodeMembershipListener)
     */
    private void registerWorker() {
        IMap<String, NodeInfo> nodeMap = getNodeMap();
        NodeInfo nodeInfo = nodeMap.get(NodeInfo.getServerAddress());
        if (isNodeNotChanged(nodeInfo) && isNodeAlive(nodeInfo)) {
            log.debug("Node {} report alive", NodeInfo.getServerAddress());
            nodeMap.put(NodeInfo.getServerAddress(), NodeInfo.getNowTimeStamp());
            return;
        }
        List<String> addressList = verticles.stream().map(AbstractEventVerticle::fullAddress).toList();
        registerVerticles(addressList);

        List<String> handlers = jobHandlers.stream().map(JobHandler::getClass).map(Class::getName).toList();
        registerHandle(handlers);
        nodeMap.put(NodeInfo.getServerAddress(), NodeInfo.getNowTimeStamp());
    }

    private boolean isNodeNotChanged(NodeInfo nodeInfo) {
        return nodeInfo != null && nodeInfo.getId().equals(NodeInfo.getInfo().getId());
    }


    private boolean isNodeAlive(NodeInfo nodeInfo) {
        return Duration.between(nodeInfo.getTimestamp(), LocalDateTime.now()).getSeconds() <= vertxConfiguration.getTimeout();
    }

    private void registerHandle(List<String> processorInfos) {
        for (String processorInfo : processorInfos) {
            MappingInfo mappingInfo = MappingInfo.of(PathUtil.getGlobalPath(Constant.DISPATCH), vertxConfiguration.getTag());
            getFeatureHolderMap().put(processorInfo, mappingInfo);
        }
        log.info("Registered {} jop handle for node {}", verticles.size(), NodeInfo.getServerAddress());
        getNodeHolderMap().putAllAsync(NodeInfo.getServerAddress(), processorInfos);
    }


    private void registerVerticles(List<String> addressList) {
        for (String address : addressList) {
            getFeatureHolderMap().put(address, MappingInfo.of(NodeInfo.getServerAddress(), vertxConfiguration.getTag()));
        }
        log.info("Registered {} verticles for node {}", verticles.size(), NodeInfo.getServerAddress());
        getNodeHolderMap().putAllAsync(NodeInfo.getServerAddress(), addressList);
    }


    private IMap<String, NodeInfo> getNodeMap() {
        if (nodeMap == null) {
            nodeMap = hazelcast.getMap(Constant.NODE_LIST);
        }
        return nodeMap;
    }

    private MultiMap<String, MappingInfo> getFeatureHolderMap() {
        if (featureHolderMap == null) {
            featureHolderMap = hazelcast.getMultiMap(Constant.FEATURE_HOLDER);
        }
        return featureHolderMap;
    }

    private MultiMap<String, String> getNodeHolderMap() {
        if (nodeHolderMap == null) {
            nodeHolderMap = hazelcast.getMultiMap(Constant.NODE_HOLDER);
        }
        return nodeHolderMap;
    }
}