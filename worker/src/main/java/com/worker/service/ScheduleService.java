package com.worker.service;

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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScheduleService implements InitializingBean {

    public static final Long RUNNING_INTERVAL = 15000L;

    private final VertxConfiguration vertxConfiguration;

    private final HazelcastInstance hazelcast;

    private final List<JobHandler> jobHandlers;

    private final List<AbstractEventVerticle<?>> verticles;

    private final NodeInfo localNode;

    private MultiMap<String, MappingInfo> featureHolderMap;

    private MultiMap<String, String> nodeHolderMap;

    private IMap<String, NodeInfo> workerNodeMap;


    @Override
    public void afterPropertiesSet() throws Exception {
        featureHolderMap = hazelcast.getMultiMap(Constant.FEATURE_HOLDER);
        nodeHolderMap = hazelcast.getMultiMap(Constant.NODE_HOLDER);
        workerNodeMap = hazelcast.getMap(Constant.WORKER_LIST);
    }

    /**
     * 心跳检查
     */
    public void heartbeat() {
        NodeInfo nodeInfo = workerNodeMap.get(localNode.getServerAddress());
        if (isNodeChanged(nodeInfo) || isNodeOffline(nodeInfo)) {
            List<String> addressList = verticles.stream().map(AbstractEventVerticle::fullAddress).toList();
            registerVerticles(addressList);
            List<String> handlers = jobHandlers.stream().map(JobHandler::getClass).map(Class::getName).toList();
            registerHandle(handlers);
        }
        log.debug("Heartbeat check for node {}", localNode.getServerAddress());
        workerNodeMap.set(localNode.getServerAddress(), localNode.updateTimeStamp(), RUNNING_INTERVAL * 4, java.util.concurrent.TimeUnit.SECONDS);
    }


    private boolean isNodeChanged(NodeInfo nodeInfo) {
        return nodeInfo == null || !nodeInfo.getId().equals(localNode.getId());
    }


    private boolean isNodeOffline(NodeInfo nodeInfo) {
        return Duration.between(nodeInfo.getTimestamp(), LocalDateTime.now()).getSeconds() > vertxConfiguration.getTimeout();
    }


    private void registerHandle(List<String> processorInfos) {
        for (String processorInfo : processorInfos) {
            String path = PathUtil.getGlobalPath(localNode.getServerAddress(), Constant.DISPATCH);
            MappingInfo mappingInfo = MappingInfo.of(path, vertxConfiguration.getTag());
            featureHolderMap.put(processorInfo, mappingInfo);
        }
        log.info("Registered {} jop handle for node {}", verticles.size(), localNode.getServerAddress());
        nodeHolderMap.putAllAsync(localNode.getServerAddress(), processorInfos);
    }


    private void registerVerticles(List<String> addressList) {
        for (String address : addressList) {
            featureHolderMap.put(address, MappingInfo.of(localNode.getServerAddress(), vertxConfiguration.getTag()));
        }
        log.info("Registered {} verticles for node {}", verticles.size(), localNode.getServerAddress());
        nodeHolderMap.putAllAsync(localNode.getServerAddress(), addressList);
    }

}
