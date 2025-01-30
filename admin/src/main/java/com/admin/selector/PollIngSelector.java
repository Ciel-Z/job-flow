package com.admin.selector;

import com.common.entity.JobInstance;
import com.hazelcast.core.HazelcastInstance;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Getter
@Component
@RequiredArgsConstructor
public class PollIngSelector implements Selector {

    private final HazelcastInstance hazelcast;

    private final AtomicLong counter = new AtomicLong(0);

    @Override
    public int getStrategyId() {
        return 1;
    }

    @Override
    public String select(String address, JobInstance instance) {
        List<String> nodes = getAvailable(address, instance.getTag());
        if (nodes.isEmpty()) {
            log.warn("No available worker nodes for processorInfo: {}", address);
            return address;
        }
        long offset = counter.getAndAdd(1);
        return nodes.get((int) (offset % nodes.size()));
    }
}
