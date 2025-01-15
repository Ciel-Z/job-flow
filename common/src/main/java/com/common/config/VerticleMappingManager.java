package com.common.config;

import com.common.constant.Constant;
import com.common.entity.NodeInfo;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.multimap.MultiMap;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class VerticleMappingManager {

    private final HazelcastInstance hazelcast;

    private final VertxConfiguration vertxConfiguration;

    public void register(String ip, String address) {
        getFeatureHolderMap().put(address, NodeInfo.of(ip, vertxConfiguration.getTag()));
        getNodeHolderMap().put(ip, address);
    }

    public void unregister(String ip) {
        getNodeHolderMap().remove(ip).forEach(address -> getFeatureHolderMap().remove(address, ip));
    }

    private MultiMap<String, NodeInfo> getFeatureHolderMap() {
        return hazelcast.getMultiMap(Constant.FEATURE_HOLDER);
    }

    private MultiMap<String, String> getNodeHolderMap() {
        return hazelcast.getMultiMap(Constant.NODE_HOLDER);
    }
}
