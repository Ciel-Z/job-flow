package com.admin.config;

import com.common.config.VertxConfiguration;
import com.common.constant.Constant;
import com.common.entity.MappingInfo;
import com.common.entity.NodeInfo;
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
public class AdminMappingManager {

    private final HazelcastInstance hazelcast;

    private final VertxConfiguration vertxConfiguration;

    private final List<AbstractEventVerticle<?>> verticles;

    private MultiMap<String, MappingInfo> featureHolderMap;

    private MultiMap<String, String> nodeHolderMap;

    private IMap<String, NodeInfo> nodeMap;


    @PostConstruct
    public void init() {
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        long initialDelay = 3L;
        long interval = vertxConfiguration.getHeartbeatInterval();
        scheduler.scheduleWithFixedDelay(this::registerAdmin, initialDelay, interval, TimeUnit.SECONDS);
    }

    /**
     * Register the verticle binding to the hazelcast.
     */
    public void registerAdmin() {
        IMap<String, NodeInfo> nodeMap = getNodeMap();
        NodeInfo nodeInfo = nodeMap.get(NodeInfo.getServerAddress());
        if (isNodeNotChanged(nodeInfo) && isNodeAlive(nodeInfo)) {
            log.debug("Node {} report alive", NodeInfo.getServerAddress());
            nodeMap.put(NodeInfo.getServerAddress(), NodeInfo.getNowTimeStamp());
            return;
        }
        List<String> list = verticles.stream().map(AbstractEventVerticle::fullAddress).toList();
        register(list);
        nodeMap.put(NodeInfo.getServerAddress(), NodeInfo.getNowTimeStamp());
    }

    public void register(List<String> addresslist) {
        for (String address : addresslist) {
            getFeatureHolderMap().put(address, MappingInfo.of(NodeInfo.getServerAddress(), vertxConfiguration.getTag()));
        }
        log.info("Registered {} verticles for node {}", verticles.size(), NodeInfo.getServerAddress());
        getNodeHolderMap().putAllAsync(NodeInfo.getServerAddress(), addresslist);
    }


    private boolean isNodeNotChanged(NodeInfo nodeInfo) {
        return nodeInfo != null && nodeInfo.getId().equals(NodeInfo.getInfo().getId());
    }


    private boolean isNodeAlive(NodeInfo nodeInfo) {
        return Duration.between(nodeInfo.getTimestamp(), LocalDateTime.now()).getSeconds() <= vertxConfiguration.getTimeout();
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
