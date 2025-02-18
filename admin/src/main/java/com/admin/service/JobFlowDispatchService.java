package com.admin.service;

public interface JobFlowDispatchService {

    Long start(Long jobFlowId);

    void stop(Long instanceId);

    void retry(Long flowInstanceId, Long nodeId);

}


