package com.admin.assign;

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
public class PollIngDispatch implements Dispatch {

    private final HazelcastInstance hazelcast;

    private final AtomicLong counter = new AtomicLong(0);

    @Override
    public int getStrategyId() {
        return 1;
    }

    @Override
    public String dispatch(String address, JobInstance instance) {
        List<String> nodes = getAvailable(address, instance.getTag());
        if (nodes.isEmpty()) {
            log.warn("No available worker nodes for processorInfo: {}", instance.getTag());
            return address;
        }
        long offset = counter.getAndAdd(1);
        String workerAddress = nodes.get((int) (offset % nodes.size()));
        return workerAddress;
    }
}
