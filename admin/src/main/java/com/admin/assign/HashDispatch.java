package com.admin.assign;

import com.common.entity.JobInstance;
import com.common.util.AssertUtils;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.internal.util.HashUtil;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Getter
@Configuration
@RequiredArgsConstructor
public class HashDispatch implements Dispatch {

    private final HazelcastInstance hazelcast;

    @Override
    public int getStrategyId() {
        return 3;
    }

    @Override
    public String dispatch(String address, JobInstance instance) {
        List<String> available = getAvailable(address, instance.getTag());
        AssertUtils.notEmpty(available, "No available nodes for signature: " + address);
        return available.get(HashUtil.hashCode(instance.getJobId()) & (available.size() - 1));
    }

}
