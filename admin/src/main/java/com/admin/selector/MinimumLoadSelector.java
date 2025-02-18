package com.admin.selector;

import com.common.constant.Constant;
import com.common.entity.JobInstance;
import com.hazelcast.core.HazelcastInstance;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Getter
@Slf4j
@Component
@RequiredArgsConstructor
public class MinimumLoadSelector implements Selector {

    private final HazelcastInstance hazelcast;

    @Override
    public int getStrategyId() {
        return 5;
    }

    @Override
    public String select(String address, JobInstance instance) {
        List<String> available = getAvailable(address, instance.getTag());
        if (available.isEmpty()) {
            log.warn("No available worker nodes for processorInfo: {}", address);
            return address;
        }
        String res = address;
        long min = Long.MAX_VALUE;
        for (String ip : available) {
            long count = hazelcast.getPNCounter(String.format(Constant.WORKER_JOB_COUNTER, ip)).get();
            if (min > count) {
                res = ip;
            }
        }
        return res;
    }

}
