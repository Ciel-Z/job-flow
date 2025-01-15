package com.admin.assign;

import com.common.constant.Constant;
import com.common.entity.JobInstance;
import com.common.entity.NodeInfo;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.multimap.MultiMap;

import java.util.List;
import java.util.stream.Stream;

public interface Dispatch {

    /**
     * Returns the strategy ID.
     * @see com.common.entity.JobInfo#dispatchStrategy
     */
    int getStrategyId();

    /**
     * Assigns the given path.
     *
     * @return the assigned path
     */
    String dispatch(String address, JobInstance instance);

    /**
     * Returns the Hazelcast instance.
     *
     * @return the Hazelcast instance
     */
    HazelcastInstance getHazelcast();


    default List<String> getAvailable(String key, String tag) {
        MultiMap<String, NodeInfo> featureHolderMap = getHazelcast().getMultiMap(Constant.FEATURE_HOLDER);
        Stream<NodeInfo> stream = featureHolderMap.get(key).stream();
        if (tag != null) {
            stream = stream.filter(nodeInfo -> tag.equals(nodeInfo.getTag()));
        }
        return stream.map(NodeInfo::getAddress).toList();
    }

    default String workerAddress(String host, String address) {
        return host + ":" + address;
    }

}
