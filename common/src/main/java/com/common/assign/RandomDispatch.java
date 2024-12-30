package com.common.assign;

import com.common.entity.JobInstance;
import com.common.util.AssertUtils;
import com.hazelcast.core.HazelcastInstance;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Random;

@Getter
@Order(3)
@Component
@RequiredArgsConstructor
public class RandomDispatch implements Dispatch {

    private final HazelcastInstance hazelcast;

    @Override
    public String dispatch(String signature, JobInstance instance) {
        List<String> nodes = availableNodes(signature);
        AssertUtils.notEmpty(nodes, "No available nodes for signature: " + signature);
        String host = nodes.get(new Random().nextInt(nodes.size()));
        return concat(host, signature);
    }

}
