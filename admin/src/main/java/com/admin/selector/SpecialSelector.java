package com.admin.selector;

import com.common.entity.JobInstance;
import com.common.util.AssertUtils;
import com.hazelcast.core.HazelcastInstance;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.HashSet;
import java.util.List;

@Getter
@Component
@RequiredArgsConstructor
public class SpecialSelector implements Selector {

    private final HazelcastInstance hazelcast;


    @Override
    public int getStrategyId() {
        return 4;
    }


    @Override
    public String select(String address, JobInstance instance) {
        String designatedWorkers = instance.getJobInfo().getDesignatedWorkers();
        String[] workers = StringUtils.split(designatedWorkers, ",");
        AssertUtils.notEmpty(workers, "missing worker address");
        List<String> available = getAvailable(address, instance.getTag());
        AssertUtils.notEmpty(available, "No available nodes for processorInfo: " + address);

        HashSet<String> availableSet = new HashSet<>(available);
        for (String worker : workers) {
            if (availableSet.contains(worker)) {
                return worker;
            }
        }
        throw new IllegalStateException("No available worker nodes for processorInfo: " + address);
    }
}
