package com.admin.assign;

import com.common.entity.JobInstance;
import com.common.util.AssertUtils;
import com.hazelcast.core.HazelcastInstance;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Random;

@Getter
@Component
@RequiredArgsConstructor
public class RandomDispatch implements Dispatch {

    private final HazelcastInstance hazelcast;


    @Override
    public int getStrategyId() {
        return 2;
    }

    @Override
    public String dispatch(String address, JobInstance instance) {
        List<String> nodes = getAvailable(address, instance.getProcessorInfo());
        AssertUtils.notEmpty(nodes, "No available nodes for signature: " + address);
        String host = nodes.get(new Random().nextInt(nodes.size()));
        return workerAddress(host, address);
    }
}
