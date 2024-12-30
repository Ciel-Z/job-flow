package com.common.assign;

import com.common.entity.JobInstance;
import com.hazelcast.core.HazelcastInstance;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Getter
@Order(2)
@Component
@RequiredArgsConstructor
public class PublishDispatch implements Dispatch {

    private final HazelcastInstance hazelcast;

    @Override
    public String dispatch(String signature, JobInstance instance) {
        return "signature";
    }

}
