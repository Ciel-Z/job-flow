package com.admin.assign;

import com.common.entity.JobInstance;
import com.common.util.AssertUtils;
import com.common.util.PathUtil;
import com.hazelcast.core.HazelcastInstance;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Getter
@Component
@RequiredArgsConstructor
public class SpecialDispatch implements Dispatch {

    private final HazelcastInstance hazelcast;


    @Override
    public int getStrategyId() {
        return 4;
    }

    @Override
    public String dispatch(String address, JobInstance instance) {
        AssertUtils.notEmpty(instance.getExtra(), "missing worker address");
        return PathUtil.getGlobalPath(instance.getExtra(), address);
    }
}
