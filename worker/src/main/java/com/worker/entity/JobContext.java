package com.worker.entity;

import com.hazelcast.map.IMap;
import com.worker.logger.JobLogger;
import lombok.Data;

/**
 * 任务上下文
 */
@Data
public class JobContext {

    private Long jobId;

    private String jobName;

    private Long instanceId;

    private Long workflowId;

    private String params;

    private JobLogger logger;

    private IMap<String, Object> workflowContext;

}
