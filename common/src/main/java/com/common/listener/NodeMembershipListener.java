package com.common.listener;

import com.common.entity.ServerInfo;
import com.common.config.VerticleMappingManager;
import com.hazelcast.cluster.Address;
import com.hazelcast.cluster.Member;
import com.hazelcast.cluster.MembershipEvent;
import com.hazelcast.cluster.MembershipListener;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionalOnBean(ServerInfo.class)
@RequiredArgsConstructor
public class NodeMembershipListener implements MembershipListener {

    private final VerticleMappingManager verticleMappingManager;

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
        verticleMappingManager.unregister(ip);
    }
}
