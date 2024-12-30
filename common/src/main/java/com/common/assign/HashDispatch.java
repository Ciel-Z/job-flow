package com.common.assign;

import com.common.entity.JobInstance;
import com.common.util.AssertUtils;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.internal.util.HashUtil;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

@Getter
@Order(4)
@Component
@RequiredArgsConstructor
public class HashDispatch implements Dispatch {

    private final HazelcastInstance hazelcast;

    @Override
    public String dispatch(String signature, JobInstance instance) {
        List<String> available = availableNodes(signature);
        AssertUtils.notEmpty(available, "No available nodes for signature: " + signature);
        return available.get(HashUtil.hashCode(instance.getJobId()) & (available.size() - 1));
    }
}
