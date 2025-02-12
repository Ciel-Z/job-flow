package com.admin.service;

import com.common.dag.JobFlowDAG;
import com.common.entity.JobFlowInstance;

public interface JobFlowDispatchService {

    Long start(Long jobFlowId);

    void stop(Long instanceId);

    void retry(Long flowInstanceId, Long nodeId);

    void startJob(JobFlowInstance instance, JobFlowDAG dag, Long nodeId);
}
