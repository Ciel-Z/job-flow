package com.common.assign;

import com.common.constant.Constant;
import com.common.entity.JobInstance;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.multimap.MultiMap;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public interface Dispatch {


    /**
     * Assigns the given path.
     *
     * @param signature the signature to assign
     * @return the assigned path
     */
    String dispatch(String signature, JobInstance instance);

    /**
     * Returns the Hazelcast instance.
     *
     * @return the Hazelcast instance
     */
    HazelcastInstance getHazelcast();


    default Collection<String> getFeature(String signature) {
        MultiMap<String, String> multiMap = getHazelcast().getMultiMap(Constant.FEATURE_HOLDER);
        return multiMap.get(signature);
    }


    default Set<String> getOnline(String signature) {
        return getHazelcast().getSet(Constant.ONLINE_NODES);
    }

    default List<String> availableNodes(String signature) {
        Collection<String> feature = getFeature(signature);
        Set<String> online = getOnline(signature);
        return feature.stream().filter(online::contains).collect(Collectors.toList());
    }

    default String concat(String host, String signature) {
        return host + ":" + signature;
    }
}
