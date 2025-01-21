package com.common.listener;

import com.common.constant.Constant;
import com.common.entity.MappingInfo;
import com.common.entity.NodeInfo;
import com.hazelcast.cluster.Address;
import com.hazelcast.cluster.Member;
import com.hazelcast.cluster.MembershipEvent;
import com.hazelcast.cluster.MembershipListener;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.multimap.MultiMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionalOnBean(NodeInfo.class)
@RequiredArgsConstructor
public class NodeMembershipListener implements MembershipListener {

    private final HazelcastInstance hazelcast;

    private MultiMap<String, MappingInfo> featureHolderMap;

    private MultiMap<String, String> nodeHolderMap;

    @Override
    public void memberAdded(MembershipEvent event) {
        Member newMember = event.getMember();
        log.info("New node joined: {}", newMember.getAddress());
    }

    @Override
    public void memberRemoved(MembershipEvent event) {
        Member removedMember = event.getMember();
        Address address = removedMember.getAddress();
        String ip = String.format("%s:%s", address.getHost(), address.getPort());
        log.info("Node leave: {}", ip);
        unregister(ip);
    }

    public void unregister(String ip) {
        getNodeHolderMap().remove(ip).forEach(address -> getFeatureHolderMap().remove(address, ip));
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
