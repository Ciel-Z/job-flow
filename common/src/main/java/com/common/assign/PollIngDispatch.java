package com.common.assign;

import com.common.entity.JobInstance;
import com.common.util.AssertUtils;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.cp.IAtomicLong;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

@Getter
@Order(1)
@Component
@RequiredArgsConstructor
public class PollIngDispatch implements Dispatch {

    private final HazelcastInstance hazelcast;

    @Override
    public String dispatch(String signature, JobInstance instance) {
        List<String> nodes = availableNodes(signature);
        AssertUtils.notEmpty(nodes, "No available nodes for signature: " + signature);
        IAtomicLong counter = hazelcast.getCPSubsystem().getAtomicLong(signature);
        long offset = counter.getAndAdd(1);
        if (offset > Integer.MAX_VALUE) {
            counter.set(0);
        }
        String host = nodes.get((int) (offset % nodes.size()));
        return concat(host, signature);
    }

}
