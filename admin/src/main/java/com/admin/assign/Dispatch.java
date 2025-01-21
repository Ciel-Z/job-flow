package com.admin.assign;

import com.common.constant.Constant;
import com.common.entity.JobInstance;
import com.common.entity.MappingInfo;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.multimap.MultiMap;
import com.hazelcast.sql.SqlResult;

import java.util.List;

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
        if (tag != null) {
            SqlResult execute = getHazelcast().getSql().execute("SELECE address FORM " + Constant.FEATURE_HOLDER + " WHERE tag = ?", tag);
            return execute.stream().map(row -> row.getObject(0)).map(String::valueOf).toList();
        }
        MultiMap<String, MappingInfo> featureHolderMap = getHazelcast().getMultiMap(Constant.FEATURE_HOLDER);
        return  featureHolderMap.get(key).stream().map(MappingInfo::getAddress).toList();
    }

    default String workerAddress(String host, String address) {
        return host + ":" + address;
    }

}
