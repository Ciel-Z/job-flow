package com.common.entity;

import com.hazelcast.map.IMap;
import lombok.Data;

/**
 * 任务上下文
 */
@Data
public class JobContext {

    private Long jobId;

    private Long instanceId;

    private Long workflowId;

    private String params;

    private IMap<String, Object> workflowContext;

}
