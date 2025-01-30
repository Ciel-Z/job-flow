package com.admin.selector;

import com.common.constant.Constant;
import com.common.entity.JobInstance;
import com.common.entity.MappingInfo;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.multimap.MultiMap;
import com.hazelcast.sql.SqlResult;

import java.util.List;

public interface Selector {

    /**
     * Returns the strategy ID.
     * @see com.common.entity.JobInfo#dispatchStrategy
     */
    int getStrategyId();

    /**
     * pick one worker.
     *
     * @return the assigned path
     */
    String select(String address, JobInstance instance);

    /**
     * Returns the Hazelcast instance.
     *
     * @return the Hazelcast instance
     */
    HazelcastInstance getHazelcast();


    default List<String> getAvailable(String address, String tag) {
        if (tag != null) {
            SqlResult execute = getHazelcast().getSql().execute("SELECE address FORM " + Constant.FEATURE_HOLDER + " WHERE tag = ?", tag);
            return execute.stream().map(row -> row.getObject(0)).map(String::valueOf).toList();
        }
        MultiMap<String, MappingInfo> featureHolderMap = getHazelcast().getMultiMap(Constant.FEATURE_HOLDER);
        return  featureHolderMap.get(address).stream().map(MappingInfo::getAddress).toList();
    }

    default String workerAddress(String host, String address) {
        return host + ":" + address;
    }

}
