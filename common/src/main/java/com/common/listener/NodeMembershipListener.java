package com.common.listener;

import com.common.constant.Constant;
import com.common.entity.ServerInfo;
import com.hazelcast.cluster.Member;
import com.hazelcast.cluster.MembershipEvent;
import com.hazelcast.cluster.MembershipListener;
import com.hazelcast.core.HazelcastInstance;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionalOnBean(ServerInfo.class)
@RequiredArgsConstructor
public class NodeMembershipListener implements MembershipListener {

    private final ServerInfo serverInfo;

    private final HazelcastInstance hazelcast;

    @Override
    public void memberAdded(MembershipEvent event) {
        Member newMember = event.getMember();
        log.info("New node joined: {}", newMember.getAddress());
    }

    @Override
    public void memberRemoved(MembershipEvent event) {
        Member removedMember = event.getMember();
        log.info("Node leave: {}", removedMember.getAddress());
        hazelcast.getSet(Constant.ONLINE_NODES).remove(removedMember.getAddress());
    }

    @PostConstruct
    public void init() {
        hazelcast.getSet(Constant.ONLINE_NODES).add(serverInfo.getIp());
    }
}
